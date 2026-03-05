#include <Ethernet.h>   // Ethernet library for Modbus TCP
#include <Controllino.h>  /* Usage of CONTROLLINO library allows you to use CONTROLLINO_xx aliases in your sketch. */
#include "ModbusRtu.h"    /* Usage of ModBusRtu library allows you to implement the Modbus RTU protocol in your sketch. */
#include <libmodbusmq.h> // Modbus TCP library
#include <OneWire.h> // OneWire library
#include <DS2438.h> 


// Temperature sensors
#define DS24B38 0x26      // 1-Wire DS2438 address (DS2438 + HIH5030 sensor)
#define DIGITAL_ONEWIRE CONTROLLINO_A0 // digital address used for bus 1-Wire
#define BUS_SENSOR_MAX 4  // Max number of sensors

OneWire ds7(DIGITAL_ONEWIRE); // OneWire object

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

#define ACUNIT_REGNUM 100

uint16_t ModbusSlaveRegisters1[ACUNIT_REGNUM];
uint16_t ModbusSlaveRegisters2[ACUNIT_REGNUM];

// Modbus address limit
#define NB_HOLDING_REGISTERS 300

// Modbus R/W addresses
#define AC1_ONOFF 0
#define AC1_MODE  1
#define AC1_FAN   2
#define AC1_VANE  3
#define AC1_SET   4
#define AC1_ECO   64

#define AC2_ONOFF 100
#define AC2_MODE  101
#define AC2_FAN   102
#define AC2_VANE  103
#define AC2_SET   104
#define AC2_ECO   164

// Temperature sensors addresses
#define FIRST_DS2438 200

// Loop R/W addresses
#define LOOPONOFF 220
#define LOOPSET1  221
#define LOOPSET2  222

// Reset Controllino address
#define RESETARD 299

void(*resetArd) (void) = 0; //declare reset function @ address 0

// This is an struct which contains read/write queries to 2x AC units slave device
modbus_t ModbusQuery[3];

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

void setup() {
  
   // initialize serial communication at 9600 bits per second:
  Serial.begin(9600);
  Serial.println("Started Serial:Console");
  Serial.println("-----------------------------------------");
  
  // start the Modbus server
  StartModbusServer();
  Serial.println("Started Serial:Modbus TCP Slave");
  Serial.println("-----------------------------------------");
  
  Serial.println("Started Serial:Modbus RTU Master");
  // ModbusQuery 0: read registers from AC unit 1
  ModbusQuery[0].u8id = SlaveModbusAdd1; // slave address
  ModbusQuery[0].u8fct = 3; // function code (this one is registers read)
  ModbusQuery[0].u16RegAdd = 0; // start address in slave
  ModbusQuery[0].u16CoilsNo = ACUNIT_REGNUM; // number of elements (coils or registers) to read
  ModbusQuery[0].au16reg = ModbusSlaveRegisters1; // pointer to a memory array in the CONTROLLINO

  // ModbusQuery 1: read registers from AC unit 2
  ModbusQuery[1].u8id = SlaveModbusAdd2; // slave address
  ModbusQuery[1].u8fct = 3; // function code (this one is registers read)
  ModbusQuery[1].u16RegAdd = 0; // start address in slave
  ModbusQuery[1].u16CoilsNo = ACUNIT_REGNUM; // number of elements (coils or registers) to read
  ModbusQuery[1].au16reg = ModbusSlaveRegisters2; // pointer to a memory array in the CONTROLLINO
  
  ControllinoModbusMaster.begin( 19200 ); // baud-rate at 19200
  ControllinoModbusMaster.setTimeOut( 5000 ); // if there is no answer in 5000 ms, roll over

  Serial.println("Started Serial:Modbus RTU Master");
  Serial.println("-----------------------------------------");
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
      
    //////////////////////////////////////////////////////////////////////////////
    // Query AC unit 1 registers
    //////////////////////////////////////////////////////////////////////////////
    ControllinoModbusMaster.query( ModbusQuery[0] );
    while (ControllinoModbusMaster.poll() == 0) // check incoming messages
       delay(100);
    if (ControllinoModbusMaster.getState() == COM_IDLE) {
       // registers read was proceed
       Serial.println("---------- READ RESPONSE from AC unit  1 ----");
       Serial.print("Slave ");
       Serial.println(SlaveModbusAdd1, DEC);
       int curaddr = 0;
       for (int i = 0; i < ACUNIT_REGNUM ; i++) {
          Serial.print(" register [");Serial.print(i);Serial.print("]=");
          Serial.println(ModbusSlaveRegisters1[i], DEC);
          mb_mapping.tab_holding_registers[curaddr++] = ModbusSlaveRegisters1[i];
       }
       Serial.println("-------------------------------------");
       Serial.println("");  
    }
    
    //////////////////////////////////////////////////////////////////////////////    
    // Listen to TCP Master
    //////////////////////////////////////////////////////////////////////////////    
    SendReceiveMaster();
    
    //////////////////////////////////////////////////////////////////////////////
    // Query AC unit 2 registers
    //////////////////////////////////////////////////////////////////////////////
    ControllinoModbusMaster.query( ModbusQuery[1] );
    while (ControllinoModbusMaster.poll() == 0) // check incoming messages
       delay(100);
    if (ControllinoModbusMaster.getState() == COM_IDLE) {
       // registers read was proceed
       Serial.println("---------- READ RESPONSE from AC unit  2 ----");
       Serial.print("Slave ");
       Serial.println(SlaveModbusAdd2, DEC);
       int curaddr = ACUNIT_REGNUM;
       for (int i = 0; i < ACUNIT_REGNUM ; i++) {
          Serial.print(" register [");Serial.print(i);Serial.print("]=");
          Serial.println(ModbusSlaveRegisters2[i], DEC);
          mb_mapping.tab_holding_registers[curaddr++] = ModbusSlaveRegisters1[i];
       }
       Serial.println("-------------------------------------");
       Serial.println("");  
    }
    
    //////////////////////////////////////////////////////////////////////////////    
    // Listen to TCP Master
    //////////////////////////////////////////////////////////////////////////////    
    SendReceiveMaster();
    
    //////////////////////////////////////////////////////////////////////////////
    // Get temperatures
    /////////////////////////////////////////////////////////////////////////////
    
    typedef union {
       float fvalue;
       uint16_t value[2];
    } FloatUint16;
    
    FloatUint16 mbValue;
    String DigitalIn[BUS_SENSOR_MAX];
    String DigitalInBis[BUS_SENSOR_MAX];
    int   busCnt = 0;
    byte  addr[8] = {0,0,0,0,0,0,0,0};
    String address[BUS_SENSOR_MAX];
    for (int i = 0 ; i < BUS_SENSOR_MAX; i++) {
       DigitalIn[i] = "";
       DigitalInBis[i] = "";
    }
    while (ds7.search(addr) == 1 && busCnt < BUS_SENSOR_MAX) { // Scan 1-Wire module
      getAddress(address[busCnt],addr);
      getDigitalIn(DigitalIn[busCnt],DigitalInBis[busCnt],addr);
      delay(100);
      busCnt++;
    }
    for (int i = 0; i < BUS_SENSOR_MAX;i++) {
       Serial.print(address[i]);Serial.print(" ");
       int index = FIRST_DS2438 + 5*i;
       if (address[i].length() > 4) {
          Serial.print("address exists:");Serial.println(address[i].substring(0,4));
          mb_mapping.tab_holding_registers[index] = (hstol(address[i].substring(0,4)));
       }
       else
          mb_mapping.tab_holding_registers[index] = 0x00;
       Serial.print("address register:");Serial.print(String(index));Serial.print(" ");Serial.println(String(mb_mapping.tab_holding_registers[index]));
       Serial.print(DigitalIn[i]);Serial.println(" ");
       if (DigitalIn[i].length() > 0)
          mbValue.fvalue = DigitalIn[i].toFloat();
       else
          mbValue.fvalue = 0x00;
       mb_mapping.tab_holding_registers[++index] = mbValue.value[0];
       Serial.print("temp register ");Serial.print(String(index));Serial.print(" ");Serial.println(String(mbValue.value[0]));
       mb_mapping.tab_holding_registers[++index] = mbValue.value[1];
       Serial.print("temp register ");Serial.print(String(index));Serial.print(" ");Serial.println(String(mbValue.value[1]));
       Serial.println(DigitalInBis[i]);
       if (DigitalInBis[i].length() > 0)
          mbValue.fvalue = DigitalInBis[i].toFloat();
       else
          mbValue.fvalue = 0x00;
       mb_mapping.tab_holding_registers[++index] = mbValue.value[0];
       Serial.print("hum register ");Serial.print(String(index));Serial.print(" ");Serial.println(String(mbValue.value[0]));
       mb_mapping.tab_holding_registers[++index] = mbValue.value[1];
       Serial.print("hum register ");Serial.print(String(index));Serial.print(" ");Serial.println(String(mbValue.value[1]));
    }
    ///////////////////////////////////////////////////////////////////////////////
    delay(1000);
}
void SendCommand(int id, int addr, int value) {

    // ModbusQuery 2: write registers to AC unit
    ModbusQuery[2].u8id = id; // slave address
    ModbusQuery[2].u8fct = 3; // function code (this one is registers read)
    ModbusQuery[2].u16RegAdd = addr; // start address in slave
    ModbusQuery[2].u16CoilsNo = 1; // number of elements (coils or registers) to read
    if ( id == 1 ) {
       ModbusQuery[2].au16reg = ModbusSlaveRegisters1; // pointer to a memory array in the CONTROLLINO
       ModbusSlaveRegisters1[addr] = value;
    }
    else if ( id == 2 ) {
       ModbusQuery[2].au16reg = ModbusSlaveRegisters2; // pointer to a memory array in the CONTROLLINO
       ModbusSlaveRegisters2[addr] = value;
    }
    ControllinoModbusMaster.query( ModbusQuery[2] );
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
    if (mb_mapping.tab_holding_registers[AC1_ONOFF] != ModbusSlaveRegisters1[0])
      SendCommand(SlaveModbusAdd1, AC1_ONOFF, mb_mapping.tab_holding_registers[AC1_ONOFF]);
    if (mb_mapping.tab_holding_registers[AC1_MODE] != ModbusSlaveRegisters1[1])
      SendCommand(SlaveModbusAdd1, AC1_MODE, mb_mapping.tab_holding_registers[AC1_MODE]);
    if (mb_mapping.tab_holding_registers[AC1_FAN] != ModbusSlaveRegisters1[2])
      SendCommand(SlaveModbusAdd1, AC1_FAN, mb_mapping.tab_holding_registers[AC1_FAN]);
    if (mb_mapping.tab_holding_registers[AC1_VANE] != ModbusSlaveRegisters1[3])
      SendCommand(SlaveModbusAdd1, AC1_VANE, mb_mapping.tab_holding_registers[AC1_VANE]);
    if (mb_mapping.tab_holding_registers[AC1_SET] != ModbusSlaveRegisters1[4])
      SendCommand(SlaveModbusAdd1, AC1_SET, mb_mapping.tab_holding_registers[AC1_SET]);
    if (mb_mapping.tab_holding_registers[AC1_ECO] != ModbusSlaveRegisters1[64])
      SendCommand(SlaveModbusAdd1, AC1_ECO, mb_mapping.tab_holding_registers[AC1_ECO]);
    /////////////////////////////////////////////////////////////////////////////////////// AC Unit 2
    if (mb_mapping.tab_holding_registers[AC2_ONOFF] != ModbusSlaveRegisters2[0])
      SendCommand(SlaveModbusAdd2, AC2_ONOFF, mb_mapping.tab_holding_registers[AC2_ONOFF]);
    if (mb_mapping.tab_holding_registers[AC2_MODE] != ModbusSlaveRegisters2[1])
      SendCommand(SlaveModbusAdd2, AC2_MODE, mb_mapping.tab_holding_registers[AC2_MODE]);
    if (mb_mapping.tab_holding_registers[AC2_FAN] != ModbusSlaveRegisters2[2])
      SendCommand(SlaveModbusAdd2, AC2_FAN, mb_mapping.tab_holding_registers[AC2_FAN]);
    if (mb_mapping.tab_holding_registers[AC2_VANE] != ModbusSlaveRegisters2[3])
      SendCommand(SlaveModbusAdd2, AC2_VANE, mb_mapping.tab_holding_registers[AC2_VANE]);
    if (mb_mapping.tab_holding_registers[AC2_SET] != ModbusSlaveRegisters2[4])
      SendCommand(SlaveModbusAdd2, AC2_SET, mb_mapping.tab_holding_registers[AC2_SET]);
    if (mb_mapping.tab_holding_registers[AC2_ECO] != ModbusSlaveRegisters2[64])
      SendCommand(SlaveModbusAdd2, AC2_ECO, mb_mapping.tab_holding_registers[AC2_ECO]);
    
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
