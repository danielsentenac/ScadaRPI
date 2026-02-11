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

   private Serial_Comm rs232 = null;
   private static final Logger logger = Logger.getLogger("Main");
   private boolean isStarted = false;
   private int channelPos = 0;
   private String serial_port;
   private Baud baudrate;
   private DataBits databits;
   private Parity parity;
   private StopBits stopbits;
   private FlowControl flowcontrol;
   private double valueRUNNING;
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
     addDataElement( new DataElement(name, "OPERATION", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     
     // Commands triggers
     addDataElement( new DataElement(name, "OPERATIONONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "DEGASONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "RUNNINGONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // RGA writable values
     addDataElement( new DataElement(name, "FILAMENT", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "MODE", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "ELECTRONENERGY", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "EMISSION", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "MULTIPLIER", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "FOCUS", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "CAGE", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=2));

     // Com Status
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=2));

     mbRegisterEnd+=1;

     logger.fine("RgaHidenHal201RC:RgaHidenHal201RC> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

      // Instantiate communication channel and init device
      logger.fine("RgaHidenHal201RC::updateDeviceData> Initialize device communication");
      try {
         initDevice();
      }
      catch (Exception ex) {
      }
   }
   
   public void updateDeviceData() {

     DataElement d1 = getDataElement("RUNNING");
     DataElement d2 = getDataElement("OPERATION");
     DataElement d3 = getDataElement("DEGAS");
     
     // Get monitoring data from device using RS232 Comm
     DataElement dcom = getDataElement("COMST");

     try {
        popCommand();  // Execute commands it in the loop is more reactive
        /****************************************************************************************/
       
        if ( d1.value == 255 ) { // RGA interrupted; Try to reconnect
           logger.finer("RgaHidenHal201RC::updateDeviceData> 1 - Re-initialize device...");
           resetDevice();
        }
        if ( d1.value == 0 ) { // RGA is not running
           isStarted = false;
           // Test Communication
           String serDataW = "lget mode\r"; // Get RGA mode
           String serDataR = null; 
           serDataR = getAnswerFromDevice(serDataW);
           if (serDataR == null ) {  // Com Status Error
              if (hasWarned == false) {
                 logger.log(Level.SEVERE, "RgaHidenHal201RC:updateDeviceData> Communication with " + name + " interrupted");
                 // Try Re-Openning the device
                 logger.fine("RgaHidenHal201RC::updateDeviceData> Re-initialize device...");
              }
              setErrorComStatus();
              resetDevice();
           }
           else {
              d2.value = Double.parseDouble(serDataR);
           }
        }
        /****************************************************************************************/
        if ( d1.value == 1 && d2.value == 1 ) { // RGA is running in operation : collect data
           String serDataW = "data\r"; // RGA data string
           String serDataR = null; 
           if (isStarted == false) {
              // init scan
              InitScanDevice();
              getAnswerFromDevice("lini Ascans\r");
              getAnswerFromDevice("sjob lget Ascans\r");
              Thread.sleep(5000); // Mark a 5 second pause for measurement data to be collected in buffer by RGA device
              serDataW = "data all\r"; // Start getting RGA data
              serDataR = getAnswerFromDevice(serDataW);
              isStarted = true;
           }
           else {
              Thread.sleep(5000); // Mark a 1 second pause for measurement data to be collected in buffer by RGA device
              serDataR = getAnswerFromDevice(serDataW);
           }
           logger.fine("RgaHidenHal201RC::updateDeviceData> serDataR = " + serDataR);
           if (serDataR == null ) {  // Com Status Error
              if ( hasWarned == false ) {
                 logger.log(Level.WARNING, "RgaHidenHal201RC:updateDeviceData> Communication with " + name + " interrupted");
                 //Try Re-Openning the device and restart scan
                 logger.fine("RgaHidenHal201RC::updateDeviceData> Re-initialize device communication");
              }
              setErrorComStatus();
              resetDevice();
           }
           else {
              dcom.value = 0; // OK COM
              if ( hasWarned == true ) {
                 hasWarned = false;
                 logger.info("RgaHidenHal201RC::updateDeviceData> Communication with " + name + " back!");
              }
              // Parse data answer string
              DataElement d = null;
              logger.finer("RgaHidenHal201RC::updateDeviceData> PARSING DATA NOW");
              try {
                 serDataR = serDataR.replace("[{","").replace("}]","");
                 logger.finer("RgaHidenHal201RC::updateDeviceData> serDataR = " + serDataR);
                 String [] result = serDataR.split(",");
                 for (int i = 0; i < result.length; i++) {
                    if (result[i].contains(":")) {
                       logger.finer("RgaHidenHal201RC::updateDeviceData> result = " + result[i]);
                       String channelPos = result[i].split(":")[0];
                       String value = result[i].split(":")[1];
                       logger.finer("RgaHidenHal201RC::updateDeviceData> channelPos length = " + channelPos.length());
                       channelPos = channelPos.replace(".00","");
                       if (channelPos.length() == 1 ) channelPos = "00" + channelPos;
                       if (channelPos.length() == 2 ) channelPos = "0" + channelPos;
                       logger.finer("RgaHidenHal201RC::updateDeviceData> channelPos = %" + channelPos + "%");
                       d = getDataElement(channelPos);
                       if (d != null) {
                          logger.finer("AMU " + channelPos + ":" + value);
                          d.value = Double.parseDouble(value);
                       }
                    }
                 }  
              }
              catch (NumberFormatException ex) {
                 logger.log(Level.SEVERE,"RgaHidenHal201RC::updateDeviceData> Not a double:" + serDataR);
                 if (d != null) d.value = 0;
                 // Try Re-Openning the device and restart scan
                 logger.fine("RgaHidenHal201RC::updateDeviceData> Re-initialize device communication");
                 resetDevice();
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
        resetDevice();
     }
   }

   public void executeCommand (DataElement e) {
  
      String serDataW = "", serDataR = null;
      logger.fine("RgaHidenHal201RC:executeCommand> command = " + e.name);
      int choice = 0;
      if (e.name.contains("ELECTRONENERGY")) { // Set Electron Energy value
           serDataW = "lput electron-energy 70 " + Double.toString(e.setvalue) + "\r";// [4,150]
           choice = 1;
      }
      else if (e.name.contains("MULTIPLIER")) { // Set Multiplier value
           serDataW = "lput multiplier 0 " + Double.toString(e.setvalue) + "\r";// [0,2000]
           choice = 2;
      }
      else if (e.name.contains("EMISSION")) { // Set Emission current value
           serDataW = "lput emission 20 " + Double.toString(e.setvalue) + "\r";// [20,5000]
           choice = 3;
      }
      else if (e.name.contains("FOCUS")) { // Set Focus value
           serDataW = "lput focus -90 " + Double.toString(e.setvalue) + "\r"; // [-200,0]
           choice = 4;
      }
      else if (e.name.contains("CAGE")) { // Set Cage value
           serDataW = "lput cage 0 " + Double.toString(e.setvalue) + "\r"; // [-10,10]
           choice = 5;
      }
      else if (e.name.contains("FILAMENT")) { // Set Filament value
         if ( e.setvalue == 1 ) { // Use Filament 1
            try {
               UseFilament1();
            }
            catch (Exception ex) {
               ex.printStackTrace();
               logger.log(Level.SEVERE, "RgaHidenHal201RC>executeCommand:" + ex.getMessage());
               logger.log(Level.WARNING, "RgaHidenHal201RC:executeCommand> Communication with " + name + " interrupted");
            }
         }
         else if ( e.setvalue == 2 ) { // Use Filament 2
            try {
               UseFilament2();
            }
            catch (Exception ex) {
               ex.printStackTrace();
               logger.log(Level.SEVERE, "RgaHidenHal201RC>executeCommand:" + ex.getMessage());
               logger.log(Level.WARNING, "RgaHidenHal201RC:executeCommand> Communication with " + name + " interrupted");
            }
         }
         choice = 6;
      }
      else if (e.name.contains("RUNNINGONOFF")) { //  RUNNINGONOFF command           
         choice = 7;
      }
      else if (e.name.contains("OPERATIONONOFF")) { //  OPERATIONONOFF command
         if ( e.value == 1 ) // OPERATION ON
           serDataW = "lset mode 1\r";
         else if ( e.value == 2 ) // OPERATION OFF (shutdown mode)
           serDataW = "lset mode 0\r";
         choice = 8;
      }
      else if (e.name.contains("DEGASONOFF")) { //  DEGASONOFF command
         if ( e.value == 1 )  { // DEGAS ON
            try {
               StartDegasDevice();
            }
            catch (Exception ex) {
               ex.printStackTrace();
               logger.log(Level.SEVERE, "RgaHidenHal201RC>executeCommand:" + ex.getMessage());
               logger.log(Level.WARNING, "RgaHidenHal201RC:executeCommand> Communication with " + name + " interrupted");
            }
         }
         else if ( e.value == 0 ) { // DEGAS OFF
            // Stop and clean device data
            try {
               StopDegasDevice();
            }
            catch (Exception ex) {
               ex.printStackTrace();
               logger.log(Level.SEVERE, "RgaHidenHal201RC>executeCommand:" + ex.getMessage());
               logger.log(Level.WARNING, "RgaHidenHal201RC:executeCommand> Communication with " + name + " interrupted");
            }
         }
         choice = 9;
      }
      else if (e.name.contains("MODE")) { //  set MODE value
         if ( e.setvalue == 0 ) { // Set Faraday mode
            try {
               UseFaraday();
            }
            catch (Exception ex) {
               ex.printStackTrace();
               logger.log(Level.SEVERE, "RgaHidenHal201RC>executeCommand:" + ex.getMessage());
               logger.log(Level.WARNING, "RgaHidenHal201RC:executeCommand> Communication with " + name + " interrupted");
            }
         }
         else if ( e.setvalue == 1 ) { // Set SEM mode
            try {
               UseSEM();
            }
            catch (Exception ex) {
               ex.printStackTrace();
               logger.log(Level.SEVERE, "RgaHidenHal201RC>executeCommand:" + ex.getMessage());
               logger.log(Level.WARNING, "RgaHidenHal201RC:executeCommand> Communication with " + name + " interrupted");
            }
         }
         choice = 10;
      }
      try {
         // Send command to device
         if (choice != 6 && choice != 9 && choice != 7 && choice != 10)  // Exclude: Filament, Degas, Running, Mode
            getAnswerFromDevice(serDataW);
         
         // Update status from device value
         DataElement d = null;
         switch (choice) {
            case 1:
               d = getDataElement("ELECTRONENERGY");
               serDataW = "lget electron-energy\r";
               serDataR = getAnswerFromDevice(serDataW);
               if ( serDataR != null )
                 d.value = Double.parseDouble(serDataR);
               break;
            case 2:
               d = getDataElement("MULTIPLIER");
               serDataW = "lget multiplier\r";
               serDataR = getAnswerFromDevice(serDataW);
               if ( serDataR != null )
                  d.value = Double.parseDouble(serDataR);
               break;
            case 3:
               d = getDataElement("EMISSION");
               serDataW = "lget emission\r";
               serDataR = getAnswerFromDevice(serDataW);
               if ( serDataR != null )
                  d.value = Double.parseDouble(serDataR);
               break;
            case 4:
               d = getDataElement("FOCUS");
               serDataW = "lget focus\r";
               serDataR = getAnswerFromDevice(serDataW);
               if ( serDataR != null )
                  d.value = Double.parseDouble(serDataR);
               break;
            case 5:
               d = getDataElement("CAGE");
               serDataW = "lget cage\r";
               serDataR = getAnswerFromDevice(serDataW);
               if ( serDataR != null )
                  d.value = Double.parseDouble(serDataR);
               break;
            case 6:
               d = getDataElement("FILAMENT");
               serDataW = "lget F1\r";
               serDataR = getAnswerFromDevice(serDataW);
               int res = -99;
               if ( serDataR != null )
                  res = Integer.parseInt(serDataR.substring(0,1));
               if ( res == 1 )
                  d.value = 1; // F1 is in use
               else if ( res == 0 ) {
                  serDataW = "lget F2\r";
                  serDataR = getAnswerFromDevice(serDataW);
                  if ( serDataR != null )
                     res = Integer.parseInt(serDataR.substring(0,1));
                  if ( res == 1 )
                  d.value = 2;  // F2 is in use
                  else if ( res == 0 )
                     d.value = 0; // F1 and F2 not in use
               }
               break;
            case 7:
               d = getDataElement("RUNNING");
               DataElement op = getDataElement("OPERATION");
               if ( e.value == 1 && op.value == 1) // RUNNING ON
                  d.value = 1;
               else if ( e.value == 2 ) { // RUNNING OFF
                  d.value = 0;
                  // Stop and clean device data
                  StopScanDevice();
               }
               break;
            case 8:
               d = getDataElement("OPERATION");
               serDataW = "lget mode\r";
               serDataR = getAnswerFromDevice(serDataW);
               if ( serDataR != null )
                  d.value = Double.parseDouble(serDataR);
               // Update status
               GetAllParamsFromDevice();
               break;
            case 9:
               d = getDataElement("DEGAS");
               serDataW = "lget degas\r";
               serDataR = getAnswerFromDevice(serDataW);
               if ( serDataR != null )
                  d.value = Double.parseDouble(serDataR);
               break;
            case 10:
               d = getDataElement("MODE");
               serDataW = "sget input\r";
               serDataR = getAnswerFromDevice(serDataW);
               if ( serDataR != null ) {
                  if ( serDataR.contains("SEM"))
                     d.value = 1; // SEM is in use
                  else if ( serDataR.contains("Faraday"))  {
                     d.value = 0;  // Faraday is in use
                  }
               }
               break;
            }
            // Reset TRIGGER value
            if (e.type == DataType.TRIGGER) {
               Thread.sleep(2000); // Wait before resetting
               e.value = 0;
               holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
            }
      }
      catch (Exception ex) {
        ex.printStackTrace();
        logger.log(Level.SEVERE, "RgaHidenHal201RC>executeCommand:" + ex.getMessage());
        logger.log(Level.WARNING, "RgaHidenHal201RC:executeCommand> Communication with " + name + " interrupted");
     }
   }

   private String getAnswerFromDevice(String query) 
            throws UnsupportedEncodingException, InterruptedException, IOException {
      String answerStr = null;
      byte [] answer = null;
      Thread.sleep(200); // Essential to get good timing through communication channel
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
      return answerStr;
   }

   private void initDevice() throws Exception {
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
        catch(UnsatisfiedLinkError e) {
           logger.log(Level.SEVERE, e.getMessage());
        }
        catch(Exception e) {
           logger.log(Level.SEVERE, e.getMessage());
        }
        // Get info from device
        GetAllParamsFromDevice();
      
   }
   private void resetDevice() {
      try {
         if (rs232 != null)
            rs232.Close();
         rs232.Open();
         if ( rs232.isOpen()) {
             // If device was running stop it
             StopScanDevice();
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
        catch(Exception e) {
           logger.log(Level.SEVERE, e.getMessage());
        }
   }

   private void GetAllParamsFromDevice() throws Exception {
      logger.finer("GetAllParamsFromDevice");
      DataElement d = null;
      String serDataW = null;
      String serDataR = null;
      d = getDataElement("ELECTRONENERGY");
      serDataW = "lget electron-energy\r";
      serDataR = getAnswerFromDevice(serDataW);
      if (serDataR != null)
         d.value = Double.parseDouble(serDataR);
      d = getDataElement("MULTIPLIER");
      serDataW = "lget multiplier\r";
      serDataR = getAnswerFromDevice(serDataW);
      if ( serDataR != null )
         d.value = Double.parseDouble(serDataR);
      d = getDataElement("EMISSION");
      serDataW = "lget emission\r";
      serDataR = getAnswerFromDevice(serDataW);
      if ( serDataR != null )
         d.value = Double.parseDouble(serDataR);
      d = getDataElement("FOCUS");
      serDataW = "lget focus\r";
      serDataR = getAnswerFromDevice(serDataW);
      if ( serDataR != null )
         d.value = Double.parseDouble(serDataR);
      d = getDataElement("CAGE");
      serDataW = "lget cage\r";
      serDataR = getAnswerFromDevice(serDataW);
      if ( serDataR != null )
         d.value = Double.parseDouble(serDataR);
      d = getDataElement("OPERATION");
      serDataW = "lget mode\r";
      serDataR = getAnswerFromDevice(serDataW);
      if ( serDataR != null )
         d.value = Double.parseDouble(serDataR);
      d = getDataElement("DEGAS");
      serDataW = "lget degas\r";
      serDataR = getAnswerFromDevice(serDataW);
      if ( serDataR != null )
         d.value = Double.parseDouble(serDataR);
      // Get Mode selection
      d = getDataElement("MODE");
      serDataW = "sget input\r";
      serDataR = getAnswerFromDevice(serDataW);
      if ( serDataR != null ) {
         if ( serDataR.contains("SEM"))
            d.value = 1; // SEM is in use
         else if ( serDataR.contains("Faraday"))  {
               d.value = 0;  // Faraday is in use
         }
       }
      // Get Filament selection
      d = getDataElement("FILAMENT");
      serDataW = "lget F1\r";
      serDataR = getAnswerFromDevice(serDataW);
      int result = -99;
      if ( serDataR != null )
        result = Integer.parseInt(serDataR.substring(0,1));
      if ( result == 1 )
         d.value = 1; // F1 is in use
      else if ( result == 0 ) {
         serDataW = "lget F2\r";
         serDataR = getAnswerFromDevice(serDataW);
         result = Integer.parseInt(serDataR.substring(0,1));
         if ( result == 1 )
            d.value = 2;  // F2 is in use
         else if ( result == 0 )
            d.value = 0; // F1 and F2 not in use
      }
   }

   private void InitScanDevice() throws Exception {
      logger.finer("InitScanDevice");
      // Enable harware
       getAnswerFromDevice("lset enable 1\r");
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
       // Set low range limit
       getAnswerFromDevice("sset low -15\r");
       // Set high range limit
       getAnswerFromDevice("sset high -5\r");
       // Set current 
       getAnswerFromDevice("sset current -10\r");
       // Set dwell time
       getAnswerFromDevice("sset dwell 1000\r");
       // Set settle time
       getAnswerFromDevice("sset settle 300\r");
       // Set RGA mode
       getAnswerFromDevice("sset mode 1\r");
       // Set report format
       getAnswerFromDevice("sset report 5\r");
       // Set infinite cycle
       getAnswerFromDevice("pset cycles 0\r");
       // Set error format
       getAnswerFromDevice("pset terse 1\r");
       // Set mass range
       getAnswerFromDevice("pset points 70\r");
      
   }

   private void StopScanDevice() throws Exception {
      logger.finer("StopScanDevice");
      String serDataR = null;
      // Init RUNNING value
      DataElement d = getDataElement("RUNNING");
      // Abort Scan
      serDataR = getAnswerFromDevice("sset state Abort:\r");
      if ( serDataR == null )
         d.value = 255; // RGA Status turns to grey in Display
      else
          d.value = 0;
      // Stop recording data
      getAnswerFromDevice("data stop\r");
      // Switch off data
      getAnswerFromDevice("data off\r");
      // Reset all data
      for (int i = 1 ; i < 200; i++) {
         d = null;
            if ( i < 10 )
               d = getDataElement("00" + Integer.toString(i));
            else if ( i < 100 && i >= 10 )
               d = getDataElement("0" + Integer.toString(i));
            else
               d = getDataElement(Integer.toString(i));
            d.value = 0.0;
      }
      // Update status
      GetAllParamsFromDevice();
   }
   
   private void StartDegasDevice() throws Exception {
      // Stop eventual Scan before
      StopScanDevice();
      // Start Degas
      getAnswerFromDevice("lset mode 0\r");
      getAnswerFromDevice("lset multiplier 0\r");
      getAnswerFromDevice("lset F1 1\r");
      getAnswerFromDevice("lset F2 1\r");
      getAnswerFromDevice("lset degas 1\r");

      // Update status
      GetAllParamsFromDevice();
   }

   private void StopDegasDevice() throws Exception {
      // Stop Degas
      getAnswerFromDevice("lset degas 0\r");
      getAnswerFromDevice("lset F1 0\r");
      getAnswerFromDevice("lset F2 0\r");
   }

   private void UseFilament1() throws Exception {
      // Use Filament 1
      getAnswerFromDevice("lput F1 0 1\r");
      getAnswerFromDevice("lput F2 0 0\r");
   }

   private void UseFilament2() throws Exception {
      // Use Filament 2
      getAnswerFromDevice("lput F1 0 0\r");
      getAnswerFromDevice("lput F2 0 1\r");
   }

   private void UseFaraday() throws Exception {
      // Use Faraday
      getAnswerFromDevice("sset input Faraday\r");
   }

   private void UseSEM() throws Exception {
      // Use SEM
      getAnswerFromDevice("sset input SEM\r");
   }
   
};

