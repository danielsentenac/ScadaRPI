/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include <ModbusTCPSlave.h> // Modbus2 library
#include <SoftwareSerial.h> // Serial library
#include <Controllino.h> // Controllino library

/*
 *  This part is the Arduino sketch code
 */ 
// Ethernet config

byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x64, 0xAB };
IPAddress ip( 192, 168, 224, 160 ); // Controllino-8 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x65, 0xAB };
//const byte ip[] = { 192, 168, 224, 161 }; // Controllino-9 has this IP

// digital pins (OUTPUTS)
#define V21_OPEN_VALVE CONTROLLINO_R0  //(Open Command V21)
#define V21_CLOSE_VALVE CONTROLLINO_R1 //(Close Command V21)
#define V22_OPEN_VALVE CONTROLLINO_R2  //(Open Command V22)
#define V22_CLOSE_VALVE CONTROLLINO_R3 //(Close Command V22)
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

#define RS485_TRANSEIVER_STATUS  A13  // RS485 TRANSEIVER STATE

SoftwareSerial XGSSerial(CONTROLLINO_D11, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_15); // RX, TX for Controllino
SoftwareSerial RS485Serial(CONTROLLINO_D9, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_13); // RX, TX for Controllino
SoftwareSerial Spare1Serial(CONTROLLINO_D10, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_14); // RX, TX for Controllino
SoftwareSerial Spare2Serial(CONTROLLINO_D8, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_12); // RX, TX for Controllino
#define MAXIGAUGESerial  Serial2 // Serial2 is hardware serial for Controllino MAXI
#define Spare1SerialIsSoftware
//Free Serial port left: SoftwareSerial FreeSerial(CONTROLLINO_PIN_HEADER_MISO, CONTROLLINO_PIN_HEADER_SS); // RX, TX for Controllino

// Define the ModbusTCPSlave object with port = 502
ModbusTCPSlave modbus(502);

// Modbus addresses limits
#define NB_HOLDING_REGISTERS 100
#define MG_START_ADDR 0
#define MG_START_CMD_ADDR 24 //(MAXIGAUGE 1 ON/OFF Command starting point)
#define MG_END_ADDR 30
#define MG2_START_ADDR MG_END_ADDR+1
#define MG2_START_CMD_ADDR 55 //(MAXIGAUGE 2 ON/OFF Command starting point)
#define MG2_END_ADDR 61
#define V21_VALVE_ADDR MG2_END_ADDR+1 //(V21CMD)
#define V22_VALVE_ADDR MG2_END_ADDR+2 //(V22CMD) 
#define P22_SCROLL_ADDR MG2_END_ADDR+3 //(P22CMD Command)
#define V21_STATUS_ADDR MG2_END_ADDR+4 //(V21ST)
#define V22_STATUS_ADDR MG2_END_ADDR+5 //(V22ST)
#define P22_STATUS_ADDR MG2_END_ADDR+6 //(P22ST)
#define V23_STATUS_ADDR MG2_END_ADDR+7 //(V23ST manual)
#define COMPRESSAIR_STATUS_ADDR MG2_END_ADDR+8 //(COMPRESSAIRST)
#define VA1_STATUS_ADDR MG2_END_ADDR+9 //(VA1ST)
#define VA2_STATUS_ADDR MG2_END_ADDR+10 //(VA2ST)
#define DCU_START_ADDR VA2_STATUS_ADDR+1
#define DCU_STARTSTOP_ADDR 81 //(DCU ON/OFF Command)
#define DCU_STANDBY_ADDR 82  //(DCU STANDBY Command)
#define DCU_END_ADDR 83
/*#define VARIAN_START_ADDR DCU_END_ADDR+1
#define VARIAN_STARTSTOP_ADDR 60 //(VARIAN ON/OFF Command)
#define VARIAN_LOWSPEED_ADDR 61  //(VARIAN LOWSPEED Command)
#define VARIAN_END_ADDR 62
*/
/*#define XGS_START_ADDR DCU_END_ADDR+1
#define XGS_EMULT1_ADDR 58   //(XGS EMULT 1 ON/OFF Command)
#define XGS_EMULT2_ADDR 59   //(XGS EMULT 2 ON/OFF Command)
#define XGS_DEGAS_ADDR 60   //(XGS DEGAS ON/OFF Command)
#define XGS_END_ADDR 61 */
#define ARD_RESET_ADDR DCU_END_ADDR+1

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

void(*resetArd) (void) = 0; //declare reset function @ address 0

// Buffer for Serial purposes
size_t length = 64;
char *buffer = (char*)malloc(sizeof(char) * length); 
    
void setup() {
  
  // Open serial communication for Com port.
  Serial.begin(9600);

  // Initialize pin header digital 7 for RE/DE switch for RS485 communication
  pinMode(RS485_TRANSEIVER_STATUS, OUTPUT);
  digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Init Transceiver  
  RS485Serial.begin(9600);
  RS485Serial.setTimeout(1000);
  // Open serial communication for MAXIGAUGE
  MAXIGAUGESerial.begin(9600);
  MAXIGAUGESerial.setTimeout(500);
  // Open serial communication for XGS-600 Varian Gauge
  //XGSSerial.begin(19200);
  //XGSSerial.setTimeout(1000);
  // Open serial communication for Spare1
  Spare1Serial.begin(9600);
  Spare1Serial.setTimeout(500);
  // Open serial communication for Spare2
  Spare2Serial.begin(9600);
  Spare2Serial.setTimeout(500);
  //Serial3.begin(9600, SERIAL_8E1);
  /* This will initialize Controllino RS485 pins */
  //Controllino_RS485Init();
  // start the Modbus server
  StartModbusServer();

}
 
void loop() {
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
  typedef union {
     int32_t ivalue;
     uint16_t value[2];
  } Int32Uint16;
  Int32Uint16 mbiValue;
  /***********************************************************************************************************/
  /*  Serial MaxiGauge 1 data / Update Modbus register values */
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
     MAXIGAUGESerial.print(serData1);
     Serial.println(serData1);
     memset(buffer,0,length);
     MAXIGAUGESerial.readBytes(buffer,length);
     //Serial.println(buffer);
     serData2 = buffer;
     MAXIGAUGESerial.flush(); 
     // Communicate with Master (send/receive data)
     SendReceiveMaster();
     MAXIGAUGESerial.print("\x05");
     memset(buffer,0,length);
     MAXIGAUGESerial.readBytes(buffer,length);
     //Serial.println(buffer);
     serData2 = buffer;
     MAXIGAUGESerial.flush();
     Serial.println(serData2); 
     if (serData2 == "") {// Com Status Error - reset all values -
      for (int j = MG_START_ADDR; j < MG_END_ADDR; j++)
        holdingRegisters[j] = 0x00;
      holdingRegisters[MG_END_ADDR] = 0x01;
      break;
     }
     else {
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
      else if (i==6) { // (i==6) case registers
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
  /*  Serial MaxiGauge 2 data / Update Modbus register values */
  /***********************************************************************************************************/
  curaddr = MG2_START_ADDR;
  for (int i = 0; i < 7; i++) {
     // Communicate with Master (send/receive data)
     SendReceiveMaster();
     String serData1,serData2 = "";
     serData1 = "PR" + String(i+1) + "\r";
     if (i==6)
      serData1 = "SEN,0,0,0,0,0,0\r";
#if defined(Spare1SerialIsSoftware)
     Spare1Serial.listen();
#endif
     Spare1Serial.print(serData1);
     Serial.println(serData1);
     memset(buffer,0,length);
     Spare1Serial.readBytes(buffer,length);
     //Serial.println(buffer);
     serData2 = buffer;
     Spare1Serial.flush(); 
     // Communicate with Master (send/receive data)
     SendReceiveMaster();
     Spare1Serial.print("\x05");
     memset(buffer,0,length);
     Spare1Serial.readBytes(buffer,length);
     //Serial.println(buffer);
     serData2 = buffer;
     Spare1Serial.flush();
     Serial.println(serData2); 
     if (serData2 == "") {// Com Status Error - reset all values -
      for (int j = MG2_START_ADDR; j < MG2_END_ADDR; j++)
        holdingRegisters[j] = 0x00;
      holdingRegisters[MG2_END_ADDR] = 0x01;
      break;
     }
     else {
      holdingRegisters[MG2_END_ADDR] = 0x00;
      if (i < 6) { // Sensor pressures & status
        String prval = serData2.substring(serData2.indexOf(",")+1);
        mbfValue.fvalue = prval.toFloat();
        holdingRegisters[curaddr++] = mbfValue.value[0]; 
        holdingRegisters[curaddr++] = mbfValue.value[1];
        Serial.println(mbfValue.fvalue);
        String stval = serData2.substring(0,serData2.indexOf(","));
        holdingRegisters[curaddr++] = stval.toInt();
        curaddr++; // Leave a register space for (i==6) case
      }
      else if (i==6) { // (i==6) case registers
        curaddr=MG2_START_ADDR - 1; // go back to initial position (- 1)
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
    //Controllino_SwitchRS485DE(HIGH);// Enable RS485 Transmit
    //Controllino_SwitchRS485RE(LOW);
    digitalWrite(RS485_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission 
    //delay(100);
    Serial.println(serData5);    
    //Serial3.print(serData5);
    RS485Serial.print(serData5);
    //Controllino_SwitchRS485DE(LOW);// Disable RS485 Transmit
    //Controllino_SwitchRS485RE(HIGH);
    digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Switch to Reception 
    //delay(100);
    //serData6 = Serial3.readString(); 
    memset(buffer,0,length);
    RS485Serial.readBytes(buffer,length);
    Serial.println(buffer);
    serData6 = buffer;
    RS485Serial.flush();
    Serial.println(serData6);
    if (serData6 == "" || serData6.length() != 20) {// Com Status Error
      for (int j = DCU_START_ADDR; j < DCU_END_ADDR; j++)
         holdingRegisters[j] = 0x00;
      holdingRegisters[DCU_END_ADDR] = 0x01;
      break;
    }
    else {
      holdingRegisters[DCU_END_ADDR] = 0x00;
      if (serData6.length() == 20) { // Data OK
       int csum = 0; // Calculate checksum
       for (int k = 0 ; k < serData6.length() - 4; k++)
         csum+= serData6[k];
       int csumd = serData6.substring(serData6.length() - 4, serData6.length() - 1).toInt();
       if ( (csum%256) == csumd) { // Data OK
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
  }*/
}

  /***********************************************************************************************************/
  /* Perform some internal logics */
  /***********************************************************************************************************/
 
void StartModbusServer()
{    
  Ethernet.begin(mac);
  // Init ModbusTCPSlave object
  modbus.begin();

  // Configure registers
  modbus.setHoldingRegisters(holdingRegisters, NB_HOLDING_REGISTERS);
  
  // Assign Modbus reserved register addresses
  // LSB & MSB registers for float pressure values from MaxiGauge 1
   for (int i = MG_START_ADDR; i < MG_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
   holdingRegisters[MG_END_ADDR] = 0x01;
  // LSB & MSB registers for float pressure values from MaxiGauge 2
   for (int i = MG2_START_ADDR; i < MG2_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
   holdingRegisters[MG2_END_ADDR] = 0x01;
  // Digital RELAY initialization and assignation
  digitalWrite(V21_OPEN_VALVE,LOW);                           // Set V21_OPEN_VALVE LOW
  digitalWrite(V21_CLOSE_VALVE,HIGH);                         // Set V21_CLOSE_VALVE HIGH
  pinMode(V21_OPEN_VALVE, OUTPUT);                            // sets the digital pin as output for Open Valve
  pinMode(V21_CLOSE_VALVE, OUTPUT);                           // sets the digital pin as output for Close Valve
  holdingRegisters[V21_VALVE_ADDR] = 0x00;    // RESET
  digitalWrite(V22_OPEN_VALVE,LOW);                           // Set V22_VALVE OPEN LOW
  digitalWrite(V22_CLOSE_VALVE,HIGH);                          // Set V22_VALVE CLOSE HIGH
  pinMode(V22_OPEN_VALVE, OUTPUT);                            // sets the digital pin as output for Valve (RELAY)
  pinMode(V22_CLOSE_VALVE, OUTPUT);                           // sets the digital pin as output for Valve (RELAY)
  holdingRegisters[V22_VALVE_ADDR] = 0x00;    // RESET
  digitalWrite(P22_ON_SCROLL,LOW);                            // Set P22_ON_SCROLL LOW
  digitalWrite(P22_OFF_SCROLL,HIGH);                           // Set P22_OFF_SCROLL HIGH
  pinMode(P22_ON_SCROLL, OUTPUT);                             // sets the digital pin as output for Switch ON Scroll
  pinMode(P22_OFF_SCROLL, OUTPUT);                            // sets the digital pin as output for Switch OFF Scroll
  holdingRegisters[P22_SCROLL_ADDR] = 0x00;   // RESET
  // Digital VALVE INPUTS initialization and assignation
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

  // registers for XGS-600 Varian Gauge
  /*for (int i = XGS_START_ADDR; i < XGS_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
  holdingRegisters[XGS_END_ADDR] = 0x01;*/
  
  
  // register for Arduino reset
  holdingRegisters[ARD_RESET_ADDR] = 0x00;     // Arduino Global Reset Status

}
void SendReceiveMaster()
{    
  // Process modbus requests
  modbus.update();
  
  /***********************************************************************************************************/
  /* Update Valve V21 position (Open/Close) */
  /***********************************************************************************************************/
  if (holdingRegisters[V21_VALVE_ADDR] == 0x02) {
     digitalWrite(V21_CLOSE_VALVE,LOW); // CLOSE VALVE
     delay(1000);
     digitalWrite(V21_CLOSE_VALVE,HIGH); // CLOSE VALVE
  }
  else if (holdingRegisters[V21_VALVE_ADDR] == 0x01) {
     digitalWrite(V21_OPEN_VALVE,HIGH); // OPEN VALVE
     delay(1000);
     digitalWrite(V21_OPEN_VALVE,LOW); // OPEN VALVE
     // Check status update for 4s
     delay(4000);
     if (digitalRead(V21_CLOSE_STATUS) == HIGH && digitalRead(V21_OPEN_STATUS) == LOW) { 
        // Valve still closed, reset command
        digitalWrite(V21_CLOSE_VALVE,LOW); // CLOSE VALVE
        delay(1000);
        digitalWrite(V21_CLOSE_VALVE,HIGH); // CLOSE VALVE
     }
  }
  holdingRegisters[V21_VALVE_ADDR] = 0x00; // Reset register
  
  /***********************************************************************************************************/
  /***********************************************************************************************************/
  /* Update Valve V22 position (Open/Close) */
  /***********************************************************************************************************/
  if (holdingRegisters[V22_VALVE_ADDR] == 0x02) {
     digitalWrite(V22_CLOSE_VALVE,LOW); // CLOSE VALVE
     delay(1000);
     digitalWrite(V22_CLOSE_VALVE,HIGH); // CLOSE VALVE
  }
  else if (holdingRegisters[V22_VALVE_ADDR] == 0x01) {
     digitalWrite(V22_OPEN_VALVE,HIGH); // OPEN VALVE
     delay(1000);
     digitalWrite(V22_OPEN_VALVE,LOW); // OPEN VALVE
     // Check status update for 4s
     delay(4000);
     if (digitalRead(V22_CLOSE_STATUS) == HIGH && digitalRead(V22_OPEN_STATUS) == LOW) { 
        // Valve still closed, reset command
        digitalWrite(V22_CLOSE_VALVE,LOW); // CLOSE VALVE
        delay(1000);
        digitalWrite(V22_CLOSE_VALVE,HIGH); // CLOSE VALVE
     }
  }
  holdingRegisters[V22_VALVE_ADDR] = 0x00; // Reset register
 
  /***********************************************************************************************************/
  /***********************************************************************************************************/
  /* Update Scroll P22 position (Open/Close) */
  /***********************************************************************************************************/
  if (holdingRegisters[P22_SCROLL_ADDR] == 0x02) {
     digitalWrite(P22_OFF_SCROLL,LOW); // SWITCH OFF SCROLL
     delay(1000);
     digitalWrite(P22_OFF_SCROLL,HIGH); // SWITCH OFF SCROLL
  }
  else if (holdingRegisters[P22_SCROLL_ADDR] == 0x01) { 
     digitalWrite(P22_ON_SCROLL,HIGH); // SWITCH ON SCROLL
     delay(1000);
     digitalWrite(P22_ON_SCROLL,LOW); // SWITCH ON SCROLL
  }
  holdingRegisters[P22_SCROLL_ADDR] = 0x00; // Reset register
 
  /***********************************************************************************************************/
  /* Update Valve position STATUS register (Open/Close) */
  /***********************************************************************************************************/
 /* Serial.print("PIN4_VALVE=");Serial.print(digitalRead(PIN4_VALVE));Serial.print(" HIGH=");
  Serial.print(HIGH);Serial.print(" LOW=");Serial.println(LOW);*/
  if (digitalRead(V21_OPEN_STATUS) == HIGH && digitalRead(V21_CLOSE_STATUS) == LOW)
     holdingRegisters[V21_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(V21_CLOSE_STATUS) == HIGH && digitalRead(V21_OPEN_STATUS) == LOW) {
     if (holdingRegisters[V21_STATUS_ADDR] == 0x01 ||
         holdingRegisters[V21_STATUS_ADDR] == 0x00) {
        // reset close command
        digitalWrite(V21_CLOSE_VALVE,LOW); // CLOSE VALVE
        delay(1000);
        digitalWrite(V21_CLOSE_VALVE,HIGH); // CLOSE VALVE
     }
     holdingRegisters[V21_STATUS_ADDR] = 0x02; // CLOSED VALVE
  }
  else {
     holdingRegisters[V21_STATUS_ADDR] = 0x00; // MOVING VALVE
     //Serial.print("V21_OPEN_STATUS=");Serial.println(digitalRead(V21_OPEN_STATUS));
     //Serial.print("V21_CLOSE_STATUS=");Serial.println(digitalRead(V21_CLOSE_STATUS));
  }
  if (digitalRead(V22_OPEN_STATUS) == HIGH && digitalRead(V22_CLOSE_STATUS) == LOW)
     holdingRegisters[V22_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(V22_CLOSE_STATUS) == HIGH && digitalRead(V22_OPEN_STATUS) == LOW) {
     if (holdingRegisters[V22_STATUS_ADDR] == 0x01 ||
         holdingRegisters[V22_STATUS_ADDR] == 0x00) {
        // reset close command
        digitalWrite(V22_CLOSE_VALVE,LOW); // CLOSE VALVE
        delay(1000);
        digitalWrite(V22_CLOSE_VALVE,HIGH); // CLOSE VALVE
     }
     holdingRegisters[V22_STATUS_ADDR] = 0x02; // CLOSED VALVE
  }
  else
     holdingRegisters[V22_STATUS_ADDR] = 0x00; // MOVING VALVE
     
  if (digitalRead(P22_STATUS) == HIGH)
     holdingRegisters[P22_STATUS_ADDR] = 0x01; // SCROLL ON
  else if (digitalRead(P22_STATUS) == LOW)
     holdingRegisters[P22_STATUS_ADDR] = 0x00; // SCROLL OFF
  else
     holdingRegisters[P22_STATUS_ADDR] = 0x06; // ERROR SCROLL
     
  if (digitalRead(V23_OPEN_STATUS) == HIGH && digitalRead(V23_CLOSE_STATUS) == LOW)
     holdingRegisters[V23_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(V23_CLOSE_STATUS) == HIGH && digitalRead(V23_OPEN_STATUS) == LOW)
     holdingRegisters[V23_STATUS_ADDR] = 0x02; // CLOSED VALVE 
  else
     holdingRegisters[V23_STATUS_ADDR] = 0x00; // MOVING VALVE

  if (digitalRead(COMPRESS_AIR_STATUS) == HIGH)
     holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x01; // COMPRESS AIR ON
  else if (digitalRead(COMPRESS_AIR_STATUS) == LOW)
     holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x00; // COMPRESS AIR OFF 
  else
     holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x03; // ERROR COMPRESS AIR

  if (digitalRead(VA1_OPEN_STATUS) == HIGH && digitalRead(VA1_CLOSE_STATUS) == LOW)
     holdingRegisters[VA1_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(VA1_CLOSE_STATUS) == HIGH && digitalRead(VA1_OPEN_STATUS) == LOW)
     holdingRegisters[VA1_STATUS_ADDR] = 0x02; // CLOSED VALVE 
  else
     holdingRegisters[VA1_STATUS_ADDR] = 0x00; // MOVING VALVE

  if (digitalRead(VA2_OPEN_STATUS) == HIGH && digitalRead(VA2_CLOSE_STATUS) == LOW)
     holdingRegisters[VA2_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(VA2_CLOSE_STATUS) == HIGH && digitalRead(VA2_OPEN_STATUS) == LOW)
     holdingRegisters[VA2_STATUS_ADDR] = 0x02; // CLOSED VALVE 
  else
     holdingRegisters[VA2_STATUS_ADDR] = 0x00; // MOVING VALVE
     
  /* Update MAXIGAUGE 1 Sensor ON/OFF Status */
  /***********************************************************************************************************/
  for (int j = MG_START_CMD_ADDR; j < MG_END_ADDR; j++) {
    if ( holdingRegisters[MG_END_ADDR] == 0x00 && holdingRegisters[j] != 0x00 ) {
      /*Serial.print(" channel:");
      Serial.print(j);
      Serial.print(" set new register:");
      Serial.println(holdingRegisters[j]);
      */
      //delay(100);
      String cmd;
      int signal = 0;
      if (holdingRegisters[j] == 0x01)
        signal = 2;
      if (holdingRegisters[j] == 0x02)
        signal = 1;
      switch(j-MG_START_CMD_ADDR) {
        case 0:
          cmd = "SEN,"+String(signal)+",0,0,0,0,0\r";
          break;
        case 1:
          cmd = "SEN,0,"+String(signal)+",0,0,0,0\r";
          break;
        case 2:
          cmd = "SEN,0,0,"+String(signal)+",0,0,0\r";
          break;
        case 3:
          cmd = "SEN,0,0,0,"+String(signal)+",0,0\r";
          break;
        case 4:
          cmd = "SEN,0,0,0,0,"+String(signal)+",0\r";
          break;
        case 5:
          cmd = "SEN,0,0,0,0,0,"+String(signal)+"\r";
          break;
      }
#if defined(MAXIGAUGESerialIsSoftware)
     MAXIGAUGESerial.listen();
#endif
      MAXIGAUGESerial.print(cmd);
      Serial.println(cmd);
      MAXIGAUGESerial.readString();
      MAXIGAUGESerial.print("\x05");
      MAXIGAUGESerial.readString();
      holdingRegisters[j] = 0x00;  // RESET register
    }
    
  } 
  /* Update MAXIGAUGE 2 Sensor ON/OFF Status */
  /***********************************************************************************************************/
  for (int j = MG2_START_CMD_ADDR; j < MG2_END_ADDR; j++) {
    if ( holdingRegisters[MG2_END_ADDR] == 0x00 && holdingRegisters[j] != 0x00 ) {
      /*Serial.print(" channel:");
      Serial.print(j);
      Serial.print(" set new register:");
      Serial.println(holdingRegisters[j]);
      */
      //delay(100);
      String cmd;
      int signal = 0;
      if (holdingRegisters[j] == 0x01)
        signal = 2;
      if (holdingRegisters[j] == 0x02)
        signal = 1;
      switch(j-MG2_START_CMD_ADDR) {
        case 0:
          cmd = "SEN,"+String(signal)+",0,0,0,0,0\r";
          break;
        case 1:
          cmd = "SEN,0,"+String(signal)+",0,0,0,0\r";
          break;
        case 2:
          cmd = "SEN,0,0,"+String(signal)+",0,0,0\r";
          break;
        case 3:
          cmd = "SEN,0,0,0,"+String(signal)+",0,0\r";
          break;
        case 4:
          cmd = "SEN,0,0,0,0,"+String(signal)+",0\r";
          break;
        case 5:
          cmd = "SEN,0,0,0,0,0,"+String(signal)+"\r";
          break;
      }
#if defined(Spare1SerialIsSoftware)
     Spare1Serial.listen();
#endif
      Spare1Serial.print(cmd);
      Serial.println(cmd);
      Spare1Serial.readString();
      Spare1Serial.print("\x05");
      Spare1Serial.readString();
      holdingRegisters[j] = 0x00;  // RESET register
    }
    
  } 
  /***********************************************************************************************************/
  /* Update SCU1600 Turbo START/STOP Status */
  /***********************************************************************************************************
  if ( holdingRegisters[SCU_STARTSTOP_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[SCU_STARTSTOP_ADDR] == 0x02 )
        cmd = "\x02\x30\x30\x31\x20\x45\x02\x03\xA8";   // STOP COMMAND
      else if ( holdingRegisters[SCU_STARTSTOP_ADDR] == 0x01 )
        cmd = "\x02\x30\x30\x31\x20\x45\x01\x03\xA7";  //START COMMAND
      SCU1600Serial.listen();
      SCU1600Serial.print(cmd);
      Serial.println(cmd);
      holdingRegisters[SCU_STARTSTOP_ADDR] = 0x00;  // RESET register
  }
  /***********************************************************************************************************/
  /* Update VARIAN Turbo START/STOP LOWSPEED ON/OFF COMMAND */
  /***********************************************************************************************************
  if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] == 0x02 )
        cmd = "\x02\x80\x30\x30\x30\x31\x30\x03\x42\x32";   // STOP COMMAND
      else if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] == 0x01 )
        cmd = "\x02\x80\x30\x30\x30\x31\x31\x03\x42\x33";  //START COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(cmd);
      Serial.println(cmd);
      holdingRegisters[VARIAN_STARTSTOP_ADDR] = 0x00;  // RESET register
  }
  if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] == 0x02 )
        cmd =  "\x02\x80\x30\x30\x31\x31\x30\x03\x42\x33";   // LOWSPEED OFF COMMAND
      else if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] == 0x01 )
        cmd =  "\x02\x80\x30\x30\x31\x31\x31\x03\x42\x34";  // LOWSPEED ON COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(cmd);
      Serial.println(cmd);
      holdingRegisters[VARIAN_LOWSPEED_ADDR] = 0x00;  // RESET register
  }
  /***********************************************************************************************************/
  /* Update DCU (PFEIFFER) Turbo START/STOP STANDBY,ON/OFF COMMAND */
  /***********************************************************************************************************/
  if ( holdingRegisters[DCU_END_ADDR] == 0x00 && holdingRegisters[DCU_STARTSTOP_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[DCU_STARTSTOP_ADDR] == 0x02 )
        cmd = "0011001006000000009\r";  // STOP PUMPING COMMAND
      else if ( holdingRegisters[DCU_STARTSTOP_ADDR] == 0x01 )
        cmd = "0011001006111111015\r";  //START PUMPING COMMAND
      Serial.println(cmd);
      RS485Serial.listen();
      digitalWrite(RS485_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission
      RS485Serial.print(cmd);
      digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Switch to Reception
      holdingRegisters[DCU_STARTSTOP_ADDR] = 0x00; // RESET COMMAND
  }
  if ( holdingRegisters[DCU_END_ADDR] == 0x00 && holdingRegisters[DCU_STANDBY_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[DCU_STANDBY_ADDR] == 0x02 )
        cmd = "0011000206000000010\r";   // STANDBY OFF COMMAND
      else if ( holdingRegisters[DCU_STANDBY_ADDR] == 0x01 )
        cmd = "0011000206111111016\r";  // STANDBY ON COMMAND
      RS485Serial.listen();
      digitalWrite(RS485_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission
      RS485Serial.print(cmd);
      Serial.println(cmd);
      digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Switch to Reception
      holdingRegisters[DCU_STANDBY_ADDR] = 0x00; // RESET COMMAND
  }
   /***********************************************************************************************************/
  /* Update VARIAN Turbo START/STOP LOWSPEED ON/OFF COMMAND */
  /***********************************************************************************************************
  if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] == 0x02 )
        cmd = "\x02\x80\x30\x30\x30\x31\x30\x03\x42\x32";   // STOP COMMAND
      else if ( holdingRegisters[VARIAN_STARTSTOP_ADDR] == 0x01 )
        cmd = "\x02\x80\x30\x30\x30\x31\x31\x03\x42\x33";  //START COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(cmd);
      Serial.println(cmd);
      holdingRegisters[VARIAN_STARTSTOP_ADDR] = 0x00;  // RESET register
  }
  if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] == 0x02 )
        cmd =  "\x02\x80\x30\x30\x31\x31\x30\x03\x42\x33";   // LOWSPEED OFF COMMAND
      else if ( holdingRegisters[VARIAN_LOWSPEED_ADDR] == 0x01 )
        cmd =  "\x02\x80\x30\x30\x31\x31\x31\x03\x42\x34";  // LOWSPEED ON COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(cmd);
      Serial.println(cmd);
      holdingRegisters[VARIAN_LOWSPEED_ADDR] = 0x00;  // RESET register
  }
  */
  /***********************************************************************************************************/
  /* Update XGS (Varian Gauge) ON/OFF COMMAND */
  /***********************************************************************************************************
  if ( holdingRegisters[XGS_END_ADDR] == 0x00 && holdingRegisters[XGS_EMULT1_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[XGS_EMULT1_ADDR] == 0x02 )
        cmd = "#0030UHFIG1\r";   // EMULT 1 OFF COMMAND
      else if ( holdingRegisters[XGS_EMULT1_ADDR] == 0x01 )
        cmd = "#0031UHFIG1\r";   // EMULT 1 ON COMMAND
      Serial.println(cmd);
      XGSSerial.listen();
      XGSSerial.print(cmd);
      holdingRegisters[XGS_EMULT1_ADDR] = 0x00; // RESET COMMAND
  }
  if ( holdingRegisters[XGS_END_ADDR] == 0x00 && holdingRegisters[XGS_EMULT2_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[XGS_EMULT2_ADDR] == 0x02 )
        cmd = "#0030UHFIG1\r";   // EMULT 2 OFF COMMAND
      else if ( holdingRegisters[XGS_EMULT2_ADDR] == 0x01 )
        cmd = "#0033UHFIG1\r";   // EMULT 2 ON COMMAND
      Serial.println(cmd);
      XGSSerial.listen();
      XGSSerial.print(cmd);
      holdingRegisters[XGS_EMULT2_ADDR] = 0x00; // RESET COMMAND
  }
  if ( holdingRegisters[XGS_END_ADDR] == 0x00 && holdingRegisters[XGS_DEGAS_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[XGS_DEGAS_ADDR] == 0x02 )
        cmd = "#0040UHFIG1\r";   // DEGAS OFF COMMAND
      else if ( holdingRegisters[XGS_DEGAS_ADDR] == 0x01 )
        cmd = "#0041UHFIG1\r";   // DEGAS ON COMMAND
      Serial.println(cmd);
      XGSSerial.listen();
      XGSSerial.print(cmd);
      holdingRegisters[XGS_DEGAS_ADDR] = 0x00; // RESET COMMAND
  }*/
}

