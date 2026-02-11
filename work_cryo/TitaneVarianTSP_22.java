/*
 * This Class is the implementation of the Titane Varian TSP device (ver. 22)
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class TitaneVarianTSP_22 extends Device implements DataTypes {

   private Serial_Comm rs485_232;
   private static final Logger logger = Logger.getLogger("Main");
   private int com_error_count = 0; 
   private final int MAX_COM_ERROR = 5;

   public TitaneVarianTSP_22 (String _name, 
                           int _mbRegisterStart,
                           String serial_port, 
                           Baud baudrate, 
                           DataBits databits, 
                           Parity parity, 
                           StopBits stopbits, 
                           FlowControl flowcontrol) {

     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("TitaneVarianTSP_22:TitaneVarianTSP_22> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;


     // Titane properties (type 1: readable only)
     addDataElement( new DataElement(name, "P31STARTST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "P31CTRLST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P31ERR", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P31ABSCUR", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P31ABSVOLT", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=2));
     
     // Commands (type 2: command triggers)
     addDataElement( new DataElement(name, "P31SUBLONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // Writable status (type 3:read/writable values)
     addDataElement( new DataElement(name, "P31AUTOSTARTMODE", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P31RECOVERMODE", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P31CTRLOPMODE", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P31FILAMENT", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P31SUBLCUR", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P31SUBLTIME", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P31SUBLPERIOD", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
 
     // Com Status
     addDataElement( new DataElement(name, "P31COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("TitaneVarianTSP_22:TitaneVarianTSP_22> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

      // Instantiate communication channel
     try {
       rs485_232 = new Serial_Comm(serial_port, baudrate, databits, parity, stopbits, flowcontrol);
       rs485_232.Open();
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
   
     // Get monitoring data from device using RS485_232 Comm
     Vector <DataElement> d = new Vector<DataElement> ();
     d.add(getDataElement("P31STARTST"));
     d.add(getDataElement("P31CTRLST"));
     d.add(getDataElement("P31ERR"));
     d.add(getDataElement("P31ABSCUR"));
     d.add(getDataElement("P31ABSVOLT"));
     d.add(getDataElement("P31AUTOSTARTMODE"));
     d.add(getDataElement("P31RECOVERMODE"));
     d.add(getDataElement("P31CTRLOPMODE"));
     d.add(getDataElement("P31FILAMENT"));
     d.add(getDataElement("P31SUBLCUR"));
     d.add(getDataElement("P31SUBLTIME"));
     d.add(getDataElement("P31SUBLPERIOD"));
     d.add(getDataElement("P31COMST"));

     try {
        for (int i = 0 ; i < 12 ; i++) {
           //logger.finer(" --> " + name + ":next Modbus commands...");
           //addModbusCommand(); // Push Modbus commands in the loop is more reactive
           //logger.finer(" --> " + name + ":next command...");
           popCommand();  // Execute commands it in the loop is more reactive
           String serDataW = "",serDataR = "";
           int dataType = 0;
           switch (i) {
             case 0:
                 serDataW = "\u0081\u0030\u0032\u0047\u003F";// Start/Stop status
                 dataType = 0; // Logic
              break;
             case 1:
                 serDataW = "\u0081\u0030\u0032\u0053\u003F";// Controller Status
                 dataType = 1; // Numeric
              break;
             case 2:
                 serDataW = "\u0081\u0030\u0032\u0045\u003F";// Error Status
                 dataType = 1; // Numeric
              break;
             case 3:
                 serDataW = "\u0081\u0030\u0032\u0049\u003F";// Absorbed Current
                 dataType = 1; // Numeric
              break;
             case 4:
                 serDataW = "\u0081\u0030\u0032\u0056\u003F";// Absorbed Voltage
                 dataType = 1; // Numeric
              break;
             case 5:
                 serDataW = "\u0081\u0030\u0032\u0041\u003F";// AutoStart Mode
                 dataType = 2; // Alpha Numeric
              break;
             case 6:
                 serDataW = "\u0081\u0030\u0032\u0052\u003F";// Recover Mode
                 dataType = 2; // Alpha Numeric
              break;
             case 7:
                 serDataW = "\u0081\u0030\u0032\u004D\u003F";// Controller Operating Mode
                 dataType = 1; // Numeric
              break;
             case 8:
                 serDataW = "\u0081\u0030\u0032\u0046\u003F";// Active Filament
                 dataType = 1; // Numeric
              break;
             case 9:
                 serDataW = "\u0081\u0030\u0032\u004E\u003F";// Sublimation Current
                 dataType = 1; // Numeric
              break;
             case 10:
                 serDataW = "\u0081\u0030\u0032\u0054\u003F";// Sublimation Time
                 dataType = 1; // Numeric
              break;
             case 11:
                 serDataW = "\u0081\u0030\u0032\u0050\u003F";// Sublimation Period
                 dataType = 1; // Numeric
              break;
           }
           serDataW+=xor_checksum(serDataW.substring(1)); // Exclude <STX> for checksum calculation
           byte[] cmd = serDataW.getBytes("UTF-8");
           rs485_232.Write(cmd);
           logger.finer("TitaneVarianTSP_22:updateDeviceData> write to device:" + i + " --> " + serDataW);
           Thread.sleep(200); // Essential to get good timing through communication channel
           byte [] answer = null;
           while ( rs485_232.BytesAvailable() > 0 )
               answer = rs485_232.Read();
           if ( answer != null )
              serDataR = new String(answer, "UTF-8");
           logger.finer("TitaneVarianTSP_22:updateDeviceData> reading from device:" + serDataR + 
                      "(length=" + serDataR.length() + ")"); 
           if ( serDataR.length() < 2 || !xor_checksum(serDataR.substring(1)).equals(serDataR.charAt(serDataR.length() - 2)) ) {
              // Com Status Error
              com_error_count+=1;
              if ( com_error_count > MAX_COM_ERROR ) {
                  if ( hasWarned == false ) 
                     logger.log(Level.WARNING, "TitaneVarianTSP_22:updateDeviceData> Communication with " + name + " interrupted");
                  setErrorComStatus();
              }
              continue;
           }
           else {
              com_error_count = 0;
              d.elementAt(d.size() -1).value = 0; // OK COM
              if ( hasWarned == true ) {
                 hasWarned = false;
                 logger.info("TitaneVarianTSP_22:updateDeviceData> Communication with " + name + " back!");
              }
              String resultStr = "";
              switch (dataType) {
                case 0: // Logic
                    resultStr = serDataR.substring(5,6);
                 break;
                case 1: // Numeric
                    resultStr = serDataR.substring(5,10);
                 break;
                case 2: // Alpha Numeric
                    resultStr = serDataR.substring(5,11);
                 break;
               }
               d.elementAt(i).value = Double.parseDouble(resultStr);
               logger.finer("TitaneVarianTSP_22:updateDeviceData> resultStr:" + resultStr + 
                      "(length=" + resultStr.length() + ")"); 
              
           }
        }
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "TitaneVarianTSP_22:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "TitaneVarianTSP_22:updateDeviceData>" + ex.getMessage());
        }
        setErrorComStatus();
     }
   }

   public void executeCommand (DataElement e) {
  
      String serDataW = "", serDataR = "";
      if (e.name.contains("SUBLONOFF")) { // Sublimation On/Off command
         if ( e.value == 1 ) // Sublimation On
           serDataW = "\u0081\u0030\u0032\u0047\u0031";
         else if ( e.value == 2 ) // Sublimation Off
           serDataW = "\u0081\u0030\u0032\u0047\u0030";
      }
      else if (e.name.contains("AUTOSTARTMODE")) { // Set AutoStart mode
         if ( e.setvalue == 1 ) //  ON
           serDataW = "\u0081\u0030\u0032\u0041\u0030";
         else if ( e.setvalue == 2 ) // OFF
           serDataW = "\u0081\u0030\u0032\u0041\u0031";
      }
      else if (e.name.contains("RECOVERMODE")) { // Set Recover mode
         if ( e.setvalue == 0 ) //  AUTOMATIC
           serDataW = "\u0081\u0030\u0032\u0052\0030";
         else if ( e.setvalue == 2 ) // MANUAL
           serDataW = "\u0081\u0030\u0032\u0052\u0031";
      }
      else if (e.name.contains("CTRLOPMODE")) { // Set Controller Operation mode
         if ( e.setvalue == 0 ) //  Manual
           serDataW = "\u0081\u0030\u0036\004D\u0030\u0030\u0030\u0030\u0030";
         else if ( e.setvalue == 1 ) // Automatic
           serDataW = "\u0081\u0030\u0036\004D\u0030\u0030\u0030\u0030\u0031";
         else if ( e.setvalue == 2 ) // Remote
           serDataW = "\u0081\u0030\u0036\004D\u0030\u0030\u0030\u0030\u0032";
         else if ( e.setvalue == 3 ) // Automatic - Remote
           serDataW = "\u0081\u0030\u0036\004D\u0030\u0030\u0030\u0030\u0033";
      }
      else if (e.name.contains("FILAMENT")) { // Set Filament mode
         if ( e.setvalue == 0 ) //  Ti-Ball
           serDataW = "\u0081\u0030\u0036\0046\u0030\u0030\u0030\u0030\u0030";
         else if ( e.setvalue == 1 ) // TSP 1
           serDataW = "\u0081\u0030\u0036\0046\u0030\u0030\u0030\u0030\u0031";
         else if ( e.setvalue == 2 ) // TSP 2
           serDataW = "\u0081\u0030\u0036\0046\u0030\u0030\u0030\u0030\u0032";
         else if ( e.setvalue == 3 ) // TSP 3
           serDataW = "\u0081\u0030\u0036\0046\u0030\u0030\u0030\u0030\u0033";
      }
      else if (e.name.contains("SUBLTIME")) { // Set Sublimation time
         String data = Integer.toString((int)e.setvalue);
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
           serDataW = "\u0081\u0030\u0036\u0054" + data;
      }
      else if (e.name.contains("SUBLCUR")) { // Set Sublimation current
         String data = Integer.toString((int)e.setvalue);
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
           serDataW = "\u0081\u0030\u0036\u004E" + data;
      }
      else if (e.name.contains("SUBLPERIOD")) { // Set Sublimation period
         String data = Integer.toString((int)e.setvalue);
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
           serDataW = "\u0081\u0030\u0036\u0050" + data;
      }
      try {
         serDataW+=xor_checksum(serDataW.substring(1));// Exclude <STX> for checksum calculation
         byte[] cmd = serDataW.getBytes("UTF-8");
         rs485_232.Write(cmd);
         Thread.sleep(200); // Essential to get good timing through communication channel
         byte [] answer = null;
         while ( rs485_232.BytesAvailable() > 0 )
            answer = rs485_232.Read();
         if ( answer != null )
           serDataR = new String(answer, "UTF-8");
        logger.finer("TitaneVarianTSP_22:executeCommand> answer from device:" + serDataR + 
                   "(length=" + serDataR.length() + ")");
         if (e.type == DataType.TRIGGER) {
            Thread.sleep(2000); // Wait before resetting
            e.value = 0;
            holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "IonicAgilentDual>executeCommand:" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "IonicAgilentDual:executeCommand> Communication with " + name + " interrupted");
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
      return (Integer.toHexString(res).toUpperCase());
   }
}; 
