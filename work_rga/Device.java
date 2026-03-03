/*
 * This Abstract Class is used to define general device functionalities
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
   public final Lock modbusmutex = new ReentrantLock(true);     // Used to lock modbus register updates between device and modbusthread.
   public static final Lock busmutex = new ReentrantLock(true); // Used to lock read/write between multiple children device sharing the same bus 
   public  String name;
   protected boolean hasWarned = false;
   protected int comm_cnt_failure = 0;
   protected int comm_max_failure = 5;
   private Thread thread;
   protected int sleepPeriod = 1000;
   public  LinkedList<DataElement> commandSetQueue = new LinkedList<DataElement>();

   public abstract void updateDeviceData();
   public abstract void executeCommand(DataElement e);

   protected final GpioController gpio = initGpioController();
   protected GpioPinDigitalInput i2c_in;
   protected GpioPinDigitalOutput i2c_out;
   protected boolean use_i2c_out = false;

   private static GpioController initGpioController() {
      String arch = System.getProperty("os.arch", "").toLowerCase();
      boolean isArm = arch.contains("arm") || arch.contains("aarch64");

      if (!isArm) {
         logger.log(Level.INFO, "Device:initGpioController> Non-ARM host detected ({0}); GPIO disabled.", arch);
         return null;
      }

      try {
         return GpioFactory.getInstance();
      } catch (Throwable t) {
         logger.log(Level.WARNING, "Device:initGpioController> GPIO init failed, continuing without GPIO: {0}", t.toString());
         return null;
      }
   }


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
               if (dataElement.type == DataType.TRIGGER)  {  // Set value if is a command trigger only
                  dataElement.value = dataElement.setvalue;
                  // Update modbus register
                  holdingRegisters.setInt16At(dataElement.mbRegisterOffset,(int)dataElement.value);
               }
               logger.fine("Device:popCommand> Device " + this.name + ": Execute command/Write value " 
                           + dataElement.name + " value " + dataElement.setvalue);
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
                logger.fine("Device:addModbusCommand>TRIGGER: Pushing Modbus command for " 
                            + dataElement.name + " with value " + dataElement.setvalue);
                commandSetQueue.add(dataElement);
             }
          }
          catch ( IllegalDataAddressException ex) {
             logger.log(Level.SEVERE, "Device:addModbusCommands> " + ex.getMessage());
          }
       }
       else if ( dataElement.type == DataType.READ_AND_WRITE_VALUE ||
                 dataElement.type == DataType.READ_AND_WRITE_STATUS ) { // Writable data from modbus
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
             logger.fine("value = " + dataElement.value + " // setvalue = " +  dataElement.setvalue);
             if (dataElement.value != dataElement.setvalue) {
                logger.fine("Device:addModbusCommand>READ_AND_WRITE_VALUE: Pushing Modbus command for " 
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
     hasWarned = true;
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
        else if ( dataElement.type == DataType.COM_STATUS )
           dataElement.value = 1;
     }
   }

   public void run() {
 
     try {
        while(true) {
           logger.finer(" --> " + name + ":updating Data...");
           updateDeviceData();
           logger.finer(" --> " + name + ":updating Modbus registers...");
           modbusmutex.lock();
           updateModbusRegisters();
           modbusmutex.unlock();
           Thread.sleep(sleepPeriod);
        }
     }
     catch (InterruptedException e) {
        logger.log(Level.SEVERE, "Device:run> " + e.getMessage());
     }
     logger.finer("Device:run> Stopped thread " + name);
   }
}; 
