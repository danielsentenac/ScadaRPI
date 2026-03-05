/* Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include <ModbusTCPSlave.h> // Modbus2 library
#include <Controllino.h> // Controllino library

/*
 *  This part is the Arduino sketch code
 */ 

// This is the Venting in CB
 
//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x74, 0xAB };
//IPAddress ip( 192, 168, 224, 170 ); // Controllino-18 has this IP (VENTING VALVE NI)

byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x75, 0xAB };
IPAddress ip( 192, 168, 224, 171 ); // Controllino-19 has this IP (VENTING VALVE WI)

// Define the ModbusTCPSlave object with port = 502
ModbusTCPSlave modbus(502);

// digital pins (INTPUTS)
#define FIRST_CLOSE_STATUS CONTROLLINO_A0  // FIRST VALVE CLOSE STATUS
#define FIRST_OPEN_STATUS CONTROLLINO_A1  // FIRST VALVE OPEN STATUS
#define SECOND_CLOSE_STATUS CONTROLLINO_A2  // SECOND VALVE CLOSE STATUS
#define SECOND_OPEN_STATUS CONTROLLINO_A3  // SECOND VALVE OPEN STATUS

// Modbus registers
#define NB_HOLDING_REGISTERS 10
#define FIRST_VALVE_STATUS_ADDR 0
#define SECOND_VALVE_STATUS_ADDR 1
#define ARD_RESET_ADDR 2

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

void(*resetArd) (void) = 0; //declare reset function @ address 0

// Buffer for Serial purposes
size_t length = 64;
char *buffer = (char*)malloc(sizeof(char) * length); 
    
void setup() {
  
  // Open serial communication for Com port.
  Serial.begin(9600);

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
  if (holdingRegisters[ARD_RESET_ADDR] == 0x01)
    resetArd();
  /***********************************************************************************************************/
  Serial.println("Loop...");
  SendReceiveMaster();
  delay(500);
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
  
  // Digital inputs initialization and assignation
  pinMode(FIRST_OPEN_STATUS, INPUT);                                // sets the digital pin as input for Open Valve STATUS
  pinMode(FIRST_CLOSE_STATUS, INPUT);                               // sets the digital pin as input for Close Valve STATUS
  holdingRegisters[FIRST_VALVE_STATUS_ADDR] = 0x00; // RESET
  pinMode(SECOND_OPEN_STATUS, INPUT);                                // sets the digital pin as input for Open Valve STATUS
  pinMode(SECOND_CLOSE_STATUS, INPUT);                               // sets the digital pin as input for Close Valve STATUS
  holdingRegisters[SECOND_VALVE_STATUS_ADDR] = 0x00; // RESET
  
  // register for Arduino reset
  holdingRegisters[ARD_RESET_ADDR] = 0x00;     // Arduino Global Reset Status

}
void SendReceiveMaster()
{    
   
  // Process modbus requests
      modbus.update();
  /***********************************************************************************************************/
  /* Update First Valve position STATUS register (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(FIRST_OPEN_STATUS) == LOW && digitalRead(FIRST_CLOSE_STATUS) == HIGH)
     holdingRegisters[FIRST_VALVE_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(FIRST_CLOSE_STATUS) == LOW && digitalRead(FIRST_OPEN_STATUS) == HIGH)
     holdingRegisters[FIRST_VALVE_STATUS_ADDR] = 0x02; // CLOSED VALVE
  else {
     holdingRegisters[FIRST_VALVE_STATUS_ADDR] = 0x00; // MOVING VALVE
  }
  /***********************************************************************************************************/
  /* Update Second Valve position STATUS register (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(SECOND_OPEN_STATUS) == LOW && digitalRead(SECOND_CLOSE_STATUS) == HIGH)
     holdingRegisters[SECOND_VALVE_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(SECOND_CLOSE_STATUS) == LOW && digitalRead(SECOND_OPEN_STATUS) == HIGH)
     holdingRegisters[SECOND_VALVE_STATUS_ADDR] = 0x02; // CLOSED VALVE
  else {
     holdingRegisters[SECOND_VALVE_STATUS_ADDR] = 0x00; // MOVING VALVE
  }
  
}

