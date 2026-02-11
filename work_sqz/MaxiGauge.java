/*
 * This Class is the implementation of the MaxiGauge device
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class MaxiGauge extends Device {

   private Serial_Comm rs485;
   private static final Logger logger = Logger.getLogger("Main");

   public MaxiGauge (String _name,
                     int _mbRegisterStart, 
                     String serial_port, 
                     Baud baudrate, 
                     DataBits databits, 
                     Parity parity, 
                     StopBits stopbits, 
                     FlowControl flowcontrol) {

     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("MaxiGauge:MaxiGauge> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Gauge Pressure
     addDataElement( new DataElement(name, "PR1", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     addDataElement( new DataElement(name, "PR2", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PR3", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PR4", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PR5", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PR6", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     // Gauge (Pressure) Status
     addDataElement( new DataElement(name, "PR1ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PR2ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR3ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR4ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR5ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR6ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     // Gauge Sensor Status
     addDataElement( new DataElement(name, "PR1SST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR2SST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR3SST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR4SST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR5SST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR6SST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     // Gauge Commands (ON/OFF)
     addDataElement( new DataElement(name, "PR1ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR2ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR3ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR4ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR5ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR6ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

    // Controller Maxigauge comm
    addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

    mbRegisterEnd+=1;

    logger.finer("MaxiGauge:MaxiGauge> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     // Instantiate communication channel
     try {
       rs485 = new Serial_Comm(serial_port, baudrate, databits, parity, stopbits, flowcontrol);
       rs485.Open();
     }
     catch (InterruptedException e) {
       logger.log(Level.SEVERE, e.getMessage());
     }
     catch (IOException e) {
       logger.log(Level.SEVERE, e.getMessage());
     }
     catch(UnsatisfiedLinkError ex) {
       logger.log(Level.SEVERE, ex.getMessage());
     }

   }
   
   public void updateDeviceData() {
   
     // Get monitoring data from device using rs485 Comm
     DataElement d1 = getDataElement("PR1");
     DataElement d2 = getDataElement("PR2");
     DataElement d3 = getDataElement("PR3");
     DataElement d4 = getDataElement("PR4");
     DataElement d5 = getDataElement("PR5");
     DataElement d6 = getDataElement("PR6");
     DataElement d1s = getDataElement("PR1ST");
     DataElement d2s = getDataElement("PR2ST");
     DataElement d3s = getDataElement("PR3ST");
     DataElement d4s = getDataElement("PR4ST");
     DataElement d5s = getDataElement("PR5ST");
     DataElement d6s = getDataElement("PR6ST");
     DataElement d1ss = getDataElement("PR1SST");
     DataElement d2ss = getDataElement("PR2SST");
     DataElement d3ss = getDataElement("PR3SST");
     DataElement d4ss = getDataElement("PR4SST");
     DataElement d5ss = getDataElement("PR5SST");
     DataElement d6ss = getDataElement("PR6SST");
     // Com Status
     DataElement dcom = getDataElement("COMST");

     try {
        for (int i = 0; i < 7; i++) {
           //logger.finer(" --> " + name + ":next Modbus commands...");
           //addModbusCommand(); // Push Modbus commands in the loop is more reactive
           //logger.finer(" --> " + name + ":next command...");
           popCommand();  // Execute commands in the loop is more reactive
           String serDataR = "", serDataW = "", serDataACK= "\u0005";
           serDataW = "PR" + Integer.toString(i+1) + "\r";
           if ( i == 6 )
              serDataW = "SEN\r";
           logger.finer("MaxiGauge:updateDeviceData> serDataW=" + serDataW);
           rs485.Write(serDataW);
           Thread.sleep(200); // Essential to get good timing through communication channel
           byte [] answer = null;
           while  (rs485.BytesAvailable() > 0 )
             answer = rs485.Read();
           if ( answer != null) 
              serDataR = new String(answer, "UTF-8");
           logger.finer("MaxiGauge:updateDeviceData> serDataR=START-" + serDataR + "-END");
           serDataR = "";
           rs485.Write(serDataACK);
           Thread.sleep(200); // Essential to get good timing through communication channel
           while (rs485.BytesAvailable() > 0 )
             answer = rs485.Read();
           if ( answer != null) 
           serDataR = new String(answer, "UTF-8");
           logger.finer("MaxiGauge:updateDeviceData> serDataR=START-" + serDataR + "-END");
           if (serDataR.length() <= 1 && ++comm_cnt_failure >= comm_max_failure ) {// Com Status Error - reset all values -
              if ( hasWarned == false )
                 logger.log(Level.WARNING, "MaxiGauge:updateDeviceData> Communication with " + name + " interrupted");
              setErrorComStatus();
              break;
           }
           else {
              comm_cnt_failure = 0;
              dcom.value = 0; // OK COM
              if ( hasWarned == true ) {
                 hasWarned = false;
                 logger.info("MaxiGauge:updateDeviceData> Communication with " + name + " back!");
              }
              if ( i < 6 ) { // Sensor pressures & status
                 double pressure_val = Double.parseDouble(serDataR.substring(serDataR.indexOf(",")+1));
                 int pressure_status = Integer.parseInt(serDataR.substring(0,serDataR.indexOf(",")));
                 logger.finer("MaxiGauge:updateDeviceData> " + i + " pressure=" + pressure_val + " status=" + pressure_status);
                 if (i == 0) { d1.value = pressure_val; d1s.value = pressure_status;}
                 if (i == 1) { d2.value = pressure_val; d2s.value = pressure_status;}
                 if (i == 2) { d3.value = pressure_val; d3s.value = pressure_status;}
                 if (i == 3) { d4.value = pressure_val; d4s.value = pressure_status;}
                 if (i == 4) { d5.value = pressure_val; d5s.value = pressure_status;}
                 if (i == 5) { d6.value = pressure_val; d6s.value = pressure_status;}
              }
              else if ( i == 6 ) { // 
                 for (int j = 0; j < 6 ; j++) {
                    int sensor_status = Integer.parseInt(serDataR.substring(2*j,2*j+1));
                    if (j == 0) { d1ss.value = sensor_status;}
                    if (j == 1) { d2ss.value = sensor_status;}
                    if (j == 2) { d3ss.value = sensor_status;}
                    if (j == 3) { d4ss.value = sensor_status;}
                    if (j == 4) { d5ss.value = sensor_status;}
                    if (j == 5) { d6ss.value = sensor_status;}
                 }
              }
           }
        }
     }
     catch (NumberFormatException ex) {
        logger.log(Level.WARNING, "MaxiGauge:updateDeviceData> Format exception with " + name );
     }
     catch (Exception ex) {
      if ( ++comm_cnt_failure >= comm_max_failure ) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "MaxiGauge:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "MaxiGauge:updateDeviceData>" + ex.getMessage()); 
        }
        setErrorComStatus();
      }
     }    
   }

   public void executeCommand( DataElement e ) {

      String serDataW = "", serDataACK= "\u0005";;
      
      int onOff = 0;
      if ( e.value == 1 )
         onOff = 2;
      else if ( e.value == 2 )
         onOff = 1;

      if (e.name.contains("PR1ONOFF"))
         serDataW = "SEN," + onOff + ",0,0,0,0,0\r";
      else if (e.name.contains("PR2ONOFF"))
         serDataW = "SEN,0," + onOff + ",0,0,0,0\r";
      else if (e.name.contains("PR3ONOFF"))
         serDataW = "SEN,0,0," + onOff + ",0,0,0\r";
      else if (e.name.contains("PR4ONOFF"))
         serDataW = "SEN,0,0,0," + onOff + ",0,0\r";
      else if (e.name.contains("PR5ONOFF"))
         serDataW = "SEN,0,0,0,0," + onOff + ",0\r";
      else if (e.name.contains("PR6ONOFF"))
         serDataW = "SEN,0,0,0,0,0," + onOff + "\r";

      try {
         rs485.Write(serDataW);
         Thread.sleep(200); // Essential to get good timing through communication channel
         byte [] answer = null;
         while  (rs485.BytesAvailable() > 0 )
            answer = rs485.Read();
         rs485.Write(serDataACK);
         Thread.sleep(200); // Essential to get good timing through communication channel
         while (rs485.BytesAvailable() > 0 )
             answer = rs485.Read();
         Thread.sleep(2000); // Wait before resetting
         // Reset
         e.value = 0;
         holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "MaxiGauge>executeCommand:" + ex.getMessage());
        setErrorComStatus();
     }
   }

}; 
