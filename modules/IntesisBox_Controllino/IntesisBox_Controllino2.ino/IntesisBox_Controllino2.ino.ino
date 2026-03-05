#include <Ethernet.h>   // Ethernet library for Modbus TCP
#include <Controllino.h>  /* Usage of CONTROLLINO library allows you to use CONTROLLINO_xx aliases in your sketch. */
#include "ModbusRtu.h"    /* Usage of ModBusRtu library allows you to implement the Modbus RTU protocol in your sketch. */
#include <ModbusTCPSlave.h> // Modbus2 library
#include <OneWire.h> // OneWire library
#include <DS2438.h> 

#ifdef abs
#undef abs
#endif

#define abs(x) ((x)>0?(x):-(x))

/*
 * The time for Check and Reset actions
 */
unsigned long RELAY_FANtime = 0;


/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean RELAY_FAN_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;
long AC_wait = 10;

// Temperature sensors
#define DS24B38 0x26      // 1-Wire DS2438 address (DS2438 + HIH5030 sensor)

#define ONEWIRE_1 CONTROLLINO_A0  // digital address used for bus 1-Wire
#define ONEWIRE_2 CONTROLLINO_A1  // digital address used for bus 1-Wire
#define ONEWIRE_3 CONTROLLINO_A2  // digital address used for bus 1-Wire
#define ONEWIRE_4 CONTROLLINO_A3  // digital address used for bus 1-Wire

#define RELAY_FAN CONTROLLINO_R0  // Relay Fan

OneWire ds1(ONEWIRE_1); // OneWire object
OneWire ds2(ONEWIRE_2); // OneWire object
OneWire ds3(ONEWIRE_3); // OneWire object
OneWire ds4(ONEWIRE_4); // OneWire object

#define BUS_SENSOR_MAX 1  // Max number of sensors per bus


// TCP address for Modbus 
byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x77, 0xAB};
const byte ip[] = { 192, 168, 224, 180 }; // Controllino-21 has this IP


// TCP Modbus objects
#define SLAVE 0x01
modbus_param_t mb_param;     // generic mb params
modbus_mapping_t mb_mapping; // registers

/* Server object will run on Arduino, will be initialized later */
EthernetServer server(MODBUS_TCP_DEFAULT_PORT);

/* Client object connected with Arduino. This initializaton will be reassigned */
EthernetClient client = 0;

// This MACRO defines Modbus master address.
// For any Modbus slave devices are reserved addresses in the range from 1 to 247.
// Important note only address 0 is reserved for a Modbus master device!
#define MasterModbusAdd  0
#define SlaveModbusAdd1  1 // AC unit 1
#define SlaveModbusAdd2  2 // AC unit 2

// This MACRO defines number of the comport that is used for RS 485 interface.
// For MAXI and MEGA RS485 is reserved UART Serial3.
#define RS485Serial     3

// The object ControllinoModbuSlave of the class Modbus is initialized with three parameters.
// The first parametr specifies the address of the Modbus slave device.
// The second parameter specifies type of the interface used for communication between devices - in this sketch is used RS485.
// The third parameter can be any number. During the initialization of the object this parameter has no effect.
Modbus ControllinoModbusMaster(MasterModbusAdd, RS485Serial, 0);

#define ACUNIT_REGNUM 65
uint16_t ModbusSlaveRegisters1[ACUNIT_REGNUM];
uint16_t ModbusSlaveRegisters2[ACUNIT_REGNUM];

// Modbus address limit
#define NB_HOLDING_REGISTERS 300

// Modbus R/W addresses
#define AC1_ONOFF_ADDR 0
#define AC1_MODE_ADDR  1
#define AC1_FAN_ADDR   2
#define AC1_VANE_ADDR  3
#define AC1_SET_ADDR   4
#define AC1_ECO_ADDR   64

#define AC2_ONOFF_ADDR 100
#define AC2_MODE_ADDR  101
#define AC2_FAN_ADDR   102
#define AC2_VANE_ADDR  103
#define AC2_SET_ADDR   104
#define AC2_ECO_ADDR   164

// Temperature sensors addresses
#define FIRST_DS2438_ADDR 200

typedef union {
       float fvalue;
       uint16_t value[2];
  } FloatUint16;
FloatUint16 mbValue;

float Temperature[4][BUS_SENSOR_MAX];
float Humidity[4][BUS_SENSOR_MAX];
String address[4];

int setpoint = 20; // Initial default setpoint
int setmin = 16; // Min AC setpoint
int setmax = 25; // Max AC setpoint
float delta = 2; // Initial default delta for setpoint and delta0
int setpointerr = 0; // Initial default value
int regime = 0; // Initial regime value is out of scope on purpose
int fanerr = 2; // error temperature for inhomogeneities between delta1 and delta2
  
// Loop R/W addresses
#define LOOP_ONOFF_ADDR 220
#define LOOP_SET_ADDR  221
#define LOOP_DELTA_ADDR  222
#define LOOP_SETERR_ADDR  223
#define LOOP_FANERR_ADDR  224
#define LOOP_REGIME_ADDR  225
#define RELAY_FAN_STATUS_ADDR   230
#define RELAY_FAN_CMD_ADDR   231

// Reset Controllino address
#define RESETARD 299

void(*resetArd) (void) = 0; //declare reset function @ address 0

void getAddress(String &id, byte addr[8]) {
  for (uint8_t i = 1; i < 7; i++) {
    if (addr[i] < 0x10) id += String("0");;
    id += String(addr[i], HEX);
  }
}
void getDigitalIn(OneWire *ds, float &Humidity, float &Temperature, byte addr[8]){
  
  DS2438 ds2438(ds, addr);
  ds2438.begin();
  ds2438.update();
  if (ds2438.isError() || ds2438.getVoltage(DS2438_CHA) == 0.0) {
      //Serial.println("Error reading from DS2438 device");
      return;
    }
  Temperature = ds2438.getTemperature();
  float rh = (ds2438.getVoltage(DS2438_CHA) / ds2438.getVoltage(DS2438_CHB) - 0.1515) / 0.00636;
  Humidity = (float)(rh / (1.0546 - 0.00216 * Temperature));
  if (Humidity < 0.0)
     Humidity = 0.0;
  if (Humidity > 100.0)
     Humidity = 100.0;
}
void getMeasureBus(OneWire *ds, int num) {
  int busCnt = 0;
  byte  addr[8] = {0, 0, 0, 0, 0, 0, 0, 0};
  address[num] = "";
  boolean success = true;
  while (ds->search(addr) == 1 && busCnt < BUS_SENSOR_MAX) { // Scan 1-Wire module
    if (OneWire::crc8(addr, 7) != addr[7]) {// Check address integrity
      //Serial.println("address failed");
      success = false;
      break;
    }
    else if (addr[0] != DS24B38) {// Check DS24B38 module type
      Serial.println("no DS24B38");
      success = false;
      break;
    }
    if (success == true) getAddress(address[num], addr);
    if (address[num].length() < 4 ) success = false;
    if (success == true) {
       getDigitalIn(ds, Humidity[num][busCnt], Temperature[num][busCnt], addr);
       delay(100);
       busCnt++;
    }
  }
  if (success == false) return;
  /*for (int i = 0; i < BUS_SENSOR_MAX; i++) {
    if (address[num] == "") return;
    char *endptr;
    Serial.print("Measuring BUS :");
    Serial.println(num);
    Serial.print(address[num]);Serial.print(" ");Serial.println(strtol(address[num].substring(0,4).c_str(), &endptr, 16));
    Serial.println(Humidity[num][i]);
    Serial.println(Temperature[num][i]);
  }*/
  // Set index
  int index = FIRST_DS2438_ADDR + 5*num;
  for (int i = 0 ; i < BUS_SENSOR_MAX; i++) {
     // Set address register
     //Serial.print("BUS "); Serial.print(num);Serial.print(" address:");Serial.println(address[i].substring(0,4));
     if (address[num].length() == 0) continue;
     //Serial.print("address=");Serial.print(hstol(address[num].substring(0,4)));Serial.print("(");Serial.print(address[num].length());Serial.println(")");
    
     mb_mapping.tab_holding_registers[index] = (hstol(address[num].substring(0,4)));
     //Serial.print("address register:");//Serial.print(String(index));//Serial.print(" ");
     //Serial.println(String(mb_mapping.tab_holding_registers[index]));

     // Set Temperature register
     //Serial.println(Temperature[num][i]);
     mbValue.fvalue = Temperature[num][i];
     mb_mapping.tab_holding_registers[++index] = mbValue.value[0];
     //Serial.print("Temperature register LSB ");//Serial.print(String(index));//Serial.print(" ");
     //Serial.println(String(mbValue.value[0]));
     mb_mapping.tab_holding_registers[++index] = mbValue.value[1];
     //Serial.print("Temperature register MSB ");//Serial.print(String(index));//Serial.print(" ");
     //Serial.println(String(mbValue.value[1]));
  
     // Set Humidity register
     //Serial.println(Humidity[num][i]);
     mbValue.fvalue = Humidity[num][i];
     mb_mapping.tab_holding_registers[++index] = mbValue.value[0];
     //Serial.print("Humidity register LSB ");//Serial.print(String(index));//Serial.print(" ");
     //Serial.println(String(mbValue.value[0]));
     mb_mapping.tab_holding_registers[++index] = mbValue.value[1];
     //Serial.print("Humidity register MSB ");//Serial.print(String(index));//Serial.print(" ");
     //Serial.println(String(mbValue.value[1]));
  }
}
void setup() {

  // 1-wire bus reset
  ds1.reset(); 
  ds2.reset(); 
  ds3.reset(); 
  ds4.reset();       

  digitalWrite(RELAY_FAN,LOW);   // Set RELAY_FAN LOW (NORMALLY CLOSED)
  pinMode(RELAY_FAN, OUTPUT);    // sets the digital pin as output for FAN
  
  // initialize serial communication at 9600 bits per second:
  Serial.begin(9600);
  Serial.println("Started Serial:Console");
  Serial.println("-----------------------------------------");
  
  // start the Modbus server
  StartModbusServer();
  Serial.println("Started Serial:Modbus TCP Slave");
  Serial.println("-----------------------------------------");
  
  ControllinoModbusMaster.begin( 19200 ); // baud-rate at 19200
  ControllinoModbusMaster.setTimeOut( 5000 ); // if there is no answer in 5000 ms, roll over

  Serial.println("Started Serial:Modbus RTU Master");
  Serial.println("-----------------------------------------");
}
void ModbusRtuQuery(int unit, int startaddr, int numreg, uint16_t *regs, int startmbreg) {
  modbus_t mq;
  // ModbusQuery 0: read registers from AC unit 
  mq.u8id = unit; // slave address
  mq.u8fct = 3; // function code (this one is registers read)
  mq.u16RegAdd = startaddr; // start address in slave
  mq.u16CoilsNo = numreg; // number of elements (coils or registers) to read
  mq.au16reg = regs+startaddr; // pointer to a memory array in the CONTROLLINO
  ControllinoModbusMaster.query( mq );
    int waitCnt = 0;
    while (ControllinoModbusMaster.poll() == 0 && waitCnt++ < AC_wait)  {
      delay(100);
    }
    if (ControllinoModbusMaster.getState() == COM_IDLE) {
       // registers read was proceed
       //Serial.println("---------- READ RESPONSE from AC unit ----");
      // Serial.print("Slave ");
      // Serial.println(unit);
       for (int i = 0; i < numreg ; i++) {
        //  Serial.print("AC Unit "); Serial.print(unit); Serial.print(" register [");Serial.print(startaddr+i);Serial.print("]=");
          mb_mapping.tab_holding_registers[startmbreg+i] = regs[startaddr+i];
         // Serial.println(regs[startaddr+i], DEC);
       }
      // Serial.println("-------------------------------------");
      // Serial.println("");  
    }
}
void loop() {

    /***********************************************************************************************************/
    // check DHCP lease
    Ethernet.maintain();
    /***********************************************************************************************************/
    // Check Reset Status
    /***********************************************************************************************************/
    if (mb_mapping.tab_holding_registers[RESETARD] == 0x01)
      resetArd();

    ///////////////////////////////////////////////////////////////////////////////
    // Get Relay Fan Status
    ///////////////////////////////////////////////////////////////////////////////
    if (digitalRead(RELAY_FAN) == HIGH)
       mb_mapping.tab_holding_registers[RELAY_FAN_STATUS_ADDR] = 0x01; // FAN ON
    else if (digitalRead(RELAY_FAN) == LOW)
       mb_mapping.tab_holding_registers[RELAY_FAN_STATUS_ADDR] = 0x02; // FAN OFF
    ///////////////////////////////////////////////////////////////////////////////
    // Get Setpoint & Delta Temperature
    ///////////////////////////////////////////////////////////////////////////////
    mb_mapping.tab_holding_registers[LOOP_SET_ADDR] = setpoint; // Writable Temperature Set Point Value
    mb_mapping.tab_holding_registers[LOOP_DELTA_ADDR] = delta; // Writable Temperature Delta Value
    mb_mapping.tab_holding_registers[LOOP_SETERR_ADDR] = setpointerr; // Readable Temperature Set Point error Value
    mb_mapping.tab_holding_registers[LOOP_FANERR_ADDR] = fanerr; // Writable Fan error Value
    mb_mapping.tab_holding_registers[LOOP_REGIME_ADDR] = regime; // Readable Regime Value
       
    //////////////////////////////////////////////////////////////////////////////
    // Query AC unit 1 registers
    //////////////////////////////////////////////////////////////////////////////
    ModbusRtuQuery(SlaveModbusAdd1, 0, 12, ModbusSlaveRegisters1, AC1_ONOFF_ADDR);
    ModbusRtuQuery(SlaveModbusAdd1, 64, 1, ModbusSlaveRegisters1, AC1_ECO_ADDR);
  
    //////////////////////////////////////////////////////////////////////////////
    // Query AC unit 2 registers
    //////////////////////////////////////////////////////////////////////////////
    ModbusRtuQuery(SlaveModbusAdd2, 0, 12, ModbusSlaveRegisters2, AC2_ONOFF_ADDR);
    ModbusRtuQuery(SlaveModbusAdd2, 64, 1, ModbusSlaveRegisters2, AC2_ECO_ADDR);
 
    //////////////////////////////////////////////////////////////////////////////
    // Get temperatures & Humidities on the BUSes
    /////////////////////////////////////////////////////////////////////////////
    getMeasureBus(&ds1,0);
   
    //////////////////////////////////////////////////////////////////////////////
    // Get temperatures & Humidities on the BUSes
    /////////////////////////////////////////////////////////////////////////////
    getMeasureBus(&ds2,1);

    //////////////////////////////////////////////////////////////////////////////
    // Get temperatures & Humidities on the BUSes
    /////////////////////////////////////////////////////////////////////////////
    getMeasureBus(&ds3,2);
   
    //////////////////////////////////////////////////////////////////////////////
    // Get temperatures & Humidities on the BUSes
    /////////////////////////////////////////////////////////////////////////////
    getMeasureBus(&ds4,3);
    
    //////////////////////////////////////////////////////////////////////////////    
    // Listen to TCP Master
    //////////////////////////////////////////////////////////////////////////////    
    SendReceiveMaster();

    /***********************************************************************************************************/
    /* Perform the control loop logic here */
    /***********************************************************************************************************/
    if (mb_mapping.tab_holding_registers[LOOP_ONOFF_ADDR] == 0x01)
       doControlLoop();
    
}
void SendCommand(int unit, int addr, int value) {

    Serial.print("Send Command to: unit:");Serial.print(unit);Serial.print(",addr:");Serial.print(addr);Serial.print(",value:");Serial.println(value);
    modbus_t mq;
    // ModbusQuery 2: write registers to AC unit
    mq.u8id = unit; // slave address
    mq.u8fct = 6; // function code this one is write a single register)
    mq.u16RegAdd = addr; // start address in slave
    mq.u16CoilsNo = 1; // number of elements (coils or registers) to read
    if ( unit == 1 ) {
       mq.au16reg = ModbusSlaveRegisters1+addr; // pointer to a memory array in the CONTROLLINO
       ModbusSlaveRegisters1[addr] = value;
    }
    else if ( unit == 2 ) {
       mq.au16reg = ModbusSlaveRegisters2+addr; // pointer to a memory array in the CONTROLLINO
       ModbusSlaveRegisters2[addr] = value;
    }
    ControllinoModbusMaster.query( mq );
    int waitCnt = 0;
    while (ControllinoModbusMaster.poll() == 0 && waitCnt++ < AC_wait)  {
      delay(100);
    }
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
 
  /* Init all registers */
  for (int i = 0; i < NB_HOLDING_REGISTERS; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
 
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
       
    // Check Modbus registers for commands //////////////////////////////////////////////// AC Unit 1
    if (mb_mapping.tab_holding_registers[AC1_ONOFF_ADDR] != ModbusSlaveRegisters1[0]) {
      Serial.print("Change register ");Serial.print(AC1_ONOFF_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC1_ONOFF_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[0]);
      SendCommand(SlaveModbusAdd1, 0, mb_mapping.tab_holding_registers[AC1_ONOFF_ADDR]);
    }
    if (mb_mapping.tab_holding_registers[AC1_MODE_ADDR] != ModbusSlaveRegisters1[1]){
      Serial.print("Change register ");Serial.print(AC1_MODE_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC1_MODE_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[1]);
      SendCommand(SlaveModbusAdd1, 1, mb_mapping.tab_holding_registers[AC1_MODE_ADDR]);
    }
    if (mb_mapping.tab_holding_registers[AC1_FAN_ADDR] != ModbusSlaveRegisters1[2]){
      Serial.print("Change register ");Serial.print(AC1_FAN_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC1_FAN_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[2]);
      SendCommand(SlaveModbusAdd1, 2, mb_mapping.tab_holding_registers[AC1_FAN_ADDR]);
    }
    if (mb_mapping.tab_holding_registers[AC1_VANE_ADDR] != ModbusSlaveRegisters1[3]){
      Serial.print("Change register ");Serial.print(AC1_VANE_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC1_VANE_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[3]);
      SendCommand(SlaveModbusAdd1, 3, mb_mapping.tab_holding_registers[AC1_VANE_ADDR]);
    }
    if (mb_mapping.tab_holding_registers[AC1_SET_ADDR] != ModbusSlaveRegisters1[4]){
      Serial.print("Change register ");Serial.print(AC1_SET_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC1_SET_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[4]);
      SendCommand(SlaveModbusAdd1, 4, mb_mapping.tab_holding_registers[AC1_SET_ADDR]);
    }
    if (mb_mapping.tab_holding_registers[AC1_ECO_ADDR] != ModbusSlaveRegisters1[64]){
      Serial.print("Change register ");Serial.print(AC1_ECO_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC1_ECO_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[64]);
      SendCommand(SlaveModbusAdd1, 64, mb_mapping.tab_holding_registers[AC1_ECO_ADDR]);
    }

     // Check Modbus registers for commands //////////////////////////////////////////////// AC Unit 1
    if (mb_mapping.tab_holding_registers[AC2_ONOFF_ADDR] != ModbusSlaveRegisters2[0]) {
      Serial.print("Change register ");Serial.print(AC2_ONOFF_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC2_ONOFF_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[0]);
      SendCommand(SlaveModbusAdd2, 0, mb_mapping.tab_holding_registers[AC2_ONOFF_ADDR]);
    }
    if (mb_mapping.tab_holding_registers[AC2_MODE_ADDR] != ModbusSlaveRegisters2[1]){
      Serial.print("Change register ");Serial.print(AC2_MODE_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC2_MODE_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[1]);
      SendCommand(SlaveModbusAdd2, 1, mb_mapping.tab_holding_registers[AC2_MODE_ADDR]);
    }
    if (mb_mapping.tab_holding_registers[AC2_FAN_ADDR] != ModbusSlaveRegisters2[2]){
      Serial.print("Change register ");Serial.print(AC2_FAN_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC2_FAN_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[2]);
      SendCommand(SlaveModbusAdd2, 2, mb_mapping.tab_holding_registers[AC2_FAN_ADDR]);
    }
    if (mb_mapping.tab_holding_registers[AC2_VANE_ADDR] != ModbusSlaveRegisters2[3]){
      Serial.print("Change register ");Serial.print(AC2_VANE_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC2_VANE_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[3]);
      SendCommand(SlaveModbusAdd2, 3, mb_mapping.tab_holding_registers[AC2_VANE_ADDR]);
    }
    if (mb_mapping.tab_holding_registers[AC2_SET_ADDR] != ModbusSlaveRegisters2[4]){
      Serial.print("Change register ");Serial.print(AC2_SET_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC2_SET_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[4]);
      SendCommand(SlaveModbusAdd2, 4, mb_mapping.tab_holding_registers[AC2_SET_ADDR]);
    }
    if (mb_mapping.tab_holding_registers[AC2_ECO_ADDR] != ModbusSlaveRegisters2[64]){
      Serial.print("Change register ");Serial.print(AC2_ECO_ADDR);Serial.print(" value:");
      Serial.print(mb_mapping.tab_holding_registers[AC2_ECO_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[64]);
      SendCommand(SlaveModbusAdd2, 64, mb_mapping.tab_holding_registers[AC2_ECO_ADDR]);
    }

    // Relay command
    if (mb_mapping.tab_holding_registers[RELAY_FAN_CMD_ADDR] == 0x01 && RELAY_FAN_RESET == false) {
       digitalWrite(RELAY_FAN,HIGH); // OPEN RELAY (FAN ON)
       RELAY_FANtime = millis();
       RELAY_FAN_RESET = true;
    }
    else if (mb_mapping.tab_holding_registers[RELAY_FAN_CMD_ADDR] == 0x02 && RELAY_FAN_RESET == false) {
       digitalWrite(RELAY_FAN,LOW); // CLOSE RELAY (FAN OFF)
       RELAY_FANtime = millis();
       RELAY_FAN_RESET = true;
    }
    // Set Point command
    if (mb_mapping.tab_holding_registers[LOOP_SET_ADDR] != setpoint) 
       if (mb_mapping.tab_holding_registers[LOOP_SET_ADDR] > setmin && 
           mb_mapping.tab_holding_registers[LOOP_SET_ADDR] < setmax)
           setpoint =  mb_mapping.tab_holding_registers[LOOP_SET_ADDR];// Set Set new Set Point
    // Set delta command
    if (mb_mapping.tab_holding_registers[LOOP_DELTA_ADDR] != delta) 
       delta =  mb_mapping.tab_holding_registers[LOOP_DELTA_ADDR];// Set Set new Delta 
    // Set fanerr command
    if (mb_mapping.tab_holding_registers[LOOP_FANERR_ADDR] != fanerr) 
       fanerr =  mb_mapping.tab_holding_registers[LOOP_FANERR_ADDR];// Set Set new Fanerr 
       
    /***********************************************************************************************************/
    /* Perform the Reset and Check routine in the loop */
    /***********************************************************************************************************/
    ResetAndCheck();
}
void ResetAndCheck() {
/*
 *  RELAYFAN Case
 */
  // Reset RELAY_FAN
  if (RELAY_FAN_RESET == true) {
    if ( millis() - RELAY_FANtime > reset_wait) {
       mb_mapping.tab_holding_registers[RELAY_FAN_CMD_ADDR] = 0x00;
       RELAY_FAN_RESET = false;  
    }
  }
}
void doControlLoop() {
  // Control AC Units to maintain setpoint Temperature
  Serial.println("/////////////////////////////////////////////////////////");
  Serial.println("Control Loop..");
  float t_center = Temperature[3][0]; // C1E5
  float t_ac1 = Temperature[0][0];    // CAE5
  float t_ac2 = Temperature[2][0];    // 8A47
  float t_ext = Temperature[1][0];    // D1E5
  // Goal: Minimize difference between internal temperatures and setpoint
  float delta0 = t_center - setpoint;
  float delta1 = t_center - t_ac1;
  float delta2 = t_center - t_ac2;
  float delta3 = t_center - t_ext;
  Serial.print("delta0=");Serial.println(delta0);
  if (delta3 > 0 && delta0 < 0 && regime != 0 && regime != 9 ) { // AC Mode Heat
     Serial.println("AC in Heat Mode");
     if (ModbusSlaveRegisters1[1] != 1)
        SendCommand(SlaveModbusAdd1, 1, 1); // Switch to Heat mode
     if (ModbusSlaveRegisters2[1] != 1)
        SendCommand(SlaveModbusAdd2, 1, 1); // Switch to Heat mode
  }
  else  {                         // AC Mode Cool
     Serial.println("AC in Cool Mode");
     if (ModbusSlaveRegisters1[1] != 4)
        SendCommand(SlaveModbusAdd1, 1, 4); // Switch to Cool mode
     if (ModbusSlaveRegisters2[1] != 4)
        SendCommand(SlaveModbusAdd2, 1, 4); // Switch to Cool mode
  }
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Adjust Fan ON / OFF in case of inhomogeneous Temp
  /*if (abs(delta1 - delta2) > fanerr ) {
     if (RELAY_FAN_RESET == false) { 
        Serial.println("Switch FAN ON");
        digitalWrite(RELAY_FAN,HIGH); // OPEN RELAY (FAN ON)
        RELAY_FANtime = millis();
        RELAY_FAN_RESET = true;
     }
  }
  else {
    if (RELAY_FAN_RESET == false) { 
       digitalWrite(RELAY_FAN,LOW); // CLOSE RELAY (FAN OFF)
       RELAY_FANtime = millis();
       RELAY_FAN_RESET = true;
    }
  }*/
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Adjust AC Setpoint (delta0 > 0)
  if ((abs(delta0) > 2*delta) && (delta0 > 0)) { // High difference: Fan High; No ECO; Set Min Temperature
     regime = 4;
     setpointerr = 4;
     //Serial.println("regime 4");
     if (ModbusSlaveRegisters1[2] != 4)
        SendCommand(SlaveModbusAdd1, 2, 4); // Fan High
     if (ModbusSlaveRegisters2[2] != 4)
        SendCommand(SlaveModbusAdd2, 2, 4); // Fan High
     if (ModbusSlaveRegisters1[64] != 0)
        SendCommand(SlaveModbusAdd1, 64, 0); // No ECO
     if (ModbusSlaveRegisters2[64] != 0)
        SendCommand(SlaveModbusAdd2, 64, 0); // No ECO
     if (ModbusSlaveRegisters1[4] != min(setmin,setpoint-setpointerr))
        SendCommand(SlaveModbusAdd1, 4, min(setmin,setpoint-setpointerr)); // Set Min Temp
     if (ModbusSlaveRegisters2[4] != min(setmin,setpoint-setpointerr))
        SendCommand(SlaveModbusAdd2, 4, min(setmin,setpoint-setpointerr)); // Set Min Temp
  }
  // Adjust AC Setpoint (delta0 > 0)
  else if ((abs(delta0) > 1.5*delta) && (abs(delta0) < 2*delta) && (delta0 > 0)) { // Medium difference Fan Medium; No ECO; Set Lower Temperature
     if (regime == 2) setpointerr+=1;
     else if (regime == 4) setpointerr-=1;
     else setpointerr = 3;
     regime = 3;
     //Serial.println("regime 3");
     if (ModbusSlaveRegisters1[2] != 3)
        SendCommand(SlaveModbusAdd1, 2, 3); // Fan Medium
     if (ModbusSlaveRegisters2[2] != 3)
        SendCommand(SlaveModbusAdd2, 2, 3); // Fan Medium
     if (ModbusSlaveRegisters1[64] != 0)
        SendCommand(SlaveModbusAdd1, 64, 0); // No ECO
     if (ModbusSlaveRegisters2[64] != 0)
        SendCommand(SlaveModbusAdd2, 64, 0); // No ECO
     if (ModbusSlaveRegisters1[4] != setpoint - setpointerr )
        SendCommand(SlaveModbusAdd1, 4, setpoint - setpointerr ); // Set setpoint - setpointerr Temp
     if (ModbusSlaveRegisters2[4] != setpoint - setpointerr )
        SendCommand(SlaveModbusAdd2, 4, setpoint - setpointerr ); // Set setpoint - setpointerr Temp
  } 
  // Adjust AC Setpoint (delta0 > 0)
  else if ((abs(delta0) > delta) && (abs(delta0) < 1.5*delta) && (delta0 > 0)) { // Low difference Fan Low; No ECO; Set Lower Temperature
     if (regime == 1) setpointerr+=1;
     else if (regime == 3) setpointerr-=1;
     else setpointerr = 2;
     regime = 2;
     //Serial.println("regime 2");
     if (ModbusSlaveRegisters1[2] != 2)
        SendCommand(SlaveModbusAdd1, 2, 2); // Fan Low
     if (ModbusSlaveRegisters2[2] != 2)
        SendCommand(SlaveModbusAdd2, 2, 2); // Fan Low
     if (ModbusSlaveRegisters1[64] != 0)
        SendCommand(SlaveModbusAdd1, 64, 0); // No ECO
     if (ModbusSlaveRegisters2[64] != 0)
        SendCommand(SlaveModbusAdd2, 64, 0); // No ECO
     if (ModbusSlaveRegisters1[4] != setpoint - setpointerr )
        SendCommand(SlaveModbusAdd1, 4, setpoint - setpointerr ); // Set setpoint - setpointerr Temp
     if (ModbusSlaveRegisters2[4] != setpoint - setpointerr )
        SendCommand(SlaveModbusAdd2, 4, setpoint - setpointerr ); // Set setpoint - setpointerr Temp
  }  
  // Adjust AC Setpoint (delta0 > 0)
  else if ((abs(delta0) < delta) && (abs(delta0) > 0.5*delta) && (delta0 > 0)) {// No difference: Fan Quiet; ECO; Set setpoint Temperature
     if (regime == 0) setpointerr+=1;
     else if (regime == 2) setpointerr-=1;
     else setpointerr = 3;
     regime = 1;
     //Serial.println("regime 1");
     if (ModbusSlaveRegisters1[2] != 1)
        SendCommand(SlaveModbusAdd1, 2, 1); // Fan Quiet
     if (ModbusSlaveRegisters2[2] != 1)
        SendCommand(SlaveModbusAdd2, 2, 1); // Fan Quiet
     if (ModbusSlaveRegisters1[64] != 1)
        SendCommand(SlaveModbusAdd1, 64, 1); // ECO
     if (ModbusSlaveRegisters2[64] != 1)
        SendCommand(SlaveModbusAdd2, 64, 1); // ECO
     if (ModbusSlaveRegisters1[4] != setpoint - setpointerr)
        SendCommand(SlaveModbusAdd1, 4, setpoint - setpointerr); // Set setpoint - setpointerr Temp
     if (ModbusSlaveRegisters2[4] != setpoint - setpointerr )
        SendCommand(SlaveModbusAdd2, 4, setpoint - setpointerr); // Set setpoint - setpointerr Temp
  }
  // Adjust AC Setpoint (delta0 > 0)
  else if ((abs(delta0) < 0.5*delta) && (delta0 > 0)) {// Lowest difference: Fan Quiet; ECO; Set setpoint Temperature
    if (regime == 1) setpointerr-=1;
    else setpointerr = 0;
     regime = 0;
     //Serial.println("regime 0");
     //Serial.println(abs(delta0));Serial.println(delta);
     if (ModbusSlaveRegisters1[2] != 1)
        SendCommand(SlaveModbusAdd1, 2, 1); // Fan Quiet
     if (ModbusSlaveRegisters2[2] != 1)
        SendCommand(SlaveModbusAdd2, 2, 1); // Fan Quiet
     if (ModbusSlaveRegisters1[64] != 1)
        SendCommand(SlaveModbusAdd1, 64, 1); // ECO
     if (ModbusSlaveRegisters2[64] != 1)
        SendCommand(SlaveModbusAdd2, 64, 1); // ECO
     if (ModbusSlaveRegisters1[4] != setpoint - setpointerr )
        SendCommand(SlaveModbusAdd1, 4, setpoint - setpointerr ); // Set setpoint Temp
     if (ModbusSlaveRegisters2[4] != setpoint - setpointerr )
        SendCommand(SlaveModbusAdd2, 4, setpoint - setpointerr ); // Set setpoint Temp
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Adjust AC Setpoint (delta0 < 0)
  else if ((abs(delta0) > 2*delta) && (delta0 < 0)) { // High difference: Fan High; No ECO; Set Min Temperature
     regime = 5;
     setpointerr = 4;
     //Serial.println("regime 5");
     if (ModbusSlaveRegisters1[2] != 4)
        SendCommand(SlaveModbusAdd1, 2, 4); // Fan High
     if (ModbusSlaveRegisters2[2] != 4)
        SendCommand(SlaveModbusAdd2, 2, 4); // Fan High
     if (ModbusSlaveRegisters1[64] != 0)
        SendCommand(SlaveModbusAdd1, 64, 0); // No ECO
     if (ModbusSlaveRegisters2[64] != 0)
        SendCommand(SlaveModbusAdd2, 64, 0); // No ECO
     if (ModbusSlaveRegisters1[4] != max(setmax,setpoint+setpointerr))
        SendCommand(SlaveModbusAdd1, 4, max(setmax,setpoint+setpointerr)); // Set Max Temp
     if (ModbusSlaveRegisters2[4] != max(setmax,setpoint+setpointerr))
        SendCommand(SlaveModbusAdd2, 4, max(setmax,setpoint+setpointerr)); // Set Max Temp
  }
  // Adjust AC Setpoint (delta0 < 0)
  else if ((abs(delta0) > 1.5*delta) && (abs(delta0) < 2*delta) && (delta0 < 0)) { // Medium difference Fan Medium; No ECO; Set Lower Temperature
     if (regime == 7) setpointerr+=1;
     else if (regime == 5) setpointerr-=1;
     else setpointerr = 3;
     regime = 6;
     //Serial.println("regime 6");
     if (ModbusSlaveRegisters1[2] != 3)
        SendCommand(SlaveModbusAdd1, 2, 3); // Fan Medium
     if (ModbusSlaveRegisters2[2] != 3)
        SendCommand(SlaveModbusAdd2, 2, 3); // Fan Medium
     if (ModbusSlaveRegisters1[64] != 0)
        SendCommand(SlaveModbusAdd1, 64, 0); // No ECO
     if (ModbusSlaveRegisters2[64] != 0)
        SendCommand(SlaveModbusAdd2, 64, 0); // No ECO
     if (ModbusSlaveRegisters1[4] != setpoint + setpointerr)
        SendCommand(SlaveModbusAdd1, 4, setpoint + setpointerr); // Set setpoint + setpointerr Temp
     if (ModbusSlaveRegisters2[4] != setpoint + setpointerr)
        SendCommand(SlaveModbusAdd2, 4, setpoint + setpointerr ); // Set setpoint + setpointerr Temp
  } 
  // Adjust AC Setpoint (delta0 < 0)
  else if ((abs(delta0) > delta) && (abs(delta0) < 1.5*delta) && (delta0 < 0)) { // Low difference Fan Low; No ECO; Set Lower Temperature
     if (regime == 8) setpointerr+=1;
     else if (regime == 6) setpointerr-=1;
     else setpointerr = 2;
     regime = 7;
     //Serial.println("regime 7");
     if (ModbusSlaveRegisters1[2] != 2)
        SendCommand(SlaveModbusAdd1, 2, 2); // Fan Low
     if (ModbusSlaveRegisters2[2] != 2)
        SendCommand(SlaveModbusAdd2, 2, 2); // Fan Low
     if (ModbusSlaveRegisters1[64] != 0)
        SendCommand(SlaveModbusAdd1, 64, 0); // No ECO
     if (ModbusSlaveRegisters2[64] != 0)
        SendCommand(SlaveModbusAdd2, 64, 0); // No ECO
     if (ModbusSlaveRegisters1[4] != setpoint + setpointerr)
        SendCommand(SlaveModbusAdd1, 4, setpoint + setpointerr ); // Set setpoint + setponterr Temp
     if (ModbusSlaveRegisters2[4] != setpoint + setpointerr)
        SendCommand(SlaveModbusAdd2, 4, setpoint + setpointerr ); // Set setpoint + setpointerr Temp
  }  
  // Adjust AC Setpoint (delta0 < 0)
  else if ((abs(delta0) < delta) && (abs(delta0) > 0.5*delta) && (delta0 < 0)) {// No difference: Fan Quiet; ECO; Set setpoint Temperature
    if (regime == 9) setpointerr+=1;
    else if (regime == 7) setpointerr-=1;
    else setpointerr = 1;
    regime = 8;
    //Serial.println("regime 8");
     if (ModbusSlaveRegisters1[2] != 1)
        SendCommand(SlaveModbusAdd1, 2, 1); // Fan Quiet
     if (ModbusSlaveRegisters2[2] != 1)
        SendCommand(SlaveModbusAdd2, 2, 1); // Fan Quiet
     if (ModbusSlaveRegisters1[64] != 1)
        SendCommand(SlaveModbusAdd1, 64, 1); // ECO
     if (ModbusSlaveRegisters2[64] != 1)
        SendCommand(SlaveModbusAdd2, 64, 1); // ECO
     if (ModbusSlaveRegisters1[4] != setpoint + setpointerr)
        SendCommand(SlaveModbusAdd1, 4, setpoint + setpointerr); // Set setpoint + setpointerr Temp
     if (ModbusSlaveRegisters2[4] != setpoint + setpointerr)
        SendCommand(SlaveModbusAdd2, 4, setpoint + setpointerr); // Set setpoint + setpointerr Temp
  }
  // Adjust AC Setpoint (delta0 < 0)
  else if ((abs(delta0) < 0.5*delta) && (delta0 < 0) ) {// Lowest difference: Fan Quiet; ECO; Set setpoint Temperature
     if (regime == 8) setpointerr-=1;
     else setpointerr = 0;
     if (setpointerr < 0 ) setpointerr = 0;
     regime = 9;
     //Serial.println("regime 9");
     if (ModbusSlaveRegisters1[2] != 1)
        SendCommand(SlaveModbusAdd1, 2, 1); // Fan Quiet
     if (ModbusSlaveRegisters2[2] != 1)
        SendCommand(SlaveModbusAdd2, 2, 1); // Fan Quiet
     if (ModbusSlaveRegisters1[64] != 1)
        SendCommand(SlaveModbusAdd1, 64, 1); // ECO
     if (ModbusSlaveRegisters2[64] != 1)
        SendCommand(SlaveModbusAdd2, 64, 1); // ECO
     if (ModbusSlaveRegisters1[4] != setpoint + setpointerr)
        SendCommand(SlaveModbusAdd1, 4, setpoint + setpointerr); // Set setpoint + setpointerr Temp
     if (ModbusSlaveRegisters2[4] != setpoint + setpointerr)
        SendCommand(SlaveModbusAdd2, 4, setpoint + setpointerr); // Set setpoint + setpointerr Temp
  }
  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  Serial.print("setpoint=");Serial.println(setpoint);
  Serial.print("delta=");Serial.println(delta);
  Serial.print("regime=");Serial.println(regime);
  Serial.print("setpointerr=");Serial.println(setpointerr);
  Serial.println("/////////////////////////////////////////////////////////");
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
      int counter = 0;                                                             \
      do {                                                                         \
          testDHCP = Ethernet.maintain();                                          \
          client = server.available();                                             \
          counter++;                                                               \
      } while((!client || testDHCP % 2 == 1) && counter < 10);                     \
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
int hstol(String recv){
  char c[recv.length() + 1];
  recv.toCharArray(c, recv.length() + 1);
  return strtol(c, NULL, 16);
}
