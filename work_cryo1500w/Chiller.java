/*
 * This Class is the implementation of the Chiller device
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class Chiller extends Device {

   private Serial_Comm rs232;
   private static final Logger logger = Logger.getLogger("Main");

   public Chiller (String _name,
                     int _mbRegisterStart, 
                     String serial_port, 
                     Baud baudrate, 
                     DataBits databits, 
                     Parity parity, 
                     StopBits stopbits, 
                     FlowControl flowcontrol) {

     pause = 3000; // pause for Device query thread cycle
     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Chiller:Chiller> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Read only values
     
     // TCMuxShied
     addDataElement( new DataElement(name, "TCMUX_T1", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     addDataElement( new DataElement(name, "TCMUX_T2", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TCMUX_T3", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TCMUX_T4", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TCMUX_T5", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TCMUX_T6", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TCMUX_T7", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TCMUX_T8", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TCMUX_T9", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
    
    // Controller Chiller comm
    addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=2));

    mbRegisterEnd+=1;

    logger.finer("Chiller:Chiller> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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
   
     popCommand();  // Execute commands in the loop is more reactive
     
     // Get monitoring data from device using rs232 Comm   
     // Com Status
     DataElement dcom = getDataElement("COMST");
     try {
        String serDataR = "", serDataW = "getdata";
        logger.finer("Chiller:updateDeviceData> final serDataW=" + serDataW);
        rs232.Write(serDataW);
        Thread.sleep(200); // Essential to get good timing through communication channel
        byte [] answer = null;
        while (rs232.BytesAvailable() > 0 )
          answer = rs232.Read();
        if ( answer != null ) 
           serDataR = new String(answer, "UTF-8");
        logger.finer("Chiller:updateDeviceData> serDataR=START-" + serDataR + "-END");
        String [] data = serDataR.split(";");
        if ( data.length == 9) {
           // Extract data from answer
           for (int i = 1; i <= 9; i++) {
              DataElement temp = getDataElement("TCMUX_T" + Integer.toString(i));
              temp.value = Double.parseDouble(data[i-1]);
           }
           // If arrived here OK
           dcom.value = 0;
        }
        else {
           dcom.value = 1;
           logger.finer("Chiller:updateDeviceData> data missing, got only " + data.length );
           }
     }
     catch (NumberFormatException ex) {
        logger.log(Level.WARNING, "Chiller:updateDeviceData> Format exception with " + name );
        logger.log(Level.WARNING, "Chiller:updateDeviceData>" + ex.getMessage()); 
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "Chiller:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "Chiller:updateDeviceData>" + ex.getMessage()); 
        }
        setErrorComStatus();
     }    
   }

   public void executeCommand( DataElement e ) {
   }
}; 
