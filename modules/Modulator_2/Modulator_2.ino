/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board (using MINI Controllino)
 */
#include <Controllino.h>
#include <CRC32.h>
#include <Wire.h>


typedef union {
    float fvalue;
    uint32_t i32value;
  } FloatUint32;
  
/*
 * I2C ADDRESS (SLAVE)
 */ 
#define I2C_SLAVE_ADDR 0x10

/* 
 * I2C BUFFER SIZE
 */
#define I2C_BUFFER 32

/* 
 * MAX PARAMETER VALUES
 */
#define MAX_VOLTAGE 4000
#define MAX_FREQUENCY 5


/*
 * Variables used in interrupted handlers
 */
volatile uint32_t i2c_buffer = 0; 
volatile byte data_array[6];
volatile boolean updateIOFromI2CBool = false;
volatile FloatUint32  output_curr_positive;   // The output current variable for HV+
volatile FloatUint32  output_curr_negative;   // The output current variable for HV-
volatile FloatUint32  uv_voltage;             // The UV sensor voltage value
volatile int16_t negative_volt; // The negative voltage variable
volatile int16_t positive_volt; // The positive voltage variable
volatile int16_t frequency;     // The frequency variable
volatile int16_t dutycycle;     // The duty cycle variable


/*
 * The time Reset actions
 */
unsigned long MAINtime = 0;
unsigned long LOOPtime = 0;
boolean switchDown = false;
boolean switchUp = false;
unsigned long debugtime = 0;
unsigned long temptime = 0;
unsigned int delayTime = 1;
unsigned int delayWrite = 1;

/*
 * Variables used Modulator Loop
 */
boolean mainOn = false;
boolean switchedMain = false;
boolean resetLoop = true;

/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean MAIN_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;

/*
 *  The waiting time for debug purpose
 */
long debug_wait = 1000;

float FACTOR = 11.46;
/*
 *  I2C BIT ASSIGNATION (MAX = 32) 
 */
#define MAIN_ON_CMD_BIT                 0   // MAIN ON command bit
#define MAIN_OFF_CMD_BIT                1   // MAIN OFF command bit
#define MAIN_ON_STATUS_BIT              2   // MAIN ON Status bit
#define MAIN_OFF_STATUS_BIT             3   // MAIN OFF Status bit
#define NEGATIVE_VOLT_TRIG_BIT          4   // NEGATIVE Volt Trigger bit
#define POSITIVE_VOLT_TRIG_BIT          5   // POSITIVE Volt Trigger bit
#define FREQUENCY_TRIG_BIT              6   // FREQUENCY Trigger bit
#define DUTYCYCLE_TRIG_BIT              7   // DUTY CYCLE Trigger bit
#define ARD_RESET_BIT                   8   // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */ 
// Digital pins (OUTPUTS)
#define MAIN_ON_CMD                CONTROLLINO_R0   // SWITCH ON MAIN COMMAND
#define MAIN_OFF_CMD               CONTROLLINO_R1   // SWITCH OFF MAIN COMMAND
#define NEGATIVE_VOLT_RELAY_CMD    CONTROLLINO_D0   // NEGATIVE VOLTAGE RELAY COMMAND
#define POSITIVE_VOLT_RELAY_CMD    CONTROLLINO_D1   // POSITIVE VOLTAGE RELAY COMMAND
#define NEGATIVE_VOLT_VALUE_CMD    CONTROLLINO_PIN_HEADER_PWM_04   // NEGATIVE VOLTAGE VALUE COMMAND (PWM)
#define POSITIVE_VOLT_VALUE_CMD    CONTROLLINO_PIN_HEADER_PWM_02   // POSITIVE VOLTAGE VALUE COMMAND (PWM)


// Analog pins (INPUTS)
#define NEGATIVE_VOLT_A0_VALUE     CONTROLLINO_A0   // NEGATIVE VOLTAGE A0 VALUE
#define NEGATIVE_VOLT_A1_VALUE     CONTROLLINO_A1   // NEGATIVE VOLTAGE A1 VALUE
#define POSITIVE_VOLT_A2_VALUE     CONTROLLINO_A2   // POSITIVE VOLTAGE A2 VALUE
#define POSITIVE_VOLT_A3_VALUE     CONTROLLINO_A3   // POSITIVE VOLTAGE A3 VALUE
#define OUTPUT_VOLT_A4_VALUE       CONTROLLINO_A4   // OUTPUT VOLTAGE A4 VALUE
#define OUTPUT_VOLT_A5_VALUE       CONTROLLINO_A5   // OUTPUT VOLTAGE A5 VALUE
#define REF_VOLT_A6_VALUE          CONTROLLINO_A6   // OUTPUT VOLTAGE A6 VALUE
#define MAIN_ON_STATUS             CONTROLLINO_A8   // MAIN ON STATUS
#define MAIN_OFF_STATUS            CONTROLLINO_A9   // MAIN OFF STATUS
#define UV_VOLT_VALUE              CONTROLLINO_PIN_HEADER_ANALOG_ADC_IN_07 // UV SENSOR VOLTAGE

void(*resetArd) (void) = 0; //declare reset function @ address 0

void setup() {

  // Open Serial communication for Console port.
 Serial.begin(9600);
 
 Serial.println("START SETUP");

 // Init IO & I2C
 InitializeIO();
 InitializeI2C();
 
 // Init Wire
 Wire.begin(I2C_SLAVE_ADDR);
 Wire.setTimeout(1000);
 
 Serial.println("END SETUP");
}
 
void loop() {  
   
  if (updateIOFromI2CBool == true) {
    //Serial.print("Received command; updated i2c_buffer =");
    //Serial.println(i2c_buffer,BIN);
    updateIOFromI2CBool = false;
    // Update I/O from updated i2c_buffer
    UpdateIOFromI2C();
  }
  
  // Update i2c_buffer from I/O
     UpdateI2CFromIO();

  // reset & Check procedure
     ResetAndCheck();

  // Modulator Loop voltage
     if ( mainOn == true ) {
        //Serial.print("switchedMain=");Serial.println(switchedMain);
        if ( switchedMain == false ) {
          switchedMain = true;
          // Read back the value set
          float refvolt = analogRead(REF_VOLT_A6_VALUE) * 0.03;
          float negative_volt_raw = analogRead(NEGATIVE_VOLT_A0_VALUE) * 0.03;
          negative_volt = 6000 * ( negative_volt_raw / FACTOR - 1 );
          // Read back the value set
          float positive_volt_raw = analogRead(POSITIVE_VOLT_A2_VALUE) * 0.03;
          positive_volt = 6000 * ( positive_volt_raw / FACTOR - 1 );
          Serial.print("REF_VOLT_A6_VALUE=");Serial.println(analogRead(REF_VOLT_A6_VALUE));
          Serial.print("NEGATIVE_VOLT_A0_VALUE=");Serial.println(analogRead(NEGATIVE_VOLT_A0_VALUE));
        }
        ModulatorLoop();
     }
     
  // For debug purpose
  /*if ( millis() - debugtime > debug_wait) {
     Serial.print("Loop: i2c_buffer=");
     Serial.println(i2c_buffer,BIN);
     Serial.println("-------------------------------");
     Serial.print("modulating on/off ?");Serial.println(mainOn);
     Serial.print("output current negative=");Serial.println(output_curr_negative.fvalue);
     Serial.print("output current positive=");Serial.println(output_curr_positive.fvalue);
     Serial.print("negative voltage=");Serial.println(negative_volt);
     Serial.print("positive voltage=");Serial.println(positive_volt);
     Serial.print("frequency=");Serial.println(frequency);
     Serial.print("duty cycle=");Serial.println(dutycycle);
     Serial.println("-------------------------------");
     debugtime = millis();
  }*/
  //delay(1);
}

void InitializeI2C()
{
  Serial.println("InitializeI2C...");
  Wire.onReceive(receiveEvent); // register receive event (i2c_buffer sent by master)  
  Wire.onRequest(requestEvent); // register request event (i2c_buffer sent by slave)
  Serial.println("InitializeI2C...Done.");
}

 void InitializeIO()
{    
  int ret;
  Serial.println("InitializeIO...");
 
  // Digital OUTPUTS assignation & initialization
  digitalWrite(MAIN_ON_CMD,LOW);                  // Set MAIN_ON_CMD LOW
  digitalWrite(MAIN_OFF_CMD,HIGH);                // Set MAIN_OFF_CMD HIGH
  pinMode(MAIN_ON_CMD, OUTPUT);                   // Set the digital pin as output for MAIN ON CMD
  pinMode(MAIN_OFF_CMD, OUTPUT);                  // Set the digital pin as output for MAIN OFF CMD
  I2CsetBit(MAIN_ON_CMD_BIT,0x00);                // Set MAIN_ON_CMD_BIT LOW
  I2CsetBit(MAIN_OFF_CMD_BIT,0x01);               // Set MAIN_OFF_CMD_BIT HIGH
  // Relay for HV
  DDRE |= (1 << DDE4);
  DDRE |= (1 << DDE5);
  // PWM signals
  pinMode(NEGATIVE_VOLT_VALUE_CMD, OUTPUT);       // Set the digital pin as output for NEGATIVE VALUE CMD
  pinMode(POSITIVE_VOLT_VALUE_CMD, OUTPUT);       // Set the digital pin as output for POSITIVE VALUE CMD

  // Analog INPUTS assignation & initialization
  pinMode(MAIN_ON_STATUS, INPUT);                 // Set the digital pin as input for MAIN ON STATUS
  pinMode(MAIN_OFF_STATUS, INPUT);                // Set the digital pin as input for MAIN OFF STATUS
  pinMode(NEGATIVE_VOLT_A0_VALUE, INPUT);         // Set the digital pin as input for NEGATIVE VOLT VALUE
  pinMode(NEGATIVE_VOLT_A1_VALUE, INPUT);         // Set the digital pin as input for NEGATIVE VOLT DUMP VALUE
  pinMode(POSITIVE_VOLT_A2_VALUE, INPUT);         // Set the digital pin as input for POSITIVE VOLT VALUE
  pinMode(POSITIVE_VOLT_A3_VALUE, INPUT);         // Set the digital pin as input for POSITIVE VOLT DUMP VALUE
  pinMode(OUTPUT_VOLT_A4_VALUE, INPUT);           // Set the digital pin as input for OUTPUT VOLT VALUE
  pinMode(OUTPUT_VOLT_A5_VALUE, INPUT);           // Set the digital pin as input for OUTPUT VOLT DUMP VALUE
  pinMode(UV_VOLT_VALUE, INPUT);                  // Set the analog pin as input for UV SENDOR VOLT VALUE

  //Initialize output values
  output_curr_negative.fvalue = 0;
  output_curr_positive.fvalue = 0;

  // Set negative voltage (0-6000 --> 5-0V)
  negative_volt = 0;
  analogWrite(NEGATIVE_VOLT_VALUE_CMD, 255 * (6000 - (float)negative_volt) / 6000);
  delay(10);
  // Read back the value set
  float refvolt = analogRead(REF_VOLT_A6_VALUE) * 0.03;
  float negative_volt_raw = analogRead(NEGATIVE_VOLT_A0_VALUE) * 0.03;
  negative_volt = 6000 * ( negative_volt_raw / FACTOR - 1 );
  Serial.print("REF_VOLT_A6_VALUE=");Serial.println(analogRead(REF_VOLT_A6_VALUE));
  Serial.print("NEGATIVE_VOLT_A0_VALUE=");Serial.println(analogRead(NEGATIVE_VOLT_A0_VALUE));
  // Set positive voltage (0-6000 --> 0-5V)
  positive_volt = 0;
  analogWrite(POSITIVE_VOLT_VALUE_CMD, 255 * (float)positive_volt / 6000);
  delay(10);
  // Read back the value set
  Serial.print("POSITIVE_VOLT_A2_VALUE=");Serial.println(analogRead(POSITIVE_VOLT_A2_VALUE));
  float positive_volt_raw = analogRead(POSITIVE_VOLT_A2_VALUE) * 0.03;
  positive_volt = 6000 * ( positive_volt_raw / FACTOR - 1 );
  digitalWrite(NEGATIVE_VOLT_RELAY_CMD, 0x00);
  digitalWrite(POSITIVE_VOLT_RELAY_CMD, 0x00);
 
  frequency = 1;
  dutycycle = 50;
  output_curr_negative.fvalue = 0;
  output_curr_positive.fvalue = 0;
  
}

void requestEvent() {

  // For debug purpose
  //Serial.print("Request_event: i2c_buffer=");
  //Serial.println(i2c_buffer,BIN);
  
  // Send i2c_buffer to master (create 4 bytes array)
  byte i2c_array[28];
  
  uint32_t tmp1 = output_curr_negative.i32value;
  //uint32_t tmp1 = 456;
  i2c_array[0] = ( tmp1 >> 24) & 0xFF;
  i2c_array[1] = ( tmp1 >> 16) & 0xFF;
  i2c_array[2] = ( tmp1 >> 8) & 0xFF;
  i2c_array[3] = tmp1 & 0xFF;

  uint32_t tmp2 = output_curr_positive.i32value;
  //uint32_t tmp2 = 456;
  i2c_array[4] = ( tmp2 >> 24) & 0xFF;
  i2c_array[5] = ( tmp2 >> 16) & 0xFF;
  i2c_array[6] = ( tmp2 >> 8) & 0xFF;
  i2c_array[7] = tmp2 & 0xFF;

  
  uint32_t tmp3 = uv_voltage.i32value;
  i2c_array[8] = ( tmp3 >> 24) & 0xFF;
  i2c_array[9] = ( tmp3 >> 16) & 0xFF;
  i2c_array[10] = ( tmp3 >> 8) & 0xFF;
  i2c_array[11] = tmp3 & 0xFF;

  int16_t tmp = negative_volt;
  i2c_array[12] = ( tmp >> 8) & 0xFF;
  i2c_array[13] =  tmp & 0xFF;

  tmp = positive_volt;
  i2c_array[14] = ( tmp >> 8) & 0xFF;
  i2c_array[15] =  tmp & 0xFF;

  tmp = frequency;
  i2c_array[16] = ( tmp >> 8) & 0xFF;
  i2c_array[17] =  tmp & 0xFF;

  tmp = dutycycle;
  i2c_array[18] = ( tmp >> 8) & 0xFF;
  i2c_array[19] =  tmp & 0xFF;
  
  i2c_array[20] = (i2c_buffer >> 24) & 0xFF;
  i2c_array[21] = (i2c_buffer >> 16) & 0xFF;
  i2c_array[22] = (i2c_buffer >> 8) & 0xFF;
  i2c_array[23] = i2c_buffer & 0xFF;

  uint32_t crc = CRC32::calculate(i2c_array, 24);

  i2c_array[24] = (crc >> 24) & 0xFF;
  i2c_array[25] = (crc >> 16) & 0xFF;
  i2c_array[26] = (crc >> 8) & 0xFF;
  i2c_array[27] = crc & 0xFF;

  /*Serial.print("I2C_BUFFER=");
  for (int i = 0 ; i < 12; i++)
     Serial.println(i2c_array[i]);
  */
  Wire.write(i2c_array, 28);

}
void receiveEvent(int numbyte) {

  //Serial.print("ReceiveEvent: numbyte = ");
  //Serial.println(numbyte);

  // Update i2c_buffer from master (4 bytes)

  if (Wire.available() == 10) { // Expect 10 bytes of data (including CRC32), update i2c_buffer
    data_array[0] = Wire.read();
    data_array[1] = Wire.read();
    data_array[2] = Wire.read();
    data_array[3] = Wire.read();

    // The value of the triggered command
    data_array[4] = Wire.read();
    data_array[5] = Wire.read();
    
    // last 4 bytes of data corresponds to CRC32
    byte crc_array[4];
    crc_array[0] = Wire.read();
    crc_array[1] = Wire.read();
    crc_array[2] = Wire.read();
    crc_array[3] = Wire.read();

    uint32_t crc;
    crc = crc_array[0];
    crc = (crc << 8) | crc_array[1];
    crc = (crc << 8) | crc_array[2];
    crc = (crc << 8) | crc_array[3];

    // calculate CRC32
    uint32_t crcdata = CRC32::calculate(data_array, 6);

    if ( crc == crcdata ) {
        //Serial.print(" Checksum good:");Serial.println(crc);
        // Update i2c_buffer in the interrupted section
        i2c_buffer = data_array[0];
        i2c_buffer = (i2c_buffer << 8) | data_array[1];
        i2c_buffer = (i2c_buffer << 8) | data_array[2];
        i2c_buffer = (i2c_buffer << 8) | data_array[3];

        // UpdateIOFromI2C in the loop
        updateIOFromI2CBool = true;    
    }
    else {
      //Serial.print(" Checksum bad:");Serial.print(crc);Serial.print("!=");Serial.println(crcdata);
      //for (int i= 0 ; i < 6;i++)
      //  Serial.println(data_array[i],2);
    }
  }
  else // Flush the wrong Wire buffer
     while(Wire.available()) Wire.read();
}

void I2CsetBit(int bit, int value) {
  if (bit < I2C_BUFFER) {
    if (value == 1)
      bitSet(i2c_buffer,bit);
    else if (value == 0)
      bitClear(i2c_buffer,bit);
  }
}
void ResetAndCheck() {

/*
 *  MAIN Case
 */
 // Reset MAIN_OFF_CMD
  if (digitalRead(MAIN_OFF_CMD) == LOW && MAIN_RESET == true) {
    if ( millis() - MAINtime > reset_wait) {
       digitalWrite(MAIN_OFF_CMD,HIGH);  // RESET MAIN_OFF
       I2CsetBit(MAIN_OFF_CMD_BIT,0x01); // RESET MAIN_OFF BIT
       MAIN_RESET = false;  
       mainOn = false;
       switchedMain = false;
    }
  }
  // Reset MAIN_ON_CMD
  if (digitalRead(MAIN_ON_CMD) == HIGH && MAIN_RESET == true) {
    if ( millis() - MAINtime > reset_wait) {
       digitalWrite(MAIN_ON_CMD,LOW);   // RESET MAIN_ON
       I2CsetBit(MAIN_ON_CMD_BIT,0x00); // RESET MAIN_ON BIT 
       MAIN_RESET = false;
       mainOn = true;
    }
  }
  /***********************************************************************************************************/
  // Check Reset Status
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,ARD_RESET_BIT) == 0x01)
    resetArd();
  /***********************************************************************************************************/
}
void UpdateIOFromI2C()
{    
  //Serial.println("UPDATE IO FROM I2C");
  /***********************************************************************************************************/
  /* Update MAIN ON/OFF position */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,MAIN_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,MAIN_OFF_CMD_BIT) == 0x01 && MAIN_RESET == false) {
     digitalWrite(MAIN_ON_CMD,HIGH);     // MAIN_ON
     MAINtime = millis();
     MAIN_RESET = true; 
  }
  else if (bitRead(i2c_buffer,MAIN_ON_CMD_BIT) == 0x00 && bitRead(i2c_buffer,MAIN_OFF_CMD_BIT) == 0x00 && MAIN_RESET == false) { 
     digitalWrite(MAIN_OFF_CMD,LOW); // MAIN_OFF
     MAINtime = millis();
     MAIN_RESET = true; 
  }

  /***********************************************************************************************************/
  /* Update Value set */
  /***********************************************************************************************************/
  if ( bitRead(i2c_buffer,NEGATIVE_VOLT_TRIG_BIT) == 0x01 ) { // Update NEGATIVE VOLTAGE
        negative_volt = data_array[4];
        negative_volt = (negative_volt << 8) | data_array[5];
        if ( negative_volt >= 0 &&  negative_volt <= MAX_VOLTAGE ){
          Serial.print("SETTING NEGATIVE VOLTAGE = ");Serial.println(255 * (float)negative_volt / 6000);
          analogWrite(NEGATIVE_VOLT_VALUE_CMD, 255 * (6000 - (float)negative_volt) / 6000);
          delay(10);
        }
        // Read back the value set
        delay(10);
        float refvolt = analogRead(REF_VOLT_A6_VALUE) * 0.03;
        float negative_volt_raw = analogRead(NEGATIVE_VOLT_A0_VALUE) * 0.03;
        negative_volt = 6000 * ( negative_volt_raw / FACTOR - 1 );
        switchedMain = false;
        // Reset trigger
        I2CsetBit(NEGATIVE_VOLT_TRIG_BIT,0x00);
  }
  else if ( bitRead(i2c_buffer,POSITIVE_VOLT_TRIG_BIT) == 0x01 ) { // Update POSITIVE VOLTAGE
        positive_volt = data_array[4];
        positive_volt = (positive_volt << 8) | data_array[5];
        if ( positive_volt >= 0 &&  positive_volt <= MAX_VOLTAGE ) {
           Serial.print("SETTING POSITIVE VOLTAGE = ");Serial.println(255 * (float)positive_volt / 6000);
           analogWrite(POSITIVE_VOLT_VALUE_CMD, 255 * (float)positive_volt / 6000);
           delay(10);
        }
        // Read back the value set
        delay(10);
        float refvolt = analogRead(REF_VOLT_A6_VALUE) * 0.03;
        float positive_volt_raw = analogRead(POSITIVE_VOLT_A2_VALUE) * 0.03;
        positive_volt = 6000 * ( positive_volt_raw / FACTOR - 1 );
        switchedMain = false;
        // Reset trigger
        I2CsetBit(POSITIVE_VOLT_TRIG_BIT,0x00);
  }
  else if ( bitRead(i2c_buffer,FREQUENCY_TRIG_BIT) == 0x01 ) { // Update FREQUENCY
        //Serial.print("UPDATE FREQUENCY:");
        int16_t tmpfreq = data_array[4];
        tmpfreq = (tmpfreq << 8) | data_array[5];
        //Serial.println(tmpfreq);
        if ( tmpfreq <= MAX_FREQUENCY)
          frequency = tmpfreq;
        //Serial.println(frequency);
        // Reset trigger
        I2CsetBit(FREQUENCY_TRIG_BIT,0x00);
  }
  else if ( bitRead(i2c_buffer,DUTYCYCLE_TRIG_BIT) == 0x01 ) { // Update DUTY CYCLE
        dutycycle = data_array[4];
        dutycycle = (dutycycle << 8) | data_array[5];
        // Reset trigger
        I2CsetBit(DUTYCYCLE_TRIG_BIT,0x00);
  }
  // Reset Loop
  resetLoop = true;
}

void UpdateI2CFromIO()
{ 
  /***********************************************************************************************************/
  /* Update MAIN position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(MAIN_ON_STATUS) == HIGH && digitalRead(MAIN_OFF_STATUS) == LOW) { // MAIN ON STATUS
     I2CsetBit(MAIN_ON_STATUS_BIT,0x01);     // UPDATE MAIN ON STATUS BIT
     I2CsetBit(MAIN_OFF_STATUS_BIT,0x00);    // UPDATE MAIN OFF STATUS BIT
  }
  else if (digitalRead(MAIN_OFF_STATUS) == HIGH && digitalRead(MAIN_ON_STATUS) == LOW) { // MAIN OFF STATUS
     if (bitRead(i2c_buffer,MAIN_ON_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,MAIN_OFF_STATUS_BIT) == 0x00) { // IF MAIN ON STATUS BIT
        // MAIN OFF command
        digitalWrite(MAIN_OFF_CMD,LOW);  // MAIN OFF COMMAND
        MAINtime = millis();
        MAIN_RESET = true;
     }
     I2CsetBit(MAIN_ON_STATUS_BIT,0x00);     // UPDATE MAIN ON STATUS BIT
     I2CsetBit(MAIN_OFF_STATUS_BIT,0x01);    // UPDATE MAIN OFF STATUS BIT
  }
  else {
     // ERROR FAN STATUS BIT
     I2CsetBit(MAIN_ON_STATUS_BIT,0x00);    // UPDATE MAIN ON STATUS BIT
     I2CsetBit(MAIN_OFF_STATUS_BIT,0x00);   // UPDATE MAIN OFF STATUS  BIT
     mainOn = false;
     switchedMain = false;
  }  
}

void ModulatorLoop()
{
   // Update the UV voltage sensor with PIN HEADER conversion; includes also conversion to mW/cm2
   uv_voltage.fvalue = analogRead(UV_VOLT_VALUE)* (5.0 / 1023.0) * 9 / 4.3;  
   Serial.print("UV voltage value=");
   Serial.println(uv_voltage.fvalue);
   //Serial.println("ModulatorLoop...");
   // Initialize new modulation loop with frequency and dutycycle
   if ( resetLoop == true ) {
      resetLoop = false;
      LOOPtime = millis();
      //Serial.print("ModulatorLoop...FIRST RESET: LOOPtime=");
      //Serial.println(LOOPtime);
      digitalWrite(NEGATIVE_VOLT_RELAY_CMD, 0x00);
      delay(delayWrite);
      digitalWrite(POSITIVE_VOLT_RELAY_CMD, 0x00);
      delay(delayWrite);
      delay(2000);
      return;
   }
   // Regolate the signal with given frequency and duty cycle
   unsigned long CURRtime = millis() - LOOPtime ;
   float vali = ((1 / (float)frequency) * (float)dutycycle / 100) * 1000;
   float vals = (1 / (float)frequency) * 1000;
   /*Serial.print("ModulatorLoop: CURRtime - LOOPtime=");
   Serial.println(CURRtime);
   Serial.print("ModulatorLoop: vali=");
   Serial.println(vali);
   Serial.print("ModulatorLoop: vals=");
   Serial.println(vals);*/
   delay(delayWrite);
   if ( dutycycle != 0 && dutycycle != 100) {
      if ( CURRtime < vali ) {
        //Serial.println("ModulatorLoop...Part 1");
        if (switchDown == false ) {
          switchDown = true;
          if ( digitalRead(NEGATIVE_VOLT_RELAY_CMD) == 0x00) {
            //PORTG = PORTG & B11011111;
            PORTE &= ~(1 << PE4);
            //digitalWrite(NEGATIVE_VOLT_RELAY_CMD, 0x00);
            delay(delayWrite);
            //PORTH = PORTH & B11110111;
            PORTE &= ~(1 << PE5);
            //digitalWrite(POSITIVE_VOLT_RELAY_CMD, 0x00);
            delay(delayTime);
            //PORTG = PORTG | B00100000;
            PORTE |= (1 << PE4);
            //digitalWrite(NEGATIVE_VOLT_RELAY_CMD, 0x01);
            delay(delayWrite);

          }
          else  {
            //PORTG = PORTG & B11011111;
            PORTE &= ~(1 << PE4);
            //digitalWrite(NEGATIVE_VOLT_RELAY_CMD, 0x00);
            delay(delayWrite);
            //PORTH = PORTH & B11110111;
            PORTE &= ~(1 << PE5);
            //digitalWrite(POSITIVE_VOLT_RELAY_CMD, 0x00);
            delay(delayTime);
            //PORTH = PORTH | B00001000;
            PORTE |= (1 << PE5);
            //digitalWrite(POSITIVE_VOLT_RELAY_CMD, 0x01);
            delay(delayWrite);
          }
          // Evaluate the output voltage & current
          EvaluateOutputs("1");
        }
      }
      else if (  CURRtime >= vali && CURRtime < vals   ) {
        //Serial.println("ModulatorLoop...Part 2");
        if (switchUp == false ) {
          switchUp = true;
          if ( digitalRead(NEGATIVE_VOLT_RELAY_CMD) == 0x00 ) {
            //PORTG = PORTG & B11011111;
            PORTE &= ~(1 << PE4);
            //digitalWrite(NEGATIVE_VOLT_RELAY_CMD, 0x00);
            delay(delayWrite);
            //PORTH = PORTH & B11110111;
            PORTE &= ~(1 << PE5);
            //digitalWrite(POSITIVE_VOLT_RELAY_CMD, 0x00);
            delay(delayTime);
            //PORTG = PORTG | B00100000;
            PORTE |= (1 << PE4);
            //digitalWrite(NEGATIVE_VOLT_RELAY_CMD, 0x01);
            delay(delayWrite);
          }
          else {
            //PORTG = PORTG & B11011111;
            PORTE &= ~(1 << PE4);
            //digitalWrite(NEGATIVE_VOLT_RELAY_CMD, 0x00);
            delay(delayWrite);
            //PORTH = PORTH & B11110111;
            PORTE &= ~(1 << PE5);
            //digitalWrite(POSITIVE_VOLT_RELAY_CMD, 0x00);
            delay(delayTime);
            //PORTH = PORTH | B00001000;
            PORTE |= (1 << PE5);
            //digitalWrite(POSITIVE_VOLT_RELAY_CMD, 0x01);
            delay(delayWrite);
          }
          // Evaluate the output voltage & current
          EvaluateOutputs("2");
        }
      } 
      else if ( CURRtime >= vals ) {
         // Reset LOOPtime and switches
         LOOPtime = millis(); 
         //Serial.print("ModulatorLoop...SUCCESSIVE RESET: LOOPtime=");
         //Serial.println(LOOPtime);
         switchDown = false;
         switchUp = false;
         EvaluateOutputs("3");
      }
   }
   else if (dutycycle == 0) {
      digitalWrite(NEGATIVE_VOLT_RELAY_CMD, 0x00);
      delay(delayWrite);
      digitalWrite(POSITIVE_VOLT_RELAY_CMD, 0x01);
      delay(delayWrite);
      // Evaluate the output voltage & current
      EvaluateOutputs("4");
      
   }
   else if (dutycycle == 100) {
      digitalWrite(NEGATIVE_VOLT_RELAY_CMD, 0x01);
      delay(delayWrite);
      digitalWrite(POSITIVE_VOLT_RELAY_CMD, 0x00);
      delay(delayWrite);
      // Evaluate the output voltage & current
      EvaluateOutputs("5");
   }
}

void EvaluateOutputs(String c) 
{
   //Serial.println("EvaluateOutputs...");
   // Evaluate the output current
   output_curr_positive.fvalue = (analogRead(POSITIVE_VOLT_A2_VALUE)-analogRead(POSITIVE_VOLT_A3_VALUE))  * 0.03  * 1000; // uA
   output_curr_negative.fvalue = (analogRead(NEGATIVE_VOLT_A0_VALUE)-analogRead(NEGATIVE_VOLT_A1_VALUE))  * 0.03  * 1000; // uA
   //Serial.print(":output_current="); Serial.println(output_curr.fvalue,8);
}
