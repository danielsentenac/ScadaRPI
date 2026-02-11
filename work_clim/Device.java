/*
 * This Abstract Class is used to deinfo general device functionalities
 *
 */
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.pi4j.io.gpio.*;

public abstract class Device extends DataManager implements Runnable, DataTypes {

   private static final Logger logger = Logger.getLogger("Main");

   public String name;
   
   public int delay;

   protected boolean comErr = false;

   public final Lock mutex = new ReentrantLock(true);

   protected Thread thread;
  
   public LinkedList<DataElement> commandSetQueue = new LinkedList<DataElement>();

   public abstract void updateDeviceData();

   public abstract void executeCommand(DataElement e);

   protected static final GpioController gpio = GpioFactory.getInstance();

   public void doStart() {
      thread = new Thread(this);
      thread.start();
   }

   public void doStop() {
        if (thread != null) thread.interrupt();
        // Change the states of variable
        thread = null;
   }

   public void popCommand() {

      // check commandSet queue
      DataElement dataElement = null;
      try {
         if (!commandSetQueue.isEmpty()) {
            dataElement = commandSetQueue.pop();
            if (dataElement != null) {
               if (dataElement.type == DataType.TRIGGER)    // Set value if is a command trigger only
                  dataElement.value = dataElement.setvalue;
               logger.finer("Device:popCommand> Device " + this.name + ": Execute command/Write " 
                           + dataElement.name + " setvalue " + dataElement.setvalue);
               logger.finer("Device:popCommand> Device " + this.name + ": Execute command/Write " 
                           + dataElement.name + " value " + dataElement.value);
               executeCommand(dataElement);
            }
         }
      }
      catch (Exception ex) {
          ex.printStackTrace();
      }
   }

   public void addModbusCommand() {

     // Check Modbus registers triggers
     for (Map.Entry<String, DataElement > e : dataList.entrySet()) {
        DataElement dataElement = e.getValue();
        if ( dataElement.type == DataType.TRIGGER ) { // Trigger command
          try {
             dataElement.setvalue = holdingRegisters.getInt16At(dataElement.mbRegisterOffset);
             if ( dataElement.setvalue != 0 ) {// Command has been triggered by Modbus Master --> add it
                logger.finer("Device:addModbusCommand> Pushing Modbus command for " 
                            + dataElement.name + " with value " + dataElement.setvalue);
                commandSetQueue.add(dataElement);
             }
          }
          catch ( IllegalDataAddressException ex) {
             logger.log(Level.SEVERE, "Device:addModbusCommands> " + ex.getMessage());
          }
       }
       else if ( dataElement.type == DataType.READ_AND_WRITE_VALUE ) { // Writable data from modbus
          try {
             // Set new reference value from holdingRegisters
             switch (dataElement.mbRegisterType) {
               case INT8:  
                      dataElement.setvalue = holdingRegisters.getInt8At(dataElement.mbRegisterOffset);
                  break;
               case INT16: 
                      dataElement.setvalue = holdingRegisters.getInt16At(dataElement.mbRegisterOffset);
                  break;
               case INT32:   
                      dataElement.setvalue = holdingRegisters.getInt32At(dataElement.mbRegisterOffset);
                  break;
               case FLOAT32:  
                      dataElement.setvalue = holdingRegisters.getFloat32At(dataElement.mbRegisterOffset);
                  break;
               default: logger.log(Level.SEVERE, "Device:addModbusCommand> Invalid data element type");
                  break;
             }
             if (dataElement.value != dataElement.setvalue) {
                logger.finer("Device:addModbusCommand> Pushing Modbus command for " 
                            + dataElement.name + " with value " + dataElement.setvalue );
                   commandSetQueue.add(dataElement);
             }
          }
          catch ( IllegalDataAddressException ex) {
             logger.log(Level.SEVERE, "Device:addModbusCommands> " + ex.getMessage());
          }
       }
     }
   }
   
   public void setErrorComStatus() {
     for (Map.Entry<String, DataElement> e : this.dataList.entrySet()) {
        DataElement dataElement = e.getValue();
        if ( dataElement.type == DataType.READ_ONLY_VALUE )
           dataElement.value = 0;
        else if ( dataElement.type == DataType.READ_ONLY_STATUS )
           dataElement.value = 255;
        else if ( dataElement.type == DataType.READ_AND_WRITE_VALUE )
           dataElement.value = 0;
         else if ( dataElement.type == DataType.READ_AND_WRITE_STATUS )
           dataElement.value = 255;
     }
   }

   public void run() {
 
     try {
        while(true) {
           //logger.finer(" --> " + name + ":updating Data...");
           updateDeviceData();
           //logger.finer(" --> " + name + ":updating Modbus registers...");
           mutex.lock();
           updateModbusRegisters();
           mutex.unlock();
           Thread.sleep(delay);
        }
     }
     catch (InterruptedException e) {
        logger.log(Level.SEVERE, "Device:run> " + e.getMessage());
     }
     logger.finer("Device:run> Stopped thread " + name);
   }
}; 
