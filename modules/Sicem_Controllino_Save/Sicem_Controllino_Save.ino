/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include <SPI.h>
#include <Ethernet.h>   // Ethernet library for Leonardo Eth
#include <libmodbusmq.h> // Modbus library
#include <SoftwareSerial.h> // Serial library
#include <Controllino.h> // Controllino library
#include <MuxShield.h>


/*
 *  This part is the Arduino sketch code
 */ 
// Ethernet config

byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x66, 0xAB };
const byte ip[] = { 192, 168, 224, 162 }; // Controllino-10 has this IP

 
// Modbus objects
#define SLAVE 0x01
modbus_param_t mb_param;     // generic mb params
modbus_mapping_t mb_mapping; // registers

/* Server object will run on Arduino, will be initialized later */
EthernetServer server(MODBUS_TCP_DEFAULT_PORT);

/* Client object connected with Arduino. This initializaton will be reassigned */
EthernetClient client = 0;

//Initialize the Mux Shield
MuxShield muxShield; // Redirection to pins A11,A12,A13

//Arrays to store (MuxShield) resistance 4 status (1=A,2=B,3=C,4=D)
int ModuleChannel[12];
//Arrays to store module setpoint values
float ModuleSetpoint[12];


// relay pins
#define CHANNEL_A_ON CONTROLLINO_R0  // A SWITCH ON COMMAND
#define CHANNEL_A_OFF CONTROLLINO_R1 // A SWITCH OFF COMMAND
#define CHANNEL_B_ON CONTROLLINO_R2  // B SWITCH ON COMMAND
#define CHANNEL_B_OFF CONTROLLINO_R3 // B SWITCH OFF COMMAND
#define CHANNEL_C_ON CONTROLLINO_R4  // C SWITCH ON COMMAND
#define CHANNEL_C_OFF CONTROLLINO_R5 // C SWITCH OFF COMMAND
#define CHANNEL_D_ON CONTROLLINO_R8  // D SWITCH ON COMMAND
#define CHANNEL_D_OFF CONTROLLINO_R9 // D SWITCH OFF COMMAND
// digital pins (INPUTS)
#define CHANNEL_A_LED_STATUS CONTROLLINO_A0  // A LED STATUS
#define CHANNEL_B_LED_STATUS CONTROLLINO_A1  // B LED STATUS
#define CHANNEL_C_LED_STATUS CONTROLLINO_A2  // C LED STATUS
#define CHANNEL_D_LED_STATUS CONTROLLINO_A3  // D LED STATUS
#define CHANNEL_A_ALRM_STATUS CONTROLLINO_A4  // A ALARM STATUS
#define CHANNEL_B_ALRM_STATUS CONTROLLINO_A5  // B ALARM STATUS
#define CHANNEL_C_ALRM_STATUS CONTROLLINO_A6  // C ALARM STATUS
#define CHANNEL_D_ALRM_STATUS CONTROLLINO_A7  // D ALARM STATUS
#define SICEM_ALRM_STATUS CONTROLLINO_A8  // SICEM GLOBAL ALARM STATUS

#define MAXIGAUGESerialIsSoftware
#define SICEMSerial  Serial2 // Serial2 is hardware serial for Controllino MAXI (RXD2, TXD2)
SoftwareSerial MAXIGAUGESerial(CONTROLLINO_D8, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_12); // RX, TX for Controllino MAXI (D8,D12)

// Modbus addresses limits
#define NB_HOLDING_REGISTERS 150
#define MG_START_ADDR 0
#define MG_START_CMD_ADDR 24 //(MAXIGAUGE ON/OFF Command starting point)
#define MG_END_ADDR 30
#define CHANNEL_A_ONOFF_CMD_ADDR MG_END_ADDR+1 //(CHANNEL_A_CMD)
#define CHANNEL_B_ONOFF_CMD_ADDR MG_END_ADDR+2 //(CHANNEL_B_CMD)
#define CHANNEL_C_ONOFF_CMD_ADDR MG_END_ADDR+3 //(CHANNEL_C_CMD)
#define CHANNEL_D_ONOFF_CMD_ADDR MG_END_ADDR+4 //(CHANNEL_D_CMD)
#define CHANNEL_A_ONOFF_STATUS_ADDR  MG_END_ADDR+5 //(A_LED_STATUS)
#define CHANNEL_B_ONOFF_STATUS_ADDR  MG_END_ADDR+6 //(B_LED_STATUS)
#define CHANNEL_C_ONOFF_STATUS_ADDR  MG_END_ADDR+7 //(C_LED_STATUS)
#define CHANNEL_D_ONOFF_STATUS_ADDR  MG_END_ADDR+8 //(D_LED_STATUS)
#define CHANNEL_A_ALRM_STATUS_ADDR  MG_END_ADDR+9 //(A_ALRM_STATUS)
#define CHANNEL_B_ALRM_STATUS_ADDR  MG_END_ADDR+10 //(B_ALRM_STATUS)
#define CHANNEL_C_ALRM_STATUS_ADDR  MG_END_ADDR+11 //(C_ALRM_STATUS)
#define CHANNEL_D_ALRM_STATUS_ADDR  MG_END_ADDR+12 //(D_ALRM_STATUS)
#define SICEM_ALRM_STATUS_ADDR  MG_END_ADDR+13 //(SICEM_ALRM_STATUS)
#define MODULE_CHANNEL_VALUE_START_ADDR  SICEM_ALRM_STATUS_ADDR+1 //(MODULE_CHANNEL_VALUE START)
#define MODULE_CHANNEL_VALUE_END_ADDR  MODULE_CHANNEL_VALUE_START_ADDR+11 //(MODULE_CHANNEL_VALUE END)
#define MODULE_TEMP_START_ADDR MODULE_CHANNEL_VALUE_END_ADDR+1 //(SICEM TEMPERATURE START)
#define MODULE_TEMP_END_ADDR  MODULE_TEMP_START_ADDR+23 //(SICEM TEMPERATURE END)
#define MODULE_SETPOINT_START_ADDR MODULE_TEMP_END_ADDR+1 //(SICEM SETPOINT START)
#define MODULE_SETPOINT_END_ADDR  MODULE_SETPOINT_START_ADDR+23 //(SICEM SETPOINT END)
#define SICEM_END_ADDR MODULE_SETPOINT_END_ADDR+1
#define ARD_RESET_ADDR SICEM_END_ADDR+1

void(*resetArd) (void) = 0; //declare reset function @ address 0

// Buffer for Serial purposes
size_t length = 64;
char *buffer = (char*)malloc(sizeof(char) * length); 
    
void setup() {

  // Setting Mux 3 pin rows to digital_in mode
  muxShield.setMode(1,DIGITAL_IN);
  muxShield.setMode(2,DIGITAL_IN);
  muxShield.setMode(3,DIGITAL_IN);
    
  // Open serial communication for Com port.
  Serial.begin(9600);

  // Open serial communication for MAXIGAUGE
  MAXIGAUGESerial.begin(9600);
  MAXIGAUGESerial.setTimeout(1000);

  // Open serial communication for SICEM
  SICEMSerial.begin(4800);
  SICEMSerial.setTimeout(1000);
 
  StartModbusServer();

}
 
void loop() {
  Serial.println("loop started");
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
  int curaddr = MG_START_ADDR;
  /***********************************************************************************************************/
  /*  Serial MaxiGauge data / Update Modbus register values */
  /***********************************************************************************************************/
  
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
  /* CHANNEL LED STATUS data (GREEN/RED) */
  /***********************************************************************************************************/
  //Serial.print("CHANNEL_A_LED_STATUS=");Serial.println(digitalRead(CHANNEL_A_LED_STATUS));
  if (digitalRead(CHANNEL_A_LED_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[CHANNEL_A_ONOFF_STATUS_ADDR] = 0x01; // GREEN STATUS
  else
     mb_mapping.tab_holding_registers[CHANNEL_A_ONOFF_STATUS_ADDR] = 0x02; // RED STATUS
  if (digitalRead(CHANNEL_B_LED_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[CHANNEL_B_ONOFF_STATUS_ADDR] = 0x01; // GREEN STATUS
  else
     mb_mapping.tab_holding_registers[CHANNEL_B_ONOFF_STATUS_ADDR] = 0x02; // RED STATUS
  if (digitalRead(CHANNEL_C_LED_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[CHANNEL_C_ONOFF_STATUS_ADDR] = 0x01; // GREEN STATUS
  else
     mb_mapping.tab_holding_registers[CHANNEL_C_ONOFF_STATUS_ADDR] = 0x02; // RED STATUS
  if (digitalRead(CHANNEL_D_LED_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[CHANNEL_D_ONOFF_STATUS_ADDR] = 0x01; // GREEN STATUS
  else 
     mb_mapping.tab_holding_registers[CHANNEL_D_ONOFF_STATUS_ADDR] = 0x02; // RED STATUS
  /***********************************************************************************************************/
  /* CHANNEL ALARM STATUS data (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(CHANNEL_A_ALRM_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[CHANNEL_A_ALRM_STATUS_ADDR] = 0x01; // ALARM ON STATUS
  else 
     mb_mapping.tab_holding_registers[CHANNEL_A_ALRM_STATUS_ADDR] = 0x00; // ALARM OFF STATUS
  if (digitalRead(CHANNEL_B_ALRM_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[CHANNEL_B_ALRM_STATUS_ADDR] = 0x01; // ALARM ON STATUS
  else 
     mb_mapping.tab_holding_registers[CHANNEL_B_ALRM_STATUS_ADDR] = 0x00; // ALARM OFF STATUS
  if (digitalRead(CHANNEL_C_ALRM_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[CHANNEL_C_ALRM_STATUS_ADDR] = 0x01; // ALARM ON STATUS
  else 
     mb_mapping.tab_holding_registers[CHANNEL_C_ALRM_STATUS_ADDR] = 0x00; // ALARM OFF STATUS
  if (digitalRead(CHANNEL_D_ALRM_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[CHANNEL_D_ALRM_STATUS_ADDR] = 0x01; // ALARM ON STATUS
  else 
     mb_mapping.tab_holding_registers[CHANNEL_D_ALRM_STATUS_ADDR] = 0x00; // ALARM OFF STATUS
  /***********************************************************************************************************/
  /* SICEM ALARM STATUS data (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(SICEM_ALRM_STATUS) == HIGH)
     mb_mapping.tab_holding_registers[SICEM_ALRM_STATUS_ADDR] = 0x01; // SICEM ALARM ON STATUS
  else  
     mb_mapping.tab_holding_registers[SICEM_ALRM_STATUS_ADDR] = 0x00; // SICEM ALARM OFF STATUS
  
  /***********************************************************************************************************/
  /* Module Channel value data (1,2,3,4) */
  /***********************************************************************************************************/
  curaddr = MODULE_CHANNEL_VALUE_START_ADDR;
  // Reset values
  for (int i = 0 ; i < 12; i++) {
      ModuleChannel[i] = 0;
      mb_mapping.tab_holding_registers[curaddr++] = 0;
  }
  curaddr = MODULE_CHANNEL_VALUE_START_ADDR;
  for (int i = 1 ; i <= 16 ; i++) {
    int module = 0;
    if ( i <= 4 ) 
      module = 1;
    else if ( i <= 8 ) 
      module = 2;
    else if ( i <= 12 ) 
      module = 3;
    else if ( i <= 16 ) 
      module = 4;
    int pos = i%4;
    if (pos == 0) pos = 4;
    //Serial.print("module[");Serial.print(module);Serial.print("][");Serial.print(pos);Serial.print("]=");Serial.println(muxShield.digitalReadMS(1,i-1));
    if ( muxShield.digitalReadMS(1,i-1) == HIGH ) {
      ModuleChannel[module] = pos;
      mb_mapping.tab_holding_registers[curaddr++] = ModuleChannel[module];
    }
  }
  for (int i = 1 ; i <= 16 ; i++) {
    int module = 0;
    if ( i <= 4 ) 
      module = 5;
    else if ( i <= 8 ) 
      module = 6;
    else if ( i <= 12 ) 
      module = 7;
    else if ( i <= 16 ) 
      module = 8;
    int pos = i%4;
    if (pos == 0) pos = 4;
    if ( muxShield.digitalReadMS(2,i-1) == HIGH ) {
      ModuleChannel[module] = pos;
      mb_mapping.tab_holding_registers[curaddr++] = ModuleChannel[module];
    }
  }
  for (int i = 1 ; i <= 16 ; i++) {
    int module = 0;
    if ( i <= 4 ) 
      module = 9;
    else if ( i <= 8 ) 
      module = 10;
    else if ( i <= 12 ) 
      module = 11;
    else if ( i <= 16 ) 
      module = 12;
    int pos = i%4;
    if (pos == 0) pos = 4;
    if ( muxShield.digitalReadMS(3,i-1) == HIGH ) {
      ModuleChannel[module] = pos;
      mb_mapping.tab_holding_registers[curaddr++] = ModuleChannel[module];
    }
  }
  /***********************************************************************************************************/
  /*  Serial SICEM data / Update Modbus register values */
  /**********************************************************************************************************/
  const char * temp[] = { "s11","s12","s13","s14","s15","s16","s17","s18","s19","s1A","s1B","s1C"};
  const char * setp[] = { "i11","i12","i13","i14","i15","i16","i17","i18","i19","i1A","i1B","i1C"};
  
  curaddr = MODULE_TEMP_START_ADDR;
  for (int i = 1 ; i <= 24 ; i++) {
    // Communicate with Master (send/receive data)
    SendReceiveMaster();
    String serData3,serData4 = "";
    unsigned char data[64];
    data[0] = 0x02; // STX
    int csum = data[0];
    int pos = 0;
    if ( i <= 12 ) {
     pos = i;
     for (int k=0;k<3;k++) {
      data[k+1] = temp[pos-1][k]; // TEMP
      csum+=data[k+1];
     }
    }
    else {      
      pos = i - 12;
      for (int k=0;k<3;k++) {
      data[k+1] = setp[pos-1][k]; // SETPOINT
      csum+=data[k+1];
     }
    }
    data[4] = 0x03; //ETX
    csum+=data[4];
    Serial.print("csum=");Serial.println(csum);
    data[5] = ((csum & 255) | 128);
    data[6] = '\0';
    serData3 = (char*)data;
    
#if defined(SICEMSerialIsSoftware)
     SICEMSerial.listen();
#endif
    Serial.println(serData3);    
    SICEMSerial.print(serData3);
    //delay(100);
    memset(buffer,0,length);
    SICEMSerial.readBytes(buffer,length);
    serData4 = buffer;
    SICEMSerial.flush();
    Serial.print("Sicem resp=");Serial.print(serData4);Serial.println("END");
    if (serData4 == "") {// Com Status Error: treat only as if module is absent!
      Serial.println("SETTING REGISTER AT SICEM_END_ADDR to 0x01");
      mb_mapping.tab_holding_registers[curaddr++] = 0x00;
      mb_mapping.tab_holding_registers[curaddr++] = 0x00;
      if (i>12) ModuleSetpoint[pos-1] = 0; // reset setpoint locally
      continue;
    }
    else { // Data OK
      Serial.print("Data OK: Sicem resp=");Serial.print(serData4);Serial.println("END");
      mb_mapping.tab_holding_registers[SICEM_END_ADDR] = 0x00;
      csum = 0; // Calculate checksum
      for (int k = 0 ; k < serData4.length() - 1; k++)
        csum+= serData4[k];
      if ( ((serData4[serData4.length() -1] & 255) | 128) == ((csum & 255) | 128) ) { // checksum OK
        if ( i <= 12 & serData4.length() > 11) { // Temperature case
           String val = serData4.substring(4,10);
           int32_t decvalue;
           decvalue = strtol(val.c_str(),NULL,16);
           float temp = double(decvalue) / 4096.;
           mbfValue.fvalue = temp;
           Serial.println(val);Serial.println(decvalue);Serial.println(temp);
           mb_mapping.tab_holding_registers[curaddr++] = mbfValue.value[0]; 
           mb_mapping.tab_holding_registers[curaddr++] = mbfValue.value[1];
        }
        else if ( i > 12 & serData4.length() > 8 ){ // Setpoint case
           String val = serData4.substring(4,7);
           int32_t decvalue;
           decvalue = strtol(val.c_str(),NULL,16);
           float setpoint = double(decvalue);
           ModuleSetpoint[pos-1] = setpoint; // Save setpoint locally
           mbfValue.fvalue = setpoint;
           Serial.println(val);Serial.println(decvalue);Serial.println(setpoint);
           mb_mapping.tab_holding_registers[curaddr++] = mbfValue.value[0]; 
           mb_mapping.tab_holding_registers[curaddr++] = mbfValue.value[1];
        }
      }
      else  { // checksum error pass
        curaddr+=2;
      }
        
    }
  }
  delay(100);
  
  Serial.println("loop finished");
}
int SendSicemRequest(String request,String *answer) {
   unsigned char data[64];
   String unformattedData="";;
   String msg="";
   data[0] = 0x02; // STX
   int csum = data[0];
   for (int i = 0; i < (int) request.length(); i++) {
      data[i+1] = request[i];
      csum+=data[i+1];
   }
   data[(int)request.length()+1] = 0x03; //ETX
   csum+=3;
   data[(int)request.length()+2] = ((csum & 255) | 128);
   data[(int)request.length()+3] = '\0';
   msg = (char*)data;
#if defined(SICEMSerialIsSoftware)
     SICEMSerial.listen();
#endif
   Serial.println(msg);    
   SICEMSerial.print(msg);
   memset(buffer,0,length);
   SICEMSerial.readBytes(buffer,length);
   unformattedData = buffer;
   SICEMSerial.flush();
   Serial.print("Sicem Request resp=");Serial.print(unformattedData);Serial.println("END");
   csum = 0;
   for (int i = 0; i < (int)unformattedData.length() - 1 ; i++)  {
     csum+=unformattedData[i];
   }
   if (((unformattedData[unformattedData.length() -1] & 255) | 128) != ((csum & 255) | 128))  {
     Serial.println("Wrong Checksum");
     return (-1); 
    }
   *answer = unformattedData; 
   return 0;
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
   // single registers for channel cmd & status led
   mb_mapping.tab_holding_registers[CHANNEL_A_ONOFF_CMD_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_B_ONOFF_CMD_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_C_ONOFF_CMD_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_D_ONOFF_CMD_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_A_ONOFF_STATUS_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_B_ONOFF_STATUS_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_C_ONOFF_STATUS_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_D_ONOFF_STATUS_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_A_ALRM_STATUS_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_B_ALRM_STATUS_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_C_ALRM_STATUS_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[CHANNEL_D_ALRM_STATUS_ADDR] = 0x00;
   mb_mapping.tab_holding_registers[SICEM_ALRM_STATUS_ADDR] = 0x00;
   // single registers for module channel values from rack
   for (int i = MODULE_CHANNEL_VALUE_START_ADDR; i < MODULE_CHANNEL_VALUE_END_ADDR; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
   // LSB & MSB registers for float temperature from Sicem modules
   for (int i = MODULE_TEMP_START_ADDR; i < MODULE_TEMP_END_ADDR; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
   // LSB & MSB registers for setpoint values from Sicem modules
   for (int i = MODULE_SETPOINT_START_ADDR; i < MODULE_SETPOINT_END_ADDR; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
   // Internal setpoint values
   for (int i = 0 ; i < 12; i++) {
      ModuleSetpoint[i] = 0;
      ModuleChannel[i] = 0;
   }
      
  // Digital RELAY initialization and assignation
  digitalWrite(CHANNEL_A_ON,LOW);                             // Set CHANNEL_A_ON LOW
  digitalWrite(CHANNEL_A_OFF,HIGH);                           // Set CHANNEL_A_OFF HIGH
  pinMode(CHANNEL_A_ON, OUTPUT);                              // sets the digital pin as output for CHANNEL_A_ON
  pinMode(CHANNEL_A_OFF, OUTPUT);                             // sets the digital pin as output for CHANNEL_A_OFF
  mb_mapping.tab_holding_registers[CHANNEL_A_ONOFF_CMD_ADDR] = 0x00; // RESET
  digitalWrite(CHANNEL_B_ON,LOW);                             // Set CHANNEL_B_ON LOW
  digitalWrite(CHANNEL_B_OFF,HIGH);                           // Set CHANNEL_B_OFF HIGH
  pinMode(CHANNEL_B_ON, OUTPUT);                              // sets the digital pin as output for CHANNEL_B_ON
  pinMode(CHANNEL_B_OFF, OUTPUT);                             // sets the digital pin as output for CHANNEL_B_OFF
  mb_mapping.tab_holding_registers[CHANNEL_B_ONOFF_CMD_ADDR] = 0x00; // RESET
  digitalWrite(CHANNEL_C_ON,LOW);                             // Set CHANNEL_C_ON LOW
  digitalWrite(CHANNEL_C_OFF,HIGH);                           // Set CHANNEL_C_OFF HIGH
  pinMode(CHANNEL_C_ON, OUTPUT);                              // sets the digital pin as output for CHANNEL_C_ON
  pinMode(CHANNEL_C_OFF, OUTPUT);                             // sets the digital pin as output for CHANNEL_C_OFF
  mb_mapping.tab_holding_registers[CHANNEL_C_ONOFF_CMD_ADDR] = 0x00; // RESET
  digitalWrite(CHANNEL_D_ON,LOW);                             // Set CHANNEL_D_ON LOW
  digitalWrite(CHANNEL_D_OFF,HIGH);                           // Set CHANNEL_D_OFF HIGH
  pinMode(CHANNEL_D_ON, OUTPUT);                              // sets the digital pin as output for CHANNEL_D_ON
  pinMode(CHANNEL_D_OFF, OUTPUT);                             // sets the digital pin as output for CHANNEL_D_OFF
  mb_mapping.tab_holding_registers[CHANNEL_D_ONOFF_CMD_ADDR] = 0x00; // RESET
  // Digital VALVE INPUTS initialization and assignation
  pinMode(CHANNEL_A_LED_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_A_LED_STATUS
  mb_mapping.tab_holding_registers[CHANNEL_A_ONOFF_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_B_LED_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_B_LED_STATUS
  mb_mapping.tab_holding_registers[CHANNEL_B_ONOFF_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_C_LED_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_C_LED_STATUS
  mb_mapping.tab_holding_registers[CHANNEL_C_ONOFF_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_D_LED_STATUS, INPUT);                          // sets the digital pin as input for CHANNEL_D_LED_STATUS 
  mb_mapping.tab_holding_registers[CHANNEL_D_ONOFF_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_A_ALRM_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_A_ALRM_STATUS
  mb_mapping.tab_holding_registers[CHANNEL_A_ALRM_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_B_ALRM_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_B_ALRM_STATUS
  mb_mapping.tab_holding_registers[CHANNEL_B_ALRM_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_C_ALRM_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_C_ALRM_STATUS
  mb_mapping.tab_holding_registers[CHANNEL_C_ALRM_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_D_ALRM_STATUS, INPUT);                          // sets the digital pin as input for CHANNEL_D_ALRM_STATUS 
  mb_mapping.tab_holding_registers[CHANNEL_D_ALRM_STATUS_ADDR] = 0x00; // RESET
  pinMode(SICEM_ALRM_STATUS, INPUT);                              // sets the digital pin as input for SICEM_ALRM_STATUS 
  mb_mapping.tab_holding_registers[SICEM_ALRM_STATUS_ADDR] = 0x00;// RESET

  // rack comm status
  mb_mapping.tab_holding_registers[SICEM_END_ADDR] = 0x01;
   
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
  typedef union {
     float fvalue;
     uint16_t value[2];
  } FloatUint16;
  FloatUint16 mbfValue;
  /***********************************************************************************************************/
  /* Update CHANNEL_A Position (ON/OFF) */
  /***********************************************************************************************************/
  if (mb_mapping.tab_holding_registers[CHANNEL_A_ONOFF_CMD_ADDR] == 0x02) {
     digitalWrite(CHANNEL_A_ON,LOW); // SWITCH OFF Channel
     delay(1000);
     digitalWrite(CHANNEL_A_OFF,HIGH); // SWITCH OFF Channel
  }
  else if (mb_mapping.tab_holding_registers[CHANNEL_A_ONOFF_CMD_ADDR] == 0x01) {
     digitalWrite(CHANNEL_A_ON,HIGH); // SWITCH ON Channel
     delay(1000);
     digitalWrite(CHANNEL_A_OFF,LOW); // SWITCH ON Channel
  }
  mb_mapping.tab_holding_registers[CHANNEL_A_ONOFF_CMD_ADDR] = 0x00; // Reset register
  /***********************************************************************************************************/
  /* Update CHANNEL_B Position (ON/OFF) */
  /***********************************************************************************************************/
  if (mb_mapping.tab_holding_registers[CHANNEL_B_ONOFF_CMD_ADDR] == 0x02) {
     digitalWrite(CHANNEL_B_ON,LOW); // SWITCH OFF Channel
     delay(1000);
     digitalWrite(CHANNEL_B_OFF,HIGH); // SWITCH OFF Channel
  }
  else if (mb_mapping.tab_holding_registers[CHANNEL_B_ONOFF_CMD_ADDR] == 0x01) {
     digitalWrite(CHANNEL_B_ON,HIGH); // SWITCH ON Channel
     delay(1000);
     digitalWrite(CHANNEL_B_OFF,LOW); // SWITCH ON Channel
  }
  mb_mapping.tab_holding_registers[CHANNEL_B_ONOFF_CMD_ADDR] = 0x00; // Reset register
  /***********************************************************************************************************/
  /* Update CHANNEL_C Position (ON/OFF) */
  /***********************************************************************************************************/
  if (mb_mapping.tab_holding_registers[CHANNEL_C_ONOFF_CMD_ADDR] == 0x02) {
     digitalWrite(CHANNEL_C_ON,LOW); // SWITCH OFF Channel
     delay(1000);
     digitalWrite(CHANNEL_C_OFF,HIGH); // SWITCH OFF Channel
  }
  else if (mb_mapping.tab_holding_registers[CHANNEL_C_ONOFF_CMD_ADDR] == 0x01) {
     digitalWrite(CHANNEL_C_ON,HIGH); // SWITCH ON Channel
     delay(1000);
     digitalWrite(CHANNEL_C_OFF,LOW); // SWITCH ON Channel
  }
  mb_mapping.tab_holding_registers[CHANNEL_C_ONOFF_CMD_ADDR] = 0x00; // Reset register
  /***********************************************************************************************************/
  /* Update CHANNEL_D Position (ON/OFF) */
  /***********************************************************************************************************/
  if (mb_mapping.tab_holding_registers[CHANNEL_D_ONOFF_CMD_ADDR] == 0x02) {
     digitalWrite(CHANNEL_D_ON,LOW); // SWITCH OFF Channel
     delay(1000);
     digitalWrite(CHANNEL_D_OFF,HIGH); // SWITCH OFF Channel
  }
  else if (mb_mapping.tab_holding_registers[CHANNEL_D_ONOFF_CMD_ADDR] == 0x01) {
     digitalWrite(CHANNEL_D_ON,HIGH); // SWITCH ON Channel
     delay(1000);
     digitalWrite(CHANNEL_D_OFF,LOW); // SWITCH ON Channel
  }
  mb_mapping.tab_holding_registers[CHANNEL_D_ONOFF_CMD_ADDR] = 0x00; // Reset register
 
  /***********************************************************************************************************/
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
  /* Update SICEM SETPOINT */
  /***********************************************************************************************************/
  int curaddr = MODULE_SETPOINT_START_ADDR;
  for (int j = 0; j < 12; j++) {
    float setpoint = ModuleSetpoint[j];
    mbfValue.value[0] = mb_mapping.tab_holding_registers[curaddr++]; 
    mbfValue.value[1] = mb_mapping.tab_holding_registers[curaddr++];
    Serial.print("ModuleSetPoint[");Serial.print(j);Serial.print("]=");Serial.print(ModuleSetpoint[j]);Serial.print("  RegisterSetPoint=");Serial.println(mbfValue.fvalue);
    if ( mbfValue.fvalue != setpoint) { // Update setpoint
      String takecmd,changesetpoint,releasecmd,answer;
      int module = j+1;
      // Take command priority
      String modulename = String(module,HEX);
      modulename.toUpperCase();
      takecmd = "h1"+ modulename + "01";
      if  (SendSicemRequest(takecmd,&answer) == -1)
        continue;
      // Do the setpoint change
      String hexSetpointVal = String((int)mbfValue.fvalue,HEX); // New setpoint is an int
      switch ((int)hexSetpointVal.length()) {
      case 1:
        hexSetpointVal = "00" + hexSetpointVal;
        break;
      case 2:
         hexSetpointVal = "0" + hexSetpointVal;
         break;
      default:
         break;
      }
      String modulenamevalue = String(module,HEX) + hexSetpointVal;
      modulenamevalue.toUpperCase();
      changesetpoint = "i1" + modulenamevalue + "2";
      if  (SendSicemRequest(changesetpoint,&answer) == -1)
        continue;
      // Update locally
      ModuleSetpoint[j] = (int)mbfValue.fvalue;
      // Release command priority
      releasecmd = "h1"+ modulename + "00";
      if  (SendSicemRequest(releasecmd,&answer) == -1)
        continue;
    }
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
        case FC_READ_COIL_STATUS:
        case FC_READ_INPUT_STATUS: {
                /* Header + nb values (code from force_multiple_coils) */
                int nb = (query[offset + 3] << 8) | query[offset + 4];
                length = 2 + (nb / 8) + ((nb % 8) ? 1 : 0);
        }
                break;
        case FC_READ_HOLDING_REGISTERS:
        case FC_READ_INPUT_REGISTERS:
                /* Header + 2 * nb values */
                length = 2 + 2 * (query[offset + 3] << 8 |
                                       query[offset + 4]);
                break;
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
        else if (function == FC_FORCE_MULTIPLE_COILS ||
                 function == FC_PRESET_MULTIPLE_REGISTERS)
                /* Multiple write */
                length = 5;
        else if (function == FC_REPORT_SLAVE_ID)
                length = 1;
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
              case FC_READ_COIL_STATUS: {
                          
                      int nb = (query[offset + 3] << 8) + query[offset + 4];
      
                      if ((address + nb) > mb_mapping->nb_coil_status) {
                            
                              resp_length = response_exception(mb_param, &sft,
                                                               ILLEGAL_DATA_ADDRESS, response);
                      } else {
                            
                              
                              resp_length = build_response_basis_tcp(&sft, response);
                              response[resp_length++] = (nb / 8) + ((nb % 8) ? 1 : 0);
                              resp_length = response_io_status(address, nb,
                                                               mb_mapping->tab_coil_status,
                                                               response, resp_length);
                      }
              }
                      break;
              case FC_READ_INPUT_STATUS: {
                            
                      /* Similar to coil status (but too much arguments to use a
                       * function) */
                      int nb = (query[offset + 3] << 8) + query[offset + 4];
      
                      if ((address + nb) > mb_mapping->nb_input_status) {
                             
                                     
                              resp_length = response_exception(mb_param, &sft,
                                                               ILLEGAL_DATA_ADDRESS, response);
                      } else {
                            
                              
                              resp_length = build_response_basis_tcp(&sft, response);
                              response[resp_length++] = (nb / 8) + ((nb % 8) ? 1 : 0);
                              resp_length = response_io_status(address, nb,
                                                               mb_mapping->tab_input_status,
                                                               response, resp_length);
                      }
              }
                      break;
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
              case FC_READ_INPUT_REGISTERS: {
                             
                      /* Similar to holding registers (but too much arguments to use a
                       * function) */
                      int nb = (query[offset + 3] << 8) + query[offset + 4];
      
                      if ((address + nb) > mb_mapping->nb_input_registers) {
                            
                              resp_length = response_exception(mb_param, &sft,
                                                               ILLEGAL_DATA_ADDRESS, response);
                      } else {
                              int i;
      
                             
                              
                              resp_length = build_response_basis_tcp(&sft, response);
                              response[resp_length++] = nb << 1;
                              for (i = address; i < address + nb; i++) {
                                      response[resp_length++] = mb_mapping->tab_input_registers[i] >> 8;
                                      response[resp_length++] = mb_mapping->tab_input_registers[i] & 0xFF;
                              }
                      }
              }
                      break;
              case FC_FORCE_SINGLE_COIL: {
                             
                      if (address >= mb_mapping->nb_coil_status) {
                            
                              resp_length = response_exception(mb_param, &sft,
                                                               ILLEGAL_DATA_ADDRESS, response);
                      } else {
                              int data = (query[offset + 3] << 8) + query[offset + 4];
      
                              if(data == 0xFF00 || data == 0x0) {
                                          
                                 
                                      
                                      mb_mapping->tab_coil_status[address] = (data) ? ON : OFF;
      
                                      /* In RTU mode, the CRC is computed and added
                                         to the query by modbus_send, the computed
                                         CRC will be same and optimisation is
                                         possible here (FIXME). */
                                      memcpy(response, query, query_length);
                                      resp_length = query_length;
                              } else {
                                     
                                      resp_length = response_exception(mb_param, &sft,
                                                                       ILLEGAL_DATA_VALUE, response);
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
              case FC_FORCE_MULTIPLE_COILS: {

                      int nb = (query[offset + 3] << 8) + query[offset + 4];
      
                      if ((address + nb) > mb_mapping->nb_coil_status) {
                           
                              resp_length = response_exception(mb_param, &sft,
                                                               ILLEGAL_DATA_ADDRESS, response);
                      } else {
                              /* 6 = byte count */
                              set_bits_from_bytes(mb_mapping->tab_coil_status, address, nb, &query[offset + 6]);

                           
                              
                              resp_length = build_response_basis_tcp(&sft, response);
                              /* 4 to copy the coil address (2) and the quantity of coils */
                              memcpy(response + resp_length, query + resp_length, 4);
                              resp_length += 4;
                      }
              }
                      break;
              case FC_PRESET_MULTIPLE_REGISTERS: {
                          
                      int nb = (query[offset + 3] << 8) + query[offset + 4];
      
                      if ((address + nb) > mb_mapping->nb_holding_registers) {
                             
                              resp_length = response_exception(mb_param, &sft,
                                                               ILLEGAL_DATA_ADDRESS, response);
                      } else {
                              int i, j;
                              
                             
                              
                              for (i = address, j = 6; i < address + nb; i++, j += 2) {
                                      /* 6 and 7 = first value */
                                      mb_mapping->tab_holding_registers[i] =
                                              (query[offset + j] << 8) + query[offset + j + 1];
                              }
      
                              resp_length = build_response_basis_tcp(&sft, response);
                              /* 4 to copy the address (2) and the no. of registers */
                              memcpy(response + resp_length, query + resp_length, 4);
                              resp_length += 4;
                      }
              }
                      break;
              case FC_READ_EXCEPTION_STATUS:
              case FC_REPORT_SLAVE_ID:
                      break;
              default:
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
//        mb_mapping->tab_holding_registers = (uint16_t *) malloc(nb_holding_registers * sizeof(uint16_t));
        mb_mapping->tab_holding_registers = (uint16_t *) calloc(nb_holding_registers, sizeof(uint16_t));
//        memset(mb_mapping->tab_holding_registers, 0, nb_holding_registers * sizeof(uint16_t));
        if (mb_mapping->tab_holding_registers == NULL) {
                free(mb_mapping->tab_coil_status);
                free(mb_mapping->tab_input_status);
                return -3;
        }

       
        
        #ifdef DEBUG
        availableMemory();
        #endif /* DEBUG */
          
        return 0;
}
/* Frees the 4 arrays */
void modbus_mapping_free(modbus_mapping_t *mb_mapping)
{
        free(mb_mapping->tab_coil_status);
        free(mb_mapping->tab_input_status);
        free(mb_mapping->tab_holding_registers);
        free(mb_mapping->tab_input_registers);
}

/* Listens for any query from one or many modbus masters in TCP */
int modbus_slave_listen_tcp(modbus_param_t *mb_param, int nb_connection)
{
          Ethernet.begin(mb_param->mac);
          /* Start listening for clients */
          server.begin();
}
/* Sets many input/coil status from a single byte value (all 8 bits of
   the byte value are set) */
void set_bits_from_byte(uint8_t *dest, int address, const uint8_t value)
{
        int i;

        for (i=0; i<8; i++) {
                dest[address+i] = (value & (1 << i)) ? ON : OFF;
        }
}

/* Sets many input/coil status from a table of bytes (only the bits
   between address and address + nb_bits are set) */
void set_bits_from_bytes(uint8_t *dest, int address, int nb_bits,
                         const uint8_t tab_byte[])
{
        int i;
        int shift = 0;

        for (i = address; i < address + nb_bits; i++) {
                dest[i] = tab_byte[(i - address) / 8] & (1 << shift) ? ON : OFF;
                /* gcc doesn't like: shift = (++shift) % 8; */
                shift++;
                shift %= 8;
        }
}

/* Gets the byte value from many input/coil status.
   To obtain a full byte, set nb_bits to 8. */
uint8_t get_byte_from_bits(const uint8_t *src, int address, int nb_bits)
{
        int i;
        uint8_t value = 0;

        if (nb_bits > 8) {
           
            nb_bits = 8;
        }

        for (i=0; i < nb_bits; i++) {
                value |= (src[address+i] << i);
        }

        return value;
}

/* Read a float from 4 bytes in Modbus format */
float modbus_read_float(const uint16_t *src)
{
        float real;
        uint32_t ireal = (src[1] << 16) + src[0];
        real = *((float *)&ireal);

        return real;
}

/* Write a float to 4 bytes in Modbus format */
void modbus_write_float(float real, uint16_t *dest)
{
        uint32_t ireal;

        ireal = *((uint32_t *)&real);
        /* Implicit mask 0xFFFF */
        dest[0] = ireal;
        dest[1] = ireal >> 16;
}

