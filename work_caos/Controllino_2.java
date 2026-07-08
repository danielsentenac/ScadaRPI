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

   private static final int VENT_OPEN_CMD_BIT       = 0;   // VENT Open bit
   private static final int VENT_CLOSE_CMD_BIT      = 1;   // VENT Close bit
   private static final int VENT_OPEN_STATUS_BIT    = 2;   // VENT Open Status bit
   private static final int VENT_CLOSE_STATUS_BIT   = 3;   // VENT Close Status bit
   private static final int VENTSOFT_OPEN_CMD_BIT   = 4;   // VENTSOFT Open bit
   private static final int VENTSOFT_CLOSE_CMD_BIT  = 5;   // VENTSOFT Close bit
   private static final int VP_OPEN_CMD_BIT         = 6;   // VP Open bit
   private static final int VP_CLOSE_CMD_BIT        = 7;   // VP Close bit
   private static final int VP_OPEN_STATUS_BIT      = 8;   // VP Open Status bit
   private static final int VP_CLOSE_STATUS_BIT     = 9;   // VP Close Status bit
   private static final int BYPASS_ON_CMD_BIT       = 10;  // BYPASS ON bit
   private static final int BYPASS_OFF_CMD_BIT      = 11;  // BYPASS OFF bit
   private static final int BYPASS_ON_STATUS_BIT    = 12;  // BYPASS ON Status bit
   private static final int BYPASS_OFF_STATUS_BIT   = 13;  // BYPASS OFF Status bit
   private static final int VSPARE_ON_CMD_BIT       = 14;  // VSPARE On/Open bit
   private static final int VSPARE_OFF_CMD_BIT      = 15;  // VSPARE Off/Close bit
   private static final int VSPARE_STATUS_BIT       = 16;  // VSPARE Status bit
   private static final int VE1_OPEN_STATUS_BIT     = 17;  // VE1 Open Status bit
   private static final int VE1_CLOSE_STATUS_BIT    = 18;  // VE1 Close Status bit
   private static final int VE2_OPEN_STATUS_BIT     = 19;  // VE2 Open Status bit
   private static final int VE2_CLOSE_STATUS_BIT    = 20;  // VE2 Close Status bit
   private static final int COMPRESSAIR_STATUS_BIT  = 21;  // COMPRESSAIRST Status bit   
   private static final int ARD_RESET_BIT           = 31;  // Controllino Reset Bit

   public Controllino_2 (String _name,
                         int _mbRegisterStart, 
                         int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino_2:Controllino_2> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Valves
     addDataElement( new DataElement(name, "V24ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "VPST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VE1ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VE2ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     
     // VSpare
     addDataElement( new DataElement(name, "VSPAREST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // ByPass
     addDataElement( new DataElement(name, "BYPASSST", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     
     // Compress Air
     addDataElement( new DataElement(name, "COMPRESSAIRST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));     

     // Commands
     addDataElement( new DataElement(name, "V24CMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V25CMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VPCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "BYPASSONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VSPAREONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

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

     DataElement vent = getDataElement("V24ST");
     DataElement ve1 = getDataElement("VE1ST");
     DataElement ve2 = getDataElement("VE2ST");
     DataElement vp = getDataElement("VPST");
     DataElement vspare = getDataElement("VSPAREST");
     DataElement bypass = getDataElement("BYPASSST");
     DataElement air = getDataElement("COMPRESSAIRST");
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
        
        if (bitRead(i2c_buffer,VENT_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VENT_OPEN_STATUS_BIT) == 0x00)
           vent.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,VENT_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VENT_CLOSE_STATUS_BIT) == 0x00) 
           vent.value = 1; // VALVE OPEN
        else
           vent.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer,VE1_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VE1_OPEN_STATUS_BIT) == 0x00)
           ve1.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,VE1_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VE1_CLOSE_STATUS_BIT) == 0x00) 
           ve1.value = 1; // VALVE OPEN
        else
           ve1.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,VE2_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VE2_OPEN_STATUS_BIT) == 0x00)
           ve2.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,VE2_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VE2_CLOSE_STATUS_BIT) == 0x00) 
           ve2.value = 1; // VALVE OPEN
        else
           ve2.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,BYPASS_OFF_STATUS_BIT) == 0x00 && bitRead(i2c_buffer,BYPASS_ON_STATUS_BIT) == 0x01)
           bypass.value = 2; // BYPASS OFF
        else if (bitRead(i2c_buffer,BYPASS_ON_STATUS_BIT) == 0x00 && bitRead(i2c_buffer,BYPASS_OFF_STATUS_BIT) == 0x01) 
           bypass.value = 1; // BYPASS ON
        else
           bypass.value = 0; // BYPASS ERROR

        if (bitRead(i2c_buffer,VP_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VP_OPEN_STATUS_BIT) == 0x00)
           vp.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,VP_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VP_CLOSE_STATUS_BIT) == 0x00) 
           vp.value = 1; // VALVE OPEN
        else
           vp.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,VSPARE_STATUS_BIT) == 0x01)
           vspare.value = 1; // VSPARE ON/OPEN
        else if (bitRead(i2c_buffer,VSPARE_STATUS_BIT) == 0x00)
           vspare.value = 2; // VSPARE OFF/CLOSE

        if (bitRead(i2c_buffer,COMPRESSAIR_STATUS_BIT) == 0x01)
           air.value = 1; // COMPRESSAIR KO
        else if (bitRead(i2c_buffer,COMPRESSAIR_STATUS_BIT) == 0x00)
           air.value = 0; // COMPRESSAIR OK     

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
                  
         logger.finer("Controllino_2:executeCommand> reading i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
 
         if ( e.name.contains("V24CMD") ) { // Valve VENT Open/Close command
            if ( e.value == 1 ) // Open Valve
              i2c_buffer = setBit(i2c_buffer,VENT_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,VENT_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V25CMD") ) { // Valve VENTSOFT Open/Close command
            logger.finer("Controllino_2:executeCommand> set bit VENTSOFT =" + e.value);
            if ( e.value == 1 ) // Open Valve
              i2c_buffer = setBit(i2c_buffer,VENTSOFT_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,VENTSOFT_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VPCMD") ) { // Valve VP Open/Close command
            logger.finer("Controllino_2:executeCommand> set bit VP =" + e.value);
            if ( e.value == 1 ) // Open Valve
              i2c_buffer = setBit(i2c_buffer,VP_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,VP_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VSPAREONOFF") ) { // VSPARE On/Off command
            logger.finer("Controllino_2:executeCommand> set bit VSPARE =" + e.value);
            if ( e.value == 1 ) // Switch VSPARE On
              i2c_buffer = setBit(i2c_buffer,VSPARE_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch VSPARE Off
              i2c_buffer = clearBit(i2c_buffer,VSPARE_OFF_CMD_BIT);
         } 
         else if ( e.name.contains("BYPASSONOFF") ) { // BYPASS On/Off command
            logger.finer("Controllino_2:executeCommand> set bit BYPASS =" + e.value);
            if ( e.value == 1 ) // Switch BYPASS On
              i2c_buffer = setBit(i2c_buffer,BYPASS_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch BYPASS Off
              i2c_buffer = clearBit(i2c_buffer,BYPASS_OFF_CMD_BIT);
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
