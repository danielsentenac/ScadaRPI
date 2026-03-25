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
unsigned long V21Atime = 0;
unsigned long V22Atime = 0;
unsigned long P22Atime = 0;
unsigned long V21Btime = 0;
unsigned long V22Btime = 0;
unsigned long P22Btime = 0;
unsigned long BYPASStime = 0;
unsigned long looptime = 0;


/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean V21A_RESET = false;
boolean V22A_RESET = false;
boolean P22A_RESET = false;
boolean V21B_RESET = false;
boolean V22B_RESET = false;
boolean P22B_RESET = false;
boolean BYPASS_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 5000;

 /*
 *  These CHECK are used to check switches status (Status inputs)
 */
boolean V21A_CHECK = false;
boolean V22A_CHECK = false;
boolean V21B_CHECK = false;
boolean V22B_CHECK = false;

/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;
 
/*
 *  I2C BIT ASSIGNATION (MAX = 32) 
 */
#define V21A_OPEN_CMD_BIT             0   // V21_A Open bit
#define V21A_CLOSE_CMD_BIT            1   // V21_A Close bit
#define V21A_OPEN_STATUS_BIT          2   // V21_A Open Status bit
#define V21A_CLOSE_STATUS_BIT         3   // V21_A Close Status bit
#define V22A_OPEN_CMD_BIT             4   // V22_A Open bit
#define V22A_CLOSE_CMD_BIT            5   // V22_A Close bit
#define V22A_OPEN_STATUS_BIT          6   // V22_A Open Status bit
#define V22A_CLOSE_STATUS_BIT         7   // V22_A Close Status bit
#define V21B_OPEN_CMD_BIT             8   // V21_B Open bit
#define V21B_CLOSE_CMD_BIT            9   // V21_B Close bit
#define V21B_OPEN_STATUS_BIT          10  // V21_B Open Status bit
#define V21B_CLOSE_STATUS_BIT         11  // V21_B Close Status bit
#define V22B_OPEN_CMD_BIT             12  // V22_B Open bit
#define V22B_CLOSE_CMD_BIT            13  // V22_B Close bit
#define V22B_OPEN_STATUS_BIT          14  // V22_B Open Status bit
#define V22B_CLOSE_STATUS_BIT         15  // V22_B Close Status bit
#define P22A_ON_CMD_BIT               16  // P22_A On bit
#define P22A_OFF_CMD_BIT              17  // P22_A Off bit
#define P22A_STATUS_BIT               18  // P22_A Status bit
#define P22B_ON_CMD_BIT               19  // P22_B On bit
#define P22B_OFF_CMD_BIT              20  // P22_B Off bit
#define P22B_STATUS_BIT               21  // P22_B Status bit
#define AIRPRESSURE_STATUS_BIT        22  // Air pressure status bit
#define BYPASS_START_CMD_BIT          23  // BYPASS Start bit
#define BYPASS_STOP_CMD_BIT           24  // BYPASS Stop bit
#define BYPASS_STATUS_BIT             25  // BYPASS Status bit

#define ARD_RESET_BIT                 31  // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */ 
// digital pins (OUTPUTS)
#define P22A_ON_CMD       CONTROLLINO_D0  //(Command Switch ON Scroll P22_A)
#define P22A_OFF_CMD      CONTROLLINO_R0  //(Command Switch OFF Scroll P22_A)
#define V21A_OPEN_CMD     CONTROLLINO_D1  //(Open Command V21_A)
#define V21A_CLOSE_CMD    CONTROLLINO_R1  //(Close Command V21_A)
#define V22A_OPEN_CMD     CONTROLLINO_D2  //(Open Command V22_A)
#define V22A_CLOSE_CMD    CONTROLLINO_R2  //(Close Command V22_A)
#define P22B_ON_CMD       CONTROLLINO_D3  //(Command Switch ON Scroll P22_B)
#define P22B_OFF_CMD      CONTROLLINO_R3  //(Command Switch OFF Scroll P22_B)
#define V21B_OPEN_CMD     CONTROLLINO_D4  //(Open Command V21_B)
#define V21B_CLOSE_CMD    CONTROLLINO_R4  //(Close Command V21_B)
#define V22B_OPEN_CMD     CONTROLLINO_D5  //(Open Command V22_B)
#define V22B_CLOSE_CMD    CONTROLLINO_R5  //(Close Command V22_B)
#define BYPASS_START_CMD  CONTROLLINO_D9  //(Start Command BYPASS)
#define BYPASS_STOP_CMD   CONTROLLINO_R9  //(Stop Command BYPASS)



// digital pins (INTPUTS)
#define AIRPRESSURE_STATUS  CONTROLLINO_A0   // AIR PRESSURE STATUS
#define V21A_OPEN_STATUS    CONTROLLINO_A1   // V21_A OPEN STATUS
#define V21A_CLOSE_STATUS   CONTROLLINO_A2   // V21_A CLOSE STATUS
#define V22A_OPEN_STATUS    CONTROLLINO_A3   // V22_A OPEN STATUS
#define V22A_CLOSE_STATUS   CONTROLLINO_A4   // V22_A CLOSE STATUS
#define V21B_OPEN_STATUS    CONTROLLINO_A5   // V21_B OPEN STATUS
#define V21B_CLOSE_STATUS   CONTROLLINO_A6   // V21_B CLOSE STATUS
#define V22B_OPEN_STATUS    CONTROLLINO_A7   // V22_B OPEN STATUS
#define V22B_CLOSE_STATUS   CONTROLLINO_A8   // V22_B CLOSE STATUS
#define P22A_STATUS         CONTROLLINO_A9   // P22_A ON/OFF STATUS
#define P22B_STATUS         CONTROLLINO_IN0  // P22_B ON/OFF STATUS
#define BYPASS_STATUS       CONTROLLINO_IN1  // BYPASS STATUS

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
  digitalWrite(V21A_OPEN_CMD,LOW);                             // Set V21_A OPEN low
  digitalWrite(V21A_CLOSE_CMD,HIGH);                           // Set V21_A CLOSE high
  pinMode(V21A_OPEN_CMD, OUTPUT);                              // Set the digital pin as output for Open Valve
  pinMode(V21A_CLOSE_CMD, OUTPUT);                             // Set the digital pin as output for Close Valve
  I2CsetBit(V21A_OPEN_CMD_BIT,0x00);                           // Set V21_A OPEN bit low
  I2CsetBit(V21A_CLOSE_CMD_BIT,0x01);                          // Set V21_A CLOSE bit high

  digitalWrite(V22A_OPEN_CMD,LOW);                             // Set V22_A OPEN low
  digitalWrite(V22A_CLOSE_CMD,HIGH);                           // Set V22_A CLOSE high
  pinMode(V22A_OPEN_CMD, OUTPUT);                              // Set the digital pin as output for Open Valve
  pinMode(V22A_CLOSE_CMD, OUTPUT);                             // Set the digital pin as output for Close Valve
  I2CsetBit(V22A_OPEN_CMD_BIT,0x00);                           // Set V22_A OPEN bit low
  I2CsetBit(V22A_CLOSE_CMD_BIT,0x01);                          // Set V22_A CLOSE bit high

  digitalWrite(P22A_ON_CMD,LOW);                               // Set P22_A ON low
  digitalWrite(P22A_OFF_CMD,HIGH);                             // Set P22_A OFF high
  pinMode(P22A_ON_CMD, OUTPUT);                                // Set the digital pin as output for Switch ON Scroll
  pinMode(P22A_OFF_CMD, OUTPUT);                               // Set the digital pin as output for Switch OFF Scroll
  I2CsetBit(P22A_ON_CMD_BIT,0x00);                             // Set P22_A ON bit low
  I2CsetBit(P22A_OFF_CMD_BIT,0x01);                            // Set P22_A OFF bit high

  digitalWrite(V21B_OPEN_CMD,LOW);                             // Set V21_B OPEN low
  digitalWrite(V21B_CLOSE_CMD,HIGH);                           // Set V21_B CLOSE high
  pinMode(V21B_OPEN_CMD, OUTPUT);                              // Set the digital pin as output for Open Valve
  pinMode(V21B_CLOSE_CMD, OUTPUT);                             // Set the digital pin as output for Close Valve
  I2CsetBit(V21B_OPEN_CMD_BIT,0x00);                           // Set V21_B OPEN bit low
  I2CsetBit(V21B_CLOSE_CMD_BIT,0x01);                          // Set V21_B CLOSE bit high

  digitalWrite(V22B_OPEN_CMD,LOW);                             // Set V22_B OPEN low
  digitalWrite(V22B_CLOSE_CMD,HIGH);                           // Set V22_B CLOSE high
  pinMode(V22B_OPEN_CMD, OUTPUT);                              // Set the digital pin as output for Open Valve
  pinMode(V22B_CLOSE_CMD, OUTPUT);                             // Set the digital pin as output for Close Valve
  I2CsetBit(V22B_OPEN_CMD_BIT,0x00);                           // Set V22_B OPEN bit low
  I2CsetBit(V22B_CLOSE_CMD_BIT,0x01);                          // Set V22_B CLOSE bit high

  digitalWrite(P22B_ON_CMD,LOW);                               // Set P22_B ON low
  digitalWrite(P22B_OFF_CMD,HIGH);                             // Set P22_B OFF high
  pinMode(P22B_ON_CMD, OUTPUT);                                // Set the digital pin as output for Switch ON Scroll
  pinMode(P22B_OFF_CMD, OUTPUT);                               // Set the digital pin as output for Switch OFF Scroll
  I2CsetBit(P22B_ON_CMD_BIT,0x00);                             // Set P22_B ON bit low
  I2CsetBit(P22B_OFF_CMD_BIT,0x01);                            // Set P22_B OFF bit high

  digitalWrite(BYPASS_START_CMD,LOW);                          // Set BYPASS START low
  digitalWrite(BYPASS_STOP_CMD,HIGH);                          // Set BYPASS STOP high
  pinMode(BYPASS_START_CMD, OUTPUT);                           // Set the digital pin as output for BYPASS START
  pinMode(BYPASS_STOP_CMD, OUTPUT);                            // Set the digital pin as output for BYPASS STOP
  I2CsetBit(BYPASS_START_CMD_BIT,0x00);                        // Set BYPASS START bit low
  I2CsetBit(BYPASS_STOP_CMD_BIT,0x01);                         // Set BYPASS STOP bit high
  
  // Digital INPUTS assignation
  pinMode(V21A_OPEN_STATUS, INPUT);                              // sets the digital pin as input for Valve V21_A OPEN STATUS
  pinMode(V21A_CLOSE_STATUS, INPUT);                             // sets the digital pin as input for Valve V21_A CLOSE STATUS
  pinMode(V22A_OPEN_STATUS, INPUT);                              // sets the digital pin as input for Valve V22_A OPEN STATUS
  pinMode(V22A_CLOSE_STATUS, INPUT);                             // sets the digital pin as input for Valve V22_A CLOSE STATUS
  pinMode(P22A_STATUS, INPUT);                                   // sets the digital pin as input for Scroll P22_A ON/OFF STATUS
  pinMode(V21B_OPEN_STATUS, INPUT);                              // sets the digital pin as input for Valve V21_B OPEN STATUS
  pinMode(V21B_CLOSE_STATUS, INPUT);                             // sets the digital pin as input for Valve V21_B CLOSE STATUS
  pinMode(V22B_OPEN_STATUS, INPUT);                              // sets the digital pin as input for Valve V22_B OPEN STATUS
  pinMode(V22B_CLOSE_STATUS, INPUT);                             // sets the digital pin as input for Valve V22_B CLOSE STATUS
  pinMode(P22B_STATUS, INPUT);                                   // sets the digital pin as input for Scroll P22_B ON/OFF STATUS
  pinMode(BYPASS_STATUS, INPUT);                                 // sets the digital pin as input for BYPASS STATUS
  pinMode(AIRPRESSURE_STATUS, INPUT);                            // sets the digital pin as input for AIR PRESSURE STATUS
  
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
  Serial.println("Receive request by master");
  
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
   *  V21_A Valve Case
   */
  if (digitalRead(V21A_CLOSE_CMD) == LOW && V21A_RESET == true) {
    if ( millis() - V21Atime > reset_wait) {
       digitalWrite(V21A_CLOSE_CMD,HIGH);
       I2CsetBit(V21A_CLOSE_CMD_BIT,0x01);
       V21A_RESET = false;
    }
  }
  if (digitalRead(V21A_OPEN_CMD) == HIGH && V21A_RESET == true) {
    if ( millis() - V21Atime > reset_wait) {
       digitalWrite(V21A_OPEN_CMD,LOW);
       I2CsetBit(V21A_OPEN_CMD_BIT,0x00);
       V21A_RESET = false;
       V21Atime = millis();
       V21A_CHECK = true;
    }
  }
  if (digitalRead(V21A_CLOSE_STATUS) == HIGH && digitalRead(V21A_OPEN_STATUS) == LOW && V21A_CHECK == true) {
    if ( millis() - V21Atime > check_wait) {
      digitalWrite(V21A_CLOSE_CMD,LOW);
      V21Atime = millis();
      V21A_CHECK = false;
      V21A_RESET = true;
    }
  }

  /*
   *  V22_A Valve Case
   */
  if (digitalRead(V22A_CLOSE_CMD) == LOW && V22A_RESET == true) {
    if ( millis() - V22Atime > reset_wait) {
       digitalWrite(V22A_CLOSE_CMD,HIGH);
       I2CsetBit(V22A_CLOSE_CMD_BIT,0x01);
       V22A_RESET = false;
    }
  }
  if (digitalRead(V22A_OPEN_CMD) == HIGH && V22A_RESET == true) {
    if ( millis() - V22Atime > reset_wait) {
       digitalWrite(V22A_OPEN_CMD,LOW);
       I2CsetBit(V22A_OPEN_CMD_BIT,0x00);
       V22A_RESET = false;
       V22Atime = millis();
       V22A_CHECK = true;
    }
  }
  if (digitalRead(V22A_CLOSE_STATUS) == HIGH && digitalRead(V22A_OPEN_STATUS) == LOW && V22A_CHECK == true) {
    if ( millis() - V22Atime > check_wait) {
      digitalWrite(V22A_CLOSE_CMD,LOW);
      V22Atime = millis();
      V22A_CHECK = false;
      V22A_RESET = true;
    }
  }

  /*
   *  P22_A Case
   */
  if (digitalRead(P22A_OFF_CMD) == LOW && P22A_RESET == true) {
    if ( millis() - P22Atime > reset_wait) {
      digitalWrite(P22A_OFF_CMD,HIGH);
      I2CsetBit(P22A_OFF_CMD_BIT,0x01);
      P22A_RESET = false;
    }
  }
  if (digitalRead(P22A_ON_CMD) == HIGH && P22A_RESET == true) {
    if ( millis() - P22Atime > reset_wait) {
      digitalWrite(P22A_ON_CMD,LOW);
      I2CsetBit(P22A_ON_CMD_BIT,0x00);
      P22A_RESET = false;
    }
  }

  /*
   *  V21_B Valve Case
   */
  if (digitalRead(V21B_CLOSE_CMD) == LOW && V21B_RESET == true) {
    if ( millis() - V21Btime > reset_wait) {
       digitalWrite(V21B_CLOSE_CMD,HIGH);
       I2CsetBit(V21B_CLOSE_CMD_BIT,0x01);
       V21B_RESET = false;
    }
  }
  if (digitalRead(V21B_OPEN_CMD) == HIGH && V21B_RESET == true) {
    if ( millis() - V21Btime > reset_wait) {
       digitalWrite(V21B_OPEN_CMD,LOW);
       I2CsetBit(V21B_OPEN_CMD_BIT,0x00);
       V21B_RESET = false;
       V21Btime = millis();
       V21B_CHECK = true;
    }
  }
  if (digitalRead(V21B_CLOSE_STATUS) == HIGH && digitalRead(V21B_OPEN_STATUS) == LOW && V21B_CHECK == true) {
    if ( millis() - V21Btime > check_wait) {
      digitalWrite(V21B_CLOSE_CMD,LOW);
      V21Btime = millis();
      V21B_CHECK = false;
      V21B_RESET = true;
    }
  }

  /*
   *  V22_B Valve Case
   */
  if (digitalRead(V22B_CLOSE_CMD) == LOW && V22B_RESET == true) {
    if ( millis() - V22Btime > reset_wait) {
       digitalWrite(V22B_CLOSE_CMD,HIGH);
       I2CsetBit(V22B_CLOSE_CMD_BIT,0x01);
       V22B_RESET = false;
    }
  }
  if (digitalRead(V22B_OPEN_CMD) == HIGH && V22B_RESET == true) {
    if ( millis() - V22Btime > reset_wait) {
       digitalWrite(V22B_OPEN_CMD,LOW);
       I2CsetBit(V22B_OPEN_CMD_BIT,0x00);
       V22B_RESET = false;
       V22Btime = millis();
       V22B_CHECK = true;
    }
  }
  if (digitalRead(V22B_CLOSE_STATUS) == HIGH && digitalRead(V22B_OPEN_STATUS) == LOW && V22B_CHECK == true) {
    if ( millis() - V22Btime > check_wait) {
      digitalWrite(V22B_CLOSE_CMD,LOW);
      V22Btime = millis();
      V22B_CHECK = false;
      V22B_RESET = true;
    }
  }

  /*
   *  P22_B Case
   */
  if (digitalRead(P22B_OFF_CMD) == LOW && P22B_RESET == true) {
    if ( millis() - P22Btime > reset_wait) {
      digitalWrite(P22B_OFF_CMD,HIGH);
      I2CsetBit(P22B_OFF_CMD_BIT,0x01);
      P22B_RESET = false;
    }
  }
  if (digitalRead(P22B_ON_CMD) == HIGH && P22B_RESET == true) {
    if ( millis() - P22Btime > reset_wait) {
      digitalWrite(P22B_ON_CMD,LOW);
      I2CsetBit(P22B_ON_CMD_BIT,0x00);
      P22B_RESET = false;
    }
  }

  /*
   *  BYPASS Case
   */
  if (digitalRead(BYPASS_STOP_CMD) == LOW && BYPASS_RESET == true) {
    if ( millis() - BYPASStime > reset_wait) {
      digitalWrite(BYPASS_STOP_CMD,HIGH);
      I2CsetBit(BYPASS_STOP_CMD_BIT,0x01);
      BYPASS_RESET = false;
    }
  }
  if (digitalRead(BYPASS_START_CMD) == HIGH && BYPASS_RESET == true) {
    if ( millis() - BYPASStime > reset_wait) {
      digitalWrite(BYPASS_START_CMD,LOW);
      I2CsetBit(BYPASS_START_CMD_BIT,0x00);
      BYPASS_RESET = false;
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
  /* Update Valve V21_A position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V21A_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V21A_OPEN_CMD_BIT) == 0x00 && V21A_RESET == false) {
     digitalWrite(V21A_CLOSE_CMD,LOW);
     V21Atime = millis();
     V21A_RESET = true;
  }
  else if (bitRead(i2c_buffer,V21A_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V21A_CLOSE_CMD_BIT) == 0x01 && V21A_RESET == false) {
     digitalWrite(V21A_OPEN_CMD,HIGH);
     V21Atime = millis();
     V21A_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve V22_A position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V22A_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V22A_OPEN_CMD_BIT) == 0x00 && V22A_RESET == false) {
     digitalWrite(V22A_CLOSE_CMD,LOW);
     V22Atime = millis();
     V22A_RESET = true;
  }
  else if (bitRead(i2c_buffer,V22A_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V22A_CLOSE_CMD_BIT) == 0x01 && V22A_RESET == false) {
     digitalWrite(V22A_OPEN_CMD,HIGH);
     V22Atime = millis();
     V22A_RESET = true;
  }
  
  /***********************************************************************************************************/
  /* Update Scroll P22_A position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,P22A_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,P22A_ON_CMD_BIT) == 0x00 && P22A_RESET == false) {
     digitalWrite(P22A_OFF_CMD,LOW);
     P22Atime = millis();
     P22A_RESET = true; 
  }
  else if (bitRead(i2c_buffer,P22A_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,P22A_OFF_CMD_BIT) == 0x01 && P22A_RESET == false) {
     digitalWrite(P22A_ON_CMD,HIGH);
     P22Atime = millis();
     P22A_RESET = true; 
  }
  /***********************************************************************************************************/
  /* Update Valve V21_B position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V21B_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V21B_OPEN_CMD_BIT) == 0x00 && V21B_RESET == false) {
     digitalWrite(V21B_CLOSE_CMD,LOW);
     V21Btime = millis();
     V21B_RESET = true;
  }
  else if (bitRead(i2c_buffer,V21B_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V21B_CLOSE_CMD_BIT) == 0x01 && V21B_RESET == false) {
     digitalWrite(V21B_OPEN_CMD,HIGH);
     V21Btime = millis();
     V21B_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve V22_B position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,V22B_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V22B_OPEN_CMD_BIT) == 0x00 && V22B_RESET == false) {
     digitalWrite(V22B_CLOSE_CMD,LOW);
     V22Btime = millis();
     V22B_RESET = true;
  }
  else if (bitRead(i2c_buffer,V22B_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V22B_CLOSE_CMD_BIT) == 0x01 && V22B_RESET == false) {
     digitalWrite(V22B_OPEN_CMD,HIGH);
     V22Btime = millis();
     V22B_RESET = true;
  }
  
  /***********************************************************************************************************/
  /* Update Scroll P22_B position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,P22B_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,P22B_ON_CMD_BIT) == 0x00 && P22B_RESET == false) {
     digitalWrite(P22B_OFF_CMD,LOW);
     P22Btime = millis();
     P22B_RESET = true; 
  }
  else if (bitRead(i2c_buffer,P22B_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,P22B_OFF_CMD_BIT) == 0x01 && P22B_RESET == false) {
     digitalWrite(P22B_ON_CMD,HIGH);
     P22Btime = millis();
     P22B_RESET = true; 
  }

  /***********************************************************************************************************/
  /* Update BYPASS position (Start/Stop) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,BYPASS_STOP_CMD_BIT) == 0x00 && bitRead(i2c_buffer,BYPASS_START_CMD_BIT) == 0x00 && BYPASS_RESET == false) {
     digitalWrite(BYPASS_STOP_CMD,LOW);
     BYPASStime = millis();
     BYPASS_RESET = true;
  }
  else if (bitRead(i2c_buffer,BYPASS_START_CMD_BIT) == 0x01 && bitRead(i2c_buffer,BYPASS_STOP_CMD_BIT) == 0x01 && BYPASS_RESET == false) {
     digitalWrite(BYPASS_START_CMD,HIGH);
     BYPASStime = millis();
     BYPASS_RESET = true;
  }
}

void UpdateI2CFromIO()
{ 
  /***********************************************************************************************************/
  /* Update V21_A Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V21A_OPEN_STATUS) == HIGH && digitalRead(V21A_CLOSE_STATUS) == LOW) {
     I2CsetBit(V21A_OPEN_STATUS_BIT,0x01);
     I2CsetBit(V21A_CLOSE_STATUS_BIT,0x00);
  }
  else if (digitalRead(V21A_CLOSE_STATUS) == HIGH && digitalRead(V21A_OPEN_STATUS) == LOW) {
     if (bitRead(i2c_buffer,V21A_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21A_CLOSE_STATUS_BIT) == 0x00) {
        // reset close command
        digitalWrite(V21A_CLOSE_CMD,LOW);
        V21Atime = millis();
        V21A_RESET = true;
     }
     I2CsetBit(V21A_OPEN_STATUS_BIT,0x00);
     I2CsetBit(V21A_CLOSE_STATUS_BIT,0x01);
  }
  else {
     I2CsetBit(V21A_OPEN_STATUS_BIT,0x00);
     I2CsetBit(V21A_CLOSE_STATUS_BIT,0x00);
  }
  /***********************************************************************************************************/
  /* Update V22_A Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V22A_OPEN_STATUS) == HIGH && digitalRead(V22A_CLOSE_STATUS) == LOW) {
     I2CsetBit(V22A_OPEN_STATUS_BIT,0x01);
     I2CsetBit(V22A_CLOSE_STATUS_BIT,0x00);
  }
  else if (digitalRead(V22A_CLOSE_STATUS) == HIGH && digitalRead(V22A_OPEN_STATUS) == LOW) {
     if (bitRead(i2c_buffer,V22A_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22A_CLOSE_STATUS_BIT) == 0x00) {
        // reset close command
        digitalWrite(V22A_CLOSE_CMD,LOW);
        V22Atime = millis();
        V22A_RESET = true;
     }
     I2CsetBit(V22A_OPEN_STATUS_BIT,0x00);
     I2CsetBit(V22A_CLOSE_STATUS_BIT,0x01);
  }
  else {
     I2CsetBit(V22A_OPEN_STATUS_BIT,0x00);
     I2CsetBit(V22A_CLOSE_STATUS_BIT,0x00);
  }
  /***********************************************************************************************************/
  /* Update P22_A Scroll position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(P22A_STATUS) == HIGH) {
     I2CsetBit(P22A_STATUS_BIT,0x01);
  }
  else if (digitalRead(P22A_STATUS) == LOW) {
     I2CsetBit(P22A_STATUS_BIT,0x00);
  }
  /***********************************************************************************************************/
  /* Update V21_B Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V21B_OPEN_STATUS) == HIGH && digitalRead(V21B_CLOSE_STATUS) == LOW) {
     I2CsetBit(V21B_OPEN_STATUS_BIT,0x01);
     I2CsetBit(V21B_CLOSE_STATUS_BIT,0x00);
  }
  else if (digitalRead(V21B_CLOSE_STATUS) == HIGH && digitalRead(V21B_OPEN_STATUS) == LOW) {
     if (bitRead(i2c_buffer,V21B_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21B_CLOSE_STATUS_BIT) == 0x00) {
        // reset close command
        digitalWrite(V21B_CLOSE_CMD,LOW);
        V21Btime = millis();
        V21B_RESET = true;
     }
     I2CsetBit(V21B_OPEN_STATUS_BIT,0x00);
     I2CsetBit(V21B_CLOSE_STATUS_BIT,0x01);
  }
  else {
     I2CsetBit(V21B_OPEN_STATUS_BIT,0x00);
     I2CsetBit(V21B_CLOSE_STATUS_BIT,0x00);
  }
  /***********************************************************************************************************/
  /* Update V22_B Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V22B_OPEN_STATUS) == HIGH && digitalRead(V22B_CLOSE_STATUS) == LOW) {
     I2CsetBit(V22B_OPEN_STATUS_BIT,0x01);
     I2CsetBit(V22B_CLOSE_STATUS_BIT,0x00);
  }
  else if (digitalRead(V22B_CLOSE_STATUS) == HIGH && digitalRead(V22B_OPEN_STATUS) == LOW) {
     if (bitRead(i2c_buffer,V22B_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22B_CLOSE_STATUS_BIT) == 0x00) {
        // reset close command
        digitalWrite(V22B_CLOSE_CMD,LOW);
        V22Btime = millis();
        V22B_RESET = true;
     }
     I2CsetBit(V22B_OPEN_STATUS_BIT,0x00);
     I2CsetBit(V22B_CLOSE_STATUS_BIT,0x01);
  }
  else {
     I2CsetBit(V22B_OPEN_STATUS_BIT,0x00);
     I2CsetBit(V22B_CLOSE_STATUS_BIT,0x00);
  }
  /***********************************************************************************************************/
  /* Update P22_B Scroll position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(P22B_STATUS) == HIGH) {
     I2CsetBit(P22B_STATUS_BIT,0x01);
  }
  else if (digitalRead(P22B_STATUS) == LOW) {
     I2CsetBit(P22B_STATUS_BIT,0x00);
  }
  /***********************************************************************************************************/
  /* Update AIR PRESSURE position STATUS bit */
  /***********************************************************************************************************/
  if (digitalRead(AIRPRESSURE_STATUS) == HIGH) {
     I2CsetBit(AIRPRESSURE_STATUS_BIT,0x01);
  }
  else if (digitalRead(AIRPRESSURE_STATUS) == LOW) {
     I2CsetBit(AIRPRESSURE_STATUS_BIT,0x00);
  }

  /***********************************************************************************************************/
  /* Update BYPASS position STATUS bit */
  /***********************************************************************************************************/
  if (digitalRead(BYPASS_STATUS) == HIGH) {
     I2CsetBit(BYPASS_STATUS_BIT,0x01);
  }
  else if (digitalRead(BYPASS_STATUS) == LOW) {
     I2CsetBit(BYPASS_STATUS_BIT,0x00);
  }

}
