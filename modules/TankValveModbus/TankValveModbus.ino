/*
  Modbus Server
 A modbus server to monitor the pumping stations using Leonardo Eth board
 */
#include <currentLoop.h> // 4-20mA board library
#include <SPI.h>
#include <Ethernet2.h>   // Ethernet library for Leonardo Eth
#include <libmodbusmq.h> // Modbus library

#include <SoftwareSerial.h> // Serial library


/*
 *  This part is the Arduino sketch code
 */ 
// Ethernet config
//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x60, 0x00 };
//const byte ip[] = { 192, 168, 224, 145 }; // Arduino-9 has this IP
//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5C, 0xDC };
//const byte ip[] = { 192, 168, 224, 146 }; // Arduino-10 has this IP


byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5F, 0xD3 };
const byte ip[] = { 192, 168, 229, 101 }; // Arduino-12 has this IP
 
// Modbus objects
#define SLAVE 0x01
modbus_param_t mb_param;     // generic mb params
modbus_mapping_t mb_mapping; // registers

/* Server object will run on Arduino, will be initialized later */
EthernetServer server(MODBUS_TCP_DEFAULT_PORT);

/* Client object connected with Arduino. This initializaton will be reassigned */
EthernetClient client = 0;

// digital relay pins (OUTPUTS)
#define PIN7_RELAY 7  //(Command V1)
#define PIN6_RELAY 6  //(Command V2)
#define PIN5_RELAY 5  //(Command Scroll)
// digital VALVE pins (INTPUTS)
#define PINA0_VALVE A0
#define PINA1_VALVE A1
#define PINA2_SCROLL A2
#define PINA3_VALVE A3 //(Manual valve / No relay command)

// define SCU1600 RS232 Serial port
SoftwareSerial SCU1600Serial(9, 12); // RX, TX for Leonardo

// define MAXIGAUGE RS232 Serial port
SoftwareSerial MAXIGAUGESerial(11, 13); // RX, TX for Leonardo

// define VARIAN Turbo-V 81-AG serial port
SoftwareSerial VARIANSerial(8, 10); // RX, TX for Leonardo

// Modbus addresses limits
#define NB_HOLDING_REGISTERS 60
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
#define SCU_START_ADDR MG_END_ADDR+8
#define SCU_STARTSTOP_ADDR 44 // (SCU ON/OFF Command)
#define SCU_END_ADDR 45
#define VARIAN_START_ADDR SCU_END_ADDR+1
#define VARIAN_STARTSTOP_ADDR 54 //(VARIAN ON/OFF Command)
#define VARIAN_LOWSPEED_ADDR 55  //(VARIAN LOWSPEED Command)
#define VARIAN_END_ADDR 56
#define ARD_RESET_ADDR VARIAN_END_ADDR+1

void(*resetArd) (void) = 0; //declare reset function @ address 0

void setup() {
  
  // Open serial communication for Com port.
  Serial.begin(9600);
  
  // Open serial communication for SCU1600.
  SCU1600Serial.begin(9600);
  
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
  FloatUint16 mbValue;
  /***********************************************************************************************************/
  /*  Serial MaxiGauge data / Update Modbus register values */
  /***********************************************************************************************************/
  int curaddr = MG_START_ADDR;
  for (int i = 0; i < 7; i++) {
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
        mb_mapping.tab_holding_registers[j] = 0x00;
      mb_mapping.tab_holding_registers[MG_END_ADDR] = 0x01;
     }
     else {
      mb_mapping.tab_holding_registers[MG_END_ADDR] = 0x00;
      if (i < 6) { // Sensor pressures & status
        String prval = serData2.substring(serData2.indexOf(",")+1);
        mbValue.fvalue = prval.toFloat();
        mb_mapping.tab_holding_registers[curaddr++] = mbValue.value[0]; 
        mb_mapping.tab_holding_registers[curaddr++] = mbValue.value[1];
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
    // Communicate with Master (send/receive data)
    SendReceiveMaster();
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
  /***********************************************************************************************************/    
  curaddr = VARIAN_START_ADDR;
  for (int i = 0; i < 8; i++) {
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
        mb_mapping.tab_holding_registers[j] = 0x00;
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
     // Communicate with Master (send/receive data)
     SendReceiveMaster();
  }
  /***********************************************************************************************************/
  /* Perform some internal logics */
  /***********************************************************************************************************/
  
}
void StartModbusServer()
{    
  int ret;
  modbus_init_tcp(&mb_param, mac, ip, MODBUS_TCP_DEFAULT_PORT, SLAVE);
  modbus_set_slave(&mb_param, SLAVE);
  modbus_set_error_handling(&mb_param, FLUSH_OR_CONNECT_ON_ERROR);
  //Serial.println(F("Arduino Modbus Slave started"));
  ret = modbus_mapping_new(&mb_param, &mb_mapping, NB_HOLDING_REGISTERS);
  if (ret < 0) {
   // Serial.println(F("): Memory allocation failed, restarting Arduino..."));
  }
  // Assign Modbus reserved register addresses
  // LSB & MSB registers for float pressure values from MaxiGauge
   for (int i = MG_START_ADDR; i < MG_END_ADDR; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
   mb_mapping.tab_holding_registers[MG_END_ADDR] = 0x01;
  // Digital RELAY initialization and assignation
  digitalWrite(PIN7_RELAY,LOW);                                // Set PIN7_RELAY LOW (NORMALLY CLOSED)
  pinMode(PIN7_RELAY, OUTPUT);                                 // sets the digital pin as output for Valve (PIN 7 RELAY)
  mb_mapping.tab_holding_registers[PIN7_RELAY_ADDR] = 0x00;    // RESET
  digitalWrite(PIN6_RELAY,LOW);                                // Set PIN6_RELAY LOW (NORMALLY  CLOSED)
  pinMode(PIN6_RELAY, OUTPUT);                                 // sets the digital pin as output for Valve (RELAY)
  mb_mapping.tab_holding_registers[PIN6_RELAY_ADDR] = 0x00;    // RESET
  digitalWrite(PIN5_RELAY,LOW);                                // Set PIN5_RELAY LOW (NORMALLY CLOSED)
  pinMode(PIN5_RELAY, OUTPUT);                                 // sets the digital pin as output for Scroll (RELAY)
  mb_mapping.tab_holding_registers[PIN5_RELAY_ADDR] = 0x00;    // RESET
  // Digital VALVE INPUTS initialization and assignation
  pinMode(PINA0_VALVE, INPUT);                                 // sets the digital pin as output for Valve V1
  mb_mapping.tab_holding_registers[PINA0_VALVE_ADDR] = 0x00;   // RESET
  pinMode(PINA1_VALVE, INPUT);                                 // sets the digital pin as input for Valve V2
  mb_mapping.tab_holding_registers[PINA1_VALVE_ADDR] = 0x00;   // RESET
  pinMode(PINA2_SCROLL, INPUT);                                // sets the digital pin as input for Scroll
  mb_mapping.tab_holding_registers[PINA2_SCROLL_ADDR] = 0x00;  // RESET
  pinMode(PINA3_VALVE, INPUT);                                // sets the digital pin as input for Valve V3
  mb_mapping.tab_holding_registers[PINA3_VALVE_ADDR] = 0x00;  // RESET
  /* registers for SCU1600 turbo*/
  for (int i = SCU_START_ADDR; i < SCU_END_ADDR; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
  mb_mapping.tab_holding_registers[SCU_END_ADDR] = 0x01;
  /* registers for VARIAN turbo*/
  for (int i = VARIAN_START_ADDR; i < VARIAN_END_ADDR; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
  mb_mapping.tab_holding_registers[VARIAN_END_ADDR] = 0x01;
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
  /* Update Valve PIN 7 RELAY position (Open/Close) */
  /***********************************************************************************************************/
  if (mb_mapping.tab_holding_registers[PIN7_RELAY_ADDR] == 0x02)
     digitalWrite(PIN7_RELAY,HIGH); // OPEN CIRCUIT
  else if (mb_mapping.tab_holding_registers[PIN7_RELAY_ADDR] == 0x01) 
     digitalWrite(PIN7_RELAY,LOW); // NORMALLY CLOSED
  //mb_mapping.tab_holding_registers[PIN7_RELAY_ADDR] = 0x00; // Reset register
  
  /***********************************************************************************************************/
  /***********************************************************************************************************/
  /* Update Valve PIN 6 RELAY position (Open/Close) */
  /***********************************************************************************************************/
  if (mb_mapping.tab_holding_registers[PIN6_RELAY_ADDR] == 0x02)
     digitalWrite(PIN6_RELAY,HIGH); // OPEN CIRCUIT
  else if (mb_mapping.tab_holding_registers[PIN6_RELAY_ADDR] == 0x01)
     digitalWrite(PIN6_RELAY,LOW); // NORMALLY CLOSED
  //mb_mapping.tab_holding_registers[PIN6_RELAY_ADDR] = 0x00; // Reset register
 
  /***********************************************************************************************************/
  /***********************************************************************************************************/
  /* Update Valve PIN 05 RELAY position (Open/Close) */
  /***********************************************************************************************************/
  if (mb_mapping.tab_holding_registers[PIN5_RELAY_ADDR] == 0x02)
     digitalWrite(PIN5_RELAY,HIGH); // OPEN CIRCUIT
  else if (mb_mapping.tab_holding_registers[PIN5_RELAY_ADDR] == 0x01) 
     digitalWrite(PIN5_RELAY,LOW); // NORMALLY CLOSED
  //mb_mapping.tab_holding_registers[PIN5_RELAY_ADDR] = 0x00; // Reset register
 
  /***********************************************************************************************************/
  /* Update Valve position STATUS register (Open/Close) */
  /***********************************************************************************************************/
 /* Serial.print("PIN4_VALVE=");Serial.print(digitalRead(PIN4_VALVE));Serial.print(" HIGH=");
  Serial.print(HIGH);Serial.print(" LOW=");Serial.println(LOW);*/
  if (digitalRead(PINA0_VALVE) == HIGH)
     mb_mapping.tab_holding_registers[PINA0_VALVE_ADDR] = 0x01; // OPENED VALVE
  else
     mb_mapping.tab_holding_registers[PINA0_VALVE_ADDR] = 0x02; // CLOSED VALVE
  if (digitalRead(PINA1_VALVE) == HIGH)
     mb_mapping.tab_holding_registers[PINA1_VALVE_ADDR] = 0x01; // OPENED VALVE
  else
     mb_mapping.tab_holding_registers[PINA1_VALVE_ADDR] = 0x02; // CLOSED VALVE
  if (digitalRead(PINA2_SCROLL) == HIGH)
     mb_mapping.tab_holding_registers[PINA2_SCROLL_ADDR] = 0x01; // SCROLL ON
  else
     mb_mapping.tab_holding_registers[PINA2_SCROLL_ADDR] = 0x00; // SCROLL OFF
  if (digitalRead(PINA3_VALVE) == HIGH)
     mb_mapping.tab_holding_registers[PINA3_VALVE_ADDR] = 0x01; // OPENED VALVE
  else
     mb_mapping.tab_holding_registers[PINA3_VALVE_ADDR] = 0x02; // CLOSED VALVE 
  /* Update MAXIGAUGE Sensor ON/OFF Status */
  /***********************************************************************************************************/
  for (int j = MG_START_CMD_ADDR; j < MG_END_ADDR; j++) {
    if ( mb_mapping.tab_holding_registers[j] != 0x00 ) {
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
      MAXIGAUGESerial.listen();
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
  /***********************************************************************************************************/
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
  /***********************************************************************************************************/
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
            Serial.println(F("Invalid setting for error handling (not changed)"));
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
