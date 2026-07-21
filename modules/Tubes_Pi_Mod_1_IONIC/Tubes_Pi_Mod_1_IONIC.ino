/*
  Modbus Server
 A modbus server to monitor the ionic pumping station (V31/V32 valves) using Controllino board
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
 * (valve commands disabled: V31/V32 not operable for now)
 */
//unsigned long V31time = 0;
//unsigned long V32time = 0;
unsigned long looptime = 0;


/*
 *  These RESET are used to reset switches (Command outputs)
 */
//boolean V31_RESET = false;
//boolean V32_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;

 /*
 *  These CHECK are used to check switches status (Status inputs)
 */
//boolean V31_CHECK = false;
//boolean V32_CHECK = false;

/*
 *  The waiting time before checking switches status
 */
//long check_wait = 10000;

/*
 *  I2C BIT ASSIGNATION (MAX = 32)
 *  Must match the Controllino device in work_ionic
 */
// Valve commands disabled: V31/V32 not operable for now
//#define V31_OPEN_CMD_BIT           0   // V31 Open bit
//#define V31_CLOSE_CMD_BIT          1   // V31 Close bit
//#define V32_OPEN_CMD_BIT           2   // V32 Open bit
//#define V32_CLOSE_CMD_BIT          3   // V32 Close bit
#define V31_OPEN_STATUS_BIT        4   // V31 Open Status bit
#define V31_CLOSE_STATUS_BIT       5   // V31 Close Status bit
#define V32_OPEN_STATUS_BIT        6   // V32 Open Status bit
#define V32_CLOSE_STATUS_BIT       7   // V32 Close Status bit

#define ARD_RESET_BIT              31  // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */
// digital pins (OUTPUTS)
// (valve commands disabled: V31/V32 not operable for now)
//#define V31_OPEN_CMD   CONTROLLINO_R0  //(Open Command V31)
//#define V31_CLOSE_CMD  CONTROLLINO_R1  //(Close Command V31)
//#define V32_OPEN_CMD   CONTROLLINO_R2  //(Open Command V32)
//#define V32_CLOSE_CMD  CONTROLLINO_R3  //(Close Command V32)


// digital pins (INTPUTS)
#define V31_OPEN_STATUS    CONTROLLINO_A0    // V31 OPEN STATUS
#define V31_CLOSE_STATUS   CONTROLLINO_A1    // V31 CLOSE STATUS
#define V32_OPEN_STATUS    CONTROLLINO_A2    // V32 OPEN STATUS
#define V32_CLOSE_STATUS   CONTROLLINO_A3    // V32 CLOSE STATUS

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

  // Valve commands disabled: V31/V32 not operable for now
  /*if (updateIOFromI2CBool == true) {
    Serial.print("Received command; updated i2c_buffer =");
    Serial.println(i2c_buffer,BIN);
    updateIOFromI2CBool = false;
    // Update I/O from updated i2c_buffer
    UpdateIOFromI2C();
  }*/

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
  // (valve commands disabled: V31/V32 not operable for now)
  /*digitalWrite(V31_OPEN_CMD,LOW);                           // Set V31_OPEN_CMD LOW
  digitalWrite(V31_CLOSE_CMD,HIGH);                         // Set V31_CLOSE_CMD HIGH
  pinMode(V31_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve
  pinMode(V31_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit(V31_OPEN_CMD_BIT,0x00);                         // Set V31_OPEN_CMD_BIT LOW
  I2CsetBit(V31_CLOSE_CMD_BIT,0x01);                        // Set V31_CLOSE_CMD_BIT HIGH
  digitalWrite(V32_OPEN_CMD,LOW);                           // Set V32_OPEN_CMD LOW
  digitalWrite(V32_CLOSE_CMD,HIGH);                         // Set V32_CLOSE_CMD HIGH
  pinMode(V32_OPEN_CMD, OUTPUT);                            // Set the digital pin as output for Open Valve
  pinMode(V32_CLOSE_CMD, OUTPUT);                           // Set the digital pin as output for Close Valve
  I2CsetBit(V32_OPEN_CMD_BIT,0x00);                         // Set V32_OPEN_CMD_BIT LOW
  I2CsetBit(V32_CLOSE_CMD_BIT,0x01);                        // Set V32_CLOSE_CMD_BIT HIGH*/

  // Digital INPUTS assignation
  pinMode(V31_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V31 OPEN STATUS
  pinMode(V31_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V31 CLOSE STATUS
  pinMode(V32_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V32 OPEN STATUS
  pinMode(V32_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V32 CLOSE STATUS

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
 *  V31 Valve Case (commands disabled: V31/V32 not operable for now)
 */
  /*
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
  */
/*
 * V32 Valve case (commands disabled: V31/V32 not operable for now)
 */
  /*
  // Reset V32_CLOSE_CMD
  if (digitalRead(V32_CLOSE_CMD) == LOW && V32_RESET == true) {
    if ( millis() - V32time > reset_wait) {
       digitalWrite(V32_CLOSE_CMD,HIGH);  // RESET CLOSE VALVE
       I2CsetBit(V32_CLOSE_CMD_BIT,0x01); // RESET CLOSE BIT
       V32_RESET = false;
    }
  }
  // Reset V32_OPEN_CMD
  if (digitalRead(V32_OPEN_CMD) == HIGH && V32_RESET == true) {
    if ( millis() - V32time > reset_wait) {
       digitalWrite(V32_OPEN_CMD,LOW);   // RESET OPEN VALVE
       I2CsetBit(V32_OPEN_CMD_BIT,0x00); // RESET OPEN BIT
       V32_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V32time = millis();
       V32_CHECK = true;
    }
  }
  // Check V32 Close Status
  if (digitalRead(V32_CLOSE_STATUS) == HIGH && digitalRead(V32_OPEN_STATUS) == LOW && V32_CHECK == true) {
    if ( millis() - V32time > check_wait) {
      digitalWrite(V32_CLOSE_CMD,LOW);  // CLOSE VALVE
      V32time = millis();
      V32_CHECK = false;
      V32_RESET = true;
    }
  }
  */
  /***********************************************************************************************************/
  // Check Reset Status
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,ARD_RESET_BIT) == 0x01)
    resetArd();
  /***********************************************************************************************************/
}
// Valve commands disabled: V31/V32 not operable for now
/*void UpdateIOFromI2C()
{
  /***********************************************************************************************************/
  /* Update Valve V31 position (Open/Close) */
  /***********************************************************************************************************/
  /*if (bitRead(i2c_buffer,V31_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V31_OPEN_CMD_BIT) == 0x00 && V31_RESET == false) {
     digitalWrite(V31_CLOSE_CMD,LOW);   // CLOSE VALVE
     V31time = millis();
     V31_RESET = true;
  }
  else if (bitRead(i2c_buffer,V31_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V31_CLOSE_CMD_BIT) == 0x01 && V31_RESET == false) {
     digitalWrite(V31_OPEN_CMD,HIGH);   // OPEN VALVE
     V31time = millis();
     V31_RESET = true;
  }*/
  /***********************************************************************************************************/
  /* Update Valve V32 position (Open/Close) */
  /***********************************************************************************************************/
  /*if (bitRead(i2c_buffer,V32_CLOSE_CMD_BIT) == 0x00 && bitRead(i2c_buffer,V32_OPEN_CMD_BIT) == 0x00 && V32_RESET == false) {
     digitalWrite(V32_CLOSE_CMD,LOW);   // CLOSE VALVE
     V32time = millis();
     V32_RESET = true;
  }
  else if (bitRead(i2c_buffer,V32_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,V32_CLOSE_CMD_BIT) == 0x01 && V32_RESET == false) {
     digitalWrite(V32_OPEN_CMD,HIGH);   // OPEN VALVE
     V32time = millis();
     V32_RESET = true;
  }
}*/

void UpdateI2CFromIO()
{
  /***********************************************************************************************************/
  /* Update V31 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V31_OPEN_STATUS) == HIGH && digitalRead(V31_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V31_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V31_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V31_CLOSE_STATUS) == HIGH && digitalRead(V31_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     // (commands disabled: V31/V32 not operable for now)
     /*if (bitRead(i2c_buffer,V31_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V31_CLOSE_STATUS_BIT) == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V31_CLOSE_CMD,LOW);  // CLOSE VALVE
        V31time = millis();
        V31_RESET = true;
     }*/
     I2CsetBit(V31_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V31_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V31_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V31_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  /***********************************************************************************************************/
  /* Update V32 Valve position STATUS bit (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(V32_OPEN_STATUS) == HIGH && digitalRead(V32_CLOSE_STATUS) == LOW) { // OPEN VALVE STATUS
     I2CsetBit(V32_OPEN_STATUS_BIT,0x01);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V32_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }
  else if (digitalRead(V32_CLOSE_STATUS) == HIGH && digitalRead(V32_OPEN_STATUS) == LOW) { // CLOSE VALVE STATUS
     // (commands disabled: V31/V32 not operable for now)
     /*if (bitRead(i2c_buffer,V32_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V32_CLOSE_STATUS_BIT) == 0x00) { // IF OPEN VALVE STATUS BIT
        // reset close command
        digitalWrite(V32_CLOSE_CMD,LOW);  // CLOSE VALVE
        V32time = millis();
        V32_RESET = true;
     }*/
     I2CsetBit(V32_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V32_CLOSE_STATUS_BIT,0x01);  // UPDATE CLOSE VALVE BIT
  }
  else {
     // MOVING VALVE STATUS BIT
     I2CsetBit(V32_OPEN_STATUS_BIT,0x00);   // UPDATE OPEN VALVE BIT
     I2CsetBit(V32_CLOSE_STATUS_BIT,0x00);  // UPDATE CLOSE VALVE BIT
  }

}
