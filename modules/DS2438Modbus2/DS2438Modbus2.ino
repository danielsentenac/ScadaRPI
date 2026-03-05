/*
  Arduino Leonardo Eth acting as a Modbus Server,
  connected to DS2438 humidity & temperature sensors, 
  4x channels 4/20mA current mezzanine
  4x channels analog inputs (Airflow sensors)
 */

#include <ModbusTCPSlave.h> // Modbus2 library
#include <currentLoop.h> // 4-20mA board library
#include <OneWire.h> // OneWire library
#include <DS2438.h> 

// Enter a MAC address and IP address for your controller below.
// The IP address will be dependent on your local network:

uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5E, 0x31 };
IPAddress ip( 192, 168, 224, 176 ); // Arduino-16 has this IP

// Define the ModbusTCPSlave object with port = 502
ModbusTCPSlave modbus(502);

// 4-20mA Board Sensor channels
#define CH_CUR1 CHANNEL1
#define CH_CUR2 CHANNEL2
#define CH_CUR3 CHANNEL3
#define CH_CUR4 CHANNEL4

// Analog channels (Airflow sensors)
#define CH_VOLT1 A5
#define CH_VOLT2 A4
#define CH_VOLT3 A3
#define CH_VOLT4 A2


// Mean calculation count
#define CH_VOLT_CNT 10

// Calibration factors
#define A_CH_CUR1 1
#define B_CH_CUR1 0

#define A_CH_CUR2 1
#define B_CH_CUR2 0

#define A_CH_CUR3 1
#define B_CH_CUR3 0

#define A_CH_CUR4 1
#define B_CH_CUR4 0

#define A_CH_VOLT1 10
#define B_CH_VOLT1 -1000

#define A_CH_VOLT2 10
#define B_CH_VOLT2 -1000

#define A_CH_VOLT3 10
#define B_CH_VOLT3 -1000

#define A_CH_VOLT4 10
#define B_CH_VOLT4 -1000

#define DS24B38 0x26      // 1-Wire DS2438 address (DS2438 + HIH5030 sensor)
#define DIGITAL_ONEWIRE A0 // digital address used for bus 1-Wire
#define BUS_SENSOR_MAX 5  // Max number of sensors

OneWire ds7(DIGITAL_ONEWIRE); // OneWire object

// Modbus addresses limits
#define NB_HOLDING_REGISTERS 35
#define CH_CUR1_ADDR 0
#define CH_CUR2_ADDR 2
#define CH_CUR3_ADDR 4
#define CH_CUR4_ADDR 6
#define CH_ID1_ADDR 8
#define CH_HUM1_ADDR 9
#define CH_TEMP1_ADDR 11
#define CH_ID2_ADDR 13
#define CH_HUM2_ADDR 14
#define CH_TEMP2_ADDR 16
#define CH_ID3_ADDR 18
#define CH_HUM3_ADDR 19
#define CH_TEMP3_ADDR 21
#define CH_ID4_ADDR 23
#define CH_HUM4_ADDR 24
#define CH_TEMP4_ADDR 26
#define CH_ID5_ADDR 28
#define CH_HUM5_ADDR 29
#define CH_TEMP5_ADDR 31
#define ARD_RESET_ADDR 33

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

void(*resetArd) (void) = 0; //declare reset function @ address 0

void getAddress(String &id, byte addr[8]) {
  for (uint8_t i = 1; i < 7; i++) {
    if (addr[i] < 0x10) id += String("0");;
    id += String(addr[i], HEX);
  }
}
void getDigitalIn(String &DigitalInStr, String &DigitalInBisStr, byte addr[8]){
  byte data[9];
  // data : Data from scratchpad
  // addr : Module address 
  float DigitalIn;
  float temperature;
  
  if (OneWire::crc8(addr, 7) != addr[7]) {// Check address integrity
    DigitalInStr = String("address failed");
    return;
  }
      
  else if (addr[0] != DS24B38) {// Check DS24B38 module
    DigitalInStr = String("no DS24B38");
    return;
  }
  DS2438 ds2438(&ds7, addr);
  ds2438.begin();
  ds2438.update();
  if (ds2438.isError() || ds2438.getVoltage(DS2438_CHA) == 0.0) {
      DigitalInStr = String("Error reading from DS2438 device");
      return;
    }
  temperature = ds2438.getTemperature();
  float rh = (ds2438.getVoltage(DS2438_CHA) / ds2438.getVoltage(DS2438_CHB) - 0.1515) / 0.00636;
  DigitalIn = (float)(rh / (1.0546 - 0.00216 * temperature));
  if (DigitalIn < 0.0)
     DigitalIn = 0.0;
  if (DigitalIn > 100.0)
     DigitalIn = 100.0;

  DigitalInBisStr = String(temperature);
  DigitalInStr = String(DigitalIn);

}

void StartModbusServer()
{    
  // Init Ethernet
  Ethernet.begin(mac);

  // Init ModbusTCPSlave object
  modbus.begin();

  // Configure registers
  modbus.setHoldingRegisters(holdingRegisters, NB_HOLDING_REGISTERS);
  
  // Init registers
  for (int i = 0 ; i < NB_HOLDING_REGISTERS ; i++) 
     holdingRegisters[i] = 0x00;
}

void setup() {
  
  // Switch OFF the 24V DC-DC converter
  //sensorBoard.OFF();
  //delay(100);

  // 1-wire bus reset
  ds7.reset();             
  
  // Open serial communications and wait for port to open:
  Serial.begin(9600);
//  Serial.println("Serial Console Started...");

  // start the Modbus server
  StartModbusServer();
//  Serial.println("Modbus Server Started...");
  
}

void loop() {

   // check DHCP lease
  Ethernet.maintain();
   /***********************************************************************************************************/
  // Check Reset Status
  /***********************************************************************************************************/
  if (holdingRegisters[ARD_RESET_ADDR] == 0x01)
    resetArd();
  /***********************************************************************************************************/
  doLoop();
  // Process modbus requests
  modbus.update();
  delay(200);
 // Serial.println("Loop...");
  
}
void doLoop()
{    
  typedef union {
     float fvalue;
     uint16_t value[2];
  } FloatUint16;
  FloatUint16 mbValue;

   // Get Channels
  float CurrentIn1 = 0;
  const char* error_C1;
  float CurrentIn2 = 0;
  const char* error_C2;
  float CurrentIn3 = 0;
  const char* error_C3;
  float CurrentIn4 = 0;
  const char* error_C4;
  float AnalogIn1 = 0;
  float AnalogIn2 = 0;
  float AnalogIn3 = 0;
  float AnalogIn4 = 0;
  String DigitalIn[BUS_SENSOR_MAX];
  String DigitalInBis[BUS_SENSOR_MAX];
  int   busCnt = 0;
  byte  addr[8] ={0,0,0,0,0,0,0,0};
  String address[BUS_SENSOR_MAX];

  for (int i = 0 ; i < BUS_SENSOR_MAX; i++) {
     DigitalIn[i] = "";
     DigitalInBis[i] = "";
  }
  while (ds7.search(addr) == 1 && busCnt < BUS_SENSOR_MAX) { // Scan 1-Wire module
    getAddress(address[busCnt],addr);
    getDigitalIn(DigitalIn[busCnt],DigitalInBis[busCnt],addr);
    delay(10);
    busCnt++;
  }
 
  if (sensorBoard.isConnected(CH_CUR1)) {
     CurrentIn1 = sensorBoard.readCurrent(CH_CUR1);
     CurrentIn1 = CurrentIn1 * A_CH_CUR1 + B_CH_CUR1;
     error_C1 = "OK";
  }
  else
     error_C1 = "not connected";

  if (sensorBoard.isConnected(CH_CUR2)) {
     CurrentIn2 = sensorBoard.readCurrent(CH_CUR2);
     CurrentIn2 = CurrentIn2 * A_CH_CUR2 + B_CH_CUR2;
     error_C2 = "OK";
  }
  else
     error_C2 = "not connected";

  if (sensorBoard.isConnected(CH_CUR3)) {
     CurrentIn3 = sensorBoard.readCurrent(CH_CUR3);
     CurrentIn3 = CurrentIn3 * A_CH_CUR2 + B_CH_CUR2;
     error_C3 = "OK";
  }
  else
     error_C3 = "not connected";

  if (sensorBoard.isConnected(CH_CUR4)) {
     CurrentIn4 = sensorBoard.readCurrent(CH_CUR4);
     CurrentIn4 = CurrentIn4 * A_CH_CUR4 + B_CH_CUR4;
     error_C4 = "OK";
  }
  else
     error_C4 = "not connected";

  // Average AnalogIn values
  for (int i = 0 ; i < CH_VOLT_CNT; i++) {
   AnalogIn1 += analogRead(CH_VOLT1);
   AnalogIn2 += analogRead(CH_VOLT2);
   AnalogIn3 += analogRead(CH_VOLT3);
   AnalogIn4 += analogRead(CH_VOLT4);
   delay(10); // sensor max response is 6ms
  }
  AnalogIn1/=CH_VOLT_CNT;
  AnalogIn2/=CH_VOLT_CNT;
  AnalogIn3/=CH_VOLT_CNT;
  AnalogIn4/=CH_VOLT_CNT;
  
  AnalogIn1 = (AnalogIn1 * A_CH_VOLT1 + B_CH_VOLT1) * 1e-3;
  AnalogIn2 = (AnalogIn2 * A_CH_VOLT2 + B_CH_VOLT2) * 1e-3;
  AnalogIn3 = (AnalogIn3 * A_CH_VOLT3 + B_CH_VOLT3) * 1e-3;
  AnalogIn4 = (AnalogIn4 * A_CH_VOLT4 + B_CH_VOLT4) * 1e-3;

  
    // Store values in registers
  mbValue.fvalue = CurrentIn1;
  holdingRegisters[0] = mbValue.value[0]; 
  holdingRegisters[1] = mbValue.value[1];
  mbValue.fvalue = CurrentIn2;
  holdingRegisters[2] = mbValue.value[0]; 
  holdingRegisters[3] = mbValue.value[1];
  mbValue.fvalue = CurrentIn3;
  holdingRegisters[4] = mbValue.value[0]; 
  holdingRegisters[5] = mbValue.value[1];
  mbValue.fvalue = CurrentIn4;
  holdingRegisters[6] = mbValue.value[0]; 
  holdingRegisters[7] = mbValue.value[1];
  for (int i = 0; i < BUS_SENSOR_MAX;i++) {
    //Serial.print(address[i]);Serial.print(" ");
    int index = 8+5*i;
    if (address[i].length() > 4) {
     //  Serial.println(address[i].substring(0,4));
       holdingRegisters[index] = (hstol(address[i].substring(0,4)));
     }
    else
       holdingRegisters[index] = 0x00;
   /* Serial.print("register ");Serial.print(String(index));Serial.print(" ");Serial.println(String(holdingRegisters[index]));
    Serial.print(DigitalIn[i]);Serial.println(" ");*/
    if (DigitalIn[i].length() > 0)
      mbValue.fvalue = DigitalIn[i].toFloat();
    else
      mbValue.fvalue = 0x00;
    holdingRegisters[++index] = mbValue.value[0];
   // Serial.print("register ");Serial.print(String(index));Serial.print(" ");Serial.println(String(mbValue.value[0]));
    holdingRegisters[++index] = mbValue.value[1];
   // Serial.print("register ");Serial.print(String(index));Serial.print(" ");Serial.println(String(mbValue.value[1]));
   // Serial.println(DigitalInBis[i]);
    if (DigitalInBis[i].length() > 0)
      mbValue.fvalue = DigitalInBis[i].toFloat();
    else
      mbValue.fvalue = 0x00;
    holdingRegisters[++index] = mbValue.value[0];
   // Serial.print("register ");Serial.print(String(index));Serial.print(" ");Serial.println(String(mbValue.value[0]));
    holdingRegisters[++index] = mbValue.value[1];
  //  Serial.print("register ");Serial.print(String(index));Serial.print(" ");Serial.println(String(mbValue.value[1]));
  }
}
// Utility function (string to hex)
int hstol(String recv){
  char c[recv.length() + 1];
  recv.toCharArray(c, recv.length() + 1);
  return strtol(c, NULL, 16);
}
