/*
  Modbus Server
 A modbus server to monitor the pumping stations using Leonardo Eth board
 */
#include <SPI.h>
#include <Ethernet2.h>   // Ethernet library for Leonardo Eth
#include <libmodbusmq.h> // Modbus library
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

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5F, 0xD3 };
//const byte ip[] = { 192, 168, 224, 153 }; // Arduino-12 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5F, 0xD2 };
//const byte ip[] = { 192, 168, 224, 173 }; // Arduino-13 has this IP

//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5F, 0xFF };
//const byte ip[] = { 192, 168, 224, 174 }; // Arduino-14 has this IP

byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5C, 0xED };
const byte ip[] = { 192, 168, 224, 175 }; // Arduino-15 has this IP

// Modbus objects
#define SLAVE 0x01
modbus_param_t mb_param;     // generic mb params
modbus_mapping_t mb_mapping; // registers

/* Server object will run on Arduino, will be initialized later */
EthernetServer server(MODBUS_TCP_DEFAULT_PORT);

/* Client object connected with Arduino. This initializaton will be reassigned */
EthernetClient client = 0;

// digital pins (INTPUTS)
#define V_OPEN_STATUS A0   // BIG VALVE OPEN STATUS
#define V_CLOSE_STATUS A1  // BIG VALVE CLOSE STATUS


// Modbus addresses limits
#define NB_HOLDING_REGISTERS 10
#define V_STATUS_ADDR 0 //(BIG VALVE ST)
#define ARD_RESET_ADDR 1

void(*resetArd) (void) = 0; //declare reset function @ address 0

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
  if (mb_mapping.tab_holding_registers[ARD_RESET_ADDR] == 0x01)
    resetArd();
  delay(1000);
  SendReceiveMaster();
  
  /***********************************************************************************************************/
  /* Perform some internal logics */
  /***********************************************************************************************************/
  
}
void StartModbusServer()
{    
  int ret;
  modbus_init_tcp(&mb_param, mac, ip, MODBUS_TCP_DEFAULT_PORT, SLAVE);
  modbus_set_slave(&mb_param, SLAVE);
  modbus_set_error_handling(&mb_param, FLUSH_OR_CONNECT_ON_ERROR);
  Serial.println(F("Arduino Modbus Slave started"));
  ret = modbus_mapping_new(&mb_param, &mb_mapping, NB_HOLDING_REGISTERS);
  if (ret < 0) {
   // Serial.println(F("): Memory allocation failed, restarting Arduino..."));
  }
  // Digital VALVE INPUTS initialization and assignation
  pinMode(V_OPEN_STATUS, INPUT);                 // sets the digital pin as output for Big Valve OPEN STATUS
  pinMode(V_CLOSE_STATUS, INPUT);                // sets the digital pin as output for Big Valve CLOSE STATUS
  mb_mapping.tab_holding_registers[V_STATUS_ADDR] = 0x00;   // RESET
  
  // register for Arduino reset
  mb_mapping.tab_holding_registers[ARD_RESET_ADDR] = 0x00;     // Arduino Global Reset Status

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
  /***********************************************************************************************************/
  /* Update Valve position STATUS register (Open/Close) */
  /***********************************************************************************************************/
  Serial.print("V_OPEN_STATUS=");Serial.println(digitalRead(V_OPEN_STATUS));
  Serial.print("V_CLOSE_STATUS=");Serial.println(digitalRead(V_CLOSE_STATUS));
  if (digitalRead(V_OPEN_STATUS) == HIGH && digitalRead(V_CLOSE_STATUS) == LOW)
     mb_mapping.tab_holding_registers[V_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(V_CLOSE_STATUS) == HIGH && digitalRead(V_OPEN_STATUS) == LOW)
     mb_mapping.tab_holding_registers[V_STATUS_ADDR] = 0x02; // CLOSED VALVE
  else {
     mb_mapping.tab_holding_registers[V_STATUS_ADDR] = 0x00; // MOVING VALVE
     //Serial.print("V21_OPEN_STATUS=");Serial.println(digitalRead(V21_OPEN_STATUS));
     //Serial.print("V21_CLOSE_STATUS=");Serial.println(digitalRead(V21_CLOSE_STATUS));
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
