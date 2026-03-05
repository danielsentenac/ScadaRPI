/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board (using MINI Controllino)
 */
#include <Controllino.h>
#include <CRC32.h>
#include <Wire.h>
#include "Adafruit_MAX31855.h" // Reads temperature through SPI

// Conversion tool

typedef union {
    float fvalue;
    uint32_t i32value;
  } FloatUint32;
  
float tempbuf; // The temporary temperature read

int tempDelta = 10; // The allowable delta between two consecutive measurements

int errcnt = 0;

#define MAX_ERRCNT 10

// The K thermocouple sensor (SPI communication using Adafruit_MAX31855 library)

#define MAXCS   CONTROLLINO_PIN_HEADER_SS
Adafruit_MAX31855 thermocouple(MAXCS);

/*
 * I2C ADDRESS (SLAVE)
 */ 
#define I2C_SLAVE_ADDR 0x10

/* 
 * I2C BUFFER SIZE
 */
#define I2C_BUFFER 32

/*
 * Variables used in interrupted handlers
 */
volatile uint32_t i2c_buffer = 0; 
volatile byte data_array[4];
volatile boolean updateIOFromI2CBool = false;
volatile byte crc_array[4];
volatile FloatUint32 temp; // The temperature variable

/*
 * The time Reset actions
 */
unsigned long PUMPtime = 0;
unsigned long AC_STOPtime_1 = 0;
unsigned long AC_STOPtime_2 = 0;
unsigned long AC_HIGHtime_1 = 0;
unsigned long AC_HIGHtime_2 = 0;
unsigned long AC_LOWtime_1 = 0;
unsigned long AC_LOWtime_2 = 0;

unsigned long debugtime = 0;
unsigned long temptime = 0;


/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean PUMP_RESET = false;
boolean AC_LOW_RESET = false;
boolean AC_LOW_RESET_1 = false;
boolean AC_LOW_RESET_2 = false;
boolean AC_HIGH_RESET = false;
boolean AC_HIGH_RESET_1 = false;
boolean AC_HIGH_RESET_2 = false;
boolean AC_STOP_RESET = false;
boolean AC_STOP_RESET_1 = false;
boolean AC_STOP_RESET_2 = false;


/*
 *  The AC status in memory 
 *  0 = OFF
 *  1 = LOW NOISE
 *  2 = HIGH NOISE
 */
int AC_STATUS = 0;
boolean AC_TO_BE_UPDATED = false; // Flag activated if IPS OFF

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;

/*
 *  The waiting time for debug purpose
 */
long debug_wait = 2000;

/*
 *  The waiting time before reading temperature
 */
long temp_wait = 500;


/*
 *  I2C BIT ASSIGNATION (MAX = 32) 
 */ 
#define AC_LOW_START_CMD_BIT       0   // LOW NOISE Open bit
#define AC_LOW_STOP_CMD_BIT        1   // LOW NOISE Close bit
#define AC_LOW_ON_STATUS_BIT       2   // AC LOW START NOISE Status bit
#define AC_LOW_OFF_STATUS_BIT      3   // AC LOW STOP NOISE Status bit
#define AC_HIGH_START_CMD_BIT      4   // HIGH NOISE Open bit
#define AC_HIGH_STOP_CMD_BIT       5   // HIGH NOISE Close bit
#define AC_HIGH_ON_STATUS_BIT      6   // AC HIGH START NOISE Status bit
#define AC_HIGH_OFF_STATUS_BIT     7   // AC HIGH STOP NOISE Status bit
#define AC_NETWORK_ON_STATUS_BIT   8   // AC NETWORK ON Status bit
#define PUMP_START_CMD_BIT         9   // PUMP_START Open bit
#define PUMP_STOP_CMD_BIT          10  // PUMP_START Close bit
#define PUMP_STATUS_BIT            11  // PUMP Status bit
#define AC_REARM_CMD_BIT           12  // AC REARM command bit
#define AC_REARM_STATUS_BIT        13  // AC REARM status bit


#define ARD_RESET_BIT              31  // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */ 
// digital pins (OUTPUTS)
#define AC_LOW_START_CMD       CONTROLLINO_R0   // Open Command LOW NOISE
#define AC_LOW_STOP_CMD        CONTROLLINO_R1   // Close Command LOW NOISE
#define AC_LOW_ON_STATUS       CONTROLLINO_A1   // AC LOW START NOISE STATUS
#define AC_LOW_OFF_STATUS      CONTROLLINO_A0   // AC LOW STOP NOISE STATUS --> NOT USED!
#define AC_HIGH_START_CMD      CONTROLLINO_R2   // Open Command HIGH NOISE
#define AC_HIGH_STOP_CMD       CONTROLLINO_R3   // Close Command HIGH NOISE
#define AC_HIGH_ON_STATUS      CONTROLLINO_A3   // AC HIGH START NOISE STATUS
#define AC_HIGH_OFF_STATUS     CONTROLLINO_A2   // AC HIGH STOP NOISE STATUS --> NOT USED!
#define AC_NETWORK_ON_STATUS   CONTROLLINO_A4   // AC NETWORK ON/OFF STATUS
#define PUMP_START_CMD         CONTROLLINO_R4   // Start Command PUMP_START
#define PUMP_STOP_CMD          CONTROLLINO_R5   // Stop Command PUMP_STOP
#define PUMP_STATUS            CONTROLLINO_A9   // PUMP STATUS 

void(*resetArd) (void) = 0; //declare reset function @ address 0

void setup() {

  // Open Serial communication for Console port.
 Serial.begin(9600);
 
 Serial.println("START SETUP");

 // Init IO & I2C
 InitializeIO();
 InitializeI2C();
 
 //Init temp
 temp.fvalue = -1000;

 // Inite Reamr Status to OFF
 I2CsetBit(AC_REARM_STATUS_BIT,0x01);

 // Init Wire
 Wire.begin(I2C_SLAVE_ADDR);
 Wire.setTimeout(1000);
 //Wire.setClock(10000);
 
 Serial.println("END SETUP");
}
 
void loop() {  
   
  if (updateIOFromI2CBool == true) {
    Serial.print("Received command; updated i2c_buffer =");
    Serial.println(i2c_buffer,BIN);
    updateIOFromI2CBool = false;
    // Update I/O from updated i2c_buffer
    UpdateIOFromI2C();
  }
  
  // Update i2c_buffer from I/O
     UpdateI2CFromIO();

  // reset & Check procedure
     ResetAndCheck();
  
  // Reading temperature
  //Serial.print("Internal Temperature="); Serial.println(thermocouple.readInternal());
  // For reading temperature
  if ( (millis() - temptime > temp_wait)) {
     tempbuf = (float) thermocouple.readCelsius();
     Serial.print("tempbuf=");Serial.println(tempbuf);
     if (isnan(tempbuf) ) {  // Setting absurd value instead of nan
        if (++errcnt > MAX_ERRCNT){
           Serial.println("Temperature error --> check sensor");
           tempbuf = -1000;
         }
     }
     else if (((abs(tempbuf - temp.fvalue) < tempDelta) || temp.fvalue == -1000) ) {
        errcnt = 0; // Reset errcnt
     }
     
     // Write temperature value
     temp.fvalue = tempbuf;
     
     Serial.println("-------------------------------");
     Serial.print("Reading temperature=");Serial.println(temp.fvalue);
     Serial.println("-------------------------------");
     temptime = millis();
  }
  // For debug purpose
  if ( millis() - debugtime > debug_wait) {
     Serial.print("Loop: i2c_buffer=");
     Serial.println(i2c_buffer,BIN);
     debugtime = millis();
  }
  delay(10);
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
 
  // Digital OUTPUTS assignation & initialization (set to HIGH NOISE status and AC stop status)
  digitalWrite(AC_HIGH_START_CMD,LOW);                   // Set AC_HIGH_START_CMD LOW
  digitalWrite(AC_HIGH_STOP_CMD,HIGH);                   // Set AC_HIGH_STOP_CMD HIGH
  pinMode(AC_HIGH_START_CMD, OUTPUT);                    // Set the digital pin as output
  pinMode(AC_HIGH_STOP_CMD, OUTPUT);                     // Set the digital pin as output
  I2CsetBit(AC_HIGH_START_CMD_BIT,0x00);                 // Set AC_HIGH_START_CMD_BIT LOW
  I2CsetBit(AC_HIGH_STOP_CMD_BIT,0x01);                  // Set AC_HIGH_STOP_CMD_BIT HIGH
  digitalWrite(AC_LOW_START_CMD,LOW);                    // Set AC_LOW_START_CMD LOW
  digitalWrite(AC_LOW_STOP_CMD,HIGH);                    // Set AC_LOW_STOP_CMD HIGH
  pinMode(AC_LOW_START_CMD, OUTPUT);                     // Set the digital pin as output
  pinMode(AC_LOW_STOP_CMD, OUTPUT);                      // Set the digital pin as output
  I2CsetBit(AC_LOW_START_CMD_BIT,0x00);                  // Set AC_LOW_START_CMD_BIT LOW
  I2CsetBit(AC_LOW_STOP_CMD_BIT,0x01);                   // Set AC_LOW_STOP_CMD_BIT HIGH
  digitalWrite(PUMP_START_CMD,LOW);                      // Set PUMP_START_CMD LOW
  pinMode(PUMP_START_CMD, OUTPUT);                       // Set the digital pin as output
  I2CsetBit(PUMP_START_CMD_BIT,0x00);                    // Set PUMP_START_CMD_BIT LOW
  digitalWrite(PUMP_STOP_CMD,HIGH);                      // Set PUMP_STOP_CMD HIGH
  pinMode(PUMP_STOP_CMD, OUTPUT);                        // Set the digital pin as output
  I2CsetBit(PUMP_STOP_CMD_BIT,0x01);                     // Set PUMP_STOP_CMD_BIT HIGH


  pinMode(AC_LOW_ON_STATUS, INPUT);                     // Set the digital pin as input for AC START STATUS
  pinMode(AC_LOW_OFF_STATUS, INPUT);                    // Set the digital pin as input for AC STOP STATUS
  pinMode(AC_HIGH_ON_STATUS, INPUT);                    // Set the digital pin as input for AC START STATUS
  pinMode(AC_HIGH_OFF_STATUS, INPUT);                   // Set the digital pin as input for AC STOP STATUS
  pinMode(PUMP_STATUS, INPUT);                          // Set the digital pin as input for PUMP STATUS
  pinMode(AC_NETWORK_ON_STATUS, INPUT);                 // Set the digital pin as input for NETWORK STATUS
}

void requestEvent() {

  // For debug purpose
  Serial.print("Request_event: i2c_buffer=");
  Serial.println(i2c_buffer,BIN);
  
  // Send i2c_buffer to master (create 4 bytes array)
  byte i2c_array[12];
  
  i2c_array[0] = (temp.i32value >> 24) & 0xFF;
  i2c_array[1] = (temp.i32value >> 16) & 0xFF;
  i2c_array[2] = (temp.i32value >> 8) & 0xFF;
  i2c_array[3] = temp.i32value & 0xFF;
  
  i2c_array[4] = (i2c_buffer >> 24) & 0xFF;
  i2c_array[5] = (i2c_buffer >> 16) & 0xFF;
  i2c_array[6] = (i2c_buffer >> 8) & 0xFF;
  i2c_array[7] = i2c_buffer & 0xFF;

  uint32_t crc = CRC32::calculate(i2c_array, 8);

  i2c_array[8] = (crc >> 24) & 0xFF;
  i2c_array[9] = (crc >> 16) & 0xFF;
  i2c_array[10] = (crc >> 8) & 0xFF;
  i2c_array[11] = crc & 0xFF;

  /*Serial.print("I2C_BUFFER=");
  for (int i = 0 ; i < 12; i++)
     Serial.println(i2c_array[i]);
  */
  Wire.write(i2c_array, 12);

}
void receiveEvent(int numbyte) {

  //Serial.print("ReceiveEvent: numbyte = ");
  //Serial.println(numbyte);

  // Update i2c_buffer from master (4 bytes)

  if (Wire.available() == 8) { // Expect 8 bytes of data (including CRC32), update i2c_buffer
    
    data_array[0] = Wire.read();
    data_array[1] = Wire.read();
    data_array[2] = Wire.read();
    data_array[3] = Wire.read();
    
    // last 4 bytes of data corresponds to CRC32
    
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
    uint32_t crcdata = CRC32::calculate(data_array, 4);

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
 *  LOW NOISE Case
 */
 // Reset LOW NOISE CMD
 if (AC_LOW_RESET_1 == true) {
    if ( millis() - AC_LOWtime_1 > reset_wait) {
       Serial.println("RESET LOW 1 ");
       digitalWrite(AC_HIGH_STOP_CMD,HIGH);  // RESET AC_HIGH_STOP CMD
       I2CsetBit(AC_HIGH_STOP_CMD_BIT,0x01); // RESET AC_LOW BIT
       AC_LOW_RESET_1 = false;
       digitalWrite(AC_LOW_START_CMD,HIGH);   // SET AC_LOW_START CMD
       I2CsetBit(AC_LOW_START_CMD_BIT,0x01); // SET AC_LOW_START BIT
       AC_LOWtime_2 = millis();
       AC_LOW_RESET_2 = true;
    }
 }
 if (AC_LOW_RESET_2 == true) {
    if ( millis() - AC_LOWtime_2 > reset_wait) {
       Serial.println("RESET LOW 2");
       digitalWrite(AC_LOW_START_CMD,LOW);  // RESET AC_LOW_START CMD
       I2CsetBit(AC_LOW_START_CMD_BIT,0x00); // RESET AC_LOW BIT
       AC_LOW_RESET_2 = false;
       AC_LOW_RESET = false;
       AC_HIGH_RESET = false;
       AC_STOP_RESET = false;
    }
 }
 // Reset AC_HIGH CMD
 if (AC_HIGH_RESET_1 == true) {
    if ( millis() - AC_HIGHtime_1 > reset_wait) {
       Serial.println("RESET STOP 1");
       digitalWrite(AC_LOW_STOP_CMD,HIGH);   // RESET AC_LOW_STOP CMD
       I2CsetBit(AC_LOW_STOP_CMD_BIT,0x01);  // RESET AC_LOW_STOP BIT
       AC_HIGH_RESET_1 = false;
       digitalWrite(AC_HIGH_START_CMD,HIGH);  // SET AC_HIGH_START CMD
       I2CsetBit(AC_HIGH_START_CMD_BIT,0x01); // SET AC_HIGH_START BIT
       AC_HIGHtime_2 = millis();
       AC_HIGH_RESET_2 = true;
    }
 }
 if (AC_HIGH_RESET_2 == true) {
    if ( millis() - AC_HIGHtime_2 > reset_wait) {
       Serial.println("RESET NORMAL 2");
       digitalWrite(AC_HIGH_START_CMD,LOW);    // RESET AC_HIGH_START CMD
       I2CsetBit(AC_HIGH_START_CMD_BIT,0x00);  // RESETSET AC_HIGH_START BIT
       AC_HIGH_RESET_2 = false;
       AC_HIGH_RESET = false;
       AC_LOW_RESET = false; 
       AC_STOP_RESET = false; 
    }
 }
 // Reset AC_STOP CMD
 if (AC_STOP_RESET_1 == true) {
    if ( millis() - AC_STOPtime_1 > reset_wait) {
       Serial.println("RESET STOP 1");
       digitalWrite(AC_LOW_STOP_CMD,HIGH);   // RESET AC_LOW_STOP CMD
       I2CsetBit(AC_LOW_STOP_CMD_BIT,0x01);  // RESET AC_LOW_STOP BIT
       AC_STOP_RESET_1 = false;
       digitalWrite(AC_HIGH_STOP_CMD,LOW);   // SET AC_HIGH_STOP CMD
       I2CsetBit(AC_HIGH_STOP_CMD_BIT,0x00); // SET AC_HIGH_STOP BIT
       AC_STOPtime_2 = millis();
       AC_STOP_RESET_2 = true;
    }
 }
 if (AC_STOP_RESET_2 == true) {
    if ( millis() - AC_STOPtime_2 > reset_wait) {
       Serial.println("RESET STOP 2");
       digitalWrite(AC_HIGH_STOP_CMD,HIGH);   // RESET AC_HIGH_STOP CMD
       I2CsetBit(AC_HIGH_STOP_CMD_BIT,0x01);  // RESET AC_HIGH_STOP BIT
       AC_STOP_RESET_2 = false;
       AC_STOP_RESET = false;
       AC_HIGH_RESET = false; 
       AC_LOW_RESET = false;
    }
 }
  /*
  *  PUMP Case
  */
  // Reset PUMP_STOP_CMD
  if (digitalRead(PUMP_STOP_CMD) == LOW && PUMP_RESET == true) {
    if ( millis() - PUMPtime > reset_wait) {
       digitalWrite(PUMP_STOP_CMD,HIGH);  // RESET PUMP_STOP AC
       I2CsetBit(PUMP_STOP_CMD_BIT,0x01); // RESET PUMP_STOP BIT
       PUMP_RESET = false;  
    }
  }
  // Reset PUMP_START_CMD
  if (digitalRead(PUMP_START_CMD) == HIGH && PUMP_RESET == true) {
    if ( millis() - PUMPtime > reset_wait) {
       digitalWrite(PUMP_START_CMD,LOW);   // RESET PUMP_START AC
       I2CsetBit(PUMP_START_CMD_BIT,0x00); // RESET PUMP_START BIT 
       PUMP_RESET = false;
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
  
  /***********************************************************************************************************/
  /* Update AC HIGH/LOW/STOP position */
  /***********************************************************************************************************/
  // STOP 
  if (bitRead(i2c_buffer,AC_LOW_STOP_CMD_BIT) == 0x00 && bitRead(i2c_buffer,AC_HIGH_STOP_CMD_BIT) == 0x00 && AC_STOP_RESET == false) {
     digitalWrite(AC_LOW_STOP_CMD,LOW);  // STOP AC
     AC_STOPtime_1 = millis();
     AC_STOP_RESET = true;
     AC_HIGH_RESET = true;
     AC_LOW_RESET = true;
     AC_STOP_RESET_1 = true;
  }
  // HIGH
  else if (bitRead(i2c_buffer,AC_LOW_STOP_CMD_BIT) == 0x00 && bitRead(i2c_buffer,AC_HIGH_START_CMD_BIT) == 0x01 && AC_HIGH_RESET == false) { 
     digitalWrite(AC_LOW_STOP_CMD,LOW); // STOP LOW NOISE 
     AC_HIGHtime_1 = millis();
     AC_HIGH_RESET = true;
     AC_STOP_RESET = true;
     AC_LOW_RESET = true;
     AC_HIGH_RESET_1 = true;
  }
  // LOW
  else if (bitRead(i2c_buffer,AC_HIGH_STOP_CMD_BIT) == 0x00 && bitRead(i2c_buffer,AC_LOW_START_CMD_BIT) == 0x01 && AC_LOW_RESET == false) { 
     digitalWrite(AC_HIGH_STOP_CMD,LOW); // STOP HIGH NOISE 
     AC_LOWtime_1 = millis();
     AC_LOW_RESET = true;
     AC_STOP_RESET = true;
     AC_HIGH_RESET = true;
     AC_LOW_RESET_1 = true;
  }

  /***********************************************************************************************************/
  /* Update PUMP START/STOP position */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,PUMP_START_CMD_BIT) == 0x01 && bitRead(i2c_buffer,PUMP_STOP_CMD_BIT) == 0x01 && PUMP_RESET == false) {
     digitalWrite(PUMP_START_CMD,HIGH);     // PUMP_START PUMP
     PUMPtime = millis();
     PUMP_RESET = true; 
  }
  else if (bitRead(i2c_buffer,PUMP_START_CMD_BIT) == 0x00 && bitRead(i2c_buffer,PUMP_STOP_CMD_BIT) == 0x00 && PUMP_RESET == false) { 
     digitalWrite(PUMP_STOP_CMD,LOW); // PUMP_STOP PUMP
     PUMPtime = millis();
     PUMP_RESET = true; 
  }
  /***********************************************************************************************************/
  /* Update boolean REARM STATUS position (1=OFF, 0=ON)*/
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,AC_REARM_CMD_BIT) == 0x01) {
     I2CsetBit(AC_REARM_STATUS_BIT,0x00);   // SET REARM STATUS ON
  }
  else if (bitRead(i2c_buffer,AC_REARM_CMD_BIT) == 0x00) {
     I2CsetBit(AC_REARM_STATUS_BIT,0x01);   // SET REARM STATUS OFF
  }
}

void UpdateI2CFromIO()
{ 

  /***********************************************************************************************************/
  /* Update PUMP position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(PUMP_STATUS) == HIGH) {    // PUMP OFF STATUS
     I2CsetBit(PUMP_STATUS_BIT,0x01);        // UPDATE PUMP BIT
  }
  else if (digitalRead(PUMP_STATUS) == LOW) { // PUMP ON STATUS
     I2CsetBit(PUMP_STATUS_BIT,0x00);         // UPDATE PUMP BIT
  }

   /***********************************************************************************************************/
  /* Update NETWORK ON/OFF STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(AC_NETWORK_ON_STATUS) == LOW) {     // IPS OFF STATUS
     I2CsetBit(AC_NETWORK_ON_STATUS_BIT,0x00);         // UPDATE IPS BIT
     if ( bitRead(i2c_buffer,AC_REARM_STATUS_BIT) == 0x00) AC_TO_BE_UPDATED = true; // IPS is OFF : Set to update old to AC_STATUS
     //Serial.print("Update AC_TO_BE_UPDATED=");Serial.println(AC_TO_BE_UPDATED);
  }
  else if (digitalRead(AC_NETWORK_ON_STATUS) == HIGH) { // IPS ON STATUS
     I2CsetBit(AC_NETWORK_ON_STATUS_BIT,0x01);         // UPDATE IPS BIT
     if (AC_TO_BE_UPDATED == false ) { // Simply memorize current AC status
      //Serial.print("Update AC_STATUS (IPS ON)");Serial.print(" // AC_TO_BE_UPDATED=");Serial.println(AC_TO_BE_UPDATED);
        // AC OFF
        if (digitalRead(AC_HIGH_ON_STATUS) == LOW && digitalRead(AC_LOW_ON_STATUS) == LOW) { // AC OFF STATUS
            I2CsetBit(AC_HIGH_ON_STATUS_BIT,0x00);   // UPDATE AC_HIGH_ON STATUS BIT
            I2CsetBit(AC_LOW_ON_STATUS_BIT,0x00);    // UPDATE AC_LOW_ON STATUS BIT
            AC_STATUS = 0;
        }
        // LOW
        else if (digitalRead(AC_LOW_ON_STATUS) == HIGH && digitalRead(AC_HIGH_ON_STATUS) == LOW ) { // AC LOW NOISE STATUS
            I2CsetBit(AC_LOW_ON_STATUS_BIT,0x01);          // UPDATE AC_LOW_ON STATUS BIT
            AC_STATUS = 1;
        }
        // HIGH
        else if (digitalRead(AC_HIGH_ON_STATUS) == HIGH && digitalRead(AC_LOW_ON_STATUS) == LOW) { // AC HIGH NOISE STATUS
            I2CsetBit(AC_HIGH_ON_STATUS_BIT,0x01);         // UPDATE AC_HIGH_ON STATUS BIT
            AC_STATUS = 2;
        }
     }
     else if (AC_TO_BE_UPDATED == true ) { // REARM AC STATUS to match memorized one
        //Serial.println("Recover AC_STATUS (IPS OFF)");Serial.print(" // AC_TO_BE_UPDATED=");Serial.println(AC_TO_BE_UPDATED);
        if (AC_STATUS == 0) { // Set AC STOP
          digitalWrite(AC_LOW_STOP_CMD,LOW);  // STOP AC
          AC_STOPtime_1 = millis();
          AC_STOP_RESET = true;
          AC_HIGH_RESET = true;
          AC_LOW_RESET = true;
          AC_STOP_RESET_1 = true;
        }
        else if (AC_STATUS == 1) { // Set LOW NOISE
          digitalWrite(AC_HIGH_STOP_CMD,LOW); // STOP HIGH NOISE 
          AC_LOWtime_1 = millis();
          AC_LOW_RESET = true;
          AC_STOP_RESET = true;
          AC_HIGH_RESET = true;
          AC_LOW_RESET_1 = true;
        }
        else if (AC_STATUS == 2) { // Set HIGH NOISE
          digitalWrite(AC_LOW_STOP_CMD,LOW); // STOP LOW NOISE 
          AC_HIGHtime_1 = millis();
          AC_HIGH_RESET = true;
          AC_STOP_RESET = true;
          AC_LOW_RESET = true;
          AC_HIGH_RESET_1 = true;
        }
        // Finally Reset AC_TO_BE_UPDATED
        AC_TO_BE_UPDATED = false;
     }
  }
  
  /***********************************************************************************************************/
  /* Update AC position STATUS bit (HIGH/LOW//STOP) */
  /***********************************************************************************************************/
  // LOW
  if (digitalRead(AC_LOW_ON_STATUS) == HIGH ) { // AC LOW NOISE STATUS
     I2CsetBit(AC_LOW_ON_STATUS_BIT,0x01);      // UPDATE AC_LOW_ON STATUS BIT
    
  }
  // HIGH
  else if (digitalRead(AC_HIGH_ON_STATUS) == HIGH) { // AC HIGH NOISE STATUS
     I2CsetBit(AC_HIGH_ON_STATUS_BIT,0x01);          // UPDATE AC_HIGH_ON STATUS BIT
    
  }
  // AC OFF
  else if (digitalRead(AC_HIGH_ON_STATUS) == LOW && digitalRead(AC_LOW_ON_STATUS) == LOW) { // AC OFF STATUS
     I2CsetBit(AC_HIGH_ON_STATUS_BIT,0x00);   // UPDATE AC_HIGH_ON STATUS BIT
     I2CsetBit(AC_LOW_ON_STATUS_BIT,0x00);    // UPDATE AC_LOW_ON STATUS BIT
  }
  else {
     // ERROR AC STATUS BIT
     I2CsetBit(AC_HIGH_ON_STATUS_BIT,0x01);  
     I2CsetBit(AC_LOW_ON_STATUS_BIT,0x01);   
  }
}
