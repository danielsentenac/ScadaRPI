/*
 * This Class is the implementation of the IntesisBox device (Temperature & Humidity)
 *
 */
import java.util.*;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.nio.ByteBuffer;
import java.io.FileReader;
import java.io.BufferedReader;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.data.ModbusHoldingRegisters;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadHoldingRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadHoldingRegistersResponse;
import com.intelligt.modbus.jlibmodbus.serial.*;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlave;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveFactory;
import com.intelligt.modbus.jlibmodbus.utils.DataUtils;
import com.intelligt.modbus.jlibmodbus.utils.FrameEvent;
import com.intelligt.modbus.jlibmodbus.utils.FrameEventListener;


public class IntesisBox extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   private ModbusMaster master;
   private SerialParameters sp;
   private String serial_port;
   private SerialPort.BaudRate baudrate;
   private int databits;
   private SerialPort.Parity parity;
   private int stopbits;
   private ModbusHoldingRegisters holdingRegisters;
   private int numReg = 65;
   private int slaveId = 2;
   public int tempMax = 24;
   public int tempMin = 16;
   public int fanMax = 3;
   public int fanMin = 0;

   public IntesisBox (String _name, 
		      int _delay,
                      int _mbRegisterStart,
                      String _serial_port,
                      SerialPort.BaudRate _baudrate,
                      int _databits,
                      SerialPort.Parity _parity,
                      int _stopbits) {

     name = _name;        // Device name
     delay = _delay;      // delay between 2 queries
     serial_port = _serial_port;
     baudrate = _baudrate;
     databits = _databits;
     parity = _parity;
     stopbits = _stopbits;
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("IntesisBox:IntesisBox> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Intesis writable registers
     addDataElement( new DataElement(name, "ACONOFF",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "ACMODE",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "ACFAN",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "ACSET",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "ACECO",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));

     // Controller IntesisBox comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("IntesisBox:IntesisBox> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     //
     // Connect Modbus Master
     //
     connect();
   }

   public void updateDeviceData() {
     // Get monitoring data from device using OneWire Comm
     DataElement dcom = getDataElement("COMST");
     popCommand();  // Execute commands
     // Read IntesisBox holding registers
     try {
        //prepare request
        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest();
        request.setServerAddress(slaveId); 
        request.setStartAddress(0);
        request.setQuantity(numReg);
        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) request.getResponse();
        master.processRequest(request);
        ModbusHoldingRegisters registers = response.getHoldingRegisters();
        for (int i = 0; i < registers.getQuantity(); i++) {
           logger.finer("IntesisBox:updateDeviceData> register(" + i + ") = " + registers.getInt16At(i));
        }
        getDataElement("ACONOFF").value = registers.getInt16At(0);
        getDataElement("ACMODE").value = registers.getInt16At(1);
        getDataElement("ACFAN").value = registers.getInt16At(2);
        getDataElement("ACSET").value = registers.getInt16At(4);
        getDataElement("ACECO").value = registers.getInt16At(64);
        if (comErr == true) {
           logger.log(Level.WARNING, "IntesisBox:updateDeviceData> Communication with " + name + " is back");
           comErr = false;
           dcom.value = 0; //ERR COM
        }
     }
     catch (Exception ex) {
     	ex.printStackTrace();
     	if (comErr == false) {
           logger.log(Level.WARNING, "IntesisBox:updateDeviceData> Communication with " + name + " is interrupted");
           comErr = true;
           dcom.value = 1; //ERR COM
        }
        setErrorComStatus();
        connect();
     }
   }
   private void connect() {
      logger.log(Level.WARNING, "IntesisBox:connect> Diconnecting/Connecting..."); 
      try {
         if (master != null) master.disconnect();
         master = null;
         sp = null;
         sp = new SerialParameters();
         sp.setParity(parity);
         sp.setStopBits(stopbits);
         sp.setDataBits(databits);
         sp.setBaudRate(baudrate);
         sp.setDevice(serial_port);
         SerialUtils.setSerialPortFactory(new SerialPortFactoryJSSC());
         master = ModbusMasterFactory.createModbusMasterRTU(sp); 
         master.setResponseTimeout(5000);
         FrameEventListener listener = new FrameEventListener() {
           @Override
            public void frameSentEvent(FrameEvent event) {
               logger.finer("IntesisBox::FrameEventListener> frame sent " + DataUtils.toAscii(event.getBytes()));
            }
           @Override
            public void frameReceivedEvent(FrameEvent event) {
               logger.finer("IntesisBox::FrameEventListener> frame recv " + DataUtils.toAscii(event.getBytes()));
            }
        };
        master.addListener(listener);
        holdingRegisters =  new ModbusHoldingRegisters(numReg);
        master.connect();
      }
      catch (Exception ex) {
     	ex.printStackTrace();
      }
   }
   public void executeCommand( DataElement e ) {
      try {
         logger.finer("IntesisBox:executeCommand> Write single register to " + e.name + " value : " + e.setvalue); 
         if (e.name.equals("ACONOFF"))  
         	master.writeSingleRegister(slaveId, 0, (int)e.setvalue);
     	 if (e.name.equals("ACMODE"))    
        	master.writeSingleRegister(slaveId, 1, (int)e.setvalue);
     	 if (e.name.equals("ACFAN"))    
     	    master.writeSingleRegister(slaveId, 2, (int)e.setvalue);
      	 if (e.name.equals("ACSET"))    
        	 master.writeSingleRegister(slaveId, 4, (int)e.setvalue);
      	 if (e.name.equals("ACECO"))    
       	     master.writeSingleRegister(slaveId, 64, (int)e.setvalue);	
      }
      catch (ModbusProtocolException ex) {
            ex.printStackTrace();
      } catch (ModbusIOException ex) {
            ex.printStackTrace();
      } catch (ModbusNumberException ex) {
            ex.printStackTrace();
      } 
   }
}; 
