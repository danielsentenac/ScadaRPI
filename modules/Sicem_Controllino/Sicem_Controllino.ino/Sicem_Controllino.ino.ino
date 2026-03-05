/*
  Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include <ModbusTCPSlave.h> // Modbus2 library
#include <SoftwareSerial.h> // Serial library
#include <Controllino.h> // Controllino library
#include <MuxShield.h>


/*
 *  This part is the Arduino sketch code
 */ 
// Ethernet config

byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x66, 0xAB };
IPAddress ip (192, 168, 224, 162 ); // Controllino-10 has this IP

//uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x72, 0xAB };
//IPAddress ip( 192, 168, 224, 168 ); // Controllino-16 has this IP

//Initialize the Mux Shield
MuxShield muxShield; // Redirection to pins A11,A12,A13

//Arrays to store (MuxShield) resistance 4 status (1=A,2=B,3=C,4=D)
int ModuleChannel[12];
//Arrays to store module setpoint values
int32_t ModuleSetpoint[12];


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

// Define the ModbusTCPSlave object with port = 502
ModbusTCPSlave modbus(502);

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

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

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
  /* CHANNEL LED STATUS data (GREEN/RED) */
  /***********************************************************************************************************/
  //Serial.print("CHANNEL_A_LED_STATUS=");Serial.println(digitalRead(CHANNEL_A_LED_STATUS));
  if (digitalRead(CHANNEL_A_LED_STATUS) == HIGH)
     holdingRegisters[CHANNEL_A_ONOFF_STATUS_ADDR] = 0x01; // GREEN STATUS
  else
     holdingRegisters[CHANNEL_A_ONOFF_STATUS_ADDR] = 0x02; // RED STATUS
  if (digitalRead(CHANNEL_B_LED_STATUS) == HIGH)
     holdingRegisters[CHANNEL_B_ONOFF_STATUS_ADDR] = 0x01; // GREEN STATUS
  else
     holdingRegisters[CHANNEL_B_ONOFF_STATUS_ADDR] = 0x02; // RED STATUS
  if (digitalRead(CHANNEL_C_LED_STATUS) == HIGH)
     holdingRegisters[CHANNEL_C_ONOFF_STATUS_ADDR] = 0x01; // GREEN STATUS
  else
     holdingRegisters[CHANNEL_C_ONOFF_STATUS_ADDR] = 0x02; // RED STATUS
  if (digitalRead(CHANNEL_D_LED_STATUS) == HIGH)
     holdingRegisters[CHANNEL_D_ONOFF_STATUS_ADDR] = 0x01; // GREEN STATUS
  else 
     holdingRegisters[CHANNEL_D_ONOFF_STATUS_ADDR] = 0x02; // RED STATUS
  /***********************************************************************************************************/
  /* CHANNEL ALARM STATUS data (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(CHANNEL_A_ALRM_STATUS) == HIGH)
     holdingRegisters[CHANNEL_A_ALRM_STATUS_ADDR] = 0x01; // ALARM ON STATUS
  else 
     holdingRegisters[CHANNEL_A_ALRM_STATUS_ADDR] = 0x00; // ALARM OFF STATUS
  if (digitalRead(CHANNEL_B_ALRM_STATUS) == HIGH)
     holdingRegisters[CHANNEL_B_ALRM_STATUS_ADDR] = 0x01; // ALARM ON STATUS
  else 
     holdingRegisters[CHANNEL_B_ALRM_STATUS_ADDR] = 0x00; // ALARM OFF STATUS
  if (digitalRead(CHANNEL_C_ALRM_STATUS) == HIGH)
     holdingRegisters[CHANNEL_C_ALRM_STATUS_ADDR] = 0x01; // ALARM ON STATUS
  else 
     holdingRegisters[CHANNEL_C_ALRM_STATUS_ADDR] = 0x00; // ALARM OFF STATUS
  if (digitalRead(CHANNEL_D_ALRM_STATUS) == HIGH)
     holdingRegisters[CHANNEL_D_ALRM_STATUS_ADDR] = 0x01; // ALARM ON STATUS
  else 
     holdingRegisters[CHANNEL_D_ALRM_STATUS_ADDR] = 0x00; // ALARM OFF STATUS
  /***********************************************************************************************************/
  /* SICEM ALARM STATUS data (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(SICEM_ALRM_STATUS) == HIGH)
     holdingRegisters[SICEM_ALRM_STATUS_ADDR] = 0x01; // SICEM ALARM ON STATUS
  else  
     holdingRegisters[SICEM_ALRM_STATUS_ADDR] = 0x00; // SICEM ALARM OFF STATUS
  
  /***********************************************************************************************************/
  /* Module Channel value data (1,2,3,4) */
  /***********************************************************************************************************/
  curaddr = MODULE_CHANNEL_VALUE_START_ADDR;
  // Reset values
  for (int i = 0 ; i < 12; i++) {
      ModuleChannel[i] = 0;
      holdingRegisters[curaddr++] = 0;
  }
  curaddr = MODULE_CHANNEL_VALUE_START_ADDR;
  int reg;
  for (int i = 1 ; i <= 16 ; i++) {
    int module = 0;
    if ( i <= 4 ) {
      module = 1;
      if (i==1) 
        reg = curaddr++;
    }
    else if ( i <= 8 ) {
      module = 2;
      if (i==5) 
        reg = curaddr++;
    }
    else if ( i <= 12 ) {
      module = 3;
      if (i==9) 
        reg = curaddr++;
    }
    else if ( i <= 16 ) {
      module = 4;
      if (i==13) 
        reg = curaddr++;
    }
    int pos = i%4;
    if (pos == 0) pos = 4;
    //Serial.print("module[");Serial.print(module);Serial.print("][");Serial.print(pos);Serial.print("]=");Serial.println(muxShield.digitalReadMS(1,i-1));
    if ( muxShield.digitalReadMS(1,i-1) == HIGH ) {
      ModuleChannel[module-1] = pos;
      holdingRegisters[reg] = pos;
    }
  }
  for (int i = 1 ; i <= 16 ; i++) {
    int module = 0;
    if ( i <= 4 ) {
      module = 5;
      if (i==1) 
        reg = curaddr++;
    }   
    else if ( i <= 8 ) {
      module = 6;
      if (i==5) 
        reg = curaddr++;
    }
    else if ( i <= 12 ) {
      module = 7;
      if (i==9) 
        reg = curaddr++;
    }
    else if ( i <= 16 ) {
      module = 8;
      if (i==13) 
        reg = curaddr++;
    }
    int pos = i%4;
    if (pos == 0) pos = 4;
    if ( muxShield.digitalReadMS(2,i-1) == HIGH ) {
      ModuleChannel[module-1] = pos;
      holdingRegisters[reg] = pos;
    }
  }
  for (int i = 1 ; i <= 16 ; i++) {
    int module = 0;
    if ( i <= 4 ) {
      module = 9;
      if (i==1) 
        reg = curaddr++;
    }
    else if ( i <= 8 ) {
      module = 10;
      if (i==5) 
        reg = curaddr++;
    }
    else if ( i <= 12 ) {
      module = 11;
      if (i==9) 
        reg = curaddr++;
    }
    else if ( i <= 16 ) {
      module = 12;
      if (i==13) 
        reg = curaddr++;
    }
    int pos = i%4;
    if (pos == 0) pos = 4;
    if ( muxShield.digitalReadMS(3,i-1) == HIGH ) {
      ModuleChannel[module-1] = pos;
      holdingRegisters[reg] = pos;
    }
  }
  /***********************************************************************************************************/
  /*  Serial SICEM data / Update Modbus register values */
  /**********************************************************************************************************/
  const char * temp[] = { "s11","s12","s13","s14","s15","s16","s17","s18","s19","s1A","s1B","s1C"};
  const char * setp[] = { "i11","i12","i13","i14","i15","i16","i17","i18","i19","i1A","i1B","i1C"};
  
  curaddr = MODULE_TEMP_START_ADDR;
  int msb,lsb;
  for (int i = 1 ; i <= 24 ; i++) {
    // Communicate with Master (send/receive data)
    SendReceiveMaster();
    msb = curaddr++;
    lsb = curaddr++;
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
    //data[6] = '\r';
    serData3 = (char*)data;
    
#if defined(SICEMSerialIsSoftware)
     SICEMSerial.listen();
#endif
    for (int l = 0 ; l < 7;l++){
       Serial.print(data[l]);
       Serial.print(",");
    }
    Serial.println("");
    SICEMSerial.print(serData3);
    memset(buffer,0,length);
    SICEMSerial.readBytes(buffer,length);
    serData4 = buffer;
    SICEMSerial.flush();
    Serial.print("Sicem resp=");Serial.print(serData4);Serial.println("END");
    if (serData4 == "") {// Com Status Error: treat only as if module is absent!
      Serial.print("No answer from Sicem for module ");Serial.println(pos);
      holdingRegisters[msb] = 0x00;
      holdingRegisters[lsb] = 0x00;
      if (i>12) ModuleSetpoint[pos-1] = 0; // reset setpoint locally
      continue;
    }
    else { // Data OK
      Serial.print("Data OK: Sicem resp=");Serial.print(serData4);Serial.println("END");
      holdingRegisters[SICEM_END_ADDR] = 0x00;
      csum = 0; // Calculate checksum
      for (int k = 0 ; k < serData4.length() - 1; k++)
        csum+= serData4[k];
      if ( ((serData4[serData4.length() -1] & 255) | 128) == ((csum & 255) | 128) ) { // checksum OK
        if ( i <= 12 & serData4.length() > 11) { // Temperature case
           String val = serData4.substring(4,10);
           int32_t decvalue;
           decvalue = strtol(val.c_str(),NULL,16);
           float temp = float(decvalue) / 4096.;
           mbfValue.fvalue = temp;
           Serial.print("TEMP Writing registers [");
           Serial.print(msb);Serial.print(",");Serial.print(lsb);Serial.print("]=[");
           Serial.print(mbfValue.value[0]);Serial.print(",");Serial.print(mbfValue.value[1]);Serial.print("]=");
           Serial.println(mbfValue.fvalue);
           holdingRegisters[msb] = mbfValue.value[0]; 
           holdingRegisters[lsb] = mbfValue.value[1];
        }
        else if ( i > 12 & serData4.length() > 8 ){ // Setpoint case
           String val = serData4.substring(4,7);
           int32_t setpoint = strtol(val.c_str(),NULL,16);
           ModuleSetpoint[pos-1] = setpoint; // Save setpoint locally
           mbiValue.ivalue = setpoint;
           Serial.print("SETPOINT Writing registers [");
           Serial.print(msb);Serial.print(",");Serial.print(lsb);Serial.print("]=[");
           Serial.print(mbiValue.value[0]);Serial.print(",");Serial.print(mbiValue.value[1]);Serial.print("]=");
           Serial.println(mbiValue.ivalue);
           holdingRegisters[msb] = mbiValue.value[0]; 
           holdingRegisters[lsb] = mbiValue.value[1];
        }
      } 
    }
  }  
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
   // single registers for channel cmd & status led
   holdingRegisters[CHANNEL_A_ONOFF_CMD_ADDR] = 0x00;
   holdingRegisters[CHANNEL_B_ONOFF_CMD_ADDR] = 0x00;
   holdingRegisters[CHANNEL_C_ONOFF_CMD_ADDR] = 0x00;
   holdingRegisters[CHANNEL_D_ONOFF_CMD_ADDR] = 0x00;
   holdingRegisters[CHANNEL_A_ONOFF_STATUS_ADDR] = 0x00;
   holdingRegisters[CHANNEL_B_ONOFF_STATUS_ADDR] = 0x00;
   holdingRegisters[CHANNEL_C_ONOFF_STATUS_ADDR] = 0x00;
   holdingRegisters[CHANNEL_D_ONOFF_STATUS_ADDR] = 0x00;
   holdingRegisters[CHANNEL_A_ALRM_STATUS_ADDR] = 0x00;
   holdingRegisters[CHANNEL_B_ALRM_STATUS_ADDR] = 0x00;
   holdingRegisters[CHANNEL_C_ALRM_STATUS_ADDR] = 0x00;
   holdingRegisters[CHANNEL_D_ALRM_STATUS_ADDR] = 0x00;
   holdingRegisters[SICEM_ALRM_STATUS_ADDR] = 0x00;
   // single registers for module channel values from rack
   for (int i = MODULE_CHANNEL_VALUE_START_ADDR; i < MODULE_CHANNEL_VALUE_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
   // LSB & MSB registers for float temperature from Sicem modules
   for (int i = MODULE_TEMP_START_ADDR; i < MODULE_TEMP_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
   // LSB & MSB registers for setpoint values from Sicem modules
   for (int i = MODULE_SETPOINT_START_ADDR; i < MODULE_SETPOINT_END_ADDR; i++)
        holdingRegisters[i] = 0x00;
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
  holdingRegisters[CHANNEL_A_ONOFF_CMD_ADDR] = 0x00; // RESET
  digitalWrite(CHANNEL_B_ON,LOW);                             // Set CHANNEL_B_ON LOW
  digitalWrite(CHANNEL_B_OFF,HIGH);                           // Set CHANNEL_B_OFF HIGH
  pinMode(CHANNEL_B_ON, OUTPUT);                              // sets the digital pin as output for CHANNEL_B_ON
  pinMode(CHANNEL_B_OFF, OUTPUT);                             // sets the digital pin as output for CHANNEL_B_OFF
  holdingRegisters[CHANNEL_B_ONOFF_CMD_ADDR] = 0x00; // RESET
  digitalWrite(CHANNEL_C_ON,LOW);                             // Set CHANNEL_C_ON LOW
  digitalWrite(CHANNEL_C_OFF,HIGH);                           // Set CHANNEL_C_OFF HIGH
  pinMode(CHANNEL_C_ON, OUTPUT);                              // sets the digital pin as output for CHANNEL_C_ON
  pinMode(CHANNEL_C_OFF, OUTPUT);                             // sets the digital pin as output for CHANNEL_C_OFF
  holdingRegisters[CHANNEL_C_ONOFF_CMD_ADDR] = 0x00; // RESET
  digitalWrite(CHANNEL_D_ON,LOW);                             // Set CHANNEL_D_ON LOW
  digitalWrite(CHANNEL_D_OFF,HIGH);                           // Set CHANNEL_D_OFF HIGH
  pinMode(CHANNEL_D_ON, OUTPUT);                              // sets the digital pin as output for CHANNEL_D_ON
  pinMode(CHANNEL_D_OFF, OUTPUT);                             // sets the digital pin as output for CHANNEL_D_OFF
  holdingRegisters[CHANNEL_D_ONOFF_CMD_ADDR] = 0x00; // RESET
  // Digital VALVE INPUTS initialization and assignation
  pinMode(CHANNEL_A_LED_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_A_LED_STATUS
  holdingRegisters[CHANNEL_A_ONOFF_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_B_LED_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_B_LED_STATUS
  holdingRegisters[CHANNEL_B_ONOFF_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_C_LED_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_C_LED_STATUS
  holdingRegisters[CHANNEL_C_ONOFF_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_D_LED_STATUS, INPUT);                          // sets the digital pin as input for CHANNEL_D_LED_STATUS 
  holdingRegisters[CHANNEL_D_ONOFF_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_A_ALRM_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_A_ALRM_STATUS
  holdingRegisters[CHANNEL_A_ALRM_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_B_ALRM_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_B_ALRM_STATUS
  holdingRegisters[CHANNEL_B_ALRM_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_C_ALRM_STATUS, INPUT);                           // sets the digital pin as input for CHANNEL_C_ALRM_STATUS
  holdingRegisters[CHANNEL_C_ALRM_STATUS_ADDR] = 0x00; // RESET
  pinMode(CHANNEL_D_ALRM_STATUS, INPUT);                          // sets the digital pin as input for CHANNEL_D_ALRM_STATUS 
  holdingRegisters[CHANNEL_D_ALRM_STATUS_ADDR] = 0x00; // RESET
  pinMode(SICEM_ALRM_STATUS, INPUT);                              // sets the digital pin as input for SICEM_ALRM_STATUS 
  holdingRegisters[SICEM_ALRM_STATUS_ADDR] = 0x00;// RESET

  // rack comm status
  holdingRegisters[SICEM_END_ADDR] = 0x01;
   
  // register for Arduino reset
  holdingRegisters[ARD_RESET_ADDR] = 0x00;     // Arduino Global Reset Status

}

void SendReceiveMaster()
{    
 
  // Process modbus requests
  modbus.update();
      
  typedef union {
     int32_t ivalue;
     uint16_t value[2];
  } Int32Uint16;
  Int32Uint16 mbiValue;
  /***********************************************************************************************************/
  /* Update CHANNEL_A Position (ON/OFF) */
  /***********************************************************************************************************/
  if (holdingRegisters[CHANNEL_A_ONOFF_CMD_ADDR] == 0x02) {
     digitalWrite(CHANNEL_A_ON,LOW); // SWITCH OFF Channel
     delay(1000);
     digitalWrite(CHANNEL_A_OFF,HIGH); // SWITCH OFF Channel
  }
  else if (holdingRegisters[CHANNEL_A_ONOFF_CMD_ADDR] == 0x01) {
     digitalWrite(CHANNEL_A_ON,HIGH); // SWITCH ON Channel
     delay(1000);
     digitalWrite(CHANNEL_A_OFF,LOW); // SWITCH ON Channel
  }
  holdingRegisters[CHANNEL_A_ONOFF_CMD_ADDR] = 0x00; // Reset register
  /***********************************************************************************************************/
  /* Update CHANNEL_B Position (ON/OFF) */
  /***********************************************************************************************************/
  if (holdingRegisters[CHANNEL_B_ONOFF_CMD_ADDR] == 0x02) {
     digitalWrite(CHANNEL_B_ON,LOW); // SWITCH OFF Channel
     delay(1000);
     digitalWrite(CHANNEL_B_OFF,HIGH); // SWITCH OFF Channel
  }
  else if (holdingRegisters[CHANNEL_B_ONOFF_CMD_ADDR] == 0x01) {
     digitalWrite(CHANNEL_B_ON,HIGH); // SWITCH ON Channel
     delay(1000);
     digitalWrite(CHANNEL_B_OFF,LOW); // SWITCH ON Channel
  }
  holdingRegisters[CHANNEL_B_ONOFF_CMD_ADDR] = 0x00; // Reset register
  /***********************************************************************************************************/
  /* Update CHANNEL_C Position (ON/OFF) */
  /***********************************************************************************************************/
  if (holdingRegisters[CHANNEL_C_ONOFF_CMD_ADDR] == 0x02) {
     digitalWrite(CHANNEL_C_ON,LOW); // SWITCH OFF Channel
     delay(1000);
     digitalWrite(CHANNEL_C_OFF,HIGH); // SWITCH OFF Channel
  }
  else if (holdingRegisters[CHANNEL_C_ONOFF_CMD_ADDR] == 0x01) {
     digitalWrite(CHANNEL_C_ON,HIGH); // SWITCH ON Channel
     delay(1000);
     digitalWrite(CHANNEL_C_OFF,LOW); // SWITCH ON Channel
  }
  holdingRegisters[CHANNEL_C_ONOFF_CMD_ADDR] = 0x00; // Reset register
  /***********************************************************************************************************/
  /* Update CHANNEL_D Position (ON/OFF) */
  /***********************************************************************************************************/
  if (holdingRegisters[CHANNEL_D_ONOFF_CMD_ADDR] == 0x02) {
     digitalWrite(CHANNEL_D_ON,LOW); // SWITCH OFF Channel
     delay(1000);
     digitalWrite(CHANNEL_D_OFF,HIGH); // SWITCH OFF Channel
  }
  else if (holdingRegisters[CHANNEL_D_ONOFF_CMD_ADDR] == 0x01) {
     digitalWrite(CHANNEL_D_ON,HIGH); // SWITCH ON Channel
     delay(1000);
     digitalWrite(CHANNEL_D_OFF,LOW); // SWITCH ON Channel
  }
  holdingRegisters[CHANNEL_D_ONOFF_CMD_ADDR] = 0x00; // Reset register
 
  /***********************************************************************************************************/
  /* Update MAXIGAUGE Sensor ON/OFF Status */
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
  /***********************************************************************************************************/
  /* Update SICEM SETPOINT */
  /***********************************************************************************************************/
  int curaddr = MODULE_SETPOINT_START_ADDR;
  for (int j = 0; j < 12; j++) {
    int32_t setpoint = ModuleSetpoint[j];
    mbiValue.value[0] = holdingRegisters[curaddr++]; 
    mbiValue.value[1] = holdingRegisters[curaddr++];
    
    if ( mbiValue.ivalue != setpoint) { // Update setpoint
      Serial.print("ModuleSetPoint[");Serial.print(j);Serial.print("]=");Serial.print(ModuleSetpoint[j]);Serial.print("  RegisterSetPoint=");Serial.println(mbiValue.ivalue);
      String takecmd,changesetpoint,releasecmd,answer;
      int module = j+1;
      // Take command priority
      String modulename = String(module,HEX);
      modulename.toUpperCase();
      takecmd = "h1"+ modulename + "01";
      if  (SendSicemRequest(takecmd,&answer) == -1)
        continue;
      // Do the setpoint change
      String hexSetpointVal = String((int)mbiValue.ivalue,HEX); // New setpoint is an int
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
      ModuleSetpoint[j] = mbiValue.ivalue;
      // Release command priority
      releasecmd = "h1"+ modulename + "00";
      if  (SendSicemRequest(releasecmd,&answer) == -1)
        continue;
    }
  }
 
}
