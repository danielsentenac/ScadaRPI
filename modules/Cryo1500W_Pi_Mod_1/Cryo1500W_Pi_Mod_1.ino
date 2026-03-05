/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board (Valves/Air Pressure/ ByPass)
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
volatile uint32_t i2c_buffer1 = 0;
volatile uint32_t i2c_buffer2 = 0;
volatile byte data_array[8];
volatile byte crc_array[4];
volatile boolean updateIOFromI2CBool = false;
volatile boolean bypassON = false;

/*
 * The time for Check and Reset actions
 */
unsigned long VPOtime = 0;
unsigned long VPItime = 0;
unsigned long P1time = 0;
unsigned long VP1time = 0;
unsigned long VOtime = 0;
unsigned long VItime = 0;
unsigned long VBOtime = 0;
unsigned long VBItime = 0;
unsigned long V2425time = 0;
unsigned long VGAStime = 0;
unsigned long BYPASStime = 0;
unsigned long looptime = 0;

/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean VPO_RESET = false;
boolean VPI_RESET = false;
boolean P1_RESET = false;
boolean VP1_RESET = false;
boolean VO_RESET = false;
boolean VI_RESET = false;
boolean VBO_RESET = false;
boolean VBI_RESET = false;
boolean V2425_RESET = false;
boolean VGAS_RESET = false;
boolean BYPASS_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;
long reset_wait2 = 5000; // used for stage primary pumps

 /*
 *  These CHECK are used to check switches status (Status inputs)
 */
boolean VPO_CHECK = false;
boolean VPI_CHECK = false;
boolean P1_CHECK = false;
boolean VP1_CHECK = false;
boolean VO_CHECK = false;
boolean VI_CHECK = false;
boolean VBO_CHECK = false;
boolean VBI_CHECK = false;
boolean V2425_CHECK = false;
boolean VGAS_CHECK = false;
boolean BYPASS_CHECK = false;

/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;
 
/*
 *  I2C BIT ASSIGNATION BUFFER 1 (MAX = 32) 
 */
#define VPO_OPEN_CMD_BIT           0   // VPO Open bit
#define VPO_CLOSE_CMD_BIT          1   // VPO Close bit
#define VPO_OPEN_STATUS_BIT        2   // VPO Open Status bit
#define VPO_CLOSE_STATUS_BIT       3   // VPO Close Status bit
#define VPI_OPEN_CMD_BIT           4   // VPI Open bit
#define VPI_CLOSE_CMD_BIT          5   // VPI Close bit
#define VPI_OPEN_STATUS_BIT        6   // VPI Open Status bit
#define VPI_CLOSE_STATUS_BIT       7   // VPI Close Status bit
#define VP1_OPEN_CMD_BIT           8   // VP1 Open bit
#define VP1_CLOSE_CMD_BIT          9   // VP1 Close bit
#define VP1_OPEN_STATUS_BIT        10  // VP1 Open Status bit
#define VP1_CLOSE_STATUS_BIT       11  // VP1 Close Status bit
#define VO_OPEN_CMD_BIT            12  // VO Open bit
#define VO_CLOSE_CMD_BIT           13  // VO Close bit
#define VO_OPEN_STATUS_BIT         14  // VO Open Status bit
#define VO_CLOSE_STATUS_BIT        15  // VO Close Status bit
#define VI_OPEN_CMD_BIT            16  // VI Open bit
#define VI_CLOSE_CMD_BIT           17  // VI Close bit
#define VI_OPEN_STATUS_BIT         18  // VI Open Status bit
#define VI_CLOSE_STATUS_BIT        19  // VI Close Status bit
#define VBO_OPEN_CMD_BIT           20  // VBO Open bit
#define VBO_CLOSE_CMD_BIT          21  // VBO Close bit
#define VBO_OPEN_STATUS_BIT        22  // VBO Open Status bit
#define VBO_CLOSE_STATUS_BIT       23  // VBO Close Status bit
#define VBI_OPEN_CMD_BIT           24  // VBI Open bit
#define VBI_CLOSE_CMD_BIT          25  // VBI Close bit
#define VBI_OPEN_STATUS_BIT        26  // VBI Open Status bit
#define VBI_CLOSE_STATUS_BIT       27  // VBI Close Status bit
#define V2425_OPEN_CMD_BIT         28  // V2425 Open bit
#define V2425_CLOSE_CMD_BIT        29  // V2425 Close bit
#define V2425_OPEN_STATUS_BIT      30  // V2425 Open Status bit
#define V2425_CLOSE_STATUS_BIT     31  // V2425 Close Status bit

/*
 *  I2C BIT ASSIGNATION BUFFER 2 (MAX = 32) 
 */
#define VGAS_OPEN_CMD_BIT          0  // VGAS Open bit
#define VGAS_CLOSE_CMD_BIT         1  // VGAS Close bit
#define VGAS_OPEN_STATUS_BIT       2  // VGAS Open Status bit
#define VGAS_CLOSE_STATUS_BIT      3  // VGAS Close Status bit
#define BYPASS_ON_CMD_BIT          4  // BYPASS On bit
#define BYPASS_OFF_CMD_BIT         5  // BYPASS Off bit
#define BYPASS_STATUS_BIT          6  // BYPASS ON Status bit
#define P1_ON_CMD_BIT              7  // P1 On bit
#define P1_OFF_CMD_BIT             8  // P1 Off bit
#define P1_STATUS_BIT              9  // P1 Status bit
#define COMPRESSAIR_STATUS_BIT     10  // COMPRESSAIR Status bit
#define ARD_RESET_BIT              31  // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */ 
// digital pins (OUTPUTS)
#define P1_ON_CMD       CONTROLLINO_D12  //(Command Switch ON Scroll P1)
#define P1_OFF_CMD      CONTROLLINO_R0  //(Command Switch OFF Scroll P1)
#define VPO_OPEN_CMD    CONTROLLINO_D13  //(Open Command VPO)
#define VPO_CLOSE_CMD   CONTROLLINO_R1  //(Close Command VPO)
#define VPI_OPEN_CMD    CONTROLLINO_D14  //(Open Command VPI)
#define VPI_CLOSE_CMD   CONTROLLINO_R2  //(Close Command VPI)
#define VP1_OPEN_CMD    CONTROLLINO_D15  //(Open Command VP1)
#define VP1_CLOSE_CMD   CONTROLLINO_R3  //(Close Command VP1)
#define VO_OPEN_CMD     CONTROLLINO_D16  //(Open Command VO)
#define VO_CLOSE_CMD    CONTROLLINO_R4  //(Close Command VO)
#define VI_OPEN_CMD     CONTROLLINO_D17  //(Open Command VI)
#define VI_CLOSE_CMD    CONTROLLINO_R5  //(Close Command VI)
#define VBO_OPEN_CMD    CONTROLLINO_D18  //(Open Command VBO)
#define VBO_CLOSE_CMD   CONTROLLINO_R6  //(Close Command VBO)
#define VBI_OPEN_CMD    CONTROLLINO_D19  //(Open Command VBI)
#define VBI_CLOSE_CMD   CONTROLLINO_R8  //(Close Command VBI)
#define V2425_OPEN_CMD  CONTROLLINO_D20  //(Open Command V2425)
#define V2425_CLOSE_CMD CONTROLLINO_R10  //(Close Command V2425)
#define VGAS_OPEN_CMD   CONTROLLINO_D21  //(Open Command VGAS)
#define VGAS_CLOSE_CMD  CONTROLLINO_R12  //(Close Command VGAS)
#define BYPASS_ON_CMD   CONTROLLINO_D22  //(Command Switch ON BYPASS)
#define BYPASS_OFF_CMD  CONTROLLINO_R14  //(Command Switch OFF BYPASS)


// digital pins (INTPUTS)
#define COMPRESSAIR_STATUS CONTROLLINO_A0    // COMPRESSAIR STATUS
#define P1_STATUS          CONTROLLINO_A1    // P1 ON/OFF STATUS
#define VPO_OPEN_STATUS    CONTROLLINO_A2    // VPO OPEN STATUS
#define VPO_CLOSE_STATUS   CONTROLLINO_A3    // VPO CLOSE STATUS
#define VPI_OPEN_STATUS    CONTROLLINO_A4    // VPI OPEN STATUS
#define VPI_CLOSE_STATUS   CONTROLLINO_A5    // VPI CLOSE STATUS
#define VP1_OPEN_STATUS    CONTROLLINO_A6    // VP1 OPEN STATUS
#define VP1_CLOSE_STATUS   CONTROLLINO_A7    // VP1 CLOSE STATUS
#define VO_OPEN_STATUS     CONTROLLINO_A8    // VO OPEN STATUS
#define VO_CLOSE_STATUS    CONTROLLINO_A9    // VO CLOSE STATUS
#define VI_OPEN_STATUS     CONTROLLINO_A10   // VI OPEN STATUS
#define VI_CLOSE_STATUS    CONTROLLINO_A11   // VI CLOSE STATUS
#define VBO_OPEN_STATUS    CONTROLLINO_A12   // VBO OPEN STATUS
#define VBO_CLOSE_STATUS   CONTROLLINO_A13   // VBO CLOSE STATUS
#define VBI_OPEN_STATUS    CONTROLLINO_A14   // VBI OPEN STATUS
#define VBI_CLOSE_STATUS   CONTROLLINO_A15   // VBI CLOSE STATUS
#define V2425_OPEN_STATUS  CONTROLLINO_I16   // V2425 OPEN STATUS
#define V2425_CLOSE_STATUS CONTROLLINO_I17   // V2425 CLOSE STATUS
#define VGAS_OPEN_STATUS   CONTROLLINO_I18   // VGAS OPEN STATUS
#define VGAS_CLOSE_STATUS  CONTROLLINO_IN0   // VGAS CLOSE STATUS
#define BYPASS_STATUS      CONTROLLINO_IN1   // BYPASS STATUS
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
     Serial.print("i2c_buffer1=");
     Serial.println(i2c_buffer1,BIN);
     Serial.print("i2c_buffer2=");
     Serial.println(i2c_buffer2,BIN);
     looptime = millis();
  }
  
  if (updateIOFromI2CBool == true) {
    Serial.print("Received command; updated i2c_buffer1 =");
    Serial.println(i2c_buffer1,BIN);
    Serial.print("Received command; updated i2c_buffer2 =");
    Serial.println(i2c_buffer2,BIN);
    updateIOFromI2CBool = false;
    // Update I/O from updated i2c_buffer1&2
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
  digitalWrite(VPO_OPEN_CMD,LOW);                           // Set VPO_OPEN_CMD LOW
  digitalWrite(VPO_CLOSE_CMD,HIGH);                         // Set VPO_CLOSE_CMD HIGH
  pinMode(VPO_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve
  pinMode(VPO_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit1(VPO_OPEN_CMD_BIT,0x00);                         // Set VPO_OPEN_CMD_BIT LOW
  I2CsetBit1(VPO_CLOSE_CMD_BIT,0x01);                        // Set VPO_CLOSE_CMD_BIT HIGH
  digitalWrite(VPI_OPEN_CMD,LOW);                           // Set VPI_OPEN_CMD LOW
  digitalWrite(VPI_CLOSE_CMD,HIGH);                         // Set VPI_CLOSE_CMD HIGH
  pinMode(VPI_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve 
  pinMode(VPI_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit1(VPI_OPEN_CMD_BIT,0x00);                         // Set VPI_OPEN_CMD_BIT LOW
  I2CsetBit1(VPI_CLOSE_CMD_BIT,0x01);                        // Set VPI_CLOSE_CMD_BIT HIGH
  digitalWrite(VP1_OPEN_CMD,LOW);                           // Set VP1_OPEN_CMD LOW
  digitalWrite(VP1_CLOSE_CMD,HIGH);                         // Set VP1_CLOSE_CMD HIGH
  pinMode(VP1_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve 
  pinMode(VP1_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit1(VP1_OPEN_CMD_BIT,0x00);                         // Set VP1_OPEN_CMD_BIT LOW
  I2CsetBit1(VP1_CLOSE_CMD_BIT,0x01);                        // Set VP1_CLOSE_CMD_BIT HIGH
  digitalWrite(VO_OPEN_CMD,LOW);                            // Set VO_OPEN_CMD LOW
  digitalWrite(VO_CLOSE_CMD,HIGH);                          // Set VO_CLOSE_CMD HIGH
  pinMode(VO_OPEN_CMD, OUTPUT);                             // Set the digital pin as output for Open Valve 
  pinMode(VO_CLOSE_CMD, OUTPUT);                            // Set the digital pin as output for Close Valve
  I2CsetBit1(VO_OPEN_CMD_BIT,0x00);                          // Set VO_OPEN_CMD_BIT LOW
  I2CsetBit1(VO_CLOSE_CMD_BIT,0x01);                         // Set VO_CLOSE_CMD_BIT HIGH
  digitalWrite(VI_OPEN_CMD,LOW);                            // Set VI_OPEN_CMD LOW
  digitalWrite(VI_CLOSE_CMD,HIGH);                          // Set VI_CLOSE_CMD HIGH
  pinMode(VI_OPEN_CMD, OUTPUT);                             // Set the digital pin as output for Open Valve 
  pinMode(VI_CLOSE_CMD, OUTPUT);                            // Set the digital pin as output for Close Valve
  I2CsetBit1(VI_OPEN_CMD_BIT,0x00);                          // Set VI_OPEN_CMD_BIT LOW
  I2CsetBit1(VI_CLOSE_CMD_BIT,0x01);                         // Set VI_CLOSE_CMD_BIT HIGH
  digitalWrite(VBO_OPEN_CMD,LOW);                           // Set VBO_OPEN_CMD LOW
  digitalWrite(VBO_CLOSE_CMD,HIGH);                         // Set VBO_CLOSE_CMD HIGH
  pinMode(VBO_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve 
  pinMode(VBO_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit1(VBO_OPEN_CMD_BIT,0x00);                         // Set VBO_OPEN_CMD_BIT LOW
  I2CsetBit1(VBO_CLOSE_CMD_BIT,0x01);                        // Set VBO_CLOSE_CMD_BIT HIGH
  digitalWrite(VBI_OPEN_CMD,LOW);                           // Set VBI_OPEN_CMD LOW
  digitalWrite(VBI_CLOSE_CMD,HIGH);                         // Set VBI_CLOSE_CMD HIGH
  pinMode(VBI_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve 
  pinMode(VBI_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit1(VBI_OPEN_CMD_BIT,0x00);                         // Set VBI_OPEN_CMD_BIT LOW
  I2CsetBit1(VBI_CLOSE_CMD_BIT,0x01);                        // Set VBI_CLOSE_CMD_BIT HIGH
  digitalWrite(V2425_OPEN_CMD,LOW);                         // Set V2425_OPEN_CMD LOW
  digitalWrite(V2425_CLOSE_CMD,HIGH);                       // Set V2425_CLOSE_CMD HIGH
  pinMode(V2425_OPEN_CMD, OUTPUT);                          // Set the digital pin as output for Open Valve 
  pinMode(V2425_CLOSE_CMD, OUTPUT);                         // Set the digital pin as output for Close Valve
  I2CsetBit1(V2425_OPEN_CMD_BIT,0x00);                       // Set V2425_OPEN_CMD_BIT LOW
  I2CsetBit1(V2425_CLOSE_CMD_BIT,0x01);                      // Set V2425_CLOSE_CMD_BIT HIGH
  digitalWrite(VGAS_OPEN_CMD,LOW);                          // Set VGAS_OPEN_CMD LOW
  digitalWrite(VGAS_CLOSE_CMD,HIGH);                        // Set VGAS_CLOSE_CMD HIGH
  pinMode(VGAS_OPEN_CMD, OUTPUT);                           // Set the digital pin as output for Open Valve 
  pinMode(VGAS_CLOSE_CMD, OUTPUT);                          // Set the digital pin as output for Close Valve
  I2CsetBit2(VGAS_OPEN_CMD_BIT,0x00);                        // Set VGAS_OPEN_CMD_BIT LOW
  I2CsetBit2(VGAS_CLOSE_CMD_BIT,0x01);                       // Set VGAS_CLOSE_CMD_BIT HIGH
  digitalWrite(P1_ON_CMD,LOW);                              // Set P1_ON_CMD LOW
  digitalWrite(P1_OFF_CMD,HIGH);                            // Set P1_OFF_CMD HIGH
  pinMode(P1_ON_CMD, OUTPUT);                               // Set the digital pin as output for Switch ON Scroll
  pinMode(P1_OFF_CMD, OUTPUT);                              // Set the digital pin as output for Switch OFF Scroll
  I2CsetBit2(P1_ON_CMD_BIT,0x00);                            // Set P1_ON_CMD_BIT LOW
  I2CsetBit2(P1_OFF_CMD_BIT,0x01);                           // Set P1_OFF_CMD_BIT HIGH
  digitalWrite(BYPASS_ON_CMD,LOW);                          // Set BYPASS_ON_CMD LOW
  digitalWrite(BYPASS_OFF_CMD,HIGH);                        // Set BYPASS_OFF_CMD HIGH
  pinMode(BYPASS_ON_CMD, OUTPUT);                           // Set the digital pin as output for Switch ON Scroll
  pinMode(BYPASS_OFF_CMD, OUTPUT);                          // Set the digital pin as output for Switch OFF Scroll
  I2CsetBit2(BYPASS_ON_CMD_BIT,0x00);                        // Set BYPASS_ON_CMD_BIT LOW
  I2CsetBit2(BYPASS_OFF_CMD_BIT,0x01);                       // Set BYPASS_OFF_CMD_BIT HIGH
  
  // Digital INPUTS assignation
  pinMode(COMPRESSAIR_STATUS, INPUT);                       // sets the digital pin as input for COMPRESSAIR OK/KO STATUS  
  pinMode(P1_STATUS, INPUT);                                // sets the digital pin as input for Scroll ON/OFF STATUS
  pinMode(VPO_OPEN_STATUS, INPUT);                          // sets the digital pin as input for Valve VPO OPEN STATUS
  pinMode(VPO_CLOSE_STATUS, INPUT);                         // sets the digital pin as input for Valve VPO CLOSE STATUS
  pinMode(VPI_OPEN_STATUS, INPUT);                          // sets the digital pin as input for Valve VPI OPEN STATUS
  pinMode(VPI_CLOSE_STATUS, INPUT);                         // sets the digital pin as input for Valve VPI CLOSE STATUS
  pinMode(VP1_OPEN_STATUS, INPUT);                          // sets the digital pin as input for Valve VP1 OPEN STATUS
  pinMode(VP1_CLOSE_STATUS, INPUT);                         // sets the digital pin as input for Valve VP1 CLOSE STATUS
  pinMode(VO_OPEN_STATUS, INPUT);                           // sets the digital pin as input for Valve VO OPEN STATUS
  pinMode(VO_CLOSE_STATUS, INPUT);                          // sets the digital pin as input for Valve VO CLOSE STATUS
  pinMode(VI_OPEN_STATUS, INPUT);                           // sets the digital pin as input for Valve VI OPEN STATUS
  pinMode(VI_CLOSE_STATUS, INPUT);                          // sets the digital pin as input for Valve VI CLOSE STATUS
  pinMode(VBO_OPEN_STATUS, INPUT);                          // sets the digital pin as input for Valve VBO OPEN STATUS
  pinMode(VBO_CLOSE_STATUS, INPUT);                         // sets the digital pin as input for Valve VBO CLOSE STATUS
  pinMode(VBI_OPEN_STATUS, INPUT);                          // sets the digital pin as input for Valve VBI OPEN STATUS
  pinMode(VBI_CLOSE_STATUS, INPUT);                         // sets the digital pin as input for Valve VBI CLOSE STATUS
  pinMode(V2425_OPEN_STATUS, INPUT);                        // sets the digital pin as input for Valve V2425 OPEN STATUS
  pinMode(V2425_CLOSE_STATUS, INPUT);                       // sets the digital pin as input for Valve V2425 CLOSE STATUS
  pinMode(VGAS_OPEN_STATUS, INPUT);                         // sets the digital pin as input for Valve VGAS OPEN STATUS
  pinMode(VGAS_CLOSE_STATUS, INPUT);                        // sets the digital pin as input for Valve VGAS CLOSE STATUS
  pinMode(BYPASS_STATUS, INPUT);                            // sets the digital pin as input for BYPASS ON/OFF STATUS
  
  
  Serial.println("Done.");
}

void requestEvent() {
  
  // Send i2c_buffer1 & i2c_buffer2 to master (create 12 bytes array including crc32)
  
  byte i2c_array[12];

  // Write i2c_buffer1 to i2c_array

  i2c_array[0] = (i2c_buffer1 >> 24) & 0xFF;
  i2c_array[1] = (i2c_buffer1 >> 16) & 0xFF;
  i2c_array[2] = (i2c_buffer1 >> 8) & 0xFF;
  i2c_array[3] = i2c_buffer1 & 0xFF;

  // Write i2c_buffer1 to i2c_array

  i2c_array[4] = (i2c_buffer2 >> 24) & 0xFF;
  i2c_array[5] = (i2c_buffer2 >> 16) & 0xFF;
  i2c_array[6] = (i2c_buffer2 >> 8) & 0xFF;
  i2c_array[7] = i2c_buffer2 & 0xFF;

  // Calculate and add crc32
  uint32_t crc = CRC32::calculate(i2c_array, 8);

  i2c_array[8] = (crc >> 24) & 0xFF;
  i2c_array[9] = (crc >> 16) & 0xFF;
  i2c_array[10] = (crc >> 8) & 0xFF;
  i2c_array[11] = crc & 0xFF;

  // Send i2c_array
  Wire.write(i2c_array, 12);

}
void receiveEvent(int numbyte) {

  // Update i2c_buffer1 & i2c_buffer2 from master (total 8 bytes)
  //Serial.print("Number of bytes=");Serial.println(numbyte);
  if (numbyte == 12) { // Expect 12 bytes of data (including CRC32), update i2c_buffer1 & i2c_buffer2

    // i2c_buffer1
    
    data_array[0] = Wire.read();
    data_array[1] = Wire.read();
    data_array[2] = Wire.read();
    data_array[3] = Wire.read();

    // i2c_buffer2
    
    data_array[4] = Wire.read();
    data_array[5] = Wire.read();
    data_array[6] = Wire.read();
    data_array[7] = Wire.read();
    
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
    uint32_t crcdata = CRC32::calculate(data_array, 8);
   // Serial.print(" Checksum 1 crc:");Serial.println(crcdata);
   // Serial.print(" Checksum 2 crc:");Serial.println(crc);
    if ( crc == crcdata ) {
        //Serial.print(" Checksum good:");Serial.println(crc);
        // Update i2c_buffer1 in the interrupted section
        i2c_buffer1 = data_array[0];
        i2c_buffer1 = (i2c_buffer1 << 8) | data_array[1];
        i2c_buffer1 = (i2c_buffer1 << 8) | data_array[2];
        i2c_buffer1 = (i2c_buffer1 << 8) | data_array[3];

        // Update i2c_buffer2 in the interrupted section
        i2c_buffer2 = data_array[4];
        i2c_buffer2 = (i2c_buffer2 << 8) | data_array[5];
        i2c_buffer2 = (i2c_buffer2 << 8) | data_array[6];
        i2c_buffer2 = (i2c_buffer2 << 8) | data_array[7];
        

        // UpdateIOFromI2C in the loop
        updateIOFromI2CBool = true;    
    }
  }
  else // Flush the wrong Wire buffer
     while(Wire.available()) Wire.read();
}

void I2CsetBit1(int bit, int value) {
  if (bit < I2C_BUFFER) {
    if (value == 1)
      bitSet(i2c_buffer1,bit);
    else if (value == 0)
      bitClear(i2c_buffer1,bit);
  }
}

void I2CsetBit2(int bit, int value) {
  if (bit < I2C_BUFFER) {
    if (value == 1)
      bitSet(i2c_buffer2,bit);
    else if (value == 0)
      bitClear(i2c_buffer2,bit);
  }
}

void ResetAndCheck() {
/*
 *  VPO Valve Case
 */
  // Reset VPO_CLOSE_CMD
  if (digitalRead(VPO_CLOSE_CMD) == LOW && VPO_RESET == true) {
    if ( millis() - VPOtime > reset_wait) {
       digitalWrite(VPO_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit1(VPO_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VPO_RESET = false;  
    }
  }
  // Reset VPO_OPEN_CMD
  if (digitalRead(VPO_OPEN_CMD) == HIGH && VPO_RESET == true) {
    if ( millis() - VPOtime > reset_wait) {
       digitalWrite(VPO_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit1(VPO_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VPO_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VPOtime = millis();
       VPO_CHECK = true; 
    }
  }
  // Check VPO Close Status
  if (digitalRead(VPO_CLOSE_STATUS) == HIGH && digitalRead(VPO_OPEN_STATUS) == LOW && VPO_CHECK == true) {
    if ( millis() - VPOtime > check_wait) {
      digitalWrite(VPO_CLOSE_CMD,LOW);  // CLOSE VALVE
      VPOtime = millis();
      VPO_CHECK = false;
      VPO_RESET = true;
    }
  }
/*
 * VPI Valve case
 */
  // Reset VPI_CLOSE_CMD
  if (digitalRead(VPI_CLOSE_CMD) == LOW && VPI_RESET == true) {
    if ( millis() - VPItime > reset_wait) {
       digitalWrite(VPI_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit1(VPI_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VPI_RESET = false;  
    }
  }
  // Reset VPI_OPEN_CMD
  if (digitalRead(VPI_OPEN_CMD) == HIGH && VPI_RESET == true) {
    if ( millis() - VPItime > reset_wait) {
       digitalWrite(VPI_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit1(VPI_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VPI_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VPItime = millis();
       VPI_CHECK = true; 
    }
  }
  // Check VPI Close Status
  if (digitalRead(VPI_CLOSE_STATUS) == HIGH && digitalRead(VPI_OPEN_STATUS) == LOW && VPI_CHECK == true) {
    if ( millis() - VPItime > check_wait) {
      digitalWrite(VPI_CLOSE_CMD,LOW);  // CLOSE VALVE
      VPItime = millis();
      VPI_CHECK = false;
      VPI_RESET = true;
    }
  }
/*
 * VP1 Valve case
 */
  // Reset VP1_CLOSE_CMD
  if (digitalRead(VP1_CLOSE_CMD) == LOW && VP1_RESET == true) {
    if ( millis() - VP1time > reset_wait) {
       digitalWrite(VP1_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit1(VP1_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VP1_RESET = false;  
    }
  }
  // Reset VP1_OPEN_CMD
  if (digitalRead(VP1_OPEN_CMD) == HIGH && VP1_RESET == true) {
    if ( millis() - VP1time > reset_wait) {
       digitalWrite(VP1_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit1(VP1_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VP1_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VP1time = millis();
       VP1_CHECK = true; 
    }
  }
  // Check VP1 Close Status
  if (digitalRead(VP1_CLOSE_STATUS) == HIGH && digitalRead(VP1_OPEN_STATUS) == LOW && VP1_CHECK == true) {
    if ( millis() - VP1time > check_wait) {
      digitalWrite(VP1_CLOSE_CMD,LOW);  // CLOSE VALVE
      VP1time = millis();
      VP1_CHECK = false;
      VP1_RESET = true;
    }
  }
/*
 * VO Valve case
 */
  // Reset VO_CLOSE_CMD
  if (digitalRead(VO_CLOSE_CMD) == LOW && VO_RESET == true) {
    if ( millis() - VOtime > reset_wait) {
       digitalWrite(VO_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit1(VO_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VO_RESET = false;  
    }
  }
  // Reset VO_OPEN_CMD
  if (digitalRead(VO_OPEN_CMD) == HIGH && VO_RESET == true) {
    if ( millis() - VOtime > reset_wait) {
       digitalWrite(VO_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit1(VO_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VO_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VOtime = millis();
       VO_CHECK = true; 
    }
  }
  // Check VO Close Status
  if (digitalRead(VO_CLOSE_STATUS) == HIGH && digitalRead(VO_OPEN_STATUS) == LOW && VO_CHECK == true) {
    if ( millis() - VOtime > check_wait) {
      digitalWrite(VO_CLOSE_CMD,LOW);  // CLOSE VALVE
      VOtime = millis();
      VO_CHECK = false;
      VO_RESET = true;
    }
  }
/*
 * VI Valve case
 */
  // Reset VI_CLOSE_CMD
  if (digitalRead(VI_CLOSE_CMD) == LOW && VI_RESET == true) {
    if ( millis() - VItime > reset_wait) {
       digitalWrite(VI_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit1(VI_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VI_RESET = false;  
    }
  }
  // Reset VI_OPEN_CMD
  if (digitalRead(VI_OPEN_CMD) == HIGH && VI_RESET == true) {
    if ( millis() - VItime > reset_wait) {
       digitalWrite(VI_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit1(VI_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VI_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VItime = millis();
       VI_CHECK = true; 
    }
  }
  // Check VI Close Status
  if (digitalRead(VI_CLOSE_STATUS) == HIGH && digitalRead(VI_OPEN_STATUS) == LOW && VI_CHECK == true) {
    if ( millis() - VItime > check_wait) {
      digitalWrite(VI_CLOSE_CMD,LOW);  // CLOSE VALVE
      VItime = millis();
      VI_CHECK = false;
      VI_RESET = true;
    }
  }
/*
 * VBO Valve case
 */
  // Reset VBO_CLOSE_CMD
  if (digitalRead(VBO_CLOSE_CMD) == LOW && VBO_RESET == true) {
    if ( millis() - VBOtime > reset_wait) {
       digitalWrite(VBO_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit1(VBO_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VBO_RESET = false;  
    }
  }
  // Reset VBO_OPEN_CMD
  if (digitalRead(VBO_OPEN_CMD) == HIGH && VBO_RESET == true) {
    if ( millis() - VBOtime > reset_wait) {
       digitalWrite(VBO_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit1(VBO_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VBO_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VBOtime = millis();
       VBO_CHECK = true; 
    }
  }
  // Check VBO Close Status
  if (digitalRead(VBO_CLOSE_STATUS) == HIGH && digitalRead(VBO_OPEN_STATUS) == LOW && VBO_CHECK == true) {
    if ( millis() - VBOtime > check_wait) {
      digitalWrite(VBO_CLOSE_CMD,LOW);  // CLOSE VALVE
      VBOtime = millis();
      VBO_CHECK = false;
      VBO_RESET = true;
    }
  }
/*
 * VBI Valve case
 */
  // Reset VBI_CLOSE_CMD
  if (digitalRead(VBI_CLOSE_CMD) == LOW && VBI_RESET == true) {
    if ( millis() - VBItime > reset_wait) {
       digitalWrite(VBI_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit1(VBI_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VBI_RESET = false;  
    }
  }
  // Reset VBI_OPEN_CMD
  if (digitalRead(VBI_OPEN_CMD) == HIGH && VBI_RESET == true) {
    if ( millis() - VBItime > reset_wait) {
       digitalWrite(VBI_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit1(VBI_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VBI_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VBItime = millis();
       VBI_CHECK = true; 
    }
  }
  // Check VBI Close Status
  if (digitalRead(VBI_CLOSE_STATUS) == HIGH && digitalRead(VBI_OPEN_STATUS) == LOW && VBI_CHECK == true) {
    if ( millis() - VBItime > check_wait) {
      digitalWrite(VBI_CLOSE_CMD,LOW);  // CLOSE VALVE
      VBItime = millis();
      VBI_CHECK = false;
      VBI_RESET = true;
    }
  }
/*
 * V2425 Valve case
 */
  // Reset V2425_CLOSE_CMD
  if (digitalRead(V2425_CLOSE_CMD) == LOW && V2425_RESET == true) {
    if ( millis() - V2425time > reset_wait) {
       digitalWrite(V2425_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit1(V2425_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       V2425_RESET = false;  
    }
  }
  // Reset V2425_OPEN_CMD
  if (digitalRead(V2425_OPEN_CMD) == HIGH && V2425_RESET == true) {
    if ( millis() - V2425time > reset_wait) {
       digitalWrite(V2425_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit1(V2425_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       V2425_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V2425time = millis();
       V2425_CHECK = true; 
    }
  }
  // Check V2425 Close Status
  if (digitalRead(V2425_CLOSE_STATUS) == HIGH && digitalRead(V2425_OPEN_STATUS) == LOW && V2425_CHECK == true) {
    if ( millis() - V2425time > check_wait) {
      digitalWrite(V2425_CLOSE_CMD,LOW);  // CLOSE VALVE
      V2425time = millis();
      V2425_CHECK = false;
      V2425_RESET = true;
    }
  }
/*
 * VGAS Valve case
 */
  // Reset VGAS_CLOSE_CMD
  if (digitalRead(VGAS_CLOSE_CMD) == LOW && VGAS_RESET == true) {
    if ( millis() - VGAStime > reset_wait) {
       digitalWrite(VGAS_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit2(VGAS_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       VGAS_RESET = false;  
    }
  }
  // Reset VGAS_OPEN_CMD
  if (digitalRead(VGAS_OPEN_CMD) == HIGH && VGAS_RESET == true) {
    if ( millis() - VGAStime > reset_wait) {
       digitalWrite(VGAS_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit2(VGAS_OPEN_CMD_BIT,0x00); // RESET OPEN BIT 
       VGAS_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       VGAStime = millis();
       VGAS_CHECK = true; 
    }
  }
  // Check VGAS Close Status
  if (digitalRead(VGAS_CLOSE_STATUS) == HIGH && digitalRead(VGAS_OPEN_STATUS) == LOW && VGAS_CHECK == true) {
    if ( millis() - VGAStime > check_wait) {
      digitalWrite(VGAS_CLOSE_CMD,LOW);  // CLOSE VALVE
      VGAStime = millis();
      VGAS_CHECK = false;
      VGAS_RESET = true;
    }
  }
/*
 *  P1 Case
 */
  // Reset P1_OFF_CMD
  if (digitalRead(P1_OFF_CMD) == LOW && P1_RESET == true) {
    if ( millis() - P1time > reset_wait2) {
      digitalWrite(P1_OFF_CMD,HIGH);    // RESET SWITCH OFF SCROLL/STAGE
      I2CsetBit2(P1_OFF_CMD_BIT,0x01);   // RESET
      P1_RESET = false;
    }
  }
  // Reset P1_ON_CMD
  if (digitalRead(P1_ON_CMD) == HIGH && P1_RESET == true) {
    if ( millis() - P1time > reset_wait2) {
      digitalWrite(P1_ON_CMD,LOW);      // RESET SWITCH ON SCROLL/STAGE
      I2CsetBit2(P1_ON_CMD_BIT,0x00);    // RESET
      P1_RESET = false;  
    }
  }
/*
 *  BYPASS Case
 */
  // Reset BYPASS_OFF_CMD
  if (digitalRead(BYPASS_OFF_CMD) == LOW && BYPASS_RESET == true) {
    if ( millis() - BYPASStime > reset_wait) {
      digitalWrite(BYPASS_OFF_CMD,HIGH);    // RESET SwITCH OFF BYPASS 
      I2CsetBit2(BYPASS_OFF_CMD_BIT,0x01);   // RESET
      BYPASS_RESET = false;
    }
  }
  // Reset BYPASS_ON_CMD
  if (digitalRead(BYPASS_ON_CMD) == HIGH && BYPASS_RESET == true) {
    if ( millis() - BYPASStime > reset_wait) {
      digitalWrite(BYPASS_ON_CMD,LOW);      // RESET SWITCH ON BYPASS
      I2CsetBit2(BYPASS_ON_CMD_BIT,0x00);    // RESET
      BYPASS_RESET = false;
    }
  }
  
  /***********************************************************************************************************/
  // Check Reset Status
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer2,ARD_RESET_BIT) == 0x01)
    resetArd();
  /***********************************************************************************************************/
}
void UpdateIOFromI2C()
{    
  /***********************************************************************************************************/
  /* Update Valve VPO position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer1,VPO_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer1,VPO_OPEN_CMD_BIT) == 0x00 && VPO_RESET == false) {
     digitalWrite(VPO_CLOSE_CMD,LOW);   // CLOSE VALVE
     VPOtime = millis();
     VPO_RESET = true;
  }
  else if (bitRead(i2c_buffer1,VPO_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer1,VPO_CLOSE_CMD_BIT) == 0x01 && VPO_RESET == false) {
     
     digitalWrite(VPO_OPEN_CMD,HIGH);   // OPEN VALVE
     VPOtime = millis();
     VPO_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VPI position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer1,VPI_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer1,VPI_OPEN_CMD_BIT) == 0x00 && VPI_RESET == false) {
     digitalWrite(VPI_CLOSE_CMD,LOW);   // CLOSE VALVE
     VPItime = millis();
     VPI_RESET = true;
  }
  else if (bitRead(i2c_buffer1,VPI_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer1,VPI_CLOSE_CMD_BIT) == 0x01 && VPI_RESET == false) {
     digitalWrite(VPI_OPEN_CMD,HIGH);   // OPEN VALVE
     VPItime = millis();
     VPI_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VP1 position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer1,VP1_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer1,VP1_OPEN_CMD_BIT) == 0x00 && VP1_RESET == false) {
     digitalWrite(VP1_CLOSE_CMD,LOW);   // CLOSE VALVE
     VP1time = millis();
     VP1_RESET = true;
  }
  else if (bitRead(i2c_buffer1,VP1_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer1,VP1_CLOSE_CMD_BIT) == 0x01 && VP1_RESET == false) {
     digitalWrite(VP1_OPEN_CMD,HIGH);   // OPEN VALVE
     VP1time = millis();
     VP1_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VO position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer1,VO_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer1,VO_OPEN_CMD_BIT) == 0x00 && VO_RESET == false) {
     digitalWrite(VO_CLOSE_CMD,LOW);   // CLOSE VALVE
     VOtime = millis();
     VO_RESET = true;
  }
  else if (bitRead(i2c_buffer1,VO_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer1,VO_CLOSE_CMD_BIT) == 0x01 && VO_RESET == false) {
     digitalWrite(VO_OPEN_CMD,HIGH);   // OPEN VALVE
     VOtime = millis();
     VO_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VI position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer1,VI_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer1,VI_OPEN_CMD_BIT) == 0x00 && VI_RESET == false) {
     digitalWrite(VI_CLOSE_CMD,LOW);   // CLOSE VALVE
     VItime = millis();
     VI_RESET = true;
  }
  else if (bitRead(i2c_buffer1,VI_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer1,VI_CLOSE_CMD_BIT) == 0x01 && VI_RESET == false) {
     digitalWrite(VI_OPEN_CMD,HIGH);   // OPEN VALVE
     VItime = millis();
     VI_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VBO position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer1,VBO_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer1,VBO_OPEN_CMD_BIT) == 0x00 && VBO_RESET == false) {
     digitalWrite(VBO_CLOSE_CMD,LOW);   // CLOSE VALVE
     VBOtime = millis();
     VBO_RESET = true;
     if (bypassON == true ) {  // CLOSE ALSO VBI VALVE
        digitalWrite(VBI_CLOSE_CMD,LOW);   // CLOSE VALVE
        VBItime = millis();
        VBI_RESET = true;
     }
  }
  else if (bitRead(i2c_buffer1,VBO_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer1,VBO_CLOSE_CMD_BIT) == 0x01 && VBO_RESET == false) {
     digitalWrite(VBO_OPEN_CMD,HIGH);   // OPEN VALVE
     VBOtime = millis();
     VBO_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VBI position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer1,VBI_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer1,VBI_OPEN_CMD_BIT) == 0x00 && VBI_RESET == false) {
     digitalWrite(VBI_CLOSE_CMD,LOW);   // CLOSE VALVE
     VBItime = millis();
     VBI_RESET = true;
     if (bypassON == true ) {  // CLOSE ALSO VBO VALVE
        digitalWrite(VBO_CLOSE_CMD,LOW);   // CLOSE VALVE
        VBOtime = millis();
        VBO_RESET = true;
     }
  }
  else if (bitRead(i2c_buffer1,VBI_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer1,VBI_CLOSE_CMD_BIT) == 0x01 && VBI_RESET == false) {
     digitalWrite(VBI_OPEN_CMD,HIGH);   // OPEN VALVE
     VBItime = millis();
     VBI_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve V2425 position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer1,V2425_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer1,V2425_OPEN_CMD_BIT) == 0x00 && V2425_RESET == false) {
     digitalWrite(V2425_CLOSE_CMD,LOW);   // CLOSE VALVE
     V2425time = millis();
     V2425_RESET = true;
  }
  else if (bitRead(i2c_buffer1,V2425_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer1,V2425_CLOSE_CMD_BIT) == 0x01 && V2425_RESET == false) {
     digitalWrite(V2425_OPEN_CMD,HIGH);   // OPEN VALVE
     V2425time = millis();
     V2425_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Valve VGAS position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer2,VGAS_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer2,VGAS_OPEN_CMD_BIT) == 0x00 && VGAS_RESET == false) {
     digitalWrite(VGAS_CLOSE_CMD,LOW);   // CLOSE VALVE
     VGAStime = millis();
     VGAS_RESET = true;
  }
  else if (bitRead(i2c_buffer2,VGAS_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer2,VGAS_CLOSE_CMD_BIT) == 0x01 && VGAS_RESET == false) {
     digitalWrite(VGAS_OPEN_CMD,HIGH);   // OPEN VALVE
     VGAStime = millis();
     VGAS_RESET = true;
  }
  /***********************************************************************************************************/
  /* Update Scroll P1 position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer2,P1_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer2,P1_ON_CMD_BIT) == 0x00 && P1_RESET == false) {
     digitalWrite(P1_OFF_CMD,LOW);     // SWITCH OFF SCROLL
     P1time = millis();
     P1_RESET = true; 
  }
  else if (bitRead(i2c_buffer2,P1_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer2,P1_OFF_CMD_BIT) == 0x01 && P1_RESET == false) { 
     digitalWrite(P1_ON_CMD,HIGH);     // SWITCH ON SCROLL
     P1time = millis();
     P1_RESET = true; 
  }
  /***********************************************************************************************************/
  /* Update BYPASS position (ON/OFF) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer2,BYPASS_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer2,BYPASS_ON_CMD_BIT) == 0x00 && BYPASS_RESET == false) {
     digitalWrite(BYPASS_OFF_CMD,LOW);     // SWITCH OFF BYPASS
     BYPASStime = millis();
     BYPASS_RESET = true; 
  }
  else if (bitRead(i2c_buffer2,BYPASS_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer2,BYPASS_OFF_CMD_BIT) == 0x01 && BYPASS_RESET == false) { 
     digitalWrite(BYPASS_ON_CMD,HIGH);     // SWITCH ON BYPASS
     BYPASStime = millis();
     BYPASS_RESET = true; 
  }
}

void UpdateI2CFromIO()
{ 
  /***********************************************************************************************************/
  /* Update VPO Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VPO_OPEN_STATUS) == HIGH && digitalRead(VPO_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit1(VPO_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VPO_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VPO_CLOSE_STATUS) == HIGH && digitalRead(VPO_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VPO_OPEN_STATUS_BIT == 0x01 && VPO_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VPO_CLOSE_CMD,LOW);  // CLOSE VALVE
        VPOtime = millis();
        VPO_RESET = true;
     }
     I2CsetBit1(VPO_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VPO_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit1(VPO_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VPO_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VPI Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VPI_OPEN_STATUS) == HIGH && digitalRead(VPI_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit1(VPI_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VPI_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VPI_CLOSE_STATUS) == HIGH && digitalRead(VPI_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VPI_OPEN_STATUS_BIT == 0x01 && VPI_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VPI_CLOSE_CMD,LOW);  // CLOSE VALVE
        VPItime = millis();
        VPI_RESET = true;
     }
     I2CsetBit1(VPI_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VPI_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit1(VPI_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VPI_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VP1 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VP1_OPEN_STATUS) == HIGH && digitalRead(VP1_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit1(VP1_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VP1_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VP1_CLOSE_STATUS) == HIGH && digitalRead(VP1_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VP1_OPEN_STATUS_BIT == 0x01 && VP1_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VP1_CLOSE_CMD,LOW);  // CLOSE VALVE
        VP1time = millis();
        VP1_RESET = true;
     }
     I2CsetBit1(VP1_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VP1_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit1(VP1_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VP1_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VO Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VO_OPEN_STATUS) == HIGH && digitalRead(VO_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit1(VO_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VO_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VO_CLOSE_STATUS) == HIGH && digitalRead(VO_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VO_OPEN_STATUS_BIT == 0x01 && VO_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VO_CLOSE_CMD,LOW);  // CLOSE VALVE
        VOtime = millis();
        VO_RESET = true;
     }
     I2CsetBit1(VO_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VO_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit1(VO_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VO_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VI Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VI_OPEN_STATUS) == HIGH && digitalRead(VI_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit1(VI_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VI_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VI_CLOSE_STATUS) == HIGH && digitalRead(VI_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VI_OPEN_STATUS_BIT == 0x01 && VI_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VI_CLOSE_CMD,LOW);  // CLOSE VALVE
        VItime = millis();
        VI_RESET = true;
     }
     I2CsetBit1(VI_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VI_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit1(VI_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VI_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VBO Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VBO_OPEN_STATUS) == HIGH && digitalRead(VBO_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit1(VBO_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VBO_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VBO_CLOSE_STATUS) == HIGH && digitalRead(VBO_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VBO_OPEN_STATUS_BIT == 0x01 && VBO_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VBO_CLOSE_CMD,LOW);  // CLOSE VALVE
        VBOtime = millis();
        VBO_RESET = true;
     }
     I2CsetBit1(VBO_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VBO_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit1(VBO_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VBO_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VBI Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VBI_OPEN_STATUS) == HIGH && digitalRead(VBI_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit1(VBI_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VBI_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VBI_CLOSE_STATUS) == HIGH && digitalRead(VBI_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VBI_OPEN_STATUS_BIT == 0x01 && VBI_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VBI_CLOSE_CMD,LOW);  // CLOSE VALVE
        VBItime = millis();
        VBI_RESET = true;
     }
     I2CsetBit1(VBI_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VBI_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit1(VBI_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(VBI_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update V2425 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V2425_OPEN_STATUS) == HIGH && digitalRead(V2425_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit1(V2425_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(V2425_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V2425_CLOSE_STATUS) == HIGH && digitalRead(V2425_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (V2425_OPEN_STATUS_BIT == 0x01 && V2425_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V2425_CLOSE_CMD,LOW);  // CLOSE VALVE
        V2425time = millis();
        V2425_RESET = true;
     }
     I2CsetBit1(V2425_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(V2425_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit1(V2425_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit1(V2425_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update VGAS Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(VGAS_OPEN_STATUS) == HIGH && digitalRead(VGAS_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit2(VGAS_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit2(VGAS_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(VGAS_CLOSE_STATUS) == HIGH && digitalRead(VGAS_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     if (VGAS_OPEN_STATUS_BIT == 0x01 && VGAS_CLOSE_STATUS_BIT == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(VGAS_CLOSE_CMD,LOW);  // CLOSE VALVE
        VGAStime = millis();
        VGAS_RESET = true;
     }
     I2CsetBit2(VGAS_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit2(VGAS_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit2(VGAS_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit2(VGAS_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update BYPASS position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(BYPASS_STATUS) == HIGH) {    // BYPASS ON STATUS
     I2CsetBit2(BYPASS_STATUS_BIT,0x01);        // UPDATE BYPASS ON BIT
     bypassON = true;
  }
  else if (digitalRead(BYPASS_STATUS) == LOW) { // BYPASS OFF STATUS
     I2CsetBit2(BYPASS_STATUS_BIT,0x00);         // UPDATE BYPASS OFF BIT
     bypassON = false;
  }
  /***********************************************************************************************************/
  /* Update P1 Scroll position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(P1_STATUS) == HIGH) {    // SCROLL ON STATUS
     I2CsetBit2(P1_STATUS_BIT,0x00);        // UPDATE SCROLL ON BIT
  }
  else if (digitalRead(P1_STATUS) == LOW) { // SCROLL OFF STATUS
     I2CsetBit2(P1_STATUS_BIT,0x01);         // UPDATE SCROLL OFF BIT
  }
  /***********************************************************************************************************/
  /* Update COMPRESSAIR position STATUS bit (On/Off) */
  /***********************************************************************************************************/
  if (digitalRead(COMPRESSAIR_STATUS) == HIGH) {    // COMPRESS AIR OK STATUS
     I2CsetBit2(COMPRESSAIR_STATUS_BIT,0x01);        // UPDATE COMPRESSAIR KO BIT
  }
  else if (digitalRead(COMPRESSAIR_STATUS) == LOW) { // COMPRESS AIR KO STATUS
     I2CsetBit2(COMPRESSAIR_STATUS_BIT,0x00);         // UPDATE COMPRESSAIR OK BIT
  }

}
