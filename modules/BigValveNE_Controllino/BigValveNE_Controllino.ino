/* Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include "Seeed_MCP9600.h" // compiled with Arduino-Wire (with timeout)
#include <SoftwareSerial.h> // Serial library
#include <ModbusTCPSlave.h> // Modbus2 library
#include <Controllino.h> // Controllino library
#include <avr/wdt.h>

/*
 *  This part is the Arduino sketch code
 */ 
// Ethernet config

// This is the TEST Controllino
//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x78, 0xAB };
//IPAddress ip( 192, 168, 224, 181 ); // Controllino-22 has this IP

// This is the WI BIG VALVE
//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x68, 0xAB };
//IPAddress ip( 192, 168, 224, 164 ); // Controllino-12 has this IP

// This is the NI BIG VALVE
//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x69, 0xAB };
//IPAddress ip( 192, 168, 224, 165 ); // Controllino-13 has this IP

// This is the WE BIG VALVE
//byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x70, 0xAB };
//IPAddress ip( 192, 168, 224, 166 ); // Controllino-14 has this IP

// This is the NE BIG VALVE
byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x71, 0xAB };
IPAddress ip( 192, 168, 224, 167 ); // Controllino-15 has this IP
 

// relay pins (OUTPUTS)
#define OPEN_VALVE  CONTROLLINO_R0    //(Open Valve Command )
#define CLOSE_VALVE CONTROLLINO_R1    //(Close Valve Command )

// digital pins (INTPUTS)
#define PRESSURE_STATUS CONTROLLINO_A0  // NI DIFFERENTIAL PRESSURE STATUS
#define GLOBAL_STATUS CONTROLLINO_A1  // NI VALVE GLOBAL STATUS
#define CLOSE_STATUS CONTROLLINO_A2  // NI VALVE CLOSE STATUS
#define OPEN_STATUS CONTROLLINO_A3  // NI VALVE OPEN STATUS
#define COMPRESSAIR_STATUS CONTROLLINO_A4  // NI COMPRESS AIR STATUS
#define BOTTLEAIR_STATUS CONTROLLINO_A5  // NI BOTTLE AIR STATUS
#define LN2TANKVALVE_NE_RELAY CONTROLLINO_R5  // NE LN2 TANK VALVE RELAY


// Define the ModbusTCPSlave object with port = 502
ModbusTCPSlave modbus(502);

// Modbus registers
#define NB_HOLDING_REGISTERS 10
#define VALVE_CMD_ADDR 0
#define VALVE_STATUS_ADDR 1
#define PRESSURE_STATUS_ADDR 2
#define GLOBAL_STATUS_ADDR 3
#define COMPRESSAIR_STATUS_ADDR 4
#define BOTTLEAIR_STATUS_ADDR 5
#define LN2TANKVALVE_NE_ADDR 6
#define CRYO_TEMP_ADDR 7
#define ARD_RESET_ADDR 9

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

void(*resetArd) (void) = 0; //declare reset function @ address 0

// Buffer for Serial purposes
size_t length = 64;
char *buffer = (char*)malloc(sizeof(char) * length); 

// The K thermocouple sensor
MCP9600 sensor;
unsigned long temptime = 0;
/*
 *  The waiting time before reading temperature
 */
long temp_wait = 5000;

// Conversion tool
typedef union {
    float fvalue;
    uint16_t value[2];
  } FloatUint16;

FloatUint16 conversion;

void softwareReset( uint8_t prescaller) {
  // start watchdog with the provided prescaller
  wdt_enable( prescaller);
  // wait for the prescaller time to expire
  // without sending the reset signal by using
  // the wdt_reset() method
  while(1) {}
}

/**@brief interruption cfg.
 * 
 * 
 * */
err_t sensor_INT_config()
{
    err_t ret=NO_ERROR;
    CHECK_RESULT(ret,sensor.set_filt_coefficients(FILT_MID));

    for(int i=0;i<4;i++)
    {
        /*Conver temp num to 16bit data*/
        CHECK_RESULT(ret,sensor.set_alert_limit(i,sensor.covert_temp_to_reg_form(28+i)));
        /*Set hysteresis.for example,set hysteresis to 2℃,when the INT limitation is 30℃,interruption will be generated when 
        the temp ecceed limitation,and the interruption flag will stay unless the temp below 30-2(limitation-hysteresis) 28℃. */
        CHECK_RESULT(ret,sensor.set_alert_hys(i,2)); 

         /*Set when interruption generated the pin's status*/
        CHECK_RESULT(ret,sensor.set_alert_bit(i,ACTIVE_LOW));

        CHECK_RESULT(ret,sensor.clear_int_flag(i));

        /*default is comparator mode*/
        CHECK_RESULT(ret,sensor.set_alert_mode_bit(i,COMPARE_MODE));

        /*Set alert pin ENABLE.*/
        CHECK_RESULT(ret,sensor.set_alert_enable(i,ENABLE));
    }    

    /*device cfg*/
    CHECK_RESULT(ret,sensor.set_cold_junc_resolution(COLD_JUNC_RESOLUTION_0_25));
    CHECK_RESULT(ret,sensor.set_ADC_meas_resolution(ADC_14BIT_RESOLUTION));
    CHECK_RESULT(ret,sensor.set_burst_mode_samp(BURST_32_SAMPLE));
    CHECK_RESULT(ret,sensor.set_sensor_mode(NORMAL_OPERATION));

    return NO_ERROR;
}


err_t get_temperature(float *value)
{
    err_t ret=NO_ERROR;
    float hot_junc=0;
    float junc_delta=0;
    float cold_junc=0;
    bool stat=true;
    
    CHECK_RESULT(ret,sensor.check_data_update(&stat));
    if(stat)
    {
        CHECK_RESULT(ret,sensor.read_hot_junc(&hot_junc));
        //CHECK_RESULT(ret,sensor.read_junc_temp_delta(&junc_delta));
        
        //CHECK_RESULT(ret,sensor.read_cold_junc(&cold_junc));
        
        *value=hot_junc;
    }
    else
    {
        Serial.println("data not ready!!");
    }

    return NO_ERROR;
}


void setup() {
  
  // Open serial communication for Com port.
  Serial.begin(9600);

  // start the Modbus server
  StartModbusServer();

  Serial.println("Modbus started");
  /* Sensor Init (thermocpuple K) */
  if(sensor.init(THER_TYPE_K))
    {
        Serial.println("sensor init failed!!");
    }
  sensor_INT_config();
  Wire.setTimeout(1000);

}
 
void loop() {
  Serial.println("Modbus loop");
  /***********************************************************************************************************/
  // check DHCP lease
  Ethernet.maintain();
  /***********************************************************************************************************/
  // Check Reset Status
  /***********************************************************************************************************/
  if (holdingRegisters[ARD_RESET_ADDR] == 0x01)
     // restart in 60 milliseconds
     softwareReset( WDTO_60MS);
     //resetArd();
  /***********************************************************************************************************/
  SendReceiveMaster();
  /***********************************************************************************************************/
  /* Read Temperature */
  /***********************************************************************************************************/
  if ( millis() - temptime > temp_wait ) {
     get_temperature(&conversion.fvalue);
     Serial.print("Temperature=");Serial.println(conversion.fvalue);
     temptime = millis();
  }
  delay(1000);
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
  
  // Assign Modbus reserved register addresses
  
  // Digital RELAY initialization and assignation
  digitalWrite(OPEN_VALVE,LOW);                           // Set VALVE OPEN CMD LOW
  digitalWrite(CLOSE_VALVE,LOW);                          // Set VALVE CLOSE CMD LOW
  pinMode(OPEN_VALVE, OUTPUT);                            // sets the digital pin as output for Open Valve (RELAY)
  pinMode(CLOSE_VALVE, OUTPUT);                           // sets the digital pin as output for Close Valve (RELAY)
  holdingRegisters[VALVE_CMD_ADDR] = 0x00;    // RESET
  
  // Digital inputs initialization and assignation
  pinMode(GLOBAL_STATUS, INPUT);                                    // sets the digital pin as input for Global Valve STATUS
  holdingRegisters[GLOBAL_STATUS_ADDR] = 0x00;      // RESET
  pinMode(OPEN_STATUS, INPUT);                                      // sets the digital pin as input for Open Valve STATUS
  pinMode(CLOSE_STATUS, INPUT);                                     // sets the digital pin as input for Close Valve STATUS
  holdingRegisters[VALVE_STATUS_ADDR] = 0x00;       // RESET
  pinMode(PRESSURE_STATUS, INPUT);                                  // sets the digital pin as input for Pressure Valve STATUS
  holdingRegisters[PRESSURE_STATUS_ADDR] = 0x00;    // RESET
  pinMode(COMPRESSAIR_STATUS, INPUT);                               // sets the digital pin as input for Compress air STATUS
  holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x00; // RESET
  pinMode(BOTTLEAIR_STATUS, INPUT);                                 // sets the digital pin as input for Bottle air STATUS
  holdingRegisters[BOTTLEAIR_STATUS_ADDR] = 0x00;   // RESET
  digitalWrite(LN2TANKVALVE_NE_RELAY,LOW);                          // Set LN2TANKVALVE_NE_RELAY to LOW
  pinMode(LN2TANKVALVE_NE_RELAY,OUTPUT) ;                           // sets the digital relay pin as output for LN2 Tank Valve
  holdingRegisters[LN2TANKVALVE_NE_ADDR] = 0x01;    // Initialize RELAY ADDRESS to OPEN
  
  holdingRegisters[CRYO_TEMP_ADDR] = 0x00;
  holdingRegisters[CRYO_TEMP_ADDR+1] = 0x00;
  
  // register for Arduino reset
  holdingRegisters[ARD_RESET_ADDR] = 0x00;     // Arduino Global Reset Status

}
void SendReceiveMaster()
{    

  // Process modbus requests
  modbus.update();
  
  /***********************************************************************************************************/
  /* Update Valve position (Close action ONLY) */
  /***********************************************************************************************************/
  if (holdingRegisters[VALVE_CMD_ADDR] == 0x02) {
     digitalWrite(CLOSE_VALVE,HIGH); // CLOSE VALVE
     delay(1000);
     digitalWrite(CLOSE_VALVE,LOW); // RESET
  }
  holdingRegisters[VALVE_CMD_ADDR] = 0x00; // Reset register

  /***********************************************************************************************************/
  /* Update Valve position STATUS register (Open/Close) */
  /***********************************************************************************************************/
  if (digitalRead(OPEN_STATUS) == HIGH && digitalRead(CLOSE_STATUS) == LOW)
     holdingRegisters[VALVE_STATUS_ADDR] = 0x01; // OPENED VALVE
  else if (digitalRead(CLOSE_STATUS) == HIGH && digitalRead(OPEN_STATUS) == LOW)
     holdingRegisters[VALVE_STATUS_ADDR] = 0x02; // CLOSED VALVE
  else {
     holdingRegisters[VALVE_STATUS_ADDR] = 0x00; // MOVING VALVE
  }
 
  /***********************************************************************************************************/
  /* Update COMPRESS AIR STATUS register (OK/KO) */
  /***********************************************************************************************************/
  if (digitalRead(COMPRESSAIR_STATUS) == HIGH)
     holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x01; // COMPRESS AIR STATUS OK
  else if (digitalRead(COMPRESSAIR_STATUS) == LOW)
     holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x00; // COMPRESS AIR STATUS KO 
  else
     holdingRegisters[COMPRESSAIR_STATUS_ADDR] = 0x03; // ERROR COMPRESS STATUS AIR

  /***********************************************************************************************************/
  /* Update PRESSURE STATUS register (OK/KO) */
  /***********************************************************************************************************/
  if (digitalRead(PRESSURE_STATUS) == HIGH)
     holdingRegisters[PRESSURE_STATUS_ADDR] = 0x01; // PRESSURE STATUS OK
  else if (digitalRead(PRESSURE_STATUS) == LOW)
     holdingRegisters[PRESSURE_STATUS_ADDR] = 0x00; // PRESSURE STATUS KO 
  else
     holdingRegisters[PRESSURE_STATUS_ADDR] = 0x03; // ERROR PRESSURE STATUS

  /***********************************************************************************************************/
  /* Update GLOBAL STATUS register (OK/KO) */
  /***********************************************************************************************************/
  if (digitalRead(GLOBAL_STATUS) == HIGH)
     holdingRegisters[GLOBAL_STATUS_ADDR] = 0x01; // GLOBAL STATUS OK
  else if (digitalRead(GLOBAL_STATUS) == LOW)
     holdingRegisters[GLOBAL_STATUS_ADDR] = 0x00; // GLOBAL STATUS KO
  else
     holdingRegisters[GLOBAL_STATUS_ADDR] = 0x03; // ERROR GLOBAL STATUS

  /***********************************************************************************************************/
  /* Update BOTTLE AIR STATUS register (OK/KO) */
  /***********************************************************************************************************/
  if (digitalRead(BOTTLEAIR_STATUS) == HIGH)
     holdingRegisters[BOTTLEAIR_STATUS_ADDR] = 0x01; // BOTTLE AIR STATUS OK
  else if (digitalRead(BOTTLEAIR_STATUS) == LOW)
     holdingRegisters[BOTTLEAIR_STATUS_ADDR] = 0x00; // BOTTLE AIR STATUS KO 
  else
     holdingRegisters[BOTTLEAIR_STATUS_ADDR] = 0x03; // ERROR BOTTLE AIR STATUS

  /***********************************************************************************************************/
  /* Update LN2 Tank Valve Relay NE (CLOSE/OPEN) */
  /***********************************************************************************************************/
  if (holdingRegisters[LN2TANKVALVE_NE_ADDR] == 0x02) {
     digitalWrite(LN2TANKVALVE_NE_RELAY,HIGH); // OPEN RELAY (CLOSE VALVE)
  }
  else if (holdingRegisters[LN2TANKVALVE_NE_ADDR] == 0x01) {
     digitalWrite(LN2TANKVALVE_NE_RELAY,LOW); // CLOSE RELAY (OPEN VALVE)
  } 
  /***********************************************************************************************************/
  /* Update Temperature register */
  /***********************************************************************************************************/
  holdingRegisters[CRYO_TEMP_ADDR] = conversion.value[0];
  Serial.print("Temperature reg 0=");Serial.println(holdingRegisters[CRYO_TEMP_ADDR]);
  holdingRegisters[CRYO_TEMP_ADDR+1] = conversion.value[1];
  Serial.print("Temperature reg 1=");Serial.println(holdingRegisters[CRYO_TEMP_ADDR+1]);
}
