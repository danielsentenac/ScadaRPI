/*
 * This Class is the implementation of the Turbo Pfeiffer DCU device
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class TurboVarianV81AG extends Device {

   private Serial_Comm rs485;
   private static final Logger logger = Logger.getLogger("Main");
   private int com_error_count = 0; 
   private final int MAX_COM_ERROR = 5;

   public TurboVarianV81AG (String _name, 
                            int _mbRegisterStart,
                            String serial_port, 
                            Baud baudrate, 
                            DataBits databits, 
                            Parity parity, 
                            StopBits stopbits, 
                            FlowControl flowcontrol) {

     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("TurboVarianV81AG:TurboVarianV81AG> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;


     // Turbo properties
     addDataElement( new DataElement(name, "P1SPEED", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "P1FSPEED", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P1HR", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P1PWR", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P1BTEMP", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P1TEMP", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P1ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P1STYST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P1BERR", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     // Commands
     addDataElement( new DataElement(name, "P1ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P1STYONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     // Com Status
     addDataElement( new DataElement(name, "P1COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("TurboVarianV81AG:TurboVarianV81AG> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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
   
     // Get monitoring data from device using RS485 Comm
     Vector <DataElement> d = new Vector<DataElement> ();
     d.add(getDataElement("P1SPEED"));
     d.add(getDataElement("P1FSPEED"));
     d.add(getDataElement("P1HR"));
     d.add(getDataElement("P1PWR"));
     d.add(getDataElement("P1BTEMP"));
     d.add(getDataElement("P1TEMP"));
     d.add(getDataElement("P1ST"));
     d.add(getDataElement("P1STYST"));
     d.add(getDataElement("P1BERR"));
     d.add(getDataElement("P1COMST"));

     try {
        for (int i = 0 ; i < 9 ; i++) {
           //logger.finer(" --> " + name + ":next Modbus commands...");
           //addModbusCommand(); // Push Modbus commands in the loop is more reactive
           //logger.finer(" --> " + name + ":next command...");
           popCommand();  // Execute commands it in the loop is more reactive
           String serDataW = "",serDataR = "";
           int dataType = 0;
           switch (i) {
             case 0:
                 serDataW = "\u0002\u0080\u0032\u0030\u0033\u0030\u0003";// Act Rot Speed
                 dataType = 1; // Numeric
              break;
             case 1:
                 serDataW = "\u0002\u0080\u0031\u0032\u0030\u0030\u0003";// Set Rot Speed
                 dataType = 1; // Numeric
              break;
             case 2:
                 serDataW = "\u0002\u0080\u0033\u0030\u0032\u0030\u0003";// Operating hours
                 dataType = 1; // Numeric
              break;
             case 3:
                 serDataW = "\u0002\u0080\u0032\u0030\u0032\u0030\u0003";// Power in watt
                 dataType = 1; // Numeric
              break;
             case 4:
                 serDataW = "\u0002\u0080\u0032\u0031\u0031\u0030\u0003";// Electronics temperature
                 dataType = 1; // Numeric
              break;
             case 5:
                 serDataW = "\u0002\u0080\u0032\u0030\u0034\u0030\u0003";// Turbo pump temperature
                 dataType = 1; // Numeric
              break;
             case 6:
                 serDataW = "\u0002\u0080\u0030\u0030\u0030\u0030\u0003";// Turbo ON/OFF
                 dataType = 0; // Logic
              break;
             case 7:
                 serDataW = "\u0002\u0080\u0030\u0030\u0031\u0030\u0003";// Standby ON/OFF
                 dataType = 0; // Logic
              break;
             case 8:
                 serDataW = "\u0002\u0080\u0032\u0030\u0036\u0030\u0003";// Error code
                 dataType = 1; // Numeric
              break;
           }
           serDataW+=xor_checksum(serDataW.substring(1)); // Exclude <STX> for checksum calculation
           rs485.Write(serDataW.substring(1)); // Exclude <STX> for checksum calculation
           byte[] cmd = serDataW.getBytes("UTF-8");
           rs485.Write(cmd);
           logger.finer("TurboVarianV81AG:updateDeviceData> write to device:" + i + " --> " + serDataW);
           Thread.sleep(200); // Essential to get good timing through communication channel
           byte [] answer = null;
           while ( rs485.BytesAvailable() > 0 )
               answer = rs485.Read();
           if ( answer != null )
              serDataR = new String(answer, "UTF-8");
           logger.finer("TurboVarianV81AG:updateDeviceData> reading from device:" + serDataR + 
                      "(length=" + serDataR.length() + ")"); 
           if ( serDataR.length() < 2 || !xor_checksum(serDataR.substring(1)).equals(serDataR.charAt(serDataR.length() - 2)) ) {
              // Com Status Error
              com_error_count+=1;
              if ( com_error_count > MAX_COM_ERROR && hasWarned == false) {
                 logger.log(Level.WARNING, "TurboVarianV81AG:updateDeviceData> Communication with " + name + " interrupted");
                 setErrorComStatus();
              }
              continue;
           }
           else {
              com_error_count = 0;
              d.elementAt(d.size() -1).value = 0; // OK COM
              if ( hasWarned == true ) {
                 hasWarned = false;
                 logger.info("TurboVarianV81AG:updateDeviceData> Communication with " + name + " back!");
              }
              String resultStr = "";
              switch (dataType) {
                case 0: // Logic
                    resultStr = serDataR.substring(6,7);
                 break;
                case 1: // Numeric
                    resultStr = serDataR.substring(6,12);
                 break;
               }
               d.elementAt(i).value = Double.parseDouble(resultStr);
               logger.finer("TitaneVarianTSP:updateDeviceData> resultStr:" + resultStr + 
                      "(length=" + resultStr.length() + ")"); 
              
           }
        }
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "TurboVarianV81AG:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "TurboVarianV81AG:updateDeviceData>" + ex.getMessage());
           setErrorComStatus();
        }
     }
   }

   public void executeCommand (DataElement e) {
  
      String serDataW = "" ,serDataR = "";
      if (e.name.contains("STYONOFF")) { // Standby On/Off command
         if ( e.value == 1 ) // Stanby On
           serDataW = "\u0002\u0080\u0030\u0030\u0031\u0031\u0031\u0003";
         else if ( e.value == 2 ) // Standby Off
           serDataW = "\u0002\u0080\u0030\u0030\u0031\u0031\u0030\u0003";
      }
      else { // Start/Stop command
         if ( e.value == 1 ) // Start
           serDataW = "\u0002\u0080\u0030\u0030\u0030\u0031\u0031\u0003";
         else if ( e.value == 2 ) // Stop
           serDataW = "\u0002\u0080\u0030\u0030\u0030\u0031\u0030\u0003";
      }
      try {
         serDataW+=xor_checksum(serDataW.substring(1));// Exclude <STX> for checksum calculation
         byte[] cmd = serDataW.getBytes("UTF-8");
         rs485.Write(cmd);
         Thread.sleep(200); // Essential to get good timing through communication channel
         byte [] answer = null;
         while ( rs485.BytesAvailable() > 0 )
            answer = rs485.Read();
         if ( answer != null )
           serDataR = new String(answer, "UTF-8");
        logger.finer("TitaneVarianTSP:executeCommand> answer from device:" + serDataR + 
                   "(length=" + serDataR.length() + ")");
         Thread.sleep(2000); // Wait before resetting
         e.value = 0;
         holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
      }
      catch (Exception ex) {
        ex.printStackTrace();
        logger.log(Level.SEVERE, "TurboVarianV81AG>executeCommand:" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "TurboVarianV81AG:executeCommand> Communication with " + name + " interrupted");
     }
   }
  
   //
   // Utility method for checksum calculation
   //
   String xor_checksum(String input) {
      short res = 0;
      for (int i = 0; i < input.length(); i++) {
         res ^= input.charAt(i);
      }
      return (Integer.toHexString(res));
   }
}; 
