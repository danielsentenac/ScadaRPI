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
  
 
  // start the Modbus server
  StartModbusServer();

}

void loop() {
  Serial.println("Loop...");
  /***********************************************************************************************************/
  // check DHCP lease
  Ethernet.maintain();
   modbus.update();
   if (holdingRegisters[10] !=0 )
     Serial.println("register 10 changed! ///////////////////////////////////////////////////////////////////////////");
 if (holdingRegisters[1] !=0 )
     Serial.println("register 1 changed! ///////////////////////////////////////////////////////////////////////////");
 
  delay(500);

  
}
void StartModbusServer()
{    
  Ethernet.begin(mac);
  // Init ModbusTCPSlave object
  modbus.begin();

  // Configure registers
  modbus.setHoldingRegisters(holdingRegisters, NB_HOLDING_REGISTERS);
  
 
  for (int i = 0; i < NB_HOLDING_REGISTERS; i++)
        holdingRegisters[i] = 0x00;
 

}

