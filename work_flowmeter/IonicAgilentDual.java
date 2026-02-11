/*
 * This Class is the implementation of the Ionic Agilent Dual device
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class IonicAgilentDual extends Device {

   private Serial_Comm rs232;
   private static final Logger logger = Logger.getLogger("Main");
   private int com_error_count = 0; 
   private final int MAX_COM_ERROR = 5;

   public IonicAgilentDual (String _name, 
                            int _mbRegisterStart,
                            String serial_port, 
                            Baud baudrate, 
                            DataBits databits, 
                            Parity parity, 
                            StopBits stopbits, 
                            FlowControl flowcontrol) {

     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("IonicAgilentDual:IonicAgilentDual> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;


     // Ionic Pump properties (type 1: read only values)
     addDataElement( new DataElement(name, "P33ST",DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "P33ABSCUR",DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33ABSVOLT",DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "P33P",DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));

     // Ionic Pump Commands (type 2: command triggers)
     addDataElement( new DataElement(name, "P33ONOFF",DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     //Ionic Pump writable values (type 3: read/writable status or value)
     addDataElement( new DataElement(name, "P33REMOTEMODE",DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33OPMODE",DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33VOLTMODE",DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33PRTCUR",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33MAXCUR",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33MAXVOLT",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33MAXW",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33STEP1VOLT",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33STEP2VOLT",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33STEP1CUR",DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33STEP2CUR",DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));

     // Com Status
     addDataElement( new DataElement(name, "P33COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=2));

     mbRegisterEnd+=1;

     logger.finer("IonicAgilentDual:IonicAgilentDual> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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
   
     // Get monitoring data from device using RS232 Comm
     Vector <DataElement> d = new Vector<DataElement> ();
     d.add(getDataElement("P33ST"));
     d.add(getDataElement("P33OPMODE"));
     d.add(getDataElement("P33VOLTMODE"));
     d.add(getDataElement("P33REMOTEMODE"));
     d.add(getDataElement("P33ABSCUR"));
     d.add(getDataElement("P33ABSVOLT"));
     d.add(getDataElement("P33PRTCUR"));
     d.add(getDataElement("P33P"));
     d.add(getDataElement("P33MAXCUR"));
     d.add(getDataElement("P33MAXVOLT"));
     d.add(getDataElement("P33MAXW"));
     d.add(getDataElement("P33STEP1VOLT"));
     d.add(getDataElement("P33STEP2VOLT"));
     d.add(getDataElement("P33STEP1CUR"));
     d.add(getDataElement("P33STEP2CUR"));
     d.add(getDataElement("P33COMST"));
    
     try {
        for (int i = 0 ; i < 15 ; i++) {
           //logger.finer(" --> ", name + ":next Modbus commands...");
           //addModbusCommand(); // Push Modbus commands in the loop is more reactive
           //logger.finer(" --> ", name + ":next command...");
           popCommand();  // Execute commands in the loop is more reactive
           String serDataW = "",serDataR = "";
           switch (i) {
              case 0:
                    serDataW = "\u0081\u0030\u0034\u0041\u0030\u0031\u003F"; // HV1 status
                    //serDataW = "#130?\r"; // On-Off Status
                 break;
              case 1:
                    serDataW = "\u0081\u0030\u0034\u0043\u0030\u0031\u003F"; // HV1 Operating mode (Started/Protect)
                    //serDataW = "#161?\r"; // Start-Protect
                 break;
              case 2:
                    serDataW = "\u0081\u0030\u0034\u0042\u0030\u0031\u003F"; // HV1 Voltage mode (Fixed/Step)
                    //serDataW = "#160?\r"; // Fixed-Step
                 break;
              case 3:
                    serDataW = "\u0081\u0030\u0034\u005A\u0030\u0030\u003F"; // local/serial/remote mode
                    //serDataW = "#010?\r"; // Local-Remote Status
                 break;
              case 4:
                    serDataW = "\u0081\u0030\u0034\u0054\u0030\u0031\u003F"; // HV1 absorbed current
                    //serDataW = "#108?\r"; // I MEAS
                 break;
              case 5:
                    serDataW = "\u0081\u0030\u0034\u0053\u0030\u0031\u003F"; // HV1 absorbed voltage
                    //serDataW = "#107?\r"; // V MEAS
                 break;
              case 6:
                    serDataW = "\u0081\u0030\u0034\u004B\u0030\u0031\u003F"; // HV1 I Protect
                    //serDataW = "#166?\r"; // IPROTECT
                 break;
              case 7:
                    serDataW = "\u0081\u0030\u0034\u0055\u0030\u0031\u003F"; // HV1 pressure measured
                    //serDataW = "#102?\r"; // PRESSURE
                 break;
              case 8:
                    serDataW = "\u0081\u0030\u0034\u0049\u0030\u0031\u003F"; // HV1 IMax
                    //serDataW = "#164?\r"; // IMAX
                 break;
              case 9:
                    serDataW = "\u0081\u0030\u0034\u0048\u0030\u0031\u003F"; // HV1 VMax
                    //serDataW = "#163?\r"; // VMAX
                 break;
              case 10:
                    serDataW = "\u0081\u0030\u0034\u004A\u0030\u0031\u003F"; // HV1 PMax
                    //serDataW = "#165?\r"; // PMAX
                 break;
              case 11:
                    serDataW = "\u0081\u0030\u0034\u004C\u0030\u0031\u003F"; // HV1 VStep1
                    //serDataW = "#167?\r"; // VSTEP1 
                 break;
              case 12:
                    serDataW = "\u0081\u0030\u0034\u004E\u0030\u0031\u003F"; // HV1 VStep2
                    //serDataW = "#169?\r"; // VSTEP2
                 break;
              case 13:
                    serDataW = "\u0081\u0030\u0034\u004D\u0030\u0031\u003F"; // HV1 IStep1
                    //serDataW = "#168?\r"; // ISTEP1
                 break;
              case 14:
                    serDataW = "\u0081\u0030\u0034\u004F\u0030\u0031\u003F"; // HV1 IStep2
                    //serDataW = "#170?\r"; // ISTEP2
                 break;
           } 
           serDataW+=xor_checksum(serDataW);
           byte[] cmd = serDataW.getBytes("UTF-8");
           rs232.Write(cmd);
           if (i==10) logger.finer("IonicAgilentDual:updateDeviceData> write to device:" + i + " --> " + serDataW);
           Thread.sleep(200); // Essential to get good timing through communication channel
           byte [] answer = null;
           while ( rs232.BytesAvailable() > 0 )
               answer = rs232.Read();
           if ( answer != null )
              serDataR = new String(answer, "UTF-8");
           if (i==10) logger.finer("IonicAgilentDual:updateDeviceData> reading from device:" + serDataR + 
                      "(length=" + serDataR.length() + ")");
           if (serDataR.equals("") || serDataR.charAt(0) != 0x01) { // Com Status Error
              com_error_count+=1;
              if ( com_error_count > MAX_COM_ERROR && hasWarned == false) {
                 logger.log(Level.WARNING, "IonicAgilentDual:updateDeviceData> Communication with " + name + " interrupted");
                 setErrorComStatus();
              }
              continue;
           }
           else {
              com_error_count = 0;
              d.elementAt(d.size() -1).value = 0; // OK COM
              if ( hasWarned == true ) {
                 hasWarned = false;
                 logger.info("IonicAgilentDual:updateDeviceData> Communication with " + name + " back!");
              }
              String resultStr = "";
              try {
                 int ndata = Integer.parseInt(serDataR.substring(1,3));
                 ndata -= 3;
                 resultStr = serDataR.substring(6,6+ndata);
                 d.elementAt(i).value = Double.parseDouble(resultStr);
              }
              catch (StringIndexOutOfBoundsException ex) {
                 logger.log(Level.SEVERE, "IonicAgilentDual:updateDeviceData> Format error " +
                                           d.elementAt(i).name + ":" +  resultStr);
                 d.elementAt(i).value = 0; 
              }
              catch (NumberFormatException n) {
                 logger.log(Level.SEVERE, "IonicAgilentDual:updateDeviceData> Format error " + 
                                           d.elementAt(i).name + ":" +  resultStr);
                 d.elementAt(i).value = 0; 
              }
           }
        }
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "IonicAgilentDual:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "IonicAgilentDual:updateDeviceData>" + ex.getMessage());
           setErrorComStatus();
        }
     }
   }

   public void executeCommand (DataElement e) {
  
      String serDataW = "", serDataR = "";
      if (e.name.contains("P33ONOFF")) { // On/Off command
         if ( e.value == 1 ) // On
           serDataW = "\u0081\u0030\u0034\u0041\u0030\u0031\u0031";
         else if ( e.value == 2 ) // Off
           serDataW = "\u0081\u0030\u0034\u0041\u0030\u0031\u0030";
      }
      else if (e.name.contains("P33REMOTEMODE")) { // Set Remote mode status
         if ( e.setvalue == 0 ) // Local 
           serDataW = "\u0081\u0030\u0034\u005A\u0030\u0030\u0030";
         else if ( e.setvalue == 1 ) // Remote I/O
           serDataW = "\u0081\u0030\u0034\u005A\u0030\u0030\u0031";
         else if ( e.setvalue == 2 ) // Serial 
           serDataW = "\u0081\u0030\u0034\u005A\u0030\u0030\u0032";
      }
      else if (e.name.contains("P33OPMODE")) { // Set Operation mode status
         if ( e.setvalue == 1 ) // Started
           serDataW = "\u0081\u0030\u0034\u0043\u0030\u0031\u0030";
         else if ( e.setvalue == 2 ) // Protected
           serDataW = "\u0081\u0030\u0034\u0043\u0030\u0031\u0031";
      }
      else if (e.name.contains("P33VOLTMODE")) { // Set Voltage mode status
         if ( e.setvalue == 1 ) // Started
           serDataW = "\u0081\u0030\u0034\u0042\u0030\u0031\u0030";
         else if ( e.setvalue == 2 ) // Protected
           serDataW = "\u0081\u0030\u0034\u0042\u0030\u0031\u0031";
      }
      else if (e.name.contains("P33PRTCUR")) { // Set Protected current value
         String data = Integer.toString((int)e.setvalue);
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
         serDataW = "\u0081\u0030\u0038\u004B\u0030\u0031" + data;
      }
      else if (e.name.contains("P33MAXCUR")) { // Set Max current value
         String data = Integer.toString((int)e.setvalue);
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
         serDataW = "\u0081\u0030\u0038\u0049\u0030\u0031" + data;
      }
      else if (e.name.contains("P33MAXVOLT")) { // Set Max voltage value
         String data = Integer.toString((int)e.setvalue);
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
         serDataW = "\u0081\u0030\u0038\u0048\u0030\u0031" + data;        
      }
      else if (e.name.contains("P33MAXW")) { // Set Max power value
         String data = Integer.toString((int)e.setvalue);
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
         serDataW = "\u0081\u0030\u0038\u004A\u0030\u0031" + data;        
      }
      else if (e.name.contains("P33STEP1VOLT")) { // Set Step1 voltage value
         String data = Integer.toString((int)e.setvalue);
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
         serDataW = "\u0081\u0030\u0038\u004C\u0030\u0031" + data;         
      }
      else if (e.name.contains("_P33STEP2VOLT")) { // Set Step2 voltage value
         String data = Integer.toString((int)e.setvalue);
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
         serDataW = "\u0081\u0030\u0038\u004E\u0030\u0031" + data;  
      }
      else if (e.name.contains("P33STEP1CUR")) { // Set Step1 current value
         String data = String.format("%7.1E",e.setvalue);
         data = data.replace(",",".");
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
         serDataW = "\u0081\u0031\u0030\u004D\u0030\u0031" + data;
      }
      else if (e.name.contains("P33STEP2CUR")) { // Set Step2 current value 
         String data = String.format("%7.1E",e.setvalue);
         data = data.replace(",",".");
         while (data.length() < 5)  // Convert to 5 bytes integer notation string
            data = "0" + data;
         serDataW = "\u0081\u0031\u0030\u004F\u0030\u0031" + data;
      }
      serDataW+=xor_checksum(serDataW);
      try {
         logger.finer("IonicAgilentDual:executeCommand> send device: " + serDataW + "(length=" + serDataW.length() + ")"); 
         byte[] cmd = serDataW.getBytes("UTF-8");
         rs232.Write(cmd);
         Thread.sleep(200); // Essential to get good timing through communication channel
         byte [] answer = null;
         while ( rs232.BytesAvailable() > 0 )
            answer = rs232.Read();
         if ( answer != null )
           serDataR = new String(answer, "UTF-8");
        logger.finer("IonicAgilentDual:executeCommand> answer from device:" + serDataR + 
                   "(length=" + serDataR.length() + ")");
        if (e.type == DataType.TRIGGER) {
            Thread.sleep(2000); // Wait before resetting
            e.value = 0;
            holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
      }
      catch (Exception ex) {
        //ex.printStackTrace();
        //logger.log(Level.SEVERE, "IonicAgilentDual>executeCommand:", ex.getMessage());
        setErrorComStatus();
        //logger.log(Level.WARNING, "IonicAgilentDual:executeCommand> Communication with ", name + " interrupted");
      }
   }

   //
   // Utility method for checksum calculation
   //
   char xor_checksum(String input) {
      char res = 0;
      for (int i = 0; i < input.length(); i++) {
         res ^= input.charAt(i);
      }
      res &= 0x7F;
      return (res);
   }

}; 
