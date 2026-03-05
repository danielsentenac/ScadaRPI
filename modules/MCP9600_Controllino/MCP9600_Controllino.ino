/* Modbus Server
 A modbus server to monitor the pumping stations using Controllino board
 */
#include "Seeed_MCP9600.h"
#include <Controllino.h> // Controllino library


/*
 *  This part is the Arduino sketch code
 */ 


// The K thermocouple sensor
MCP9600 sensor(0x60);

// Conversion tool
typedef union {
    float fvalue;
    uint16_t value[2];
  } FloatUint16;
  
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
        CHECK_RESULT(ret,sensor.read_junc_temp_delta(&junc_delta));
        
        CHECK_RESULT(ret,sensor.read_cold_junc(&cold_junc));
        
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

  Serial.println("Modbus started");
  /* Sensor Init (thermocpuple K) */
  if(sensor.init(THER_TYPE_K))
    {
        Serial.println("sensor init failed!!");
    }
  sensor_INT_config();

}
 
void loop() {
 
  Serial.println("Modbus loop");
  delay(1000);
  FloatUint16 conversion;
  float temp = 0;
  get_temperature(&temp);
  conversion.fvalue = temp;
  Serial.print("Temperature=");Serial.println(conversion.fvalue);
  Serial.print("Temperature reg 0=");
  Serial.print("Temperature reg 1=");
}

 
 


