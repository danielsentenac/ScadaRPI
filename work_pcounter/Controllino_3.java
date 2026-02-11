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

public class Controllino_3 extends Device {

   private I2C_Comm i2c;
   private static final Logger logger = Logger.getLogger("Main");

   private static final int AC_LOW_START_CMD_BIT       = 0;   // AC HIGH NOISE Open bit
   private static final int AC_LOW_STOP_CMD_BIT        = 1;   // AC HIGH NOISE Close bit
   private static final int AC_LOW_ON_STATUS_BIT       = 2;   // AC LOW_ON NOISE Status bit
   private static final int AC_LOW_OFF_STATUS_BIT      = 3;   // AC LOW_OFF NOISE Status bit --> NOT USED
   private static final int AC_HIGH_START_CMD_BIT      = 4;   // AC LOW NOISE Open bit
   private static final int AC_HIGH_STOP_CMD_BIT       = 5;   // AC LOW NOISE Close bit 
   private static final int AC_HIGH_ON_STATUS_BIT      = 6;   // AC HIGH_ON NOISE Status bit
   private static final int AC_HIGH_OFF_STATUS_BIT     = 7;   // AC HIGH_OFF NOISE Status bit --> NOT USED
   private static final int AC_NET_STATUS_BIT          = 8;   // AC NETWORK ON Status bit
   private static final int PUMP_START_CMD_BIT         = 9;   // PUMP START CMD bit
   private static final int PUMP_STOP_CMD_BIT          = 10;  // PUMP STOP CMD bit
   private static final int PUMP_STATUS_BIT            = 11;  // PUMP ON/OFF Status bit
   private static final int AC_REARM_CMD_BIT           = 12;  // AC REARM Command bit
   private static final int AC_REARM_STATUS_BIT        = 13;  // AC REARM Status bit

   private static final int ARD_RESET_BIT               = 31;  // Controllino Reset Bit

   public Controllino_3 (String _name,
                         int _mbRegisterStart, 
                         int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino_3:Controllino_3> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;


     // AC Status
     addDataElement( new DataElement(name, "ACST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "ACNETST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "REARMST", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     
     // Pump Status
     addDataElement( new DataElement(name, "PUMPST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
  
     // Values for Operation
     addDataElement( new DataElement(name, "TEMP", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "ACQTIME", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TIMEINTER", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TIME", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     
     // Commands
     addDataElement( new DataElement(name, "ACCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PUMPONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "REARMONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // Controller Controllino comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("Controllino_3:Controllino_3> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     // Create I2C communication channel
     try {
        i2c = new I2C_Comm(i2c_addr);
     }
     catch (Exception e) {
        logger.log(Level.SEVERE, e.getMessage());
     }
   }
   
   public void updateDeviceData() {
   
     logger.finer("Controllino_3:updateDeviceData> START");
     // Get monitoring data from device using RS232 Comm

     DataElement pumpst = getDataElement("PUMPST");
     DataElement acst = getDataElement("ACST");
     DataElement rearmst = getDataElement("REARMST");
     DataElement acnetst = getDataElement("ACNETST");
     DataElement temp = getDataElement("TEMP");
     DataElement dcom = getDataElement("COMST");
    
     byte[] serDataR = null;
     
     try {
        // Lock the bus during read/write command to insure correct multiple slave interaction
        while (busmutex.tryLock() == false) {
           try {
              Thread.sleep(10);
           }
           catch (InterruptedException e) {
           }
        }
        try {
           popCommand();  // Execute commands in the loop is more reactive
           serDataR = i2c.Read8bytesPlusCRC32(); // Expect 8 bytes from Module 3        
        }
        catch (Exception ex) {
           if ( hasWarned == false ) {
              logger.log(Level.WARNING, "Controllino_3:updateDeviceData> 1 Communication with " + name + " interrupted");
              logger.log(Level.SEVERE, "Controllino_3:updateDeviceData>" + ex.getMessage());
           }
        }
        finally {  
           // Release master, unlock it
           busmutex.unlock();
        }
        if (serDataR == null) {
           if (++comm_cnt_failure > comm_max_failure)  setErrorComStatus();
           return;
        }
        logger.finer("Controllino_3:updateDeviceData> TREATING DATA !");
        int tempbuf = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();
        
        // If here hopefully all data are good
        temp.value = (double) Float.intBitsToFloat(tempbuf);
        logger.finer("Controllino_3:updateDeviceData> temperature (bit)= " + String.format("%32s",Integer.toBinaryString(tempbuf)).replaceAll(" ", "0"));
        logger.finer("Controllino_3:updateDeviceData> temperature (float)= " + String.valueOf(temp.value));
        int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 4, 8)).getInt();
        logger.finer("Controllino_3:updateDeviceData> i2c_buffer =" +  i2c_buffer);
        logger.finer("Controllino_3:updateDeviceData> i2c_buffer=" + 
        String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
        
        if (bitRead(i2c_buffer,PUMP_STATUS_BIT) == 0x01)
           pumpst.value = 2; // PUMP OFF
        else if (bitRead(i2c_buffer,PUMP_STATUS_BIT) == 0x00)
           pumpst.value = 1; // PUMP ON
           
        if (bitRead(i2c_buffer,AC_REARM_STATUS_BIT) == 0x01)
           rearmst.value = 2; // REARM OFF
        else if (bitRead(i2c_buffer,AC_REARM_STATUS_BIT) == 0x00)
           rearmst.value = 1; // REARM ON
           
        logger.finer("Controllino_3:updateDeviceData> = AC_NET_STATUS_BIT=" +  bitRead(i2c_buffer,AC_NET_STATUS_BIT));
        
        if (bitRead(i2c_buffer,AC_NET_STATUS_BIT) == 0x01)
           acnetst.value = 1; // AC NETWORK ON
        else if (bitRead(i2c_buffer,AC_NET_STATUS_BIT) == 0x00)
           acnetst.value = 2; // AC NETWORK OFF
           
        logger.finer("Controllino_3:updateDeviceData> acnetst.value=" +  ((int)acnetst.value));
        
        logger.finer("Controllino_3:updateDeviceData> = AC_HIGH_ON_STATUS_BIT=" +  bitRead(i2c_buffer,AC_HIGH_ON_STATUS_BIT));
        logger.finer("Controllino_3:updateDeviceData> = AC_HIGH_OFF_STATUS_BIT=" +  bitRead(i2c_buffer,AC_HIGH_OFF_STATUS_BIT));
        logger.finer("Controllino_3:updateDeviceData> = AC_LOW_ON_STATUS_BIT=" +  bitRead(i2c_buffer,AC_LOW_ON_STATUS_BIT));
        logger.finer("Controllino_3:updateDeviceData> = AC_LOW_OFF_STATUS_BIT=" +  bitRead(i2c_buffer,AC_LOW_OFF_STATUS_BIT));
        logger.finer("-----------------------------------------------------------------------------------------------");
        
        if (bitRead(i2c_buffer,AC_HIGH_ON_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,AC_LOW_ON_STATUS_BIT) == 0x00)
           acst.value = 2; // AC HIGH NOISE ON
        if (bitRead(i2c_buffer,AC_LOW_ON_STATUS_BIT) == 0x01 bitRead(i2c_buffer,AC_HIGH_ON_STATUS_BIT) == 0x00)
           acst.value = 1; // AC LOW NOISE ON 
        if (bitRead(i2c_buffer,AC_HIGH_ON_STATUS_BIT) == 0x00 && bitRead(i2c_buffer,AC_LOW_ON_STATUS_BIT) == 0x00)
           acst.value = 0; // AC OFF
        if (bitRead(i2c_buffer,AC_HIGH_ON_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,AC_LOW_ON_STATUS_BIT) == 0x01)
           acst.value = 3; // AC STATUS ERROR
        
        dcom.value = 0; // if arriving here COM OK
        
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("Controllino_3:updateDeviceData> Communication with " + name + " back!");
        }
        comm_cnt_failure = 0;
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "Controllino_3:updateDeviceData> 2 Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "Controllino_3:updateDeviceData>" + ex.getMessage());
        }
        if (++comm_cnt_failure > comm_max_failure) {
           setErrorComStatus();
        }
     }
   }
   
   public void executeCommand( DataElement e ) {
       
      try {
         // Read the (old) i2c buffer from the bus
         byte[] serDataR = null;
         logger.finer("Controllino_3:executeCommand> BEFORE Read8bytesPlusCRC32");
         serDataR = i2c.Read8bytesPlusCRC32();
         if (serDataR == null) {
           logger.log(Level.WARNING,"Controllino_3:executeCommand> NO COMMAND SENT! (data=null)");
           return;
        }
         logger.finer("Controllino_3:executeCommand> AFTER Read8bytesPlusCRC32");
         int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 4, 8)).getInt();
         if (i2c_buffer == 0) { // Avoid empty bytes on wire occurence
           logger.log(Level.WARNING, "Controllino_3:executeCommand> Corrupted data from wire for " + name);
           return;
         }
         logger.finer("Controllino_3:executeCommand> reading i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
 
         if ( e.name.contains("ACCMD") ) { // AC HIGH/LOW/OFF command
            if ( e.value == 1 ) { // Set OFF command
              i2c_buffer = clearBit(i2c_buffer,AC_LOW_STOP_CMD_BIT);
              i2c_buffer = clearBit(i2c_buffer,AC_HIGH_STOP_CMD_BIT);
            }
            else if ( e.value == 2 ) { // Set LOW NOISE command
              i2c_buffer = clearBit(i2c_buffer,AC_HIGH_STOP_CMD_BIT);
              i2c_buffer = setBit(i2c_buffer,AC_LOW_START_CMD_BIT);
            }
            else if ( e.value == 3 ) { // Set HIGH NOISE command
              i2c_buffer = clearBit(i2c_buffer,AC_LOW_STOP_CMD_BIT);
              i2c_buffer = setBit(i2c_buffer,AC_HIGH_START_CMD_BIT);
            }
         }
         else if ( e.name.contains("PUMPONOFF") ) { // Pump Start/Stop command
            if ( e.value == 1 ) // Start Pump
              i2c_buffer = setBit(i2c_buffer,PUMP_START_CMD_BIT);
            else if ( e.value == 2 ) // Stop Pump
              i2c_buffer = clearBit(i2c_buffer,PUMP_STOP_CMD_BIT);
         }
         else if ( e.name.contains("REARMONOFF") ) { // Rearm On/Off command
            if ( e.value == 1 ) // Rearm ON
              i2c_buffer = setBit(i2c_buffer,AC_REARM_CMD_BIT);
            else if ( e.value == 2 ) // Rearm OFF
              i2c_buffer = clearBit(i2c_buffer,AC_REARM_CMD_BIT);
         }
         else if ( e.name.contains("RESET3") ) { // RESET Controllino 3 command
            if ( e.value == 1 ) // Rest Controllino 3
              i2c_buffer = setBit(i2c_buffer,ARD_RESET_BIT);
         }
      
         logger.finer("Controllino_3:executeCommand> send i2c_buffer=" +      
                      String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
         // Write the (new) i2c_buffer on the bus
         ByteBuffer buffer = ByteBuffer.allocate(4);
         buffer.putInt(i2c_buffer);
         byte[] msg = buffer.array();   
         i2c.WritePlusCRC32(msg);
         Thread.sleep(2000); // Wait before going on
         if (e.type == DataType.TRIGGER) {
            // Reset
            e.value = 0;
            holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "Controllino_3:executeCommand>" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "Controllino_3:executeCommand> Communication with " + name + " interrupted");
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
