/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include <Wire.h> // I2C library
#include <Controllino.h> // Controllino library

/*
 * I2C ADDRESS (SLAVE)
 */ 
#define I2C_ADDR 0x08
/* 
 * I2C BUFFER SIZE
 */
#define I2C_BUFFER 32

uint32_t i2c_buffer = 0; 

/*
 * The time for Check and Reset actions
 */
unsigned long V21time = 0;
unsigned long V22time = 0;
unsigned long P22time = 0;
unsigned long V31time = 0;
unsigned long P31_32time = 0;


/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean V21_RESET = false;
boolean V22_RESET = false;
boolean P22_RESET = false;
boolean V31_RESET = false;
boolean P31_32_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;

 /*
 *  These CHECK are used to check switches status (Status inputs)
 */
boolean V21_CHECK = false;
boolean V22_CHECK = false;
boolean P22_CHECK = false;
boolean V31_CHECK = false;
boolean P31_32_CHECK = false;


/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;
 
/*
 *  I2C BIT ASSIGNATION (MAX = 32) 
 */
#define V21_OPEN_CMD_BIT           0   // V21 Open bit
#define V21_CLOSE_CMD_BIT          1   // V21 Close bit
#define V22_OPEN_CMD_BIT           2   // V22 Open bit
#define V22_CLOSE_CMD_BIT          3   // V22 Close bit
#define P22_ON_CMD_BIT             4   // P22 On bit
#define P22_OFF_CMD_BIT            5   // P22 Off bit
#define V21_OPEN_STATUS_BIT        6   // V21ST Open Status bit
#define V21_CLOSE_STATUS_BIT       7   // V21ST Close Status bit
#define V22_OPEN_STATUS_BIT        8   // V22ST Open Status bit
#define V22_CLOSE_STATUS_BIT       9   // V22ST Close Status bit
#define P22_STATUS_BIT             10  // P22ST Status bit
#define V23_OPEN_STATUS_BIT        11  // V23ST Open Status bit
#define V23_CLOSE_STATUS_BIT       12  // V23ST Close Status bit
#define COMPRESSAIR_STATUS_BIT     13  // COMPRESSAIRST Status bit
#define VA1_OPEN_STATUS_BIT        14  // VA1ST Open Status bit
#define VA1_CLOSE_STATUS_BIT       15  // VA1ST Close Status bit
#define VA2_OPEN_STATUS_BIT        16  // VA2ST Open Status bit
#define VA2_CLOSE_STATUS_BIT       17  // VA2ST Close Status bit
#define V31_OPEN_STATUS_BIT        18  // V31ST Open Status bit
#define V31_CLOSE_STATUS_BIT       19  // V31ST Close Status bit
#define V31_OPEN_CMD_BIT           20  // V31 Open bit
#define V31_CLOSE_CMD_BIT          21  // V31 Close bit
#define P31_32_ON_CMD_BIT          22  // P31_32 On bit
#define P31_32_OFF_CMD_BIT         23  // P31_32 Off bit
#define P31_32_STATUS_BIT          24  // P31_32ST Status bit
#define ARD_RESET_BIT              31  // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */ 
// digital pins (OUTPUTS)
#define P22_ON_CMD CONTROLLINO_D0    //(Command Switch ON Scroll P22)
#define P22_OFF_CMD CONTROLLINO_R0   //(Command Switch OFF Scroll P22)
#define V21_OPEN_CMD CONTROLLINO_D1  //(Open Command V21)
#define V21_CLOSE_CMD CONTROLLINO_R1 //(Close Command V21)
#define V22_OPEN_CMD CONTROLLINO_D2  //(Open Command V22)
#define V22_CLOSE_CMD CONTROLLINO_R2 //(Close Command V22)
#define P31_32_ON_CMD CONTROLLINO_D3 //(Command Switch ON Titanium P31-32)
#define P31_32_OFF_CMD CONTROLLINO_R3//(Command Switch OFF Titanium P31-32)
#define V31_OPEN_CMD CONTROLLINO_D4  //(Open Command V31)
#define V31_CLOSE_CMD CONTROLLINO_R4 //(Close Command V31)

// digital pins (INTPUTS)
#define P22_STATUS CONTROLLINO_A0         // P22 ON/OFF STATUS
#define V21_OPEN_STATUS CONTROLLINO_A1    // V21 OPEN STATUS
#define V21_CLOSE_STATUS CONTROLLINO_A2   // V21 CLOSE STATUS
#define V22_OPEN_STATUS CONTROLLINO_A3    // V22 OPEN STATUS
#define V22_CLOSE_STATUS CONTROLLINO_A4   // V22 CLOSE STATUS
#define P31_32_STATUS CONTROLLINO_A5      // P31_32 ON/OFF STATUS
#define V31_OPEN_STATUS CONTROLLINO_A6    // V31 OPEN STATUS
#define V31_CLOSE_STATUS CONTROLLINO_A7   // V31 CLOSE STATUS
#define VA2_OPEN_STATUS CONTROLLINO_A8    // VA2 OPEN STATUS
#define VA2_CLOSE_STATUS CONTROLLINO_A9   // VA2 CLOSE STATUS
#define V23_OPEN_STATUS  CONTROLLINO_PIN_HEADER_ANALOG_ADC_IN_10   // V23 OPEN STATUS  (5V!)
#define V23_CLOSE_STATUS CONTROLLINO_PIN_HEADER_ANALOG_ADC_IN_11   // V23 CLOSE STATUS (5V!)
#define VA1_OPEN_STATUS CONTROLLINO_PIN_HEADER_ANALOG_ADC_IN_12    // VA1 OPEN STATUS  (5V!)
#define VA1_CLOSE_STATUS CONTROLLINO_PIN_HEADER_ANALOG_ADC_IN_13   // VA1 CLOSE STATUS (5V!)
#define COMPRESSAIR_STATUS CONTROLLINO_IN0 // COMPRESS AIR STATUS


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
  Serial.print("i2c_buffer=");
  Serial.println(i2c_buffer,BIN);
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
  digitalWrite(V31_OPEN_CMD,LOW);                           // Set V31_OPEN_CMD LOW
  digitalWrite(V31_CLOSE_CMD,HIGH);                         // Set V31_CLOSE_CMD HIGH
  pinMode(V31_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve 
  pinMode(V31_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit(V31_OPEN_CMD_BIT,0x00);                         // Set V31_OPEN_CMD_BIT LOW
  I2CsetBit(V31_CLOSE_CMD_BIT,0x01);                        // Set V31_CLOSE_CMD_BIT HIGH
  digitalWrite(P31_32_ON_CMD,LOW);                          // Set P31_32_ON_CMD LOW
  digitalWrite(P31_32_OFF_CMD,HIGH);                        // Set P31_32_OFF_CMD HIGH
  pinMode(P31_32_ON_CMD, OUTPUT);                           // Set the digital pin as output for Switch ON Scroll
  pinMode(P31_32_OFF_CMD, OUTPUT);                          // Set the digital pin as output for Switch OFF Scroll
  I2CsetBit(P31_32_ON_CMD_BIT,0x00);                        // Set P31_32_ON_CMD_BIT LOW
  I2CsetBit(P31_32_OFF_CMD_BIT,0x01);                       // Set P31_32_OFF_CMD_BIT HIGH
  
  // Digital INPUTS assignation
  pinMode(V21_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V21 OPEN STATUS
  pinMode(V21_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V21 CLOSE STATUS
  pinMode(V22_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V22 OPEN STATUS
  pinMode(V22_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V22 CLOSE STATUS
  pinMode(P22_STATUS, INPUT);                                 // sets the digital pin as input for Scroll ON/OFF STATUS
  pinMode(V31_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V31 OPEN STATUS
  pinMode(V31_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V31 CLOSE STATUS
  pinMode(P31_32_STATUS, INPUT);                              // sets the digital pin as input for Titanium ON/OFF STATUS
  pinMode(V23_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve OPEN STATUS
  pinMode(V23_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve CLOSE STATUS
  pinMode(COMPRESSAIR_STATUS, INPUT);                         // sets the digital pin as input for Compress Air STATUS
  pinMode(VA1_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve OPEN STATUS
  pinMode(VA1_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve CLOSE STATUS
  pinMode(VA2_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve OPEN STATUS
  pinMode(VA2_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve CLOSE STATUS
  
  
  Serial.println("Done.");
}

void requestEvent() {

  // First update i2c_buffer from I/O
  UpdateI2CFromIO();
  
  // Send i2c_buffer to master (create 4 bytes array)
  byte i2c_array[4];

  i2c_array[0] = (i2c_buffer >> 24) & 0xFF;
  i2c_array[1] = (i2c_buffer >> 16) & 0xFF;
  i2c_array[2] = (i2c_buffer >> 8) & 0xFF;
  i2c_array[3] = i2c_buffer & 0xFF;
  
  Wire.write(i2c_array, 4);

}
void receiveEvent(int numbyte) {

  // Update i2c_buffer from master (4 bytes)
  byte a,b,c,d;

  if (numbyte == 4) { // Expect 4 bytes of data, update i2c_buffer
    a = Wire.read();
    b = Wire.read();
    c = Wire.read();
    d = Wire.read();
  
    i2c_buffer = a;
    i2c_buffer = (i2c_buffer << 8) | b;
    i2c_buffer = (i2c_buffer << 8) | c;
    i2c_buffer = (i2c_buffer << 8) | d;

    // Finally update I/O
    UpdateIOFromI2C();
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
 *  P22 Case
 */
  // Reset P22_OFF_CMD
  if (digitalRead(P22_OFF_CMD) == LOW && P22_RESET == true) {
    if ( millis() - P22time > reset_wait) {
      digitalWrite(P22_OFF_CMD,HIGH);    // RESET SWITCH ON SCROLL
      I2CsetBit(P22_OFF_CMD_BIT,0x01);   // RESET
      P22_RESET = false;
    }
  }
  // Reset P22_ON_CMD
  if (digitalRead(P22_ON_CMD) == HIGH && P22_RESET == true) {
    if ( millis() - P22time > reset_wait) {
      digitalWrite(P22_ON_CMD,LOW);      // RESET SWITCH ON SCROLL
      I2CsetBit(P22_ON_CMD_BIT,0x00);    // RESET
      P22_RESET = false;  
    }
  }
  /*
 * V31 Valve case
 */
  // Reset V31_CLOSE_CMD
  if (digitalRead(V31_CLOSE_CMD) == LOW && V31_RESET == true) {
    if ( millis() - V31time > reset_wait) {
       digitalWrite(V31_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(V31_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       V31_RESET = false;  
    }
  }
  // Reset V31_OPEN_CMD
  if (digitalRead(V31_OPEN_CMD) == HIGH && V31_RESET == true) {
    if ( millis() - V31time > reset_wait) {
       digitalWrite(V31_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(V31_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       V31_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V31time = millis();
       V31_CHECK = true; 
    }
  }
  // Check V31 Close Status
  if (digitalRead(V31_CLOSE_STATUS) == HIGH && digitalRead(V31_OPEN_STATUS) == LOW && V31_CHECK == true) {
    if ( millis() - V31time > check_wait) {
      digitalWrite(V31_CLOSE_CMD,LOW);  // CLOSE VALVE
      V31time = millis();
      V31_CHECK = false;
      V31_RESET = true;
    }
  }
/*
 *  P31_32 Case
 */
  // Reset P31_32_OFF_CMD
  if (digitalRead(P31_32_OFF_CMD) == LOW && P31_32_RESET == true) {
    if ( millis() - P31_32time > reset_wait) {
      digitalWrite(P31_32_OFF_CMD,HIGH);    // RESET SWITCH ON TITANIUM
      I2CsetBit(P31_32_OFF_CMD_BIT,0x01);   // RESET
      P31_32_RESET = false;
    }
  }
  // Reset P31_32_ON_CMD
  if (digitalRead(P31_32_ON_CMD) == HIGH && P31_32_RESET == true) {
    if ( millis() - P31_32time > reset_wait) {
      digitalWrite(P31_32_ON_CMD,LOW);      // RESET SWITCH ON TITANIUM
      I2CsetBit(P31_32_ON_CMD_BIT,0x00);    // RESET
      P31_32_RESET = false;  
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
  /* Update Valve V31 position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V31_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V31_OPEN_CMD_BIT) == 0x00 && V31_RESET == false) {
     digitalWrite(V31_CLOSE_CMD,LOW);   // CLOSE VALVE
     V31time = millis();
     V31_RESET = true;
  }
  else if (bitRead(i2c_buffer,V31_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V31_CLOSE_CMD_BIT) == 0x01 && V31_RESET == false) {
     digitalWrite(V31_OPEN_CMD,HIGH);   // OPEN VALVE
     V31time = millis();
     V31_RESET = true;
  }
  
  /***********************************************************************************************************/
  /* Update Titanium P31_32 position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,P31_32_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,P31_32_ON_CMD_BIT) == 0x00 && P31_32_RESET == false) {
     digitalWrite(P31_32_OFF_CMD,LOW);     // SWITCH OFF TITANIUM
     P31_32time = millis();
     P31_32_RESET = true; 
  }
  else if (bitRead(i2c_buffer,P31_32_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,P31_32_OFF_CMD_BIT) == 0x01 && P31_32_RESET == false) { 
     digitalWrite(P31_32_ON_CMD,HIGH);     // SWITCH ON TITANIUM
     P31_32time = millis();
     P31_32_RESET = true; 
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
  /* Update VA1 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VA1_OPEN_STATUS) == HIGH && digitalRead(VA1_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(VA1_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VA1_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VA1_CLOSE_STATUS) == HIGH && digitalRead(VA1_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     I2CsetBit(VA1_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VA1_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(VA1_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VA1_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VA2 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VA2_OPEN_STATUS) == HIGH && digitalRead(VA2_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(VA2_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VA2_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VA2_CLOSE_STATUS) == HIGH && digitalRead(VA2_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     I2CsetBit(VA2_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VA2_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(VA2_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VA2_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update V31 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V31_OPEN_STATUS) == HIGH && digitalRead(V31_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V31_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V31_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V31_CLOSE_STATUS) == HIGH && digitalRead(V31_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (bitRead(i2c_buffer,V31_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V31_CLOSE_STATUS_BIT) == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V31_CLOSE_CMD,LOW);  // CLOSE VALVE
        V31time = millis();
        V31_RESET = true;
     }
     I2CsetBit(V31_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V31_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V31_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V31_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
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
  /* Update P31_32 Titanium position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(P31_32_STATUS) == HIGH) {    // TITANIUM ON STATUS
     I2CsetBit(P31_32_STATUS_BIT,0x01);        // UPDATE TITANIUM ON BIT
  }
  else if (digitalRead(P31_32_STATUS) == LOW) { // TITANIUM OFF STATUS
     I2CsetBit(P31_32_STATUS_BIT,0x00);    // UPDATE TITANIUM OFF BIT
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
