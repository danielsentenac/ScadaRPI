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
#define I2C_ADDR 0x08
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
unsigned long V21time = 0;
unsigned long V22time = 0;
unsigned long P22time = 0;
unsigned long V1time = 0;
unsigned long BYPASStime = 0;
unsigned long looptime = 0;


/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean V21_RESET = false;
boolean V22_RESET = false;
boolean P22_RESET = false;
boolean V1_RESET = false;
boolean BYPASS_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;
long reset_wait2 = 5000; // used for stage primary pumps

 /*
 *  These CHECK are used to check switches status (Status inputs)
 */
boolean V21_CHECK = false;
boolean V22_CHECK = false;
boolean P22_CHECK = false;
boolean V1_CHECK = false;
boolean BYPASS_CHECK = false;

/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;
 
/*
 *  I2C BIT ASSIGNATION (MAX = 32) 
 */
#define V21_OPEN_CMD_BIT           0   // V21 Open bit
#define V21_CLOSE_CMD_BIT          1   // V21 Close bit
#define V21_OPEN_STATUS_BIT        2   // V21 Open Status bit
#define V21_CLOSE_STATUS_BIT       3   // V21 Close Status bit
#define V22_OPEN_CMD_BIT           4   // V22 Open bit
#define V22_CLOSE_CMD_BIT          5   // V22 Close bit
#define V22_OPEN_STATUS_BIT        6   // V22 Open Status bit
#define V22_CLOSE_STATUS_BIT       7   // V22 Close Status bit
#define V1_OPEN_CMD_BIT            8   // V1 Open bit
#define V1_CLOSE_CMD_BIT           9   // V1 Close bit
#define V1_OPEN_STATUS_BIT         10  // V1 Open Status bit
#define V1_CLOSE_STATUS_BIT        11  // V1 Close Status bit
#define BYPASS_ON_CMD_BIT          12  // BYPASS On bit
#define BYPASS_OFF_CMD_BIT         13  // BYPASS Off bit
#define BYPASS_ON_STATUS_BIT       14  // BYPASS ON Status bit
#define BYPASS_OFF_STATUS_BIT      15  // BYPASS OFF Status bit
#define P22_ON_CMD_BIT             16  // P22 On bit
#define P22_OFF_CMD_BIT            17  // P22 Off bit
#define P22_STATUS_BIT             18  // P22 Status bit
#define V23_OPEN_STATUS_BIT        19  // V23 Open Status bit
#define V23_CLOSE_STATUS_BIT       20  // V23 Close Status bit
#define COMPRESSAIR_STATUS_BIT     21  // COMPRESSAIR Status bit

#define ARD_RESET_BIT              31  // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */ 
// digital pins (OUTPUTS)
#define V21_OPEN_CMD   CONTROLLINO_R0  //(Open Command V21)
#define V21_CLOSE_CMD  CONTROLLINO_R1  //(Close Command V21)
#define V22_OPEN_CMD   CONTROLLINO_R2  //(Open Command V22)
#define V22_CLOSE_CMD  CONTROLLINO_R3  //(Close Command V22)
#define P22_ON_CMD     CONTROLLINO_R4  //(Command Switch ON Scroll P22)
#define P22_OFF_CMD    CONTROLLINO_R5  //(Command Switch OFF Scroll P22)
#define V1_OPEN_CMD    CONTROLLINO_R6  //(Open Command V1)
#define V1_CLOSE_CMD   CONTROLLINO_R7  //(Close Command V1)
#define BYPASS_ON_CMD  CONTROLLINO_R8  //(Command Switch ON BYPASS)
#define BYPASS_OFF_CMD CONTROLLINO_R9  //(Command Switch OFF BYPASS)


// digital pins (INTPUTS)
#define COMPRESSAIR_STATUS CONTROLLINO_A0    // COMPRESSAIR STATUS
#define BYPASS_ON_STATUS   CONTROLLINO_A1    // BYPASS STATUS
#define BYPASS_OFF_STATUS  CONTROLLINO_A2    // BYPASS STATUS
#define V1_OPEN_STATUS     CONTROLLINO_A3    // V1 OPEN STATUS
#define V1_CLOSE_STATUS    CONTROLLINO_A4    // V1 CLOSE STATUS
#define V21_OPEN_STATUS    CONTROLLINO_A5    // V21 OPEN STATUS
#define V21_CLOSE_STATUS   CONTROLLINO_A6    // V21 CLOSE STATUS
#define V22_OPEN_STATUS    CONTROLLINO_A7    // V22 OPEN STATUS
#define V22_CLOSE_STATUS   CONTROLLINO_A8    // V22 CLOSE STATUS
#define P22_STATUS         CONTROLLINO_A9    // P22 ON/OFF STATUS
#define V23_OPEN_STATUS    CONTROLLINO_IN0   // V23 OPEN STATUS
#define V23_CLOSE_STATUS   CONTROLLINO_IN1   // V23 CLOSE STATUS

void(*resetArd) (void) = 0; //declare reset function @ address 0

void setup() {

 InitializeIO();
 InitializeI2C();

 // Open Serial communication for Console port.
 Serial.begin(9600);
}
 
void loop() {  
  delay(100);
  // For debug purpose
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
  Wire.begin(I2C_ADDR);         // join i2c bus with address #8
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
  digitalWrite(V21_OPEN_CMD,LOW);                           // Set V21_OPEN_CMD LOW
  digitalWrite(V21_CLOSE_CMD,HIGH);                         // Set V21_CLOSE_CMD HIGH
  pinMode(V21_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve
  pinMode(V21_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit(V21_OPEN_CMD_BIT,0x00);                         // Set V21_OPEN_CMD_BIT LOW
  I2CsetBit(V21_CLOSE_CMD_BIT,0x01);                        // Set V21_CLOSE_CMD_BIT HIGH
  digitalWrite(V22_OPEN_CMD,LOW);                           // Set V22_OPEN_CMD LOW
  digitalWrite(V22_CLOSE_CMD,HIGH);                         // Set V22_CLOSE_CMD HIGH
  pinMode(V22_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve 
  pinMode(V22_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit(V22_OPEN_CMD_BIT,0x00);                         // Set V22_OPEN_CMD_BIT LOW
  I2CsetBit(V22_CLOSE_CMD_BIT,0x01);                        // Set V22_CLOSE_CMD_BIT HIGH
  digitalWrite(P22_ON_CMD,LOW);                             // Set P22_ON_CMD LOW
  digitalWrite(P22_OFF_CMD,HIGH);                           // Set P22_OFF_CMD HIGH
  pinMode(P22_ON_CMD, OUTPUT);                              // Set the digital pin as output for Switch ON Scroll
  pinMode(P22_OFF_CMD, OUTPUT);                             // Set the digital pin as output for Switch OFF Scroll
  I2CsetBit(P22_ON_CMD_BIT,0x00);                           // Set P22_ON_CMD_BIT LOW
  I2CsetBit(P22_OFF_CMD_BIT,0x01);                          // Set P22_OFF_CMD_BIT HIGH
  digitalWrite(V1_OPEN_CMD,LOW);                            // Set V1_OPEN_CMD LOW
  digitalWrite(V1_CLOSE_CMD,HIGH);                          // Set V1_CLOSE_CMD HIGH
  pinMode(V1_OPEN_CMD, OUTPUT);                             // Set the digital pin as output for Open Valve 
  pinMode(V1_CLOSE_CMD, OUTPUT);                            // Set the digital pin as output for Close Valve
  I2CsetBit(V1_OPEN_CMD_BIT,0x00);                          // Set V1_OPEN_CMD_BIT LOW
  I2CsetBit(V1_CLOSE_CMD_BIT,0x01);                         // Set V1_CLOSE_CMD_BIT HIGH
  digitalWrite(BYPASS_ON_CMD,LOW);                          // Set BYPASS_ON_CMD LOW
  digitalWrite(BYPASS_OFF_CMD,HIGH);                        // Set BYPASS_OFF_CMD HIGH
  pinMode(BYPASS_ON_CMD, OUTPUT);                           // Set the digital pin as output for Switch ON Scroll
  pinMode(BYPASS_OFF_CMD, OUTPUT);                          // Set the digital pin as output for Switch OFF Scroll
  I2CsetBit(BYPASS_ON_CMD_BIT,0x00);                        // Set BYPASS_ON_CMD_BIT LOW
  I2CsetBit(BYPASS_OFF_CMD_BIT,0x01);                       // Set BYPASS_OFF_CMD_BIT HIGH
  
  // Digital INPUTS assignation
  pinMode(V21_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V21 OPEN STATUS
  pinMode(V21_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V21 CLOSE STATUS
  pinMode(V22_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V22 OPEN STATUS
  pinMode(V22_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V22 CLOSE STATUS
  pinMode(P22_STATUS, INPUT);                                 // sets the digital pin as input for Scroll ON/OFF STATUS
  pinMode(V1_OPEN_STATUS, INPUT);                             // sets the digital pin as input for Valve V1 OPEN STATUS
  pinMode(V1_CLOSE_STATUS, INPUT);                            // sets the digital pin as input for Valve V1 CLOSE STATUS
  pinMode(BYPASS_ON_STATUS, INPUT);                           // sets the digital pin as input for BYPASS ON STATUS
  pinMode(BYPASS_OFF_STATUS, INPUT);                          // sets the digital pin as input for BYPASS OFF STATUS
  pinMode(V23_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V23 OPEN STATUS
  pinMode(V23_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V23 CLOSE STATUS
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
 *  V21 Valve Case
 */
  // Reset V21_CLOSE_CMD
  if (digitalRead(V21_CLOSE_CMD) == LOW && V21_RESET == true) {
    if ( millis() - V21time > reset_wait) {
       digitalWrite(V21_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(V21_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       V21_RESET = false;  
    }
  }
  // Reset V21_OPEN_CMD
  if (digitalRead(V21_OPEN_CMD) == HIGH && V21_RESET == true) {
    if ( millis() - V21time > reset_wait) {
       digitalWrite(V21_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(V21_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       V21_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V21time = millis();
       V21_CHECK = true; 
    }
  }
  // Check V21 Close Status
  if (digitalRead(V21_CLOSE_STATUS) == HIGH && digitalRead(V21_OPEN_STATUS) == LOW && V21_CHECK == true) {
    if ( millis() - V21time > check_wait) {
      digitalWrite(V21_CLOSE_CMD,LOW);  // CLOSE VALVE
      V21time = millis();
      V21_CHECK = false;
      V21_RESET = true;
    }
  }
/*
 * V22 Valve case
 */
  // Reset V22_CLOSE_CMD
  if (digitalRead(V22_CLOSE_CMD) == LOW && V22_RESET == true) {
    if ( millis() - V22time > reset_wait) {
       digitalWrite(V22_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(V22_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       V22_RESET = false;  
    }
  }
  // Reset V22_OPEN_CMD
  if (digitalRead(V22_OPEN_CMD) == HIGH && V22_RESET == true) {
    if ( millis() - V22time > reset_wait) {
       digitalWrite(V22_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(V22_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       V22_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V22time = millis();
       V22_CHECK = true; 
    }
  }
  // Check V22 Close Status
  if (digitalRead(V22_CLOSE_STATUS) == HIGH && digitalRead(V22_OPEN_STATUS) == LOW && V22_CHECK == true) {
    if ( millis() - V22time > check_wait) {
      digitalWrite(V22_CLOSE_CMD,LOW);  // CLOSE VALVE
      V22time = millis();
      V22_CHECK = false;
      V22_RESET = true;
    }
  }
/*
 * V1 Valve case
 */
  // Reset V1_CLOSE_CMD
  if (digitalRead(V1_CLOSE_CMD) == LOW && V1_RESET == true) {
    if ( millis() - V1time > reset_wait) {
       digitalWrite(V1_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(V1_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       V1_RESET = false;  
    }
  }
  // Reset V1_OPEN_CMD
  if (digitalRead(V1_OPEN_CMD) == HIGH && V1_RESET == true) {
    if ( millis() - V1time > reset_wait) {
       digitalWrite(V1_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(V1_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       V1_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V1time = millis();
       V1_CHECK = true; 
    }
  }
  // Check V1 Close Status
  if (digitalRead(V1_CLOSE_STATUS) == HIGH && digitalRead(V1_OPEN_STATUS) == LOW && V1_CHECK == true) {
    if ( millis() - V1time > check_wait) {
      digitalWrite(V1_CLOSE_CMD,LOW);  // CLOSE VALVE
      V1time = millis();
      V1_CHECK = false;
      V1_RESET = true;
    }
  }
/*
 *  P22 Case
 */
  // Reset P22_OFF_CMD
  if (digitalRead(P22_OFF_CMD) == LOW && P22_RESET == true) {
    if ( millis() - P22time > reset_wait2) {
      digitalWrite(P22_OFF_CMD,HIGH);    // RESET SWITCH OFF SCROLL/STAGE
      I2CsetBit(P22_OFF_CMD_BIT,0x01);   // RESET
      P22_RESET = false;
    }
  }
  // Reset P22_ON_CMD
  if (digitalRead(P22_ON_CMD) == HIGH && P22_RESET == true) {
    if ( millis() - P22time > reset_wait2) {
      digitalWrite(P22_ON_CMD,LOW);      // RESET SWITCH ON SCROLL/STAGE
      I2CsetBit(P22_ON_CMD_BIT,0x00);    // RESET
      P22_RESET = false;  
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
      //BYPASS_CHECK = true; Not used for BYPASS 
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
  /* Update Valve V21 position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V21_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V21_OPEN_CMD_BIT) == 0x00 && V21_RESET == false) {
     digitalWrite(V21_CLOSE_CMD,LOW);   // CLOSE VALVE
     V21time = millis();
     V21_RESET = true;
  }
  else if (bitRead(i2c_buffer,V21_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V21_CLOSE_CMD_BIT) == 0x01 && V21_RESET == false) {
     digitalWrite(V21_OPEN_CMD,HIGH);   // OPEN VALVE
     V21time = millis();
     V21_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve V22 position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V22_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V22_OPEN_CMD_BIT) == 0x00 && V22_RESET == false) {
     digitalWrite(V22_CLOSE_CMD,LOW);   // CLOSE VALVE
     V22time = millis();
     V22_RESET = true;
  }
  else if (bitRead(i2c_buffer,V22_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V22_CLOSE_CMD_BIT) == 0x01 && V22_RESET == false) {
     digitalWrite(V22_OPEN_CMD,HIGH);   // OPEN VALVE
     V22time = millis();
     V22_RESET = true;
  }
  
  /***********************************************************************************************************/
  /* Update Scroll P22 position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,P22_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,P22_ON_CMD_BIT) == 0x00 && P22_RESET == false) {
     digitalWrite(P22_OFF_CMD,LOW);     // SWITCH OFF SCROLL
     P22time = millis();
     P22_RESET = true; 
  }
  else if (bitRead(i2c_buffer,P22_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,P22_OFF_CMD_BIT) == 0x01 && P22_RESET == false) { 
     digitalWrite(P22_ON_CMD,HIGH);     // SWITCH ON SCROLL
     P22time = millis();
     P22_RESET = true; 
  }
  /***********************************************************************************************************/
  /* Update Valve V1 position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V1_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V1_OPEN_CMD_BIT) == 0x00 && V1_RESET == false) {
     digitalWrite(V1_CLOSE_CMD,LOW);   // CLOSE VALVE
     V1time = millis();
     V1_RESET = true;
  }
  else if (bitRead(i2c_buffer,V1_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V1_CLOSE_CMD_BIT) == 0x01 && V1_RESET == false) {
     digitalWrite(V1_OPEN_CMD,HIGH);   // OPEN VALVE
     V1time = millis();
     V1_RESET = true;
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
  /* Update V21 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V21_OPEN_STATUS) == HIGH && digitalRead(V21_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V21_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V21_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V21_CLOSE_STATUS) == HIGH && digitalRead(V21_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (bitRead(i2c_buffer,V21_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21_CLOSE_STATUS_BIT) == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V21_CLOSE_CMD,LOW);  // CLOSE VALVE
        V21time = millis();
        V21_RESET = true;
     }
     I2CsetBit(V21_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V21_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V21_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V21_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update V22 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V22_OPEN_STATUS) == HIGH && digitalRead(V22_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V22_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V22_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V22_CLOSE_STATUS) == HIGH && digitalRead(V22_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (bitRead(i2c_buffer,V22_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22_CLOSE_STATUS_BIT) == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V22_CLOSE_CMD,LOW);  // CLOSE VALVE
        V22time = millis();
        V22_RESET = true;
     }
     I2CsetBit(V22_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V22_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V22_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V22_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update V23 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V23_OPEN_STATUS) == HIGH && digitalRead(V23_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V23_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V23_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V23_CLOSE_STATUS) == HIGH && digitalRead(V23_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     I2CsetBit(V23_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V23_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V23_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V23_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update V1 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V1_OPEN_STATUS) == HIGH && digitalRead(V1_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V1_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V1_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V1_CLOSE_STATUS) == HIGH && digitalRead(V1_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (bitRead(i2c_buffer,V1_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V1_CLOSE_STATUS_BIT) == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V1_CLOSE_CMD,LOW);  // CLOSE VALVE
        V1time = millis();
        V1_RESET = true;
     }
     I2CsetBit(V1_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V1_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V1_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V1_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update BYPASS position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(BYPASS_ON_STATUS) == HIGH && digitalRead(BYPASS_OFF_STATUS) == LOW) { // BYPASS ON STATUS
     I2CsetBit(BYPASS_ON_STATUS_BIT,0x01);   // UPDATE BYPASS ON BIT
     I2CsetBit(BYPASS_OFF_STATUS_BIT,0x00);  // UPDATE BYPASS OFF BIT
  }
  else if (digitalRead(BYPASS_OFF_STATUS) == HIGH && digitalRead(BYPASS_ON_STATUS) == LOW) { // BYPASS OFF STATUS
     if (bitRead(i2c_buffer,BYPASS_ON_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,BYPASS_OFF_STATUS_BIT) == 0x00) { // IF BYPASS ON STATUS BIT
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
  /* Update P22 Scroll position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(P22_STATUS) == HIGH) {    // SCROLL ON STATUS
     I2CsetBit(P22_STATUS_BIT,0x01);        // UPDATE SCROLL ON BIT
  }
  else if (digitalRead(P22_STATUS) == LOW) { // SCROLL OFF STATUS
     I2CsetBit(P22_STATUS_BIT,0x00);         // UPDATE SCROLL OFF BIT
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
