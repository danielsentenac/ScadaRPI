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
import com.pi4j.io.gpio.*;

public class Controllino_1 extends Device {

   private I2C_Comm i2c;
   private static final Logger logger = Logger.getLogger("Main");

   private static final int V21DET_OPEN_CMD_BIT      = 0;   // V21 Open bit
   private static final int V21DET_CLOSE_CMD_BIT     = 1;   // V21 Close bit
   private static final int V21DET_OPEN_STATUS_BIT   = 2;   // V21 Open Status bit
   private static final int V21DET_CLOSE_STATUS_BIT  = 3;   // V21 Close Status bit
   private static final int V22DET_OPEN_CMD_BIT      = 4;   // V22 Open bit
   private static final int V22DET_CLOSE_CMD_BIT     = 5;   // V22 Close bit
   private static final int V22DET_OPEN_STATUS_BIT   = 6;   // V22 Open Status bit
   private static final int V22DET_CLOSE_STATUS_BIT  = 7;   // V22 Close Status bit
   private static final int V21IB_OPEN_CMD_BIT       = 8;   // V21 Open bit
   private static final int V21IB_CLOSE_CMD_BIT      = 9;   // V21 Close bit
   private static final int V21IB_OPEN_STATUS_BIT    = 10;   // V21 Open Status bit
   private static final int V21IB_CLOSE_STATUS_BIT   = 11;   // V21 Close Status bit
   private static final int V22IB_OPEN_CMD_BIT       = 12;   // V22 Open bit
   private static final int V22IB_CLOSE_CMD_BIT      = 13;   // V22 Close bit
   private static final int V22IB_OPEN_STATUS_BIT    = 14;   // V22 Open Status bit
   private static final int V22IB_CLOSE_STATUS_BIT   = 15;   // V22 Close Status bit
   private static final int P22DET_ON_CMD_BIT        = 16;  // P22 On bit
   private static final int P22DET_OFF_CMD_BIT       = 17;  // P22 Off bit
   private static final int P22DET_STATUS_BIT        = 18;  // P22 Status bit
   private static final int P22IB_ON_CMD_BIT         = 19;  // P22 On bit
   private static final int P22IB_OFF_CMD_BIT        = 20;  // P22 Off bit
   private static final int P22IB_STATUS_BIT         = 21;  // P22 Status bit
   private static final int COMPRESSAIR_STATUS_BIT   = 22;  // COMPRESSAIR Status bit
   private static final int ARD_RESET_BIT            = 31;  // Controllino Reset Bit

   public Controllino_1 (String _name,
                         int _mbRegisterStart, 
                         int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino_1:Controllino_1> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Valves
     addDataElement( new DataElement(name, "V21DETST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "V22DETST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V21IBST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V22IBST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Compress Air
     addDataElement( new DataElement(name, "COMPRESSAIRST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Pumps
     addDataElement( new DataElement(name, "P22DETST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P22IBST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Commands
     addDataElement( new DataElement(name, "V21DETCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V22DETCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P22DETONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V21IBCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V22IBCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P22IBONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // Controller Controllino comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("Controllino_1:Controllino_1> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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

     DataElement v21det = getDataElement("V21DETST");
     DataElement v22det = getDataElement("V22DETST");
     DataElement v21ib = getDataElement("V21IBST");
     DataElement v22ib  = getDataElement("V22IBST");
     DataElement air = getDataElement("COMPRESSAIRST");
     DataElement p22det = getDataElement("P22DETST");
     DataElement p22ib = getDataElement("P22IBST");
     DataElement dcom = getDataElement("COMST");
    
     byte[] serDataR = null;

     try {
        // Lock the bus during read/write command to insure correct multiple slave interaction
        while (busmutex.tryLock() == false) {
           try {
              Thread.sleep(10);
              logger.finer("CONTROLLINO 1: mutex locked");
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
        int i2c_buffer = ByteBuffer.wrap(serDataR).getInt();
        logger.finer("Controllino_1:updateDeviceData> i2c_buffer=" + 
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
        
        if (bitRead(i2c_buffer,V21DET_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21DET_OPEN_STATUS_BIT) == 0x00)
           v21det.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V21DET_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21DET_CLOSE_STATUS_BIT) == 0x00) 
           v21det.value = 1; // VALVE OPEN
        else
           v21det.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer,V22DET_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22DET_OPEN_STATUS_BIT) == 0x00)
           v22det.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V22DET_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22DET_CLOSE_STATUS_BIT) == 0x00) 
           v22det.value = 1; // VALVE OPEN
        else
           v22det.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,V21IB_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21IB_OPEN_STATUS_BIT) == 0x00)
           v21ib.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V21IB_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21IB_CLOSE_STATUS_BIT) == 0x00) 
           v21ib.value = 1; // VALVE OPEN
        else
           v21ib.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer,V22IB_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22IB_OPEN_STATUS_BIT) == 0x00)
           v22ib.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V22IB_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22IB_CLOSE_STATUS_BIT) == 0x00) 
           v22ib.value = 1; // VALVE OPEN
        else
           v22ib.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,P22DET_STATUS_BIT) == 0x01)
           p22det.value = 0; // SCROLL OFF
        else if (bitRead(i2c_buffer,P22DET_STATUS_BIT) == 0x00)
           p22det.value = 1; // SCROLL ON

        if (bitRead(i2c_buffer,P22IB_STATUS_BIT) == 0x01)
           p22ib.value = 0; // SCROLL OFF
        else if (bitRead(i2c_buffer,P22IB_STATUS_BIT) == 0x00)
           p22ib.value = 1; // SCROLL ON

        if (bitRead(i2c_buffer,COMPRESSAIR_STATUS_BIT) == 0x01)
           air.value = 1; // COMPRESSAIR KO
        else if (bitRead(i2c_buffer,COMPRESSAIR_STATUS_BIT) == 0x00)
           air.value = 0; // COMPRESSAIR OK    

        dcom.value = 0; // if arriving here COM OK
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("Controllino_1:updateDeviceData> Communication with " + name + " back!");
        }
        comm_cnt_failure = 0;
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "Controllino_1:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "Controllino_1:updateDeviceData>" + ex.getMessage());
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
         int i2c_buffer = ByteBuffer.wrap(serDataR).getInt();
         logger.finer("Controllino_1:executeCommand> reading i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
 
         if ( e.name.contains("V21DETCMD") ) { // Valve V21DET Open/Close command
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,V21DET_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V21DET_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V22DETCMD") ) { // Valve V22DET Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit V22DET =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,V22DET_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V22DET_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V21IBCMD") ) { // Valve V21IB Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit V21IB =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,V21IB_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V21IB_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V22IBCMD") ) { // Valve V21IB Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit V22IB =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,V22IB_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V22IB_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("P22DETONOFF") ) { // Scroll P22DET On/Off command
            logger.finer("Controllino_1:executeCommand> set bit P22DET =" + e.value);
            if ( e.value == 1 )      // Switch P22DET On
              i2c_buffer = setBit(i2c_buffer,P22DET_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch P22DET Off
              i2c_buffer = clearBit(i2c_buffer,P22DET_OFF_CMD_BIT);
         }
         else if ( e.name.contains("P22IBONOFF") ) { // Scroll P22IB On/Off command
            logger.finer("Controllino_1:executeCommand> set bit P22IB =" + e.value);
            if ( e.value == 1 )      // Switch P22IB On
              i2c_buffer = setBit(i2c_buffer,P22IB_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch P22IB Off
              i2c_buffer = clearBit(i2c_buffer,P22IB_OFF_CMD_BIT);
         }      
         logger.finer("Controllino_1:executeCommand> send i2c_buffer=" +      
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
        logger.log(Level.SEVERE, "Controllino_1:executeCommand>" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "Controllino_1:executeCommand> Communication with " + name + " interrupted");
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
