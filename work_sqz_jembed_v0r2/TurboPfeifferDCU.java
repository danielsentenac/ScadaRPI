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

public class TurboPfeifferDCU extends Device {

   private Serial_Comm rs485;
   private static final Logger logger = Logger.getLogger("Main");
   private int com_error_count = 0; 
   private final int MAX_COM_ERROR = 5;

   public TurboPfeifferDCU (String _name, 
                            int _mbRegisterStart,
                            String serial_port, 
                            Baud baudrate, 
                            DataBits databits, 
                            Parity parity, 
                            StopBits stopbits, 
                            FlowControl flowcontrol) {

     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("TurboPfeifferDCU:TurboPfeifferDCU> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;


     // Turbo properties
     addDataElement( new DataElement(name, "P21SPEED", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "P21FSPEED", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P21HR", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P21PWR", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P21BTPST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P21TPST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P21ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P21STYST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P21BERR", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     // Commands
     addDataElement( new DataElement(name, "P21ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P21STYONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     // Com Status
     addDataElement( new DataElement(name, "P21COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("TurboPfeifferDCU:TurboPfeifferDCU> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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
     DataElement d0 = getDataElement("P21SPEED");
     DataElement d1 = getDataElement("P21FSPEED");
     DataElement d2 = getDataElement("P21HR");
     DataElement d3 = getDataElement("P21PWR");
     DataElement d4 = getDataElement("P21BTPST");
     DataElement d5 = getDataElement("P21TPST");
     DataElement d6 = getDataElement("P21ST");
     DataElement d7 = getDataElement("P21STYST");
     DataElement d8 = getDataElement("P21BERR");
     DataElement dcom = getDataElement("P21COMST");

     try {
        for (int i = 0 ; i < 9 ; i++) {
           popCommand();  // Execute commands it in the loop is more reactive
           String serDataW = "",serDataR = "";
           switch (i) {
             case 0:
                 serDataW = "0010030902=?107\r";// Act Rot Speed
              break;
             case 1:
                 serDataW = "0010030802=?106\r";// Set Rot Speed
              break;
             case 2:
                 serDataW = "0010031102=?100\r";// Operating hours
              break;
             case 3:
                 serDataW = "0010031602=?105\r";// Power in watt
              break;
             case 4:
                 serDataW = "0010030402=?102\r";// Electronics temperature status
              break;
             case 5:
                 serDataW = "0010030502=?103\r";// Turbo pump temperature status
              break;
             case 6:
                 serDataW = "0010001002=?096\r";// Turbo ON/OFF
              break;
             case 7:
                 serDataW = "0010000202=?097\r";// Standby ON/OFF
              break;
             case 8:
                 serDataW = "0010030302=?101\r";// Error code
              break;
           }
           rs485.Write(serDataW);
           logger.finer("TurboPfeifferDCU:updateDeviceData> write to device:" + i + " --> " + serDataW);
           Thread.sleep(200); // Essential to get good timing through communication channel
           byte [] answer = null;
           while ( rs485.BytesAvailable() > 0 )
               answer = rs485.Read();
           if ( answer != null )
              serDataR = new String(answer, "UTF-8");
           //if (serDataR.length() > serDataW.length())
           //  serDataR = serDataR.substring(serDataW.length());  // remove echo
           logger.finer("TurboPfeifferDCU:updateDeviceData> reading from device:" + serDataR + 
                      "(length=" + serDataR.length() + ")"); 
           if (serDataR.equals("") || serDataR.length() != 20) {// Com Status Error
              com_error_count+=1;
              if ( com_error_count > MAX_COM_ERROR ) {
                 if ( hasWarned == false ) {
                    logger.log(Level.WARNING, "TurboPfeifferDCU:updateDeviceData> Communication with " + name + " interrupted");
                 }
                 setErrorComStatus();
              }
              continue;
           }
           else {
              com_error_count = 0;
              dcom.value = 0; // OK COM
              if ( hasWarned == true ) {
                 hasWarned = false;
                 logger.info("TurboPfeifferDCU:updateDeviceData> Communication with " + name + " back!");
              }
              if (serDataR.length() == 20) { // Data OK
                 int csum = 0; // Calculate checksum
                 for (int k = 0 ; k < serDataR.length() - 4; k++) {
                    csum+= (int)serDataR.charAt(k);
                 }
                 //logger.finer("TurboPfeifferDCU:updateDeviceData> csum = " + csum);
                 int csumd = Integer.parseInt(serDataR.substring(serDataR.length() - 4, serDataR.length() - 1));
                 //logger.finer("TurboPfeifferDCU:updateDeviceData> csumd = " + csumd);
                 if ( (csum%256) == csumd) { // Data OK
                    // check Channel read
                    //logger.finer("TurboPfeifferDCU:updateDeviceData> Channel OK ? (" + serDataR.substring(5,8) + ")");
                    //logger.finer("TurboPfeifferDCU:updateDeviceData> Channel OK ? (" + serDataW.substring(5,8) + ")");
                    if ( serDataR.substring(5, 8).equals(serDataW.substring(5, 8))) { // Channel OK
                       //logger.finer("TurboPfeifferDCU:updateDeviceData> Channel OK (" + serDataR.substring(5,8) + ")");
                       if ( i < 4 ) { // short value
                          int result = 0;
                          result = Integer.parseInt(serDataR.substring(10,16));
                          if (i == 0) d0.value = (double) result;
                          if (i == 1) d1.value = (double) result;
                          if (i == 2) d2.value = (double) result;
                          if (i == 3) d3.value = (double) result;
                       }
                       else if ( i >= 4  && i <= 7 ) { // Boolean value
                          int result = 0;
                          if ( serDataR.substring(10,16).equals("111111"))
                             result = 1;
                          else if ( serDataR.substring(10,16).equals("000000"))
                             result = 0;
                          //logger.finer("TurboPfeifferDCU:updateDeviceData> d[" + i + "]=" 
                          //            + result + " (" + serDataR.substring(10,16) + ")");
                          if (i == 4) d4.value = (double) result;
                          if (i == 5) d5.value = (double) result;
                          if (i == 6) d6.value = (double) result;
                          if (i == 7) d7.value = (double) result;
                       }
                       else if (i == 8) { // String value
                          if ( (serDataR.substring(10,16).equals("no Err")) || 
                             (serDataR.substring(10,13).equals("Wrn")) || 
                             (serDataR.substring(10,16).equals("000000"))) {
                             d8.value = 0;
                          }
                          else if ( serDataR.substring(10,13).equals("Err")) {
                             int result = 0;
                             result = Integer.parseInt(serDataR.substring(13,16));
                             d8.value = result;
                          }
                       }
                    }
                 }
              }
           }
        }
     }
     catch (Exception ex) {
        if (hasWarned == false) {
           logger.log(Level.SEVERE, "TurboPfeifferDCU>updateDeviceData:" + ex.getMessage());
           logger.log(Level.WARNING, "TurboPfeifferDCU:updateDeviceData> Communication with " + name + " interrupted");
        }
        setErrorComStatus();
     }
   }

   public void executeCommand (DataElement e) {
  
      String serDataW = "";
      if (e.name.contains("STYONOFF")) { // Standby On/Off command
         if ( e.value == 1 ) // Stanby On
           serDataW = "0011000206111111016\r";
         else if ( e.value == 2 ) // Standby Off
           serDataW = "0011000206000000010\r";
      }
      else { // Start/Stop command
         if ( e.value == 1 ) // Start
           serDataW = "0011001006111111015\r";
         else if ( e.value == 2 ) // Stop
           serDataW = "0011001006000000009\r";
      }
      try {
         rs485.Write(serDataW);
         Thread.sleep(2000); // Wait before resetting
         e.value = 0;
         holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
      }
      catch (Exception ex) {
        ex.printStackTrace();
        logger.log(Level.SEVERE, "TurboPfeifferDCU>executeCommand:" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "TurboPfeifferDCU:executeCommand> Communication with " + name + " interrupted");
     }
   }
}; 
