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

public class Controllino_1 extends Device {

   private I2C_Comm i2c;
   private static final Logger logger = Logger.getLogger("Main");

   private static final int VBYPASS_OPEN_CMD_BIT       = 0;   // VBYPASS Open bit
   private static final int VBYPASS_CLOSE_CMD_BIT      = 1;   // VBYPASS Close bit
   private static final int VBYPASS_OPEN_STATUS_BIT    = 2;   // VBYPASS Open Status bit
   private static final int VBYPASS_CLOSE_STATUS_BIT   = 3;   // VBYPASS Close Status bit
   private static final int VRP_OPEN_CMD_BIT           = 4;   // VRP Open bit
   private static final int VRP_CLOSE_CMD_BIT          = 5;   // VRP Close bit
   private static final int VRP_OPEN_STATUS_BIT        = 6;   // VRP Open Status bit
   private static final int VRP_CLOSE_STATUS_BIT       = 7;   // VRP Close Status bit
   private static final int VDRYER_OPEN_CMD_BIT        = 8;   // VDRYER Open bit
   private static final int VDRYER_CLOSE_CMD_BIT       = 9;   // VDRYER Close bit
   private static final int VDRYER_OPEN_STATUS_BIT     = 10;  // VDRYER Open Status bit
   private static final int VDRYER_CLOSE_STATUS_BIT    = 11;  // VDRYER Close Status bit
   private static final int V1_OPEN_CMD_BIT            = 12;  // V1 Open bit
   private static final int V1_CLOSE_CMD_BIT           = 13;  // V1 Close bit
   private static final int V1_OPEN_STATUS_BIT         = 14;  // V1 Open Status bit
   private static final int V1_CLOSE_STATUS_BIT        = 15;  // V1 Close Status bit
   private static final int VMAIN_OPEN_CMD_BIT         = 16;  // VMAIN Open bit
   private static final int VMAIN_CLOSE_CMD_BIT        = 17;  // VMAIN Close bit
   private static final int VENT_OPEN_STATUS_BIT       = 18;  // VENT Open Status bit
   private static final int VENT_CLOSE_STATUS_BIT      = 19;  // VENT Close Status bit
   private static final int VSOFT_OPEN_CMD_BIT         = 20;  // VSOFT Open bit
   private static final int VSOFT_CLOSE_CMD_BIT        = 21;  // VSOFT Close bit
   private static final int P1_ON_CMD_BIT              = 22;  // P1 On bit
   private static final int P1_OFF_CMD_BIT             = 23;  // P1 Off bit
   private static final int P1_STATUS_BIT              = 24;  // P1 Status bit
   
   private static final int ARD_RESET_BIT              = 31;  // Controllino Reset Bit

   public Controllino_1 (String _name,
                         int _mbRegisterStart, 
                         int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino_1:Controllino_1> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Valves
     addDataElement( new DataElement(name, "VBYPASSST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "VRPST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VDRYERST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V1ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VENTST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Pumps
     addDataElement( new DataElement(name, "P1ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Commands
     addDataElement( new DataElement(name, "VBYPASSCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VRPCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VDRYERCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "V1CMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VMAINCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VSOFTCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P1ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     

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

     DataElement vbypass = getDataElement("VBYPASSST");
     DataElement vrp = getDataElement("VRPST");
     DataElement vdryer = getDataElement("VDRYERST");
     DataElement vent = getDataElement("VENTST");
     DataElement v1  = getDataElement("V1ST");
     DataElement p1 = getDataElement("P1ST");
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
           // Release master, unlock
           busmutex.unlock();
        }
        if (serDataR == null ) {
           if (++comm_cnt_failure > comm_max_failure)  setErrorComStatus();
           return;
        }
        int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();

        logger.finer("Controllino_1:updateDeviceData> i2c_buffer=" + 
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
        
        if (bitRead(i2c_buffer,VBYPASS_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VBYPASS_OPEN_STATUS_BIT) == 0x00)
           vbypass.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,VBYPASS_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VBYPASS_CLOSE_STATUS_BIT) == 0x00) 
           vbypass.value = 1; // VALVE OPEN
        else
           vbypass.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer,VRP_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VRP_OPEN_STATUS_BIT) == 0x00)
           vrp.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,VRP_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VRP_CLOSE_STATUS_BIT) == 0x00) 
           vrp.value = 1; // VALVE OPEN
        else
           vrp.value = 0; // VALVE MOVING
           
        if (bitRead(i2c_buffer,VDRYER_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VDRYER_OPEN_STATUS_BIT) == 0x00)
           vdryer.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,VDRYER_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VDRYER_CLOSE_STATUS_BIT) == 0x00) 
           vdryer.value = 1; // VALVE OPEN
        else
           vdryer.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,V1_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V1_OPEN_STATUS_BIT) == 0x00)
           v1.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,V1_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,V1_CLOSE_STATUS_BIT) == 0x00) 
           v1.value = 1; // VALVE OPEN
        else
           v1.value = 0; // VALVE MOVING
           
        if (bitRead(i2c_buffer,VENT_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VENT_OPEN_STATUS_BIT) == 0x00)
           vent.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer,VENT_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,VENT_CLOSE_STATUS_BIT) == 0x00) 
           vent.value = 1; // VALVE OPEN
        else
           vent.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer,P1_STATUS_BIT) == 0x00)
           p1.value = 1; // SCROLL ON
        else if (bitRead(i2c_buffer,P1_STATUS_BIT) == 0x01)
           p1.value = 0; // SCROLL OFF

        dcom.value = 0; // if arriving here COM OK
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("Controllino_1:updateDeviceData> Communication with " + name + " back!");
        }
        comm_cnt_failure = 0;
     }
     catch (Exception ex) {
        ex.printStackTrace();
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
         int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();
                 
         logger.finer("Controllino_1:executeCommand> reading i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
 
         if ( e.name.contains("VBYPASSCMD") ) { // Valve VBYPASS Open/Close command
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,VBYPASS_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,VBYPASS_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VRPCMD") ) { // Valve VRP Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit VRP =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,VRP_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,VRP_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VDRYERCMD") ) { // Valve VDRYER Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit VDRYER =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,VDRYER_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,VDRYER_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("V1CMD") ) { // Valve V1 Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit V1 =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,V1_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,V1_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VMAINCMD") ) { // Valve VMAIN Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit VMAIN =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,VMAIN_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,VMAIN_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VSOFTCMD") ) { // Valve VSOFT Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit VSOFT =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer = setBit(i2c_buffer,VSOFT_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer = clearBit(i2c_buffer,VSOFT_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("P1ONOFF") ) { // Scroll P1 On/Off command
            logger.finer("Controllino_1:executeCommand> set bit P1 =" + e.value);
            if ( e.value == 1 )      // Switch P1 On
              i2c_buffer = setBit(i2c_buffer,P1_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch P1 Off
              i2c_buffer = clearBit(i2c_buffer,P1_OFF_CMD_BIT);
         }       
         logger.fine("Controllino_1:executeCommand> send i2c_buffer=" +      
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
