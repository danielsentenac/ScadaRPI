/*
 * This Class is the implementation of the Modbus thread
 *
 */
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Observer;
import java.util.ArrayList;
import java.util.List;

// jlibmodbus
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.data.DataHolder;
import com.intelligt.modbus.jlibmodbus.data.ModbusHoldingRegisters;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadHoldingRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadHoldingRegistersResponse;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveFactory;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveTCP;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import com.intelligt.modbus.jlibmodbus.utils.*;



public class ModbusSlaveThread implements Runnable, DataTypes  {

   private static final Logger logger = Logger.getLogger("Main");
   private DeviceManager deviceManager;
   private ModbusSlaveTCP slave;
   private Thread thread;

   public ModbusSlaveThread(DeviceManager _deviceManager) {
     
      deviceManager = _deviceManager;

      try {
        TcpParameters tcpParameters = new TcpParameters();
        //listening on localhost
        tcpParameters.setHost(InetAddress.getByName("192.168.224.70"));
        tcpParameters.setPort(1288); // Listening port: must be > 1024 to be run by non admin user
        tcpParameters.setKeepAlive(true);
     
        slave = (ModbusSlaveTCP) ModbusSlaveFactory.createModbusSlaveTCP(tcpParameters);
        slave.setServerAddress(Modbus.TCP_DEFAULT_ID);
        slave.setBroadcastEnabled(true);
        slave.setReadTimeout(5000);

        ModbusSlaveDataHolder modbusSlaveDataHolder = new ModbusSlaveDataHolder();
            modbusSlaveDataHolder.addEventListener(new ModbusEventListener() {
                @Override
                public void onWriteToSingleCoil(int address, boolean value) {
                    logger.finer("ModbusSlaveThread:ModbusEventListener> onWriteToSingleCoil: address " 
                                 + address + ", value " + value);
                }

                @Override
                public void onWriteToMultipleCoils(int address, int quantity, boolean[] values) {
                    logger.finer("ModbusSlaveThread:ModbusEventListener> onWriteToMultipleCoils: address " 
                                 + address + ", quantity " + quantity);
                }

                @Override
                public void onWriteToSingleHoldingRegister(int address, int value) {
                    logger.finer("ModbusSlaveThread:ModbusEventListener> onWriteToSingleHoldingRegister: address " 
                                 + address + ", value " + value);
                }

                @Override
                public void onWriteToMultipleHoldingRegisters(int address, int quantity, int[] values) {
                    logger.finer("ModbusSlaveThread:ModbusEventListener> onWriteToMultipleHoldingRegisters: address " 
                                 + address + ", quantity " + quantity);
                }
        });

        slave.setDataHolder(modbusSlaveDataHolder);

        FrameEventListener listener = new FrameEventListener() {
           @Override
           public void frameSentEvent(FrameEvent event) {
              logger.finer("ModbusSlaveThread:FrameEventListener> Frame sent " + 
                           DataUtils.toAscii(event.getBytes()));
           }
           @Override
           public void frameReceivedEvent(FrameEvent event) {
              logger.finer("ModbusSlaveThread:FrameEventListener> Frame recv " + 
                           DataUtils.toAscii(event.getBytes()));
           }
        };
        slave.addListener(listener);

        Observer o = new ModbusSlaveTcpObserver() {
           @Override
           public void clientAccepted(TcpClientInfo info) {
              logger.finer("ModbusSlaveThread:ModbusSlaveTcpObserver> Client connected " + 
                           info.getTcpParameters().getHost());
           }
           @Override
           public void clientDisconnected(TcpClientInfo info) {
              logger.finer("ModbusSlaveThread:ModbusSlaveTcpObserver> Client disconnected " +
                           info.getTcpParameters().getHost());
           }
        };
        slave.addObserver(o);
        Modbus.setAutoIncrementTransactionId(true);
      }
      catch ( UnknownHostException e) {
         logger.log(Level.SEVERE, e.getMessage());
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

   public void configureRegistersFromAllDevice(ModbusHoldingRegisters holdingRegisters) {

      logger.finer("ModbusSlaveThread:configureRegistersFromAllDevice> Configure Modbus Registers for all devices");
      
      for (Map.Entry<String, Device> d : deviceManager.deviceList.entrySet()) {
         for (Map.Entry<String, DataElement> e : d.getValue().dataList.entrySet()) {
            DataElement dataElement = e.getValue();
            try {
               switch (dataElement.mbRegisterType) {
                case INT8:    dataElement.mbRegisterLength = 1;  // 8 bit Byte
                           holdingRegisters.set(dataElement.mbRegisterOffset, 0);
                  break;
                case INT16:    dataElement.mbRegisterLength = 1;  // 16 bit Short
                           holdingRegisters.set(dataElement.mbRegisterOffset, 0);
                  break;
                case INT32:    dataElement.mbRegisterLength = 2;  // 32 bit Integer
                           holdingRegisters.set(dataElement.mbRegisterOffset, 0); 
                           holdingRegisters.set(dataElement.mbRegisterOffset+1, 0);
                  break;
                case FLOAT32:    dataElement.mbRegisterLength = 2;  // 32 bit Float
                           holdingRegisters.set(dataElement.mbRegisterOffset, 0);  
                           holdingRegisters.set(dataElement.mbRegisterOffset+1, 0);
                  break;
                default: logger.log(Level.SEVERE, "ModbusSlaveThread:configureRegistersFromAllDevice> " +
                                   dataElement.name + ":Invalid data element type:");
                  break;
               }
            }
            catch ( IllegalDataAddressException ex) {
               logger.log(Level.SEVERE, "ModbusSlaveThread:configureRegistersFromAllDevice> " +
                                   d.getKey() + "," + dataElement.name + ":" +  ex.getMessage() + ":" + ex.getDataAddress());
            }
            catch ( IllegalDataValueException ex) {
               logger.log(Level.SEVERE, "ModbusSlaveThread:configureRegistersFromAllDevice> " +
                                  d.getKey() + "," + dataElement.name + ":"+ ex.getMessage());
            }
         }
      }
   }
 
   public void run () {
             
      try {
         slave.listen();
      }
      catch (ModbusIOException e) {
          logger.log(Level.SEVERE, "ModbusSlaveThread:run> " + e.getMessage());
      }      
   }

   public interface ModbusEventListener {

        void onWriteToSingleCoil(int address, boolean value);

        void onWriteToMultipleCoils(int address, int quantity, boolean[] values);

        void onWriteToSingleHoldingRegister(int address, int value);

        void onWriteToMultipleHoldingRegisters(int address, int quantity, int[] values);
   }

   public class ModbusSlaveDataHolder extends DataHolder {

        final List<ModbusEventListener> modbusEventListenerList = new ArrayList<ModbusEventListener>();

        public ModbusSlaveDataHolder() {
            
            // Set up registers according to deviceManager
            logger.finer( "ModbusSlaveThread:ModbusSlaveDataHolder> Configure Registers");
            ModbusHoldingRegisters holdingRegisters = new ModbusHoldingRegisters(10000);
            configureRegistersFromAllDevice(holdingRegisters);
            setHoldingRegisters(holdingRegisters);
            // Attach modbus holding registers to all devices
            for (Map.Entry<String, Device> d : deviceManager.deviceList.entrySet())
              (d.getValue()).setHoldingRegisters(holdingRegisters);
                   
        }

        public void addEventListener(ModbusEventListener listener) {
            modbusEventListenerList.add(listener);
        }

        public boolean removeEventListener(ModbusEventListener listener) {
            return modbusEventListenerList.remove(listener);
        }

        @Override
        public void writeHoldingRegister(int offset, int value) 
           throws IllegalDataAddressException, IllegalDataValueException {
           logger.fine("ModbusSlaveThread:writeHoldingRegister> Writing register at offset " + offset + " value=" + value);
           for (ModbusEventListener l : modbusEventListenerList) {
              l.onWriteToSingleHoldingRegister(offset, value);
           }
           for (Map.Entry<String, Device> d : deviceManager.deviceList.entrySet()) {
              Device device = d.getValue();
              logger.finer("ModbusSlaveThread:writeHoldingRegister> found device:" + device.name);
              DataElement dataElement = device.getDataElement(offset);
              logger.finer("ModbusSlaveThread:writeHoldingRegister> found dataElement:" + dataElement);
              if (dataElement != null && 
                 ( dataElement.type == DataType.TRIGGER ||   // Command triggers registers
                   dataElement.type == DataType.READ_AND_WRITE_VALUE ) ) { // Writable registers
                 logger.finer("ModbusSlaveThread:writeHoldingRegister> Acquire lock..");
                 device.modbusmutex.lock();
                 logger.finer("ModbusSlaveThread:writeHoldingRegister> Writing register..");
                 super.writeHoldingRegister(offset, value);
                 device.addModbusCommand();
                 device.modbusmutex.unlock();
                 break;
              }
           }
        }

        @Override
        public void writeHoldingRegisterRange(int offset, int[] range) 
            throws IllegalDataAddressException, IllegalDataValueException {
            for (ModbusEventListener l : modbusEventListenerList) {
                l.onWriteToMultipleHoldingRegisters(offset, range.length, range);
            }
            super.writeHoldingRegisterRange(offset, range);
        }

        @Override
        public void writeCoil(int offset, boolean value) 
            throws IllegalDataAddressException, IllegalDataValueException {
            for (ModbusEventListener l : modbusEventListenerList) {
                l.onWriteToSingleCoil(offset, value);
            }
            super.writeCoil(offset, value);
        }

        @Override
        public void writeCoilRange(int offset, boolean[] range) 
            throws IllegalDataAddressException, IllegalDataValueException {
            for (ModbusEventListener l : modbusEventListenerList) {
                l.onWriteToMultipleCoils(offset, range.length, range);
            }
            super.writeCoilRange(offset, range);
        }
    }
};
