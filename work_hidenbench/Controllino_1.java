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

   private static final int V21A_OPEN_CMD_BIT        = 0;   // V21_A Open bit
   private static final int V21A_CLOSE_CMD_BIT       = 1;   // V21_A Close bit
   private static final int V21A_OPEN_STATUS_BIT     = 2;   // V21_A Open Status bit
   private static final int V21A_CLOSE_STATUS_BIT    = 3;   // V21_A Close Status bit
   private static final int V22A_OPEN_CMD_BIT        = 4;   // V22_A Open bit
   private static final int V22A_CLOSE_CMD_BIT       = 5;   // V22_A Close bit
   private static final int V22A_OPEN_STATUS_BIT     = 6;   // V22_A Open Status bit
   private static final int V22A_CLOSE_STATUS_BIT    = 7;   // V22_A Close Status bit
   private static final int V21B_OPEN_CMD_BIT        = 8;   // V21_B Open bit
   private static final int V21B_CLOSE_CMD_BIT       = 9;   // V21_B Close bit
   private static final int V21B_OPEN_STATUS_BIT     = 10;  // V21_B Open Status bit
   private static final int V21B_CLOSE_STATUS_BIT    = 11;  // V21_B Close Status bit
   private static final int V22B_OPEN_CMD_BIT        = 12;  // V22_B Open bit
   private static final int V22B_CLOSE_CMD_BIT       = 13;  // V22_B Close bit
   private static final int V22B_OPEN_STATUS_BIT     = 14;  // V22_B Open Status bit
   private static final int V22B_CLOSE_STATUS_BIT    = 15;  // V22_B Close Status bit
   private static final int P22A_ON_CMD_BIT          = 16;  // P22_A On bit
   private static final int P22A_OFF_CMD_BIT         = 17;  // P22_A Off bit
   private static final int P22A_STATUS_BIT          = 18;  // P22_A Status bit
   private static final int P22B_ON_CMD_BIT          = 19;  // P22_B On bit
   private static final int P22B_OFF_CMD_BIT         = 20;  // P22_B Off bit
   private static final int P22B_STATUS_BIT          = 21;  // P22_B Status bit
   private static final int AIRPRESSURE_STATUS_BIT   = 22;  // Air pressure status bit
   private static final int BYPASS_START_CMD_BIT     = 23;  // BYPASS Start bit
   private static final int BYPASS_STOP_CMD_BIT      = 24;  // BYPASS Stop bit
   private static final int BYPASS_STATUS_BIT        = 25;  // BYPASS Status bit
   private static final int ARD_RESET_BIT            = 31;  // Controllino Reset Bit

   public Controllino_1 (String _name,
                         int _mbRegisterStart, 
                         int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino_1:Controllino_1> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Valves
     addDataElement( new DataElement(name, "V21AST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "V22AST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V21BST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V22BST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Air pressure
     addDataElement( new DataElement(name, "AIRPRESSUREST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Pumps and bypass
     addDataElement( new DataElement(name, "P22AST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P22BST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "BYPASSST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Commands
     addDataElement( new DataElement(name, "V21ACMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V22ACMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P22AONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V21BCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V22BCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P22BONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "BYPASSONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

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

     DataElement v21a = getDataElement("V21AST");
     DataElement v22a = getDataElement("V22AST");
     DataElement v21b = getDataElement("V21BST");
     DataElement v22b = getDataElement("V22BST");
     DataElement air = getDataElement("AIRPRESSUREST");
     DataElement p22a = getDataElement("P22AST");
     DataElement p22b = getDataElement("P22BST");
     DataElement bypass = getDataElement("BYPASSST");
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
              logger.log(Level.WARNING, "Controllino_1:updateDeviceData> Communication with " + name + " interrupted");
              logger.log(Level.SEVERE, "Controllino_1:updateDeviceData>" + ex.getMessage());
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
        logger.finer("Controllino_1:updateDeviceData> i2c_buffer=" + 
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
        
        if (bitRead(i2c_buffer,V21A_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21A_OPEN_STATUS_BIT) == 0x00)
           v21a.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V21A_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21A_CLOSE_STATUS_BIT) == 0x00) 
           v21a.value = 1; // VALVE OPEN
        else
           v21a.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer,V22A_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22A_OPEN_STATUS_BIT) == 0x00)
           v22a.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V22A_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22A_CLOSE_STATUS_BIT) == 0x00) 
           v22a.value = 1; // VALVE OPEN
        else
           v22a.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,V21B_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21B_OPEN_STATUS_BIT) == 0x00)
           v21b.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V21B_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21B_CLOSE_STATUS_BIT) == 0x00) 
           v21b.value = 1; // VALVE OPEN
        else
           v21b.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer,V22B_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22B_OPEN_STATUS_BIT) == 0x00)
           v22b.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V22B_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22B_CLOSE_STATUS_BIT) == 0x00) 
           v22b.value = 1; // VALVE OPEN
        else
           v22b.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,P22A_STATUS_BIT) == 0x01)
           p22a.value = 0; // SCROLL OFF
        else if (bitRead(i2c_buffer,P22A_STATUS_BIT) == 0x00)
           p22a.value = 1; // SCROLL ON

        if (bitRead(i2c_buffer,P22B_STATUS_BIT) == 0x01)
           p22b.value = 0; // SCROLL OFF
        else if (bitRead(i2c_buffer,P22B_STATUS_BIT) == 0x00)
           p22b.value = 1; // SCROLL ON

        if (bitRead(i2c_buffer,AIRPRESSURE_STATUS_BIT) == 0x01)
           air.value = 1; // AIR PRESSURE FAIL
        else if (bitRead(i2c_buffer,AIRPRESSURE_STATUS_BIT) == 0x00)
           air.value = 0; // AIR PRESSURE OK

        if (bitRead(i2c_buffer,BYPASS_STATUS_BIT) == 0x01)
           bypass.value = 1; // BYPASS ON
        else if (bitRead(i2c_buffer,BYPASS_STATUS_BIT) == 0x00)
           bypass.value = 2; // BYPASS OFF

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
           logger.log(Level.WARNING,"Controllino_1:executeCommand> NO COMMAND SENT! (data=null)");
           return;
         }
         int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();
         logger.finer("Controllino_1:executeCommand> reading i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
 
         if ( e.name.contains("V21ACMD") ) { // Valve V21_A Open/Close command
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,V21A_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V21A_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V22ACMD") ) { // Valve V22_A Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit V22A =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,V22A_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V22A_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V21BCMD") ) { // Valve V21_B Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit V21B =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,V21B_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V21B_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V22BCMD") ) { // Valve V22_B Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit V22B =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,V22B_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V22B_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("P22AONOFF") ) { // Scroll P22_A On/Off command
            logger.finer("Controllino_1:executeCommand> set bit P22A =" + e.value);
            if ( e.value == 1 )      // Switch P22_A On
              i2c_buffer = setBit(i2c_buffer,P22A_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch P22_A Off
              i2c_buffer = clearBit(i2c_buffer,P22A_OFF_CMD_BIT);
         }
         else if ( e.name.contains("P22BONOFF") ) { // Scroll P22_B On/Off command
            logger.finer("Controllino_1:executeCommand> set bit P22B =" + e.value);
            if ( e.value == 1 )      // Switch P22_B On
              i2c_buffer = setBit(i2c_buffer,P22B_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch P22_B Off
              i2c_buffer = clearBit(i2c_buffer,P22B_OFF_CMD_BIT);
         }
         else if ( e.name.contains("BYPASSONOFF") ) { // BYPASS Start/Stop command
            logger.finer("Controllino_1:executeCommand> set bit BYPASS =" + e.value);
            if ( e.value == 1 )      // Start BYPASS
              i2c_buffer = setBit(i2c_buffer,BYPASS_START_CMD_BIT);
            else if ( e.value == 2 ) // Stop BYPASS
              i2c_buffer = clearBit(i2c_buffer,BYPASS_STOP_CMD_BIT);
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
