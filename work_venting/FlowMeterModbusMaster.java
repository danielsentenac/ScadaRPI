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
import com.intelligt.modbus.jlibmodbus.data.ModbusCoils;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadHoldingRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadHoldingRegistersResponse;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse;
import com.intelligt.modbus.jlibmodbus.msg.request.WriteMultipleRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadCoilsRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadCoilsResponse;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveFactory;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveTCP;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import com.intelligt.modbus.jlibmodbus.utils.*;

public class FlowMeterModbusMaster extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   private static int slaveID = 0xFF;
   private String ipAddress;
   private int port;
   private ModbusMaster m;

   public FlowMeterModbusMaster (String _name,
                        String _ipAddress,
                        int _port,
                        int _mbRegisterStart) {

     name = _name; // Device name
     ipAddress = _ipAddress; // Modbus slave address
     port = _port;   // Modbus slave port
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("ModbusMaster:ModbusMaster> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Read only Values
     addDataElement( new DataElement(name, "FLOW", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     addDataElement( new DataElement(name, "TEMP", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "VALVE_POS", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     
     // Read and Write values
     addDataElement( new DataElement(name, "FLOW_SETP", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "RAMP", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "VALVE_OPEN", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "VALVE_CLOSE", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "FLOW_ZERO", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     
     // For Operation
     addDataElement( new DataElement(name, "OPFLOWSTEP", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "OPFLOWMAX", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "OPTIMEINTER", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "OPTIME", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     
     // FlowMeter status comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=2));

     mbRegisterEnd+=1;

     logger.finer("ModbusMaster:ModbusMaster> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     // Init Master modbus
      try {
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(ipAddress));
            tcpParameters.setPort(port);
            tcpParameters.setKeepAlive(true);

            m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);
                
            if (!m.isConnected()) {
                 m.connect();
            }
      }
      catch (ModbusIOException e) {
                e.printStackTrace();
      }
      catch(Exception e) {
                e.printStackTrace();
      }
   }
   
   @SuppressWarnings("unchecked")
   public void updateDeviceData() {
   
     // Get monitoring data from device using RS232 Comm

     DataElement flow = getDataElement("FLOW");
     DataElement temp = getDataElement("TEMP");
     DataElement valve_pos = getDataElement("VALVE_POS");
     DataElement flow_setp = getDataElement("FLOW_SETP");
     DataElement ramp = getDataElement("RAMP");
     DataElement valve_open = getDataElement("VALVE_OPEN");
     DataElement valve_close = getDataElement("VALVE_CLOSE");
     DataElement flow_zero = getDataElement("FLOW_ZERO");
     DataElement dcom = getDataElement("COMST");
     
     try {
                 
        popCommand();  // Execute commands in the loop is more reactive
   
        //prepare INPUT request
        ReadInputRegistersRequest request = new ReadInputRegistersRequest();
        request.setServerAddress(slaveID);
        request.setStartAddress(16384);
        request.setQuantity(6);
        m.processRequest(request);
        
        ReadInputRegistersResponse response = (ReadInputRegistersResponse) request.getResponse();
        ModbusHoldingRegisters holdingregisters = response.getHoldingRegisters();
        int [] registers = holdingregisters.getRegisters();

        int resultInt32 = (registers[0] << 16) + registers[1];
        flow.value = (double) Float.intBitsToFloat(resultInt32);
        
        resultInt32 = (registers[2] << 16) + registers[3];
        temp.value = Float.intBitsToFloat(resultInt32);
        
        resultInt32 = (registers[4] << 16) + registers[5];
        valve_pos.value = Float.intBitsToFloat(resultInt32);
        
        logger.finer("ModbusMaster:updateDeviceData>" + flow.name + " = " + String.valueOf(flow.value));
        logger.finer("ModbusMaster:updateDeviceData>" + temp.name + " = " + String.valueOf(temp.value));
        logger.finer("ModbusMaster:updateDeviceData>" + valve_pos.name + " = " + String.valueOf(valve_pos.value));
        
        //prepare HOLDING requesth
        ReadHoldingRegistersRequest requesth = new ReadHoldingRegistersRequest();
        requesth.setServerAddress(slaveID);
        requesth.setStartAddress(40960);
        requesth.setQuantity(4);
        m.processRequest(requesth);
        
        ReadHoldingRegistersResponse responseh = (ReadHoldingRegistersResponse) requesth.getResponse();
        ModbusHoldingRegisters holdingregistersh = responseh.getHoldingRegisters();
        int [] registersh = holdingregistersh.getRegisters();
        
        int resultInt32h = (registersh[0] << 16) + registersh[1];
        flow_setp.value = (double) Float.intBitsToFloat(resultInt32h);
        
        logger.finer("FlowMeterModbusMaster:updateDeviceData> receive registersh[2]=" +      
                      String.format("%32s",Integer.toBinaryString(registersh[2])).replaceAll(" ", "0"));
        logger.finer("FlowMeterModbusMaster:updateDeviceData> receive registersh[3]=" +      
                      String.format("%32s",Integer.toBinaryString(registersh[3])).replaceAll(" ", "0"));
                      
        resultInt32h = (registersh[2] << 16) + registersh[3];
        ramp.value = (double) (resultInt32h);
        
        //prepare COILS requestc
        ReadCoilsRequest requestc = new ReadCoilsRequest();
        requestc.setServerAddress(slaveID);
        requestc.setStartAddress(57345);
        requestc.setQuantity(10);
        m.processRequest(requestc);
        
        ReadCoilsResponse responsec = (ReadCoilsResponse) requestc.getResponse();
        ModbusCoils coils = responsec.getModbusCoils();
        
        logger.finer("ModbusMaster:updateDeviceData> Quantity Coils = " + coils.getQuantity());
        
        valve_open.value = coils.get(0)?1.0:0.0;
        valve_close.value = coils.get(1)?1.0:0.0;
        flow_zero.value = 0;
                
        dcom.value = 0; // if arriving here COM OK
        
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("ModbusMaster:updateDeviceData> Communication with " + name + " back!");
        }
     }
     catch (Exception ex) {
        ex.printStackTrace();
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "ModbusMaster:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "ModbusMaster:updateDeviceData>" + ex.getMessage());
        }
        setErrorComStatus();
     }
   }
   @SuppressWarnings("unchecked")
   public void executeCommand( DataElement e ) {
      
      try {
         if ( e.name.contains("FLOW_SETP") ) { // Flow Set point value
            float value = (float) e.setvalue;      //  New setpoint
            System.out.println(name + ":Set new value FLOW SETP " + String.valueOf(value));
            int bits = Float.floatToIntBits(value);
            logger.finer("FlowMeterModbusMaster:executeCommand> send bits=" +      
                      String.format("%32s",Integer.toBinaryString(bits)).replaceAll(" ", "0"));
            int[] registers = new int[2];
            registers[1] = (bits & 0xFFFF);
            registers[0] = ((bits >> 16) & 0xFFFF);
            m.writeMultipleRegisters(slaveID, 40960, registers);
         }
         else if ( e.name.contains("RAMP") ) { // Ramp set value
            int value = (int) e.setvalue;      //  New ramp value
            System.out.println(name + ": Set new RAMP " + String.valueOf(value));
            int[] registers = new int[2];
            registers[1] = (value & 0xFFFF);
            registers[0] = ((value >> 16) & 0xFFFF);
            logger.finer("FlowMeterModbusMaster:executeCommand> send registers[0]=" +      
                      String.format("%32s",Integer.toBinaryString(registers[0])).replaceAll(" ", "0"));
            logger.finer("FlowMeterModbusMaster:executeCommand> send registers[1]=" +      
                      String.format("%32s",Integer.toBinaryString(registers[1])).replaceAll(" ", "0"));
            m.writeMultipleRegisters(slaveID, 40962, registers);
         }
         else if ( e.name.contains("VALVE_OPEN") ) { // Valve OPEN value
            int value = (int) e.setvalue;      //  New valve OPEN value
            if (value != 0 && value != 1) {
              logger.warning("FlowMeterModbusMaster:executeCommand> VALVE_OPEN value must be 0 or 1)");
              return;
            }
            System.out.println(name + ": Set new VALVE OPEN " + String.valueOf(value));
            boolean val = false;
            if ( value == 0 ) val = false;
            else if ( value == 1 ) val = true;
            m.writeSingleCoil(slaveID, 57345, val);
         }
         else if ( e.name.contains("VALVE_CLOSE") ) { // Valve CLOSE value
            int value = (int) e.setvalue;      //  New valve CLOSE value
            if (value != 0 && value != 1) {
              logger.warning("FlowMeterModbusMaster:executeCommand> VALVE_CLOSE value must be 0 or 1)");
              return;
            }
            System.out.println(name + ": Set new VALVE CLOSE " + String.valueOf(value));
            boolean val = false;
            if ( value == 0 ) val = false;
            else if ( value == 1 ) val = true;
            m.writeSingleCoil(slaveID, 57346, val);
         }
         else if ( e.name.contains("FLOW_ZERO") ) { // FLOW ZERO value
            int value = (int) e.setvalue;      //  New flow zeroing value
            if (value != 0 && value != 1) {
              logger.warning("FlowMeterModbusMaster:executeCommand> FLOW ZERO value must be 0 or 1)");
              return;
            }
            System.out.println(name + ": Set new FLOW ZERO " + String.valueOf(value));
            boolean val = false;
            if ( value == 0 ) val = false;
            else if ( value == 1 ) val = true;
            m.writeSingleCoil(slaveID, 57347, val);
         }
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "ModbusMaster:executeCommand>" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "ModbusMaster:executeCommand> Communication with " + name + " interrupted");
     }
   }
}; 
