/*
  Modbus Server
 A Sicem Module with TCMuxshield and Controllino board
 */
#include <Wire.h>        // I2C library
#include <Controllino.h> // Controllino library
#include <CRC32.h>       // Checksum library
#include <SoftwareSerial.h>

// Conversion tool

typedef union {
    float fvalue;
    uint32_t i32value;
  } FloatUint32;
  
/*
 * I2C ADDRESS (SLAVE)
 */ 
#define I2C_SLAVE_ADDR 0x09
/* 
 * I2C BUFFER SIZE
 */
#define I2C_BUFFER 32
/*
 * Variables used in interrupted handlers
 */
volatile uint32_t i2c_buffer = 0; 
volatile byte data_array[8]; // contains i2c_buffer
volatile byte crc_array[4];
volatile boolean updateIOFromI2CBool = false;
volatile FloatUint32 sensorTemp[9]; // The TCMuxShield temperature array (8 sensor tempertures + internal temperature)

//#define RS232Serial  Serial2 // Serial2 is hardware serial for Controllino MAXI (RXD2, TXD2)
SoftwareSerial RS232Serial(CONTROLLINO_D10, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_14); // RX, TX for Controllino (Raspberry Com)

/*
 * The time for Check and Reset actions
 */
unsigned long CP900time = 0;
unsigned long CP800time = 0;
unsigned long CHILLERtime = 0;
unsigned long WATERLOOPtime = 0;
unsigned long PUMPtime = 0;

unsigned long looptime = 0;

/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean CP900_RESET = false;
boolean CP800_RESET = false;
boolean CHILLER_RESET = false;
boolean WATERLOOP_RESET = false;
boolean PUMP_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 5000;

/*
*  These CHECK are used to check switches status (Status inputs)
*/
boolean CP900_CHECK = false;
boolean CP800_CHECK = false;
boolean CHILLER_CHECK = false;
boolean WATERLOOP_CHECK = false;



/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;

// relay pins
#define CP900_ON_CMD    CONTROLLINO_R0    // CP900 SWITCH ON COMMAND
#define CP900_OFF_CMD   CONTROLLINO_R1    // CP900 SWITCH OFF COMMAND
#define CP800_ON_CMD    CONTROLLINO_R2    // CP800 SWITCH ON COMMAND
#define CP800_OFF_CMD   CONTROLLINO_R3    // CP800 SWITCH OFF COMMAND
#define CHILLER_ON_CMD  CONTROLLINO_R4    // CHILLER SWITCH ON COMMAND
#define CHILLER_OFF_CMD CONTROLLINO_R5    // CHILLER SWITCH OFF COMMAND
#define PUMP_ON_CMD     CONTROLLINO_R6    // PUMP SWITCH ON COMMAND
#define PUMP_OFF_CMD    CONTROLLINO_R7    // PUMP SWITCH OFF COMMAND


// digital pins (INPUTS)

#define LEVEL_STATUS   CONTROLLINO_A0     // Level water STATUS
#define FLUX_STATUS    CONTROLLINO_A1     // Flux water STATUS
#define CP900_STATUS   CONTROLLINO_A2     // CP900 STATUS
#define CP800_STATUS   CONTROLLINO_A3     // CP800 STATUS
#define CHILLER_STATUS CONTROLLINO_A4     // CHILLER STATUS
#define PUMP_STATUS    CONTROLLINO_A5     // PUMP STATUS


// TCMuxShield local pin definition

#define PINEN 8   //Mux Enable pin
#define PINA0 9   //Mux Address 0 pin
#define PINA1 10  //Mux Address 1 pin
#define PINA2 11  //Mux Address 2 pin
#define PINSO 13  //TCAmp Slave Out pin (MISO)
#define PINSC 42  //TCAmp Serial Clock (SCK)
#define PINCS 43  //TCAmp Chip Select Change this to match the position of the Chip Select Link

/*
 *  I2C BIT ASSIGNATION (I2C_BUFFER; MAX = 256) 
 */
#define LEVEL_STATUS_BIT       0                         // LEVEL water STATUS bit
#define FLUX_STATUS_BIT        1                         // LEVEL water STATUS bit
#define CP900_STATUS_BIT       2                         // CP900 Status bit
#define CP800_STATUS_BIT       3                         // CP800 Status bit
#define CHILLER_STATUS_BIT     4                         // CHILLER Status bit
#define PUMP_STATUS_BIT        5                         // PUMP Status bit
#define CP900_ON_CMD_BIT       6                         // CP900 On CMD bit
#define CP900_OFF_CMD_BIT      7                         // CP900 Off CMD bit
#define CP800_ON_CMD_BIT       8                         // CP800 On CMD bit
#define CP800_OFF_CMD_BIT      9                         // CP800 Off CMD bit
#define CHILLER_ON_CMD_BIT     10                         // CHILLER On CMD bit
#define CHILLER_OFF_CMD_BIT    11                        // CHILLER Off CMD bit
#define PUMP_ON_CMD_BIT        12                        // PUMP On CMD bit
#define PUMP_OFF_CMD_BIT       13                        // PUMP Off CMD bit
#define MUXSHIELD_COMERR_BIT   14                        // MUXSHIELD COMM ERR STATUS
#define ARD_RESET_BIT          31                        // Controllino Reset Bit

void(*resetArd) (void) = 0; //declare reset function @ address 0

// Buffer for Serial purposes
size_t length = 64;
char *buffer = (char*)malloc(sizeof(char) * length); 

// TCMuxShieldMux variables
int rawTemp[8], sensorFail[8];
char failMode[8];
int internalTemp;
unsigned int mask;
//char data[16];
int numSensors = 8, updateDelay = 500;    
unsigned long TCMuxtime;
unsigned int cnt;
// Normalization offset & Temperature 
#define tempOffset 3
#define tempDelta 3


void setup() {

    // Open Serial communication for Console port.
   Serial.begin(9600);

   Serial.println("START SETUP");

   // Open serial communication for Raspberry
   RS232Serial.begin(9600);
   RS232Serial.setTimeout(1000);
  
   InitializeIO();
   InitializeI2C();

   // Configure TCMuxShield pins and init variables
  TCMuxtime = millis();
  cnt = -1;   
  pinMode(PINEN, OUTPUT);     
  pinMode(PINA0, OUTPUT);    
  pinMode(PINA1, OUTPUT);    
  pinMode(PINA2, OUTPUT);    
  pinMode(PINSO, INPUT);    
  pinMode(PINCS, OUTPUT);    
  pinMode(PINSC, OUTPUT);    

  // Init TCMuxShield pins
  
  digitalWrite(PINEN, HIGH);   // enable on
  digitalWrite(PINA0, LOW); 
  digitalWrite(PINA1, LOW); 
  digitalWrite(PINA2, LOW); 
  digitalWrite(PINSC, LOW); //put clock in low

  //Initialize TCMuxShield Temperature array values
  
  for (int i = 0; i < 9 ; i++) 
    sensorTemp[i].fvalue = -99;

  // Init Wire
  Wire.begin(I2C_SLAVE_ADDR);
  Wire.setTimeout(1000);


}
void InitializeI2C()
{
  Serial.println("InitializeI2C...");
  
  Wire.onReceive(receiveEvent); // register receive event (i2c_buffer sent by master)  
  Wire.onRequest(requestEvent); // register request event (i2c_buffer sent by slave)  
  
  Serial.println("Done.");
}

void InitializeIO()
{    
  Serial.println("InitializeIO...");
  
  // Digital OUTPUTS assignation & initialization
  digitalWrite(CP900_ON_CMD,LOW);        // Set CP900_ON_CMD LOW
  digitalWrite(CP900_OFF_CMD,HIGH);      // Set CP900_OFF_CMD HIGH
  pinMode(CP900_ON_CMD, OUTPUT);         // Set the digital pin as output for CP900 ON
  pinMode(CP900_OFF_CMD, OUTPUT);        // Set the digital pin as output for CP900 OFF
  I2CsetBit(CP900_ON_CMD_BIT,0x00);      // Set CP900_ON_CMD_BIT LOW
  I2CsetBit(CP900_OFF_CMD_BIT,0x01);     // Set CP900_OFF_CMD_BIT HIGH
  digitalWrite(CP800_ON_CMD,LOW);        // Set CP800_ON_CMD LOW
  digitalWrite(CP800_OFF_CMD,HIGH);      // Set CP800_OFF_CMD HIGH
  pinMode(CP800_ON_CMD, OUTPUT);         // Set the digital pin as output for CP800 ON
  pinMode(CP800_OFF_CMD, OUTPUT);        // Set the digital pin as output for CP800 OFF
  I2CsetBit(CP800_ON_CMD_BIT,0x00);      // Set CP800_ON_CMD_BIT LOW
  I2CsetBit(CP800_OFF_CMD_BIT,0x01);     // Set CP800_OFF_CMD_BIT HIGH
  digitalWrite(CHILLER_ON_CMD,LOW);      // Set CHILLER_ON_CMD LOW
  digitalWrite(CHILLER_OFF_CMD,HIGH);    // Set CHILLER_OFF_CMD HIGH
  pinMode(CHILLER_ON_CMD, OUTPUT);       // Set the digital pin as output for CHILLER ON
  pinMode(CHILLER_OFF_CMD, OUTPUT);      // Set the digital pin as output for CHILLER OFF
  I2CsetBit(CHILLER_ON_CMD_BIT,0x00);    // Set CHILLER_ON_CMD_BIT LOW
  I2CsetBit(CHILLER_OFF_CMD_BIT,0x01);   // Set CHILLER_OFF_CMD_BIT HIGH
  digitalWrite(PUMP_ON_CMD,LOW);         // Set PUMP_ON_CMD LOW
  digitalWrite(PUMP_OFF_CMD,HIGH);       // Set PUMP_OFF_CMD HIGH
  pinMode(PUMP_ON_CMD, OUTPUT);          // Set the digital pin as output for PUMP ON
  pinMode(PUMP_OFF_CMD, OUTPUT);         // Set the digital pin as output for PUMP OFF
  I2CsetBit(PUMP_ON_CMD_BIT,0x00);       // Set PUMP_ON_CMD_BIT LOW
  I2CsetBit(PUMP_OFF_CMD_BIT,0x01);      // Set PUMP_OFF_CMD_BIT HIGH
 
  Serial.print("START  with i2c_buffer=");
  Serial.println(i2c_buffer,BIN);
  
  // Digital INPUTS assignation
  pinMode(CP900_STATUS, INPUT);    // sets the digital pin as input for CP900 STATUS
  pinMode(CP800_STATUS, INPUT);    // sets the digital pin as input for CP800 STATUS
  pinMode(CHILLER_STATUS, INPUT);  // sets the digital pin as input for CHILLER STATUS
  pinMode(LEVEL_STATUS, INPUT);    // sets the digital pin as input for LEVEL water STATUS
  pinMode(FLUX_STATUS, INPUT);     // sets the digital pin as input for FLUX water STATUS
  pinMode(PUMP_STATUS, INPUT);     // sets the digital pin as input for PUMP STATUS

  Serial.println("Done.");
}

void ListenToRaspberry() {
      Serial.println("ListenToRaspberry..");
      RS232Serial.listen();
      String request = RS232Serial.readString();
      Serial.println(request);
      if (request.substring(0,7).compareTo("getdata") == 0) {
         Serial.print("GETDATA received: Sending data Sicem...");
         // Sicem Temp
         String data = "";
         // TCMux Temp
         for ( int i = 1; i <= 9 ; i++) {
            data+=String(sensorTemp[i-1].fvalue,2);
            data+=";";
         }
         // Write data to the wire
         Serial.println(data);
         RS232Serial.print(data);
      }
}

void UpdateI2C() {

  if (updateIOFromI2CBool == true) {
    Serial.print("Received command; updated i2c_buffer =");
    Serial.println(i2c_buffer,BIN);
    updateIOFromI2CBool = false;
    // Update I/O from updated i2c_buffer
    UpdateIOFromI2C();
  }
  
  // Update i2c_buffer from I/O
  UpdateI2CFromIO();
  
  ResetAndCheck();
  
}
void loop() {
  
  //Serial.println("loop started");
  // For debug purpose
  if ( millis() - looptime > reset_wait) {
     Serial.print("i2c_buffer=");
     Serial.println(i2c_buffer,BIN);
     looptime = millis();
  }

  /***********************************************************************************************************/
  // Get TCMuxShield sensor Temperature array
  /***********************************************************************************************************/
  getTCMuxShieldTemp();

  /***********************************************************************************************************/
  // Read incoming message from Raspberry
  /***********************************************************************************************************/
  ListenToRaspberry();

  /***********************************************************************************************************/
  // Update I2C buffer & IO
  /***********************************************************************************************************/
  UpdateI2C();

     
  //Serial.println("loop finished");
}

void requestEvent() {
  
  // Send i2c_buffer to master (create 8 bytes array, including checksum)
  byte i2c_array[8];
  
  i2c_array[0] = (i2c_buffer >> 24) & 0xFF;
  i2c_array[1] = (i2c_buffer >> 16) & 0xFF;
  i2c_array[2] = (i2c_buffer >> 8) & 0xFF;
  i2c_array[3] = i2c_buffer & 0xFF;
     
  // Calculate Checksum
  uint32_t crc = CRC32::calculate(i2c_array, 4);

  i2c_array[4] = (crc >> 24) & 0xFF;
  i2c_array[5] = (crc >> 16) & 0xFF;
  i2c_array[6] = (crc >> 8) & 0xFF;
  i2c_array[7] = crc & 0xFF;
  
  Wire.write(i2c_array, 8);

}
void receiveEvent(int numbyte) {

  // Update i2c_buffer from master (13 bytes)
  while (Wire.available()) {
    if (numbyte == 8) { // Expect 8 bytes of data (including CRC32), update i2c_buffer
      // i2c_buffer
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

      //Serial.print(" ReceiveEvent:Checksum:");Serial.print(crc);Serial.print(" <--> ");Serial.println(crcdata);
      //Serial.print(" ReceiveEvent:pos:");Serial.println((int)data_array[4]);
      if ( crc == crcdata ) {
          Serial.print(" ReceiveEvent:Checksum good:");Serial.println(crc);
        // Update i2c_buffer in the interrupted section
        i2c_buffer = data_array[0];
        i2c_buffer = (i2c_buffer << 8) | data_array[1];
        i2c_buffer = (i2c_buffer << 8) | data_array[2];
        i2c_buffer = (i2c_buffer << 8) | data_array[3];
   
        // UpdateIOFromI2C in the loop
        updateIOFromI2CBool = true;   
      }
    }
    else {// Flush the wrong Wire buffer
      while(Wire.available()) Wire.read();
    }
  }
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
   *  CP900 Switch ON/OFF Case
   */
  // Reset CP900_OFF_CMD
  if (digitalRead(CP900_OFF_CMD) == LOW && CP900_RESET == true) {
    if ( millis() - CP900time > reset_wait) {
       digitalWrite(CP900_OFF_CMD,HIGH);  // RESET CP900 OFF CMD
       I2CsetBit(CP900_OFF_CMD_BIT,0x01); // RESET CP900 OFF CMD BIT
       CP900_RESET = false;  
    }
  }
  // Reset CP900_ON_CMD
  if (digitalRead(CP900_ON_CMD) == HIGH && CP900_RESET == true) {
    if ( millis() - CP900time > reset_wait) {
       digitalWrite(CP900_ON_CMD,LOW);   // RESET CP900 ON CMD
       I2CsetBit(CP900_ON_CMD_BIT,0x00); // RESET CP900 ON CMD BIT
       CP900_RESET = false;
    }
  }
  /*
   *  CP800 Switch ON/OFF Case
   */
  // Reset CP800_OFF_CMD
  if (digitalRead(CP800_OFF_CMD) == LOW && CP800_RESET == true) {
    if ( millis() - CP800time > reset_wait) {
       digitalWrite(CP800_OFF_CMD,HIGH);  // RESET CP800 OFF CMD
       I2CsetBit(CP800_OFF_CMD_BIT,0x01); // RESET CP800 OFF CMD BIT
       CP800_RESET = false;  
    }
  }
  // Reset CP800_ON_CMD
  if (digitalRead(CP800_ON_CMD) == HIGH && CP800_RESET == true) {
    if ( millis() - CP800time > reset_wait) {
       digitalWrite(CP800_ON_CMD,LOW);   // RESET CP800 ON CMD
       I2CsetBit(CP800_ON_CMD_BIT,0x00); // RESET CP800 ON CMD BIT
       CP800_RESET = false;
    }
  }
  /*
   *  CHILLER Switch ON/OFF Case
   */
  // Reset CHILLER_OFF_CMD
  if (digitalRead(CHILLER_OFF_CMD) == LOW && CHILLER_RESET == true) {
    if ( millis() - CHILLERtime > reset_wait) {
       digitalWrite(CHILLER_OFF_CMD,HIGH);  // RESET CHILLER OFF CMD
       I2CsetBit(CHILLER_OFF_CMD_BIT,0x01); // RESET CHILLER OFF CMD BIT
       CHILLER_RESET = false;  
    }
  }
  // Reset CHILLER_ON_CMD
  if (digitalRead(CHILLER_ON_CMD) == HIGH && CHILLER_RESET == true) {
    if ( millis() - CHILLERtime > reset_wait) {
       digitalWrite(CHILLER_ON_CMD,LOW);   // RESET CHILLER ON CMD
       I2CsetBit(CHILLER_ON_CMD_BIT,0x00); // RESET CHILLER ON CMD BIT
       CHILLER_RESET = false;
    }
  }
  /*
   *  PUMP Switch ON/OFF Case
   */
  // Reset PUMP_OFF_CMD
  if (digitalRead(PUMP_OFF_CMD) == LOW && PUMP_RESET == true) {
    if ( millis() - PUMPtime > reset_wait) {
       digitalWrite(PUMP_OFF_CMD,HIGH);  // RESET PUMP OFF CMD
       I2CsetBit(PUMP_OFF_CMD_BIT,0x01); // RESET PUMP OFF CMD BIT
       PUMP_RESET = false;  
    }
  }
  // Reset PUMP_ON_CMD
  if (digitalRead(PUMP_ON_CMD) == HIGH && PUMP_RESET == true) {
    if ( millis() - PUMPtime > reset_wait) {
       digitalWrite(PUMP_ON_CMD,LOW);   // RESET PUMP ON CMD
       I2CsetBit(PUMP_ON_CMD_BIT,0x00); // RESET PUMP ON CMD BIT
       PUMP_RESET = false;
    }
  }
  /***********************************************************************************************************/
  // Check Controllino Reset Status
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,ARD_RESET_BIT) == 0x01)
    resetArd();
  /***********************************************************************************************************/
}

void UpdateIOFromI2C()
{    
  Serial.println("UPDATE IO from I2C!!");
  /***********************************************************************************************************/
  /* Update CP900 position (On/Off) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,CP900_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,CP900_ON_CMD_BIT) == 0x00 && CP900_RESET == false) {
     digitalWrite(CP900_OFF_CMD,LOW);   // SWITCH OFF CP900
     CP900time = millis();
     CP900_RESET = true;
  }
  else if (bitRead(i2c_buffer,CP900_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,CP900_OFF_CMD_BIT) == 0x01 && CP900_RESET == false) {
     digitalWrite(CP900_ON_CMD,HIGH);   // SWITCH ON CP900
     CP900time = millis();
     CP900_RESET = true;
  }

  /***********************************************************************************************************/
  /* Update CP800 position (On/Off) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,CP800_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,CP800_ON_CMD_BIT) == 0x00 && CP800_RESET == false) {
     digitalWrite(CP800_OFF_CMD,LOW);   // SWITCH OFF CP800
     CP800time = millis();
     CP800_RESET = true;
  }
  else if (bitRead(i2c_buffer,CP800_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,CP800_OFF_CMD_BIT) == 0x01 && CP800_RESET == false) {
     digitalWrite(CP800_ON_CMD,HIGH);   // SWITCH ON CP800
     CP800time = millis();
     CP800_RESET = true;
  }

  /***********************************************************************************************************/
  /* Update CHILLER position (On/Off) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,CHILLER_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,CHILLER_ON_CMD_BIT) == 0x00 && CHILLER_RESET == false) {
     digitalWrite(CHILLER_OFF_CMD,LOW);   // SWITCH OFF CHILLER
     CHILLERtime = millis();
     CHILLER_RESET = true;
  }
  else if (bitRead(i2c_buffer,CHILLER_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,CHILLER_OFF_CMD_BIT) == 0x01 && CHILLER_RESET == false) {
     Serial.println("ACTION CHILLER ON!!!!");
     digitalWrite(CHILLER_ON_CMD,HIGH);   // SWITCH ON CHILLER
     CHILLERtime = millis();
     CHILLER_RESET = true;
  }
  
  /***********************************************************************************************************/
  /* Update PUMP position (On/Off) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,PUMP_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,PUMP_ON_CMD_BIT) == 0x00 && PUMP_RESET == false) {
     digitalWrite(PUMP_OFF_CMD,LOW);   // SWITCH OFF PUMP
     PUMPtime = millis();
     PUMP_RESET = true;
  }
  else if (bitRead(i2c_buffer,PUMP_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,PUMP_OFF_CMD_BIT) == 0x01 && PUMP_RESET == false) {
     digitalWrite(PUMP_ON_CMD,HIGH);   // SWITCH ON PUMP
     PUMPtime = millis();
     PUMP_RESET = true;
  }

}

void UpdateI2CFromIO()
{ 
  /***********************************************************************************************************/
  /* Update CP900 position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(CP900_STATUS) == HIGH ) { // CP900 ON STATUS
     I2CsetBit(CP900_STATUS_BIT,0x01);   // UPDATE CP900 ON  BIT
  }
  else if (digitalRead(CP900_STATUS) == LOW) { // CP900 OFF STATUS
     I2CsetBit(CP900_STATUS_BIT,0x00);  // UPDATE CP900 OFF BIT
  }
  /***********************************************************************************************************/
  /* Update CP800 position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(CP800_STATUS) == HIGH ) { // CP800 ON STATUS
     I2CsetBit(CP800_STATUS_BIT,0x01);   // UPDATE CP800 ON  BIT
  }
  else if (digitalRead(CP800_STATUS) == LOW) { // CP800 OFF STATUS
     I2CsetBit(CP800_STATUS_BIT,0x00);   // UPDATE CP800 ON BIT
  }
  /***********************************************************************************************************/
  /* Update CHILLER position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(CHILLER_STATUS) == HIGH ) { // CHILLER ON STATUS
     I2CsetBit(CHILLER_STATUS_BIT,0x01);   // UPDATE CHILLER ON  BIT
  }
  else if (digitalRead(CHILLER_STATUS) == LOW) { // CHILLER OFF STATUS
     I2CsetBit(CHILLER_STATUS_BIT,0x00);   // UPDATE CHILLER ON BIT
  }
  /***********************************************************************************************************/
  /* Update PUMP position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(PUMP_STATUS) == HIGH ) { // PUMP ON STATUS
     I2CsetBit(PUMP_STATUS_BIT,0x01);   // UPDATE PUMP ON  BIT
  }
  else if (digitalRead(PUMP_STATUS) == LOW) { // PUMP OFF STATUS
     I2CsetBit(PUMP_STATUS_BIT,0x00);   // UPDATE PUMP ON BIT
  }
  /***********************************************************************************************************/
  /* Update LEVEL position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(LEVEL_STATUS) == HIGH ) { // LEVEL ON STATUS
     I2CsetBit(LEVEL_STATUS_BIT,0x01);   // UPDATE LEVEL ON  BIT
  }
  else if (digitalRead(LEVEL_STATUS) == LOW) { // LEVEL OFF STATUS
     I2CsetBit(LEVEL_STATUS_BIT,0x00);   // UPDATE LEVEL ON BIT
  }
  /***********************************************************************************************************/
  /* Update FLUX position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(FLUX_STATUS) == HIGH ) { // FLUX ON STATUS
     I2CsetBit(FLUX_STATUS_BIT,0x01);   // UPDATE FLUX ON  BIT
  }
  else if (digitalRead(FLUX_STATUS) == LOW) { // FLUX OFF STATUS
     I2CsetBit(FLUX_STATUS_BIT,0x00);   // UPDATE FLUX ON BIT
  }
}
void getTCMuxShieldTemp() 
{
  if (millis() > (TCMuxtime + ((unsigned int)updateDelay)) || cnt == -1)
  {
    cnt = 0;
    TCMuxtime = millis();
    for(int j = 0 ; j < numSensors ; j++)
    {
      switch (j) //select channel
      {
        case 0:
          digitalWrite(PINA0, LOW); 
          digitalWrite(PINA1, LOW); 
          digitalWrite(PINA2, LOW);
        break;
        case 1:
          digitalWrite(PINA0, HIGH); 
          digitalWrite(PINA1, LOW); 
          digitalWrite(PINA2, LOW);
        break;
        case 2:
          digitalWrite(PINA0, LOW); 
          digitalWrite(PINA1, HIGH); 
          digitalWrite(PINA2, LOW);
        break;
        case 3:
          digitalWrite(PINA0, HIGH); 
          digitalWrite(PINA1, HIGH); 
          digitalWrite(PINA2, LOW);
        break;
        case 4:
          digitalWrite(PINA0, LOW); 
          digitalWrite(PINA1, LOW); 
          digitalWrite(PINA2, HIGH);
        break;
        case 5:
          digitalWrite(PINA0, HIGH); 
          digitalWrite(PINA1, LOW); 
          digitalWrite(PINA2, HIGH);
        break;
        case 6:
          digitalWrite(PINA0, LOW); 
          digitalWrite(PINA1, HIGH); 
          digitalWrite(PINA2, HIGH);
        break;
        case 7:
          digitalWrite(PINA0, HIGH); 
          digitalWrite(PINA1, HIGH); 
          digitalWrite(PINA2, HIGH);
        break;
      }
      
      delay(10);
      digitalWrite(PINCS, LOW); //stop conversion
      delay(10);
      digitalWrite(PINCS, HIGH); //begin conversion
      delay(150);  //wait 100 ms for conversion to complete
      digitalWrite(PINCS, LOW); //stop conversion, start serial interface
      delay(10);
      
      rawTemp[j] = 0;
      failMode[j] = 0;
      sensorFail[j] = 0;
      internalTemp = 0;

      for (int i = 31 ; i >= 0 ; i--)
      {
          digitalWrite(PINSC, HIGH);
          delay(1);
          
           //print out bits
         #ifdef SHOWMEYOURBITS
         if (digitalRead(PINSO)==1)
          {
            Serial.print("1");
          }
          else
          {
            Serial.print("0");
          }
          #endif
          
        if ((i<=31) && (i>=18))
        {
          // these 14 bits are the thermocouple sensorTemperature data
          // bit 31 sign
          // bit 30 MSB = 2^10
          // bit 18 LSB = 2^-2 (0.25 degC)
          
          mask = 1<<(i-18);
          if (digitalRead(PINSO)==1)
          {
            if (i == 31)
            {
              rawTemp[j] += (0b11<<14);//pad the sensorTemp with the bit 31 value so we can read negative values correctly
            }
            rawTemp[j] += mask;
            //Serial.print("1");
          }
          else
          {
           // Serial.print("0");
          }
        }
        //bit 17 is reserved
        //bit 16 is sensor fault
        if (i==16)
        {
          sensorFail[j] = digitalRead(PINSO);
        }
        
        if ((i<=15) && (i>=4))
        {
          //these 12 bits are the internal sensorTemp of the chip
          //bit 15 sign
          //bit 14 MSB = 2^6
          //bit 4 LSB = 2^-4 (0.0625 degC)
          mask = 1<<(i-4);
          if (digitalRead(PINSO)==1)
          {
            if (i == 15)
            {
              internalTemp += (0b1111<<12);//pad the sensorTemp with the bit 31 value so we can read negative values correctly
            }
            
            internalTemp += mask;//should probably pad the sensorTemp with the bit 15 value so we can read negative values correctly
            //Serial.print("1");
          }
          else
          {
           // Serial.print("0");
          }
          
        }
        //bit 3 is reserved
        if (i==2)
        {
          failMode[j] += digitalRead(PINSO)<<2;//bit 2 is set if shorted to VCC
        }
        if (i==1)
        {
          failMode[j] += digitalRead(PINSO)<<1;//bit 1 is set if shorted to GND
        }
        if (i==0)
        {
          failMode[j] += digitalRead(PINSO)<<0;//bit 0 is set if open circuit
        }
        
        
        digitalWrite(PINSC, LOW);
        delay(1);
      }
      //Serial.println();
      //Serial.println(rawTemp[j],BIN);
      //Serial.print("#");
      //Serial.print(j+1,DEC);
      //Serial.print(": ");
      if (sensorFail[j] == 1)
      {
        //Serial.print("FAIL");
        if ((failMode[j] & 0b0100) == 0b0100)
        {
          //Serial.println(" SHORT TO VCC");
        }
        if ((failMode[j] & 0b0010) == 0b0010)
        {
          //Serial.println(" SHORT TO GND");
        }
        if ((failMode[j] & 0b0001) == 0b0001)
        {
          //Serial.println(" OPEN CIRCUIT");
        }
      }
      else
      {
        double newTemp = (float)rawTemp[j] * 0.25;
        newTemp-=tempOffset;
        if ( (abs(newTemp - sensorTemp[j].fvalue) > tempDelta) || sensorTemp[j].fvalue == -99)
           // Update sensorTemperature value
           sensorTemp[j].fvalue = newTemp;
        Serial.print(sensorTemp[j].fvalue,2);
        Serial.println(" degC");
        Serial.println("");
      }
      //Pause between 2 measures
      delay(10);
    }
    //end reading sensors

    // Treating internal Temperature
    Serial.print("#9: (Int) ");
    double newTemp = (float)internalTemp * 0.0625;
    newTemp-=tempOffset;
    if ( (abs(newTemp - sensorTemp[8].fvalue) > tempDelta) || sensorTemp[8].fvalue == -99)
       // Update internal Temperature value
       sensorTemp[8].fvalue = newTemp;
    Serial.print(sensorTemp[8].fvalue,2);
    Serial.println(" degC");
    Serial.println("");
    I2CsetBit(MUXSHIELD_COMERR_BIT,0x00); // UPDATE SICEM COMM ERROR BIT
  }
}
