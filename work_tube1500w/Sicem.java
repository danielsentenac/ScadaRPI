/*
 * This Class is the implementation of the Sicem device
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class Sicem extends Device {

   private Serial_Comm rs232;
   private static final Logger logger = Logger.getLogger("Main");

   public Sicem (String _name,
                     int _mbRegisterStart, 
                     String serial_port, 
                     Baud baudrate, 
                     DataBits databits, 
                     Parity parity, 
                     StopBits stopbits, 
                     FlowControl flowcontrol) {

     pause = 5000; // pause for Device query thread cycle
     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Sicem:Sicem> " + name + " Modbus registers starts at offset " + mbRegisterStart);

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
     addDataElement( new DataElement(name, "TCMUX_T9", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2)); // INT TEMP
     
     // Temperatures
     addDataElement( new DataElement(name, "T1", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T2", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T3", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T4", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T5", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T6", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T7", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T8", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T9", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T10", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T11", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T12", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     
     // SetPoints
     addDataElement( new DataElement(name, "SETP1", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP2", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP3", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP4", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP5", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP6", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP7", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP8", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP9", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP10", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP11", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "SETP12", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     
     // For Operation
     addDataElement( new DataElement(name, "TEMPSTEP", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TEMPMAX", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TIMEINTER", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TIME", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
    
    // Controller Sicem comm
    addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=2));

    mbRegisterEnd+=1;

    logger.finer("Sicem:Sicem> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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
   
     popCommand();  // Execute commands it in the loop is more reactive
     // Get monitoring data from device using rs232 Comm   
     // Com Status
     DataElement dcom = getDataElement("COMST");

     try {
        String serDataR = "", serDataW = "getdata";
        logger.finer("Sicem:updateDeviceData> final serDataW=" + serDataW);
        rs232.Write(serDataW);
        Thread.sleep(200); // Essential to get good timing through communication channel
        byte [] answer = null;
        while (rs232.BytesAvailable() > 0 )
          answer = rs232.Read();
        if ( answer != null ) 
           serDataR = new String(answer, "UTF-8");
        logger.finer("Sicem:updateDeviceData> serDataR=START-" + serDataR + "-END");
        String [] data = serDataR.split(";");
        if ( data.length == 33) {
           // Extract data from answer
           for (int i = 1; i <= 12; i++) {
              DataElement temp = getDataElement("T" + Integer.toString(i));
              temp.value = Double.parseDouble(data[i-1]);
           }
           for (int i = 1; i <= 12; i++) {
              DataElement temp = getDataElement("SETP" + Integer.toString(i));
              temp.value = Double.parseDouble(data[11 + i]);
           }
           for (int i = 1; i <= 9; i++) {
              DataElement temp = getDataElement("TCMUX_T" + Integer.toString(i));
              temp.value = Double.parseDouble(data[23 + i]);
           }
           // If arrived here OK
           dcom.value = 0;
        }
        else
           logger.log(Level.WARNING, "Sicem:updateDeviceData> data missing, got only " + data.length );
     }
     catch (NumberFormatException ex) {
        logger.log(Level.WARNING, "Sicem:updateDeviceData> Format exception with " + name );
        logger.log(Level.WARNING, "Sicem:updateDeviceData>" + ex.getMessage()); 
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "Sicem:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "Sicem:updateDeviceData>" + ex.getMessage()); 
        }
        setErrorComStatus();
     }    
   }

   public void executeCommand( DataElement e ) {

      String serDataW = "";
      if (e.name.contains("SETP")) { // SetPoint command
         logger.finer("Sicem::executeCommand> e.name= " + e.name);
         String modulenumber = e.name.substring(4,e.name.length());
         logger.finer("Sicem::executeCommand> modulenumber= " + modulenumber);
         if (modulenumber.length() == 1) modulenumber = "0" + modulenumber;
         serDataW = "SETP" + modulenumber + "=" + e.setvalue;
         logger.finer("Sicem::executeCommand> write " + serDataW);
      }
      try {
         rs232.Write(serDataW);
      }
      catch (Exception ex) {
           logger.log(Level.WARNING, "Sicem:executeCommand> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "Sicem:executeCommand>" + ex.getMessage()); 
        }  
   }
}; 
