/*
 * This Class is the implementation of the Titane Varian TSP device (ver. 32)
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class TitaneVarianTSP_32 extends Device implements DataTypes {

   private Serial_Comm rs485_232;
   private static final Logger logger = Logger.getLogger("Main");
   private int com_error_count = 0; 
   private final int MAX_COM_ERROR = 5;

   public TitaneVarianTSP_32 (String _name, 
                           int _mbRegisterStart,
                           String serial_port, 
                           Baud baudrate, 
                           DataBits databits, 
                           Parity parity, 
                           StopBits stopbits, 
                           FlowControl flowcontrol) {

     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("TitaneVarianTSP:TitaneVarianTSP> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;


     // Titane properties (type 1: readable only)
     addDataElement( new DataElement(name, "P32STARTST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "P32CTRLST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32ERR", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32BTEMP", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32CYCLE", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32HR", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32ABSCUR", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32ABSVOLT", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=2));
     
     // Commands (type 2: command triggers)
     addDataElement( new DataElement(name, "P32SUBLONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // Writable status (type 3:read/writable values)
     addDataElement( new DataElement(name, "P32REMOTEMODE", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32OPMODE", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32CTRLOPMODE", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32FILAMENT", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32SUBLCUR", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32SUBLTIME", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32SUBLPERIOD", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P32SUBLWAIT", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
 
     // Com Status
     addDataElement( new DataElement(name, "P32COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("TitaneVarianTSP:TitaneVarianTSP> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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
     d.add(getDataElement("P32STARTST"));
     d.add(getDataElement("P32CTRLST"));
     d.add(getDataElement("P32ERR"));
     d.add(getDataElement("P32BTEMP"));
     d.add(getDataElement("P32CYCLE"));
     d.add(getDataElement("P32HR"));
     d.add(getDataElement("P32ABSCUR"));
     d.add(getDataElement("P32ABSVOLT"));
     d.add(getDataElement("P32REMOTEMODE"));
     d.add(getDataElement("P32OPMODE"));
     d.add(getDataElement("P32CTRLOPMODE"));
     d.add(getDataElement("P32FILAMENT"));
     d.add(getDataElement("P32SUBLCUR"));
     d.add(getDataElement("P32SUBLTIME"));
     d.add(getDataElement("P32SUBLPERIOD"));
     d.add(getDataElement("P32SUBLWAIT"));
     d.add(getDataElement("P32COMST"));

     try {
        for (int i = 0 ; i < 16 ; i++) {
           //logger.finer(" --> " + name + ":next Modbus commands...");
           //addModbusCommand(); // Push Modbus commands in the loop is more reactive
           //logger.finer(" --> " + name + ":next command...");
           popCommand();  // Execute commands it in the loop is more reactive
           String serDataW = "",serDataR = "";
           int dataType = 0;
           switch (i) {
             case 0:
                 serDataW = "\u0002\u0080\u0030\u0032\u0032\u0030\u0003";// Start/Stop status
                 dataType = 0; // Logic
              break;
             case 1:
                 serDataW = "\u0002\u0080\u0032\u0030\u0035\u0030\u0003";// Controller Status
                 dataType = 1; // Numeric
              break;
             case 2:
                 serDataW = "\u0002\u0080\u0032\u0030\u0036\u0030\u0003";// Error Status
                 dataType = 1; // Numeric
              break;
             case 3:
                 serDataW = "\u0002\u0080\u0032\u0032\u0036\u0030\u0003";// Controller Temperature
                 dataType = 1; // Numeric
              break;
             case 4:
                 serDataW = "\u0002\u0080\u0033\u0039\u0038\u0030\u0003";// Controller Cycles
                 dataType = 1; // Numeric
              break;
             case 5:
                 serDataW = "\u0002\u0080\u0033\u0039\u0039\u0030\u0003";// Controller Hours
                 dataType = 1; // Numeric
              break;
             case 6:
                 serDataW = "\u0002\u0080\u0038\u0032\u0032\u0030\u0003";// Absorbed Current
                 dataType = 1; // Numeric
              break;
             case 7:
                 serDataW = "\u0002\u0080\u0038\u0032\u0030\u0030\u0003";// Absorbed Voltage
                 dataType = 1; // Numeric
              break;
             case 8:
                 serDataW = "\u0002\u0080\u0030\u0030\u0038\u0030\u0003";// Remote Mode
                 dataType = 1; // Numeric
              break;
             case 9:
                 serDataW = "\u0002\u0080\u0036\u0030\u0032\u0030\u0003";// Operating Mode
                 dataType = 2; // Alpha Numeric
              break;
             case 10:
                 serDataW = "\u0002\u0080\u0036\u0037\u0030\u0030\u0003";// Controller Operating Mode
                 dataType = 1; // Numeric
              break;
             case 11:
                 serDataW = "\u0002\u0080\u0036\u0037\u0032\u0030\u0003";// Active Filament
                 dataType = 1; // Numeric
              break;
             case 12:
                 serDataW = "\u0002\u0080\u0036\u0037\u0032\u0030\u0003";// Sublimation Current
                 dataType = 1; // Numeric
              break;
             case 13:
                 serDataW = "\u0002\u0080\u0036\u0037\u0034\u0030\u0003";// Sublimation Time
                 dataType = 1; // Numeric
              break;
             case 14:
                 serDataW = "\u0002\u0080\u0036\u0037\u0033\u0030\u0003";// Sublimation Period
                 dataType = 1; // Numeric
              break;
             case 15:
                 serDataW = "\u0002\u0080\u0036\u0037\u0035\u0030\u0003";// Wait Time
                 dataType = 1; // Numeric
              break;
           }
           serDataW+=xor_checksum(serDataW.substring(1)); // Exclude <STX> for checksum calculation
           byte[] cmd = serDataW.getBytes("UTF-8");
           rs485_232.Write(cmd);
           //logger.finer("TitaneVarianTSP:updateDeviceData> write to device:" + i + " --> " + serDataW);
           Thread.sleep(200); // Essential to get good timing through communication channel
           byte [] answer = null;
           while ( rs485_232.BytesAvailable() > 0 )
               answer = rs485_232.Read();
           if ( answer != null )
              serDataR = new String(answer, "UTF-8");
           //logger.finer("TitaneVarianTSP:updateDeviceData> reading from device:" + serDataR + 
           //           "(length=" + serDataR.length() + ")"); 
           if ( serDataR.length() < 2 || !xor_checksum(serDataR.substring(1)).equals(serDataR.charAt(serDataR.length() - 2)) ) {
              // Com Status Error
              com_error_count+=1;
              if ( com_error_count > MAX_COM_ERROR ) {
                 d.elementAt(d.size() -1).value = 1; // ERR COM
                 setErrorComStatus();
              }
              //logger.log(Level.SEVERE, "TitaneVarianTSP:updateDeviceData> Communication with " + name + " interrupted");
              continue;
           }
           else {
              com_error_count = 0;
              d.elementAt(d.size() -1).value = 0; // OK COM
              String resultStr = "";
              switch (dataType) {
                case 0: // Logic
                    resultStr = serDataR.substring(6,7);
                 break;
                case 1: // Numeric
                    resultStr = serDataR.substring(6,12);
                 break;
                case 2: // Alpha Numeric
                    resultStr = serDataR.substring(6,16);
                 break;
               }
               logger.finer("TitaneVarianTSP:updateDeviceData> resultStr:" + resultStr + 
                      "(length=" + resultStr.length() + ")"); 
              
           }
        }
     }
     catch (Exception ex) {
        //ex.printStackTrace();
        //logger.log(Level.SEVERE, "TitaneVarianTSP>updateDeviceData:" + ex.getMessage());
        //logger.log(Level.SEVERE, "TitaneVarianTSP:updateDeviceData> Communication with " + name + " interrupted");
        setErrorComStatus();
        d.elementAt(d.size() -1).value = 1; // ERR COM
     }
   }

   public void executeCommand (DataElement e) {
  
      String serDataW = "", serDataR = "";
      if (e.name.contains("SUBLONOFF")) { // Sublimation On/Off command
         if ( e.value == 1 ) // Sublimation On
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0032\u0003";
         else if ( e.value == 2 ) // Sublimation Off
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0003";
      }
      else if (e.name.contains("REMOTEMODE")) { // Set Remote mode
         if ( e.setvalue == 0 ) //  Serial mode
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0030\u0003";
         else if ( e.setvalue == 2 ) // Local mode
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0032\u0003";
      }
      else if (e.name.contains("OPMODE") && e.name.contains("CTRL")) { // Set Operation mode
         if ( e.setvalue == 0 ) // Auto start - Recover auto
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0030\u0030\u0030\u0030\u0030\u0003";
         else if ( e.setvalue == 1 ) // Auto start - Recover manual
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0032\u0030\u0030\u0030\u0030\u0003";
         else if ( e.setvalue == 2 ) // Manual - Recover auto
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0032\u0030\u0030\u0030\u0030\u0003";
         else if ( e.setvalue == 3 ) // Manual - Recover manual
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0032\u0030\u0030\u0030\u0030\u0003";
      }
      else if (e.name.contains("CTRLOPMODE")) { // Set Controller Operation mode
         if ( e.setvalue == 0 ) //  Manual
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0030\u0003";
         else if ( e.setvalue == 1 ) // Automatic
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0032\u0003";
         else if ( e.setvalue == 2 ) // Remote
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0032\u0003";
         else if ( e.setvalue == 3 ) // Automatic - Remote
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0033\u0003";
      }
      else if (e.name.contains("FILAMENT")) { // Set Filament mode
         if ( e.setvalue == 0 ) //  Ti-Ball
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0030\u0003";
         else if ( e.setvalue == 1 ) // TSP 1
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0032\u0003";
         else if ( e.setvalue == 2 ) // TSP 2
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0032\u0003";
         else if ( e.setvalue == 3 ) // TSP 3
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032\u0030\u0030\u0030\u0030\u0030\u0033\u0003";
      }
      else if (e.name.contains("SUBL")) { // Set Sublimation current/period/time/wait
         String data = Integer.toString((int)e.setvalue);
         while (data.length() < 6)  // Convert to 6 bytes integer notation string
            data = "0" + data;
           serDataW = "\u0002\u0080\u0030\u0032\u0032\u0032" + data + "\u0003";
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
        logger.finer("TitaneVarianTSP:executeCommand> answer from device:" + serDataR + 
                   "(length=" + serDataR.length() + ")");
         if (e.type == DataType.TRIGGER) {
            Thread.sleep(2000); // Wait before resetting
            e.value = 0;
            holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
      }
      catch (Exception ex) {
        //ex.printStackTrace();
        //logger.log(Level.SEVERE, "IonicAgilentDual>executeCommand:" + ex.getMessage());
        setErrorComStatus();
        //logger.log(Level.WARNING, "IonicAgilentDual:executeCommand> Communication with " + name + " interrupted");
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
