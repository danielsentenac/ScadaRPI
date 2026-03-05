/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include <SPI.h>
#include <Ethernet.h>   // Ethernet library for Leonardo Eth
#include <libmodbusmq.h> // Modbus library
#include <SoftwareSerial.h> // Serial library
#include <Controllino.h> // Controllino library

/*
 *  This part is the Arduino sketch code
 */ 
// Ethernet config

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x57, 0xAB };
//const byte ip[] = { 192, 168, 224, 152 }; // Controllino-1 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x58, 0xAB };
//const byte ip[] = { 192, 168, 224, 154 }; // Controllino-2 has this IP

byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x59, 0xAB };
const byte ip[] = { 192, 168, 224, 155 }; // Controllino-3 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x60, 0xAB };
//const byte ip[] = { 192, 168, 224, 156 }; // Controllino-4 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x61, 0xAB };
//const byte ip[] = { 192, 168, 224, 157 }; // Controllino-5 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x62, 0xAB };
//const byte ip[] = { 192, 168, 224, 158 }; // Controllino-6 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x64, 0xAB };
//const byte ip[] = { 192, 168, 224, 160 }; // Controllino-8 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x65, 0xAB };
//const byte ip[] = { 192, 168, 224, 161 }; // Controllino-9 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x73, 0xAB };
//const byte ip[] = { 192, 168, 224, 169 }; // Controllino-17 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x76, 0xAB };
//const byte ip[] = { 192, 168, 224, ??? }; // Controllino-20 has this IP

 
// Modbus objects
#define SLAVE 0x01
modbus_param_t mb_param;     // generic mb params
modbus_mapping_t mb_mapping; // registers

/* Server object will run on Arduino, will be initialized later */
EthernetServer server(MODBUS_TCP_DEFAULT_PORT);

/* Client object connected with Arduino. This initializaton will be reassigned */
EthernetClient client = 0;

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
//#define MAXIGAUGESerialIsSoftware
//Free Serial port left: SoftwareSerial FreeSerial(CONTROLLINO_PIN_HEADER_MISO, CONTROLLINO_PIN_HEADER_SS); // RX, TX for Controllino

// Modbus addresses limits
#define NB_HOLDING_REGISTERS 70
#define MG_START_ADDR 0
#define MG_START_CMD_ADDR 24 //(MAXIGAUGE ON/OFF Command starting point)
#define MG_END_ADDR 30
#define V21_VALVE_ADDR MG_END_ADDR+1 //(V21CMD)
#define V22_VALVE_ADDR MG_END_ADDR+2 //(V22CMD) 
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
/*#define VARIAN_START_ADDR DCU_END_ADDR+1
#define VARIAN_STARTSTOP_ADDR 60 //(VARIAN ON/OFF Command)
#define VARIAN_LOWSPEED_ADDR 61  //(VARIAN LOWSPEED Command)
#define VARIAN_END_ADDR 62
*/
#define XGS_START_ADDR DCU_END_ADDR+1
#define XGS_EMULT1_ADDR 58   //(XGS EMULT 1 ON/OFF Command)
#define XGS_EMULT2_ADDR 59   //(XGS EMULT 2 ON/OFF Command)
#define XGS_DEGAS_ADDR 60   //(XGS DEGAS ON/OFF Command)
#define XGS_END_ADDR 61 
#define ARD_RESET_ADDR XGS_END_ADDR+1

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
  MAXIGAUGESerial.setTimeout(1000);
  // Open serial communication for XGS-600 Varian Gauge
  XGSSerial.begin(19200);
  XGSSerial.setTimeout(1000);
  // Open serial communication for Spare1
  Spare1Serial.begin(9600);
  Spare1Serial.setTimeout(1000);
  // Open serial communication for Spare2
  Spare2Serial.begin(9600);
  Spare2Serial.setTimeout(1000);
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
  if (mb_mapping.tab_holding_registers[ARD_RESET_ADDR] == 0x01)
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
        mb_mapping.tab_holding_registers[j] = 0x00;
      mb_mapping.tab_holding_registers[MG_END_ADDR] = 0x01;
      break;
     }
     else {
      mb_mapping.tab_holding_registers[MG_END_ADDR] = 0x00;
      if (i < 6) { // Sensor pressures & status
        String prval = serData2.substring(serData2.indexOf(",")+1);
        mbfValue.fvalue = prval.toFloat();
        mb_mapping.tab_holding_registers[curaddr++] = mbfValue.value[0]; 
        mb_mapping.tab_holding_registers[curaddr++] = mbfValue.value[1];
        String stval = serData2.substring(0,serData2.indexOf(","));
        mb_mapping.tab_holding_registers[curaddr++] = stval.toInt();
        curaddr++; // Leave a register space for (i==6) case
      }
      else if (i==6) { // (i==6) case registers
        curaddr=MG_START_ADDR - 1; // go back to initial position (- 1)
        for (int j = 0; j < 6 ; j++) {
          curaddr+=4;
          String senst = serData2.substring(2*j,2*j+1);
          mb_mapping.tab_holding_registers[curaddr] = senst.toInt();
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
        mb_mapping.tab_holding_registers[j] = 0x00;
      mb_mapping.tab_holding_registers[SCU_END_ADDR] = 0x01;
     } 
     else {
      mb_mapping.tab_holding_registers[SCU_END_ADDR] = 0x00;
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
       mb_mapping.tab_holding_registers[curaddr++] = result;
      }
      else {
       mb_mapping.tab_holding_registers[curaddr++] = result;
       mb_mapping.tab_holding_registers[curaddr++] = result1;
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
        mb_mapping.tab_holding_registers[j] = 0xFF;
      mb_mapping.tab_holding_registers[VARIAN_END_ADDR] = 0x01;
     }
     else {
      mb_mapping.tab_holding_registers[VARIAN_END_ADDR] = 0x00;
      uint16_t result=0;
      if ( i != 6 && i != 7)
        serData6 = serData6.substring(6,12);
      else
        serData6 = serData6.substring(6,7);
      sscanf(serData6.c_str(),"%d",&result);
      mb_mapping.tab_holding_registers[curaddr++] = result;
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
    //Serial.println(buffer);
    serData6 = buffer;
    RS485Serial.flush();
    Serial.println(serData6);
    if (serData6 == "" || serData6.length() != 20) {// Com Status Error
      for (int j = DCU_START_ADDR; j < DCU_END_ADDR; j++)
         mb_mapping.tab_holding_registers[j] = 0x00;
      mb_mapping.tab_holding_registers[DCU_END_ADDR] = 0x01;
      break;
    }
    else {
      mb_mapping.tab_holding_registers[DCU_END_ADDR] = 0x00;
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
             mb_mapping.tab_holding_registers[curaddr++] = result;
          }
          else if (i == 4 || i == 5 || i == 6 || i == 7) { // Boolean value
            if ( serData6.substring(10,16) == "111111")
              mb_mapping.tab_holding_registers[curaddr++] = 0x01;
            else if ( serData6.substring(10,16) == "000000")
              mb_mapping.tab_holding_registers[curaddr++] = 0x00;
            
          }
          else if (i == 8) { // String value
            if ( (serData6.substring(10,16) == "no Err") || (serData6.substring(10,13) == "Wrn") || (serData6.substring(10,16) == "000000")) {
              mb_mapping.tab_holding_registers[curaddr++] = 0x00;
            }
            else if ( serData6.substring(10,13) == "Err") {
              uint16_t result=0;
              //Serial.println(serData6[i].substring(13,16));
              sscanf(serData6.substring(13,16).c_str(),"%d",&result);
              mb_mapping.tab_holding_registers[curaddr++] = result;
            }
          }
        }
      }
    }
  }
 }
 /***********************************************************************************************************/
  /*  Serial XGS-600 data / Update Modbus register values */
  /**********************************************************************************************************/
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
         mb_mapping.tab_holding_registers[j] = 0x00;
      mb_mapping.tab_holding_registers[XGS_END_ADDR] = 0x01;
      break;
    }
    else { // Data OK
      mb_mapping.tab_holding_registers[XGS_END_ADDR] = 0x00;
      uint16_t result=0;
      if ( i == 0 ) { // Pressure float
        String prval = serData8.substring(1);
        mbfValue.fvalue = prval.toFloat();
        mb_mapping.tab_holding_registers[curaddr++] = mbfValue.value[0]; 
        mb_mapping.tab_holding_registers[curaddr++] = mbfValue.value[1];
      }
      else { // Gauge Emission & Degas & Filament status
        String prval = serData8.substring(1);
        uint16_t result=0;
        sscanf(serData8.substring(1).c_str(),"%d",&result); // 00 -> OFF; 01 -> ON // 01 -> Fil1; 02 -> Fil2
        mb_mapping.tab_holding_registers[curaddr++] = result;
      }
    }
 }
}

  /***********************************************************************************************************/
  /* Perform some internal logics */
  /***********************************************************************************************************/
 
void StartModbusServer()
{    
  int ret;
  modbus_init_tcp(&mb_param, mac, ip, MODBUS_TCP_DEFAULT_PORT, SLAVE);
  modbus_set_slave(&mb_param, SLAVE);
  modbus_set_error_handling(&mb_param, FLUSH_OR_CONNECT_ON_ERROR);
  Serial.println("Arduino Modbus Slave started");
  ret = modbus_mapping_new(&mb_param, &mb_mapping, NB_HOLDING_REGISTERS);
  if (ret < 0) {
    Serial.println("Memory allocation failed, restarting Arduino...");
  }
  // Assign Modbus reserved register addresses
  // LSB & MSB registers for float pressure values from MaxiGauge
   for (int i = MG_START_ADDR; i < MG_END_ADDR; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
   mb_mapping.tab_holding_registers[MG_END_ADDR] = 0x01;
  // Digital RELAY initialization and assignation
  digitalWrite(V21_OPEN_VALVE,LOW);                           // Set V21_OPEN_VALVE LOW
  digitalWrite(V21_CLOSE_VALVE,HIGH);                         // Set V21_CLOSE_VALVE HIGH
  pinMode(V21_OPEN_VALVE, OUTPUT);                            // sets the digital pin as output for Open Valve
  pinMode(V21_CLOSE_VALVE, OUTPUT);                           // sets the digital pin as output for Close Valve
  mb_mapping.tab_holding_registers[V21_VALVE_ADDR] = 0x00;    // RESET
  digitalWrite(V22_OPEN_VALVE,LOW);                           // Set V22_VALVE OPEN LOW
  digitalWrite(V22_CLOSE_VALVE,HIGH);                          // Set V22_VALVE CLOSE HIGH
  pinMode(V22_OPEN_VALVE, OUTPUT);                            // sets the digital pin as output for Valve (RELAY)
  pinMode(V22_CLOSE_VALVE, OUTPUT);                           // sets the digital pin as output for Valve (RELAY)
  mb_mapping.tab_holding_registers[V22_VALVE_ADDR] = 0x00;    // RESET
  digitalWrite(P22_ON_SCROLL,LOW);                            // Set P22_ON_SCROLL LOW
  digitalWrite(P22_OFF_SCROLL,HIGH);                           // Set P22_OFF_SCROLL HIGH
  pinMode(P22_ON_SCROLL, OUTPUT);                             // sets the digital pin as output for Switch ON Scroll
  pinMode(P22_OFF_SCROLL, OUTPUT);                            // sets the digital pin as output for Switch OFF Scroll
  mb_mapping.tab_holding_registers[P22_SCROLL_ADDR] = 0x00;   // RESET
  // Digital VALVE INPUTS initialization and assignation
  pinMode(V21_OPEN_STATUS, INPUT);                            // sets the digital pin as output for Valve V21 OPEN STATUS
  pinMode(V21_CLOSE_STATUS, INPUT);                           // sets the digital pin as output for Valve V21 CLOSE STATUS
  mb_mapping.tab_holding_registers[V21_STATUS_ADDR] = 0x00;   // RESET
  pinMode(V22_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve V22 OPEN STATUS
  pinMode(V22_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve V22 CLOSE STATUS
  mb_mapping.tab_holding_registers[V22_STATUS_ADDR] = 0x00;   // RESET
  pinMode(P22_STATUS, INPUT);                              // sets the digital pin as input for Scroll ON/OFF STATUS
  mb_mapping.tab_holding_registers[P22_STATUS_ADDR] = 0x00;   // RESET
  pinMode(V23_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve OPEN STATUS
  pinMode(V23_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve CLOSE STATUS
  mb_mapping.tab_holding_registers[V23_STATUS_ADDR] = 0x00;   // RESET
  pinMode(COMPRESS_AIR_STATUS, INPUT);                        // sets the digital pin as input for Compress Air STATUS
  mb_mapping.tab_holding_registers[COMPRESSAIR_STATUS_ADDR] = 0x00;  // RESET
  pinMode(VA1_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve OPEN STATUS
  pinMode(VA1_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve CLOSE STATUS
  mb_mapping.tab_holding_registers[VA1_STATUS_ADDR] = 0x00;   // RESET
  pinMode(VA2_OPEN_STATUS, INPUT);                            // sets the digital pin as input for Valve OPEN STATUS
  pinMode(VA2_CLOSE_STATUS, INPUT);                           // sets the digital pin as input for Valve CLOSE STATUS
  mb_mapping.tab_holding_registers[VA2_STATUS_ADDR] = 0x00;   // RESET
  
  // registers for DCU turbo
  for (int i = DCU_START_ADDR; i < DCU_END_ADDR; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
  mb_mapping.tab_holding_registers[DCU_END_ADDR] = 0x01;

  // registers for XGS-600 Varian Gauge
  for (int i = XGS_START_ADDR; i < XGS_END_ADDR; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
  mb_mapping.tab_holding_registers[XGS_END_ADDR] = 0x01;
  
  
  // register for Arduino reset
  mb_mapping.tab_holding_registers[ARD_RESET_ADDR] = 0x00;     // Arduino Global Reset Status

  modbus_slave_listen_tcp(&mb_param, 1);
}
void SendReceiveMaster()
{    
    uint8_t query[MAX_MESSAGE_LENGTH];
    int ret = modbus_slave_receive_tcp(&mb_param, MSG_LENGTH_UNDEFINED, query);
    // ret is the query size
    if(ret >= 0) {
       modbus_slave_manage(&mb_param, query, ret, &mb_mapping);
    }
    else if(ret == CONNECTION_CLOSED) {
       /* Connection closed by the client, end of server */
       // Serial.println("Server ending, closed by client");
    }
  /***********************************************************************************************************/
  /* Update Valve V21 position (Open/Close) */
  /***********************************************************************************************************/
  if (mb_mapping.tab_holding_registers[V21_VALVE_ADDR] == 0x02) {
     digitalWrite(V21_CLOSE_VALVE,LOW); // CLOSE VALVE
     delay(1000);
     digitalWrite(V21_CLOSE_VALVE,HIGH); // CLOSE VALVE
  }
  else if (mb_mapping.tab_holding_registers[V21_VALVE_ADDR] == 0x01) {
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
  mb_mapping.tab_holding_registers[V21_VALVE_ADDR] = 0x00; // Reset register
  
  /***********************************************************************************************************/
  /***********************************************************************************************************/
  /* Update Valve V22 position (Open/Close) */
  /***********************************************************************************************************/
  if (mb_mapping.tab_holding_registers[V22_VALVE_ADDR] == 0x02) {
     digitalWrite(V22_CLOSE_VALVE,LOW); // CLOSE VALVE
     delay(1000);
     digitalWrite(V22_CLOSE_VALVE,HIGH); // CLOSE VALVE
  }
  else if (mb_mapping.tab_holding_registers[V22_VALVE_ADDR] == 0x01) {
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
  mb_mapping.tab_holding_registers[V22_VALVE_ADDR] = 0x00; // Reset register
 
  /***********************************************************************************************************/
  /***********************************************************************************************************/
  /* Update Scroll P22 position (Open/Close) */
  /***********************************************************************************************************/
  if (mb_mapping.tab_holding_registers[P22_SCROLL_ADDR] == 0x02) {
     digitalWrite(P22_OFF_SCROLL,LOW); // SWITCH OFF SCROLL
     delay(1000);
     digitalWrite(P22_OFF_SCROLL,HIGH); // SWITCH OFF SCROLL
  }
  else if (mb_mapping.tab_holding_registers[P22_SCROLL_ADDR] == 0x01) { 
     digitalWrite(P22_ON_SCROLL,HIGH); // SWITCH ON SCROLL
     delay(1000);
     digitalWrite(P22_ON_SCROLL,LOW); // SWITCH ON SCROLL
  }
  mb_mapping.tab_holding_registers[P22_SCROLL_ADDR] = 0x00; // Reset register
 
  /***********************************************************************************************************/
  /* Update Valve position STATUS register (Open/Close) */
  /***********************************************************************************************************/
 /* Serial.print("PIN4_VALVE=");Serial.print(digitalRead(PIN4_VALVE));Serial.print(" HIGH=");
  Serial.print(HIGH);Serial.print(" LOW=");Serial.println(LOW);*/
  if (digitalRead(V21_OPEN_STATUS) == HIGH && digitalRead(V21_CLOSE_STATUS) == LOW)
     mb_mapping.tab_holding_registers[V21_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(V21_CLOSE_STATUS) == HIGH && digitalRead(V21_OPEN_STATUS) == LOW) {
     if (mb_mapping.tab_holding_registers[V21_STATUS_ADDR] == 0x01 ||
         mb_mapping.tab_holding_registers[V21_STATUS_ADDR] == 0x00) {
        // reset close command
        digitalWrite(V21_CLOSE_VALVE,LOW); // CLOSE VALVE
        delay(1000);
        digitalWrite(V21_CLOSE_VALVE,HIGH); // CLOSE VALVE
     }
     mb_mapping.tab_holding_registers[V21_STATUS_ADDR] = 0x02; // CLOSED VALVE
  }
  else {
     mb_mapping.tab_holding_registers[V21_STATUS_ADDR] = 0x00; // MOVING VALVE
     //Serial.print("V21_OPEN_STATUS=");Serial.println(digitalRead(V21_OPEN_STATUS));
     //Serial.print("V21_CLOSE_STATUS=");Serial.println(digitalRead(V21_CLOSE_STATUS));
  }
  if (digitalRead(V22_OPEN_STATUS) == HIGH && digitalRead(V22_CLOSE_STATUS) == LOW)
     mb_mapping.tab_holding_registers[V22_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(V22_CLOSE_STATUS) == HIGH && digitalRead(V22_OPEN_STATUS) == LOW) {
     if (mb_mapping.tab_holding_registers[V22_STATUS_ADDR] == 0x01 ||
         mb_mapping.tab_holding_registers[V22_STATUS_ADDR] == 0x00) {
        // reset close command
        digitalWrite(V22_CLOSE_VALVE,LOW); // CLOSE VALVE
        delay(1000);
        digitalWrite(V22_CLOSE_VALVE,HIGH); // CLOSE VALVE
     }
     mb_mapping.tab_holding_registers[V22_STATUS_ADDR] = 0x02; // CLOSED VALVE
  }
  else
     mb_mapping.tab_holding_registers[V22_STATUS_ADDR] = 0x00; // MOVING VALVE
     
  if (digitalRead(P22_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[P22_STATUS_ADDR] = 0x01; // SCROLL ON
  else if (digitalRead(P22_STATUS) == LOW)
     mb_mapping.tab_holding_registers[P22_STATUS_ADDR] = 0x00; // SCROLL OFF
  else
     mb_mapping.tab_holding_registers[P22_STATUS_ADDR] = 0x06; // ERROR SCROLL
     
  if (digitalRead(V23_OPEN_STATUS) == HIGH && digitalRead(V23_CLOSE_STATUS) == LOW)
     mb_mapping.tab_holding_registers[V23_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(V23_CLOSE_STATUS) == HIGH && digitalRead(V23_OPEN_STATUS) == LOW)
     mb_mapping.tab_holding_registers[V23_STATUS_ADDR] = 0x02; // CLOSED VALVE 
  else
     mb_mapping.tab_holding_registers[V23_STATUS_ADDR] = 0x00; // MOVING VALVE

  if (digitalRead(COMPRESS_AIR_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[COMPRESSAIR_STATUS_ADDR] = 0x01; // COMPRESS AIR OFF
  else if (digitalRead(COMPRESS_AIR_STATUS) == LOW)
     mb_mapping.tab_holding_registers[COMPRESSAIR_STATUS_ADDR] = 0x00; // COMPRESS AIR ON 
  else
     mb_mapping.tab_holding_registers[COMPRESSAIR_STATUS_ADDR] = 0x03; // ERROR COMPRESS AIR

  if (digitalRead(VA1_OPEN_STATUS) == HIGH && digitalRead(VA1_CLOSE_STATUS) == LOW)
     mb_mapping.tab_holding_registers[VA1_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(VA1_CLOSE_STATUS) == HIGH && digitalRead(VA1_OPEN_STATUS) == LOW)
     mb_mapping.tab_holding_registers[VA1_STATUS_ADDR] = 0x02; // CLOSED VALVE 
  else
     mb_mapping.tab_holding_registers[VA1_STATUS_ADDR] = 0x00; // MOVING VALVE

  if (digitalRead(VA2_OPEN_STATUS) == HIGH && digitalRead(VA2_CLOSE_STATUS) == LOW)
     mb_mapping.tab_holding_registers[VA2_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(VA2_CLOSE_STATUS) == HIGH && digitalRead(VA2_OPEN_STATUS) == LOW)
     mb_mapping.tab_holding_registers[VA2_STATUS_ADDR] = 0x02; // CLOSED VALVE 
  else
     mb_mapping.tab_holding_registers[VA2_STATUS_ADDR] = 0x00; // MOVING VALVE
     
  /* Update MAXIGAUGE Sensor ON/OFF Status */
  /***********************************************************************************************************/
  for (int j = MG_START_CMD_ADDR; j < MG_END_ADDR; j++) {
    if ( mb_mapping.tab_holding_registers[MG_END_ADDR] == 0x00 && mb_mapping.tab_holding_registers[j] != 0x00 ) {
      /*Serial.print(" channel:");
      Serial.print(j);
      Serial.print(" set new register:");
      Serial.println(mb_mapping.tab_holding_registers[j]);
      */
      //delay(100);
      String cmd;
      int signal = 0;
      if (mb_mapping.tab_holding_registers[j] == 0x01)
        signal = 2;
      if (mb_mapping.tab_holding_registers[j] == 0x02)
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
      mb_mapping.tab_holding_registers[j] = 0x00;  // RESET register
    }
    
  } 
  /***********************************************************************************************************/
  /* Update SCU1600 Turbo START/STOP Status */
  /***********************************************************************************************************
  if ( mb_mapping.tab_holding_registers[SCU_STARTSTOP_ADDR] != 0x00 ) {
      String cmd="";;
      if ( mb_mapping.tab_holding_registers[SCU_STARTSTOP_ADDR] == 0x02 )
        cmd = "\x02\x30\x30\x31\x20\x45\x02\x03\xA8";   // STOP COMMAND
      else if ( mb_mapping.tab_holding_registers[SCU_STARTSTOP_ADDR] == 0x01 )
        cmd = "\x02\x30\x30\x31\x20\x45\x01\x03\xA7";  //START COMMAND
      SCU1600Serial.listen();
      SCU1600Serial.print(cmd);
      Serial.println(cmd);
      mb_mapping.tab_holding_registers[SCU_STARTSTOP_ADDR] = 0x00;  // RESET register
  }
  /***********************************************************************************************************/
  /* Update VARIAN Turbo START/STOP LOWSPEED ON/OFF COMMAND */
  /***********************************************************************************************************
  if ( mb_mapping.tab_holding_registers[VARIAN_STARTSTOP_ADDR] != 0x00 ) {
      String cmd="";;
      if ( mb_mapping.tab_holding_registers[VARIAN_STARTSTOP_ADDR] == 0x02 )
        cmd = "\x02\x80\x30\x30\x30\x31\x30\x03\x42\x32";   // STOP COMMAND
      else if ( mb_mapping.tab_holding_registers[VARIAN_STARTSTOP_ADDR] == 0x01 )
        cmd = "\x02\x80\x30\x30\x30\x31\x31\x03\x42\x33";  //START COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(cmd);
      Serial.println(cmd);
      mb_mapping.tab_holding_registers[VARIAN_STARTSTOP_ADDR] = 0x00;  // RESET register
  }
  if ( mb_mapping.tab_holding_registers[VARIAN_LOWSPEED_ADDR] != 0x00 ) {
      String cmd="";;
      if ( mb_mapping.tab_holding_registers[VARIAN_LOWSPEED_ADDR] == 0x02 )
        cmd =  "\x02\x80\x30\x30\x31\x31\x30\x03\x42\x33";   // LOWSPEED OFF COMMAND
      else if ( mb_mapping.tab_holding_registers[VARIAN_LOWSPEED_ADDR] == 0x01 )
        cmd =  "\x02\x80\x30\x30\x31\x31\x31\x03\x42\x34";  // LOWSPEED ON COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(cmd);
      Serial.println(cmd);
      mb_mapping.tab_holding_registers[VARIAN_LOWSPEED_ADDR] = 0x00;  // RESET register
  }
  /***********************************************************************************************************/
  /* Update DCU (PFEIFFER) Turbo START/STOP STANDBY,ON/OFF COMMAND */
  /***********************************************************************************************************/
  if ( mb_mapping.tab_holding_registers[DCU_END_ADDR] == 0x00 && mb_mapping.tab_holding_registers[DCU_STARTSTOP_ADDR] != 0x00 ) {
      String cmd="";;
      if ( mb_mapping.tab_holding_registers[DCU_STARTSTOP_ADDR] == 0x02 )
        cmd = "0011001006000000009\r";  // STOP PUMPING COMMAND
      else if ( mb_mapping.tab_holding_registers[DCU_STARTSTOP_ADDR] == 0x01 )
        cmd = "0011001006111111015\r";  //START PUMPING COMMAND
      Serial.println(cmd);
      RS485Serial.listen();
      digitalWrite(RS485_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission
      RS485Serial.print(cmd);
      digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Switch to Reception
      mb_mapping.tab_holding_registers[DCU_STARTSTOP_ADDR] = 0x00; // RESET COMMAND
  }
  if ( mb_mapping.tab_holding_registers[DCU_END_ADDR] == 0x00 && mb_mapping.tab_holding_registers[DCU_STANDBY_ADDR] != 0x00 ) {
      String cmd="";;
      if ( mb_mapping.tab_holding_registers[DCU_STANDBY_ADDR] == 0x02 )
        cmd = "0011000206000000010\r";   // STANDBY OFF COMMAND
      else if ( mb_mapping.tab_holding_registers[DCU_STANDBY_ADDR] == 0x01 )
        cmd = "0011000206111111016\r";  // STANDBY ON COMMAND
      RS485Serial.listen();
      digitalWrite(RS485_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission
      RS485Serial.print(cmd);
      Serial.println(cmd);
      digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Switch to Reception
      mb_mapping.tab_holding_registers[DCU_STANDBY_ADDR] = 0x00; // RESET COMMAND
  }
   /***********************************************************************************************************/
  /* Update VARIAN Turbo START/STOP LOWSPEED ON/OFF COMMAND */
  /***********************************************************************************************************
  if ( mb_mapping.tab_holding_registers[VARIAN_STARTSTOP_ADDR] != 0x00 ) {
      String cmd="";;
      if ( mb_mapping.tab_holding_registers[VARIAN_STARTSTOP_ADDR] == 0x02 )
        cmd = "\x02\x80\x30\x30\x30\x31\x30\x03\x42\x32";   // STOP COMMAND
      else if ( mb_mapping.tab_holding_registers[VARIAN_STARTSTOP_ADDR] == 0x01 )
        cmd = "\x02\x80\x30\x30\x30\x31\x31\x03\x42\x33";  //START COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(cmd);
      Serial.println(cmd);
      mb_mapping.tab_holding_registers[VARIAN_STARTSTOP_ADDR] = 0x00;  // RESET register
  }
  if ( mb_mapping.tab_holding_registers[VARIAN_LOWSPEED_ADDR] != 0x00 ) {
      String cmd="";;
      if ( mb_mapping.tab_holding_registers[VARIAN_LOWSPEED_ADDR] == 0x02 )
        cmd =  "\x02\x80\x30\x30\x31\x31\x30\x03\x42\x33";   // LOWSPEED OFF COMMAND
      else if ( mb_mapping.tab_holding_registers[VARIAN_LOWSPEED_ADDR] == 0x01 )
        cmd =  "\x02\x80\x30\x30\x31\x31\x31\x03\x42\x34";  // LOWSPEED ON COMMAND
      VARIANSerial.listen();
      VARIANSerial.print(cmd);
      Serial.println(cmd);
      mb_mapping.tab_holding_registers[VARIAN_LOWSPEED_ADDR] = 0x00;  // RESET register
  }
  */
  /***********************************************************************************************************/
  /* Update XGS (Varian Gauge) ON/OFF COMMAND */
  /***********************************************************************************************************/
  if ( mb_mapping.tab_holding_registers[XGS_END_ADDR] == 0x00 && mb_mapping.tab_holding_registers[XGS_EMULT1_ADDR] != 0x00 ) {
      String cmd="";;
      if ( mb_mapping.tab_holding_registers[XGS_EMULT1_ADDR] == 0x02 )
        cmd = "#0030UHFIG1\r";   // EMULT 1 OFF COMMAND
      else if ( mb_mapping.tab_holding_registers[XGS_EMULT1_ADDR] == 0x01 )
        cmd = "#0031UHFIG1\r";   // EMULT 1 ON COMMAND
      Serial.println(cmd);
      XGSSerial.listen();
      XGSSerial.print(cmd);
      mb_mapping.tab_holding_registers[XGS_EMULT1_ADDR] = 0x00; // RESET COMMAND
  }
  if ( mb_mapping.tab_holding_registers[XGS_END_ADDR] == 0x00 && mb_mapping.tab_holding_registers[XGS_EMULT2_ADDR] != 0x00 ) {
      String cmd="";;
      if ( mb_mapping.tab_holding_registers[XGS_EMULT2_ADDR] == 0x02 )
        cmd = "#0030UHFIG1\r";   // EMULT 2 OFF COMMAND
      else if ( mb_mapping.tab_holding_registers[XGS_EMULT2_ADDR] == 0x01 )
        cmd = "#0033UHFIG1\r";   // EMULT 2 ON COMMAND
      Serial.println(cmd);
      XGSSerial.listen();
      XGSSerial.print(cmd);
      mb_mapping.tab_holding_registers[XGS_EMULT2_ADDR] = 0x00; // RESET COMMAND
  }
  if ( mb_mapping.tab_holding_registers[XGS_END_ADDR] == 0x00 && mb_mapping.tab_holding_registers[XGS_DEGAS_ADDR] != 0x00 ) {
      String cmd="";;
      if ( mb_mapping.tab_holding_registers[XGS_DEGAS_ADDR] == 0x02 )
        cmd = "#0040UHFIG1\r";   // DEGAS OFF COMMAND
      else if ( mb_mapping.tab_holding_registers[XGS_DEGAS_ADDR] == 0x01 )
        cmd = "#0041UHFIG1\r";   // DEGAS ON COMMAND
      Serial.println(cmd);
      XGSSerial.listen();
      XGSSerial.print(cmd);
      mb_mapping.tab_holding_registers[XGS_DEGAS_ADDR] = 0x00; // RESET COMMAND
  }
}
/* Treats errors and flush or close connection if necessary */
static void error_treat(modbus_param_t *mb_param, int code, const char *string)
{
        if (mb_param->error_handling == FLUSH_OR_CONNECT_ON_ERROR) {
                switch (code) {
                case INVALID_DATA:
                case INVALID_CRC:
                case INVALID_EXCEPTION_CODE:
                        modbus_flush(mb_param);
                        break;
                case SELECT_FAILURE:
                case SOCKET_FAILURE:
                case CONNECTION_CLOSED:
                        modbus_close_tcp(mb_param);
                        break;
                }
        }
}

void modbus_flush(modbus_param_t *mb_param)
{
        client.flush();
}

/* Computes the length of the expected response */
static unsigned int compute_response_length(modbus_param_t *mb_param,
                                            uint8_t *query)
{
        int length;
        int offset;

        offset = HEADER_LENGTH_TCP;

        switch (query[offset]) {
        case FC_READ_HOLDING_REGISTERS:
        case FC_READ_EXCEPTION_STATUS:
                length = 3;
                break;
        case FC_REPORT_SLAVE_ID:
                /* The response is device specific (the header provides the
                   length) */
                return MSG_LENGTH_UNDEFINED;
        default:
                length = 5;
        }

        return length + offset + CHECKSUM_LENGTH_TCP;
}

/* Builds a TCP response header */
static int build_response_basis_tcp(sft_t *sft, uint8_t *response)
{
        /* Extract from MODBUS Messaging on TCP/IP Implementation
           Guide V1.0b (page 23/46):
           The transaction identifier is used to associate the future
           response with the request. */
        response[0] = sft->t_id >> 8;
        response[1] = sft->t_id & 0x00ff;

        /* Protocol Modbus */
        response[2] = 0;
        response[3] = 0;

        /* Length to fix later with set_message_length_tcp (4 and 5) */

        response[6] = sft->slave;
        response[7] = sft->function;

        return PRESET_RESPONSE_LENGTH_TCP;
}


/* Sets the length of TCP message in the message (query and response) */
void set_message_length_tcp(modbus_param_t *mb_param, uint8_t *msg, int msg_length)
{
        /* Substract the header length to the message length */
        // mbap_length is the length of a message _without_ the mbap header
        int mbap_length = msg_length - 6;

        /* Set the Lenght field: byte 4 and 5. Other bytes are the same of the request */
        msg[4] = mbap_length >> 8;
        msg[5] = mbap_length & 0x00FF;
}

/* Sends a query/response over a serial or a TCP communication */
static int modbus_send(modbus_param_t *mb_param, uint8_t *query, int query_length)
{
        set_message_length_tcp(mb_param, query, query_length);        
        server.write(query, query_length);        
        return query_length;
}

/* Computes the length of the header following the function code */
static uint8_t compute_query_length_header(int function)
{
        int length;
        if (function <= FC_FORCE_SINGLE_COIL ||
            function == FC_PRESET_SINGLE_REGISTER)
                /* Read and single write */
                length = 4;
        else
                length = 0;

        return length;
}

/* Computes the length of the data to write in the query */
static int compute_query_length_data(modbus_param_t *mb_param, uint8_t *msg)
{
        int function = msg[HEADER_LENGTH_TCP];
        int length;

        if (function == FC_FORCE_MULTIPLE_COILS ||
            function == FC_PRESET_MULTIPLE_REGISTERS)
                length = msg[HEADER_LENGTH_TCP + 5];
        else if (function == FC_REPORT_SLAVE_ID)
                length = msg[HEADER_LENGTH_TCP + 1];
        else
                length = 0;

        length += CHECKSUM_LENGTH_TCP;

        return length;
}

#define WAIT_DATA()                                                                \
{                                                                                  \
      /* I wait for incoming connection, then i go ahead */                        \
      int testDHCP = 1;                                                            \
      do {                                                                         \
          testDHCP = Ethernet.maintain();                                          \
          client = server.available();                                             \
      } while(!client && testDHCP == 0);                                           \
                                                                                   \
}


/* Waits a reply from a modbus slave or a query from a modbus master.
   This function blocks if there is no replies (3 timeouts).

   In
   - msg_length_computed must be set to MSG_LENGTH_UNDEFINED if undefined

   Out
   - msg is an array of uint8_t to receive the message

   On success, return the number of received characters. On error, return
   a negative value.
*/
static int modbus_slave_receive_tcp(modbus_param_t *mb_param, int msg_length_computed, uint8_t *msg)
{
        int select_ret;
        int read_ret;
        int length_to_read;
        uint8_t *p_msg;
        enum { FUNCTION, BYTE_, COMPLETE };
        int state;

        int msg_length = 0;

        if(msg_length_computed == MSG_LENGTH_UNDEFINED) {

                /* The message length is undefined (query receiving) so
                 * we need to analyse the message step by step.
                 * At the first step, we want to reach the function
                 * code (that's why + 1) because all packets have that information. */
                state = FUNCTION;
                msg_length_computed = HEADER_LENGTH_TCP + 1;
        } else {
                state = COMPLETE;
        }

        length_to_read = msg_length_computed;

        select_ret = TRUE;
        WAIT_DATA();

        p_msg = msg;
        while (select_ret) {
                 int testDHCP = Ethernet.maintain();
                 if (testDHCP == 1 || testDHCP == 3) break;
                /* read cycle */
                for(read_ret = 0; read_ret < length_to_read; read_ret++)
                        p_msg[read_ret] = client.read();

                /* Sums bytes received */
                msg_length += read_ret;
                if (msg_length < msg_length_computed) {
                        /* Message incomplete */
                        length_to_read = msg_length_computed - msg_length;
                } else {
                        switch (state) {
                        case FUNCTION:
                                /* Function code position */
                                length_to_read = compute_query_length_header(msg[HEADER_LENGTH_TCP]);
                                msg_length_computed += length_to_read;
                                /* It's useless to check the value of
                                   msg_length_computed in this case (only
                                   defined values are used). */
                                state = BYTE_;
                                break;
                        case BYTE_:
                                length_to_read = compute_query_length_data(mb_param, msg);
                                msg_length_computed += length_to_read;
                                if (msg_length_computed > MAX_ADU_LENGTH_TCP) {
                                     error_treat(mb_param, INVALID_DATA, "Too many data");
                                     return INVALID_DATA;
                                }
                                state = COMPLETE;
                                break;
                        case COMPLETE:
                                length_to_read = 0;
                                break;
                        }
                }
                /* Moves the pointer to receive other data */
                p_msg = &(p_msg[read_ret]);

                if (length_to_read > 0) {

                      WAIT_DATA();
      
              } else {
                        /* All chars are received */
                        select_ret = FALSE;
                }
        }
        /* OK */
        return msg_length;
}


static int response_io_status(int address, int nb,
                              uint8_t *tab_io_status,
                              uint8_t *response, int offset)
{
        int shift = 0;
        int byte = 0;
        int i;

        for (i = address; i < address+nb; i++) {
                byte |= tab_io_status[i] << shift;
                if (shift == 7) {
                        /* Byte is full */
                        response[offset++] = byte;
                        byte = shift = 0;
                } else {
                        shift++;
                }
        }

        if (shift != 0)
                response[offset++] = byte;

        return offset;
}

/* Build the exception response */
static int response_exception(modbus_param_t *mb_param, sft_t *sft,
                              int exception_code, uint8_t *response)
{
        int response_length;

        sft->function = sft->function + 0x80;
        response_length = build_response_basis_tcp(sft, response);

        /* Positive exception code */
        response[response_length++] = -exception_code;

        return response_length;
}

/* Manages the received query.
   Analyses the query and constructs a response.

   If an error occurs, this function construct the response
   accordingly.
*/
void modbus_slave_manage(modbus_param_t *mb_param, const uint8_t *query,
                         int query_length, modbus_mapping_t *mb_mapping)
{
        int offset = HEADER_LENGTH_TCP;
        int slave = query[offset - 1];
        int function = query[offset];
        uint16_t address = (query[offset + 1] << 8) + query[offset + 2];
        uint8_t response[MAX_MESSAGE_LENGTH];
        int resp_length = 0;
        sft_t sft;

        if (slave != mb_param->slave && slave != MODBUS_BROADCAST_ADDRESS) {
            // Ignores the query (not for me)
            return;
        }
        sft.slave = slave;
        sft.function = function;
        sft.t_id = (query[0] << 8) + query[1];

        switch (function) {
              case FC_READ_HOLDING_REGISTERS: {
                      int nb = (query[offset + 3] << 8) + query[offset + 4];
      
                      if ((address + nb) > mb_mapping->nb_holding_registers) {
                              resp_length = response_exception(mb_param, &sft,
                                                               ILLEGAL_DATA_ADDRESS, response);
                      } else {
                              int i;
                              resp_length = build_response_basis_tcp(&sft, response);
                              response[resp_length++] = nb << 1;
                              for (i = address; i < address + nb; i++) {
                                      response[resp_length++] = mb_mapping->tab_holding_registers[i] >> 8;
                                      response[resp_length++] = mb_mapping->tab_holding_registers[i] & 0xFF;
                              }
                      }
              }
                      break;
              case FC_PRESET_SINGLE_REGISTER: {
                      if (address >= mb_mapping->nb_holding_registers) {
                              resp_length = response_exception(mb_param, &sft,
                                                               ILLEGAL_DATA_ADDRESS, response);
                      } else {
                              int data = (query[offset + 3] << 8) + query[offset + 4];
                              mb_mapping->tab_holding_registers[address] = data;
                              memcpy(response, query, query_length);
                              resp_length = query_length;
                      }
              }
                      break;
        }
        modbus_send(mb_param, response, resp_length);
}

/* Initializes the modbus_param_t structure for TCP.
   
   Set the port to MODBUS_TCP_DEFAULT_PORT to use the default one
   (502). It's convenient to use a port number greater than or equal
   to 1024 because it's not necessary to be root to use this port
   number.
*/
void modbus_init_tcp(modbus_param_t *mb_param, const byte *mac, const byte *ip, int port, int slave)
{
        memset(mb_param, 0, sizeof(modbus_param_t));
        strncpy((char *) mb_param->mac, (char *) mac, 6);
        strncpy((char *) mb_param->ip, (char *) ip, 4);
        mb_param->port = port;
        mb_param->error_handling = FLUSH_OR_CONNECT_ON_ERROR;
        mb_param->slave = slave;
}

/* Define the slave number.
   The special value MODBUS_BROADCAST_ADDRESS can be used. */
void modbus_set_slave(modbus_param_t *mb_param, int slave)
{
        mb_param->slave = slave;
}

/* By default, the error handling mode used is FLUSH_OR_CONNECT_ON_ERROR.

   With FLUSH_OR_CONNECT_ON_ERROR, the library will attempt an immediate
   reconnection which may hang for several seconds if the network to
   the remote target unit is down.

   With NOP_ON_ERROR, it is expected that the application will
   check for error returns and deal with them as necessary.
*/
void modbus_set_error_handling(modbus_param_t *mb_param,
                               error_handling_t error_handling)
{
        if (error_handling == FLUSH_OR_CONNECT_ON_ERROR ||
            error_handling == NOP_ON_ERROR) {
                mb_param->error_handling = error_handling;
        } else {
            Serial.println("Invalid setting for error handling (not changed)");
        }
}

/* Closes the network connection and socket in TCP mode */
static void modbus_close_tcp(modbus_param_t *mb_param)
{
      client.stop();
}

/* Allocates 4 arrays to store coils, input status, input registers and
   holding registers. The pointers are stored in modbus_mapping structure.

   Returns 0 on success and -1 on failure.
*/
int modbus_mapping_new(modbus_param_t *mb_param, modbus_mapping_t *mb_mapping,
                       int nb_holding_registers)
{
        /* 4X */
        mb_mapping->nb_holding_registers = nb_holding_registers;
        mb_mapping->tab_holding_registers = (uint16_t *) calloc(nb_holding_registers, sizeof(uint16_t));
        return 0;
}

/* Listens for any query from one or many modbus masters in TCP */
int modbus_slave_listen_tcp(modbus_param_t *mb_param, int nb_connection)
{
          Ethernet.begin(mb_param->mac);
          /* Start listening for clients */
          server.begin();
}

