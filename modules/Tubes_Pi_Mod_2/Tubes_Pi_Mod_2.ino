/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include <Wire.h> // I2C library
#include <Controllino.h> // Controllino library
#include <CRC32.h>

/*
 * I2C ADDRESS (SLAVE)
 */ 
#define I2C_ADDR 0x09
/* 
 * I2C BUFFER SIZE
 */
#define I2C_BUFFER 32

/*
 * Variables used in interrupted handlers
 */
volatile uint32_t i2c_buffer = 0; 
volatile byte data_array[4];
volatile byte crc_array[4];
volatile boolean updateIOFromI2CBool = false;

/*
 * The time for Check and Reset actions
 */
unsigned long VENTtime = 0;
unsigned long VENTSOFTtime = 0;
unsigned long VSPAREtime = 0;
unsigned long VPtime = 0;
unsigned long BYPASStime = 0;
unsigned long looptime = 0;


/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean VENT_RESET = false;
boolean VENTSOFT_RESET = false;
boolean VSPARE_RESET = false;
boolean VP_RESET = false;
boolean BYPASS_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;

 /*
 *  These CHECK are used to check switches status (Status inputs)
 */
boolean VENT_CHECK = false;
boolean VENTSOFT_CHECK = false;
boolean VSPARE_CHECK = false;
boolean VP_CHECK = false;
boolean BYPASS_CHECK = false;

/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;
 
/*
 *  I2C BIT ASSIGNATION (MAX = 32) 
 */
#define VENT_OPEN_CMD_BIT          0   // VENT Open bit
#define VENT_CLOSE_CMD_BIT         1   // VENT Close bit
#define VENT_OPEN_STATUS_BIT       2   // VENT Open Status bit
#define VENT_CLOSE_STATUS_BIT      3   // VENT Close Status bit
#define VENTSOFT_OPEN_CMD_BIT      4   // VENTSOFT Open bit
#define VENTSOFT_CLOSE_CMD_BIT     5   // VENTSOFT Close bit
#define VP_OPEN_CMD_BIT            6   // VP Open bit
#define VP_CLOSE_CMD_BIT           7   // VP Close bit
#define VP_OPEN_STATUS_BIT         8   // VP Open Status bit
#define VP_CLOSE_STATUS_BIT        9   // VP Close Status bit
#define BYPASS_ON_CMD_BIT          10  // BYPASS On bit
#define BYPASS_OFF_CMD_BIT         11  // BYPASS Off bit
#define BYPASS_ON_STATUS_BIT       12  // BYPASS ON Status bit
#define BYPASS_OFF_STATUS_BIT      13  // BYPASS OFF Status bit
#define VSPARE_OPEN_CMD_BIT          14  // VSPARE On bit
#define VSPARE_CLOSE_CMD_BIT         15  // VSPARE Off bit
#define VSPARE_STATUS_BIT          16  // VSPARE Status bit
#define Ve1_OPEN_STATUS_BIT        17  // Ve1 Open Status bit
#define Ve1_CLOSE_STATUS_BIT       18  // Ve1 Close Status bit
#define Ve2_OPEN_STATUS_BIT        19  // Ve2 Open Status bit
#define Ve2_CLOSE_STATUS_BIT       20  // Ve2 Close Status bit
#define COMPRESSAIR_STATUS_BIT     21  // COMPRESSAIR Status bit

#define ARD_RESET_BIT              31  // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */ 
// digital pins (OUTPUTS)
#define VP_OPEN_CMD        CONTROLLINO_R0   //(Open Command VP)
#define VP_CLOSE_CMD       CONTROLLINO_R1   //(Close Command VP)
#define VENTSOFT_OPEN_CMD  CONTROLLINO_R2   //(Open Command VENT SOFT)
#define VENTSOFT_CLOSE_CMD CONTROLLINO_R3   //(Close Command VENT SOFT)
#define VENT_OPEN_CMD      CONTROLLINO_R4   //(Open Command VENT)
#define VENT_CLOSE_CMD     CONTROLLINO_R5   //(Close Command VENT)
#define VSPARE_OPEN_CMD    CONTROLLINO_R6   //(Command Switch ON VSPARE)
#define VSPARE_CLOSE_CMD   CONTROLLINO_R7   //(Command Switch OFF VSPARE)
#define BYPASS_ON_CMD      CONTROLLINO_R8   //(Command Switch ON BYPASS)
#define BYPASS_OFF_CMD     CONTROLLINO_R9   //(Command Switch OFF BYPASS)


// digital pins (INTPUTS)
#define COMPRESSAIR_STATUS   CONTROLLINO_A0    // COMPRESSAIR STATUS
#define BYPASS_ON_STATUS     CONTROLLINO_A1    // BYPASS STATUS
#define BYPASS_OFF_STATUS    CONTROLLINO_A2    // BYPASS STATUS
#define Ve2_OPEN_STATUS      CONTROLLINO_A3    // Ve2 OPEN STATUS
#define Ve2_CLOSE_STATUS     CONTROLLINO_A4    // Ve2 CLOSE STATUS
#define Ve1_OPEN_STATUS      CONTROLLINO_A5    // Ve1 OPEN STATUS
#define Ve1_CLOSE_STATUS     CONTROLLINO_A6    // Ve1 CLOSE STATUS
#define VP_OPEN_STATUS       CONTROLLINO_A7    // VP OPEN STATUS
#define VP_CLOSE_STATUS      CONTROLLINO_A8    // VP CLOSE STATUS
#define VSPARE_STATUS        CONTROLLINO_A9    // VSPARE CLOSE/OPEN STATUS
#define VENT_OPEN_STATUS     CONTROLLINO_IN0   // VENT OPEN STATUS
#define VENT_CLOSE_STATUS    CONTROLLINO_IN1   // VENT CLOSE STATUS

void(*resetArd) (void) = 0; //declare reset function @ address 0

void setup() {

 InitializeIO();
 InitializeI2C();

 // Open Serial communication for Console port.
 Serial.begin(9600);
}
 
void loop() {  
  delay(100);
  if ( millis() - looptime > reset_wait) {
     Serial.print("i2c_buffer=");
     Serial.println(i2c_buffer,BIN);
     looptime = millis();
  }
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

void InitializeI2C()
{
  Serial.println("InitializeI2C...");
  Wire.begin(I2C_ADDR);         // join i2c bus with address #9
  //Wire.setClock(9600);
  Wire.onReceive(receiveEvent); // register receive event (i2c_buffer sent by master)  
  Wire.onRequest(requestEvent); // register request event (i2c_buffer sent by slave)  
  Serial.println("Done.");
}

 void InitializeIO()
{    
  int ret;
  Serial.println("InitializeIO...");
 
  // Digital OUTPUTS assignation & initialization
  digitalWrite(VENT_OPEN_CMD,LOW);                           // Set VENT_OPEN_CMD LOW
  digitalWrite(VENT_CLOSE_CMD,HIGH);                         // Set VENT_CLOSE_CMD HIGH
  pinMode(VENT_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve
  pinMode(VENT_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit(VENT_OPEN_CMD_BIT,0x00);                         // Set VENT_OPEN_CMD_BIT LOW
  I2CsetBit(VENT_CLOSE_CMD_BIT,0x01);                        // Set VENT_CLOSE_CMD_BIT HIGH
  digitalWrite(VENTSOFT_OPEN_CMD,LOW);                       // Set VENTSOFT_OPEN_CMD LOW
  digitalWrite(VENTSOFT_CLOSE_CMD,HIGH);                     // Set VENTSOFT_CLOSE_CMD HIGH
  pinMode(VENTSOFT_OPEN_CMD, OUTPUT);                        // Set the digital pin as output for Open Valve 
  pinMode(VENTSOFT_CLOSE_CMD, OUTPUT);                       // Set the digital pin as output for Close Valve
  I2CsetBit(VENTSOFT_OPEN_CMD_BIT,0x00);                     // Set VENTSOFT_OPEN_CMD_BIT LOW
  I2CsetBit(VENTSOFT_CLOSE_CMD_BIT,0x01);                    // Set VENTSOFT_CLOSE_CMD_BIT HIGH
  digitalWrite(VSPARE_OPEN_CMD,LOW);                         // Set VSPARE_OPEN_CMD LOW
  digitalWrite(VSPARE_CLOSE_CMD,HIGH);                       // Set VSPARE_CLOSE_CMD HIGH
  pinMode(VSPARE_OPEN_CMD, OUTPUT);                          // Set the digital pin as output for Switch ON VSPARE
  pinMode(VSPARE_CLOSE_CMD, OUTPUT);                         // Set the digital pin as output for Switch OFF VSPARE
  I2CsetBit(VSPARE_OPEN_CMD_BIT,0x00);                       // Set VSPARE_OPEN_CMD_BIT LOW
  I2CsetBit(VSPARE_CLOSE_CMD_BIT,0x01);                      // Set VSPARE_CLOSE_CMD_BIT HIGH
  digitalWrite(VP_OPEN_CMD,LOW);                             // Set VP_OPEN_CMD LOW
  digitalWrite(VP_CLOSE_CMD,HIGH);                           // Set VP_CLOSE_CMD HIGH
  pinMode(VP_OPEN_CMD, OUTPUT);                              // Set the digital pin as output for Open Valve 
  pinMode(VP_CLOSE_CMD, OUTPUT);                             // Set the digital pin as output for Close Valve
  I2CsetBit(VP_OPEN_CMD_BIT,0x00);                           // Set VP_OPEN_CMD_BIT LOW
  I2CsetBit(VP_CLOSE_CMD_BIT,0x01);                          // Set VP_CLOSE_CMD_BIT HIGH
  digitalWrite(BYPASS_ON_CMD,LOW);                           // Set BYPASS_ON_CMD LOW
  digitalWrite(BYPASS_OFF_CMD,HIGH);                         // Set BYPASS_OFF_CMD HIGH
  pinMode(BYPASS_ON_CMD, OUTPUT);                            // Set the digital pin as output for Switch ON VSPARE
  pinMode(BYPASS_OFF_CMD, OUTPUT);                           // Set the digital pin as output for Switch OFF VSPARE
  I2CsetBit(BYPASS_ON_CMD_BIT,0x00);                         // Set BYPASS_ON_CMD_BIT LOW
  I2CsetBit(BYPASS_OFF_CMD_BIT,0x01);                        // Set BYPASS_OFF_CMD_BIT HIGH
  
  // Digital INPUTS assignation
  pinMode(VENT_OPEN_STATUS, INPUT);                           // sets the digital pin as input for Valve VENT OPEN STATUS
  pinMode(VENT_CLOSE_STATUS, INPUT);                          // sets the digital pin as input for Valve VENT CLOSE STATUS
  pinMode(VSPARE_STATUS, INPUT);                              // sets the digital pin as input for VSPARE CLOSE_OPEN STATUS
  pinMode(VP_OPEN_STATUS, INPUT);                             // sets the digital pin as input for Valve VP OPEN STATUS
  pinMode(VP_CLOSE_STATUS, INPUT);                            // sets the digital pin as input for Valve VP CLOSE STATUS
  pinMode(BYPASS_ON_STATUS, INPUT);                           // sets the digital pin as input for BYPASS ON STATUS
  pinMode(BYPASS_OFF_STATUS, INPUT);                          // sets the digital pin as input for BYPASS OFF STATUS
  pinMode(Ve1_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve Ve1 OPEN STATUS
  pinMode(Ve1_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve Ve1 CLOSE STATUS
  pinMode(Ve2_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve Ve2 OPEN STATUS
  pinMode(Ve2_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve Ve2 CLOSE STATUS
  pinMode(COMPRESSAIR_STATUS, INPUT);                         // sets the digital pin as input for COMPRESSAIR OK/KO STATUS  
  
  Serial.println("Done.");
}

void requestEvent() {
  
  // Send i2c_buffer to master (create 4 bytes array)
  byte i2c_array[8];

  i2c_array[0] = (i2c_buffer >> 24) & 0xFF;
  i2c_array[1] = (i2c_buffer >> 16) & 0xFF;
  i2c_array[2] = (i2c_buffer >> 8) & 0xFF;
  i2c_array[3] = i2c_buffer & 0xFF;
  
  uint32_t crc = CRC32::calculate(i2c_array, 4);

  i2c_array[4] = (crc >> 24) & 0xFF;
  i2c_array[5] = (crc >> 16) & 0xFF;
  i2c_array[6] = (crc >> 8) & 0xFF;
  i2c_array[7] = crc & 0xFF;
  
  Wire.write(i2c_array, 8);

}

void receiveEvent(int numbyte) {

  // Update i2c_buffer from master (4 bytes)
  
  if (numbyte == 8) { // Expect 8 bytes of data (including CRC32), update i2c_buffer
    
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
 *  VENT Valve Case
 */
  // Reset VENT_CLOSE_CMD
  if (digitalRead(VENT_CLOSE_CMD) == LOW && VENT_RESET == true) {
    if ( millis() - VENTtime > reset_wait) {
       digitalWrite(VENT_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(VENT_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VENT_RESET = false;  
    }
  }
  // Reset VENT_OPEN_CMD
  if (digitalRead(VENT_OPEN_CMD) == HIGH && VENT_RESET == true) {
    if ( millis() - VENTtime > reset_wait) {
       digitalWrite(VENT_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(VENT_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VENT_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VENTtime = millis();
       VENT_CHECK = true; 
    }
  }
  // Check VENT Close Status
  if (digitalRead(VENT_CLOSE_STATUS) == HIGH && digitalRead(VENT_OPEN_STATUS) == LOW && VENT_CHECK == true) {
    if ( millis() - VENTtime > check_wait) {
      digitalWrite(VENT_CLOSE_CMD,LOW);  // CLOSE VALVE
      VENTtime = millis();
      VENT_CHECK = false;
      VENT_RESET = true;
    }
  }
/*
 * VENTSOFT Valve case
 */
  // Reset VENTSOFT_CLOSE_CMD
  if (digitalRead(VENTSOFT_CLOSE_CMD) == LOW && VENTSOFT_RESET == true) {
    if ( millis() - VENTSOFTtime > reset_wait) {
       digitalWrite(VENTSOFT_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(VENTSOFT_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VENTSOFT_RESET = false;  
    }
  }
  // Reset VENTSOFT_OPEN_CMD
  if (digitalRead(VENTSOFT_OPEN_CMD) == HIGH && VENTSOFT_RESET == true) {
    if ( millis() - VENTSOFTtime > reset_wait) {
       digitalWrite(VENTSOFT_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(VENTSOFT_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VENTSOFT_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VENTSOFTtime = millis();
       VENTSOFT_CHECK = true; 
    }
  }
  /* Check VENTSOFT Close Status
  if (digitalRead(VENTSOFT_CLOSE_STATUS) == HIGH && digitalRead(VENTSOFT_OPEN_STATUS) == LOW && VENTSOFT_CHECK == true) {
    if ( millis() - VENTSOFTtime > check_wait) {
      digitalWrite(VENTSOFT_CLOSE_CMD,LOW);  // CLOSE VALVE
      VENTSOFTtime = millis();
      VENTSOFT_CHECK = false;
      VENTSOFT_RESET = true;
    }
  }*/
/*
 * VP Valve case
 */
  // Reset VP_CLOSE_CMD
  if (digitalRead(VP_CLOSE_CMD) == LOW && VP_RESET == true) {
    if ( millis() - VPtime > reset_wait) {
       digitalWrite(VP_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(VP_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VP_RESET = false;  
    }
  }
  // Reset VP_OPEN_CMD
  if (digitalRead(VP_OPEN_CMD) == HIGH && VP_RESET == true) {
    if ( millis() - VPtime > reset_wait) {
       digitalWrite(VP_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(VP_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VP_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VPtime = millis();
       VP_CHECK = true; 
    }
  }
  // Check VP Close Status
  if (digitalRead(VP_CLOSE_STATUS) == HIGH && digitalRead(VP_OPEN_STATUS) == LOW && VP_CHECK == true) {
    if ( millis() - VPtime > check_wait) {
      digitalWrite(VP_CLOSE_CMD,LOW);  // CLOSE VALVE
      VPtime = millis();
      VP_CHECK = false;
      VP_RESET = true;
    }
  }
/*
 *  VSPARE Case
 */
  // Reset VSPARE_CLOSE_CMD
  if (digitalRead(VSPARE_CLOSE_CMD) == LOW && VSPARE_RESET == true) {
    if ( millis() - VSPAREtime > reset_wait) {
      digitalWrite(VSPARE_CLOSE_CMD,HIGH);    // RESET CLOSE VSPARE
      I2CsetBit(VSPARE_CLOSE_CMD_BIT,0x01);   // RESET
      VSPARE_RESET = false;
    }
  }
  // Reset VSPARE_OPEN_CMD
  if (digitalRead(VSPARE_OPEN_CMD) == HIGH && VSPARE_RESET == true) {
    if ( millis() - VSPAREtime > reset_wait) {
      digitalWrite(VSPARE_OPEN_CMD,LOW);      // RESET OPEN VSPARE
      I2CsetBit(VSPARE_OPEN_CMD_BIT,0x00);    // RESET
      VSPARE_RESET = false;  
      // Now we will check that Valve has effectively closed after some time
      VSPAREtime = millis();
      VSPARE_CHECK = true; 
    }
  }
  // Check VSPARE Close Status
  if (digitalRead(VSPARE_STATUS) == LOW && VSPARE_CHECK == true) {
    if ( millis() - VSPAREtime > check_wait) {
      digitalWrite(VSPARE_CLOSE_CMD,LOW);  // CLOSE VALVE
      VSPAREtime = millis();
      VSPARE_CHECK = false;
      VSPARE_RESET = true;
    }
  }
/*
 *  BYPASS Case
 */
  // Reset BYPASS_OFF_CMD
  if (digitalRead(BYPASS_OFF_CMD) == LOW && BYPASS_RESET == true) {
    if ( millis() - BYPASStime > reset_wait) {
      digitalWrite(BYPASS_OFF_CMD,HIGH);    // RESET SwITCH OFF BYPASS 
      I2CsetBit(BYPASS_OFF_CMD_BIT,0x01);   // RESET
      BYPASS_RESET = false;
    }
  }
  // Reset BYPASS_ON_CMD
  if (digitalRead(BYPASS_ON_CMD) == HIGH && BYPASS_RESET == true) {
    if ( millis() - BYPASStime > reset_wait) {
      digitalWrite(BYPASS_ON_CMD,LOW);      // RESET SWITCH ON BYPASS
      I2CsetBit(BYPASS_ON_CMD_BIT,0x00);    // RESET
      BYPASS_RESET = false;
      // Now we will check that BYPASS is effectively off after some time
      BYPASStime = millis();
      //BYPASS_CHECK = true; Not used by BYPASS 
    }
  }
  // Check BYPASS Close Status
  if (digitalRead(BYPASS_OFF_STATUS) == HIGH && digitalRead(BYPASS_ON_STATUS) == LOW && BYPASS_CHECK == true) {
    if ( millis() - BYPASStime > check_wait) {
      digitalWrite(BYPASS_OFF_CMD,LOW);  // BYPASS OFF
      BYPASStime = millis();
      BYPASS_CHECK = false;
      BYPASS_RESET = true;
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
  /* Update Valve VENT position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,VENT_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,VENT_OPEN_CMD_BIT) == 0x00 && VENT_RESET == false) {
     digitalWrite(VENT_CLOSE_CMD,LOW);   // CLOSE VALVE
     VENTtime = millis();
     VENT_RESET = true;
  }
  else if (bitRead(i2c_buffer,VENT_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,VENT_CLOSE_CMD_BIT) == 0x01 && VENT_RESET == false) {
     digitalWrite(VENT_OPEN_CMD,HIGH);   // OPEN VALVE
     VENTtime = millis();
     VENT_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VENTSOFT position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,VENTSOFT_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,VENTSOFT_OPEN_CMD_BIT) == 0x00 && VENTSOFT_RESET == false) {
     digitalWrite(VENTSOFT_CLOSE_CMD,LOW);   // CLOSE VALVE
     VENTSOFTtime = millis();
     VENTSOFT_RESET = true;
  }
  else if (bitRead(i2c_buffer,VENTSOFT_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,VENTSOFT_CLOSE_CMD_BIT) == 0x01 && VENTSOFT_RESET == false) {
     digitalWrite(VENTSOFT_OPEN_CMD,HIGH);   // OPEN VALVE
     VENTSOFTtime = millis();
     VENTSOFT_RESET = true;
  }
  
  /***********************************************************************************************************/
  /* Update VSPARE position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,VSPARE_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,VSPARE_OPEN_CMD_BIT) == 0x00 && VSPARE_RESET == false) {
     digitalWrite(VSPARE_CLOSE_CMD,LOW);     // CLOSE VALVE
     VSPAREtime = millis();
     VSPARE_RESET = true; 
  }
  else if (bitRead(i2c_buffer,VSPARE_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,VSPARE_CLOSE_CMD_BIT) == 0x01 && VSPARE_RESET == false) { 
     digitalWrite(VSPARE_OPEN_CMD,HIGH);     // OPEN VALVE
     VSPAREtime = millis();
     VSPARE_RESET = true; 
  }
  /***********************************************************************************************************/
  /* Update Valve VP position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,VP_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,VP_OPEN_CMD_BIT) == 0x00 && VP_RESET == false) {
     digitalWrite(VP_CLOSE_CMD,LOW);   // CLOSE VALVE
     VPtime = millis();
     VP_RESET = true;
  }
  else if (bitRead(i2c_buffer,VP_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,VP_CLOSE_CMD_BIT) == 0x01 && VP_RESET == false) {
     digitalWrite(VP_OPEN_CMD,HIGH);   // OPEN VALVE
     VPtime = millis();
     VP_RESET = true;
  }
  
  /***********************************************************************************************************/
  /* Update BYPASS position (ON/OFF) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,BYPASS_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,BYPASS_ON_CMD_BIT) == 0x00 && BYPASS_RESET == false) {
     digitalWrite(BYPASS_OFF_CMD,LOW);     // SWITCH OFF BYPASS
     BYPASStime = millis();
     BYPASS_RESET = true; 
  }
  else if (bitRead(i2c_buffer,BYPASS_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,BYPASS_OFF_CMD_BIT) == 0x01 && BYPASS_RESET == false) { 
     digitalWrite(BYPASS_ON_CMD,HIGH);     // SWITCH ON BYPASS
     BYPASStime = millis();
     BYPASS_RESET = true; 
  }
}

void UpdateI2CFromIO()
{ 
  /***********************************************************************************************************/
  /* Update VENT Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VENT_OPEN_STATUS) == HIGH && digitalRead(VENT_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(VENT_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VENT_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VENT_CLOSE_STATUS) == HIGH && digitalRead(VENT_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VENT_OPEN_STATUS_BIT == 0x01 && VENT_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VENT_CLOSE_CMD,LOW);  // CLOSE VALVE
        VENTtime = millis();
        VENT_RESET = true;
     }
     I2CsetBit(VENT_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VENT_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(VENT_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VENT_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update Ve1 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(Ve1_OPEN_STATUS) == HIGH && digitalRead(Ve1_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(Ve1_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(Ve1_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(Ve1_CLOSE_STATUS) == HIGH && digitalRead(Ve1_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     I2CsetBit(Ve1_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(Ve1_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(Ve1_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(Ve1_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update Ve2 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(Ve2_OPEN_STATUS) == HIGH && digitalRead(Ve2_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(Ve2_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(Ve2_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(Ve2_CLOSE_STATUS) == HIGH && digitalRead(Ve2_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     I2CsetBit(Ve2_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(Ve2_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(Ve2_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(Ve2_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VP Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VP_OPEN_STATUS) == HIGH && digitalRead(VP_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(VP_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VP_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VP_CLOSE_STATUS) == HIGH && digitalRead(VP_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VP_OPEN_STATUS_BIT == 0x01 && VP_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VP_CLOSE_CMD,LOW);  // CLOSE VALVE
        VPtime = millis();
        VP_RESET = true;
     }
     I2CsetBit(VP_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VP_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(VP_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VP_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update BYPASS position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(BYPASS_ON_STATUS) == HIGH && digitalRead(BYPASS_OFF_STATUS) == LOW) { // BYPASS ON STATUS
     I2CsetBit(BYPASS_ON_STATUS_BIT,0x01);   // UPDATE BYPASS ON BIT
     I2CsetBit(BYPASS_OFF_STATUS_BIT,0x00);  // UPDATE BYPASS OFF BIT
  }
  else if (digitalRead(BYPASS_OFF_STATUS) == HIGH && digitalRead(BYPASS_ON_STATUS) == LOW) { // BYPASS OFF STATUS
     if (BYPASS_ON_STATUS_BIT == 0x01 && BYPASS_OFF_STATUS_BIT == 0x00) { // IF BYPASS ON STATUS BIT
        // reset BYPASS OFF command
        digitalWrite(BYPASS_OFF_CMD,LOW);  // SET BYPASS OFF VALVE
        BYPASStime = millis();
        BYPASS_RESET = true;
     }
     I2CsetBit(BYPASS_ON_STATUS_BIT,0x00);   // UPDATE BYPASS ON BIT
     I2CsetBit(BYPASS_OFF_STATUS_BIT,0x01);  // UPDATE BYPASS OFF BIT
  }
  else {
     // UNKNOWN BYPASS STATUS BIT
     I2CsetBit(BYPASS_ON_STATUS_BIT,0x00);   // UPDATE BYPASS ON BIT
     I2CsetBit(BYPASS_OFF_STATUS_BIT,0x00);  // UPDATE BYPASS OFF BIT
  }
  /***********************************************************************************************************/
  /* Update VSPARE Scroll position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(VSPARE_STATUS) == HIGH) {    // VSPARE OPEN STATUS
     I2CsetBit(VSPARE_STATUS_BIT,0x01);        // UPDATE VSPARE OPEN BIT
  }
  else if (digitalRead(VSPARE_STATUS) == LOW) { // VSPARE CLOSE STATUS
     I2CsetBit(VSPARE_STATUS_BIT,0x00);         // UPDATE VSPARE CLOSE BIT
  }
  /***********************************************************************************************************/
  /* Update COMPRESSAIR position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(COMPRESSAIR_STATUS) == HIGH) {    // COMPRESS AIR OK STATUS
     I2CsetBit(COMPRESSAIR_STATUS_BIT,0x01);        // UPDATE COMPRESSAIR KO BIT
  }
  else if (digitalRead(COMPRESSAIR_STATUS) == LOW) { // COMPRESS AIR KO STATUS
     I2CsetBit(COMPRESSAIR_STATUS_BIT,0x00);         // UPDATE COMPRESSAIR OK BIT
  }

}
