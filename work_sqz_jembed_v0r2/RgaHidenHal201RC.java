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

public class RgaHidenHal201RC extends Device {

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

   public RgaHidenHal201RC (String _name, 
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

     logger.finer("RgaHidenHal201RC:RgaHidenHal201RC> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;


     // RGA data & status
     addDataElement( new DataElement(name, "001", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     for ( int i = 2 ; i < 200 ; i++ ) {
         if ( i < 10 )
           addDataElement( new DataElement(name, "00" + Integer.toString(i),
                                           DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
         else if ( i < 100 && i >= 10 )
           addDataElement( new DataElement(name, "0" + Integer.toString(i),
                                           DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
         else 
           addDataElement( new DataElement(name, Integer.toString(i),
                                           DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     }
     // RGA properties (read only)
     addDataElement( new DataElement(name, "RUNNING", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "DEGAS", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "EMISSION", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     
     // Commands triggers
     addDataElement( new DataElement(name, "EMISSIONONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "DEGASONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "SEMONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "RUNNINGONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // RGA writable values
     addDataElement( new DataElement(name, "FILAMENT", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "MODE", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Com Status
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.fine("RgaHidenHal201RC:RgaHidenHal201RC> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

      // Instantiate communication channel
     try {
       rs232 = new Serial_Comm(serial_port, baudrate, databits, parity, stopbits, flowcontrol);
       rs232.Open();
       // Remove all data from all tables
       getAnswerFromDevice("sdel all\r");
       // Set new scan table
       getAnswerFromDevice("sset scan Ascans\r");
       // Set new scan row
       getAnswerFromDevice("sset row 1\r");
       // Set mass scan
       getAnswerFromDevice("sset output mass\r");
       // Set start mass
       getAnswerFromDevice("sset start 1.00\r");
       // Set stop mass
       getAnswerFromDevice("sset stop 200.00\r");
       // Set step mass
       getAnswerFromDevice("sset step 1.00\r");
       // Set source detector
       getAnswerFromDevice("sset input Faraday\r");
       // Set low range limit
       getAnswerFromDevice("sset low -10\r");
       // Set high range limit
       getAnswerFromDevice("sset high -5\r");
       // Set current 
       getAnswerFromDevice("sset current -5\r");
       // Set dwell time
       getAnswerFromDevice("sset dwell 1000\r");
       // Set settle time
       getAnswerFromDevice("sset settle 100%\r");
       // Set RGA mode
       getAnswerFromDevice("sset mode 1\r");
       // Set report format
       getAnswerFromDevice("sset report 21\r");
       // Set infinite cycle
       getAnswerFromDevice("pset cycles 0\r");
       // Set error format
       getAnswerFromDevice("pset terse 1\r");
       // Set mass range
       getAnswerFromDevice("pset points 200\r");
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
   
     DataElement d1 = getDataElement("RUNNING");
     DataElement d2 = getDataElement("EMISSION");
     DataElement d3 = getDataElement("MODE");
     DataElement d4 = getDataElement("DEGAS");
     DataElement d5 = getDataElement("FILAMENT");
    
     // Get monitoring data from device using RS232 Comm
     DataElement dcom = getDataElement("COMST");

     try {
        popCommand();  // Execute commands it in the loop is more reactive
        /****************************************************************************************/
      
        if ( d1.value == 255 ) { // RGA interrupted; Try to reconnect
           try {
              logger.fine("RgaHidenHal201RC::updateDeviceData> 1 - Re-initialize device...");
              rs232.Close();
              rs232.Open();
              // Reinitialize values
              if (serDataR != null) 
                 d1.value = d2.value = d3.value = d4.value = d5.value = 0;
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
        if ( d1.value == 0 ) { // RGA is stopped
           isStarted = false;
           // Test Communication
           String serDataW = "help\r"; // Get RGA help
           String serDataR = null; 
           serDataR = getAnswerFromDevice(serDataW);
           if (serDataR == null ) {  // Com Status Error
              if (hasWarned == false) {
                 logger.log(Level.SEVERE, "RgaHidenHal201RC:updateDeviceData> Communication with " + name + " interrupted");
                 // Try Re-Openning the device
                 logger.fine("RgaHidenHal201RC::updateDeviceData> Re-initialize device...");
              }
              setErrorComStatus();
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
           }
           Thread.sleep(1000); // Mark a 1 second pause for measurement data to be collected in buffer by RGA device
           String serDataW = "data\r"; // Get RGA data
           String serDataR = null; 
           serDataR = getAnswerFromDevice(serDataW);
           if (serDataR == null ) {  // Com Status Error
              if ( hasWarned == false ) {
                 logger.log(Level.WARNING, "RgaHidenHal201RC:updateDeviceData> Communication with " + name + " interrupted");
                 // Try Re-Openning the device and restart scan
                 logger.fine("RgaHidenHal201RC::updateDeviceData> Re-initialize device and restart scan from beginning...");
              }
              setErrorComStatus();
              try {
                 rs232.Close();
                 rs232.Open();
                 isStarted = false;
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
                 logger.info("RgaHidenHal201RC::updateDeviceData> Communication with " + name + " back!");
              }
              DataElement d = null;
              if ( channelPos < 10 )
                 d = getDataElement("00" + Integer.toString(channelPos));
              else if ( channelPos < 100 && channelPos >= 10 )
                 d = getDataElement("0" + Integer.toString(channelPos));
              else
                 d = getDataElement(Integer.toString(channelPos));
              if (d != null) {
                 try {
                    logger.finer("AMU " + channelPos + ":" + serDataR);
                    d.value = Double.parseDouble(serDataR);
                 }
                 catch (NumberFormatException ex) {
                    logger.log(Level.SEVERE,"RgaHidenHal201RC::updateDeviceData> Not a double:" + serDataR);
                    d.value = 0;
                    // Try Re-Openning the device and restart scan
                    logger.fine("RgaHidenHal201RC::updateDeviceData> Re-initialize device and restart scan from beginning...");
                    try {
                       rs232.Close();
                       rs232.Open();
                       // re-init scan
                       // Initialize RGA Emission
                       getAnswerFromDevice("FIE,1\r");
                       // Stop/Start SCAN
                       getAnswerFromDevice("CRU,0\r");
                       getAnswerFromDevice("CRU,1\r");
                       isStarted = false;
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
           }
        }
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "RgaHidenHal201RC:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "RgaHidenHal201RC:updateDeviceData>" + ex.getMessage());
           // Try Re-Openning the device
           logger.fine("RgaHidenHal201RC::updateDeviceData> Re-initialize device...");   
        }
        setErrorComStatus();
        
        try {
           rs232.Close();
           rs232.Open();
           if ( d1.value == 1) {// Scan was running - restart it
              // re-init scan
              // Initialize RGA Emission
              getAnswerFromDevice("FIE,1\r");
              // Stop/Start SCAN
              getAnswerFromDevice("CRU,0\r");
              getAnswerFromDevice("CRU,1\r");
              isStarted = false;
           }
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
      logger.fine("RgaHidenHal201RC:executeCommand> command = " + e.name);
      if (e.name.contains("MULTIPLIER")) { // Set Multiplier value
           serDataW = "sset multiplier " + Integer.toString(e.setvalue) + "\r";
      }
      if (e.name.contains("MODE")) { // Set Mode
         if ( e.setvalue == 0 ) // Faraday Mode
           serDataW = "sset input Faraday\r";
         else if ( e.setvalue == 1 ) // SEM Mode
           serDataW = "sset input SEM\r";
      }
      else if (e.name.contains("FIL1ONOFF")) { //  FIL1ONOFF command
         if ( e.value == 1 ) // FIL1 ON
           serDataW = "lset F1 1\r";
         else if ( e.value == 2 ) // FIL1 OFF
           serDataW = "lset F1 0\r";
      }
       else if (e.name.contains("FIL2ONOFF")) { //  FIL2ONOFF command
         if ( e.value == 1 ) // FIL2 ON
           serDataW = "lset F2 1\r";
         else if ( e.value == 2 ) // FIL2 OFF
           serDataW = "lset F2 0\r";
      }
      else if (e.name.contains("DEGASONOFF")) { //  DEGASONOFF command
         if ( e.value == 1 ) // Degas enabled
           serDataW = "ISC,1\r";
         else if ( e.value == 2 ) // Degas disabled
           serDataW = "ISC,0\r";
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
            logger.log(Level.SEVERE, "RgaHidenHal201RC:executeCommand> Communication with " + name + " interrupted ?");
         }
         else {
            DataElement d1 = getDataElement("RUNNING");
            DataElement d2 = getDataElement("EMISSION");
            DataElement d3 = getDataElement("MODE");
            DataElement d4 = getDataElement("DEGAS");

            if ( e.name.contains("FILAMENT") ) // Save acknowledged value answer from device 
               e.value = Double.parseDouble(serDataR);
            else if ( e.name.contains("MODE") ) // Save acknowledged value answer from device 
               e.value = Double.parseDouble(serDataR);
            else if (e.name.contains("RUNNING")) {
               d1.value = Double.parseDouble(serDataR);
               if (d1.value == 0) { // SCAN is OFF Reset all values
                  for (int i = 1 ; i < 200; i++) {
                     DataElement d = null;
                     if ( i < 10 )
                        d = getDataElement("00" + Integer.toString(i));
                     else if ( i < 100 && i >= 10 )
                        d = getDataElement("0" + Integer.toString(i));
                     else
                        d = getDataElement(Integer.toString(i));
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
        logger.log(Level.SEVERE, "RgaHidenHal201RC>executeCommand:" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "RgaHidenHal201RC:executeCommand> Communication with " + name + " interrupted");
     }
   }

   private String getAnswerFromDevice(String query) 
            throws UnsupportedEncodingException, InterruptedException, IOException {
      String answerStr = null;
      byte [] answer = null;
      logger.finer("RgaHidenHal201RC:getAnswerFromDevice> write to device:" + query + 
                     "(length=" + query.length() + ")");
      rs232.Write(query);
      Thread.sleep(200); // Essential to get good timing through communication channel
      while ( rs232.BytesAvailable() > 0 )
         answer = rs232.Read();
      if ( answer != null ) {
         answerStr = new String(answer, "UTF-8");
         logger.finer("RgaHidenHal201RC:getAnswerFromDevice> answer from device:" + answerStr + 
                     "(length=" + answerStr.length() + ")");
      }
      //else
      //  logger.log(Level.SEVERE, "RgaHidenHal201RC:updateDeviceData> Null answer from device!");
      rs232.Write("\u0005");
      Thread.sleep(200); // Essential to get good timing through communication channel
      while ( rs232.BytesAvailable() > 0 )
         answer = rs232.Read();
      if ( answer != null ) {
         answerStr = new String(answer, "UTF-8");
         logger.finer("RgaHidenHal201RC:>getAnswerFromDevice> answer from device:" + answerStr + 
                     "(length=" + answerStr.length() + ")");
      }
      //else
      //   logger.log(Level.SEVERE, "RgaHidenHal201RC:updateDeviceData> Null answer from device!");

      return answerStr;
   }

   public int bitRead(int target, int bit) {
      return (target >> bit) & 1;
   }
};

