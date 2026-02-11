/*
 * This Class is the implementation of the XGSGauge device
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class XGSGauge extends Device {

   private Serial_Comm rs232;
   private static final Logger logger = Logger.getLogger("Main");

   public XGSGauge (String _name,
                    int _mbRegisterStart, 
                    String serial_port, 
                    Baud baudrate, 
                    DataBits databits, 
                    Parity parity, 
                    StopBits stopbits, 
                    FlowControl flowcontrol) {

     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("XGSGauge:XGSGauge> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Gauge Pressure
     addDataElement( new DataElement(name, "PR7", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     // Gauge Emission Status
     addDataElement( new DataElement(name, "PR7ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
     // Gauge Degas Status
     addDataElement( new DataElement(name, "PR7DST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
    // Gauge Filament Status
     addDataElement( new DataElement(name, "PR7FST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     // Gauge Commands (ON/OFF)
     addDataElement( new DataElement(name, "PR7EMULT1ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR7EMULT2ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PR7DEGASONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     

    // Controller XGSgauge comm
    addDataElement( new DataElement(name, "GA4COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

    mbRegisterEnd+=1;

    logger.finer("XGSGauge:XGSGauge> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     // Instantiate communication channel
     try {
       rs232 = new Serial_Comm(serial_port, baudrate, databits, parity, stopbits, flowcontrol);
       rs232.Open();
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
   
     // Get monitoring data from device using RS232 Comm
     DataElement d7 = getDataElement("PR7");
     DataElement d7s = getDataElement("PR7ST");
     DataElement d7ds = getDataElement("PR7DST");
     DataElement d7fs = getDataElement("PR7FST");
     // Com Status
     DataElement dcom = getDataElement("GA4COMST");

     try {
        for (int i = 0; i < 3; i++) {
           popCommand();  // Execute commands in the loop is more reactive
           String serDataR = "", serDataW = "", serDataACK= "\u0005";
           switch (i) {
               case 0:  serDataW = "#0002UHFIG1\r"; // Gauge pressure value
             break;
               case 1:  serDataW = "#0032UHFIG1\r"; // Gauge Emission status (ON/OFF)
             break;
               case 2:  serDataW = "#0042UHFIG1\r"; // Gauge Degas status (ON/OFF)
             break;
               case 3:  serDataW = "#0034UHFIG1\r"; // Gauge Filament status (1/2)
             break;
           }
           logger.finer("XGSGauge:updateDeviceData> serDataW=" + serDataW);
           rs232.Write(serDataW);
           Thread.sleep(200); // Essential to get good timing through communication channel
           byte [] answer = null;
           while  (rs232.BytesAvailable() > 0 )
             answer = rs232.Read();
           if ( answer != null) 
              serDataR = new String(answer, "UTF-8");
           logger.finer("XGSGauge:updateDeviceData> serDataR=" + serDataR);
           if (serDataR.length() == 0 ) {// Com Status Error - reset all values -
              if ( hasWarned == false) 
                 logger.log(Level.WARNING, "XGSGauge:updateDeviceData> Communication with " + name + " interrupted");
              setErrorComStatus();
              break;
           }
           else {
              dcom.value = 0; // OK COM
              if ( hasWarned == true ) {
                 hasWarned = false;
                 logger.info("XGSGauge:updateDeviceData> Communication with " + name + " back!");
              }
              if ( i == 0 ) { // Sensor pressure
                 double pressure_val = Double.parseDouble(serDataR.substring(1));
                 d7.value = pressure_val;
              }
              else {
                 int status_val = Integer.parseInt(serDataR.substring(1));
                 if ( i == 1 ) d7s.value  =  status_val;  // Emission status
                 if ( i == 2 ) d7ds.value =  status_val;  // Degas status
                 if ( i == 3 ) d7fs.value =  status_val;  // Filament status
              }
           } 
        }
     }
     catch (NumberFormatException ex) {
        logger.log(Level.WARNING, "XGSGauge:updateDeviceData> Format exception with " + name );
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "XGSGauge:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "XGSGauge:updateDeviceData>" + ex.getMessage());
        }
        setErrorComStatus();
     }
   }
   
   public void executeCommand( DataElement e ) {

      String serDataW = "";

      if ( e.name.equals("PR7EMULT1ONOFF") && e.value == 1 )
          serDataW = "#0031UHFIG1\r";
      else if ( e.name.equals("PR7EMULT1ONOFF") && e.value == 2 )
         serDataW = "#0030UHFIG1\r";
      else if ( e.name.equals("PR7EMULT2ONOFF") && e.value == 1 )
         serDataW = "#0033UHFIG1\r";
      else if ( e.name.equals("PR7EMULT2ONOFF") && e.value == 2 )
         serDataW = "#0030UHFIG1\r";
      else if ( e.name.equals("PR7DEGASONOFF") && e.value == 1 )
         serDataW = "#0041UHFIG1\r";
      else if ( e.name.equals("PR7DEGASONOFF") && e.value == 2 )
         serDataW = "#0040UHFIG1\r";
 
      try {
         rs232.Write(serDataW);
         Thread.sleep(200); // Essential to get good timing through communication channel
         byte [] answer = null;
         while  (rs232.BytesAvailable() > 0 )
            answer = rs232.Read();
         Thread.sleep(2000); // Wait before resetting
         // Reset & Update modbus register
         e.value = 0;
         holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "XGSGauge>executeCommand:" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "XGSGauge:executeCommand> Communication with " + name + " interrupted");
     }
   }

}; 
