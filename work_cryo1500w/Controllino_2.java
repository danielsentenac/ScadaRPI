/*
 * This Class is the implementation of the Controllino device
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;
import java.nio.ByteBuffer;

public class Controllino_2 extends Device {

   private I2C_Comm i2c;
   private static final Logger logger = Logger.getLogger("Main");

   private static final int LEVEL_STATUS_BIT       = 0;                         // LEVEL water STATUS bit
   private static final int FLUX_STATUS_BIT        = 1;                         // LEVEL water STATUS bit
   private static final int CP900_STATUS_BIT       = 2;                         // CP900 Status bit
   private static final int CP800_STATUS_BIT       = 3;                         // CP800 Status bit
   private static final int CHILLER_STATUS_BIT     = 4;                         // CHILLER Status bit
   private static final int PUMP_STATUS_BIT        = 5;                         // PUMP Status bit
   private static final int CP900_ON_CMD_BIT       = 6;                         // CP900 On CMD bit
   private static final int CP900_OFF_CMD_BIT      = 7;                         // CP900 Off CMD bit
   private static final int CP800_ON_CMD_BIT       = 8;                         // CP800 On CMD bit
   private static final int CP800_OFF_CMD_BIT      = 9;                         // CP800 Off CMD bit
   private static final int CHILLER_ON_CMD_BIT     = 10;                        // CHILLER On CMD bit
   private static final int CHILLER_OFF_CMD_BIT    = 11;                        // CHILLER Off CMD bit
   private static final int PUMP_ON_CMD_BIT        = 12;                        // PUMP On CMD bit
   private static final int PUMP_OFF_CMD_BIT       = 13;                        // PUMP Off CMD bit
   private static final int MUXSHIELD_COMERR_BIT   = 14;                        // MUXSHIELD COMM ERR STATUS
   private static final int ARD_RESET_BIT          = 31;                        // Controllino Reset Bit

  
   public Controllino_2 (String _name,
                         int _mbRegisterStart, 
                         int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino_2:Controllino_2> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Status elements
     addDataElement( new DataElement(name, "LEVELST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "FLUXST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "CP900ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "CP800ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1)); 
     addDataElement( new DataElement(name, "CHILLERST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1)); 
     addDataElement( new DataElement(name, "PUMPST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1)); 

     // Commands
     addDataElement( new DataElement(name, "CP900ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "CP800ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "CHILLERONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "PUMPONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // Controller Controllino comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("Controllino_2:Controllino_2> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     // Create I2C communication channel
     try {
        i2c = new I2C_Comm(i2c_addr);
     }
     catch (Exception e) {
        logger.log(Level.SEVERE, e.getMessage());
     }
   }
   
   public void updateDeviceData() {
   
     // Get monitoring data from device using RS232 Comm

     DataElement level = getDataElement("LEVELST");
     DataElement flux = getDataElement("FLUXST");
     DataElement cp900 = getDataElement("CP900ST");
     DataElement cp800 = getDataElement("CP800ST");
     DataElement chiller = getDataElement("CHILLERST");
     DataElement pump = getDataElement("PUMPST");
     DataElement dcom = getDataElement("COMST");
    
     byte[] serDataR = null;
     try {
        // Lock the bus during read/write command to insure correct multiple slave interaction
        while (busmutex.tryLock() == false) {
           try {
              Thread.sleep(10);
              logger.finer("CONTROLLINO 2: mutex locked");
           }
           catch (InterruptedException e) {}
        }
        try {
           popCommand();  // Execute commands in the loop is more reactive
           serDataR = i2c.Read4bytesPlusCRC32();
        }
        catch (Exception ex) {
           if ( hasWarned == false ) {
              logger.log(Level.WARNING, "Controllino_2:updateDeviceData> Communication with " + name + " interrupted");
              logger.log(Level.SEVERE, "Controllino_2:updateDeviceData>" + ex.getMessage());
           }
        }
        finally {
           // Release master priority, unlock 
           busmutex.unlock();
        }
        if (serDataR == null) {
           if (++comm_cnt_failure > comm_max_failure)  setErrorComStatus();
           return;
        }
        int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();
      
        logger.finer("Controllino_2:updateDeviceData> i2c_buffer=" + 
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
        
        if (bitRead(i2c_buffer,LEVEL_STATUS_BIT) == 0x01)
           level.value = 1; // LEVEL KO
        else if (bitRead(i2c_buffer,LEVEL_STATUS_BIT) == 0x00) 
           level.value = 0; // LEVEL OK
        if (bitRead(i2c_buffer,FLUX_STATUS_BIT) == 0x01)
           flux.value = 1; // FLUX KO
        else if (bitRead(i2c_buffer,FLUX_STATUS_BIT) == 0x00) 
           flux.value = 0; // FLUX OK
        if (bitRead(i2c_buffer,CP900_STATUS_BIT) == 0x01)
           cp900.value = 1; // CP900 OFF
        else if (bitRead(i2c_buffer,CP900_STATUS_BIT) == 0x00) 
           cp900.value = 0; // CP900 ON
        if (bitRead(i2c_buffer,CP800_STATUS_BIT) == 0x01)
           cp800.value = 1; // CP800 OFF
        else if (bitRead(i2c_buffer,CP800_STATUS_BIT) == 0x00) 
           cp800.value = 0; // CP800 ON
        if (bitRead(i2c_buffer,CHILLER_STATUS_BIT) == 0x01)
           chiller.value = 1; // CHILLER OFF
        else if (bitRead(i2c_buffer,CHILLER_STATUS_BIT) == 0x00) 
           chiller.value = 0; // CHILLER ON
        if (bitRead(i2c_buffer,PUMP_STATUS_BIT) == 0x01)
           pump.value = 0; // PUMP OFF
        else if (bitRead(i2c_buffer,PUMP_STATUS_BIT) == 0x00) 
           pump.value = 1; // PUMP ON
           
        dcom.value = 0; // if arriving here COM OK
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("Controllino_2:updateDeviceData> Communication with " + name + " back!");
        }
        comm_cnt_failure = 0;
     }
     catch (Exception ex) {
        ex.printStackTrace();
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "Controllino_2:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "Controllino_2:updateDeviceData>" + ex.getMessage());
        }
        if (++comm_cnt_failure > comm_max_failure) {
           setErrorComStatus();
        }
     }
   }
   
   public void executeCommand( DataElement e ) {
      
      try {
         // Read the (old) i2c buffer from the bus
         byte[] serDataR = i2c.Read4bytesPlusCRC32();
         if (serDataR == null) {
           logger.log(Level.WARNING,"Controllino_3:executeCommand> NO COMMAND SENT! (data=null)");
           return;
         }
         int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();
                  
         logger.fine("Controllino_2:executeCommand> reading i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
 
        
         if ( e.name.contains("CP900ONOFF") ) { // CP900 On/Off command
            logger.finer("Controllino_2:executeCommand> set bit CP900 =" + e.value);
            if ( e.value == 1 ) // Switch CP900 On
              i2c_buffer = setBit(i2c_buffer,CP900_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch CP900 Off
              i2c_buffer = clearBit(i2c_buffer,CP900_OFF_CMD_BIT);
         } 
         else if ( e.name.contains("CP800ONOFF") ) { // CP800 On/Off command
            logger.finer("Controllino_2:executeCommand> set bit CP800 =" + e.value);
            if ( e.value == 1 ) // Switch CP800 On
              i2c_buffer = setBit(i2c_buffer,CP800_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch CP800 Off
              i2c_buffer = clearBit(i2c_buffer,CP800_OFF_CMD_BIT);
         } 
         else if ( e.name.contains("CHILLERONOFF") ) { // CHILLER On/Off command
            logger.fine("Controllino_2:executeCommand> set bit CHILLER =" + e.value);
            if ( e.value == 1 ) // Switch CHILLER On
              i2c_buffer = setBit(i2c_buffer,CHILLER_ON_CMD_BIT);
            else if ( e.value == 2 ) {// Switch CHILLER Off
            logger.fine("Controllino_2:executeCommand> before i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
              i2c_buffer = clearBit(i2c_buffer,CHILLER_OFF_CMD_BIT);
              logger.fine("Controllino_2:executeCommand> after i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
              }
         }
         else if ( e.name.contains("PUMPONOFF") ) { // PUMP On/Off command
            logger.finer("Controllino_2:executeCommand> set bit PUMP =" + e.value);
            if ( e.value == 1 ) // Switch PUMP On
              i2c_buffer = setBit(i2c_buffer,PUMP_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch PUMP Off
              i2c_buffer = clearBit(i2c_buffer,PUMP_OFF_CMD_BIT);
         }
         logger.fine("Controllino_2:executeCommand> send i2c_buffer=" +      
                      String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
         // Write the (new) i2c_buffer on the bus
         ByteBuffer buffer = ByteBuffer.allocate(4);
         buffer.putInt(i2c_buffer);
         byte[] msg = buffer.array();   
         i2c.WritePlusCRC32(msg);
         Thread.sleep(2000); // Wait before resetting
         // Reset
         e.value = 0;
         holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "Controllino_2:executeCommand>" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "Controllino_2:executeCommand> Communication with " + name + " interrupted");
     }
   }
    
   public int bitRead(int target, int bit) {
      return (target >> bit) & 1;
   }

   public int setBit(int target, int bit) {
      return (target |= 1 << bit);
   }
   
   public int clearBit(int target, int bit) {
      return (target &= ~(1 << bit));
   }
   


}; 
