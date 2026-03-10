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
unsigned long V21DETtime = 0;
unsigned long V22DETtime = 0;
unsigned long P22DETtime = 0;
unsigned long V21IBtime = 0;
unsigned long V22IBtime = 0;
unsigned long P22IBtime = 0;
unsigned long looptime = 0;


/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean V21DET_RESET = false;
boolean V22DET_RESET = false;
boolean P22DET_RESET = false;
boolean V21IB_RESET = false;
boolean V22IB_RESET = false;
boolean P22IB_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;

 /*
 *  These CHECK are used to check switches status (Status inputs)
 */
boolean V21DET_CHECK = false;
boolean V22DET_CHECK = false;
boolean P22DET_CHECK = false;
boolean V21IB_CHECK = false;
boolean V22IB_CHECK = false;
boolean P22IB_CHECK = false;

/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;
 
/*
 *  I2C BIT ASSIGNATION (MAX = 32) 
 */
#define V21DET_OPEN_CMD_BIT           0   // V21DET Open bit
#define V21DET_CLOSE_CMD_BIT          1   // V21DET Close bit
#define V21DET_OPEN_STATUS_BIT        2   // V21DET Open Status bit
#define V21DET_CLOSE_STATUS_BIT       3   // V21DET Close Status bit
#define V22DET_OPEN_CMD_BIT           4   // V22DET Open bit
#define V22DET_CLOSE_CMD_BIT          5   // V22DET Close bit
#define V22DET_OPEN_STATUS_BIT        6   // V22DET Open Status bit
#define V22DET_CLOSE_STATUS_BIT       7   // V22DET Close Status bit
#define V21IB_OPEN_CMD_BIT            8   // V21IB Open bit
#define V21IB_CLOSE_CMD_BIT           9   // V21IB Close bit
#define V21IB_OPEN_STATUS_BIT         10  // V21IB Open Status bit
#define V21IB_CLOSE_STATUS_BIT        11  // V21IB Close Status bit
#define V22IB_OPEN_CMD_BIT            12  // V22IB Open bit
#define V22IB_CLOSE_CMD_BIT           13  // V22IB Close bit
#define V22IB_OPEN_STATUS_BIT         14  // V22IB Open Status bit
#define V22IB_CLOSE_STATUS_BIT        15  // V22IB Close Status bit
#define P22DET_ON_CMD_BIT             16  // P22DET On bit
#define P22DET_OFF_CMD_BIT            17  // P22DET Off bit
#define P22DET_STATUS_BIT             18  // P22DET Status bit
#define P22IB_ON_CMD_BIT              19  // P22IB On bit
#define P22IB_OFF_CMD_BIT             20  // P22IB Off bit
#define P22IB_STATUS_BIT              21  // P22IB Status bit
#define COMPRESSAIR_STATUS_BIT        22  // COMPRESSAIR Status bit

#define ARD_RESET_BIT                 31  // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */ 
// digital pins (OUTPUTS)
#define P22DET_ON_CMD CONTROLLINO_D0    //(Command Switch ON Scroll P22DET)
#define P22DET_OFF_CMD CONTROLLINO_R0   //(Command Switch OFF Scroll P22DET)
#define V21DET_OPEN_CMD CONTROLLINO_D1  //(Open Command V21DET)
#define V21DET_CLOSE_CMD CONTROLLINO_R1 //(Close Command V21DET)
#define V22DET_OPEN_CMD CONTROLLINO_D2  //(Open Command V22DET)
#define V22DET_CLOSE_CMD CONTROLLINO_R2 //(Close Command V22DET)
#define P22IB_ON_CMD CONTROLLINO_D3 //(Command Switch ON Scroll P22IB)
#define P22IB_OFF_CMD CONTROLLINO_R3//(Command Switch OFF Scroll P22IB)
#define V21IB_OPEN_CMD CONTROLLINO_D4  //(Open Command V21IB)
#define V21IB_CLOSE_CMD CONTROLLINO_R4 //(Close Command V21IB)
#define V22IB_OPEN_CMD CONTROLLINO_D5  //(Open Command V22IB)
#define V22IB_CLOSE_CMD CONTROLLINO_R5 //(Close Command V22IB)



// digital pins (INTPUTS)
#define COMPRESSAIR_STATUS    CONTROLLINO_A0    // COMPRESSAIR STATUS
#define V21IB_OPEN_STATUS     CONTROLLINO_A1    // V21IB OPEN STATUS
#define V21IB_CLOSE_STATUS    CONTROLLINO_A2    // V21IB CLOSE STATUS
#define V22IB_OPEN_STATUS     CONTROLLINO_A3    // V22IB OPEN STATUS
#define V22IB_CLOSE_STATUS    CONTROLLINO_A4    // V22IB CLOSE STATUS
#define V21DET_OPEN_STATUS    CONTROLLINO_A5    // V21DET OPEN STATUS
#define V21DET_CLOSE_STATUS   CONTROLLINO_A6    // V21DET CLOSE STATUS
#define V22DET_OPEN_STATUS    CONTROLLINO_A7    // V22DET OPEN STATUS
#define V22DET_CLOSE_STATUS   CONTROLLINO_A8    // V22DET CLOSE STATUS
#define P22DET_STATUS         CONTROLLINO_A9    // P22DET ON/OFF STATUS
#define P22IB_STATUS          CONTROLLINO_IN0   // P22IB ON/OFF STATUS

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
    updateIOFromI2CBool = false;
    uint32_t crc;
    crc = crc_array[0];
    crc = (crc << 8) | crc_array[1];
    crc = (crc << 8) | crc_array[2];
    crc = (crc << 8) | crc_array[3];
 
    // calculate CRC32
    uint32_t crcdata = CRC32::calculate(data_array, 4);
    
    if ( crc == crcdata ) {
       Serial.print(" Checksum good:");Serial.println(crc);
        i2c_buffer = data_array[0];
        i2c_buffer = (i2c_buffer << 8) | data_array[1];
        i2c_buffer = (i2c_buffer << 8) | data_array[2];
        i2c_buffer = (i2c_buffer << 8) | data_array[3];
        
        // Finally update I/O
        UpdateIOFromI2C();
    }
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
  digitalWrite(V21DET_OPEN_CMD,LOW);                           // Set V21DET_OPEN_CMD LOW
  digitalWrite(V21DET_CLOSE_CMD,HIGH);                         // Set V21DET_CLOSE_CMD HIGH
  pinMode(V21DET_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve
  pinMode(V21DET_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit(V21DET_OPEN_CMD_BIT,0x00);                         // Set V21DET_OPEN_CMD_BIT LOW
  I2CsetBit(V21DET_CLOSE_CMD_BIT,0x01);                        // Set V21DET_CLOSE_CMD_BIT HIGH
  digitalWrite(V22DET_OPEN_CMD,LOW);                           // Set V22DET_OPEN_CMD LOW
  digitalWrite(V22DET_CLOSE_CMD,HIGH);                         // Set V22DET_CLOSE_CMD HIGH
  pinMode(V22DET_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve 
  pinMode(V22DET_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit(V22DET_OPEN_CMD_BIT,0x00);                         // Set V22DET_OPEN_CMD_BIT LOW
  I2CsetBit(V22DET_CLOSE_CMD_BIT,0x01);                        // Set V22DET_CLOSE_CMD_BIT HIGH
  digitalWrite(P22DET_ON_CMD,LOW);                             // Set P22DET_ON_CMD LOW
  digitalWrite(P22DET_OFF_CMD,HIGH);                           // Set P22DET_OFF_CMD HIGH
  pinMode(P22DET_ON_CMD, OUTPUT);                              // Set the digital pin as output for Switch ON Scroll
  pinMode(P22DET_OFF_CMD, OUTPUT);                             // Set the digital pin as output for Switch OFF Scroll
  I2CsetBit(P22DET_ON_CMD_BIT,0x00);                           // Set P22DET_ON_CMD_BIT LOW
  I2CsetBit(P22DET_OFF_CMD_BIT,0x01);                          // Set P22DET_OFF_CMD_BIT HIGH
  digitalWrite(V21IB_OPEN_CMD,LOW);                            // Set V21IB_OPEN_CMD LOW
  digitalWrite(V21IB_CLOSE_CMD,HIGH);                          // Set V21IB_CLOSE_CMD HIGH
  pinMode(V21IB_OPEN_CMD, OUTPUT);                             // Set the digital pin as output for Open Valve
  pinMode(V21IB_CLOSE_CMD, OUTPUT);                            // Set the digital pin as output for Close Valve
  I2CsetBit(V21IB_OPEN_CMD_BIT,0x00);                          // Set V21IB_OPEN_CMD_BIT LOW
  I2CsetBit(V21IB_CLOSE_CMD_BIT,0x01);                         // Set V21IB_CLOSE_CMD_BIT HIGH
  digitalWrite(V22IB_OPEN_CMD,LOW);                            // Set V22IB_OPEN_CMD LOW
  digitalWrite(V22IB_CLOSE_CMD,HIGH);                          // Set V22IB_CLOSE_CMD HIGH
  pinMode(V22IB_OPEN_CMD, OUTPUT);                             // Set the digital pin as output for Open Valve 
  pinMode(V22IB_CLOSE_CMD, OUTPUT);                            // Set the digital pin as output for Close Valve
  I2CsetBit(V22IB_OPEN_CMD_BIT,0x00);                          // Set V22IB_OPEN_CMD_BIT LOW
  I2CsetBit(V22IB_CLOSE_CMD_BIT,0x01);                         // Set V22IB_CLOSE_CMD_BIT HIGH
  digitalWrite(P22IB_ON_CMD,LOW);                              // Set P22IB_ON_CMD LOW
  digitalWrite(P22IB_OFF_CMD,HIGH);                            // Set P22IB_OFF_CMD HIGH
  pinMode(P22IB_ON_CMD, OUTPUT);                               // Set the digital pin as output for Switch ON Scroll
  pinMode(P22IB_OFF_CMD, OUTPUT);                              // Set the digital pin as output for Switch OFF Scroll
  I2CsetBit(P22IB_ON_CMD_BIT,0x00);                            // Set P22IB_ON_CMD_BIT LOW
  I2CsetBit(P22IB_OFF_CMD_BIT,0x01);                           // Set P22IB_OFF_CMD_BIT HIGH
  
  // Digital INPUTS assignation
  pinMode(V21DET_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V21DET OPEN STATUS
  pinMode(V21DET_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V21DET CLOSE STATUS
  pinMode(V22DET_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V22DET OPEN STATUS
  pinMode(V22DET_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V22DET CLOSE STATUS
  pinMode(P22DET_STATUS, INPUT);                                 // sets the digital pin as input for Scroll P22DET ON/OFF STATUS
  pinMode(V21IB_OPEN_STATUS, INPUT);                             // sets the digital pin as input for Valve V21IB OPEN STATUS
  pinMode(V21IB_CLOSE_STATUS, INPUT);                            // sets the digital pin as input for Valve V21IB CLOSE STATUS
  pinMode(V22IB_OPEN_STATUS, INPUT);                             // sets the digital pin as input for Valve V22IB OPEN STATUS
  pinMode(V22IB_CLOSE_STATUS, INPUT);                            // sets the digital pin as input for Valve V22IB CLOSE STATUS
  pinMode(P22IB_STATUS, INPUT);                                  // sets the digital pin as input for Scroll P22IB ON/OFF STATUS
  pinMode(COMPRESSAIR_STATUS, INPUT);                            // sets the digital pin as input for COMPRESSAIR OK/KO STATUS  
  
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
  
  if (numbyte == 8) { // Expect 4 bytes of data, update i2c_buffer
    data_array[0] = Wire.read();
    data_array[1] = Wire.read();
    data_array[2] = Wire.read();
    data_array[3] = Wire.read();
    
    // last 4 bytes of data corresponds to CRC32
    
    crc_array[0] = Wire.read();
    crc_array[1] = Wire.read();
    crc_array[2] = Wire.read();
    crc_array[3] = Wire.read();

    updateIOFromI2CBool = true;
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
 *  V21DET Valve Case
 */
  // Reset V21DET_CLOSE_CMD
  if (digitalRead(V21DET_CLOSE_CMD) == LOW && V21DET_RESET == true) {
    if ( millis() - V21DETtime > reset_wait) {
       digitalWrite(V21DET_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(V21DET_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       V21DET_RESET = false;  
    }
  }
  // Reset V21DET_OPEN_CMD
  if (digitalRead(V21DET_OPEN_CMD) == HIGH && V21DET_RESET == true) {
    if ( millis() - V21DETtime > reset_wait) {
       digitalWrite(V21DET_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(V21DET_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       V21DET_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V21DETtime = millis();
       V21DET_CHECK = true; 
    }
  }
  // Check V21DET Close Status
  if (digitalRead(V21DET_CLOSE_STATUS) == HIGH && digitalRead(V21DET_OPEN_STATUS) == LOW && V21DET_CHECK == true) {
    if ( millis() - V21DETtime > check_wait) {
      digitalWrite(V21DET_CLOSE_CMD,LOW);  // CLOSE VALVE
      V21DETtime = millis();
      V21DET_CHECK = false;
      V21DET_RESET = true;
    }
  }
/*
 * V22DET Valve case
 */
  // Reset V22DET_CLOSE_CMD
  if (digitalRead(V22DET_CLOSE_CMD) == LOW && V22DET_RESET == true) {
    if ( millis() - V22DETtime > reset_wait) {
       digitalWrite(V22DET_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(V22DET_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       V22DET_RESET = false;  
    }
  }
  // Reset V22DET_OPEN_CMD
  if (digitalRead(V22DET_OPEN_CMD) == HIGH && V22DET_RESET == true) {
    if ( millis() - V22DETtime > reset_wait) {
       digitalWrite(V22DET_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(V22DET_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       V22DET_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V22DETtime = millis();
       V22DET_CHECK = true; 
    }
  }
  // Check V22DET Close Status
  if (digitalRead(V22DET_CLOSE_STATUS) == HIGH && digitalRead(V22DET_OPEN_STATUS) == LOW && V22DET_CHECK == true) {
    if ( millis() - V22DETtime > check_wait) {
      digitalWrite(V22DET_CLOSE_CMD,LOW);  // CLOSE VALVE
      V22DETtime = millis();
      V22DET_CHECK = false;
      V22DET_RESET = true;
    }
  }
/*
 *  P22DET Case
 */
  // Reset P22DET_OFF_CMD
  if (digitalRead(P22DET_OFF_CMD) == LOW && P22DET_RESET == true) {
    if ( millis() - P22DETtime > reset_wait) {
      digitalWrite(P22DET_OFF_CMD,HIGH);    // RESET SWITCH OFF SCROLL
      I2CsetBit(P22DET_OFF_CMD_BIT,0x01);   // RESET
      P22DET_RESET = false;
    }
  }
  // Reset P22DET_ON_CMD
  if (digitalRead(P22DET_ON_CMD) == HIGH && P22DET_RESET == true) {
    if ( millis() - P22DETtime > reset_wait) {
      digitalWrite(P22DET_ON_CMD,LOW);      // RESET SWITCH ON SCROLL
      I2CsetBit(P22DET_ON_CMD_BIT,0x00);    // RESET
      P22DET_RESET = false;  
    }
  }
/*
 *  V21IB Valve Case
 */
  // Reset V21IB_CLOSE_CMD
  if (digitalRead(V21IB_CLOSE_CMD) == LOW && V21IB_RESET == true) {
    if ( millis() - V21IBtime > reset_wait) {
       digitalWrite(V21IB_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(V21IB_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       V21IB_RESET = false;  
    }
  }
  // Reset V21IB_OPEN_CMD
  if (digitalRead(V21IB_OPEN_CMD) == HIGH && V21IB_RESET == true) {
    if ( millis() - V21IBtime > reset_wait) {
       digitalWrite(V21IB_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(V21IB_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       V21IB_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V21IBtime = millis();
       V21IB_CHECK = true; 
    }
  }
  // Check V21IB Close Status
  if (digitalRead(V21IB_CLOSE_STATUS) == HIGH && digitalRead(V21IB_OPEN_STATUS) == LOW && V21IB_CHECK == true) {
    if ( millis() - V21IBtime > check_wait) {
      digitalWrite(V21IB_CLOSE_CMD,LOW);  // CLOSE VALVE
      V21IBtime = millis();
      V21IB_CHECK = false;
      V21IB_RESET = true;
    }
  }
/*
 * V22IB Valve case
 */
  // Reset V22IB_CLOSE_CMD
  if (digitalRead(V22IB_CLOSE_CMD) == LOW && V22IB_RESET == true) {
    if ( millis() - V22IBtime > reset_wait) {
       digitalWrite(V22IB_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(V22IB_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       V22IB_RESET = false;  
    }
  }
  // Reset V22IB_OPEN_CMD
  if (digitalRead(V22IB_OPEN_CMD) == HIGH && V22IB_RESET == true) {
    if ( millis() - V22IBtime > reset_wait) {
       digitalWrite(V22IB_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(V22IB_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       V22IB_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V22IBtime = millis();
       V22IB_CHECK = true; 
    }
  }
  // Check V22IB Close Status
  if (digitalRead(V22IB_CLOSE_STATUS) == HIGH && digitalRead(V22IB_OPEN_STATUS) == LOW && V22IB_CHECK == true) {
    if ( millis() - V22IBtime > check_wait) {
      digitalWrite(V22IB_CLOSE_CMD,LOW);  // CLOSE VALVE
      V22IBtime = millis();
      V22IB_CHECK = false;
      V22IB_RESET = true;
    }
  }
/*
 *  P22IB Case
 */
  // Reset P22IB_OFF_CMD
  if (digitalRead(P22IB_OFF_CMD) == LOW && P22IB_RESET == true) {
    if ( millis() - P22IBtime > reset_wait) {
      digitalWrite(P22IB_OFF_CMD,HIGH);    // RESET SWITCH OFF SCROLL
      I2CsetBit(P22IB_OFF_CMD_BIT,0x01);   // RESET
      P22IB_RESET = false;
    }
  }
  // Reset P22IB_ON_CMD
  if (digitalRead(P22IB_ON_CMD) == HIGH && P22IB_RESET == true) {
    if ( millis() - P22IBtime > reset_wait) {
      digitalWrite(P22IB_ON_CMD,LOW);      // RESET SWITCH ON SCROLL
      I2CsetBit(P22IB_ON_CMD_BIT,0x00);    // RESET
      P22IB_RESET = false;  
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
  /* Update Valve V21DET position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V21DET_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V21DET_OPEN_CMD_BIT) == 0x00 && V21DET_RESET == false) {
     digitalWrite(V21DET_CLOSE_CMD,LOW);   // CLOSE VALVE
     V21DETtime = millis();
     V21DET_RESET = true;
  }
  else if (bitRead(i2c_buffer,V21DET_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V21DET_CLOSE_CMD_BIT) == 0x01 && V21DET_RESET == false) {
     digitalWrite(V21DET_OPEN_CMD,HIGH);   // OPEN VALVE
     V21DETtime = millis();
     V21DET_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve V22DET position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V22DET_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V22DET_OPEN_CMD_BIT) == 0x00 && V22DET_RESET == false) {
     digitalWrite(V22DET_CLOSE_CMD,LOW);   // CLOSE VALVE
     V22DETtime = millis();
     V22DET_RESET = true;
  }
  else if (bitRead(i2c_buffer,V22DET_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V22DET_CLOSE_CMD_BIT) == 0x01 && V22DET_RESET == false) {
     digitalWrite(V22DET_OPEN_CMD,HIGH);   // OPEN VALVE
     V22DETtime = millis();
     V22DET_RESET = true;
  }
  
  /***********************************************************************************************************/
  /* Update Scroll P22DET position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,P22DET_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,P22DET_ON_CMD_BIT) == 0x00 && P22DET_RESET == false) {
     digitalWrite(P22DET_OFF_CMD,LOW);     // SWITCH OFF SCROLL
     P22DETtime = millis();
     P22DET_RESET = true; 
  }
  else if (bitRead(i2c_buffer,P22DET_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,P22DET_OFF_CMD_BIT) == 0x01 && P22DET_RESET == false) { 
     digitalWrite(P22DET_ON_CMD,HIGH);     // SWITCH ON SCROLL
     P22DETtime = millis();
     P22DET_RESET = true; 
  }
  /***********************************************************************************************************/
  /* Update Valve V21IB position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V21IB_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V21IB_OPEN_CMD_BIT) == 0x00 && V21IB_RESET == false) {
     digitalWrite(V21IB_CLOSE_CMD,LOW);   // CLOSE VALVE
     V21IBtime = millis();
     V21IB_RESET = true;
  }
  else if (bitRead(i2c_buffer,V21IB_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V21IB_CLOSE_CMD_BIT) == 0x01 && V21IB_RESET == false) {
     digitalWrite(V21IB_OPEN_CMD,HIGH);   // OPEN VALVE
     V21IBtime = millis();
     V21IB_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve V22IB position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V22IB_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V22IB_OPEN_CMD_BIT) == 0x00 && V22IB_RESET == false) {
     digitalWrite(V22IB_CLOSE_CMD,LOW);   // CLOSE VALVE
     V22IBtime = millis();
     V22IB_RESET = true;
  }
  else if (bitRead(i2c_buffer,V22IB_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V22IB_CLOSE_CMD_BIT) == 0x01 && V22IB_RESET == false) {
     digitalWrite(V22IB_OPEN_CMD,HIGH);   // OPEN VALVE
     V22IBtime = millis();
     V22IB_RESET = true;
  }
  
  /***********************************************************************************************************/
  /* Update Scroll P22IB position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,P22IB_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,P22IB_ON_CMD_BIT) == 0x00 && P22IB_RESET == false) {
     digitalWrite(P22IB_OFF_CMD,LOW);     // SWITCH OFF SCROLL
     P22IBtime = millis();
     P22IB_RESET = true; 
  }
  else if (bitRead(i2c_buffer,P22IB_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,P22IB_OFF_CMD_BIT) == 0x01 && P22IB_RESET == false) { 
     digitalWrite(P22IB_ON_CMD,HIGH);     // SWITCH ON SCROLL
     P22IBtime = millis();
     P22IB_RESET = true; 
  }
}

void UpdateI2CFromIO()
{ 
  /***********************************************************************************************************/
  /* Update V21DET Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V21DET_OPEN_STATUS) == HIGH && digitalRead(V21DET_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V21DET_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V21DET_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V21DET_CLOSE_STATUS) == HIGH && digitalRead(V21DET_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (bitRead(i2c_buffer,V21DET_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21DET_CLOSE_STATUS_BIT) == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V21DET_CLOSE_CMD,LOW);  // CLOSE VALVE
        V21DETtime = millis();
        V21DET_RESET = true;
     }
     I2CsetBit(V21DET_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V21DET_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V21DET_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V21DET_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update V22DET Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V22DET_OPEN_STATUS) == HIGH && digitalRead(V22DET_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V22DET_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V22DET_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V22DET_CLOSE_STATUS) == HIGH && digitalRead(V22DET_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (bitRead(i2c_buffer,V22DET_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22DET_CLOSE_STATUS_BIT) == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V22DET_CLOSE_CMD,LOW);  // CLOSE VALVE
        V22DETtime = millis();
        V22DET_RESET = true;
     }
     I2CsetBit(V22DET_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V22DET_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V22DET_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V22DET_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update P22DET Scroll position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(P22DET_STATUS) == HIGH) {    // SCROLL ON STATUS
     I2CsetBit(P22DET_STATUS_BIT,0x01);        // UPDATE SCROLL ON BIT
  }
  else if (digitalRead(P22DET_STATUS) == LOW) { // SCROLL OFF STATUS
     I2CsetBit(P22DET_STATUS_BIT,0x00);         // UPDATE SCROLL OFF BIT
  }
  /***********************************************************************************************************/
  /* Update V21IB Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V21IB_OPEN_STATUS) == HIGH && digitalRead(V21IB_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V21IB_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V21IB_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V21IB_CLOSE_STATUS) == HIGH && digitalRead(V21IB_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (bitRead(i2c_buffer,V21IB_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21IB_CLOSE_STATUS_BIT) == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V21IB_CLOSE_CMD,LOW);  // CLOSE VALVE
        V21IBtime = millis();
        V21IB_RESET = true;
     }
     I2CsetBit(V21IB_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V21IB_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V21IB_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V21IB_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update V22IB Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V22IB_OPEN_STATUS) == HIGH && digitalRead(V22IB_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V22IB_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V22IB_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V22IB_CLOSE_STATUS) == HIGH && digitalRead(V22IB_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (bitRead(i2c_buffer,V22IB_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22IB_CLOSE_STATUS_BIT) == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V22IB_CLOSE_CMD,LOW);  // CLOSE VALVE
        V22IBtime = millis();
        V22IB_RESET = true;
     }
     I2CsetBit(V22IB_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V22IB_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V22IB_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V22IB_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update P22IB Scroll position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(P22IB_STATUS) == HIGH) {    // SCROLL ON STATUS
     I2CsetBit(P22IB_STATUS_BIT,0x01);        // UPDATE SCROLL ON BIT
  }
  else if (digitalRead(P22IB_STATUS) == LOW) { // SCROLL OFF STATUS
     I2CsetBit(P22IB_STATUS_BIT,0x00);         // UPDATE SCROLL OFF BIT
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
