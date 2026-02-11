/*
 * This Class is the implementation of the MksRotor device
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class MksRotor extends Device {

   private Serial_Comm rs232;
   private static final Logger logger = Logger.getLogger("Main");

   public MksRotor (String _name,
                     int _mbRegisterStart, 
                     String serial_port, 
                     Baud baudrate, 
                     DataBits databits, 
                     Parity parity, 
                     StopBits stopbits, 
                     FlowControl flowcontrol) {

     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("MksRotor:MksRotor> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Gauge Pressure
     addDataElement( new DataElement(name, "PR1", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     addDataElement( new DataElement(name, "PR1ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));

    // Controller MksRotor comm
    addDataElement( new DataElement(name, "GA5COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

    mbRegisterEnd+=1;

    logger.finer("MksRotor:MksRotor> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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
   
     // Get monitoring data from device using rs232 Comm
     DataElement d1 = getDataElement("PR1");
     DataElement d2 = getDataElement("PR1ST");
     // Com Status
     DataElement dcom = getDataElement("GA5COMST");

     try {
           popCommand();  // Execute commands in the loop is more reactive
           String serDataR = "", serDataW = "";
           serDataW = "val\r";
           logger.finer("MksRotor:updateDeviceData> serDataW=" + serDataW);
           rs232.Write(serDataW);
           Thread.sleep(200); // Essential to get good timing through communication channel
           byte [] answer = null;
           while  (rs232.BytesAvailable() > 0 )
             answer = rs232.Read();
           if ( answer != null) 
              serDataR = new String(answer, "UTF-8");
           logger.finer("MksRotor:updateDeviceData> serDataR=START-" + serDataR + "-END");
           if (serDataR.length() <= 1 ) {// Com Status Error - reset all values -
              if ( hasWarned == false )
                 logger.log(Level.WARNING, "MksRotor:updateDeviceData> Communication with " + name + " interrupted");
              setErrorComStatus();
           }
           serDataR = serDataR.substring(1, serDataR.length()-3);
           logger.finer("MksRotor:updateDeviceData2> serDataR=START-" + serDataR + "-END");
           // Store value
           d1.value = (double) Double.valueOf(serDataR);
           d2.value = 1.0;
           dcom.value = 0;
     }
     catch (NumberFormatException ex) {
        logger.log(Level.WARNING, "MksRotor:updateDeviceData> Format exception with " + name );
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "MksRotor:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "MksRotor:updateDeviceData>" + ex.getMessage()); 
        }
        setErrorComStatus();
        d2.value = 0;
        
        
     }    
   }

   public void executeCommand( DataElement e ) {

      String serDataW = "", serDataACK= "\u0005";;
      try {
         rs232.Write(serDataW);
         Thread.sleep(200); // Essential to get good timing through communication channel
         byte [] answer = null;
         while (rs232.BytesAvailable() > 0 )
             answer = rs232.Read();
         Thread.sleep(2000); // Wait before resetting
         // Reset
         e.value = 0;
         holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "MksRotor>executeCommand:" + ex.getMessage());
        setErrorComStatus();
     }
   }

}; 
