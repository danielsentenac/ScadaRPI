/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include <ModbusTCPSlave.h> // Modbus2 library
#include <SoftwareSerial.h> // Serial library
#include <Controllino.h> // Controllino library
#include <Wire.h> // I2C library from Arduino-Wire github project (with timeout)
#include <CRC32.h>

/*
 *  This part is the Arduino sketch code
 */ 
// Ethernet config

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x57, 0xAB };
//const byte ip[] = { 192, 168, 224, 152 }; // Controllino-1 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x58, 0xAB };
//const byte ip[] = { 192, 168, 224, 154 }; // Controllino-2 has this IPip[] = { 192, 168, 224, 152 }; // Controllino-1 has this IP


//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x59, 0xAB };
//IPAddress ip[] = { 192, 168, 224, 155 }; // Controllino-3 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x60, 0xAB };
//const byte ip[] = { 192, 168, 224, 156 }; // Controllino-4 has this IP

byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x61, 0xAB };
IPAddress ip[] = { 192, 168, 224, 157 }; // Controllino-5 has this IP (TUBE SQZ 100N)

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x62, 0xAB };
//const byte ip[] = { 192, 168, 224, 158 }; // Controllino-6 has this IP (TUBE SQZ 200N)

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x76, 0xAB };
//const byte ip[] = { 192, 168, 224, 172 }; // Controllino-20 has this IP  ( TUBE MC )

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x64, 0xAB };
//const byte ip[] = { 192, 168, 224, 160 }; // Controllino-8 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x65, 0xAB };
//const byte ip[] = { 192, 168, 224, 161 }; // Controllino-9 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x73, 0xAB };
//const byte ip[] = { 192, 168, 224, 169 }; // Controllino-17 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x76, 0xAB };
//const byte ip[] = { 192, 168, 224, ??? }; // Controllino-20 has this IP


#define MAX_TRIALS 5
int mgtrials = 0;
int p21trials = 0;

uint32_t tempbuf; // The temporary temperature read

// Modbus objects
ModbusTCPSlave modbus(502);


// I2C objects to communicate with other Controllino (RACK FAN/TEMP)
/* 
 * I2C BUFFER SIZE
 */
#define I2C_BUFFER 32

uint32_t i2c_buffer = 0;

// Define Slave I2C Address
#define SLAVE_ADDR 0x10

/*
 * The time for Check and Reset actions
 */
unsigned long V21time = 0;
unsigned long V22time = 0;
unsigned long P22time = 0;
unsigned long FANtime = 0;
unsigned long P21_ONOFFtime = 0;
unsigned long P21_STDBYtime = 0;
unsigned long MGtime[] = {0,0,0,0,0,0};
unsigned long global = millis(); // For debug
boolean delstartfan = false;     // For debug
boolean delstopfan = false;      // For debug

/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean V21_RESET = false;
boolean V22_RESET = false;
boolean P22_RESET = false;
boolean FAN_RESET = false;
boolean P21_ONOFF_RESET = false;
boolean P21_STDBY_RESET = false;
boolean MG_RESET[] = {false,false,false,false,false,false};

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 6000;
long resetinstrument_wait = 5000;

 /*
 *  These CHECK are used to check switches status (Status inputs)
 */
boolean V21_CHECK = false;
boolean V22_CHECK = false;

/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;

// digital pins (OUTPUTS)
#define V21_OPEN_CMD CONTROLLINO_R0  //(Open Command V21)
#define V21_CLOSE_CMD CONTROLLINO_R1 //(Close Command V21)
#define V22_OPEN_CMD CONTROLLINO_R2  //(Open Command V22)
#define V22_CLOSE_CMD CONTROLLINO_R3 //(Close Command V22)
#define P22_ON_SCROLL CONTROLLINO_R4   //(Command Switch ON Scroll P22)
#define P22_OFF_SCROLL CONTROLLINO_R5  //(Command Switch OFF Scroll P22)

// digital pins (INTPUTS)
#define V21_OPEN_STATUS CONTROLLINO_A5   // V21 OPEN STATUS
#define V21_CLOSE_STATUS CONTROLLINO_A6  // V21 CLOSE STATUS
#define V22_OPEN_STATUS CONTROLLINO_A7   // V22 OPEN STATUS
#define V22_CLOSE_STATUS CONTROLLINO_A8  // V22 CLOSE STATUS
#define P22_STATUS CONTROLLINO_A9     // P22 ON/OFF STATUS
#define V23_OPEN_STATUS CONTROLLINO_IN0   // V23 OPEN STATUS
#define V23_CLOSE_STATUS CONTROLLINO_IN1  // V23 CLOSE STATUS
#define COMPRESS_AIR_STATUS CONTROLLINO_A0  // COMPRESS AIR STATUS
#define VA1_OPEN_STATUS CONTROLLINO_A1   // VA1 OPEN STATUS
#define VA1_CLOSE_STATUS CONTROLLINO_A2  // VA1 CLOSE STATUS
#define VA2_OPEN_STATUS CONTROLLINO_A3   // VA2 OPEN STATUS
#define VA2_CLOSE_STATUS CONTROLLINO_A4  // VA2 CLOSE STATUS

// RS485
#define RS485_TRANSEIVER_STATUS  A13  // RS485 TRANSEIVER STATE
#define MAXIGAUGE_TRANSEIVER_STATUS  A12  // RS485 Maxigauge TRANSEIVER STATE

//SoftwareSerial XGSSerial(CONTROLLINO_D11, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_15); // RX, TX for Controllino
SoftwareSerial RS485Serial(CONTROLLINO_D9, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_13); // RX, TX for Controllino
SoftwareSerial MAXIGAUGESerial(CONTROLLINO_D10, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_14); // RX, TX for Controllino
//SoftwareSerial Spare2Serial(CONTROLLINO_D8, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_12); // RX, TX for Controllino
//#define MAXIGAUGESerial  Serial2 // Serial2 is hardware serial for Controllino MAXI
#define MAXIGAUGESerialIsSoftware

// Modbus addresses limits
#define NB_HOLDING_REGISTERS 70
#define MG_START_ADDR 0
#define MG_START_CMD_ADDR 24 //(MAXIGAUGE ON/OFF Command starting point)
#define MG_END_ADDR 30
#define V21_CMD_ADDR MG_END_ADDR+1 //(V21CMD)
#define V22_CMD_ADDR MG_END_ADDR+2 //(V22CMD) 
#define P22_SCROLL_ADDR MG_END_ADDR+3 //(P22CMD Command)
#define V21_STATUS_ADDR MG_END_ADDR+4 //(V21ST)
#define V22_STATUS_ADDR MG_END_ADDR+5 //(V22ST)
#define P22_STATUS_ADDR MG_END_ADDR+6 //(P22ST)
#define V23_STATUS_ADDR MG_END_ADDR+7 //(V23ST manual)
#define COMPRESSAIR_STATUS_ADDR MG_END_ADDR+8 //(COMPRESSAIRST)
#define VA1_STATUS_ADDR MG_END_ADDR+9 //(VA1ST)
#define VA2_STATUS_ADDR MG_END_ADDR+10 //(VA2ST)
#define DCU_START_ADDR VA2_STATUS_ADDR+1
#define DCU_STARTSTOP_ADDR 50 //(DCU ON/OFF Command)
#define DCU_STANDBY_ADDR 51  //(DCU STANDBY Command)
#define DCU_END_ADDR 52
#define RACK_FAN_STATUS_ADDR DCU_END_ADDR+1 //(RACK FAN ON/OFF STATUS)
#define RACK_TEMP_ADDR DCU_END_ADDR+2 //(RACK FAN TEMP)
#define RACK_FAN_CMD_ADDR 56 // (RACK FAN ON/OFF Command)
#define RACK_FAN_SPEED_ADDR 57 // (RACK FAN SPEED NORMAL/LOW Command & Status)
#define RACK_END_ADDR 58
/*#define VARIAN_START_ADDR DCU_END_ADDR+1
#define VARIAN_STARTSTOP_ADDR 60 //(VARIAN ON/OFF Command)
#define VARIAN_LOWSPEED_ADDR 61  //(VARIAN LOWSPEED Command)
#define VARIAN_END_ADDR 62
*/
/*#define XGS_START_ADDR DCU_END_ADDR+1
#define XGS_EMULT1_ADDR 58   //(XGS EMULT 1 ON/OFF Command)
#define XGS_EMULT2_ADDR 59   //(XGS EMULT 2 ON/OFF Command)
#define XGS_DEGAS_ADDR 60   //(XGS DEGAS ON/OFF Command)
#define XGS_END_ADDR 61 
*/
#define ARD_RESET_ADDR RACK_END_ADDR+1

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

void(*resetArd) (void) = 0; //declare reset function @ address 0

// I2C bit adresses
#define NORMAL_SPEED_OPEN_CMD_BIT   0   // NORMAL SPEED Open bit
#define NORMAL_SPEED_CLOSE_CMD_BIT  1   // NORMAL SPEED Close bit
#define LOW_NOISE_OPEN_CMD_BIT      2   // LOW NOISE Open bit
#define LOW_NOISE_CLOSE_CMD_BIT     3   // LOW NOISE Close bit
#define FAN_START_CMD_BIT           4   // FAN_START Open bit
#define FAN_STOP_CMD_BIT            5   // FAN_START Close bit
#define FAN_START_STATUS_BIT        6   // FAN START Status bit
#define FAN_STOP_STATUS_BIT         7   // FAN STOP Status bit
#define ARD_RESET_BIT               8   // Controllino Reset Bit

// Buffer for Serial purposes
size_t length = 64;
char *buffer = (char*)malloc(sizeof(char) * length); 

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
       Serial.println("RESET CLOSE V21");
       digitalWrite(V21_CLOSE_CMD,HIGH);  // RESET CLOSE CMD
       holdingRegisters[V21_CMD_ADDR] = 0x00; // RESET register
       V21_RESET = false;  
    }
  }
  // Reset V21_OPEN_CMD
  if (digitalRead(V21_OPEN_CMD) == HIGH && V21_RESET == true) {
    if ( millis() - V21time > reset_wait) {
       Serial.println("RESET OPEN V21 COMMAND");
       digitalWrite(V21_OPEN_CMD,LOW);   // RESET OPEN CMD
       holdingRegisters[V21_CMD_ADDR] = 0x00; // RESET register
       V21_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V21time = millis();
       V21_CHECK = true; 
    }
  }
  // Check V21 Close Status
  if (digitalRead(V21_CLOSE_STATUS) == HIGH && digitalRead(V21_OPEN_STATUS) == LOW && V21_CHECK == true) {
    if ( millis() - V21time > check_wait) {
      Serial.println("CHECK CLOSE V21 STATUS");
      digitalWrite(V21_CLOSE_CMD,LOW);  // CLOSE CMD
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
       digitalWrite(V22_CLOSE_CMD,HIGH);  // RESET CLOSE CMD
       holdingRegisters[V22_CMD_ADDR] = 0x00; // RESET register
       V22_RESET = false;  
    }
  }
  // Reset V22_OPEN_CMD
  if (digitalRead(V22_OPEN_CMD) == HIGH && V22_RESET == true) {
    if ( millis() - V22time > reset_wait) {
       digitalWrite(V22_OPEN_CMD,LOW);   // RESET OPEN CMD
       holdingRegisters[V22_CMD_ADDR] = 0x00; // RESET register
       V22_RESET = false;
       // Now we will check that Valve has effectively closed after some time
       V22time = millis();
       V22_CHECK = true; 
    }
  }
  // Check V22 Close Status
  if (digitalRead(V22_CLOSE_STATUS) == HIGH && digitalRead(V22_OPEN_STATUS) == LOW && V22_CHECK == true) {
    if ( millis() - V22time > check_wait) {
      digitalWrite(V22_CLOSE_CMD,LOW);  // CLOSE CMD
      V22time = millis();
      V22_CHECK = false;
      V22_RESET = true;
    }
  }
/*
 *  P22 Case
 */
  // Reset P22_OFF_SCROLL
  if (digitalRead(P22_OFF_SCROLL) == LOW && P22_RESET == true) {
    if ( millis() - P22time > reset_wait) {
      digitalWrite(P22_OFF_SCROLL,HIGH);    // RESET SWITCH ON SCROLL
      holdingRegisters[P22_SCROLL_ADDR] = 0x00; // RESET register
      P22_RESET = false;
    }
  }
  // Reset P22_ON_SCROLL
  if (digitalRead(P22_ON_SCROLL) == HIGH && P22_RESET == true) {
    if ( millis() - P22time > reset_wait) {
      digitalWrite(P22_ON_SCROLL,LOW);      // RESET SWITCH ON SCROLL
      holdingRegisters[P22_SCROLL_ADDR] = 0x00; // RESET register
      P22_RESET = false;  
    }
  }
  //  FAN Case
  // Reset FAN register
  if (FAN_RESET == true) {
    if ( millis() - FANtime > resetinstrument_wait) {
       holdingRegisters[RACK_FAN_CMD_ADDR] = 0x00; // RESET register
       FAN_RESET = false;  
    }
  }
 // P21 Case
  // Reset P21 ONOFF register
  if (P21_ONOFF_RESET == true) {
    if ( millis() - P21_ONOFFtime > resetinstrument_wait) {
       holdingRegisters[DCU_STARTSTOP_ADDR] = 0x00; // RESET COMMAND
       P21_ONOFF_RESET = false;  
    }
  }
  // Reset P21 StandBy register
  if (P21_STDBY_RESET == true) {
    if ( millis() - P21_STDBYtime > resetinstrument_wait) {
       holdingRegisters[DCU_STANDBY_ADDR] = 0x00; // RESET COMMAND
       P21_STDBY_RESET = false;  
    }
  }
  // Reset MG ONOFF register
  int pos = -1;
  for (int j = MG_START_CMD_ADDR; j < MG_END_ADDR; j++) {
    pos++;
    if (MG_RESET[pos] == true) {
      Serial.print("WAIT TO RESET MG REGISTER ");Serial.println(pos);
       if ( millis() - MGtime[j] > resetinstrument_wait) {
          Serial.print("RESET MG REGISTER ");Serial.println(pos);
          holdingRegisters[j] = 0x00; // RESET COMMAND
          MG_RESET[pos] = false;
       }  
    }
  }
}
void setup() {

  // Open serial communication for Com port.
  Serial.begin(9600);
  // Initialize pin header digital (A13) for RE/DE switch for RS485 communication
  pinMode(RS485_TRANSEIVER_STATUS, OUTPUT);
  digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Init Transceiver  
  RS485Serial.begin(9600);
  RS485Serial.setTimeout(1000);
  // Open serial communication for MAXIGAUGE
  // Initialize pin header digital for RE/DE switch for RS485 communication
  pinMode(MAXIGAUGE_TRANSEIVER_STATUS, OUTPUT);
  digitalWrite(MAXIGAUGE_TRANSEIVER_STATUS, LOW);  // Init Transceiver  
  MAXIGAUGESerial.begin(9600);
  MAXIGAUGESerial.setTimeout(1000);
  // Open serial communication for XGS-600 Varian Gauge
  //XGSSerial.begin(19200);
  //XGSSerial.setTimeout(1000);
  // Open serial communication for Spare1
  //Spare1Serial.begin(9600);
  //Spare1Serial.setTimeout(1000);
  // Open serial communication for Spare2
  //Spare2Serial.begin(9600);
  //Spare2Serial.setTimeout(1000);

  // Initialize I2C communications as Master
  Wire.begin();
  Wire.setTimeout(1000);
  // start the Modbus server
  StartModbusServer();
}
 
void loop() {
 Serial.println("Entering loop...");
 /***********************************************************************************************************/
  // check DHCP lease
  Ethernet.maintain();
  /***********************************************************************************************************/
  // Check Reset Status
  /***********************************************************************************************************/
  if (holdingRegisters[ARD_RESET_ADDR] == 0x01)
    resetArd();
  /***********************************************************************************************************/
  
  typedef union {
     float fvalue;
     uint16_t value[2];
  } FloatUint16;
  FloatUint16 mbfValue;
 
  /***********************************************************************************************************/
  /*  Serial MaxiGauge data / Update Modbus register values */
  /***********************************************************************************************************/
  int curaddr = MG_START_ADDR;
  for (int i = 0; i < 7; i++) {
     // Communicate with Master (send/receive data)
     SendReceiveMaster();
     String serData1,serData2 = "";
     serData1 = "PR" + String(i+1) + "\r";
     if (i==6)
      serData1 = "SEN,0,0,0,0,0,0\r";
#if defined(MAXIGAUGESerialIsSoftware)
     MAXIGAUGESerial.listen();
#endif
     digitalWrite(MAXIGAUGE_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission 
     delay(10);
     MAXIGAUGESerial.print(serData1);
     digitalWrite(MAXIGAUGE_TRANSEIVER_STATUS, LOW);  // Switch to Reception 
     delay(10);
     //Serial.println(serData1);
     memset(buffer,0,length);
     MAXIGAUGESerial.readBytes(buffer,length);
     //Serial.println(buffer);
     serData2 = buffer;
     MAXIGAUGESerial.flush(); 
     // Communicate with Master (send/receive data)
     SendReceiveMaster();
     digitalWrite(MAXIGAUGE_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission ù
     delay(10);
     MAXIGAUGESerial.print("\x05");
     digitalWrite(MAXIGAUGE_TRANSEIVER_STATUS, LOW);  // Switch to Reception 
     delay(10);
     memset(buffer,0,length);
     MAXIGAUGESerial.readBytes(buffer,length);
     //Serial.println(buffer);
     serData2 = buffer;
     MAXIGAUGESerial.flush();
     //Serial.println(serData2); 
     if ( serData2 == "" ) {// Com Status Error
        if (++mgtrials > MAX_TRIALS) {
           for (int j = MG_START_ADDR; j < MG_END_ADDR; j++)
              holdingRegisters[j] = 0x00;
           holdingRegisters[MG_END_ADDR] = 0x01;
        }
        break;
     }
     else {
      mgtrials = 0;
      holdingRegisters[MG_END_ADDR] = 0x00;
      if (i < 6) { // Sensor pressures & status
        String prval = serData2.substring(serData2.indexOf(",")+1);
        mbfValue.fvalue = prval.toFloat();
        holdingRegisters[curaddr++] = mbfValue.value[0]; 
        holdingRegisters[curaddr++] = mbfValue.value[1];
        String stval = serData2.substring(0,serData2.indexOf(","));
        holdingRegisters[curaddr++] = stval.toInt();
        curaddr++; // Leave a register space for (i==6) case
      }
      else if (i==6) { // (i==6) Sensor status
        curaddr=MG_START_ADDR - 1; // go back to initial position (- 1)
        for (int j = 0; j < 6 ; j++) {
          curaddr+=4;
          String senst = serData2.substring(2*j,2*j+1);
          holdingRegisters[curaddr] = senst.toInt();
        }
      }
     }
  }
  /***********************************************************************************************************/
  /*  Serial SCU1600 data / Update Modbus register values */
  /***********************************************************************************************************
  curaddr = SCU_START_ADDR;
  for (int i = 0; i < 5; i++) {
     String serData3,serData4 = "";
     switch (i) {
       case 0:
        serData3 = "\x02\x30\x30\x31\x3F\x44\x03\xB4";
         break;
       case 1:
         serData3 = "\x02\x30\x30\x31\x3F\x64\x03\x94";
         break;
       case 2:
         serData3 = "\x02\x30\x30\x31\x3F\x65\x03\x95";
         break;
       case 3:
         serData3 = "\x02\x30\x30\x31\x3F\x63\x03\x93";
         break;
       case 4:
         serData3 = "\x02\x30\x30\x31\x3F\x4D\x03\xBD";
         break;
     }
     SCU1600Serial.listen();
     SCU1600Serial.print(serData3);
     Serial.println(serData3); 
     serData4 = SCU1600Serial.readString();
     Serial.println(serData4); 
     if (serData4 == "") {     // Com Status Error
      for (int j = SCU_START_ADDR; j < SCU_END_ADDR; j++)
        holdingRegisters[j] = 0x00;
      holdingRegisters[SCU_END_ADDR] = 0x01;
     } 
     else {
      holdingRegisters[SCU_END_ADDR] = 0x00;
      uint16_t result=0,result1=0;
      if (i==0) {              // ReadMeas
        serData4 = serData4.substring(21,25); 
        sscanf(serData4.c_str(),"%x",&result);
      }
      else if (i==1 || i==2) { // ReadSetPoint + ReadMotorTemp
        serData4 = serData4.substring(7,11); 
        sscanf(serData4.c_str(),"%x",&result);
      }
      else if (i==3) {        // ReadCounters
        serData4 = serData4.substring(27,35); 
        sscanf(serData4.c_str(),"%x",&result);
      }
      else if (i==4) {        // ReadModFonct
        serData4 = serData4.substring(7,11); 
        sscanf(serData4.c_str(),"%2x%2x",&result,&result1);
      }
      if ( i < 4 ) {
       holdingRegisters[curaddr++] = result;
      }
      else {
       holdingRegisters[curaddr++] = result;
       holdingRegisters[curaddr++] = result1;
      }
     }
    // Communicate with Master (send/receive data)
  SendReceiveMaster();
  }
  /***********************************************************************************************************/
  /*  Serial VARIAN data / Update Modbus register values */
  /***********************************************************************************************************
  curaddr = VARIAN_START_ADDR;
  for (int i = 0; i < 8; i++) {
  // Communicate with Master (send/receive data)
    SendReceiveMaster();
     String serData5,serData6 = "";
     switch (i) {
       case 0:
        serData5 = "\x02\x80\x32\x30\x33\x30\x03\x38\x32"; // Rotation freq
         break;
       case 1:
        serData5 = "\x02\x80\x31\x32\x30\x30\x03\x38\x30"; // Rotation final freq
         break;
       case 2:
        serData5 = "\x02\x80\x33\x30\x32\x30\x03\x38\x32"; // Pump life in hours
         break;
       case 3:
        serData5 = "\x02\x80\x32\x30\x35\x30\x03\x38\x34"; // Pump status
         break;
       case 4:
        serData5 = "\x02\x80\x32\x30\x36\x30\x03\x38\x37"; // Error Code
         break;
       case 5:
        serData5 = "\x02\x80\x32\x30\x34\x30\x03\x38\x35"; // Temperature
         break;
       case 6:
        serData5 = "\x02\x80\x30\x30\x30\x30\x03\x38\x33"; // ON/OFF STATUS
         break;
       case 7:
        serData5 = "\x02\x80\x30\x30\x31\x30\x03\x38\x32"; // LOW SPEED STATUS
         break;
     }
     delay(100);
     VARIANSerial.listen();
     VARIANSerial.print(serData5);
     Serial.println(serData5);
     delay(100);
     serData6 = VARIANSerial.readString();
     Serial.println(serData6); 
     if (serData6 == "") {// Com Status Error
     for (int j = VARIAN_START_ADDR; j < VARIAN_END_ADDR; j++)
        holdingRegisters[j] = 0xFF;
      holdingRegisters[VARIAN_END_ADDR] = 0x01;
     }
     else {
      holdingRegisters[VARIAN_END_ADDR] = 0x00;
      uint16_t result=0;
      if ( i != 6 && i != 7)
        serData6 = serData6.substring(6,12);
      else
        serData6 = serData6.substring(6,7);
      sscanf(serData6.c_str(),"%d",&result);
      holdingRegisters[curaddr++] = result;
     }
  }
  /***********************************************************************************************************/
  /*  Serial DCU1601 data / Update Modbus register values */
  /**********************************************************************************************************/
  curaddr = DCU_START_ADDR;
  for (int i = 0 ; i < 9 ; i++) {
    // Communicate with Master (send/receive data)
    SendReceiveMaster();
    String serData5,serData6 = "";
     switch (i) {
       case 0:
         serData5 = "0010030902=?107\r";// Act Rot Speed
         break;
       case 1:
         serData5 = "0010030802=?106\r";// Set Rot Speed
         break;
       case 2:
         serData5 = "0010031102=?100\r";// Operating hours
         break;
       case 3:
         serData5 = "0010031602=?105\r";// Power in watt
         break;
       case 4:
         serData5 = "0010030402=?102\r";// Electronics temperature status
         break;
       case 5:
         serData5 = "0010030502=?103\r";// Turbo pump temperature status
         break;
       case 6:
         serData5 = "0010001002=?096\r";// Turbo ON/OFF
         break;
       case 7:
         serData5 = "0010000202=?097\r";// Standby ON/OFF
         break;
       case 8:
         serData5 = "0010030302=?101\r";// Error code
         break;
     }
    RS485Serial.listen();
    digitalWrite(RS485_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission 
    delay(10);
    //Serial.println(serData5);  
    RS485Serial.print(serData5);
    digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Switch to Reception 
    delay(10);
    memset(buffer,0,length);
    RS485Serial.readBytes(buffer,length);
    serData6 = buffer;
    RS485Serial.flush();
    //Serial.println(serData6);
    if (serData6 == "" || serData6.length() != 20) {// Com Status Error
      if (++p21trials > MAX_TRIALS) {
         for (int j = DCU_START_ADDR; j < DCU_END_ADDR; j++)
            holdingRegisters[j] = 0x00;
         holdingRegisters[DCU_END_ADDR] = 0x01;
      }
      break;
    }
    else {
       int csum = 0; // Calculate checksum
       for (int k = 0 ; k < serData6.length() - 4; k++)
         csum+= serData6[k];
       int csumd = serData6.substring(serData6.length() - 4, serData6.length() - 1).toInt();
       if ( (csum%256) != csumd) { // Data KO
          if (++p21trials > MAX_TRIALS) {
            for (int j = DCU_START_ADDR; j < DCU_END_ADDR; j++)
               holdingRegisters[j] = 0x00;
             holdingRegisters[DCU_END_ADDR] = 0x01;
            }
          break;
       }
       else { // Data OK
        p21trials = 0;
        holdingRegisters[DCU_END_ADDR] = 0x00;
        // check Channel read
        if ( serData6.substring(5, 8) == serData5.substring(5, 8)) { // Channel OK
          if ( i < 4 ) { // short value
             uint16_t result=0;
             sscanf(serData6.substring(10,16).c_str(),"%d",&result);
             holdingRegisters[curaddr++] = result;
          }
          else if (i == 4 || i == 5 || i == 6 || i == 7) { // Boolean value
            if ( serData6.substring(10,16) == "111111")
              holdingRegisters[curaddr++] = 0x01;
            else if ( serData6.substring(10,16) == "000000")
              holdingRegisters[curaddr++] = 0x00;
            
          }
          else if (i == 8) { // String value
            if ( (serData6.substring(10,16) == "no Err") || (serData6.substring(10,13) == "Wrn") || (serData6.substring(10,16) == "000000")) {
              holdingRegisters[curaddr++] = 0x00;
            }
            else if ( serData6.substring(10,13) == "Err") {
              uint16_t result=0;
              //Serial.println(serData6[i].substring(13,16));
              sscanf(serData6.substring(13,16).c_str(),"%d",&result);
              holdingRegisters[curaddr++] = result;
            }
          }
        }
      }
  }
 }
 /***********************************************************************************************************/
 /*  Serial XGS-600 data / Update Modbus register values */
 /**********************************************************************************************************
  curaddr = XGS_START_ADDR;
  for (int i = 0 ; i < 4 ; i++) {
    // Communicate with Master (send/receive data)
    SendReceiveMaster();
    String serData7,serData8 = "";
    switch (i) {
       case 0:
         serData7 = "#0002UHFIG1\r"; // Gauge pressure value
         break;
       case 1:
         serData7 = "#0032UHFIG1\r"; // Gauge Emission status (ON/OFF)
         break;
       case 2:
         serData7 = "#0042UHFIG1\r"; // Gauge Degas status (ON/OFF)
         break;
       case 3:
         serData7 = "#0034UHFIG1\r"; // Gauge Filament status (1/2)
         break;
    }
    XGSSerial.listen();
    Serial.println(serData7);    
    XGSSerial.print(serData7);
    memset(buffer,0,length);
    XGSSerial.readBytes(buffer,length);
    serData8 = buffer;
    XGSSerial.flush();
    Serial.println(serData8.substring(1));
    if (serData8 == "") {// Com Status Error
      for (int j = XGS_START_ADDR; j < XGS_END_ADDR; j++)
         holdingRegisters[j] = 0x00;
      holdingRegisters[XGS_END_ADDR] = 0x01;
      break;
    }
    else { // Data OK
      holdingRegisters[XGS_END_ADDR] = 0x00;
      uint16_t result=0;
      if ( i == 0 ) { // Pressure float
        String prval = serData8.substring(1);
        mbfValue.fvalue = prval.toFloat();
        holdingRegisters[curaddr++] = mbfValue.value[0]; 
        holdingRegisters[curaddr++] = mbfValue.value[1];
      }
      else { // Gauge Emission & Degas & Filament status
        String prval = serData8.substring(1);
        uint16_t result=0;
        sscanf(serData8.substring(1).c_str(),"%d",&result); // 00 -> OFF; 01 -> ON // 01 -> Fil1; 02 -> Fil2
        holdingRegisters[curaddr++] = result;
      }
    }
  }
  */
  
  
}

void I2CupdateData() 
{
  /***********************************************************************************************************/
  /*  I2C update data / Update Modbus register values */
  /***********************************************************************************************************/

   typedef union {
     float fvalue;
     uint16_t value[2];
  } FloatUint16;
  FloatUint16 mbfValue;
  
   typedef union {
    float fvalue;
    uint32_t i32value;
  } FloatUint32;
  FloatUint32 temp;
  int pos = 0;
 
 
  //Serial.println("Request event from slave");
  int numbyte = Wire.requestFrom(SLAVE_ADDR, 12); // Expect 12 byte from slave
  //Serial.print("Read numbyte=");
  //Serial.println(numbyte);
  uint32_t crc;
  byte data_array[8];
  byte crc_array[4];
  if (numbyte == 12) { // Expect 8 bytes of data
    
     // first 4 bytes is temperature
     data_array[0] = Wire.read();
     data_array[1] = Wire.read();
     data_array[2] = Wire.read();
     data_array[3] = Wire.read();
   
     // next 4 bytes of data corresponds to i2c_buffer
     data_array[4] = Wire.read();
     data_array[5] = Wire.read();
     data_array[6] = Wire.read();
     data_array[7] = Wire.read();

     // last 4 bytes of data corresponds to CRC32
    
     crc_array[0] = Wire.read();
     crc_array[1] = Wire.read();
     crc_array[2] = Wire.read();
     crc_array[3] = Wire.read();

    /*Serial.print("I2C_BUFFER=");
     for (int i = 0 ; i < 8; i++)
     Serial.println(data_array[i]);
     */
     crc = crc_array[0];
     crc = (crc << 8) | crc_array[1];
     crc = (crc << 8) | crc_array[2];
     crc = (crc << 8) | crc_array[3];

     // calculate CRC32
     uint32_t crcdata = CRC32::calculate(data_array, 8);

     if ( crc == crcdata ) {
        //Serial.print("Checksum good:");Serial.println(crc);
        temp.i32value = data_array[0];
        temp.i32value = (temp.i32value << 8) | data_array[1];
        temp.i32value = (temp.i32value << 8) | data_array[2];
        temp.i32value = (temp.i32value << 8) | data_array[3];    
        //Serial.print("Temperature = "); Serial.println(temp.fvalue);
        i2c_buffer = data_array[4];
        i2c_buffer = (i2c_buffer << 8) | data_array[5];
        i2c_buffer = (i2c_buffer << 8) | data_array[6];
        i2c_buffer = (i2c_buffer << 8) | data_array[7];
        // Finally update Modbus registers
        // Temperature
        mbfValue.fvalue = temp.fvalue; // convert float from 32bit to 16bit
        //Serial.print("TEMPERATURE 16 bit = ");
        //Serial.println(mbfValue.fvalue);
        holdingRegisters[RACK_TEMP_ADDR] = mbfValue.value[0];
        holdingRegisters[RACK_TEMP_ADDR+1] = mbfValue.value[1];
        // I2C_buffer
        if (bitRead(i2c_buffer,FAN_START_STATUS_BIT) == 0x00 && bitRead(i2c_buffer,FAN_STOP_STATUS_BIT) == 0x01)
          holdingRegisters[RACK_FAN_STATUS_ADDR] = 0x02;   // FAN OFF
        else if (bitRead(i2c_buffer,FAN_STOP_STATUS_BIT) == 0x00 && bitRead(i2c_buffer,FAN_START_STATUS_BIT) == 0x01) 
          holdingRegisters[RACK_FAN_STATUS_ADDR] = 0x01;    // FAN ON
        else
          holdingRegisters[RACK_FAN_STATUS_ADDR] = 0x00;   // FAN STATUS ERROR
        /*Serial.print("FAN_START_STATUS_BIT=");
        Serial.println(bitRead(i2c_buffer,FAN_START_STATUS_BIT));
        Serial.print("FAN_STOP_STATUS_BIT=");
        Serial.println(bitRead(i2c_buffer,FAN_STOP_STATUS_BIT));*/
        if (bitRead(i2c_buffer,NORMAL_SPEED_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,NORMAL_SPEED_CLOSE_CMD_BIT) == 0x01 &&
            bitRead(i2c_buffer,LOW_NOISE_OPEN_CMD_BIT) == 0x00 && bitRead(i2c_buffer,LOW_NOISE_CLOSE_CMD_BIT) == 0x00)
           holdingRegisters[RACK_FAN_SPEED_ADDR] = 0x01; // FAN NORMAL SPEED
        else if (bitRead(i2c_buffer,LOW_NOISE_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,LOW_NOISE_CLOSE_CMD_BIT) == 0x01 &&
                 bitRead(i2c_buffer,NORMAL_SPEED_OPEN_CMD_BIT) == 0x00 && bitRead(i2c_buffer,NORMAL_SPEED_CLOSE_CMD_BIT) == 0x00) 
           holdingRegisters[RACK_FAN_SPEED_ADDR] = 0x02; // FAN LOW NOISE
        else
           holdingRegisters[RACK_FAN_SPEED_ADDR] = 0x00; // FAN SPEED in progress..
         holdingRegisters[RACK_END_ADDR] = 0x00; // COM OK
     }
     else {
        Serial.print(" Checksum mismatch crc=");Serial.print(crc);Serial.print(" != crcdata=");Serial.println(crcdata);
        holdingRegisters[RACK_END_ADDR] = 0x01; // ERROR COM
        holdingRegisters[RACK_TEMP_ADDR] = 0;
        holdingRegisters[RACK_TEMP_ADDR+1] = 0;
        holdingRegisters[RACK_FAN_STATUS_ADDR] = 0x00;
        holdingRegisters[RACK_FAN_SPEED_ADDR] = 0x00;
     }
   }
   else {
        Serial.print("numbyte wrong value:");Serial.println(numbyte);
        holdingRegisters[RACK_END_ADDR] = 0x01; // ERROR COM
        holdingRegisters[RACK_TEMP_ADDR] = 0;
        holdingRegisters[RACK_TEMP_ADDR+1] = 0;
        holdingRegisters[RACK_FAN_STATUS_ADDR] = 0x00;
        holdingRegisters[RACK_FAN_SPEED_ADDR] = 0x00;
     }
}

void StartModbusServer()
{    
  Ethernet.begin(mac);
  // Init ModbusTCPSlave object
  modbus.begin();

  // Configure registers
  modbus.setHoldingRegisters(holdingRegisters, NB_HOLDING_REGISTERS);
     
  // Assign Modbus reserved register addresses
  // LSB & MSB registers for float pressure values from MaxiGauge
   for (int i = MG_START_ADDR; i < MG_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
   holdingRegisters[MG_END_ADDR] = 0x01;
  // Digital RELAY initialization and assignation
  digitalWrite(V21_OPEN_CMD,LOW);                           // Set V21_OPEN_CMD LOW
  digitalWrite(V21_CLOSE_CMD,HIGH);                         // Set V21_CLOSE_CMD HIGH
  pinMode(V21_OPEN_CMD, OUTPUT);                            // sets the digital pin as output for Open Valve
  pinMode(V21_CLOSE_CMD, OUTPUT);                           // sets the digital pin as output for Close Valve
  holdingRegisters[V21_CMD_ADDR] = 0x00;    // RESET
  digitalWrite(V22_OPEN_CMD,LOW);                           // Set V22_CMD OPEN LOW
  digitalWrite(V22_CLOSE_CMD,HIGH);                          // Set V22_CMD CLOSE HIGH
  pinMode(V22_OPEN_CMD, OUTPUT);                            // sets the digital pin as output for Valve (RELAY)
  pinMode(V22_CLOSE_CMD, OUTPUT);                           // sets the digital pin as output for Valve (RELAY)
  holdingRegisters[V22_CMD_ADDR] = 0x00;    // RESET
  digitalWrite(P22_ON_SCROLL,LOW);                            // Set P22_ON_SCROLL LOW
  digitalWrite(P22_OFF_SCROLL,HIGH);                           // Set P22_OFF_SCROLL HIGH
  pinMode(P22_ON_SCROLL, OUTPUT);                             // sets the digital pin as output for Switch ON Scroll
  pinMode(P22_OFF_SCROLL, OUTPUT);                            // sets the digital pin as output for Switch OFF Scroll
  holdingRegisters[P22_SCROLL_ADDR] = 0x00;   // RESET
  // Digital CMD INPUTS initialization and assignation
  pinMode(V21_OPEN_STATUS, INPUT);                            // sets the digital pin as output for Valve V21 OPEN STATUS
  pinMode(V21_CLOSE_STATUS, INPUT);                           // sets the digital pin as output for Valve V21 CLOSE STATUS
  holdingRegisters[V21_STATUS_ADDR] = 0x00;   // RESET
  pinMode(V22_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V22 OPEN STATUS
  pinMode(V22_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V22 CLOSE STATUS
  holdingRegisters[V22_STATUS_ADDR] = 0x00;   // RESET
  pinMode(P22_STATUS, INPUT);                              // sets the digital pin as input for Scroll ON/OFF STATUS
  holdingRegisters[P22_STATUS_ADDR] = 0x00;   // RESET
  pinMode(V23_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve OPEN STATUS
  pinMode(V23_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve CLOSE STATUS
  holdingRegisters[V23_STATUS_ADDR] = 0x00;   // RESET
  pinMode(COMPRESS_AIR_STATUS, INPUT);                        // sets the digital pin as input for Compress Air STATUS
  holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x00;  // RESET
  pinMode(VA1_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve OPEN STATUS
  pinMode(VA1_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve CLOSE STATUS
  holdingRegisters[VA1_STATUS_ADDR] = 0x00;   // RESET
  pinMode(VA2_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve OPEN STATUS
  pinMode(VA2_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve CLOSE STATUS
  holdingRegisters[VA2_STATUS_ADDR] = 0x00;   // RESET
  
  // registers for DCU turbo
  for (int i = DCU_START_ADDR; i < DCU_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
  holdingRegisters[DCU_END_ADDR] = 0x01;

  /* registers for XGS-600 Varian Gauge
  for (int i = XGS_START_ADDR; i < XGS_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
  holdingRegisters[XGS_END_ADDR] = 0x01;
  */
  
  // register for Arduino reset
  holdingRegisters[ARD_RESET_ADDR] = 0x00;     // Arduino Global Reset Status

}
void SendReceiveMaster()
{    

  // I2C Update data
  I2CupdateData();
  // Reset & Check
  ResetAndCheck();
  
  // Process modbus requests
  modbus.update();
  
  /***********************************************************************************************************/
  /* Update Valve V21 position (Open/Close) */
  /***********************************************************************************************************/
  if (holdingRegisters[V21_CMD_ADDR] == 0x02 && V21_RESET == false) {
     digitalWrite(V21_CLOSE_CMD,LOW); // CLOSE CMD
     V21time = millis();
     V21_RESET = true;
  }        
  else if (holdingRegisters[V21_CMD_ADDR] == 0x01 && V21_RESET == false) {
     digitalWrite(V21_OPEN_CMD,HIGH); // OPEN CMD
     V21time = millis();
     V21_RESET = true;
  }
  
  /***********************************************************************************************************/
  /***********************************************************************************************************/
  /* Update Valve V22 position (Open/Close) */
  /***********************************************************************************************************/
  if (holdingRegisters[V22_CMD_ADDR] == 0x02 && V22_RESET == false) {
     digitalWrite(V22_CLOSE_CMD,LOW); // CLOSE CMD
     V22time = millis();
     V22_RESET = true;
  }
  else if (holdingRegisters[V22_CMD_ADDR] == 0x01 && V22_RESET == false) {
     digitalWrite(V22_OPEN_CMD,HIGH); // OPEN CMD
     V22time = millis();
     V22_RESET = true;
  }
 
  /***********************************************************************************************************/
  /***********************************************************************************************************/
  /* Update Scroll P22 position (Open/Close) */
  /***********************************************************************************************************/
  if (holdingRegisters[P22_SCROLL_ADDR] == 0x02 && P22_RESET == false) {
     digitalWrite(P22_OFF_SCROLL,LOW); // SWITCH OFF SCROLL
     P22time = millis();
     P22_RESET = true; 
  }
  else if (holdingRegisters[P22_SCROLL_ADDR] == 0x01 && P22_RESET == false) { 
     digitalWrite(P22_ON_SCROLL,HIGH); // SWITCH ON SCROLL
     P22time = millis();
     P22_RESET = true; 
  }
 
  /***********************************************************************************************************/
  /* Update Valve position STATUS register (Open/Close) */
  /***********************************************************************************************************/
 /* Serial.print("PIN4_CMD=");Serial.print(digitalRead(PIN4_CMD));Serial.print(" HIGH=");
  Serial.print(HIGH);Serial.print(" LOW=");Serial.println(LOW);*/
  if (digitalRead(V21_OPEN_STATUS) == HIGH && digitalRead(V21_CLOSE_STATUS) == LOW)
     holdingRegisters[V21_STATUS_ADDR] = 0x01; // OPENED CMD
  else if (digitalRead(V21_CLOSE_STATUS) == HIGH && digitalRead(V21_OPEN_STATUS) == LOW) {
     if ((holdingRegisters[V21_STATUS_ADDR] == 0x01 || holdingRegisters[V21_STATUS_ADDR] == 0x00) && V21_RESET == false) {
        // reset close command
        digitalWrite(V21_CLOSE_CMD,LOW); // CLOSE CMD
        V21_RESET = true;
        V21time = millis();
     }
     holdingRegisters[V21_STATUS_ADDR] = 0x02; // CLOSED CMD
  }
  else {
     holdingRegisters[V21_STATUS_ADDR] = 0x00; // MOVING CMD
     //Serial.print("V21_OPEN_STATUS=");Serial.println(digitalRead(V21_OPEN_STATUS));
     //Serial.print("V21_CLOSE_STATUS=");Serial.println(digitalRead(V21_CLOSE_STATUS));
  }
  
  if (digitalRead(V22_OPEN_STATUS) == HIGH && digitalRead(V22_CLOSE_STATUS) == LOW)
     holdingRegisters[V22_STATUS_ADDR] = 0x01; // OPENED CMD
  else if (digitalRead(V22_CLOSE_STATUS) == HIGH && digitalRead(V22_OPEN_STATUS) == LOW) {
     if ((holdingRegisters[V22_STATUS_ADDR] == 0x01 || holdingRegisters[V22_STATUS_ADDR] == 0x00) && V22_RESET == false) {
        // reset close command
        digitalWrite(V22_CLOSE_CMD,LOW); // CLOSE CMD
        V22_RESET = true;
        V22time = millis();
     }
     holdingRegisters[V22_STATUS_ADDR] = 0x02; // CLOSED CMD
  }
  else
     holdingRegisters[V22_STATUS_ADDR] = 0x00; // MOVING CMD
     
  if (digitalRead(P22_STATUS) == HIGH)
     holdingRegisters[P22_STATUS_ADDR] = 0x01; // SCROLL ON
  else if (digitalRead(P22_STATUS) == LOW)
     holdingRegisters[P22_STATUS_ADDR] = 0x00; // SCROLL OFF
  else
     holdingRegisters[P22_STATUS_ADDR] = 0x06; // ERROR SCROLL
     
  if (digitalRead(V23_OPEN_STATUS) == HIGH && digitalRead(V23_CLOSE_STATUS) == LOW)
     holdingRegisters[V23_STATUS_ADDR] = 0x01; // OPENED CMD
  else if (digitalRead(V23_CLOSE_STATUS) == HIGH && digitalRead(V23_OPEN_STATUS) == LOW)
     holdingRegisters[V23_STATUS_ADDR] = 0x02; // CLOSED CMD 
  else
     holdingRegisters[V23_STATUS_ADDR] = 0x00; // MOVING CMD

  if (digitalRead(COMPRESS_AIR_STATUS) == HIGH)
     holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x01; // COMPRESS AIR OFF
  else if (digitalRead(COMPRESS_AIR_STATUS) == LOW)
     holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x00; // COMPRESS AIR ON 
  else
     holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x03; // ERROR COMPRESS AIR

  if (digitalRead(VA1_OPEN_STATUS) == HIGH && digitalRead(VA1_CLOSE_STATUS) == LOW)
     holdingRegisters[VA1_STATUS_ADDR] = 0x01; // OPENED CMD
  else if (digitalRead(VA1_CLOSE_STATUS) == HIGH && digitalRead(VA1_OPEN_STATUS) == LOW)
     holdingRegisters[VA1_STATUS_ADDR] = 0x02; // CLOSED CMD 
  else
     holdingRegisters[VA1_STATUS_ADDR] = 0x00; // MOVING CMD

  if (digitalRead(VA2_OPEN_STATUS) == HIGH && digitalRead(VA2_CLOSE_STATUS) == LOW)
     holdingRegisters[VA2_STATUS_ADDR] = 0x01; // OPENED CMD
  else if (digitalRead(VA2_CLOSE_STATUS) == HIGH && digitalRead(VA2_OPEN_STATUS) == LOW)
     holdingRegisters[VA2_STATUS_ADDR] = 0x02; // CLOSED CMD 
  else
     holdingRegisters[VA2_STATUS_ADDR] = 0x00; // MOVING CMD
     
  /* Update MAXIGAUGE Sensor ON/OFF Status */
  /***********************************************************************************************************/
  int pos = -1;
  for (int j = MG_START_CMD_ADDR; j < MG_END_ADDR; j++) {
    pos++;
    if ( holdingRegisters[MG_END_ADDR] == 0x00 && holdingRegisters[j] != 0x00 && MG_RESET[pos] == false) {
      /*Serial.print(" channel:");
      Serial.print(j);
      Serial.print(" set new register:");
      Serial.println(holdingRegisters[j]);
      */
      //delay(100);
      String CMD;
      int signal = 0;
      if (holdingRegisters[j] == 0x01)
        signal = 2;
      if (holdingRegisters[j] == 0x02)
        signal = 1;
      switch(j-MG_START_CMD_ADDR) {
        case 0:
          CMD = "SEN,"+String(signal)+",0,0,0,0,0\r";
          break;
        case 1:
          CMD = "SEN,0,"+String(signal)+",0,0,0,0\r";
          break;
        case 2:
          CMD = "SEN,0,0,"+String(signal)+",0,0,0\r";
          break;
        case 3:
          CMD = "SEN,0,0,0,"+String(signal)+",0,0\r";
          break;
        case 4:
          CMD = "SEN,0,0,0,0,"+String(signal)+",0\r";
          break;
        case 5:
          CMD = "SEN,0,0,0,0,0,"+String(signal)+"\r";
          break;
      }
#if defined(MAXIGAUGESerialIsSoftware)
     MAXIGAUGESerial.listen();
#endif
      digitalWrite(MAXIGAUGE_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission 
      delay(10);
      MAXIGAUGESerial.print(CMD);
      digitalWrite(MAXIGAUGE_TRANSEIVER_STATUS, LOW);  // Switch to Reception
      delay(10);
      Serial.println(CMD);
      MAXIGAUGESerial.readString();
      digitalWrite(MAXIGAUGE_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission 
      delay(10);
      MAXIGAUGESerial.print("\x05");
      digitalWrite(MAXIGAUGE_TRANSEIVER_STATUS, LOW);  // Switch to Reception
      delay(10);
      MAXIGAUGESerial.readString();
      Serial.println("FINISH SENDING CMD MAXIGAUGE");
      MGtime[j] = millis();
      MG_RESET[pos] = true;
    }
    
  } 
  /***********************************************************************************************************/
  /* Update SCU1600 Turbo START/STOP Status */
  /***********************************************************************************************************
  if ( holdingRegisters[SCU_STARTSTOP_ADDR] != 0x00 ) {
      String CMD="";;
      if ( holdingRegisters[SCU_STARTSTOP_ADDR] == 0x02 )
        CMD = "\x02\x30\x30\x31\x20\x45\x02\x03\xA8";   // STOP COMMAND
      else if ( holdingRegisters[SCU_STARTSTOP_ADDR] == 0x01 )
        CMD = "\x02\x30\x30\x31\x20\x45\x01\x03\xA7";  //START COMMAND
      SCU1600Serial.listen();
      SCU1600Serial.print(CMD);
      Serial.println(CMD);
      holdingRegisters[SCU_STARTSTOP_ADDR] = 0x00;  // RESET register
  }
  /***********************************************************************************************************/
  /* Update VARIAN Turbo START/STOP LOWSPEED ON/OFF COMMAND */
  /***********************************************************************************************************
  if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] != 0x00 ) {
      String CMD="";;
      if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] == 0x02 )
        CMD = "\x02\x80\x30\x30\x30\x31\x30\x03\x42\x32";   // STOP COMMAND
      else if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] == 0x01 )
        CMD = "\x02\x80\x30\x30\x30\x31\x31\x03\x42\x33";  //START COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(CMD);
      Serial.println(CMD);
      holdingRegisters[VARIAN_STARTSTOP_ADDR] = 0x00;  // RESET register
  }
  if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] != 0x00 ) {
      String CMD="";;
      if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] == 0x02 )
        CMD =  "\x02\x80\x30\x30\x31\x31\x30\x03\x42\x33";   // LOWSPEED OFF COMMAND
      else if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] == 0x01 )
        CMD =  "\x02\x80\x30\x30\x31\x31\x31\x03\x42\x34";  // LOWSPEED ON COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(CMD);
      Serial.println(CMD);
      holdingRegisters[VARIAN_LOWSPEED_ADDR] = 0x00;  // RESET register
  }
  /***********************************************************************************************************/
  /* Update DCU (PFEIFFER) Turbo START/STOP STANDBY,ON/OFF COMMAND */
  /***********************************************************************************************************/
  if ( holdingRegisters[DCU_END_ADDR] == 0x00 && holdingRegisters[DCU_STARTSTOP_ADDR] != 0x00 && P21_ONOFF_RESET == false) {
      String CMD="";;
      if ( holdingRegisters[DCU_STARTSTOP_ADDR] == 0x02 )
        CMD = "0011001006000000009\r";  // STOP PUMPING COMMAND
      else if ( holdingRegisters[DCU_STARTSTOP_ADDR] == 0x01 )
        CMD = "0011001006111111015\r";  //START PUMPING COMMAND
      Serial.println(CMD);
      RS485Serial.listen();
      digitalWrite(RS485_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission
      delay(10);
      RS485Serial.print(CMD);
      digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Switch to Reception
      delay(10);
      P21_ONOFFtime = millis();
      P21_ONOFF_RESET = true;
  }
  if ( holdingRegisters[DCU_END_ADDR] == 0x00 && holdingRegisters[DCU_STANDBY_ADDR] != 0x00 && P21_STDBY_RESET == false) {
      String CMD="";;
      if ( holdingRegisters[DCU_STANDBY_ADDR] == 0x02 )
        CMD = "0011000206000000010\r";   // STANDBY OFF COMMAND
      else if ( holdingRegisters[DCU_STANDBY_ADDR] == 0x01 )
        CMD = "0011000206111111016\r";  // STANDBY ON COMMAND
      RS485Serial.listen();
      digitalWrite(RS485_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission
      delay(10);
      RS485Serial.print(CMD);
      Serial.println(CMD);
      digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Switch to Reception
      delay(10);
      P21_STDBYtime = millis();
      P21_STDBY_RESET = true;
  }
   /***********************************************************************************************************/
  /* Update VARIAN Turbo START/STOP LOWSPEED ON/OFF COMMAND */
  /***********************************************************************************************************
  if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] != 0x00 ) {
      String CMD="";;
      if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] == 0x02 )
        CMD = "\x02\x80\x30\x30\x30\x31\x30\x03\x42\x32";   // STOP COMMAND
      else if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] == 0x01 )
        CMD = "\x02\x80\x30\x30\x30\x31\x31\x03\x42\x33";  //START COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(CMD);
      Serial.println(CMD);
      holdingRegisters[VARIAN_STARTSTOP_ADDR] = 0x00;  // RESET register
  }
  if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] != 0x00 ) {
      String CMD="";;
      if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] == 0x02 )
        CMD =  "\x02\x80\x30\x30\x31\x31\x30\x03\x42\x33";   // LOWSPEED OFF COMMAND
      else if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] == 0x01 )
        CMD =  "\x02\x80\x30\x30\x31\x31\x31\x03\x42\x34";  // LOWSPEED ON COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(CMD);
      Serial.println(CMD);
      holdingRegisters[VARIAN_LOWSPEED_ADDR] = 0x00;  // RESET register
  }
  */
  /***********************************************************************************************************/
  /* Update XGS (Varian Gauge) ON/OFF COMMAND */
  /***********************************************************************************************************
  if ( holdingRegisters[XGS_END_ADDR] == 0x00 && holdingRegisters[XGS_EMULT1_ADDR] != 0x00 ) {
      String CMD="";;
      if ( holdingRegisters[XGS_EMULT1_ADDR] == 0x02 )
        CMD = "#0030UHFIG1\r";   // EMULT 1 OFF COMMAND
      else if ( holdingRegisters[XGS_EMULT1_ADDR] == 0x01 )
        CMD = "#0031UHFIG1\r";   // EMULT 1 ON COMMAND
      Serial.println(CMD);
      XGSSerial.listen();
      XGSSerial.print(CMD);
      holdingRegisters[XGS_EMULT1_ADDR] = 0x00; // RESET COMMAND
  }
  if ( holdingRegisters[XGS_END_ADDR] == 0x00 && holdingRegisters[XGS_EMULT2_ADDR] != 0x00 ) {
      String CMD="";;
      if ( holdingRegisters[XGS_EMULT2_ADDR] == 0x02 )
        CMD = "#0030UHFIG1\r";   // EMULT 2 OFF COMMAND
      else if ( holdingRegisters[XGS_EMULT2_ADDR] == 0x01 )
        CMD = "#0033UHFIG1\r";   // EMULT 2 ON COMMAND
      Serial.println(CMD);
      XGSSerial.listen();
      XGSSerial.print(CMD);
      holdingRegisters[XGS_EMULT2_ADDR] = 0x00; // RESET COMMAND
  }
  if ( holdingRegisters[XGS_END_ADDR] == 0x00 && holdingRegisters[XGS_DEGAS_ADDR] != 0x00 ) {
      String CMD="";;
      if ( holdingRegisters[XGS_DEGAS_ADDR] == 0x02 )
        CMD = "#0040UHFIG1\r";   // DEGAS OFF COMMAND
      else if ( holdingRegisters[XGS_DEGAS_ADDR] == 0x01 )
        CMD = "#0041UHFIG1\r";   // DEGAS ON COMMAND
      Serial.println(CMD);
      XGSSerial.listen();
      XGSSerial.print(CMD);
      holdingRegisters[XGS_DEGAS_ADDR] = 0x00; // RESET COMMAND
  }
  */
  /***********************************************************************************************************/
  /* Update I2C buffer Command*/
  /***********************************************************************************************************/
  boolean SEND_REQUEST = false;
 
 /* if ( millis() - global > 20000 && delstartfan == false) {
    Serial.println("SWITCH ON FAN");
    bitSet(i2c_buffer,FAN_START_CMD_BIT);
     FAN_RESET = true;
     SEND_REQUEST = true;
     delstartfan = true;
  }
  if ( millis() - global >  40000 && delstopfan == false) {
    Serial.println("SWITCH OFF FAN");
    bitClear(i2c_buffer,FAN_STOP_CMD_BIT);
     FAN_RESET = true;
     SEND_REQUEST = true;
     delstopfan = true;
  }
  
  if ( millis() - global > 20000 && delstartfan == false) {
    Serial.println("SWITCH SPEED LOW  FAN");
    bitClear(i2c_buffer,NORMAL_SPEED_CLOSE_CMD_BIT);
     SEND_REQUEST = true;
     delstartfan = true;
  }
  if ( millis() - global >  40000 && delstopfan == false) {
    Serial.println("SWITCH SPEED NORMAL  FAN");
    bitClear(i2c_buffer,LOW_NOISE_OPEN_CMD_BIT); // Set Normal SPEED
     SEND_REQUEST = true;
     delstopfan = true;
  }*/
  if (  holdingRegisters[RACK_END_ADDR] == 0x00 && holdingRegisters[RACK_FAN_CMD_ADDR] == 0x01 &&
        bitRead(i2c_buffer,FAN_START_CMD_BIT) == 0x00 &&
        bitRead(i2c_buffer,FAN_START_STATUS_BIT) == 0x00 &&
        bitRead(i2c_buffer,FAN_STOP_STATUS_BIT) == 0x01 && FAN_RESET == false) {
     Serial.println("");
     Serial.println("STARTING FAN!!!!!!");
     Serial.println("");
     bitSet(i2c_buffer,FAN_START_CMD_BIT);
     FANtime = millis();
     FAN_RESET = true;
     SEND_REQUEST = true;
  }
  else if ( holdingRegisters[RACK_END_ADDR] == 0x00 && holdingRegisters[RACK_FAN_CMD_ADDR] == 0x02 &&
            bitRead(i2c_buffer,FAN_STOP_CMD_BIT) == 0x01 &&
            bitRead(i2c_buffer,FAN_START_STATUS_BIT) == 0x01 &&
            bitRead(i2c_buffer,FAN_STOP_STATUS_BIT) == 0x00 && FAN_RESET == false) {
     Serial.println("");
     Serial.println("STOPING FAN!!!!!!");
     Serial.println("");
     bitClear(i2c_buffer,FAN_STOP_CMD_BIT);
     FANtime = millis();
     FAN_RESET = true;
     SEND_REQUEST = true;
  }
  else if ( holdingRegisters[RACK_END_ADDR] == 0x00 && holdingRegisters[RACK_FAN_CMD_ADDR] == 0x01 &&
            bitRead(i2c_buffer,FAN_STOP_CMD_BIT) == 0x01 &&
            bitRead(i2c_buffer,FAN_START_STATUS_BIT) == 0x01 &&
            bitRead(i2c_buffer,FAN_STOP_STATUS_BIT) == 0x00 && FAN_RESET == false) {
              Serial.println("");
     Serial.println("ALREADY STARTED FAN !!!!!");
     Serial.println("");
     holdingRegisters[RACK_FAN_CMD_ADDR] = 0x00;  // Operator pushed 2 times same button
  }
  else if ( holdingRegisters[RACK_END_ADDR] == 0x00 && holdingRegisters[RACK_FAN_CMD_ADDR] == 0x02 &&
            bitRead(i2c_buffer,FAN_START_CMD_BIT) == 0x00 &&
            bitRead(i2c_buffer,FAN_START_STATUS_BIT) == 0x00 &&
            bitRead(i2c_buffer,FAN_STOP_STATUS_BIT) == 0x01 && FAN_RESET == false) {   
              Serial.println("");
     Serial.println("ALREADY STOPPED FAN !!!!!");
     Serial.println("");
     holdingRegisters[RACK_FAN_CMD_ADDR] = 0x00;  // Operator pushed 2 times same buttons
  }
     
  if ( holdingRegisters[RACK_END_ADDR] == 0x00 && holdingRegisters[RACK_FAN_SPEED_ADDR] == 0x01 && bitRead(i2c_buffer,LOW_NOISE_OPEN_CMD_BIT) != 0x00) {
     Serial.println("");
     Serial.println("NORMAL SPEED FAN!!!!!!");
     Serial.println("");
     bitClear(i2c_buffer,LOW_NOISE_OPEN_CMD_BIT); // Set Normal SPEED
     SEND_REQUEST = true;
  }
  else if ( holdingRegisters[RACK_END_ADDR] == 0x00 && holdingRegisters[RACK_FAN_SPEED_ADDR] == 0x02 && bitRead(i2c_buffer,NORMAL_SPEED_CLOSE_CMD_BIT) != 0x00) {
     Serial.println("");
     Serial.println("LOW NOISE  FAN!!!!!!");
     bitClear(i2c_buffer,NORMAL_SPEED_CLOSE_CMD_BIT); // Set LOW SPEED
     Serial.println("");
     SEND_REQUEST = true;
  }

  if (SEND_REQUEST == true) {
     // Send I2C buffer (8 bytes)
     Serial.print("BEGIN TRANSMISSION:sending i2c_buffer=");
     Serial.println(i2c_buffer,BIN);
     Wire.beginTransmission(SLAVE_ADDR);
     byte i2c_array[8];
     
     i2c_array[0] = (i2c_buffer >> 24) & 0xFF;
     i2c_array[1] = (i2c_buffer >> 16) & 0xFF;
     i2c_array[2] = (i2c_buffer >> 8) & 0xFF;
     i2c_array[3] = i2c_buffer & 0xFF;
     
     // Add Chekcsum CRC32
     
     uint32_t crc = CRC32::calculate(i2c_array, 4);
     
     i2c_array[4] = (crc >> 24) & 0xFF;
     i2c_array[5] = (crc >> 16) & 0xFF;
     i2c_array[6] = (crc >> 8) & 0xFF;
     i2c_array[7] = crc & 0xFF;
     Wire.write(i2c_array,8);
     
     Wire.endTransmission();
  }
}
