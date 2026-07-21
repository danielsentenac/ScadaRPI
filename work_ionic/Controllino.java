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

   // Valve commands disabled: V31/V32 not operable for now
   //private static final int V31_OPEN_CMD_BIT       = 0;   // V31 Open bit
   //private static final int V31_CLOSE_CMD_BIT      = 1;   // V31 Close bit
   //private static final int V32_OPEN_CMD_BIT       = 2;   // V32 Open bit
   //private static final int V32_CLOSE_CMD_BIT      = 3;   // V32 Close bit
   private static final int V31_OPEN_STATUS_BIT    = 4;   // V31ST Open Status bit
   private static final int V31_CLOSE_STATUS_BIT   = 5;   // V31ST Close Status bit
   private static final int V32_OPEN_STATUS_BIT    = 6;   // V32ST Open Status bit
   private static final int V32_CLOSE_STATUS_BIT   = 7;   // V32ST Close Status bit
   private static final int ARD_RESET_BIT          = 31;  // Controllino Reset Bit


   public Controllino (String _name,
                       int _mbRegisterStart, 
                       int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino:Controllino> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Valves
     addDataElement( new DataElement(name, "V31ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "V32ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Commands (disabled: V31/V32 not operable for now)
     //addDataElement( new DataElement(name, "V31CMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     //addDataElement( new DataElement(name, "V32CMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

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

     DataElement v31 = getDataElement("V31ST");
     DataElement v32 = getDataElement("V32ST");
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
        
        if (bitRead(i2c_buffer,V31_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V31_OPEN_STATUS_BIT) == 0x00)
           v31.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V31_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V31_CLOSE_STATUS_BIT) == 0x00)
           v31.value = 1; // VALVE OPEN
        else
           v31.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,V32_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V32_OPEN_STATUS_BIT) == 0x00)
           v32.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V32_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V32_CLOSE_STATUS_BIT) == 0x00)
           v32.value = 1; // VALVE OPEN
        else
           v32.value = 0; // VALVE MOVING
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
 
         // Valve commands disabled: V31/V32 not operable for now
         /*if ( e.name.contains("V31CMD") ) { // Valve V31 Open/Close command
            logger.finer("Controllino:executeCommand> set bit V31 =" + e.value);
            if ( e.value == 1 ) // Open Valve
              i2c_buffer = setBit(i2c_buffer,V31_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V31_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V32CMD") ) { // Valve V32 Open/Close command
            logger.finer("Controllino:executeCommand> set bit V32 =" + e.value);
            if ( e.value == 1 ) // Open Valve
              i2c_buffer = setBit(i2c_buffer,V32_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V32_CLOSE_CMD_BIT);
         }*/

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
