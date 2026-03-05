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
uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x77, 0xAB};
IPAddress ip (192, 168, 224, 180 ); // Controllino-21 has this IP

// Define the ModbusTCPSlave object with port = 502
ModbusTCPSlave modbus(502);

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

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

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
       delay(10);
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
    
     holdingRegisters[index] = (hstol(address[num].substring(0,4)));
     //Serial.print("address register:");//Serial.print(String(index));//Serial.print(" ");
     //Serial.println(String(holdingRegisters[index]));

     // Set Temperature register
     //Serial.println(Temperature[num][i]);
     mbValue.fvalue = Temperature[num][i];
     holdingRegisters[++index] = mbValue.value[0];
     //Serial.print("Temperature register LSB ");//Serial.print(String(index));//Serial.print(" ");
     //Serial.println(String(mbValue.value[0]));
     holdingRegisters[++index] = mbValue.value[1];
     //Serial.print("Temperature register MSB ");//Serial.print(String(index));//Serial.print(" ");
     //Serial.println(String(mbValue.value[1]));
  
     // Set Humidity register
     //Serial.println(Humidity[num][i]);
     mbValue.fvalue = Humidity[num][i];
     holdingRegisters[++index] = mbValue.value[0];
     //Serial.print("Humidity register LSB ");//Serial.print(String(index));//Serial.print(" ");
     //Serial.println(String(mbValue.value[0]));
     holdingRegisters[++index] = mbValue.value[1];
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
          holdingRegisters[startmbreg+i] = regs[startaddr+i];
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
    if (holdingRegisters[RESETARD] == 0x01)
      resetArd();

    ///////////////////////////////////////////////////////////////////////////////
    // Get Relay Fan Status
    ///////////////////////////////////////////////////////////////////////////////
    if (digitalRead(RELAY_FAN) == HIGH)
       holdingRegisters[RELAY_FAN_STATUS_ADDR] = 0x01; // FAN ON
    else if (digitalRead(RELAY_FAN) == LOW)
       holdingRegisters[RELAY_FAN_STATUS_ADDR] = 0x02; // FAN OFF
    ///////////////////////////////////////////////////////////////////////////////
    // Get Setpoint & Delta Temperature
    ///////////////////////////////////////////////////////////////////////////////
    holdingRegisters[LOOP_SET_ADDR] = setpoint; // Writable Temperature Set Point Value
    holdingRegisters[LOOP_DELTA_ADDR] = delta; // Writable Temperature Delta Value
    holdingRegisters[LOOP_SETERR_ADDR] = setpointerr; // Readable Temperature Set Point error Value
    holdingRegisters[LOOP_FANERR_ADDR] = fanerr; // Writable Fan error Value
    holdingRegisters[LOOP_REGIME_ADDR] = regime; // Readable Regime Value
       
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
    // Listen to TCP Master
    //////////////////////////////////////////////////////////////////////////////    
    SendReceiveMaster();
    
    //////////////////////////////////////////////////////////////////////////////
    // Get temperatures & Humidities on the BUSes
    /////////////////////////////////////////////////////////////////////////////
    getMeasureBus(&ds1,0);
   
    //////////////////////////////////////////////////////////////////////////////
    // Get temperatures & Humidities on the BUSes
    /////////////////////////////////////////////////////////////////////////////
    getMeasureBus(&ds2,1);

    //////////////////////////////////////////////////////////////////////////////    
    // Listen to TCP Master
    //////////////////////////////////////////////////////////////////////////////    
    SendReceiveMaster();
    
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
    if (holdingRegisters[LOOP_ONOFF_ADDR] == 0x01)
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
   Ethernet.begin(mac);
  // Init ModbusTCPSlave object
  modbus.begin();

  // Configure registers
  modbus.setHoldingRegisters(holdingRegisters, NB_HOLDING_REGISTERS);
  
  // Init registers
  for (int i = 0 ; i < NB_HOLDING_REGISTERS ; i++) 
     holdingRegisters[i] = 0x00;
}
 
void SendReceiveMaster()
{    
    // Process modbus requests
     modbus.update();
             
    // Check Modbus registers for commands //////////////////////////////////////////////// AC Unit 1
    if (holdingRegisters[AC1_ONOFF_ADDR] != ModbusSlaveRegisters1[0]) {
      Serial.print("Change register ");Serial.print(AC1_ONOFF_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC1_ONOFF_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[0]);
      SendCommand(SlaveModbusAdd1, 0, holdingRegisters[AC1_ONOFF_ADDR]);
    }
    if (holdingRegisters[AC1_MODE_ADDR] != ModbusSlaveRegisters1[1]){
      Serial.print("Change register ");Serial.print(AC1_MODE_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC1_MODE_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[1]);
      SendCommand(SlaveModbusAdd1, 1, holdingRegisters[AC1_MODE_ADDR]);
    }
    if (holdingRegisters[AC1_FAN_ADDR] != ModbusSlaveRegisters1[2]){
      Serial.print("Change register ");Serial.print(AC1_FAN_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC1_FAN_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[2]);
      SendCommand(SlaveModbusAdd1, 2, holdingRegisters[AC1_FAN_ADDR]);
    }
    if (holdingRegisters[AC1_VANE_ADDR] != ModbusSlaveRegisters1[3]){
      Serial.print("Change register ");Serial.print(AC1_VANE_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC1_VANE_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[3]);
      SendCommand(SlaveModbusAdd1, 3, holdingRegisters[AC1_VANE_ADDR]);
    }
    if (holdingRegisters[AC1_SET_ADDR] != ModbusSlaveRegisters1[4]){
      Serial.print("Change register ");Serial.print(AC1_SET_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC1_SET_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[4]);
      SendCommand(SlaveModbusAdd1, 4, holdingRegisters[AC1_SET_ADDR]);
    }
    if (holdingRegisters[AC1_ECO_ADDR] != ModbusSlaveRegisters1[64]){
      Serial.print("Change register ");Serial.print(AC1_ECO_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC1_ECO_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters1[64]);
      SendCommand(SlaveModbusAdd1, 64, holdingRegisters[AC1_ECO_ADDR]);
    }

     // Check Modbus registers for commands //////////////////////////////////////////////// AC Unit 1
    if (holdingRegisters[AC2_ONOFF_ADDR] != ModbusSlaveRegisters2[0]) {
      Serial.print("Change register ");Serial.print(AC2_ONOFF_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC2_ONOFF_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[0]);
      SendCommand(SlaveModbusAdd2, 0, holdingRegisters[AC2_ONOFF_ADDR]);
    }
    if (holdingRegisters[AC2_MODE_ADDR] != ModbusSlaveRegisters2[1]){
      Serial.print("Change register ");Serial.print(AC2_MODE_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC2_MODE_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[1]);
      SendCommand(SlaveModbusAdd2, 1, holdingRegisters[AC2_MODE_ADDR]);
    }
    if (holdingRegisters[AC2_FAN_ADDR] != ModbusSlaveRegisters2[2]){
      Serial.print("Change register ");Serial.print(AC2_FAN_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC2_FAN_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[2]);
      SendCommand(SlaveModbusAdd2, 2, holdingRegisters[AC2_FAN_ADDR]);
    }
    if (holdingRegisters[AC2_VANE_ADDR] != ModbusSlaveRegisters2[3]){
      Serial.print("Change register ");Serial.print(AC2_VANE_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC2_VANE_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[3]);
      SendCommand(SlaveModbusAdd2, 3, holdingRegisters[AC2_VANE_ADDR]);
    }
    if (holdingRegisters[AC2_SET_ADDR] != ModbusSlaveRegisters2[4]){
      Serial.print("Change register ");Serial.print(AC2_SET_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC2_SET_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[4]);
      SendCommand(SlaveModbusAdd2, 4, holdingRegisters[AC2_SET_ADDR]);
    }
    if (holdingRegisters[AC2_ECO_ADDR] != ModbusSlaveRegisters2[64]){
      Serial.print("Change register ");Serial.print(AC2_ECO_ADDR);Serial.print(" value:");
      Serial.print(holdingRegisters[AC2_ECO_ADDR]);Serial.print(" from:");
      Serial.println(ModbusSlaveRegisters2[64]);
      SendCommand(SlaveModbusAdd2, 64, holdingRegisters[AC2_ECO_ADDR]);
    }

    // Relay command
    if (holdingRegisters[RELAY_FAN_CMD_ADDR] == 0x01 && RELAY_FAN_RESET == false) {
       digitalWrite(RELAY_FAN,HIGH); // OPEN RELAY (FAN ON)
       RELAY_FANtime = millis();
       RELAY_FAN_RESET = true;
    }
    else if (holdingRegisters[RELAY_FAN_CMD_ADDR] == 0x02 && RELAY_FAN_RESET == false) {
       digitalWrite(RELAY_FAN,LOW); // CLOSE RELAY (FAN OFF)
       RELAY_FANtime = millis();
       RELAY_FAN_RESET = true;
    }
    // Set Point command
    if (holdingRegisters[LOOP_SET_ADDR] != setpoint) 
       if (holdingRegisters[LOOP_SET_ADDR] > setmin && 
           holdingRegisters[LOOP_SET_ADDR] < setmax)
           setpoint =  holdingRegisters[LOOP_SET_ADDR];// Set Set new Set Point
    // Set delta command
    if (holdingRegisters[LOOP_DELTA_ADDR] != delta) 
       delta =  holdingRegisters[LOOP_DELTA_ADDR];// Set Set new Delta 
    // Set fanerr command
    if (holdingRegisters[LOOP_FANERR_ADDR] != fanerr) 
       fanerr =  holdingRegisters[LOOP_FANERR_ADDR];// Set Set new Fanerr 
       
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
       holdingRegisters[RELAY_FAN_CMD_ADDR] = 0x00;
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
     if (ModbusSlaveRegisters1[4] != setmin)
        SendCommand(SlaveModbusAdd1, 4, setpoint - setpointerr); // Set Min Temp
     if (ModbusSlaveRegisters2[4] != setmin)
        SendCommand(SlaveModbusAdd2, 4, setpoint - setpointerr); // Set Min Temp
  }
  // Adjust AC Setpoint (delta0 > 0)
  else if ((abs(delta0) > 1.5*delta) && (abs(delta0) <= 2*delta) && (delta0 > 0)) { // Medium difference Fan Medium; No ECO; Set Lower Temperature
     regime = 3;
     setpointerr = 3;
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
  else if ((abs(delta0) > delta) && (abs(delta0) <= 1.5*delta) && (delta0 > 0)) { // Low difference Fan Low; No ECO; Set Lower Temperature
     regime = 2;
     setpointerr = 2;
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
  else if ((abs(delta0) <= delta) && (abs(delta0) > 0.5*delta) && (delta0 > 0)) {// No difference: Fan Quiet; ECO; Set setpoint Temperature
     regime = 1;
     setpointerr = 1;
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
  else if ((abs(delta0) <= 0.5*delta) && (delta0 > 0)) {// Lowest difference: Fan Quiet; ECO; Set setpoint Temperature
    regime = 0;
     setpointerr = 0;
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
     if (ModbusSlaveRegisters1[4] != setmax)
        SendCommand(SlaveModbusAdd1, 4, setpoint + setpointerr); // Set Max Temp
     if (ModbusSlaveRegisters2[4] != setmax)
        SendCommand(SlaveModbusAdd2, 4, setpoint + setpointerr); // Set Max Temp
  }
  // Adjust AC Setpoint (delta0 < 0)
  else if ((abs(delta0) > 1.5*delta) && (abs(delta0) <= 2*delta) && (delta0 < 0)) { // Medium difference Fan Medium; No ECO; Set Lower Temperature
     regime = 6;
     setpointerr = 3;
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
  else if ((abs(delta0) > delta) && (abs(delta0) <= 1.5*delta) && (delta0 < 0)) { // Low difference Fan Low; No ECO; Set Lower Temperature
     regime = 7;
     setpointerr = 2;
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
  else if ((abs(delta0) <= delta) && (abs(delta0) > 0.5*delta) && (delta0 < 0)) {// No difference: Fan Quiet; ECO; Set setpoint Temperature
     regime = 8;
     setpointerr = 1;
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
  else if ((abs(delta0) <= 0.5*delta) && (delta0 < 0) ) {// Lowest difference: Fan Quiet; ECO; Set setpoint Temperature
     regime = 9;
     setpointerr = 0;
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

int hstol(String recv){
  char c[recv.length() + 1];
  recv.toCharArray(c, recv.length() + 1);
  return strtol(c, NULL, 16);
}
