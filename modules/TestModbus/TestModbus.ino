/* Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include <ModbusTCPSlave.h> // Modbus2 library
#include <Controllino.h> // Controllino library

/*
 *  This part is the Arduino sketch code
 */ 
// Ethernet config
// This is the WI BIG VALVE
byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x78, 0xAB };
IPAddress ip( 192, 168, 224, 181 ); // Controllino-22 has this IP

// Define the ModbusTCPSlave object with port = 502
ModbusTCPSlave modbus(502);

// Modbus registers
#define NB_HOLDING_REGISTERS 10

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

void(*resetArd) (void) = 0; //declare reset function @ address 0

// Buffer for Serial purposes
size_t length = 64;
char *buffer = (char*)malloc(sizeof(char) * length); 

// Conversion tool
typedef union {
    float fvalue;
    uint16_t value[2];
} FloatUint16;
  

void setup() {
  
  // Open serial communication for Com port.
  Serial.begin(9600);

  // start the Modbus server
  StartModbusServer();

  Serial.println("Modbus started");
 
}
 
void loop() {
 /***********************************************************************************************************/
  // check DHCP lease
  Ethernet.maintain();
 
  SendReceiveMaster();
  Serial.println("Modbus loop");
  delay(1000);
}

  /***********************************************************************************************************/
  /* Perform some internal logics */
  /***********************************************************************************************************/
 
void StartModbusServer()
{    
  while (Ethernet.begin(mac) == 0) {
   delay(100);
   Serial.println("Failed to configure Ethernet using DHCP");
  }
  
  // Init ModbusTCPSlave object
  modbus.begin();

  // Configure registers
  modbus.setHoldingRegisters(holdingRegisters, NB_HOLDING_REGISTERS);

}
void SendReceiveMaster()
{    

  // Process modbus requests
  modbus.update();
  FloatUint16 conversion;
  float temp = 7.81;
  conversion.fvalue = temp;
  Serial.print("Temp = ");Serial.println(temp); 
  Serial.print("reg 0 = "); Serial.println(conversion.value[0]);
  Serial.print("reg 1 = "); Serial.println(conversion.value[1]);
  
  
 
}

