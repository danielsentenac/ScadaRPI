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

public class Controllino_3 extends Device {

   private I2C_Comm i2c;
   private static final Logger logger = Logger.getLogger("Main");

   private static final int NORMAL_SPEED_OPEN_CMD_BIT   = 0;   // NORMAL SPEED Open bit
   private static final int NORMAL_SPEED_CLOSE_CMD_BIT  = 1;   // NORMAL SPEED Close bit
   private static final int LOW_NOISE_OPEN_CMD_BIT      = 2;   // LOW NOISE Open bit
   private static final int LOW_NOISE_CLOSE_CMD_BIT     = 3;   // LOW NOISE Close bit
   private static final int FAN_START_CMD_BIT           = 4;   // FAN_START Open bit
   private static final int FAN_STOP_CMD_BIT            = 5;   // FAN_START Close bit
   private static final int FAN_START_STATUS_BIT        = 6;   // FAN START Status bit
   private static final int FAN_STOP_STATUS_BIT         = 7;   // FAN STOP Status bit
   private static final int ARD_RESET_BIT               = 8;   // Controllino Reset Bit

   public Controllino_3 (String _name,
                         int _mbRegisterStart, 
                         int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino_3:Controllino_3> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Status
     addDataElement( new DataElement(name, "FANST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
  
     // Value
     addDataElement( new DataElement(name, "TEMP", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));

     // Commands
     addDataElement( new DataElement(name, "FANSPEED", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "FANONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     //addDataElement( new DataElement(name, "RESET3", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

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
     // Set using i2c_out gpio
     use_i2c_out = true;
   }
   
   public void updateDeviceData() {
   
     // Get monitoring data from device using RS232 Comm

     DataElement fanst = getDataElement("FANST");
     DataElement temp = getDataElement("TEMP");
     DataElement fanspeed = getDataElement("FANSPEED");
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
           // Wait for availability of the master (multiple master case)
           i2c_in = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04);
           while (i2c_in.isHigh()) {
              try {
                 Thread.sleep(10);
                 logger.finer("CONTROLLINO 3: mutex locked");
              }
              catch (InterruptedException e) { 
              }
           }
           // Master available, lock it
           try {
              i2c_out = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03);
              i2c_out.high();
              logger.finer("Controllino_3:updateDeviceData> MASTER LOCKED !!");
              Thread.sleep(100);
              
           }
           catch (InterruptedException e) {
           }
        }
        finally {
           gpio.unprovisionPin(i2c_in);
           popCommand();  // Execute commands in the loop is more reactive
           serDataR = i2c.Read8bytes(); // Expect 8 bytes from Module 3
           // Release master, unlock it
           i2c_out.low();
           gpio.unprovisionPin(i2c_out);
           busmutex.unlock();
           logger.finer("Controllino_3:updateDeviceData> MASTER UNLOCKED !!");
        }
        logger.finer("Controllino_3:updateDeviceData> TREATING DATA !!!!!!!");
        int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 4, 8)).getInt();
        logger.finer("Controllino_3:updateDeviceData> i2c_buffer =" +  i2c_buffer);
        logger.finer("Controllino_3:updateDeviceData> i2c_buffer=" + 
        String.format("%8s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
        
        if (bitRead(i2c_buffer,NORMAL_SPEED_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,NORMAL_SPEED_CLOSE_CMD_BIT) == 0x01 &&
            bitRead(i2c_buffer,LOW_NOISE_OPEN_CMD_BIT) == 0x00 && bitRead(i2c_buffer,LOW_NOISE_CLOSE_CMD_BIT) == 0x00)
           fanspeed.value = 1; // FAN NORMAL SPEED
        else if (bitRead(i2c_buffer,LOW_NOISE_OPEN_CMD_BIT) == 0x01 && bitRead(i2c_buffer,LOW_NOISE_CLOSE_CMD_BIT) == 0x01 &&
                 bitRead(i2c_buffer,NORMAL_SPEED_OPEN_CMD_BIT) == 0x00 && bitRead(i2c_buffer,NORMAL_SPEED_CLOSE_CMD_BIT) == 0x00) 
           fanspeed.value = 2; // FAN LOW NOISE
        else
           fanspeed.value = 0; // FAN SPEED in progress..

        if (bitRead(i2c_buffer,FAN_START_STATUS_BIT) == 0x00 && bitRead(i2c_buffer,FAN_STOP_STATUS_BIT) == 0x01)
           fanst.value = 2;    // FAN OFF
        else if (bitRead(i2c_buffer,FAN_STOP_STATUS_BIT) == 0x00 && bitRead(i2c_buffer,FAN_START_STATUS_BIT) == 0x01) 
           fanst.value = 1;    // FAN ON
        else
           fanst.value = 0;    // FAN STATUS ERROR
        
        temp.value = (double) ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getFloat();
        logger.finer("Controllino_3:updateDeviceData> temperature= " + String.valueOf(temp.value));
        dcom.value = 0; // if arriving here COM OK
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("Controllino_3:updateDeviceData> Communication with " + name + " back!");
        }
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "Controllino_3:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "Controllino_3:updateDeviceData>" + ex.getMessage());
        }
        setErrorComStatus();
     }
   }
   
   public void executeCommand( DataElement e ) {
       
      try {
         // Read the (old) i2c buffer from the bus
         byte[] serDataR = i2c.Read8bytes();
         int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 4, 8)).getInt();
         logger.finer("Controllino_3:executeCommand> reading i2c_buffer=" +      
                     String.format("%8s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
 
         if ( e.name.contains("FANSPEED") ) { // Fan speed command
            if ( e.setvalue == 1 ) { // Normal speed
              logger.finer("Controllino_3:executeCommand> SET NORMAL SPEED");
              i2c_buffer = clearBit(i2c_buffer,LOW_NOISE_OPEN_CMD_BIT);
            }
            else if ( e.setvalue == 2 ) { // Low noise
              i2c_buffer = clearBit(i2c_buffer,NORMAL_SPEED_CLOSE_CMD_BIT);
            }
         }
         else if ( e.name.contains("FANONOFF") ) { // Fan Start/Stop command
            if ( e.value == 1 ) // Start Fan
              i2c_buffer = setBit(i2c_buffer,FAN_START_CMD_BIT);
            else if ( e.value == 2 ) // Stop Fan
              i2c_buffer = clearBit(i2c_buffer,FAN_STOP_CMD_BIT);
         }
         else if ( e.name.contains("RESET3") ) { // RESET Controllino 3 command
            if ( e.value == 1 ) // Rest Controllino 3
              i2c_buffer = setBit(i2c_buffer,ARD_RESET_BIT);
         }
      
         logger.finer("Controllino_3:executeCommand> send i2c_buffer=" +      
                      String.format("%8s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
         // Write the (new) i2c_buffer on the bus
         ByteBuffer buffer = ByteBuffer.allocate(4);
         buffer.putInt(i2c_buffer);
         byte[] msg = buffer.array();   
         i2c.Write(msg);
         if (e.type == DataType.TRIGGER) {
            Thread.sleep(2000); // Wait before resetting
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
