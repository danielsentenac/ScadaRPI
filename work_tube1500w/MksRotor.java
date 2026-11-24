/*
 * This Class is the implementation of the MksRotor device
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

public class MksRotor extends Device {

   private Serial_Comm rs232;
   private static final Logger logger = Logger.getLogger("Main");

   // Reading status published in PR1ST
   public static final int ST_STOPPED = 0, ST_MEASURING = 1, ST_STARTING = 2, ST_STOPPING = 3, ST_NO_SENSOR = 4;
   private boolean unitChecked = false;
   private int lastRcs = -1; // Last rotor control status logged

   public MksRotor (String _name,
                     int _mbRegisterStart, 
                     String serial_port, 
                     Baud baudrate, 
                     DataBits databits, 
                     Parity parity, 
                     StopBits stopbits, 
                     FlowControl flowcontrol) {

     name = _name; // Device name

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("MksRotor:MksRotor> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Gauge Pressure
     addDataElement( new DataElement(name, "PR1", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     // 0: stopped, 1: measuring, 2: starting, 3: stopping, 4: no sensor
     addDataElement( new DataElement(name, "PR1ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));

    // Controller MksRotor comm
    addDataElement( new DataElement(name, "GA5COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

    // Measurement START/STOP (1 = start, 2 = stop)
    addDataElement( new DataElement(name, "GA5ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

    mbRegisterEnd+=1;

    logger.finer("MksRotor:MksRotor> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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
   
   // Send a command line to the SRG-3 and return its reply without the prompt character.
   // The SRG-3 ends each reply with CR LF and then prompts with '>' (or '?' if a command failed).
   private boolean lastCommandFailed = false;
   private String exchange(String commands) throws IOException, InterruptedException {
      logger.finer("MksRotor:exchange> serDataW=" + commands);
      rs232.Write(commands + "\r");
      Thread.sleep(200); // Essential to get good timing through communication channel
      StringBuilder reply = new StringBuilder();
      while ( rs232.BytesAvailable() > 0 ) {
         reply.append(new String(rs232.Read(), "UTF-8"));
         Thread.sleep(20);
      }
      logger.finer("MksRotor:exchange> serDataR=START-" + reply + "-END");
      if ( reply.length() == 0 )
         throw new IOException("no answer to [" + commands + "]");
      lastCommandFailed = reply.indexOf("?") >= 0;
      return reply.toString().replace(">", " ").replace("?", " ").trim();
   }

   // Rotor control status (RCS, bits 3..0) to the published reading status
   private static int readingStatus(int rotorState) {
      switch (rotorState) {
         case 6:  return ST_MEASURING;
         case 5:  return ST_STARTING;
         case 7:
         case 8:  return ST_STOPPING;
         case 1:
         case 2:  return ST_NO_SENSOR;
         default: return ST_STOPPED;  // 0 disarmed, 3 idle (rotor at rest), 4 standby (rotor coasting)
      }
   }

   public void updateDeviceData() {

     DataElement d1 = getDataElement("PR1");
     DataElement d2 = getDataElement("PR1ST");
     DataElement dcom = getDataElement("GA5COMST");

     try {
           popCommand();  // Execute commands in the loop is more reactive
           // Rotor control status and measured value in one request : "n  x.xxxxE-xx"
           String[] fields = exchange("rcs val").split("\\s+");
           if ( fields.length < 2 )
              throw new IOException("unexpected answer to [rcs val]");
           int rcs = Integer.parseInt(fields[0]);
           double value = Double.parseDouble(fields[fields.length - 1]);
           int status = readingStatus(rcs & 0x0F);
           if ( rcs != lastRcs )
              logger.info("MksRotor:updateDeviceData> " + name + " rotor control status " + rcs + " -> reading status " + status);
           // The SRG-3 keeps returning its last reading when it is not measuring : publish 0 instead
           d1.value = (status == ST_MEASURING) ? value : 0.0;
           d2.value = status;
           lastRcs = rcs;
           if ( !unitChecked ) { // Once : the panel shows mbar (unit 2)
              String unit = exchange("unt");
              if ( !unit.equals("2") )
                 logger.log(Level.WARNING, "MksRotor:updateDeviceData> " + name + " measurement unit is " + unit + ", not 2 (mbar)");
              unitChecked = true;
           }
           dcom.value = 0;
           hasWarned = false;
     }
     catch (NumberFormatException ex) {
        logger.log(Level.WARNING, "MksRotor:updateDeviceData> Format exception with " + name );
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "MksRotor:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "MksRotor:updateDeviceData>" + ex.getMessage());
        }
        setErrorComStatus();
        d2.value = 0;
     }
   }

   // GA5ONOFF : 1 = start measurement (rotor suspended and driven to speed), 2 = stop the rotor
   public void executeCommand( DataElement e ) {

      logger.fine("MksRotor:executeCommand> command = " + e.name + " value = " + e.setvalue);
      try {
         if ( e.name.equals("GA5ONOFF") && (e.setvalue == 1 || e.setvalue == 2) ) {
            String command = (e.setvalue == 1) ? "sta" : "stp";
            exchange(command);
            if ( lastCommandFailed )
               logger.log(Level.WARNING, "MksRotor:executeCommand> " + name + " refused [" + command + "] : " + exchange("msg"));
            else
               logger.info("MksRotor:executeCommand> " + name + " [" + command + "] sent");
         }
         Thread.sleep(2000); // Wait before resetting
         // Reset
         e.value = 0;
         holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "MksRotor>executeCommand:" + ex.getMessage());
        setErrorComStatus();
     }
   }

};
