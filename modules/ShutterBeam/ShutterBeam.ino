/* Modbus Server
 A modbus server to control the beam shutter using Leonardo Eth board.

 Shutter wiring (see schematics):
   D6 = CLOSE command output, initialized HIGH (5V). Close pulse = D6 goes LOW then back HIGH.
   D7 = OPEN  command output, initialized LOW  (0V). Open  pulse = D7 goes HIGH then back LOW.
   A0 = OPEN  status input  (A0=1, A1=0 -> SHUTTER OPEN)
   A1 = CLOSE status input  (A0=0, A1=1 -> SHUTTER CLOSED)

 Modbus interface (single Status + Command register, like a valve):
   - Holding register 0 (SHUTTER_ST_ADDR)  : status  1=OPEN, 2=CLOSED, 0=MOVING/UNKNOWN
   - Holding register 1 (SHUTTER_CMD_ADDR) : command 1=OPEN, 2=CLOSE
   - Holding register 2 (ARD_RESET_ADDR)   : write 1 to reboot the Arduino

 Open/Close handling reproduces exactly the V21 valve scheme of Tubes_Pi_Mod_1:
   UpdateIOFromModbus() triggers the D6/D7 pulse and arms SHUTTER_RESET; ResetAndCheck()
   ends the pulse and resets the command register after reset_wait, then (after an OPEN)
   arms SHUTTER_CHECK to verify the move within check_wait; UpdateModbusFromIO() derives
   the status from A0/A1 and re-issues CLOSE if the shutter drifts from OPEN to CLOSED.
 */

#include <ModbusTCPSlave.h> // Modbus2 library (arduino-Tools40)

/*
 *  This part is the Arduino sketch code
 */
// Ethernet config
byte mac[] = { 0x96, 0xA2, 0xDA, 0x10, 0x5F, 0xD3 }; // This is mac of dns name: "shutterbeam1" arduino Leonardo ETH board 
IPAddress ip( 192, 168, 224, 190 ); // shutterbeam1 static fallback IP

/*
 *  The waiting time for a DHCP lease before falling back to the static IP
 */
long dhcp_timeout = 15000;

/* Records whether the address came from DHCP (true) or the static fallback (false) */
bool dhcp_ok = false;

/*
 * ARDUINO I/O ASSIGNATION
 */
// digital pins (OUTPUTS)
#define SHUTTER_OPEN_CMD   7   //(Open Command Shutter, D7 idle LOW, pulse HIGH)
#define SHUTTER_CLOSE_CMD  6   //(Close Command Shutter, D6 idle HIGH, pulse LOW)
// digital pins (INPUTS)
#define SHUTTER_OPEN_STATUS   A0  // SHUTTER OPEN STATUS (STATO OPEN)
#define SHUTTER_CLOSE_STATUS  A1  // SHUTTER CLOSE STATUS (STATO CLOSE)

// Define the ModbusTCPSlave object with port = 502
ModbusTCPSlave modbus(502);

// Modbus registers (single ST + CMD register per shutter, like a valve)
#define NB_HOLDING_REGISTERS 3
#define SHUTTER_ST_ADDR   0  // Shutter Status  register (read)
#define SHUTTER_CMD_ADDR  1  // Shutter Command register (write)
#define ARD_RESET_ADDR    2  // Arduino Reset register

// Shutter Status register values (SHUTTER_ST_ADDR)
#define SHUTTER_ST_MOVING  0  // moving / unknown
#define SHUTTER_ST_OPEN    1  // open   (A0=1, A1=0)
#define SHUTTER_ST_CLOSED  2  // closed (A0=0, A1=1)

// Shutter Command register values (SHUTTER_CMD_ADDR), cleared to IDLE once executed
#define SHUTTER_CMD_IDLE   0  // no pending command
#define SHUTTER_CMD_OPEN   1  // open  the shutter (pulse HIGH on D7)
#define SHUTTER_CMD_CLOSE  2  // close the shutter (pulse LOW  on D6)

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

/*
 * The time for Check and Reset actions (same scheme as V21 valve in Tubes_Pi_Mod_1)
 */
unsigned long SHUTTERtime = 0;

/*
 *  This RESET is used to reset switches (Command outputs)
 */
boolean SHUTTER_RESET = false;

/*
 *  This CHECK is used to check switches status (Status inputs)
 */
boolean SHUTTER_CHECK = false;

/*
 *  The waiting time before resetting switches command
 */
long reset_wait = 2000;

/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;

/*
 *  Command-received flag, mirroring updateIOFromI2CBool of Tubes_Pi_Mod_1: it is
 *  set when the master writes a new command to the command register, so that
 *  UpdateIOFromModbus() runs once per received command (not every loop). Because
 *  the ModbusTCPSlave library has no receive callback, lastCmdReg lets us watch
 *  the register for a change - the equivalent of the I2C receiveEvent in Tubes.
 */
boolean updateIOFromModbusBool = false;
uint16_t lastCmdReg = SHUTTER_CMD_IDLE;

/*
 *  Periodic heartbeat + log-on-change state
 */
unsigned long heartbeat_time = 0;
long heartbeat_wait = 5000;
int SHUTTER_LAST_STATUS = -1;

void(*resetArd) (void) = 0; //declare reset function @ address 0

void setup() {

  // Open serial communication for Com port.
  Serial.begin(9600);
  // Leonardo (native USB): wait up to 3s for the serial monitor so the boot
  // messages are visible, but do not block a headless boot if none connects.
  unsigned long t0 = millis();
  while (!Serial && (millis() - t0) < 3000) ;

  // start the Modbus server
  StartModbusServer();

  Serial.println(F("Modbus started"));
}

void loop() {
  /***********************************************************************************************************/
  // check DHCP lease
  Ethernet.maintain();
  /***********************************************************************************************************/
  delay(100);

   /***********************************************************************************************************/
  // Periodic heartbeat so the console always shows life, network mode, IP and status
  if (millis() - heartbeat_time > heartbeat_wait) {
     heartbeat_time = millis();
     Serial.print(F("[hb] NET="));
     Serial.print(dhcp_ok ? F("DHCP") : F("STATIC"));
     Serial.print(F(" IP="));
     Serial.print(Ethernet.localIP());
     Serial.print(F(" ST="));
     int st = holdingRegisters[SHUTTER_ST_ADDR];
     if (st == SHUTTER_ST_OPEN)        Serial.print(F("OPEN"));
     else if (st == SHUTTER_ST_CLOSED) Serial.print(F("CLOSED"));
     else                              Serial.print(F("MOVING/UNKNOWN"));
     Serial.print(F(" CMD="));
     Serial.println(holdingRegisters[SHUTTER_CMD_ADDR]);
  }
  
  // Check Reset Status
  /***********************************************************************************************************/
  if (holdingRegisters[ARD_RESET_ADDR] == 0x01)
    resetArd();
  /***********************************************************************************************************/
  // Process Modbus requests (non-blocking). Must be called fast and repeatedly:
  // the ModbusTCPSlave receive is a state machine with a 1s internal timeout.
  modbus.update();

  // Detect a freshly written command (equivalent of the I2C receiveEvent that
  // sets updateIOFromI2CBool in Tubes_Pi_Mod_1): watch the command register for a change.
  if (holdingRegisters[SHUTTER_CMD_ADDR] != lastCmdReg) {
     lastCmdReg = holdingRegisters[SHUTTER_CMD_ADDR];
     if (holdingRegisters[SHUTTER_CMD_ADDR] == SHUTTER_CMD_OPEN ||
         holdingRegisters[SHUTTER_CMD_ADDR] == SHUTTER_CMD_CLOSE)
        updateIOFromModbusBool = true;
  }

  // Same loop logic as Tubes_Pi_Mod_1: act on a received command only when the flag
  // is set, then always refresh the status and run the Reset/Check state machine.
  if (updateIOFromModbusBool == true) {
    Serial.print(F("Received command; CMD register ="));
    Serial.println(holdingRegisters[SHUTTER_CMD_ADDR]);
    updateIOFromModbusBool = false;
    // Update I/O from the updated command register
    UpdateIOFromModbus();
  }

  // Update status register from I/O
  UpdateModbusFromIO();

  ResetAndCheck();
 
}

void StartModbusServer()
{
  /* Try DHCP first with a bounded timeout, then fall back to the static IP.
     The standard Ethernet library takes the DHCP timeout as a parameter. */
  Serial.print(F("Trying DHCP (timeout "));
  Serial.print(dhcp_timeout);
  Serial.println(F(" ms)..."));
  if (Ethernet.begin(mac, dhcp_timeout) == 1) {
     dhcp_ok = true;
     Serial.println(F("DHCP lease obtained"));
  }
  else {
     dhcp_ok = false;
     Serial.println(F("DHCP failed, falling back to static IP"));
     Ethernet.begin(mac, ip);
  }

  // Init ModbusTCPSlave object
  modbus.begin();

  // Configure registers
  modbus.setHoldingRegisters(holdingRegisters, NB_HOLDING_REGISTERS);

  // Digital OUTPUTS assignation & initialization (INIZIALIZZAZIONE: D6=1, D7=0)
  digitalWrite(SHUTTER_OPEN_CMD,LOW);                        // Set SHUTTER_OPEN_CMD (D7) LOW
  digitalWrite(SHUTTER_CLOSE_CMD,HIGH);                      // Set SHUTTER_CLOSE_CMD (D6) HIGH
  pinMode(SHUTTER_OPEN_CMD, OUTPUT);                         // sets the digital pin as output for Open Shutter
  pinMode(SHUTTER_CLOSE_CMD, OUTPUT);                        // sets the digital pin as output for Close Shutter
  holdingRegisters[SHUTTER_CMD_ADDR] = SHUTTER_CMD_IDLE;     // No pending command

  // Digital INPUTS assignation
  pinMode(SHUTTER_OPEN_STATUS, INPUT);                       // sets the digital pin as input for Shutter OPEN STATUS
  pinMode(SHUTTER_CLOSE_STATUS, INPUT);                      // sets the digital pin as input for Shutter CLOSE STATUS
  holdingRegisters[SHUTTER_ST_ADDR] = SHUTTER_ST_MOVING;     // RESET

  // register for Arduino reset
  holdingRegisters[ARD_RESET_ADDR] = 0x00;                  // Arduino Global Reset Status

  Serial.print(F("Modbus server listening on "));
  Serial.println(Ethernet.localIP());
}

/*
 * Act on the command register - exact reproduction of the V21 valve OPEN/CLOSE
 * logic (UpdateIOFromI2C) in Tubes_Pi_Mod_1: trigger the pulse, arm the RESET,
 * do NOT clear anything here (the pulse and the command register are reset later
 * in ResetAndCheck after reset_wait). Gated by SHUTTER_RESET so it fires once.
 */
void UpdateIOFromModbus()
{
  /***********************************************************************************************************/
  /* Update Shutter position (Open/Close) */
  /***********************************************************************************************************/
  if (holdingRegisters[SHUTTER_CMD_ADDR] == SHUTTER_CMD_CLOSE && SHUTTER_RESET == false) {
     Serial.println(F("Received CLOSE command: pulse LOW on D6"));
     digitalWrite(SHUTTER_CLOSE_CMD,LOW);   // CLOSE SHUTTER (D6 pulse LOW)
     SHUTTERtime = millis();
     SHUTTER_RESET = true;
  }
  else if (holdingRegisters[SHUTTER_CMD_ADDR] == SHUTTER_CMD_OPEN && SHUTTER_RESET == false) {
     Serial.println(F("Received OPEN command: pulse HIGH on D7"));
     digitalWrite(SHUTTER_OPEN_CMD,HIGH);   // OPEN SHUTTER (D7 pulse HIGH)
     SHUTTERtime = millis();
     SHUTTER_RESET = true;
  }
}

/*
 * Refresh the status register from A0/A1 - exact reproduction of the V21 valve
 * status logic (UpdateI2CFromIO) in Tubes_Pi_Mod_1, including the "was OPEN but
 * now reads CLOSED -> re-issue the close command" safety re-latch.
 */
void UpdateModbusFromIO()
{
  /* Update Shutter position STATUS register (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(SHUTTER_OPEN_STATUS) == HIGH && digitalRead(SHUTTER_CLOSE_STATUS) == LOW) { // OPEN STATUS (A0=1,A1=0)
     holdingRegisters[SHUTTER_ST_ADDR] = SHUTTER_ST_OPEN;
  }
  else if (digitalRead(SHUTTER_CLOSE_STATUS) == HIGH && digitalRead(SHUTTER_OPEN_STATUS) == LOW) { // CLOSE STATUS (A0=0,A1=1)
     if (holdingRegisters[SHUTTER_ST_ADDR] == SHUTTER_ST_OPEN) { // IF status was OPEN
        // reset close command
        digitalWrite(SHUTTER_CLOSE_CMD,LOW);  // CLOSE SHUTTER
        SHUTTERtime = millis();
        SHUTTER_RESET = true;
     }
     holdingRegisters[SHUTTER_ST_ADDR] = SHUTTER_ST_CLOSED;
  }
  else { // MOVING / UNKNOWN STATUS
     holdingRegisters[SHUTTER_ST_ADDR] = SHUTTER_ST_MOVING;
  }

  // Log only on change to avoid flooding the console
  int status = holdingRegisters[SHUTTER_ST_ADDR];
  if (status != SHUTTER_LAST_STATUS) {
     if (status == SHUTTER_ST_OPEN)        Serial.println(F("Shutter status: OPEN (A0=1,A1=0)"));
     else if (status == SHUTTER_ST_CLOSED) Serial.println(F("Shutter status: CLOSED (A0=0,A1=1)"));
     else                                  Serial.println(F("Shutter status: MOVING/UNKNOWN"));
     SHUTTER_LAST_STATUS = status;
  }
}

/*
 * Reset the command pulses and run the post-open CHECK - exact reproduction of
 * the V21 valve ResetAndCheck in Tubes_Pi_Mod_1.
 */
void ResetAndCheck()
{
  /*
   *  SHUTTER Case
   */
  // Reset SHUTTER_CLOSE_CMD (end the D6 close pulse after reset_wait)
  if (digitalRead(SHUTTER_CLOSE_CMD) == LOW && SHUTTER_RESET == true) {
    if ( millis() - SHUTTERtime > reset_wait) {
       digitalWrite(SHUTTER_CLOSE_CMD,HIGH);                       // RESET CLOSE SHUTTER (D6 back HIGH)
       holdingRegisters[SHUTTER_CMD_ADDR] = SHUTTER_CMD_IDLE;      // RESET CLOSE COMMAND
       SHUTTER_RESET = false;
    }
  }
  // Reset SHUTTER_OPEN_CMD (end the D7 open pulse after reset_wait, then arm CHECK)
  if (digitalRead(SHUTTER_OPEN_CMD) == HIGH && SHUTTER_RESET == true) {
    if ( millis() - SHUTTERtime > reset_wait) {
       digitalWrite(SHUTTER_OPEN_CMD,LOW);                         // RESET OPEN SHUTTER (D7 back LOW)
       holdingRegisters[SHUTTER_CMD_ADDR] = SHUTTER_CMD_IDLE;      // RESET OPEN COMMAND
       SHUTTER_RESET = false;
       // Now we will check that the shutter has effectively opened after some time
       SHUTTERtime = millis();
       SHUTTER_CHECK = true;
    }
  }
  // Check SHUTTER Close Status
  if (digitalRead(SHUTTER_CLOSE_STATUS) == HIGH && digitalRead(SHUTTER_OPEN_STATUS) == LOW && SHUTTER_CHECK == true) {
    if ( millis() - SHUTTERtime > check_wait) {
      digitalWrite(SHUTTER_CLOSE_CMD,LOW);  // CLOSE SHUTTER
      SHUTTERtime = millis();
      SHUTTER_CHECK = false;
      SHUTTER_RESET = true;
    }
  }
}
