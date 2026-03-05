/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include "Adafruit_MCP9600.h" // includes Wire library; reads temperature as master; can be configured as slave
#include <Controllino.h> // Controllino library


// Conversion tool

typedef union {
    float fvalue;
    uint32_t i32value;
  } FloatUint32;
  
FloatUint32 temp;

// The K thermocouple sensor (I2C communication using SoftwareWire)

Adafruit_MCP9600 mcp;

/*
 * I2C ADDRESS (SLAVE)
 */ 
#define I2C_SLAVE_ADDR 0x10
/* 
 * I2C BUFFER SIZE
 */
#define I2C_BUFFER 32

uint32_t i2c_buffer = 0; 

/*
 * The time Reset actions
 */
unsigned long FANtime = 0;
unsigned long NORMAL_SPEEDtime_1 = 0;
unsigned long LOW_NOISEtime_1 = 0;
unsigned long NORMAL_SPEEDtime_2 = 0;
unsigned long LOW_NOISEtime_2 = 0;
unsigned long debugtime = 0;
unsigned long temptime = 0;


/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean FAN_RESET = false;
boolean LOW_NOISE_RESET = false;
boolean LOW_NOISE_RESET_1 = false;
boolean LOW_NOISE_RESET_2 = false;
boolean NORMAL_SPEED_RESET = false;
boolean NORMAL_SPEED_RESET_1 = false;
boolean NORMAL_SPEED_RESET_2 = false;


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
long temp_wait = 100;

/*
 *  I2C BIT ASSIGNATION (MAX = 32) 
 */
#define NORMAL_SPEED_OPEN_CMD_BIT   0   // NORMAL SPEED Open bit
#define NORMAL_SPEED_CLOSE_CMD_BIT  1   // NORMAL SPEED Close bit
#define LOW_NOISE_OPEN_CMD_BIT      2   // LOW NOISE Open bit
#define LOW_NOISE_CLOSE_CMD_BIT     3   // LOW NOISE Close bit
#define FAN_START_CMD_BIT           4   // FAN_START Open bit
#define FAN_STOP_CMD_BIT            5   // FAN_START Close bit
#define FAN_START_STATUS_BIT        6   // FAN START Status bit
#define FAN_STOP_STATUS_BIT         7   // FAN STOP Status bit
#define ARD_RESET_BIT               8   // Controllino Reset Bit

/*
 * CONTROLLINO I/O ASSIGNATION
 */ 
// digital pins (OUTPUTS)
#define NORMAL_SPEED_OPEN_CMD   CONTROLLINO_D0   // Open Command NORMAL SPEED
#define NORMAL_SPEED_CLOSE_CMD  CONTROLLINO_D2   // Close Command NORMAL SPEED
#define LOW_NOISE_OPEN_CMD      CONTROLLINO_D1   // Open Command LOW NOISE
#define LOW_NOISE_CLOSE_CMD     CONTROLLINO_D3   // Close Command LOW NOISE
#define FAN_START_CMD           CONTROLLINO_D4   // Start Command FAN_START
#define FAN_STOP_CMD            CONTROLLINO_D5   // Stop Command FAN_STOP
#define FAN_START_STATUS        CONTROLLINO_A0   // FAN START STATUS
#define FAN_STOP_STATUS         CONTROLLINO_A1   // FAN STOP STATUS
#define MASTER_IN_STATUS        CONTROLLINO_A2   // MASTER IN STATUS
#define MASTER_OUT_STATUS       CONTROLLINO_A3   // MASTER OUT STATUS

void(*resetArd) (void) = 0; //declare reset function @ address 0

void setup() {

  // Open Serial communication for Console port.
 Serial.begin(9600);

 /* Initialise the driver with I2C_ADDRESS and the default I2C bus. */
    if (! mcp.begin(0x60)) {
        Serial.println("Sensor not found. Check wiring!");
        while (1);
    }
 Serial.println("Found MCP9600!");

 mcp.setADCresolution(MCP9600_ADCRESOLUTION_18);
 Serial.print("ADC resolution set to ");
  switch (mcp.getADCresolution()) {
    case MCP9600_ADCRESOLUTION_18:   Serial.print("18"); break;
    case MCP9600_ADCRESOLUTION_16:   Serial.print("16"); break;
    case MCP9600_ADCRESOLUTION_14:   Serial.print("14"); break;
    case MCP9600_ADCRESOLUTION_12:   Serial.print("12"); break;
  }
  Serial.println(" bits");
  mcp.setThermocoupleType(MCP9600_TYPE_K);
  Serial.print("Thermocouple type set to ");
  switch (mcp.getThermocoupleType()) {
    case MCP9600_TYPE_K:  Serial.print("K"); break;
    case MCP9600_TYPE_J:  Serial.print("J"); break;
    case MCP9600_TYPE_T:  Serial.print("T"); break;
    case MCP9600_TYPE_N:  Serial.print("N"); break;
    case MCP9600_TYPE_S:  Serial.print("S"); break;
    case MCP9600_TYPE_E:  Serial.print("E"); break;
    case MCP9600_TYPE_B:  Serial.print("B"); break;
    case MCP9600_TYPE_R:  Serial.print("R"); break;
  }
  Serial.println(" type");

  mcp.setFilterCoefficient(3);
  Serial.print("Filter coefficient value set to: ");
  Serial.println(mcp.getFilterCoefficient());

  mcp.enable(true);

 InitializeIO();
 InitializeI2C();
}
 
void loop() {  

  ResetAndCheck();
   
  // For debug purpose
  if ( millis() - debugtime > debug_wait) {
     Serial.print("i2c_buffer=");
     Serial.println(i2c_buffer,BIN);
     debugtime = millis();
  }
  // For reading temperature
  if ( millis() - temptime > temp_wait && digitalRead(MASTER_IN_STATUS) == LOW ) {
     // Lock Master
     digitalWrite(MASTER_OUT_STATUS, HIGH);
     delay(100);
     Wire.end();
     Wire.begin();
     // Read Temp (as Master)
     readTemp();
     Wire.end();
     Wire.begin(I2C_SLAVE_ADDR);
     Serial.print("Temperature=");Serial.println(temp.fvalue);
     temptime = millis();
     // Release Master
     digitalWrite(MASTER_OUT_STATUS, LOW);
     delay(100);
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
 
  // Digital OUTPUTS assignation & initialization (set to NORMAL SPEED status and FAN stop status)
  digitalWrite(NORMAL_SPEED_OPEN_CMD,HIGH);                  // Set NORMAL_SPEED_OPEN_CMD LOW
  digitalWrite(NORMAL_SPEED_CLOSE_CMD,HIGH);                 // Set NORMAL_SPEED_CLOSE_CMD LOW
  pinMode(NORMAL_SPEED_OPEN_CMD, OUTPUT);                    // Set the digital pin as output
  pinMode(NORMAL_SPEED_CLOSE_CMD, OUTPUT);                   // Set the digital pin as output
  I2CsetBit(NORMAL_SPEED_OPEN_CMD_BIT,0x01);                 // Set NORMAL_SPEED_OPEN_CMD_BIT LOW
  I2CsetBit(NORMAL_SPEED_CLOSE_CMD_BIT,0x01);                // Set NORMAL_SPEED_CLOSE_CMD_BIT LOW
  digitalWrite(LOW_NOISE_OPEN_CMD,LOW);                      // Set LOW_NOISE_OPEN_CMD HIGH
  digitalWrite(LOW_NOISE_CLOSE_CMD,LOW);                     // Set LOW_NOISE_CLOSE_CMD HIGH
  pinMode(LOW_NOISE_OPEN_CMD, OUTPUT);                       // Set the digital pin as output
  pinMode(LOW_NOISE_CLOSE_CMD, OUTPUT);                      // Set the digital pin as output
  I2CsetBit(LOW_NOISE_OPEN_CMD_BIT,0x00);                    // Set LOW_NOISE_OPEN_CMD_BIT HIGH
  I2CsetBit(LOW_NOISE_CLOSE_CMD_BIT,0x00);                   // Set LOW_NOISE_CLOSE_CMD_BIT HIGH
  digitalWrite(FAN_START_CMD,LOW);                           // Set FAN_START_CMD LOW
  pinMode(FAN_START_CMD, OUTPUT);                            // Set the digital pin as output
  I2CsetBit(FAN_START_CMD_BIT,0x00);                         // Set FAN_START_CMD_BIT LOW
  digitalWrite(FAN_STOP_CMD,HIGH);                           // Set FAN_STOP_CMD HIGH
  pinMode(FAN_STOP_CMD, OUTPUT);                             // Set the digital pin as output
  I2CsetBit(FAN_STOP_CMD_BIT,0x01);                          // Set FAN_STOP_CMD_BIT HIGH

  pinMode(FAN_START_STATUS, INPUT);                          // Set the digital pin as input for FAN START STATUS
  pinMode(FAN_STOP_STATUS, INPUT);                           // Set the digital pin as input for FAN STOP STATUS

  pinMode(MASTER_IN_STATUS, INPUT);                          // Set the digital pin as input for MASTER IN STATUS
  pinMode(MASTER_OUT_STATUS, OUTPUT);                        // Set the digital pin as output for MASTER OUT STATUS
  digitalWrite(MASTER_OUT_STATUS,LOW);                       // Set MASTER OUT STATUS LOW
}

void requestEvent() {

  // First update i2c_buffer from I/O
  UpdateI2CFromIO();
  
  // Send i2c_buffer to master (create 4 bytes array)
  byte i2c_array[8];
  
  i2c_array[0] = (temp.i32value >> 24) & 0xFF;
  i2c_array[1] = (temp.i32value >> 16) & 0xFF;
  i2c_array[2] = (temp.i32value >> 8) & 0xFF;
  i2c_array[3] = temp.i32value & 0xFF;
  i2c_array[4] = (i2c_buffer >> 24) & 0xFF;
  i2c_array[5] = (i2c_buffer >> 16) & 0xFF;
  i2c_array[6] = (i2c_buffer >> 8) & 0xFF;
  i2c_array[7] = i2c_buffer & 0xFF;

  Wire.write(i2c_array, 8);
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
 *  NORMAL SPEED Case
 */
 // Reset LOW NOISE CMD
 if (LOW_NOISE_RESET_1 == true) {
    if ( millis() - LOW_NOISEtime_1 > reset_wait) {
       Serial.println("RESET LOW 1 ");
       digitalWrite(LOW_NOISE_CLOSE_CMD,HIGH);  // RESET LOW_NOISE CMD
       I2CsetBit(LOW_NOISE_CLOSE_CMD_BIT,0x01); // RESET LOW_NOISE BIT
       LOW_NOISE_RESET_1 = false;
       digitalWrite(NORMAL_SPEED_OPEN_CMD,LOW);  // RESET LOW_NOISE CMD
       I2CsetBit(NORMAL_SPEED_OPEN_CMD_BIT,0x00); // RESET LOW_NOISE BIT
       LOW_NOISEtime_2 = millis();
       LOW_NOISE_RESET_2 = true;
    }
 }
 // Reset LOW NOISE CMD
 if (LOW_NOISE_RESET_2 == true) {
    if ( millis() - LOW_NOISEtime_2 > reset_wait) {
       Serial.println("RESET LOW 2");
       digitalWrite(LOW_NOISE_OPEN_CMD,HIGH);  // RESET LOW_NOISE CMD
       I2CsetBit(LOW_NOISE_OPEN_CMD_BIT,0x01); // RESET LOW_NOISE BIT
       LOW_NOISE_RESET_2 = false;
       LOW_NOISE_RESET = false;
       NORMAL_SPEED_RESET = false;
    }
 }
 // Reset NORMAL_SPEED CMD
 if (NORMAL_SPEED_RESET_1 == true) {
    if ( millis() - NORMAL_SPEEDtime_1 > reset_wait) {
       Serial.println("RESET NORMAL 1");
       digitalWrite(NORMAL_SPEED_OPEN_CMD,HIGH);   // RESET NORMAL_SPEED CMD
       I2CsetBit(NORMAL_SPEED_OPEN_CMD_BIT,0x01);  // RESET NORMAL_SPEED BIT
       NORMAL_SPEED_RESET_1 = false;
       digitalWrite(LOW_NOISE_CLOSE_CMD,LOW);  // RESET LOW_NOISE CMD
       I2CsetBit(LOW_NOISE_CLOSE_CMD_BIT,0x00); // RESET LOW_NOISE BIT
       NORMAL_SPEEDtime_2 = millis();
       NORMAL_SPEED_RESET_2 = true;
    }
 }
 // Reset NORMAL_SPEED CMD
 if (NORMAL_SPEED_RESET_2 == true) {
    if ( millis() - NORMAL_SPEEDtime_2 > reset_wait) {
       Serial.println("RESET NORMAL 2");
       digitalWrite(NORMAL_SPEED_CLOSE_CMD,HIGH);   // RESET NORMAL_SPEED CMD
       I2CsetBit(NORMAL_SPEED_CLOSE_CMD_BIT,0x01);  // RESET NORMAL_SPEED BIT
       NORMAL_SPEED_RESET_2 = false;
       NORMAL_SPEED_RESET = false;
       LOW_NOISE_RESET = false; 
    }
 }
  
/*
 *  FAN Case
 */
 // Reset FAN_STOP_CMD
  if (digitalRead(FAN_STOP_CMD) == LOW && FAN_RESET == true) {
    if ( millis() - FANtime > reset_wait) {
       digitalWrite(FAN_STOP_CMD,HIGH);  // RESET FAN_STOP FAN
       I2CsetBit(FAN_STOP_CMD_BIT,0x01); // RESET FAN_STOP BIT
       FAN_RESET = false;  
    }
  }
  // Reset FAN_START_CMD
  if (digitalRead(FAN_START_CMD) == HIGH && FAN_RESET == true) {
    if ( millis() - FANtime > reset_wait) {
       digitalWrite(FAN_START_CMD,LOW);   // RESET FAN_START FAN
       I2CsetBit(FAN_START_CMD_BIT,0x00); // RESET FAN_START BIT 
       FAN_RESET = false;
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
  /* Update FAN LOW NOISE relay position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,NORMAL_SPEED_OPEN_CMD_BIT) == 0x01 && 
      bitRead(i2c_buffer,LOW_NOISE_OPEN_CMD_BIT) == 0x00 &&
      bitRead(i2c_buffer,NORMAL_SPEED_CLOSE_CMD_BIT) == 0x00 &&
      bitRead(i2c_buffer,LOW_NOISE_CLOSE_CMD_BIT) == 0x00 &&
      LOW_NOISE_RESET == false ) {
     Serial.println("RESET LOW");
     digitalWrite(NORMAL_SPEED_CLOSE_CMD,LOW);   // CLOSE RELAY
     LOW_NOISEtime_1 = millis();
     NORMAL_SPEED_RESET = true;
     LOW_NOISE_RESET = true; 
     LOW_NOISE_RESET_1 = true; 
  }
  
  /***********************************************************************************************************/
  /* Update FAN NORMAL SPEED relay position (Open/Close) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,NORMAL_SPEED_OPEN_CMD_BIT) == 0x00 && 
      bitRead(i2c_buffer,LOW_NOISE_OPEN_CMD_BIT) == 0x00 &&
      bitRead(i2c_buffer,NORMAL_SPEED_CLOSE_CMD_BIT) == 0x00 &&
      bitRead(i2c_buffer,LOW_NOISE_CLOSE_CMD_BIT) == 0x01 &&
      NORMAL_SPEED_RESET == false) {
     Serial.println("RESET NORMAL");
     digitalWrite(LOW_NOISE_OPEN_CMD,LOW);    // CLOSE RELAY
     NORMAL_SPEEDtime_1 = millis();
     LOW_NOISE_RESET = true; 
     NORMAL_SPEED_RESET = true; 
     NORMAL_SPEED_RESET_1 = true; 
     
  }
  
  /***********************************************************************************************************/
  /* Update FAN START/STOP position */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,FAN_START_CMD_BIT) == 0x01 && bitRead(i2c_buffer,FAN_STOP_CMD_BIT) == 0x01 && FAN_RESET == false) {
     digitalWrite(FAN_START_CMD,HIGH);     // FAN_START FAN
     FANtime = millis();
     FAN_RESET = true; 
  }
  else if (bitRead(i2c_buffer,FAN_START_CMD_BIT) == 0x00 && bitRead(i2c_buffer,FAN_STOP_CMD_BIT) == 0x00 && FAN_RESET == false) { 
     digitalWrite(FAN_STOP_CMD,LOW); // FAN_STOP FAN
     FANtime = millis();
     FAN_RESET = true; 
  }
}

void UpdateI2CFromIO()
{ 
  /***********************************************************************************************************/
  /* Update FAN position STATUS bit (START/STOP) */
  /***********************************************************************************************************/
  if (digitalRead(FAN_START_STATUS) == HIGH && digitalRead(FAN_STOP_STATUS) == LOW) { // FAN START STATUS
     I2CsetBit(FAN_START_STATUS_BIT,0x01);   // UPDATE FAN START STATUS BIT
     I2CsetBit(FAN_STOP_STATUS_BIT,0x00);    // UPDATE FAN STOP STATUS BIT
  }
  else if (digitalRead(FAN_STOP_STATUS) == HIGH && digitalRead(FAN_START_STATUS) == LOW) { // FAN STOP STATUS
     /*if (FAN_START_STATUS_BIT == 0x01 && FAN_STOP_STATUS_BIT == 0x00) { // IF FAN STARTED 
        // reset close command
        digitalWrite(FAN_STOP_CMD,LOW);
        FANtime = millis();
        FAN_RESET = true;
     }*/
     I2CsetBit(FAN_START_STATUS_BIT,0x00);   // UPDATE FAN START STATUS BIT
     I2CsetBit(FAN_STOP_STATUS_BIT,0x01);    // UPDATE FAN CLOSE STATUS BIT
  }
  else {
     // ERROR FAN STATUS BIT
     I2CsetBit(FAN_START_STATUS_BIT,0x00);   // UPDATE FAN START STATUS BIT
     I2CsetBit(FAN_STOP_STATUS_BIT,0x00);   // UPDATE FAN STOP STATUS  BIT
  }
}

void readTemp() {
  temp.fvalue = mcp.readThermocouple();
 
}
