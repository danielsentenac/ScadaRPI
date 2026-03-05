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
unsigned long VDRYERtime  = 0;
unsigned long V1time      = 0;
unsigned long P1time      = 0;
unsigned long VBYPASStime = 0;
unsigned long VRPtime     = 0;
unsigned long VMAINtime   = 0;
unsigned long VSOFTtime   = 0;
unsigned long looptime    = 0;


/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean VDRYER_RESET  = false;
boolean V1_RESET      = false;
boolean P1_RESET      = false;
boolean VBYPASS_RESET = false;
boolean VRP_RESET     = false;
boolean VMAIN_RESET   = false;
boolean VSOFT_RESET   = false;


/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;

 /*
 *  These CHECK are used to check switches status (Status inputs)
 */
boolean VDRYER_CHECK = false;
boolean V1_CHECK = false;
boolean P1_CHECK = false;
boolean VBYPASS_CHECK = false;
boolean VRP_CHECK = false;
boolean VMAIN_CHECK = false;
boolean VSOFT_CHECK = false;

/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;
 
/*
 *  I2C BIT ASSIGNATION (MAX = 32) 
 */
#define VBYPASS_OPEN_CMD_BIT            0   // VBYPASS Open bit
#define VBYPASS_CLOSE_CMD_BIT           1   // VBYPASS Close bit
#define VBYPASS_OPEN_STATUS_BIT         2   // VBYPASS Open Status bit
#define VBYPASS_CLOSE_STATUS_BIT        3   // VBYPASS Close Status bit
#define VRP_OPEN_CMD_BIT                4   // VRP Open bit
#define VRP_CLOSE_CMD_BIT               5   // VRP Close bit
#define VRP_OPEN_STATUS_BIT             6   // VRP Open Status bit
#define VRP_CLOSE_STATUS_BIT            7   // VRP Close Status bit
#define VDRYER_OPEN_CMD_BIT             8   // VDRYER Open bit
#define VDRYER_CLOSE_CMD_BIT            9   // VDRYER Close bit
#define VDRYER_OPEN_STATUS_BIT          10  // VDRYER Open Status bit
#define VDRYER_CLOSE_STATUS_BIT         11  // VDRYER Close Status bit
#define V1_OPEN_CMD_BIT                 12  // V1 Open bit
#define V1_CLOSE_CMD_BIT                13  // V1 Close bit
#define V1_OPEN_STATUS_BIT              14  // V1 Open Status bit
#define V1_CLOSE_STATUS_BIT             15  // V1 Close Status bit
#define VMAIN_OPEN_CMD_BIT              16  // VMAIN Open bit
#define VMAIN_CLOSE_CMD_BIT             17  // VMAIN Close bit
#define VMAIN_OPEN_STATUS_BIT           18  // VMAIN Open Status bit
#define VMAIN_CLOSE_STATUS_BIT          19  // VMAIN Close Status bit
#define VSOFT_OPEN_CMD_BIT              20  // VSOFT Open bit
#define VSOFT_CLOSE_CMD_BIT             21  // VSOFT Close bit
#define P1_ON_CMD_BIT                   22  // P1 On bit
#define P1_OFF_CMD_BIT                  23  // P1 Off bit
#define P1_STATUS_BIT                   24  // P1 Status bit

#define ARD_RESET_BIT                   31  // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */ 
//
// digital pins (OUTPUTS)
//
#define P1_ON_CMD            CONTROLLINO_D0    //(Command Switch ON Scroll P1)
#define P1_OFF_CMD           CONTROLLINO_R0    //(Command Switch OFF Scroll P1)
#define V1_OPEN_CMD          CONTROLLINO_D1    //(Open Command V1)
#define V1_CLOSE_CMD         CONTROLLINO_R1    //(Close Command V1)
#define VBYPASS_OPEN_CMD     CONTROLLINO_D2    //(Open Command VBYPASS)
#define VBYPASS_CLOSE_CMD    CONTROLLINO_R2    //(Close Command VBYPASS)
#define VRP_OPEN_CMD         CONTROLLINO_D3    //(Open Command VRP)
#define VRP_CLOSE_CMD        CONTROLLINO_R3    //(Close Command VRP)
#define VDRYER_OPEN_CMD      CONTROLLINO_D4    //(Open Command VDRYER)
#define VDRYER_CLOSE_CMD     CONTROLLINO_R4    //(Close Command VDRYER)
#define VMAIN_OPEN_CMD       CONTROLLINO_D5    //(Open Command VMAIN)
#define VMAIN_CLOSE_CMD      CONTROLLINO_R5    //(Close Command VMAIN)
#define VSOFT_OPEN_CMD       CONTROLLINO_D6    //(Open Command VSOFT)
#define VSOFT_CLOSE_CMD      CONTROLLINO_R6    //(Close Command VSOFT)
//
// digital pins (INTPUTS)
//
#define VBYPASS_OPEN_STATUS  CONTROLLINO_A1    // VBYPASS OPEN STATUS
#define VBYPASS_CLOSE_STATUS CONTROLLINO_A2    // VBYPASS CLOSE STATUS
#define VRP_OPEN_STATUS      CONTROLLINO_A3    // VRP OPEN STATUS
#define VRP_CLOSE_STATUS     CONTROLLINO_A4    // VRP CLOSE STATUS
#define VDRYER_OPEN_STATUS   CONTROLLINO_A5    // VDRYER OPEN STATUS
#define VDRYER_CLOSE_STATUS  CONTROLLINO_A6    // VDRYER CLOSE STATUS
#define V1_OPEN_STATUS       CONTROLLINO_A7    // V1 OPEN STATUS
#define V1_CLOSE_STATUS      CONTROLLINO_A8    // V1 CLOSE STATUS
#define P1_STATUS            CONTROLLINO_A9    // P1 ON/OFF STATUS
#define VMAIN_OPEN_STATUS    CONTROLLINO_IN0   // VMAIN OPEN STATUS
#define VMAIN_CLOSE_STATUS   CONTROLLINO_IN1   // VMAIN CLOSE STATUS


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
  digitalWrite(VDRYER_OPEN_CMD,LOW);                         // Set VDRYER_OPEN_CMD LOW
  digitalWrite(VDRYER_CLOSE_CMD,HIGH);                       // Set VDRYER_CLOSE_CMD HIGH
  pinMode(VDRYER_OPEN_CMD, OUTPUT);                          // Set the digital pin as output for Open Valve
  pinMode(VDRYER_CLOSE_CMD, OUTPUT);                         // Set the digital pin as output for Close Valve
  I2CsetBit(VDRYER_OPEN_CMD_BIT,0x00);                       // Set VDRYER_OPEN_CMD_BIT LOW
  I2CsetBit(VDRYER_CLOSE_CMD_BIT,0x01);                      // Set VDRYER_CLOSE_CMD_BIT HIGH
  digitalWrite(V1_OPEN_CMD,LOW);                             // Set V1_OPEN_CMD LOW
  digitalWrite(V1_CLOSE_CMD,HIGH);                           // Set V1_CLOSE_CMD HIGH
  pinMode(V1_OPEN_CMD, OUTPUT);                              // Set the digital pin as output for Open Valve 
  pinMode(V1_CLOSE_CMD, OUTPUT);                             // Set the digital pin as output for Close Valve
  I2CsetBit(V1_OPEN_CMD_BIT,0x00);                           // Set V1_OPEN_CMD_BIT LOW
  I2CsetBit(V1_CLOSE_CMD_BIT,0x01);                          // Set V1_CLOSE_CMD_BIT HIGH
  digitalWrite(P1_ON_CMD,LOW);                               // Set P1_ON_CMD LOW
  digitalWrite(P1_OFF_CMD,HIGH);                             // Set P1_OFF_CMD HIGH
  pinMode(P1_ON_CMD, OUTPUT);                                // Set the digital pin as output for Switch ON Scroll
  pinMode(P1_OFF_CMD, OUTPUT);                               // Set the digital pin as output for Switch OFF Scroll
  I2CsetBit(P1_ON_CMD_BIT,0x00);                             // Set P1_ON_CMD_BIT LOW
  I2CsetBit(P1_OFF_CMD_BIT,0x01);                            // Set P1_OFF_CMD_BIT HIGH
  digitalWrite(VBYPASS_OPEN_CMD,LOW);                        // Set VBYPASS_OPEN_CMD LOW
  digitalWrite(VBYPASS_CLOSE_CMD,HIGH);                      // Set VBYPASS_CLOSE_CMD HIGH
  pinMode(VBYPASS_OPEN_CMD, OUTPUT);                         // Set the digital pin as output for Open Valve
  pinMode(VBYPASS_CLOSE_CMD, OUTPUT);                        // Set the digital pin as output for Close Valve
  I2CsetBit(VBYPASS_OPEN_CMD_BIT,0x00);                      // Set VBYPASS_OPEN_CMD_BIT LOW
  I2CsetBit(VBYPASS_CLOSE_CMD_BIT,0x01);                     // Set VBYPASS_CLOSE_CMD_BIT HIGH
  digitalWrite(VMAIN_OPEN_CMD,LOW);                          // Set VMAIN_OPEN_CMD LOW
  digitalWrite(VMAIN_CLOSE_CMD,HIGH);                        // Set VMAIN_CLOSE_CMD HIGH
  pinMode(VMAIN_OPEN_CMD, OUTPUT);                           // Set the digital pin as output for Open Valve
  pinMode(VMAIN_CLOSE_CMD, OUTPUT);                          // Set the digital pin as output for Close Valve
  I2CsetBit(VMAIN_OPEN_CMD_BIT,0x00);                        // Set VMAIN_OPEN_CMD_BIT LOW
  I2CsetBit(VMAIN_CLOSE_CMD_BIT,0x01);                       // Set VMAIN_CLOSE_CMD_BIT HIGH
  digitalWrite(VSOFT_OPEN_CMD,LOW);                          // Set VSOFT_OPEN_CMD LOW
  digitalWrite(VSOFT_CLOSE_CMD,HIGH);                        // Set VSOFT_CLOSE_CMD HIGH
  pinMode(VSOFT_OPEN_CMD, OUTPUT);                           // Set the digital pin as output for Open Valve
  pinMode(VSOFT_CLOSE_CMD, OUTPUT);                          // Set the digital pin as output for Close Valve
  I2CsetBit(VSOFT_OPEN_CMD_BIT,0x00);                        // Set VSOFT_OPEN_CMD_BIT LOW
  I2CsetBit(VSOFT_CLOSE_CMD_BIT,0x01);                       // Set VSOFT_CLOSE_CMD_BIT HIGH
  digitalWrite(VRP_OPEN_CMD,LOW);                            // Set VRP_OPEN_CMD LOW
  digitalWrite(VRP_CLOSE_CMD,HIGH);                          // Set VRP_CLOSE_CMD HIGH
  pinMode(VRP_OPEN_CMD, OUTPUT);                             // Set the digital pin as output for Open Valve 
  pinMode(VRP_CLOSE_CMD, OUTPUT);                            // Set the digital pin as output for Close Valve
  I2CsetBit(VRP_OPEN_CMD_BIT,0x00);                          // Set VRP_OPEN_CMD_BIT LOW
  I2CsetBit(VRP_CLOSE_CMD_BIT,0x01);                         // Set VRP_CLOSE_CMD_BIT HIGH
  
  // Digital INPUTS assignation
  pinMode(VDRYER_OPEN_STATUS, INPUT);                        // sets the digital pin as input for Valve VDRYER OPEN STATUS
  pinMode(VDRYER_CLOSE_STATUS, INPUT);                       // sets the digital pin as input for Valve VDRYER CLOSE STATUS
  pinMode(V1_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V1 OPEN STATUS
  pinMode(V1_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V1 CLOSE STATUS
  pinMode(P1_STATUS, INPUT);                                 // sets the digital pin as input for Scroll P1 ON/OFF STATUS
  pinMode(VBYPASS_OPEN_STATUS, INPUT);                       // sets the digital pin as input for Valve VBYPASS OPEN STATUS
  pinMode(VBYPASS_CLOSE_STATUS, INPUT);                      // sets the digital pin as input for Valve VBYPASS CLOSE STATUS
  pinMode(VRP_OPEN_STATUS, INPUT);                           // sets the digital pin as input for Valve VRP OPEN STATUS
  pinMode(VRP_CLOSE_STATUS, INPUT);                          // sets the digital pin as input for Valve VRP CLOSE STATUS
  pinMode(VMAIN_OPEN_STATUS, INPUT);                         // sets the digital pin as input for Valve VDRYER OPEN STATUS
  pinMode(VMAIN_CLOSE_STATUS, INPUT);                        // sets the digital pin as input for Valve VDRYER CLOSE STATUS
  
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
 *  VDRYER Valve Case
 */
  // Reset VDRYER_CLOSE_CMD
  if (digitalRead(VDRYER_CLOSE_CMD) == LOW && VDRYER_RESET == true) {
    if ( millis() - VDRYERtime > reset_wait) {
       digitalWrite(VDRYER_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(VDRYER_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VDRYER_RESET = false;  
    }
  }
  // Reset VDRYER_OPEN_CMD
  if (digitalRead(VDRYER_OPEN_CMD) == HIGH && VDRYER_RESET == true) {
    if ( millis() - VDRYERtime > reset_wait) {
       digitalWrite(VDRYER_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(VDRYER_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VDRYER_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VDRYERtime = millis();
       VDRYER_CHECK = true; 
    }
  }
  // Check VDRYER Close Status
  if (digitalRead(VDRYER_CLOSE_STATUS) == HIGH && digitalRead(VDRYER_OPEN_STATUS) == LOW && VDRYER_CHECK == true) {
    if ( millis() - VDRYERtime > check_wait) {
      digitalWrite(VDRYER_CLOSE_CMD,LOW);  // CLOSE VALVE
      VDRYERtime = millis();
      VDRYER_CHECK = false;
      VDRYER_RESET = true;
    }
  }
/*
 *  VSOFT Valve Case
 */
  // Reset VSOFT_CLOSE_CMD
  if (digitalRead(VSOFT_CLOSE_CMD) == LOW && VSOFT_RESET == true) {
    if ( millis() - VSOFTtime > reset_wait) {
       digitalWrite(VSOFT_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(VSOFT_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VSOFT_RESET = false;  
    }
  }
  // Reset VSOFT_OPEN_CMD
  if (digitalRead(VSOFT_OPEN_CMD) == HIGH && VSOFT_RESET == true) {
    if ( millis() - VSOFTtime > reset_wait) {
       digitalWrite(VSOFT_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(VSOFT_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VSOFT_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VSOFTtime = millis();
       VSOFT_CHECK = true; 
    }
  }
  /* Check VSOFT Close Status
  if (digitalRead(VSOFT_CLOSE_STATUS) == HIGH && digitalRead(VSOFT_OPEN_STATUS) == LOW && VSOFT_CHECK == true) {
    if ( millis() - VSOFTtime > check_wait) {
      digitalWrite(VSOFT_CLOSE_CMD,LOW);  // CLOSE VALVE
      VSOFTtime = millis();
      VSOFT_CHECK = false;
      VSOFT_RESET = true;
    }
  }*/
/*
 *  VMAIN Valve Case
 */
  // Reset VMAIN_CLOSE_CMD
  if (digitalRead(VMAIN_CLOSE_CMD) == LOW && VMAIN_RESET == true) {
    if ( millis() - VMAINtime > reset_wait) {
       digitalWrite(VMAIN_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(VMAIN_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VMAIN_RESET = false;  
    }
  }
  // Reset VMAIN_OPEN_CMD
  if (digitalRead(VMAIN_OPEN_CMD) == HIGH && VMAIN_RESET == true) {
    if ( millis() - VMAINtime > reset_wait) {
       digitalWrite(VMAIN_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(VMAIN_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VMAIN_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VMAINtime = millis();
       VMAIN_CHECK = true; 
    }
  }
  // Check VMAIN Close Status
  if (digitalRead(VMAIN_CLOSE_STATUS) == HIGH && digitalRead(VMAIN_OPEN_STATUS) == LOW && VMAIN_CHECK == true) {
    if ( millis() - VMAINtime > check_wait) {
      digitalWrite(VMAIN_CLOSE_CMD,LOW);  // CLOSE VALVE
      VMAINtime = millis();
      VMAIN_CHECK = false;
      VMAIN_RESET = true;
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
 *  P1 Case
 */
  // Reset P1_OFF_CMD
  if (digitalRead(P1_OFF_CMD) == LOW && P1_RESET == true) {
    if ( millis() - P1time > reset_wait) {
      digitalWrite(P1_OFF_CMD,HIGH);    // RESET SWITCH OFF SCROLL
      I2CsetBit(P1_OFF_CMD_BIT,0x01);   // RESET
      P1_RESET = false;
    }
  }
  // Reset P1_ON_CMD
  if (digitalRead(P1_ON_CMD) == HIGH && P1_RESET == true) {
    if ( millis() - P1time > reset_wait) {
      digitalWrite(P1_ON_CMD,LOW);      // RESET SWITCH ON SCROLL
      I2CsetBit(P1_ON_CMD_BIT,0x00);    // RESET
      P1_RESET = false;  
    }
  }
/*
 *  VBYPASS Valve Case
 */
  // Reset VBYPASS_CLOSE_CMD
  if (digitalRead(VBYPASS_CLOSE_CMD) == LOW && VBYPASS_RESET == true) {
    if ( millis() - VBYPASStime > reset_wait) {
       digitalWrite(VBYPASS_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(VBYPASS_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VBYPASS_RESET = false;  
    }
  }
  // Reset VBYPASS_OPEN_CMD
  if (digitalRead(VBYPASS_OPEN_CMD) == HIGH && VBYPASS_RESET == true) {
    if ( millis() - VBYPASStime > reset_wait) {
       digitalWrite(VBYPASS_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(VBYPASS_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VBYPASS_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VBYPASStime = millis();
       VBYPASS_CHECK = true; 
    }
  }
  // Check VBYPASS Close Status
  if (digitalRead(VBYPASS_CLOSE_STATUS) == HIGH && digitalRead(VBYPASS_OPEN_STATUS) == LOW && VBYPASS_CHECK == true) {
    if ( millis() - VBYPASStime > check_wait) {
      digitalWrite(VBYPASS_CLOSE_CMD,LOW);  // CLOSE VALVE
      VBYPASStime = millis();
      VBYPASS_CHECK = false;
      VBYPASS_RESET = true;
    }
  }
/*
 * VRP Valve case
 */
  // Reset VRP_CLOSE_CMD
  if (digitalRead(VRP_CLOSE_CMD) == LOW && VRP_RESET == true) {
    if ( millis() - VRPtime > reset_wait) {
       digitalWrite(VRP_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(VRP_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VRP_RESET = false;  
    }
  }
  // Reset VRP_OPEN_CMD
  if (digitalRead(VRP_OPEN_CMD) == HIGH && VRP_RESET == true) {
    if ( millis() - VRPtime > reset_wait) {
       digitalWrite(VRP_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(VRP_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VRP_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VRPtime = millis();
       VRP_CHECK = true; 
    }
  }
  // Check VRP Close Status
  if (digitalRead(VRP_CLOSE_STATUS) == HIGH && digitalRead(VRP_OPEN_STATUS) == LOW && VRP_CHECK == true) {
    if ( millis() - VRPtime > check_wait) {
      digitalWrite(VRP_CLOSE_CMD,LOW);  // CLOSE VALVE
      VRPtime = millis();
      VRP_CHECK = false;
      VRP_RESET = true;
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
  /* Update Valve VDRYER position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,VDRYER_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,VDRYER_OPEN_CMD_BIT) == 0x00 && VDRYER_RESET == false) {
     digitalWrite(VDRYER_CLOSE_CMD,LOW);   // CLOSE VALVE
     VDRYERtime = millis();
     VDRYER_RESET = true;
  }
  else if (bitRead(i2c_buffer,VDRYER_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,VDRYER_CLOSE_CMD_BIT) == 0x01 && VDRYER_RESET == false) {
     digitalWrite(VDRYER_OPEN_CMD,HIGH);   // OPEN VALVE
     VDRYERtime = millis();
     VDRYER_RESET = true;
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
  /* Update Scroll P1 position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,P1_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,P1_ON_CMD_BIT) == 0x00 && P1_RESET == false) {
     digitalWrite(P1_OFF_CMD,LOW);     // SWITCH OFF SCROLL
     P1time = millis();
     P1_RESET = true; 
  }
  else if (bitRead(i2c_buffer,P1_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,P1_OFF_CMD_BIT) == 0x01 && P1_RESET == false) { 
     digitalWrite(P1_ON_CMD,HIGH);     // SWITCH ON SCROLL
     P1time = millis();
     P1_RESET = true; 
  }
  /***********************************************************************************************************/
  /* Update Valve VBYPASS position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,VBYPASS_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,VBYPASS_OPEN_CMD_BIT) == 0x00 && VBYPASS_RESET == false) {
     digitalWrite(VBYPASS_CLOSE_CMD,LOW);   // CLOSE VALVE
     VBYPASStime = millis();
     VBYPASS_RESET = true;
  }
  else if (bitRead(i2c_buffer,VBYPASS_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,VBYPASS_CLOSE_CMD_BIT) == 0x01 && VBYPASS_RESET == false) {
     digitalWrite(VBYPASS_OPEN_CMD,HIGH);   // OPEN VALVE
     VBYPASStime = millis();
     VBYPASS_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VRP position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,VRP_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,VRP_OPEN_CMD_BIT) == 0x00 && VRP_RESET == false) {
     digitalWrite(VRP_CLOSE_CMD,LOW);   // CLOSE VALVE
     VRPtime = millis();
     VRP_RESET = true;
  }
  else if (bitRead(i2c_buffer,VRP_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,VRP_CLOSE_CMD_BIT) == 0x01 && VRP_RESET == false) {
     digitalWrite(VRP_OPEN_CMD,HIGH);   // OPEN VALVE
     VRPtime = millis();
     VRP_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VMAIN position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,VMAIN_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,VMAIN_OPEN_CMD_BIT) == 0x00 && VMAIN_RESET == false) {
     digitalWrite(VMAIN_CLOSE_CMD,LOW);   // CLOSE VALVE
     VMAINtime = millis();
     VMAIN_RESET = true;
  }
  else if (bitRead(i2c_buffer,VMAIN_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,VMAIN_CLOSE_CMD_BIT) == 0x01 && VMAIN_RESET == false) {
     digitalWrite(VMAIN_OPEN_CMD,HIGH);   // OPEN VALVE
     VMAINtime = millis();
     VMAIN_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VSOFT position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,VSOFT_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,VSOFT_OPEN_CMD_BIT) == 0x00 && VSOFT_RESET == false) {
     digitalWrite(VSOFT_CLOSE_CMD,LOW);   // CLOSE VALVE
     VSOFTtime = millis();
     VSOFT_RESET = true;
  }
  else if (bitRead(i2c_buffer,VSOFT_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,VSOFT_CLOSE_CMD_BIT) == 0x01 && VSOFT_RESET == false) {
     digitalWrite(VSOFT_OPEN_CMD,HIGH);   // OPEN VALVE
     VSOFTtime = millis();
     VSOFT_RESET = true;
  }
}

void UpdateI2CFromIO()
{ 

  
  /***********************************************************************************************************/
  /* Update VDRYER Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VDRYER_OPEN_STATUS) == HIGH && digitalRead(VDRYER_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(VDRYER_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VDRYER_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VDRYER_CLOSE_STATUS) == HIGH && digitalRead(VDRYER_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VDRYER_OPEN_STATUS_BIT == 0x01 && VDRYER_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VDRYER_CLOSE_CMD,LOW);  // CLOSE VALVE
        VDRYERtime = millis();
        VDRYER_RESET = true;
     }
     I2CsetBit(VDRYER_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VDRYER_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(VDRYER_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VDRYER_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update V1 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V1_OPEN_STATUS) == HIGH && digitalRead(V1_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V1_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V1_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V1_CLOSE_STATUS) == HIGH && digitalRead(V1_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (V1_OPEN_STATUS_BIT == 0x01 && V1_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
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
  /* Update P1 Scroll position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(P1_STATUS) == HIGH) {    // SCROLL ON STATUS
     I2CsetBit(P1_STATUS_BIT,0x01);        // UPDATE SCROLL ON BIT
  }
  else if (digitalRead(P1_STATUS) == LOW) { // SCROLL OFF STATUS
     I2CsetBit(P1_STATUS_BIT,0x00);         // UPDATE SCROLL OFF BIT
  }
  /***********************************************************************************************************/
  /* Update VBYPASS Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VBYPASS_OPEN_STATUS) == HIGH && digitalRead(VBYPASS_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(VBYPASS_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VBYPASS_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VBYPASS_CLOSE_STATUS) == HIGH && digitalRead(VBYPASS_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VBYPASS_OPEN_STATUS_BIT == 0x01 && VBYPASS_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VBYPASS_CLOSE_CMD,LOW);  // CLOSE VALVE
        VBYPASStime = millis();
        VBYPASS_RESET = true;
     }
     I2CsetBit(VBYPASS_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VBYPASS_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(VBYPASS_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VBYPASS_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VRP Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VRP_OPEN_STATUS) == HIGH && digitalRead(VRP_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(VRP_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VRP_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VRP_CLOSE_STATUS) == HIGH && digitalRead(VRP_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VRP_OPEN_STATUS_BIT == 0x01 && VRP_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VRP_CLOSE_CMD,LOW);  // CLOSE VALVE
        VRPtime = millis();
        VRP_RESET = true;
     }
     I2CsetBit(VRP_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VRP_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(VRP_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VRP_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VMAIN Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VMAIN_OPEN_STATUS) == HIGH && digitalRead(VMAIN_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(VMAIN_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VMAIN_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VMAIN_CLOSE_STATUS) == HIGH && digitalRead(VMAIN_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VMAIN_OPEN_STATUS_BIT == 0x01 && VMAIN_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VMAIN_CLOSE_CMD,LOW);  // CLOSE VALVE
        VMAINtime = millis();
        VMAIN_RESET = true;
     }
     I2CsetBit(VMAIN_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VMAIN_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(VMAIN_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(VMAIN_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }

}
