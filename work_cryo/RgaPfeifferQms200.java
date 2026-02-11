/*
 * This Class is the implementation of the Turbo Pfeiffer DCU device
 *
 */
import java.util.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class RgaPfeifferQms200 extends Device {

   private Serial_Comm rs232;
   private static final Logger logger = Logger.getLogger("Main");
   private boolean isStarted = false;
   private int channelPos = 0;
   private String serial_port;
   private Baud baudrate;
   private DataBits databits;
   private Parity parity;
   private StopBits stopbits;
   private FlowControl flowcontrol;

   public RgaPfeifferQms200 (String _name, 
                            int _mbRegisterStart,
                            String _serial_port, 
                            Baud _baudrate, 
                            DataBits _databits, 
                            Parity _parity, 
                            StopBits _stopbits, 
                            FlowControl _flowcontrol) {

     name = _name; // Device name

     serial_port = _serial_port;
     baudrate = _baudrate;
     databits = _databits;
     parity = _parity;
     stopbits = _stopbits;
     flowcontrol = _flowcontrol;

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("RGAPfeifferQms200:RGAPfeifferQms200> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;


     // RGA data & status
     addDataElement( new DataElement(name, "RGAGA5.001", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     for ( int i = 2 ; i <= 200 ; i++ ) {
         if ( i < 10 )
           addDataElement( new DataElement(name, "RGAGA5.00" + Integer.toString(i),
                                           DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
         else if ( i < 100 && i >= 10 )
           addDataElement( new DataElement(name, "RGAGA5.0" + Integer.toString(i),
                                           DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
         else 
           addDataElement( new DataElement(name, "RGAGA5." + Integer.toString(i),
                                           DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     }
     // RGA properties (read only)
     addDataElement( new DataElement(name, "RGAGA5RUNNING", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "RGAGA5DEGAS", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "RGAGA5EMISSION", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     
     // Commands triggers
     addDataElement( new DataElement(name, "RGAGA5EMISSIONONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "RGAGA5DEGASONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "RGAGA5SEMONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "RGAGA5RUNNINGONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // RGA writable values
     addDataElement( new DataElement(name, "RGAGA5FILAMENT", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "RGAGA5MODE", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Com Status
     addDataElement( new DataElement(name, "RGAGA5COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.fine("RGAPfeifferQms200:RGAPfeifferQms200> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

      // Instantiate communication channel
     try {
       rs232 = new Serial_Comm(serial_port, baudrate, databits, parity, stopbits, flowcontrol);
       rs232.Open();
       // Initialize communication in ASCII format
       getAnswerFromDevice("CMO,1\r");
       // Initialize RGA Measurement operation
       getAnswerFromDevice("CFU,0\r");
       // Initialize RGA scan monochannel cycle
       getAnswerFromDevice("CYM,0\r");
       // Initialize RGA with infinite Cycle
       getAnswerFromDevice("CYS,0\r");
       // Initialize channel
       getAnswerFromDevice("CBE,6\r");
       // Select channel
       getAnswerFromDevice("SPC,6\r");
       // Initialize RGA in Faraday Mode
       getAnswerFromDevice("SDT,0\r");
       // Initialize RGA in Faraday Source
       getAnswerFromDevice("DTY,0\r");
       // Initialize SEM High-voltage 
       getAnswerFromDevice("SHV,1400\r");
       // Initialize SEM High-voltage from channel 
       getAnswerFromDevice("DSE,1400\r");
       // Initialize RGA SEM mode OFF
       getAnswerFromDevice("SEM,0\r");
       // Initialize RGA range [1,200]
       getAnswerFromDevice("SMR,1\r");
       // Initialize Range AMU
       getAnswerFromDevice("MWI,200\r");
       // Initialize RGA measuremnt speed 1 second
       getAnswerFromDevice("MSD,10\r");
       // Initialize RGA scan mode SCAN
       getAnswerFromDevice("MMO,0\r");
       // Initialize RGA first mass scan 
       getAnswerFromDevice("MFM,1\r");
       // Initialize Electrometer auto range
       getAnswerFromDevice("AMO,2\r");
       
       // Initialize RGA Filament selection
       getAnswerFromDevice("IFI,0\r");
       // Initialize RGA Emission
       getAnswerFromDevice("FIE,0\r");
       // Stop any running SCAN
       getAnswerFromDevice("CRU,0\r");
       // Measured data 
       getAnswerFromDevice("MBH\r");
       // Get Status
       getAnswerFromDevice("ESQ\r");
       // Get B-Header
       getAnswerFromDevice("MBH\r");
     }
     catch (InterruptedException e) {
       logger.log(Level.SEVERE, e.getMessage());
     }
     catch (IOException e) {
       logger.log(Level.SEVERE, e.getMessage());
     }
     catch(UnsatisfiedLinkError e) {
       logger.log(Level.SEVERE, e.getMessage());
     }

   }
   
   public void updateDeviceData() {
   
     DataElement d1 = getDataElement("RGAGA5RUNNING");
     DataElement d2 = getDataElement("RGAGA5EMISSION");
     DataElement d3 = getDataElement("RGAGA5MODE");
     DataElement d4 = getDataElement("RGAGA5DEGAS");
    
     // Get monitoring data from device using RS232 Comm
     DataElement dcom = getDataElement("RGAGA5COMST");

     try {
        //logger.finer(" --> " + name + ":next Modbus commands...");
        //addModbusCommand(); // Push Modbus commands in the loop is more reactive
        //logger.finer(" --> " + name + ":next command...");
        popCommand();  // Execute commands it in the loop is more reactive
        /****************************************************************************************/
      
        if ( d1.value == 0 ) { // RGA is stopped
           isStarted = false;
           // Test Communication
           String serDataW = "ERR\r"; // Get RGA status
           String serDataR = null; 
           serDataR = getAnswerFromDevice(serDataW);
           serDataW = "ESQ\r"; // Get RGA status
           serDataR = null; 
           serDataR = getAnswerFromDevice(serDataW);
           if (serDataR == null ) {  // Com Status Error
              if (hasWarned == false) 
                 logger.log(Level.SEVERE, "1 - RGAPfeifferQms200:updateDeviceData> Communication with " + name + " interrupted");
              setErrorComStatus();
              // Try Re-Openning the device
              try {
                 rs232.Close();
                 rs232.Open();
              }
              catch (InterruptedException e) {
                 logger.log(Level.SEVERE, e.getMessage());
              }
              catch (IOException e) {
                 logger.log(Level.SEVERE, e.getMessage());
              }
              catch(UnsatisfiedLinkError e) {
                 logger.log(Level.SEVERE, e.getMessage());
              }
           }
        }
        /****************************************************************************************/
        if ( d1.value == 1 ) { // RGA is started : collect data
           if (isStarted == false) { 
              isStarted = true;
              channelPos = 1;
              Thread.sleep(5000); // Mark an initial 5 second pause for measurement data to be collected in buffer by RGA device
           }
           else {
              channelPos++;
              if (channelPos > 200) channelPos = 1;
           }
           Thread.sleep(2000); // Mark a 2 second pause for measurement data to be collected in buffer by RGA device
           String serDataW = "MDB\r"; // Get RGA data
           String serDataR = null; 
           serDataR = getAnswerFromDevice(serDataW);
           if (serDataR == null ) {  // Com Status Error
              if ( hasWarned == false ) {
                 logger.log(Level.WARNING, "RGAPfeifferQms200:updateDeviceData> Communication with " + name + " interrupted");
              }
              setErrorComStatus();
              // Try Re-Openning the device
              try {
                 rs232.Close();
                 rs232.Open();
              }
              catch (InterruptedException e) {
                 logger.log(Level.SEVERE, e.getMessage());
              }
              catch (IOException e) {
                 logger.log(Level.SEVERE, e.getMessage());
              }
              catch(UnsatisfiedLinkError e) {
                 logger.log(Level.SEVERE, e.getMessage());
              }
           }
           else {
              dcom.value = 0; // OK COM
              if ( hasWarned == true ) {
                 hasWarned = false;
                 logger.info("Controllino_1:updateDeviceData> Communication with " + name + " back!");
              }
              DataElement d = null;
              if ( channelPos < 10 )
                 d = getDataElement("RGAGA5.00" + Integer.toString(channelPos));
              else if ( channelPos < 100 && channelPos >= 10 )
                 d = getDataElement("RGAGA5.0" + Integer.toString(channelPos));
              else
                 d = getDataElement("RGAGA5." + Integer.toString(channelPos));
              if (d != null) {
                 try {
                    logger.fine("AMU " + channelPos + ":" + serDataR);
                    d.value = Double.parseDouble(serDataR);
                 }
                 catch (NumberFormatException ex) {
                    logger.log(Level.SEVERE,"RGAPfeifferQms200::updateDeviceData> Not a double:" + serDataR);
                    d.value = 0;
                 }
              }
           }
        }
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "RGAPfeifferQms200:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "RGAPfeifferQms200:updateDeviceData>" + ex.getMessage());
        }
        setErrorComStatus();
        // Try Re-Openning the device
        try {
           rs232.Close();
           rs232.Open();
        }
        catch (InterruptedException e) {
           logger.log(Level.SEVERE, e.getMessage());
        }
        catch (IOException e) {
           logger.log(Level.SEVERE, e.getMessage());
        }
        catch(UnsatisfiedLinkError e) {
           logger.log(Level.SEVERE, e.getMessage());
        }
     }
   }

   public void executeCommand (DataElement e) {
  
      String serDataW = "", serDataR = null;
      logger.fine("e.name=" + e.name);
      if (e.name.contains("FILAMENT")) { // Set FILAMENT value
         if ( e.setvalue == 0 ) // SelectFilament 0
           serDataW = "IFI,0\r";
         else if ( e.setvalue == 1 ) // SelectFilament 1
           serDataW = "IFI,1\r";
         else if ( e.setvalue == 2 ) // SelectFilament 1&2
           serDataW = "IFI,2\r";
      }
      if (e.name.contains("MODE")) { // Set Mode
         if ( e.setvalue == 0 ) // Faraday Mode
           serDataW = "SDT,0\r";
         else if ( e.setvalue == 1 ) // SEM Mode
           serDataW = "SDT,1\r";
      }
      else if (e.name.contains("EMISSIONONOFF")) { //  EMISSIONONOFF command
         if ( e.value == 1 ) // Emission enabled
           serDataW = "FIE,1\r";
         else if ( e.value == 2 ) // Emission disabled
           serDataW = "FIE,0\r";
      }
      else if (e.name.contains("DEGASONOFF")) { //  DEGASONOFF command
         if ( e.value == 1 ) // Degas enabled
           serDataW = "ISC,1\r";
         else if ( e.value == 2 ) // Degas disabled
           serDataW = "ISC,0\r";
      }
      else if (e.name.contains("SEMONOFF")) { //  SEMONOFF command
         if ( e.value == 1 ) // SEM enabled
           serDataW = "SEM,1\r";
         else if ( e.value == 2 ) // SEM disabled
           serDataW = "SEM,0\r";
      }
      else if (e.name.contains("RUNNINGONOFF")) { // RUNNINGONOFF  command
         if ( e.value == 1 ) // Start Scan
           serDataW = "CRU,1\r";
         else if ( e.value == 2 ) // Stop Scan
           serDataW = "CRU,0\r";
      }
      
      try {
         serDataR = getAnswerFromDevice(serDataW);
         if (serDataR == null ) {  // Com Status Error
            logger.log(Level.SEVERE, "RGAPfeifferQms200:executeCommand> Communication with " + name + " interrupted ?");
         }
         else {
            DataElement d1 = getDataElement("RGAGA5RUNNING");
            DataElement d2 = getDataElement("RGAGA5EMISSION");
            DataElement d3 = getDataElement("RGAGA5MODE");
            DataElement d4 = getDataElement("RGAGA5DEGAS");

            if ( e.name.contains("FILAMENT") ) // Save acknowledged value answer from device 
               e.value = Double.parseDouble(serDataR);
            else if ( e.name.contains("MODE") ) // Save acknowledged value answer from device 
               e.value = Double.parseDouble(serDataR);
            else if (e.name.contains("RUNNING")) {
               d1.value = Double.parseDouble(serDataR);
               if (d1.value == 0) { // SCAN is OFF Reset all values
                  for (int i = 1 ; i <= 200; i++) {
                     DataElement d = null;
                     if ( i < 10 )
                        d = getDataElement("RGAGA5.00" + Integer.toString(i));
                     else if ( i < 100 && i >= 10 )
                        d = getDataElement("RGAGA5.0" + Integer.toString(i));
                     else
                        d = getDataElement("RGAGA5." + Integer.toString(i));
                     d.value = 0.0;
                  }
               }  
            }
            else if (e.name.contains("EMISSION"))
               d2.value = Double.parseDouble(serDataR);
            else if (e.name.contains("DEGAS"))
               d4.value = Double.parseDouble(serDataR);

            // Reset TRIGGER value
            if (e.type == DataType.TRIGGER) {
               Thread.sleep(2000); // Wait before resetting
               e.value = 0;
               holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
            }
         }
      }
      catch (Exception ex) {
        ex.printStackTrace();
        logger.log(Level.SEVERE, "RGAPfeifferQms200>executeCommand:" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "RGAPfeifferQms200:executeCommand> Communication with " + name + " interrupted");
     }
   }

   private String getAnswerFromDevice(String query) 
            throws UnsupportedEncodingException, InterruptedException, IOException {
      String answerStr = null;
      byte [] answer = null;
      logger.fine("RGAPfeifferQms200:getAnswerFromDevice> write to device:" + query + 
                     "(length=" + query.length() + ")");
      rs232.Write(query);
      Thread.sleep(200); // Essential to get good timing through communication channel
      while ( rs232.BytesAvailable() > 0 )
         answer = rs232.Read();
      if ( answer != null ) {
         answerStr = new String(answer, "UTF-8");
         logger.fine("RGAPfeifferQms200:getAnswerFromDevice> answer from device:" + answerStr + 
                     "(length=" + answerStr.length() + ")");
      }
      //else
      //   logger.log(Level.SEVERE, "RGAPfeifferQms200:updateDeviceData> Null answer from device!");
      rs232.Write("\u0005");
      Thread.sleep(200); // Essential to get good timing through communication channel
      while ( rs232.BytesAvailable() > 0 )
         answer = rs232.Read();
      if ( answer != null ) {
         answerStr = new String(answer, "UTF-8");
         logger.fine("RGAPfeifferQms200:>getAnswerFromDevice> answer from device:" + answerStr + 
                     "(length=" + answerStr.length() + ")");
      }
      //else
      //   logger.log(Level.SEVERE, "RGAPfeifferQms200:updateDeviceData> Null answer from device!");

      return answerStr;
   }

   public int bitRead(int target, int bit) {
      return (target >> bit) & 1;
   }
};

