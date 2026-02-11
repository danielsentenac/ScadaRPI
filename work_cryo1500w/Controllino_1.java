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

   private static final int VPO_OPEN_CMD_BIT       = 0;   // VPO Open bit
   private static final int VPO_CLOSE_CMD_BIT      = 1;   // VPO Close bit
   private static final int VPO_OPEN_STATUS_BIT    = 2;   // VPO Open Status bit
   private static final int VPO_CLOSE_STATUS_BIT   = 3;   // VPO Close Status bit
   private static final int VPI_OPEN_CMD_BIT       = 4;   // VPI Open bit
   private static final int VPI_CLOSE_CMD_BIT      = 5;   // VPI Close bit
   private static final int VPI_OPEN_STATUS_BIT    = 6;   // VPI Open Status bit
   private static final int VPI_CLOSE_STATUS_BIT   = 7;   // VPI Close Status bit
   private static final int VP1_OPEN_CMD_BIT        = 8;  // VP1 Open bit
   private static final int VP1_CLOSE_CMD_BIT       = 9;  // VP1 Close bit
   private static final int VP1_OPEN_STATUS_BIT     = 10; // VP1 Open Status bit
   private static final int VP1_CLOSE_STATUS_BIT    = 11; // VP1 Close Status bit
   private static final int VO_OPEN_CMD_BIT         = 12; // VO Open bit
   private static final int VO_CLOSE_CMD_BIT        = 13; // VO Close bit
   private static final int VO_OPEN_STATUS_BIT      = 14; // VO Open Status bit
   private static final int VO_CLOSE_STATUS_BIT     = 15; // VO Close Status bit
   private static final int VI_OPEN_CMD_BIT         = 16; // VI Open bit
   private static final int VI_CLOSE_CMD_BIT        = 17; // VI Close bit
   private static final int VI_OPEN_STATUS_BIT      = 18; // VI Open Status bit
   private static final int VI_CLOSE_STATUS_BIT     = 19; // VI Close Status bit
   private static final int VBO_OPEN_CMD_BIT        = 20; // VBO Open bit
   private static final int VBO_CLOSE_CMD_BIT       = 21; // VBO Close bit
   private static final int VBO_OPEN_STATUS_BIT     = 22; // VBO Open Status bit
   private static final int VBO_CLOSE_STATUS_BIT    = 23; // VBO Close Status bit
   private static final int VBI_OPEN_CMD_BIT        = 24; // VBI Open bit
   private static final int VBI_CLOSE_CMD_BIT       = 25; // VBI Close bit
   private static final int VBI_OPEN_STATUS_BIT     = 26; // VBI Open Status bit
   private static final int VBI_CLOSE_STATUS_BIT    = 27; // VBI Close Status bit
   private static final int VVENTING_OPEN_CMD_BIT      = 28; // VVENTING Open bit
   private static final int VVENTING_CLOSE_CMD_BIT     = 29; // VVENTING Close bit
   private static final int VVENTING_OPEN_STATUS_BIT   = 30; // VVENTING Open Status bit
   private static final int VVENTING_CLOSE_STATUS_BIT  = 31; // VVENTING Close Status bit
   
   private static final int VGAS_OPEN_CMD_BIT       = 0; // VGAS Open bit
   private static final int VGAS_CLOSE_CMD_BIT      = 1; // VGAS Close bit
   private static final int VGAS_OPEN_STATUS_BIT    = 2; // VGAS Open Status bit
   private static final int VGAS_CLOSE_STATUS_BIT   = 3; // VGAS Close Status bit
   private static final int BYPASS_ON_CMD_BIT       = 4;  // BYPASS On bit
   private static final int BYPASS_OFF_CMD_BIT      = 5;  // BYPASS Off bit
   private static final int BYPASS_STATUS_BIT       = 6;  // BYPASS On Status bit
   private static final int P1_ON_CMD_BIT           = 7;  // P1 On bit
   private static final int P1_OFF_CMD_BIT          = 8;  // P1 Off bit
   private static final int P1_STATUS_BIT           = 9;  // P1 Status bit
   private static final int COMPRESSAIR_STATUS_BIT  = 10;  // COMPRESSAIR Status bit
   private static final int ARD_RESET_BIT           = 31;  // Controllino Reset Bit

   public Controllino_1 (String _name,
                         int _mbRegisterStart, 
                         int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino_1:Controllino_1> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Valves
     addDataElement( new DataElement(name, "VPOST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "VPIST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VP1ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VOST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VIST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VBOST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VBIST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VVENTINGST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VGASST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     // ByPass
     addDataElement( new DataElement(name, "BYPASSST", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));

     // Compress Air
     addDataElement( new DataElement(name, "COMPRESSAIRST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Pumps
     addDataElement( new DataElement(name, "P1ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Commands
     addDataElement( new DataElement(name, "VPOCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VPICMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VP1CMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VOCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VICMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VBOCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VBICMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VVENTINGCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VGASCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P1ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
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

     DataElement vPO = getDataElement("VPOST");
     DataElement vPI = getDataElement("VPIST");     
     DataElement vP1  = getDataElement("VP1ST");
     DataElement vO = getDataElement("VOST");
     DataElement vI = getDataElement("VIST");
     DataElement vBO = getDataElement("VBOST");
     DataElement vBI = getDataElement("VBIST");
     DataElement VVENTING = getDataElement("VVENTINGST");
     DataElement vGAS = getDataElement("VGASST");
     DataElement bypass  = getDataElement("BYPASSST");
     DataElement air = getDataElement("COMPRESSAIRST");
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
           serDataR = i2c.Read8bytesPlusCRC32();
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
        logger.finer("Controllino_1:updateDeviceData> serDataR = " + serDataR + " size serDataR = " + serDataR.length);
        int i2c_buffer1 = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();
        int i2c_buffer2 = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 4, 8)).getInt();
        
        logger.finer("Controllino_1:updateDeviceData> i2c_buffer1=" + 
                     String.format("%32s",Integer.toBinaryString(i2c_buffer1)).replaceAll(" ", "0"));
        logger.finer("Controllino_1:updateDeviceData> i2c_buffer2=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer2)).replaceAll(" ", "0"));
        
        if (bitRead(i2c_buffer1,VPO_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VPO_OPEN_STATUS_BIT) == 0x00)
           vPO.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer1,VPO_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VPO_CLOSE_STATUS_BIT) == 0x00) 
           vPO.value = 1; // VALVE OPEN
        else
           vPO.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer1,VPI_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VPI_OPEN_STATUS_BIT) == 0x00)
           vPI.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer1,VPI_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VPI_CLOSE_STATUS_BIT) == 0x00) 
           vPI.value = 1; // VALVE OPEN
        else
           vPI.value = 0; // VALVE MOVING
           
        if (bitRead(i2c_buffer1,VP1_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VP1_OPEN_STATUS_BIT) == 0x00)
           vP1.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer1,VP1_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VP1_CLOSE_STATUS_BIT) == 0x00) 
           vP1.value = 1; // VALVE OPEN
        else
           vP1.value = 0; // VALVE MOVING
           
        if (bitRead(i2c_buffer1,VO_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VO_OPEN_STATUS_BIT) == 0x00)
           vO.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer1,VO_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VO_CLOSE_STATUS_BIT) == 0x00) 
           vO.value = 1; // VALVE OPEN
        else
           vO.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer1,VI_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VI_OPEN_STATUS_BIT) == 0x00)
           vI.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer1,VI_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VI_CLOSE_STATUS_BIT) == 0x00) 
           vI.value = 1; // VALVE OPEN
        else
           vI.value = 0; // VALVE MOVING
           
        if (bitRead(i2c_buffer1,VBO_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VBO_OPEN_STATUS_BIT) == 0x00)
           vBO.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer1,VBO_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VBO_CLOSE_STATUS_BIT) == 0x00) 
           vBO.value = 1; // VALVE OPEN
        else
           vBO.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer1,VBI_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VBI_OPEN_STATUS_BIT) == 0x00)
           vBI.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer1,VBI_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VBI_CLOSE_STATUS_BIT) == 0x00) 
           vBI.value = 1; // VALVE OPEN
        else
           vBI.value = 0; // VALVE MOVING
           
        if (bitRead(i2c_buffer1,VVENTING_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VVENTING_OPEN_STATUS_BIT) == 0x00)
           VVENTING.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer1,VVENTING_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer1,VVENTING_CLOSE_STATUS_BIT) == 0x00) 
           VVENTING.value = 1; // VALVE OPEN
        else
           VVENTING.value = 0; // VALVE MOVING
        
        if (bitRead(i2c_buffer2,VGAS_CLOSE_STATUS_BIT) == 0x01 && bitRead(i2c_buffer2,VGAS_OPEN_STATUS_BIT) == 0x00)
           vGAS.value = 2; // VALVE CLOSED
        else if (bitRead(i2c_buffer2,VGAS_OPEN_STATUS_BIT) == 0x01 && bitRead(i2c_buffer2,VGAS_CLOSE_STATUS_BIT) == 0x00) 
           vGAS.value = 1; // VALVE OPEN
        else
           vGAS.value = 0; // VALVE MOVING

        if (bitRead(i2c_buffer2,BYPASS_STATUS_BIT) == 0x01)
           bypass.value = 1; // BYPASS ON
        else if (bitRead(i2c_buffer2,BYPASS_STATUS_BIT) == 0x00) 
           bypass.value = 0; // BYPASS OFF

        if (bitRead(i2c_buffer2,P1_STATUS_BIT) == 0x01)
           p1.value = 1; // SCROLL ON
        else if (bitRead(i2c_buffer2,P1_STATUS_BIT) == 0x00)
           p1.value = 0; // SCROLL OFF

        if (bitRead(i2c_buffer2,COMPRESSAIR_STATUS_BIT) == 0x01)
           air.value = 1; // COMPRESSAIR KO
        else if (bitRead(i2c_buffer2,COMPRESSAIR_STATUS_BIT) == 0x00)
           air.value = 0; // COMPRESSAIR OK    

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
         byte[] serDataR = i2c.Read8bytesPlusCRC32();
         if (serDataR == null) {
           logger.log(Level.WARNING,"Controllino_3:executeCommand> NO COMMAND SENT! (data=null)");
           return;
         }
         int i2c_buffer1 = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();
         int i2c_buffer2 = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 4, 8)).getInt();
               
         logger.fine("Controllino_1:executeCommand> reading i2c_buffer1=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer1)).replaceAll(" ", "0"));
         logger.fine("Controllino_1:executeCommand> reading i2c_buffer2=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer2)).replaceAll(" ", "0"));
 
         if ( e.name.contains("VPOCMD") ) { // Valve VPO Open/Close command
            if ( e.value == 1 )      // Open Valve
              i2c_buffer1 = setBit(i2c_buffer1,VPO_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer1 = clearBit(i2c_buffer1,VPO_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VPICMD") ) { // Valve VPI Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit VPI =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer1 = setBit(i2c_buffer1,VPI_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer1 = clearBit(i2c_buffer1,VPI_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VP1CMD") ) { // Valve VP1 Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit VP1 =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer1 = setBit(i2c_buffer1,VP1_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer1 = clearBit(i2c_buffer1,VP1_CLOSE_CMD_BIT);
         }
         if ( e.name.contains("VOCMD") ) { // Valve VO Open/Close command
            if ( e.value == 1 )      // Open Valve
              i2c_buffer1 = setBit(i2c_buffer1,VO_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer1 = clearBit(i2c_buffer1,VO_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VICMD") ) { // Valve VI Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit VI =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer1 = setBit(i2c_buffer1,VI_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer1 = clearBit(i2c_buffer1,VI_CLOSE_CMD_BIT);
         }
         if ( e.name.contains("VBOCMD") ) { // Valve VBO Open/Close command
            if ( e.value == 1 )      // Open Valve
              i2c_buffer1 = setBit(i2c_buffer1,VBO_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer1 = clearBit(i2c_buffer1,VBO_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VBICMD") ) { // Valve VBI Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit VBI =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer1 = setBit(i2c_buffer1,VBI_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer1 = clearBit(i2c_buffer1,VBI_CLOSE_CMD_BIT);
         }
         if ( e.name.contains("VVENTINGCMD") ) { // Valve VVENTING Open/Close command
            if ( e.value == 1 )      // Open Valve
              i2c_buffer1 = setBit(i2c_buffer1,VVENTING_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer1 = clearBit(i2c_buffer1,VVENTING_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("VGASCMD") ) { // Valve VGAS Open/Close command
            logger.finer("Controllino_1:executeCommand> set bit VGAS =" + e.value);
            if ( e.value == 1 )      // Open Valve
              i2c_buffer2 = setBit(i2c_buffer2,VGAS_OPEN_CMD_BIT);
            else if ( e.value == 2 ) // Close Valve
              i2c_buffer2 = clearBit(i2c_buffer2,VGAS_CLOSE_CMD_BIT);
         }
         else if ( e.name.contains("P1ONOFF") ) { // Scroll P1 On/Off command
            logger.fine("Controllino_1:executeCommand> set bit P1 =" + e.value);
            if ( e.value == 1 )  {    // Switch P1 On
               logger.fine("Controllino_1:executeCommand> CAZZO i2c_buffer2=" +      
                      String.format("%32s",Integer.toBinaryString(i2c_buffer2)).replaceAll(" ", "0"));
              i2c_buffer2 = setBit(i2c_buffer2,P1_ON_CMD_BIT);
               logger.fine("Controllino_1:executeCommand> CAZZO i2c_buffer2=" +      
                      String.format("%32s",Integer.toBinaryString(i2c_buffer2)).replaceAll(" ", "0"));
              }
            else if ( e.value == 2 ) // Switch P1 Off
              i2c_buffer2 = clearBit(i2c_buffer2,P1_OFF_CMD_BIT);
         } 
         else if ( e.name.contains("BYPASSONOFF") ) { // BYPASS On/Off command
            logger.finer("Controllino_1:executeCommand> set bit BYPASS =" + e.value);
            if ( e.value == 1 )      // Switch BYPASS On
              i2c_buffer2 = setBit(i2c_buffer2,BYPASS_ON_CMD_BIT);
            else if ( e.value == 2 ) // Switch  BYPASS Off
              i2c_buffer2 = clearBit(i2c_buffer2,BYPASS_OFF_CMD_BIT);
         } 
      
         logger.fine("Controllino_1:executeCommand> send i2c_buffer1=" +      
                      String.format("%32s",Integer.toBinaryString(i2c_buffer1)).replaceAll(" ", "0"));
         logger.fine("Controllino_1:executeCommand> send i2c_buffer2=" +      
                      String.format("%32s",Integer.toBinaryString(i2c_buffer2)).replaceAll(" ", "0"));
         // Write the (new) i2c_buffer1 & i2c_buffer2 on the bus
         ByteBuffer buffer = ByteBuffer.allocate(8);
         buffer.putInt(i2c_buffer1);
         buffer.putInt(4,i2c_buffer2);
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
