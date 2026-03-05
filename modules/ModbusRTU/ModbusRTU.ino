/*
  Modbus Server
 A modbus server to control the TKS temperature controller using Leonardo Eth board
 */
#include <SPI.h>
#include <Ethernet2.h>   // Ethernet library for Leonardo Eth
#include <libmodbusmq.h> // Modbus library
#include <SoftwareSerial.h> // Serial library
/*
 *  This part is the Arduino sketch code
 */ 

byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5E, 0xD31 };
const byte ip[] = { 192, 168, 224, 176 }; // Arduino-16 has this IP

// Modbus objects
#define SLAVE 0x01
modbus_param_t mb_param;     // generic mb params
modbus_mapping_t mb_mapping; // registers

/* Server object will run on Arduino, will be initialized later */
EthernetServer server(MODBUS_TCP_DEFAULT_PORT);

/* Client object connected with Arduino. This initializaton will be reassigned */
EthernetClient client = 0;

// define Modbus RTU Serial port
SoftwareSerial RS485Serial(8, 10); // RX, TX for Leonardo

// Modbus addresses limits
#define NB_HOLDING_REGISTERS 60
#define MODBUS_TEMP_DEVICE_1_ADDR 10
#define MODBUS_SETPOINT_DEVICE_1_ADDR 11
#define MODBUS_TEMP_DEVICE_2_ADDR 20
#define MODBUS_SETPOINT_DEVICE_2_ADDR 21
#define ARD_RESET_ADDR NB_HOLDING_REGISTERS - 1

uint16_t refSETP1 = 0; // Reference SETPOINT DEVICE #1
uint16_t refSETP2 = 0; // Reference SETPOINT DEVICE #2

void(*resetArd) (void) = 0; //declare reset function @ address 0

// Buffer for Serial purposes
size_t length = 64;
unsigned char *buffer = (char*)malloc(sizeof(unsigned char) * length); 

void setup() {
  
  // Open serial communication for Console port.
  Serial.begin(9600);
  Serial.println("Setup..");
  // Open serial communication for MODBUS RTU COM
  // Initialize pin header (digital 3) for RE/DE switch for RS485 communication
  pinMode(3, OUTPUT);
  digitalWrite(3, LOW);  // Init Transceiver  
  RS485Serial.begin(9600);
  RS485Serial.setTimeout(250);
        
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
  
  Serial.println("DEVICE #1:GET TEMPERATURE & SETPOINT");
  getDeviceData(MODBUS_TEMP_DEVICE_1_ADDR);
  Serial.println("DEVICE #2:GET TEMPERATURE & SETPOINT");
  getDeviceData(MODBUS_TEMP_DEVICE_2_ADDR);

  // Listen to incoming request
  SendReceiveMaster();
   
  delay(100);
  /***********************************************************************************************************/
  /* Perform some internal logics */
  /***********************************************************************************************************/
}
int setDeviceData(uint16_t reg, uint16_t value) {
  
 typedef union {
      uint16_t uint16value;
      uint8_t value[2];
 } Uint16Uint8;
 Uint16Uint8 setpoint;

 setpoint.uint16value = value;

 // Extract device #ID from reg
 float deviceIdFloat = (float)(reg);
 deviceIdFloat /= 10;
 uint8_t deviceId = (uint8_t) deviceIdFloat;
 Serial.print("Device ID = ");Serial.println(deviceId);
 
 // Take control over device
 uint8_t frame[8];
 frame[0] = deviceId; // device ID
 frame[1] = 0x05;     // function code 
 frame[2] = 0x00;
 frame[3] = 0x0A;     // device control REGISTER 
 frame[4] = 0xFF;     //
 frame[5] = 0x00;     // Set Bit to 1
 frame[6] = 0x00;
 frame[7] = 0x00;
 
 uint16_t crc = CRC16(frame,6);
 Serial.print("CRC16=");Serial.print(((crc >> 0) & 0xff));Serial.print(", ");Serial.println(((crc >> 8) & 0xff));
 frame[6] = ((crc >> 0) & 0xFF);
 frame[7] = ((crc >> 8) & 0xFF);

 Serial.println("Write..");
 digitalWrite(3, HIGH);  // Enable RS485 Transmit
 int bytesSent = RS485Serial.write(frame,8);
 Serial.print("Take control for device ");
 Serial.print(deviceId);
 Serial.print(" #reg=");
 Serial.print("0x0A");
 Serial.print(" (sent ");
 Serial.print(bytesSent);
 Serial.println(" bytes).");
 digitalWrite(3, LOW);  // Disable RS485 Transmit
 
 Serial.println("Read..");
 memset(buffer,0,length);
 RS485Serial.readBytes(buffer,length);
 RS485Serial.flush(); 
 for (int i=0;i<8;i++){
     Serial.print("resp[");Serial.print(i);Serial.print("]=");
     Serial.println(buffer[i]);
 }
 // Check CRC
 uint16_t crc_resp = CRC16(buffer,6);
 if ( (buffer[6] == ((crc_resp >> 0) & 0xFF)) && (buffer[7] == ((crc_resp >> 8) & 0xFF))) {
   Serial.println("Take control OK");
 }
 else {
   Serial.println("Take Control:Error CheckSum.");
   return (-1);
 }
 // OK: update SETPOINT VALUE
 delay(100);
 // Extract device register from reg
 uint8_t deviceReg = (uint8_t)(reg%10);
 Serial.print("Set value for Register = ");Serial.println(deviceReg);
 frame[0] = deviceId; // device ID
 frame[1] = 0x06;     // function code 
 frame[2] = 0x00;
 frame[3] = 0x01;     // SETPOINT REGISTER 
 frame[4] = setpoint.value[1];
 frame[5] = setpoint.value[0];      
 frame[6] = 0x00;
 frame[7] = 0x00;
 
 crc = CRC16(frame,6);
 Serial.print("CRC16=");Serial.print(((crc >> 0) & 0xff));Serial.print(", ");Serial.println(((crc >> 8) & 0xff));
 frame[6] = ((crc >> 0) & 0xFF);
 frame[7] = ((crc >> 8) & 0xFF);

 Serial.println("Write..");
 digitalWrite(3, HIGH);  // Enable RS485 Transmit
 bytesSent = RS485Serial.write(frame,8);
 Serial.print("New Setpoint for device ");
 Serial.print(deviceId);
 Serial.print(" #reg=");
 Serial.print(deviceReg);
 Serial.print(" (sent ");
 Serial.print(bytesSent);
 Serial.println(" bytes).");
 digitalWrite(3, LOW);  // Disable RS485 Transmit
 
 Serial.println("Read..");
 memset(buffer,0,length);
 RS485Serial.readBytes(buffer,length);
 RS485Serial.flush();
 for (int i=0;i<8;i++){
     Serial.print("buffer[");Serial.print(i);Serial.print("]=");
     Serial.println(buffer[i]);
 }
 // Check CRC
 crc_resp = CRC16(buffer,6);
 if ( (buffer[6] == ((crc_resp >> 0) & 0xFF)) && (buffer[7] == ((crc_resp >> 8) & 0xFF))) {
   Serial.println("New Setpoint set OK");
 }
 else {
   Serial.print("New SetPoint:Error CheckSum.");
   return (-1);
 }
 // Release control over device
 delay(100);
 frame[0] = deviceId; // device ID
 frame[1] = 0x05;     // function code 
 frame[2] = 0x00;
 frame[3] = 0x0A;     // device control REGISTER 
 frame[4] = 0x00;     //
 frame[5] = 0x00;     // Set Bit to 1
 frame[6] = 0x00;
 frame[7] = 0x00;
 
 crc = CRC16(frame,6);
 Serial.print("CRC16=");Serial.print(((crc >> 0) & 0xff));Serial.print(", ");Serial.println(((crc >> 8) & 0xff));
 frame[6] = ((crc >> 0) & 0xFF);
 frame[7] = ((crc >> 8) & 0xFF);

 Serial.println("Write..");
 digitalWrite(3, HIGH);  // Enable RS485 Transmit
 bytesSent = RS485Serial.write(frame,8);
 Serial.print("Take control for device ");
 Serial.print(deviceId);
 Serial.print(" #reg=");
 Serial.print("0x0A");
 Serial.print(" (sent ");
 Serial.print(bytesSent);
 Serial.println(" bytes).");
 digitalWrite(3, LOW);  // Disable RS485 Transmit
 
 Serial.println("Read..");
 memset(buffer,0,length);
 RS485Serial.readBytes(buffer,length);
 RS485Serial.flush(); 
 for (int i=0;i<8;i++){
     Serial.print("resp[");Serial.print(i);Serial.print("]=");
     Serial.println(buffer[i]);
 }
 // Check CRC
 crc_resp = CRC16(buffer,6);
 if ( (buffer[6] == ((crc_resp >> 0) & 0xFF)) && (buffer[7] == ((crc_resp >> 8) & 0xFF))) {
   Serial.println("Take control OK");
 }
 else {
   Serial.println("Release Control:Error CheckSum.");
   return (-1);
 }
 return 0;
}
int getDeviceData(int reg) {
  
 typedef union {
      uint16_t uint16value;
      uint8_t value[2];
 } Uint16Uint8;
 Uint16Uint8 temp;
 Uint16Uint8 setpoint;

 // Extract device #ID from reg
 float deviceIdFloat = (float)(reg);
 deviceIdFloat /= 10;
 uint8_t deviceId = (uint8_t) deviceIdFloat;
 Serial.print("Device ID = ");Serial.println(deviceId);
 // Extract device register from reg
 uint8_t deviceReg = (uint8_t)(reg%10);
 Serial.print("Register = ");Serial.println(deviceReg);
 delay(100);
 uint8_t frame[8];
 frame[0] = deviceId; // device ID
 frame[1] = 0x03;     // function code
 frame[2] = 0x00;
 frame[3] = deviceReg; // device REGISTER 
 frame[4] = 0x00;
 frame[5] = 0x02;      // Ask two data registers (Temp & SetPoint)
 frame[6] = 0x00;
 frame[7] = 0x00;
 
 uint16_t crc = CRC16(frame,6);
 Serial.print("CRC16=");Serial.print(((crc >> 0) & 0xff));Serial.print(", ");Serial.println(((crc >> 8) & 0xff));
 frame[6] = ((crc >> 0) & 0xFF);
 frame[7] = ((crc >> 8) & 0xFF);

 Serial.println("Write..");
 digitalWrite(3, HIGH);  // Enable RS485 Transmit
 int bytesSent = RS485Serial.write(frame,8);

 Serial.print("Query for device ");
 Serial.print(deviceId);
 Serial.print(" #reg=");
 Serial.print(deviceReg);
 Serial.print(" (sent ");
 Serial.print(bytesSent);
 Serial.println(" bytes).");
 
 digitalWrite(3, LOW);  // Disable RS485 Transmit
 Serial.println("Read.."); 
 memset(buffer,0,length);
 RS485Serial.readBytes(buffer,length);
 RS485Serial.flush();
 for (int i=0;i<9;i++){
     Serial.print("buffer[");Serial.print(i);Serial.print("]=");
     Serial.println(buffer[i]);
 }
 // Check CRC
 uint16_t crc_resp = CRC16(buffer,7);
 if ( (buffer[7] == ((crc_resp >> 0) & 0xFF)) && (buffer[8] == ((crc_resp >> 8) & 0xFF))) {
   temp.value[0] = buffer[4];
   temp.value[1] = buffer[3];
   Serial.print("register TEMPERATURE value = ");Serial.println(temp.uint16value);
   setpoint.value[0] = buffer[6];
   setpoint.value[1] = buffer[5];
   Serial.print("register SETPOINT value = ");Serial.println(setpoint.uint16value);
   // 
   // Update internal register value
   //
   mb_mapping.tab_holding_registers[reg++] = temp.uint16value;
   if (deviceId == 1) {
     if ( refSETP1 != setpoint.uint16value) { // Set Point changed in LOCAL mode
        Serial.print("SETPOINT REGISTER=");
        Serial.print(mb_mapping.tab_holding_registers[reg]);
        Serial.print(" DEVICE SETPOINT VALUE=");
        Serial.println(setpoint.uint16value);
        mb_mapping.tab_holding_registers[reg] = setpoint.uint16value; // Update register value
        refSETP1 = setpoint.uint16value; // Update reference Set Point
     }
     if ( refSETP1 != mb_mapping.tab_holding_registers[reg]) { // Set Point changed in REMOTE mode
        refSETP1 = setpoint.uint16value; // Update reference Set Point
        setDeviceData(reg,mb_mapping.tab_holding_registers[reg]); // Change Set Point
     }
   }
   if (deviceId == 2) {
     if ( refSETP2 != setpoint.uint16value) { // Set Point changed in LOCAL mode
        Serial.print("SETPOINT REGISTER=");
        Serial.print(mb_mapping.tab_holding_registers[reg]);
        Serial.print(" DEVICE SETPOINT VALUE=");
        Serial.println(setpoint.uint16value);
        mb_mapping.tab_holding_registers[reg] = setpoint.uint16value; // Update register value
        refSETP2 = setpoint.uint16value; // Update reference Set Point
     }
     if ( refSETP2 != mb_mapping.tab_holding_registers[reg]) { // Set Point changed in REMOTE mode
        refSETP2 = setpoint.uint16value; // Update reference Set Point
        setDeviceData(reg,mb_mapping.tab_holding_registers[reg]); // Change Set Point
     }
   }
 }
 else {
   Serial.print("Get Data: Error CheckSum.");
   return (-1);
 }
 return 0;
}

// 
// CRC16 Checksum calculation
//
uint16_t CRC16 (const uint8_t *nData, uint16_t wLength)
{
static const short wCRCTable[] = {
   0X0000, 0XC0C1, 0XC181, 0X0140, 0XC301, 0X03C0, 0X0280, 0XC241,
   0XC601, 0X06C0, 0X0780, 0XC741, 0X0500, 0XC5C1, 0XC481, 0X0440,
   0XCC01, 0X0CC0, 0X0D80, 0XCD41, 0X0F00, 0XCFC1, 0XCE81, 0X0E40,
   0X0A00, 0XCAC1, 0XCB81, 0X0B40, 0XC901, 0X09C0, 0X0880, 0XC841,
   0XD801, 0X18C0, 0X1980, 0XD941, 0X1B00, 0XDBC1, 0XDA81, 0X1A40,
   0X1E00, 0XDEC1, 0XDF81, 0X1F40, 0XDD01, 0X1DC0, 0X1C80, 0XDC41,
   0X1400, 0XD4C1, 0XD581, 0X1540, 0XD701, 0X17C0, 0X1680, 0XD641,
   0XD201, 0X12C0, 0X1380, 0XD341, 0X1100, 0XD1C1, 0XD081, 0X1040,
   0XF001, 0X30C0, 0X3180, 0XF141, 0X3300, 0XF3C1, 0XF281, 0X3240,
   0X3600, 0XF6C1, 0XF781, 0X3740, 0XF501, 0X35C0, 0X3480, 0XF441,
   0X3C00, 0XFCC1, 0XFD81, 0X3D40, 0XFF01, 0X3FC0, 0X3E80, 0XFE41,
   0XFA01, 0X3AC0, 0X3B80, 0XFB41, 0X3900, 0XF9C1, 0XF881, 0X3840,
   0X2800, 0XE8C1, 0XE981, 0X2940, 0XEB01, 0X2BC0, 0X2A80, 0XEA41,
   0XEE01, 0X2EC0, 0X2F80, 0XEF41, 0X2D00, 0XEDC1, 0XEC81, 0X2C40,
   0XE401, 0X24C0, 0X2580, 0XE541, 0X2700, 0XE7C1, 0XE681, 0X2640,
   0X2200, 0XE2C1, 0XE381, 0X2340, 0XE101, 0X21C0, 0X2080, 0XE041,
   0XA001, 0X60C0, 0X6180, 0XA141, 0X6300, 0XA3C1, 0XA281, 0X6240,
   0X6600, 0XA6C1, 0XA781, 0X6740, 0XA501, 0X65C0, 0X6480, 0XA441,
   0X6C00, 0XACC1, 0XAD81, 0X6D40, 0XAF01, 0X6FC0, 0X6E80, 0XAE41,
   0XAA01, 0X6AC0, 0X6B80, 0XAB41, 0X6900, 0XA9C1, 0XA881, 0X6840,
   0X7800, 0XB8C1, 0XB981, 0X7940, 0XBB01, 0X7BC0, 0X7A80, 0XBA41,
   0XBE01, 0X7EC0, 0X7F80, 0XBF41, 0X7D00, 0XBDC1, 0XBC81, 0X7C40,
   0XB401, 0X74C0, 0X7580, 0XB541, 0X7700, 0XB7C1, 0XB681, 0X7640,
   0X7200, 0XB2C1, 0XB381, 0X7340, 0XB101, 0X71C0, 0X7080, 0XB041,
   0X5000, 0X90C1, 0X9181, 0X5140, 0X9301, 0X53C0, 0X5280, 0X9241,
   0X9601, 0X56C0, 0X5780, 0X9741, 0X5500, 0X95C1, 0X9481, 0X5440,
   0X9C01, 0X5CC0, 0X5D80, 0X9D41, 0X5F00, 0X9FC1, 0X9E81, 0X5E40,
   0X5A00, 0X9AC1, 0X9B81, 0X5B40, 0X9901, 0X59C0, 0X5880, 0X9841,
   0X8801, 0X48C0, 0X4980, 0X8941, 0X4B00, 0X8BC1, 0X8A81, 0X4A40,
   0X4E00, 0X8EC1, 0X8F81, 0X4F40, 0X8D01, 0X4DC0, 0X4C80, 0X8C41,
   0X4400, 0X84C1, 0X8581, 0X4540, 0X8701, 0X47C0, 0X4680, 0X8641,
   0X8201, 0X42C0, 0X4380, 0X8341, 0X4100, 0X81C1, 0X8081, 0X4040 };

uint8_t nTemp;
uint16_t wCRCWord = 0xFFFF;

   while (wLength--)
   {
      nTemp = *nData++ ^ wCRCWord;
      wCRCWord >>= 8;
      wCRCWord  ^= wCRCTable[nTemp];
   }
   return wCRCWord;

} // End: CRC16

void StartModbusServer()
{    
  int ret;
  modbus_init_tcp(&mb_param, mac, ip, MODBUS_TCP_DEFAULT_PORT, SLAVE);
  modbus_set_slave(&mb_param, SLAVE);
  modbus_set_error_handling(&mb_param, FLUSH_OR_CONNECT_ON_ERROR);
  ret = modbus_mapping_new(&mb_param, &mb_mapping, NB_HOLDING_REGISTERS);
  if (ret < 0) {
    Serial.println(F("): Memory allocation failed, restarting Arduino..."));
  }
  // Assign Modbus reserved register addresses
  for (int i = 0; i < NB_HOLDING_REGISTERS; i++)
        mb_mapping.tab_holding_registers[i] = 0x00;
        
  Serial.println(F("Arduino Modbus Slave started"));
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
