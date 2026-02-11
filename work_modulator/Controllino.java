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

   private static final int MAIN_ON_CMD_BIT               =  0;   // MAIN ON command bit
   private static final int MAIN_OFF_CMD_BIT              =  1;   // MAIN OFF command bit
   private static final int MAIN_ON_STATUS_BIT            =  2;   // MAIN ON Status bit
   private static final int MAIN_OFF_STATUS_BIT           =  3;   // MAIN OFF Status bit
   private static final int NEGATIVE_VOLT_TRIG_BIT        =  4;   // NEGATIVE Volt Trigger bit
   private static final int POSITIVE_VOLT_TRIG_BIT        =  5;   // POSITIVE Volt Trigger bit
   private static final int FREQUENCY_TRIG_BIT            =  6;   // FREQUENCY Trigger bit
   private static final int DUTYCYCLE_TRIG_BIT            =  7;   // DUTY CYCLE Trigger bit
   private static final int ARD_RESET_BIT                 =  8;   // Controllino Reset Bit

   public Controllino (String _name,
                       int _mbRegisterStart, 
                       int i2c_addr) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("Controllino:Controllino> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Read only status
     addDataElement( new DataElement(name, "MAINST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));

     // Read only value
     addDataElement( new DataElement(name, "NEGATIVECUR", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "POSITIVECUR", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "UVVOLTAGE", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));

     // Triggers
     addDataElement( new DataElement(name, "MAINONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=2));

     // Read & Write values
     addDataElement( new DataElement(name, "NEGATIVEVOLT", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "POSITIVEVOLT", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "FREQUENCY", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "DUTYCYCLE", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));

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
     // Set interloop pause
     sleepTime = 100;
   }
   
   public void updateDeviceData() {
   
     // Get monitoring data from device using RS232 Comm

     DataElement main = getDataElement("MAINST");
     DataElement negativecur = getDataElement("NEGATIVECUR");
     DataElement positivecur = getDataElement("POSITIVECUR");
     DataElement uvvoltage = getDataElement("UVVOLTAGE");
     DataElement negv = getDataElement("NEGATIVEVOLT");
     DataElement posv = getDataElement("POSITIVEVOLT");
     DataElement freq  = getDataElement("FREQUENCY");
     DataElement duty  = getDataElement("DUTYCYCLE");
     DataElement dcom = getDataElement("COMST");
    
     byte[] serDataR = null;

     try {
        // Lock the bus during read/write command to insure correct multiple slave interaction
        while (busmutex.tryLock() == false) {
           try {
              Thread.sleep(10);
              logger.fine("CONTROLLINO: mutex locked");
           }
           catch (InterruptedException e) {}
        }
        try {
           popCommand();  // Execute commands in the loop is more reactive
           serDataR = i2c.Read28bytesPlusCRC32();
           int full_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 24)).getInt();
           logger.finer("Controllino:executeCommand> send full_buffer=" +      
                      String.format("%32s",Integer.toBinaryString(full_buffer)).replaceAll(" ", "0"));
        }
        catch (Exception ex) {
           if ( hasWarned == false ) {
              logger.log(Level.WARNING, "Controllino:updateDeviceData> Communication with " + name + " interrupted");
              logger.log(Level.SEVERE, "Controllino:updateDeviceData>" + ex.getMessage());
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
        // NEGATIVE CURRENT (FLOAT)
        int tmpbuf = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 0, 4)).getInt();
        negativecur.value = (double) Float.intBitsToFloat(tmpbuf);
        // POSITIVE CURRENT (FLOAT)
        tmpbuf = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 4, 8)).getInt();
        positivecur.value = (double) Float.intBitsToFloat(tmpbuf);
        // UV VOLTAGE (FLOAT)
        tmpbuf = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 8, 12)).getInt();
        uvvoltage.value = (double) Float.intBitsToFloat(tmpbuf);
        // NEGATIVE VOLTAGE VALUE (INT16)
        negv.value = (double) ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 12, 14)).getShort();
        // POSITIVE VOLTAGE VALUE (INT16)
        posv.value = (double) ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 14, 16)).getShort();
        // FREQUENCY VALUE (INT16)
        freq.value = (double) ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 16, 18)).getShort();
        // FREQUENCY VALUE (INT16)
        duty.value = (double) ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 18, 20)).getShort();
        // I2C_BUFFER (INT32)
        int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 20, 24)).getInt();

        logger.finer("Controllino:updateDeviceData> i2c_buffer=" + 
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
        
        if (bitRead(i2c_buffer,MAIN_ON_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,MAIN_OFF_STATUS_BIT) == 0x00)
           main.value = 1; // MODULATOR ON
        else if (bitRead(i2c_buffer,MAIN_OFF_STATUS_BIT) == 0x01 && bitRead(i2c_buffer,MAIN_ON_STATUS_BIT) == 0x00) 
           main.value = 2; // MODULATOR OFF
        else
           main.value = 0; // ERROR
        
        
        dcom.value = 0; // if arriving here COM OK
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("Controllino:updateDeviceData> Communication with " + name + " back!");
        }
        comm_cnt_failure = 0;
     }
     catch (Exception ex) {
        ex.printStackTrace();
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "Controllino:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "Controllino:updateDeviceData>" + ex.getMessage());
        }
        if (++comm_cnt_failure > comm_max_failure) {
           setErrorComStatus();
        }
     }
   }
   
   public void executeCommand( DataElement e ) {
      
      try {
         // Read the (old) i2c buffer from the bus
         byte[] serDataR = i2c.Read28bytesPlusCRC32();
         if (serDataR == null) {
           logger.log(Level.WARNING,"Controllino_3:executeCommand> NO COMMAND SENT! (data=null)");
           return;
         }
         int i2c_buffer = ByteBuffer.wrap(Arrays.copyOfRange(serDataR, 20, 24)).getInt();
         byte[] value = new byte[]{0,0};

         logger.finer("Controllino:executeCommand> reading i2c_buffer=" +      
                     String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
 
         if ( e.name.contains("MAINONOFF") ) { // MAIN ON/OFF command
            if ( e.value == 1 )    // Switch ON
              i2c_buffer = setBit(i2c_buffer,MAIN_ON_CMD_BIT);
            else if ( e.value == 2 )  // Switch OFF
              i2c_buffer = clearBit(i2c_buffer,MAIN_OFF_CMD_BIT);
         }
         else if ( e.name.contains("NEGATIVEVOLT") ) { // Set value NEGATIVE VOLTAGE command
            short tmp = (short) e.setvalue;
            value[0] = (byte)(( tmp  >> 8) & 0xFF);
            value[1] = (byte)(tmp & 0xFF);
            // Set the trigger bit
            i2c_buffer = setBit(i2c_buffer,NEGATIVE_VOLT_TRIG_BIT);
         }
         else if ( e.name.contains("POSITIVEVOLT") ) { // Set value POSITIVE VOLTAGE command
            short tmp = (short) e.setvalue;
            value[0] = (byte)(( tmp  >> 8) & 0xFF);
            value[1] = (byte)(tmp & 0xFF);
            // Set the trigger bit
            i2c_buffer = setBit(i2c_buffer,POSITIVE_VOLT_TRIG_BIT);
         }
         else if ( e.name.contains("FREQUENCY") ) { // Set value FREQUENCY command
            short tmp = (short) e.setvalue;
            value[0] = (byte)(( tmp  >> 8) & 0xFF);
            value[1] = (byte)(tmp & 0xFF);
            // Set the trigger bit
            i2c_buffer = setBit(i2c_buffer,FREQUENCY_TRIG_BIT);
         }
         else if ( e.name.contains("DUTYCYCLE") ) { // Set value DUTY CYCLE command
            short tmp = (short) e.setvalue;
            value[0] = (byte)(( tmp  >> 8) & 0xFF);
            value[1] = (byte)(tmp & 0xFF);
            // Set the trigger bit
            i2c_buffer = setBit(i2c_buffer,DUTYCYCLE_TRIG_BIT);
         }
         logger.finer("Controllino:executeCommand> send i2c_buffer=" +      
                      String.format("%32s",Integer.toBinaryString(i2c_buffer)).replaceAll(" ", "0"));
         // Write the i2c_buf on the bus
         byte[] i2cbuf = new byte[]{0,0,0,0,0,0};
         i2cbuf[0] = (byte)(( i2c_buffer >> 24) & 0xFF);
         i2cbuf[1] = (byte)(( i2c_buffer >> 16) & 0xFF);
         i2cbuf[2] = (byte)(( i2c_buffer >> 8)  & 0xFF);
         i2cbuf[3] = (byte)( (i2c_buffer >> 0 ) & 0xFF);
         i2cbuf[4] = value[0];
         i2cbuf[5] = value[1];
         for (int i = 0 ; i < 6 ; i++)
           logger.finer("SENDING BYTE BUFFER:" + String.format("%64s",Integer.toBinaryString(i2cbuf[i])).replaceAll(" ", "0"));  
         i2c.WritePlusCRC32(i2cbuf);
         if (e.type == DataType.TRIGGER) {
            Thread.sleep(2000); // Wait before resetting
            e.value = 0;
            holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "Controllino:executeCommand>" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "Controllino:executeCommand> Communication with " + name + " interrupted");
     }
   }
    
   private byte[] intToBytes( final int i ) {
    ByteBuffer bb = ByteBuffer.allocate(4); 
    bb.putInt(i); 
    return bb.array();
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
