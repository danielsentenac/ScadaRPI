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

   private static final int CHANNEL_1_ON_STATUS_BIT       = 0;                         // Channel 1 On Status bit
   private static final int CHANNEL_1_OFF_STATUS_BIT      = 1;                         // Channel 1 Off Status bit
   private static final int CHANNEL_2_ON_STATUS_BIT       = 2;                         // Channel 1 On Status bit
   private static final int CHANNEL_2_OFF_STATUS_BIT      = 3;                         // Channel 1 Off Status bit
   private static final int CHANNEL_1_ON_CMD_BIT          = 4;                         // Channel 1 On CMD bit
   private static final int CHANNEL_1_OFF_CMD_BIT         = 5;                         // Channel 1 Off CMD bit
   private static final int CHANNEL_2_ON_CMD_BIT          = 6;                         // Channel 2 On CMD bit
   private static final int CHANNEL_2_OFF_CMD_BIT         = 7;                         // Channel 2 Off CMD bit
   private static final int SICEM_ALRM_STATUS_BIT         = 8;                         // SICEM GLOBAL ALARM STATUS bit
   private static final int SICEM_COMERR_STATUS_BIT       = 9;                         // SICEM COMM ERR STATUS
   private static final int MUXSHIELD_COMERR_STATUS_BIT   = 10;                        // MUXSHIELD COMM ERR STATUS
   private static final int ARD_RESET_BIT                 = 11;                        // Controllino Reset Bit

   public Controllino_2 (String _name,
                         int _mbRegisterStart, 
                         int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset
     logger.finer("Controllino_2:Controllino_2> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // All Read Status
     addDataElement( new DataElement(name, "CHANNEL_1_ONOFF_ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "CHANNEL_2_ONOFF_ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "SICEM_ALRM_ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "SICEM_COMERR_ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "MUXSHIELD_COMERR_ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     
     // Commands
     addDataElement( new DataElement(name, "CHANNEL_1_ONOFF_CMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "CHANNEL_2_ONOFF_CMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     
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

     DataElement ch1onoff = getDataElement("CHANNEL_1_ONOFF_ST");
     DataElement ch2onoff = getDataElement("CHANNEL_2_ONOFF_ST");
     DataElement sicemalrm = getDataElement("SICEM_ALRM_ST");
     DataElement sicemcom = getDataElement("SICEM_COMERR_ST");
     DataElement muxcom = getDataElement("MUXSHIELD_COMERR_ST");
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
            Thread.sleep(1000);
           // Assign i2c_buffer values
           serDataR = i2c.ReadbytesPlusCRC32(4); // Reads data
           if (serDataR == null) {
              if (++comm_cnt_failure > comm_max_failure)  setErrorComStatus();
              return;
           }        
           else
               comm_cnt_failure = 0;  
           int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();
           logger.finer("Controllino_2:updateDeviceData> REGULAR i2c_buffer=" + 
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
        
           if (bitRead(i2c_buffer,CHANNEL_1_ON_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,CHANNEL_1_OFF_STATUS_BIT) == 0x00)
              ch1onoff.value = 1; // CHANNEL 1 ON
           else if (bitRead(i2c_buffer,CHANNEL_1_ON_STATUS_BIT) == 0x00 && bitRead(i2c_buffer,CHANNEL_1_OFF_STATUS_BIT) == 0x01) 
              ch1onoff.value = 2; // CHANNEL 1 ON
           else
              ch1onoff.value = 0; // CHANNEL 1 ERROR
          
           if (bitRead(i2c_buffer,CHANNEL_2_ON_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,CHANNEL_2_OFF_STATUS_BIT) == 0x00)
              ch2onoff.value = 1; // CHANNEL 2 ON
           else if (bitRead(i2c_buffer,CHANNEL_2_ON_STATUS_BIT) == 0x00 && bitRead(i2c_buffer,CHANNEL_2_OFF_STATUS_BIT) == 0x01) 
              ch2onoff.value = 2; // CHANNEL 2 ON
           else
              ch2onoff.value = 0; // CHANNEL 2 ERROR
         
           sicemalrm.value  = (double)bitRead(i2c_buffer,SICEM_ALRM_STATUS_BIT);
           sicemcom.value   = (double)bitRead(i2c_buffer,SICEM_COMERR_STATUS_BIT);
           muxcom.value     = (double)bitRead(i2c_buffer,MUXSHIELD_COMERR_STATUS_BIT);
          
           dcom.value = 0; // if arriving here COM OK
           // RESET HasWarned flag
           if ( hasWarned == true ) {
              hasWarned = false;
              logger.info("Controllino_2:updateDeviceData> Communication with " + name + " back!");
           }
        }
        catch (Exception ex) {
           ex.printStackTrace();
           if ( hasWarned == false ) {
              logger.log(Level.WARNING, "Controllino_2:updateDeviceData> Communication with " + name + " interrupted");
              logger.log(Level.SEVERE, "Controllino_2:updateDeviceData> " + ex.getMessage());
              if (++comm_cnt_failure > comm_max_failure)  setErrorComStatus();
           }
        }
        finally {
           // Release master priority, unlock 
           busmutex.unlock();
        }
     }
     catch (Exception ex) {
        ex.printStackTrace();
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "Controllino_2:updateDeviceData> 2 Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "Controllino_2:updateDeviceData> 2 " + ex.getMessage());
        }
        if (++comm_cnt_failure > comm_max_failure) {
           setErrorComStatus();
        }
     }
   }
   
   public void executeCommand( DataElement e ) {
      
      try {
         // Init return values  
         int pos = -1;
         int setpoint = 0;
         // Read the (old) i2c buffer from the bus
         // Assign i2c_buffer values
         byte[] serDataR = i2c.ReadbytesPlusCRC32(4);
         if (serDataR == null) {
           logger.log(Level.WARNING,"Controllino_2:executeCommand> NO COMMAND SENT! (data=null)");
           return;
         }
         int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();
         
         logger.finer("Controllino_2:executeCommand> reading i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
         logger.finer("Controllino_2:executeCommand> e.name =" + e.name); 
         if ( e.name.contains("CHANNEL_1_ONOFF_CMD") ) { // CHANNEL 1 ON/OFF command
            if ( e.value == 1 ) // CHANNEL 1 ON
              i2c_buffer = setBit(i2c_buffer,CHANNEL_1_ON_CMD_BIT);
            else if ( e.value == 2 )  // CHANNEL 1 OFF
              i2c_buffer = clearBit(i2c_buffer,CHANNEL_1_OFF_CMD_BIT);
         }
         else if ( e.name.contains("CHANNEL_2_ONOFF_CMD") ) { // CHANNEL 2 ON/OFF command
            if ( e.value == 1 )  // CHANNEL 2 ON
              i2c_buffer = setBit(i2c_buffer,CHANNEL_2_ON_CMD_BIT);
            else if ( e.value == 2 ) // CHANNEL 2 OFF
              i2c_buffer = clearBit(i2c_buffer,CHANNEL_2_OFF_CMD_BIT);
         }
         logger.finer("Controllino_2:executeCommand> send i2c_buffer = " +      
                      String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
         // Write msg on the bus
         ByteBuffer buffer = ByteBuffer.allocate(4);
         buffer.putInt(i2c_buffer);
         byte[] msg = buffer.array();   
         i2c.WritePlusCRC32(msg);
         if (e.type == DataType.TRIGGER) {
            Thread.sleep(2000); // Wait before resetting
            // Reset
            e.value = 0;
            holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
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
