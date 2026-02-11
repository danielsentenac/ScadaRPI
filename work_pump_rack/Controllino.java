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

public class Controllino extends Device {

   private I2C_Comm i2c;
   private static final Logger logger = Logger.getLogger("Main");

   private static final int V21_OPEN_CMD_BIT       = 0;   // V21 Open bit
   private static final int V21_CLOSE_CMD_BIT      = 1;   // V21 Close bit
   private static final int V22_OPEN_CMD_BIT       = 2;   // V22 Open bit
   private static final int V22_CLOSE_CMD_BIT      = 3;   // V22 Close bit
   private static final int P22_ON_CMD_BIT         = 4;   // P22 On bit
   private static final int P22_OFF_CMD_BIT        = 5;   // P22 Off bit
   private static final int V21_OPEN_STATUS_BIT    = 6;   // V21ST Open Status bit
   private static final int V21_CLOSE_STATUS_BIT   = 7;   // V21ST Close Status bit
   private static final int V22_OPEN_STATUS_BIT    = 8;   // V22ST Open Status bit
   private static final int V22_CLOSE_STATUS_BIT   = 9;   // V22ST Close Status bit
   private static final int P22_STATUS_BIT         = 10;  // P22ST Status bit
   private static final int V23_OPEN_STATUS_BIT    = 11;  // V23ST Open Status bit
   private static final int V23_CLOSE_STATUS_BIT   = 12;  // V23ST Close Status bit
   private static final int COMPRESSAIR_STATUS_BIT = 13;  // COMPRESSAIRST Status bit
   private static final int VA1_OPEN_STATUS_BIT    = 14;  // VA1ST Open Status bit
   private static final int VA1_CLOSE_STATUS_BIT   = 15;  // VA1ST Close Status bit
   private static final int VA2_OPEN_STATUS_BIT    = 16;  // VA2ST Open Status bit
   private static final int VA2_CLOSE_STATUS_BIT   = 17;  // VA2ST Close Status bit
   private static final int V31_OPEN_STATUS_BIT    = 18;  // V31ST Open Status bit
   private static final int V31_CLOSE_STATUS_BIT   = 19;  // V31ST Close Status bit
   private static final int V31_OPEN_CMD_BIT       = 20;  // V31 Open bit
   private static final int V31_CLOSE_CMD_BIT      = 21;  // V31 Close bit
   private static final int P31_32_ON_CMD_BIT      = 22;  // P31_32 On bit
   private static final int P31_32_OFF_CMD_BIT     = 23;  // P31_32 Off bit
   private static final int P31_32_STATUS_BIT      = 24;  // P31_32ST Status bit
   private static final int ARD_RESET_BIT          = 31;  // Controllino Reset Bit


   public Controllino (String _name,
                       int _mbRegisterStart, 
                       int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino:Controllino> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Valves
     addDataElement( new DataElement(name, "V21ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "V22ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V23ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VA1ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VA2ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V31ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
  
     // Compress Air
     addDataElement( new DataElement(name, "COMPRESSAIRST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Pumps
     addDataElement( new DataElement(name, "P22ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P3132ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Commands
     addDataElement( new DataElement(name, "V21CMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V22CMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P22ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P3132ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // Controller Controllino comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("Controllino:Controllino> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

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

     DataElement v21 = getDataElement("V21ST");
     DataElement v22 = getDataElement("V22ST");
     DataElement v23 = getDataElement("V23ST");
     DataElement va1 = getDataElement("VA1ST");
     DataElement va2 = getDataElement("VA2ST");
     DataElement v31 = getDataElement("V31ST");
     DataElement air = getDataElement("COMPRESSAIRST");
     DataElement p22 = getDataElement("P22ST");
     DataElement p31_32 = getDataElement("P3132ST");
     DataElement dcom = getDataElement("COMST");
    
     try {
        //addModbusCommand(); // Push Modbus commands in the loop is more reactive
        //logger.finer(" --> " + name +  ":next command...");
        // Lock the bus during read/write command to insure correct multiple slave interaction
        busmutex.lock();
        popCommand();  // Execute commands in the loop is more reactive
        byte[] serDataR = i2c.Read();
        busmutex.unlock();
        dcom.value = 0; // if arriving here COM OK
        int i2c_buffer = ByteBuffer.wrap(serDataR).getInt();
        //logger.finer("Controllino:updateDeviceData> i2c_buffer=" + 
        //             String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
        
        if (bitRead(i2c_buffer,V21_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21_OPEN_STATUS_BIT) == 0x00)
           v21.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V21_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V21_CLOSE_STATUS_BIT) == 0x00) 
           v21.value = 1; // VALVE OPEN
        else
           v21.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer,V22_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22_OPEN_STATUS_BIT) == 0x00)
           v22.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V22_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V22_CLOSE_STATUS_BIT) == 0x00) 
           v22.value = 1; // VALVE OPEN
        else
           v22.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,V23_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V23_OPEN_STATUS_BIT) == 0x00)
           v23.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V23_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V23_CLOSE_STATUS_BIT) == 0x00) 
           v23.value = 1; // VALVE OPEN
        else
           v23.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,VA1_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VA1_OPEN_STATUS_BIT) == 0x00)
           va1.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,VA1_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VA1_CLOSE_STATUS_BIT) == 0x00) 
           va1.value = 1; // VALVE OPEN
        else
           va1.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,VA2_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VA2_OPEN_STATUS_BIT) == 0x00)
           va2.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,VA2_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VA2_CLOSE_STATUS_BIT) == 0x00) 
           va2.value = 1; // VALVE OPEN
        else
           va2.value = 0; // VALVE MOVING
        if (bitRead(i2c_buffer,V31_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V31_OPEN_STATUS_BIT) == 0x00)
           va2.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V31_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V31_CLOSE_STATUS_BIT) == 0x00) 
           v31.value = 1; // VALVE OPEN
        else
           v31.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,P22_STATUS_BIT) == 0x01)
           p22.value = 1; // SCROLL ON
        else if (bitRead(i2c_buffer,P22_STATUS_BIT) == 0x00)
           p22.value = 0; // SCROLL OFF

         if (bitRead(i2c_buffer,P31_32_STATUS_BIT) == 0x01)
           p31_32.value = 1; // TITANIUM ON
        else if (bitRead(i2c_buffer,P31_32_STATUS_BIT) == 0x00)
           p31_32.value = 0; // TITANIUM OFF

        if (bitRead(i2c_buffer,COMPRESSAIR_STATUS_BIT) == 0x01)
           air.value = 1; // COMPRESSAIR OK
        else if (bitRead(i2c_buffer,COMPRESSAIR_STATUS_BIT) == 0x00)
           air.value = 0; // COMPRESSAIR KO        
     }
     catch (Exception ex) {
        logger.log(Level.WARNING, "Controllino:updateDeviceData> Communication with " + name + " interrupted");
        logger.log(Level.SEVERE, "Controllino>updateDeviceData:" + ex.getMessage());
        ex.printStackTrace();
        dcom.value = 1; //ERR COM
        setErrorComStatus();
     }
   }
   
   public void executeCommand( DataElement e ) {
      
      try {
         // Read the (old) i2c buffer from the bus
         byte[] serDataR = i2c.Read();
         int i2c_buffer = ByteBuffer.wrap(serDataR).getInt();
         logger.finer("Controllino:executeCommand> reading i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
 
         if ( e.name.contains("V21CMD") ) { // Valve V21 Open/Close command
            if ( e.value == 1 ) // Open Valve
              i2c_buffer = setBit(i2c_buffer,V21_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V21_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V22CMD") ) { // Valve V22 Open/Close command
            logger.finer("Controllino:executeCommand> set bit V22 =" + e.value);
            if ( e.value == 1 ) // Open Valve
              i2c_buffer = setBit(i2c_buffer,V22_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V22_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V31CMD") ) { // Valve V31 Open/Close command
            logger.finer("Controllino:executeCommand> set bit V31 =" + e.value);
            if ( e.value == 1 ) // Open Valve
              i2c_buffer = setBit(i2c_buffer,V31_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V31_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("P22ONOFF") ) { // Scroll P22 On/Off command
            logger.finer("Controllino:executeCommand> set bit P22 =" + e.value);
            if ( e.value == 1 ) // Switch P22 On
              i2c_buffer = setBit(i2c_buffer,P22_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch P22 Off
              i2c_buffer = clearBit(i2c_buffer,P22_OFF_CMD_BIT);
         } 
         else if ( e.name.contains("P3132ONOFF") ) { // Titanium P31_32 On/Off command
            logger.finer("Controllino:executeCommand> set bit P31_32 =" + e.value);
            if ( e.value == 1 ) // Switch P31_32 On
              i2c_buffer = setBit(i2c_buffer,P31_32_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch P31_32 Off
              i2c_buffer = clearBit(i2c_buffer,P31_32_OFF_CMD_BIT);
         } 
      
         logger.finer("Controllino:executeCommand> send i2c_buffer=" +      
                      String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
         // Write the (new) i2c_buffer on the bus
         ByteBuffer buffer = ByteBuffer.allocate(4);
         buffer.putInt(i2c_buffer);
         byte[] msg = buffer.array();   
         i2c.Write(msg);
         Thread.sleep(2000); // Wait before resetting
         // Reset
         e.value = 0;
         holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "Controllino>executeCommand:" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "Controllino:executeCommand> Communication with " + name + " interrupted");
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
