/*
 * This Class is the implementation of the LakeShore device
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class LakeShore extends Device {

   private Serial_Comm rs232;
   private static final Logger logger = Logger.getLogger("Main");

   public LakeShore (String _name,
                     int _mbRegisterStart, 
                     String serial_port, 
                     Baud baudrate, 
                     DataBits databits, 
                     Parity parity, 
                     StopBits stopbits, 
                     FlowControl flowcontrol) {

     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("LakeShore:LakeShore> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Lakeshore temperatures
     addDataElement( new DataElement(name, "T1", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     addDataElement( new DataElement(name, "T2", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T3", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T4", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T5", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T6", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T7", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "T8", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));

    // Controller Lakeshore comm
    addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

    mbRegisterEnd+=1;

    logger.finer("LakeShore:LakeShore> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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
     DataElement d1 = getDataElement("T1");
     DataElement d2 = getDataElement("T2");
     DataElement d3 = getDataElement("T3");
     DataElement d4 = getDataElement("T4");
     DataElement d5 = getDataElement("T5");
     DataElement d6 = getDataElement("T6");
     DataElement d7 = getDataElement("T7");
     DataElement d8 = getDataElement("T8");
    
     // Com Status
     DataElement dcom = getDataElement("COMST");

     try {
        for (int i = 0; i < 8; i++) {
           //logger.finer(" --> " + name + ":next Modbus commands...");
           //addModbusCommand(); // Push Modbus commands in the loop is more reactive
           //logger.finer(" --> " + name + ":next command...");
           popCommand();  // Execute commands in the loop is more reactive
           String serDataR = "", serDataW = "";
           serDataW = "KRDG? " + Integer.toString(i+1) + "\n\r";
           logger.finer("LakeShore:updateDeviceData> " + name + " WRITE serDataW= " + serDataW);
           rs232.Write(serDataW);
           Thread.sleep(200); // Essential to get good timing through communication channel
           byte [] answer = null;
           while  (rs232.BytesAvailable() > 0 )
             answer = rs232.Read();
           if ( answer != null) 
              serDataR = new String(answer, "UTF-8");
           logger.finer("LakeShore:updateDeviceData> " + name + " serDataR=START-" + serDataR + "-END");
           if (serDataR.length() <= 1 ) {// Com Status Error - reset all values -
              if ( hasWarned == false )
                 logger.log(Level.WARNING, "LakeShore:updateDeviceData> Communication with " + name + " interrupted");
              setErrorComStatus();
              break;
           }
           else {
              dcom.value = 0; // OK COM
              if ( hasWarned == true ) {
                 hasWarned = false;
                 logger.info("LakeShore:updateDeviceData> Communication with " + name + " back!");
              }
              double temperature_val = Double.parseDouble(serDataR.substring(serDataR.indexOf("+")+1));
              logger.finer("LakeShore:updateDeviceData> " + i + " temperature=" + temperature_val);
              if (i == 0) { d1.value = temperature_val;}
              if (i == 1) { d2.value = temperature_val;}
              if (i == 2) { d3.value = temperature_val;}
              if (i == 3) { d4.value = temperature_val;}
              if (i == 4) { d5.value = temperature_val;}
              if (i == 5) { d6.value = temperature_val;}
              if (i == 6) { d7.value = temperature_val;}
              if (i == 7) { d8.value = temperature_val;}
           }
        }
     }
     catch (NumberFormatException ex) {
        logger.log(Level.WARNING, "LakeShore:updateDeviceData> Format exception with " + name );
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "LakeShore:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "LakeShore:updateDeviceData>" + ex.getMessage()); 
        }
        setErrorComStatus();
     }    
   }

   public void executeCommand( DataElement e ) {
   }

}; 
