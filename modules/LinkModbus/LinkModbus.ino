/*
  Modbus Server
 A modbus server to monitor the pumping stations using Leonardo Eth board
 */
#include <ModbusTCPSlave.h> // Modbus2 library
#include <SoftwareSerial.h> // Serial library
/*
 *  This part is the Arduino sketch code
 */ 
// Ethernet config
//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x60, 0x00 };
//const byte ip[] = { 192, 168, 224, 145 }; // Arduino-9 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x60, 0x42 };
//const byte ip[] = { 192, 168, 224, 151 }; // Arduino-11 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5C, 0xDC };
//const byte ip[] = { 192, 168, 224, 146 }; // Arduino-10 has this IP


byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5F, 0xD3 };
IPAddress ip ( 192, 168, 224, 153 ); // Arduino-12 has this IP

// Buffer for Serial purposes
size_t length = 64;
char *buffer = (char*)malloc(sizeof(char) * length); 

// digital relay pins (OUTPUTS)
#define PIN7_RELAY 7  //(Command V1)
#define PIN6_RELAY 6  //(Command V2)
#define PIN5_RELAY 5  //(Command Scroll)
// digital VALVE pins (INTPUTS)
#define PINA0_VALVE  A0
#define PINA1_VALVE  A1
#define PINA2_SCROLL A2
#define PINA3_VALVE  A3 //(Manual valve / No relay command)

#define RS485_TRANSEIVER_STATUS  A4  // RS485 TRANSEIVER STATE

// define SCU1600/TCM RS232 Serial port
SoftwareSerial SCU1600Serial(9, 12); // RX, TX for Leonardo

// define MAXIGAUGE RS232 Serial port
SoftwareSerial MAXIGAUGESerial(11, 13); // RX, TX for Leonardo

// define VARIAN Turbo-V 81-AG serial port
SoftwareSerial VARIANSerial(8, 10); // RX, TX for Leonardo

// Define the ModbusTCPSlave object with port = 502
ModbusTCPSlave modbus(502);

// Modbus addresses limits
#define NB_HOLDING_REGISTERS 70
#define MG_START_ADDR 0
#define MG_END_ADDR 30
#define MG_START_CMD_ADDR 24 //(MAXIGAUGE ON/OFF Command starting point)
#define PIN7_RELAY_ADDR MG_END_ADDR+1 //(V1CMD)
#define PIN6_RELAY_ADDR MG_END_ADDR+2 //(V2CMD) 
#define PIN5_RELAY_ADDR MG_END_ADDR+3 //(SCROLLONOFF Command)
#define PINA0_VALVE_ADDR MG_END_ADDR+4 //(V1ST)
#define PINA1_VALVE_ADDR MG_END_ADDR+5 //(V2ST)
#define PINA2_SCROLL_ADDR MG_END_ADDR+6 //(SCROLLST)
#define PINA3_VALVE_ADDR MG_END_ADDR+7 //(MANUAL V3ST)
//#define DCU_START_ADDR MG_END_ADDR+8
//#define DCU_STARTSTOP_ADDR 47 //(DCU ON/OFF Command)
//#define DCU_STANDBY_ADDR 48  //(DCU STANDBY Command)
//#define DCU_END_ADDR 49
#define SCU_START_ADDR MG_END_ADDR+8
#define SCU_STARTSTOP_ADDR 45 // (SCU ON/OFF Command)
#define SCU_END_ADDR 46
#define VARIAN_START_ADDR SCU_END_ADDR+1
#define VARIAN_STARTSTOP_ADDR 55 //(VARIAN ON/OFF Command)
#define VARIAN_LOWSPEED_ADDR 56  //(VARIAN LOWSPEED Command)
#define VARIAN_END_ADDR 57
#define ARD_RESET_ADDR VARIAN_END_ADDR+1

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

void(*resetArd) (void) = 0; //declare reset function @ address 0

void setup() {
  
  // Open serial communication for Com port.
  Serial.begin(9600);
  
  // Open serial communication for SCU1600.
  SCU1600Serial.begin(9600);

  // Open serial communication for TCM/DCU Turbo
  //SCUTCMSerial.begin(9600);
  
  // Open serial communication for Varian Turbo-V.
  VARIANSerial.begin(9600);
  
  // Open serial communication for MAXIGAUGE
  MAXIGAUGESerial.begin(9600);
 
  // start the Modbus server
  StartModbusServer();

}

void loop() {
  /***********************************************************************************************************/
  // check DHCP lease
  Ethernet.maintain();
  SendReceiveMaster();
  delay(1000);
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
  FloatUint16 mbValue;
  /***********************************************************************************************************/
  /*  Serial MaxiGauge data / Update Modbus register values */
  /***********************************************************************************************************/
  int curaddr = MG_START_ADDR;
  for (int i = 0; i < 7; i++) {
     // Communicate with Master (send/receive data)
     //SendReceiveMaster();
     String serData1,serData2 = "";
     serData1 = "PR" + String(i+1) + "\r";
     if (i==6)
      serData1 = "SEN,0,0,0,0,0,0\r";
     MAXIGAUGESerial.listen();
     MAXIGAUGESerial.print(serData1);
     Serial.println(serData1); 
     serData2 = MAXIGAUGESerial.readString();
     MAXIGAUGESerial.print("\x05");
     serData2 = MAXIGAUGESerial.readString();
     
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
        mbValue.fvalue = prval.toFloat();
        holdingRegisters[curaddr++] = mbValue.value[0]; 
        holdingRegisters[curaddr++] = mbValue.value[1];
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
  /*  Serial DCU1601 data / Update Modbus register values */
  /**********************************************************************************************************
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
    SCUTCMSerial.listen();
    Serial.println(serData5);    
    //Serial3.print(serData5);
    SCUTCMSerial.print(serData5);
    serData6 = SCUTCMSerial.readString();
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
  /*  Serial SCU1600 data / Update Modbus register values */
  /***********************************************************************************************************/   
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
     //Serial.println(serData3); 
     serData4 = SCU1600Serial.readString();
     //Serial.println(serData4); 
     if (serData4 == "") {     // Com Status Error
      for (int j = SCU_START_ADDR; j < SCU_END_ADDR; j++)
        holdingRegisters[j] = 0x00;
      holdingRegisters[SCU_END_ADDR] = 0x01;
     } 
     else {
      holdingRegisters[SCU_END_ADDR] = 0x00;
      typedef union {
       uint32_t uint32value;
       uint16_t value[2];
      } Uint32Uint16;
      Uint32Uint16 mbValue;
      if (i==0) {              // ReadMeas
        serData4 = serData4.substring(21,25); 
        sscanf(serData4.c_str(),"%x",&mbValue.value[0]);
      }
      else if (i==1 || i==2) { // ReadSetPoint + ReadMotorTemp
        serData4 = serData4.substring(7,11); 
        sscanf(serData4.c_str(),"%x",&mbValue.value[0]);
      }
      else if (i==3) {        // ReadCounters
        serData4 = serData4.substring(27,35);
        //Serial.println(serData4);
        sscanf(serData4.c_str(),"%4x%4x",&mbValue.value[1],&mbValue.value[0]);
        mbValue.uint32value/=60; // result is in minutes, convert to hours
        Serial.println(mbValue.uint32value);
      }
      else if (i==4) {        // ReadModFonct
        serData4 = serData4.substring(7,11); 
        sscanf(serData4.c_str(),"%2x%2x",&mbValue.value[0],&mbValue.value[1]);
      }
      if ( i < 3 ) {
       holdingRegisters[curaddr++] = mbValue.value[0];
      }
      else {
       holdingRegisters[curaddr++] = mbValue.value[0];
       holdingRegisters[curaddr++] = mbValue.value[1];
      }
     }
     // Communicate with Master (send/receive data)
     //SendReceiveMaster();
  }
  /***********************************************************************************************************
  /*  Serial VARIAN data / Update Modbus register values */
  /***********************************************************************************************************/   
  curaddr = VARIAN_START_ADDR;
  for (int i = 0; i < 8; i++) {
     // Communicate with Master (send/receive data)
     //SendReceiveMaster();
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
        holdingRegisters[j] = 0x00;
      holdingRegisters[VARIAN_END_ADDR] = 0x01;
      break;
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
  /* Perform some internal logics */
  /***********************************************************************************************************/
  
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
  digitalWrite(PIN7_RELAY,LOW);                                // Set PIN7_RELAY LOW (NORMALLY CLOSED)
  pinMode(PIN7_RELAY, OUTPUT);                                 // sets the digital pin as output for Valve (PIN 7 RELAY)
  holdingRegisters[PIN7_RELAY_ADDR] = 0x00;    // RESET
  digitalWrite(PIN6_RELAY,LOW);                                // Set PIN6_RELAY LOW (NORMALLY  CLOSED)
  pinMode(PIN6_RELAY, OUTPUT);                                 // sets the digital pin as output for Valve (RELAY)
  holdingRegisters[PIN6_RELAY_ADDR] = 0x00;    // RESET
  digitalWrite(PIN5_RELAY,LOW);                                // Set PIN5_RELAY LOW (NORMALLY CLOSED)
  pinMode(PIN5_RELAY, OUTPUT);                                 // sets the digital pin as output for Scroll (RELAY)
  holdingRegisters[PIN5_RELAY_ADDR] = 0x00;    // RESET
  // Digital VALVE INPUTS initialization and assignation
  pinMode(PINA0_VALVE, INPUT);                                 // sets the digital pin as output for Valve V1
  holdingRegisters[PINA0_VALVE_ADDR] = 0x00;   // RESET
  pinMode(PINA1_VALVE, INPUT);                                 // sets the digital pin as input for Valve V2
  holdingRegisters[PINA1_VALVE_ADDR] = 0x00;   // RESET
  pinMode(PINA2_SCROLL, INPUT);                                // sets the digital pin as input for Scroll
  holdingRegisters[PINA2_SCROLL_ADDR] = 0x00;  // RESET
  pinMode(PINA3_VALVE, INPUT);                                // sets the digital pin as input for Valve V3
  holdingRegisters[PINA3_VALVE_ADDR] = 0x00;  // RESET
  /* registers for SCU1600 turbo*/
  for (int i = SCU_START_ADDR; i < SCU_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
  holdingRegisters[SCU_END_ADDR] = 0x01;
  /* registers for VARIAN turbo*/
  for (int i = VARIAN_START_ADDR; i < VARIAN_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
  holdingRegisters[VARIAN_END_ADDR] = 0x01;
  // register for Arduino reset
  holdingRegisters[ARD_RESET_ADDR] = 0x00;     // Arduino Global Reset Status

}
void SendReceiveMaster()
{    
 // Process modbus requests
  modbus.update();
  if (holdingRegisters[10] !=0)
     Serial.println("register 10 changed! ///////////////////////////////////////////////////////////////////////////");
  /***********************************************************************************************************/
  /* Update Valve PIN 7 RELAY position (Open/Close) */
  /***********************************************************************************************************/
  if (holdingRegisters[PIN7_RELAY_ADDR] == 0x02)
     digitalWrite(PIN7_RELAY,HIGH); // OPEN CIRCUIT
  else if (holdingRegisters[PIN7_RELAY_ADDR] == 0x01) 
     digitalWrite(PIN7_RELAY,LOW); // NORMALLY CLOSED
  //holdingRegisters[PIN7_RELAY_ADDR] = 0x00; // Reset register
  
  /***********************************************************************************************************/
  /***********************************************************************************************************/
  /* Update Valve PIN 6 RELAY position (Open/Close) */
  /***********************************************************************************************************/
  if (holdingRegisters[PIN6_RELAY_ADDR] == 0x02)
     digitalWrite(PIN6_RELAY,HIGH); // OPEN CIRCUIT
  else if (holdingRegisters[PIN6_RELAY_ADDR] == 0x01)
     digitalWrite(PIN6_RELAY,LOW); // NORMALLY CLOSED
  //holdingRegisters[PIN6_RELAY_ADDR] = 0x00; // Reset register
 
  /***********************************************************************************************************/
  /***********************************************************************************************************/
  /* Update Valve PIN 05 RELAY position (Open/Close) */
  /***********************************************************************************************************/
  if (holdingRegisters[PIN5_RELAY_ADDR] == 0x02)
     digitalWrite(PIN5_RELAY,HIGH); // OPEN CIRCUIT
  else if (holdingRegisters[PIN5_RELAY_ADDR] == 0x01) 
     digitalWrite(PIN5_RELAY,LOW); // NORMALLY CLOSED
  //holdingRegisters[PIN5_RELAY_ADDR] = 0x00; // Reset register
 
  /***********************************************************************************************************/
  /* Update Valve position STATUS register (Open/Close) */
  /***********************************************************************************************************/
 /* Serial.print("PIN4_VALVE=");Serial.print(digitalRead(PIN4_VALVE));Serial.print(" HIGH=");
  Serial.print(HIGH);Serial.print(" LOW=");Serial.println(LOW);*/
  if (digitalRead(PINA0_VALVE) == HIGH)
     holdingRegisters[PINA0_VALVE_ADDR] = 0x01; // OPENED VALVE
  else
     holdingRegisters[PINA0_VALVE_ADDR] = 0x02; // CLOSED VALVE
  if (digitalRead(PINA1_VALVE) == HIGH)
     holdingRegisters[PINA1_VALVE_ADDR] = 0x01; // OPENED VALVE
  else
     holdingRegisters[PINA1_VALVE_ADDR] = 0x02; // CLOSED VALVE
  if (digitalRead(PINA2_SCROLL) == HIGH)
     holdingRegisters[PINA2_SCROLL_ADDR] = 0x01; // SCROLL ON
  else
     holdingRegisters[PINA2_SCROLL_ADDR] = 0x00; // SCROLL OFF
  if (digitalRead(PINA3_VALVE) == HIGH)
     holdingRegisters[PINA3_VALVE_ADDR] = 0x01; // OPENED VALVE
  else
     holdingRegisters[PINA3_VALVE_ADDR] = 0x02; // CLOSED VALVE 
  /* Update MAXIGAUGE Sensor ON/OFF Status */
  /***********************************************************************************************************/
  for (int j = MG_START_CMD_ADDR; j < MG_END_ADDR; j++) {
    if ( holdingRegisters[j] != 0x00 ) {
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
      MAXIGAUGESerial.listen();
      MAXIGAUGESerial.print(cmd);
      Serial.println(cmd);
      MAXIGAUGESerial.readString();
      MAXIGAUGESerial.print("\x05");
      MAXIGAUGESerial.readString();
      holdingRegisters[j] = 0x00;  // RESET register
    }
  }
  /***********************************************************************************************************/
  /* Update DCU (PFEIFFER) Turbo START/STOP STANDBY,ON/OFF COMMAND */
  /***********************************************************************************************************
  if ( holdingRegisters[DCU_END_ADDR] == 0x00 && holdingRegisters[DCU_STARTSTOP_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[DCU_STARTSTOP_ADDR] == 0x02 )
        cmd = "0011001006000000009\r";  // STOP PUMPING COMMAND
      else if ( holdingRegisters[DCU_STARTSTOP_ADDR] == 0x01 )
        cmd = "0011001006111111015\r";  //START PUMPING COMMAND
      Serial.println(cmd);
      SCUTCMSerial.listen();
      digitalWrite(RS485_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission
      SCUTCMSerial.print(cmd);
      digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Switch to Reception
      holdingRegisters[DCU_STARTSTOP_ADDR] = 0x00; // RESET COMMAND
  }
  if ( holdingRegisters[DCU_END_ADDR] == 0x00 && holdingRegisters[DCU_STANDBY_ADDR] != 0x00 ) {
      String cmd="";;
      if ( holdingRegisters[DCU_STANDBY_ADDR] == 0x02 )
        cmd = "0011000206000000010\r";   // STANDBY OFF COMMAND
      else if ( holdingRegisters[DCU_STANDBY_ADDR] == 0x01 )
        cmd = "0011000206111111016\r";  // STANDBY ON COMMAND
      SCUTCMSerial.listen();
      digitalWrite(RS485_TRANSEIVER_STATUS, HIGH);  // Switch to Transmission
      SCUTCMSerial.print(cmd);
      Serial.println(cmd);
      digitalWrite(RS485_TRANSEIVER_STATUS, LOW);  // Switch to Reception
      holdingRegisters[DCU_STANDBY_ADDR] = 0x00; // RESET COMMAND
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
  /***********************************************************************************************************
  /* Update VARIAN Turbo START/STOP LOWSPEED ON/OFF COMMAND */
  /***********************************************************************************************************/
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
}

