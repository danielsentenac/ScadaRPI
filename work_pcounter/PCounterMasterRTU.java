/*
 * This Class is the implementation of the PCounter Master RTU thread
 *
 */
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.utils.DataUtils;
import com.intelligt.modbus.jlibmodbus.serial.*;
import com.intelligt.modbus.jlibmodbus.utils.FrameEvent;
import com.intelligt.modbus.jlibmodbus.utils.FrameEventListener;
import java.io.*;
import java.util.Properties;



public class PCounterMasterRTU extends Device  {

    private static final Logger logger = Logger.getLogger("Main");
    private ModbusMaster master;
    private int slaveId = 247;
    private SerialPort.BaudRate baudrate;
    private int databits;
    private SerialPort.Parity parity;
    private int stopbits;
    private String serial_port;

    private String getConfigFilePath() {
        String filename = "particlecounter" + name + ".properties";
        File localPath = new File(filename);
        if (localPath.exists()) {
            return localPath.getPath();
        }

        return new File("work_pcounter", filename).getPath();
    }

    public PCounterMasterRTU(String _name,
                             String _aliasName,
                           int _mbRegisterStart, 
                           String _serial_port, 
                           SerialPort.BaudRate _baudrate, 
                           int _databits, 
                           SerialPort.Parity _parity, 
                           int _stopbits) {

        serial_port = _serial_port;
        baudrate = _baudrate;
        databits = _databits;
        parity = _parity;
        stopbits = _stopbits;

        comm_max_failure = 2;
        sleep_period = 1000;
        name = _name; // Device name
        aliasName = _aliasName; // Device location name
        mbRegisterStart = _mbRegisterStart;  // Starting PCounter register offset
           
        logger.finer("PCounterMasterRTU:PCounterMasterRTU> " + name + " PCounter registers starts at offset " + mbRegisterStart);

        mbRegisterEnd = mbRegisterStart;
        
        // Particle counter channel values (FT3)
        addDataElement( new DataElement(name, "UM03", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
        addDataElement( new DataElement(name, "UM05", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
        addDataElement( new DataElement(name, "UM1", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
        addDataElement( new DataElement(name, "UM205", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
        addDataElement( new DataElement(name, "UM5", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
        addDataElement( new DataElement(name, "UM10", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
        
        // Particle counter status (acquiring/pause/holding/stopped)
        addDataElement( new DataElement(name, "ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
        
        // Particle counter sampling time value
        addDataElement( new DataElement(name, "SAMPLING", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
        
        // Particle counter holding time value
        addDataElement( new DataElement(name, "HOLDING", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
        
        // Particle counter cycle number value
        addDataElement( new DataElement(name, "CYCLE", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
        
        // Particle counter flow value
        addDataElement( new DataElement(name, "FLOW", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
        
        // Particle counter instrument status
        addDataElement( new DataElement(name, "INSTRST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
        
        // Particle counter threshold values
        addDataElement( new DataElement(name, "UM03THR", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=1));
        addDataElement( new DataElement(name, "UM05THR", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=2));
        addDataElement( new DataElement(name, "UM1THR", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=2));
        addDataElement( new DataElement(name, "UM205THR", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=2));
        addDataElement( new DataElement(name, "UM5THR", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=2));
        addDataElement( new DataElement(name, "UM10THR", DataType.READ_AND_WRITE_VALUE,RegisterType.INT32,mbRegisterEnd+=2));
        
        // Particle counter flags
        addDataElement( new DataElement(name, "UM03FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
        addDataElement( new DataElement(name, "UM05FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
        addDataElement( new DataElement(name, "UM1FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
        addDataElement( new DataElement(name, "UM205FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
        addDataElement( new DataElement(name, "UM5FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
        addDataElement( new DataElement(name, "UM10FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
        
        // Particle counter START/STOP command
        addDataElement( new DataElement(name, "STARTSTOP", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
        
        // ParticlePlus comm
        addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
        
        mbRegisterEnd+=1;
        
        // Initialize threashold calues from configuration file
         try {
            String configFilePath = getConfigFilePath();
            FileInputStream propsInput = new FileInputStream(configFilePath);
            Properties prop = new Properties();
            prop.load(propsInput);
            String UM03THRStr = prop.getProperty("UM03THR","0");
            String UM05THRStr = prop.getProperty("UM05THR","0");
            String UM1THRStr = prop.getProperty("UM1THR","0");
            String UM205THRStr = prop.getProperty("UM205THR","0");
            String UM5THRStr = prop.getProperty("UM5THR","0");
            String UM10THRStr = prop.getProperty("UM10THR","0");
            DataElement d1thr = getDataElement("UM03THR");
            DataElement d2thr = getDataElement("UM05THR");
            DataElement d3thr = getDataElement("UM1THR");
            DataElement d4thr = getDataElement("UM205THR");
            DataElement d5thr = getDataElement("UM5THR");
            DataElement d6thr = getDataElement("UM10THR");
            d1thr.value = Integer.valueOf(UM03THRStr);
            d2thr.value = Integer.valueOf(UM05THRStr);
            d3thr.value = Integer.valueOf(UM1THRStr);
            d4thr.value = Integer.valueOf(UM205THRStr);
            d5thr.value = Integer.valueOf(UM5THRStr);
            d6thr.value = Integer.valueOf(UM10THRStr);
            logger.info("PCounterMasterRTU::PCounterMasterRTU> " + name + " : Set threshold value for " + d1thr.name + " = " + Double.toString(d1thr.value));
            logger.info("PCounterMasterRTU::PCounterMasterRTU> " + name + " : Set threshold value for " + d2thr.name + " = " + Double.toString(d2thr.value));
            logger.info("PCounterMasterRTU::PCounterMasterRTU> " + name + " : Set threshold value for " + d3thr.name + " = " + Double.toString(d3thr.value));
            logger.info("PCounterMasterRTU::PCounterMasterRTU> " + name + " : Set threshold value for " + d4thr.name + " = " + Double.toString(d4thr.value));
            logger.info("PCounterMasterRTU::PCounterMasterRTU> " + name + " : Set threshold value for " + d5thr.name + " = " + Double.toString(d5thr.value));
            logger.info("PCounterMasterRTU::PCounterMasterRTU> " + name + " : Set threshold value for " + d6thr.name + " = " + Double.toString(d6thr.value));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
         
        // Instantiate communication channel
        try {
                SerialParameters sp = new SerialParameters(); // Serial parameters
                sp.setDevice(serial_port);
                sp.setBaudRate(baudrate);
                sp.setDataBits(databits);
                sp.setParity(parity);
                sp.setStopBits(stopbits);
                SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
                master = ModbusMasterFactory.createModbusMasterRTU(sp);
                master.setResponseTimeout(5000);  
                FrameEventListener listener = new FrameEventListener() {
                   @Override
                   public void frameSentEvent(FrameEvent event) {
                      System.out.println("frame sent " + DataUtils.toAscii(event.getBytes()));
                   }

                   @Override
                   public void frameReceivedEvent(FrameEvent event) {
                      System.out.println("frame recv " + DataUtils.toAscii(event.getBytes()));
                   }
                };
                master.addListener(listener);
        } 
        catch (SerialPortException e) {
            if ( hasWarned == false ) {
                logger.log(Level.WARNING, "PCounterMasterRTU::PCounterMasterRTU> Communication with " + name + " interrupted");
                logger.log(Level.SEVERE, "PCounterMasterRTU::PCounterMasterRTU> " + name + ": "  + e.getMessage());
             }
        }
    }
    
    public void setErrorComStatus() {
     hasWarned = true;
     for (Map.Entry<String, DataElement> e : this.dataList.entrySet()) {
        DataElement dataElement = e.getValue();
        if ( dataElement.type == DataType.READ_ONLY_VALUE )
           dataElement.value = 0;
        else if ( (dataElement.type == DataType.READ_ONLY_STATUS) && !dataElement.name.contains("FLG"))
           dataElement.value = 255;
        else if ( (dataElement.type == DataType.READ_ONLY_STATUS) && dataElement.name.contains("FLG"))
           dataElement.value = 1;
        else if ( dataElement.type == DataType.READ_AND_WRITE_VALUE && !(dataElement.name).contains("THR"))
           dataElement.value = 0;
        else if ( dataElement.type == DataType.READ_AND_WRITE_STATUS )
           dataElement.value = 255;
        else if ( dataElement.type == DataType.COM_STATUS )
           dataElement.value = 1;
     }
   }
   
    public void updateDeviceData() {
   
     // Get monitoring data from device using rs485 Comm
     DataElement d1 = getDataElement("UM03");
     DataElement d2 = getDataElement("UM05");
     DataElement d3 = getDataElement("UM1");
     DataElement d4 = getDataElement("UM205");
     DataElement d5 = getDataElement("UM5");
     DataElement d6 = getDataElement("UM10");
     DataElement d1flg = getDataElement("UM03FLG");
     DataElement d2flg = getDataElement("UM05FLG");
     DataElement d3flg = getDataElement("UM1FLG");
     DataElement d4flg = getDataElement("UM205FLG");
     DataElement d5flg = getDataElement("UM5FLG");
     DataElement d6flg = getDataElement("UM10FLG");
     DataElement d1thr = getDataElement("UM03THR");
     DataElement d2thr = getDataElement("UM05THR");
     DataElement d3thr = getDataElement("UM1THR");
     DataElement d4thr = getDataElement("UM205THR");
     DataElement d5thr = getDataElement("UM5THR");
     DataElement d6thr = getDataElement("UM10THR");
     DataElement st = getDataElement("ST");
     DataElement instrst = getDataElement("INSTRST");
     DataElement sampling = getDataElement("SAMPLING");
     DataElement holding = getDataElement("HOLDING");
     DataElement flow = getDataElement("FLOW");  
     DataElement cycle = getDataElement("CYCLE");
     
     // Com Status
     DataElement dcom = getDataElement("COMST");

     int mbCumulativeFT3 = 41500; // Start offset for channels DIFFERENTIAL FT3 values (Float)
     int mbLatchData = 41000; // Latch data
     int mbStatus = 40500; // Start offset for Status (Int16)
     int mbInstrStatus = 41033; // Start offset for Instrument Error Status (Int16)
     int mbSampling = 40504; // Start offset for Sampling time (Int16)
     int mbHolding = 40505; // Start offset for Holding time (Int16)
     int mbCycle = 40506; // Start offset for Cycle number (Int16)
     int mbFlow = 41032; // Start offset for Flow rate (Int16)
     try {
         popCommand();  // Execute commands it in the loop is more reactive
         /****************************************************************************************/
         /****************************************************************************************/
         // Read All cumulative FT3 values for 6 channels and convert to Float values
         master.connect();
         // Latch current data
         int[] registers = {0,0};
         master.writeMultipleRegisters(slaveId, mbLatchData, registers);
         
         // Read channel values
         for (int i = 0; i < 12 ; i+=2) {
             int register = mbCumulativeFT3 + i;
             int[] registerValues = master.readHoldingRegisters(slaveId, register, 2);
             int resultInt32 = (registerValues[0] << 16) + registerValues[1];
             float result = Float.intBitsToFloat(resultInt32);
             String s = String.valueOf(result);
             //System.out.println("Cumulative FT3 Address: " + register + ", registers Value: " + registerValues[0] + "," + registerValues[1]); 
             //System.out.println("Cumulative FT3 Address: " + register + ", Float Value: " + s);
             switch (i) {
               case 0: d1.value = ( Double.isInfinite(result) || Double.isNaN(result) ) ?  0 : result;
                       if (d1.value >= d1thr.value) d1flg.value = 2;
                       else if (d1.value >= d1thr.value*0.9)  d1flg.value = 1; 
                       else d1flg.value = 0;
                  break;
               case 2: d2.value = ( Double.isInfinite(result) || Double.isNaN(result) ) ?  0 : result;
                       if (d2.value >= d2thr.value) d2flg.value = 2; 
                       else if (d2.value >= d2thr.value*0.9)  d2flg.value = 1; 
                       else d2flg.value = 0;
                  break;
               case 4: d3.value = ( Double.isInfinite(result) || Double.isNaN(result) ) ?  0 : result;
                       if (d3.value >= d3thr.value) d3flg.value = 2; 
                       else if (d3.value >= d3thr.value*0.9)  d3flg.value = 1; 
                       else d3flg.value = 0;
                  break;
               case 6: d4.value = ( Double.isInfinite(result) || Double.isNaN(result) ) ?  0 : result;
                       if (d4.value >= d4thr.value) d4flg.value = 2; 
                       else if (d4.value >= d4thr.value*0.9)  d4flg.value = 1; 
                       else d4flg.value = 0;
                  break;
               case 8: d5.value = ( Double.isInfinite(result) || Double.isNaN(result) ) ?  0 : result;
                       if (d5.value >= d5thr.value) d5flg.value = 2;
                       else if (d5.value >= d5thr.value*0.9)  d5flg.value = 1; 
                       else d5flg.value = 0;
                  break;
               case 10: d6.value = ( Double.isInfinite(result) || Double.isNaN(result) ) ?  0 : result;
                        if (d6.value >= d6thr.value) d6flg.value = 2;
                        else if (d6.value >= d6thr.value*0.9)  d6flg.value = 1; 
                        else d6flg.value = 0;
                  break;
               default:
                 break;
             }
         }
         // Read SAMPLING value
         int[] registerValues = master.readHoldingRegisters(slaveId, mbSampling, 1);
         //System.out.println("SAMPLING Address: " + mbStatus + ", Value: " + registerValues[0]);
         sampling.value = registerValues[0];
         // Read HOLDING value
         registerValues = master.readHoldingRegisters(slaveId, mbHolding, 1);
         //System.out.println("HOLDING Address: " + mbStatus + ", Value: " + registerValues[0]);
         holding.value = registerValues[0];
         // Read FLOW value
         registerValues = master.readHoldingRegisters(slaveId, mbFlow, 1);
         //System.out.println("FLOW Address: " + mbStatus + ", Value: " + registerValues[0]);
         flow.value = registerValues[0];
         // Read CYCLE value
         registerValues = master.readHoldingRegisters(slaveId, mbCycle, 1);
         //System.out.println("CYCLE Address: " + mbStatus + ", Value: " + registerValues[0]);
         cycle.value = registerValues[0];
         // Read STATUS value
         registerValues = master.readHoldingRegisters(slaveId, mbStatus, 1);
         //System.out.println("STATUS Address: " + mbStatus + ", Value: " + registerValues[0]);
         st.value = registerValues[0];
         // Read INSTRUMENT STATUS value
         registerValues = master.readHoldingRegisters(slaveId, mbInstrStatus, 1);
         logger.finer("PCounterMasterRTU:updateDeviceData> INSTRUMENT STATUS Address: " + mbInstrStatus + ", Value: " + registerValues[0]);
         instrst.value = registerValues[0];
         if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("PCounterMasterRTU:updateDeviceData> Communication with " + name + " back!");
         }

         comm_cnt_failure = 0;
         dcom.value = 0;
         master.disconnect();
     } catch (RuntimeException e) {
           throw e;
     } catch (Exception e) {
           logger.log(Level.WARNING, "PCounterMasterRTU::updateDeviceData> hasWarned=" + comm_cnt_failure);
           try {
             master.disconnect();
           }
           catch (Exception e1) {}
           if (++comm_cnt_failure > comm_max_failure) {
              if ( hasWarned == false ) {
                logger.log(Level.WARNING, "PCounterMasterRTU::updateDeviceData> Communication with " + name + " interrupted");
                logger.log(Level.SEVERE, "PCounterMasterRTU::updateDeviceData> " + name + ": " + e.getMessage());
              }
              setErrorComStatus();
           }
       }  
   }

   public void executeCommand( DataElement e ) {
      System.out.println("executeCommand element name and value " + e.name + " value = " + e.value);
      try {
         master.connect();
         if (e.name.contains("STARTSTOP")) {
            int mbStartStop = 40501; // Start/Stop command offset (Int16)
            if ( e.value == 1 ) {       // START 
               int[] registers = {1};
               master.writeMultipleRegisters(slaveId, mbStartStop, registers);
            }
            else if ( e.value == 2 ) {  // STOP
               int[] registers = {0};
               master.writeMultipleRegisters(slaveId, mbStartStop, registers);
            }
         }
         else if (e.name.contains("SAMPLING")) {
            int mbSampling = 40504; // Sampling value offset (Int16)
            int[] registers = {(int)e.setvalue};
            master.writeMultipleRegisters(slaveId, mbSampling, registers);
         }
         else if (e.name.contains("HOLDING")) {
            int mbHolding = 40505; // Holding value offset (Int16)
            int[] registers = {(int)e.setvalue};
            master.writeMultipleRegisters(slaveId, mbHolding, registers);
         }
         else if (e.name.contains("CYCLE")) {
            int mbCycle = 40506; // Cycle value offset (Int16)
            int[] registers = {(int)e.setvalue};
            master.writeMultipleRegisters(slaveId, mbCycle, registers);
         }
         else if (e.name.contains("THR")) {
            System.out.println("executeCommand Assign to element name setvalue " + e.name + " value = " + e.setvalue);
            e.value = e.setvalue;
            holdingRegisters.setFloat32At(e.mbRegisterOffset, (float)e.value);
            String configFilePath = getConfigFilePath();
            FileInputStream propsInput = new FileInputStream(configFilePath);
            Properties prop = new Properties();
            prop.load(propsInput);
            prop.setProperty(e.name,Integer.toString((int)e.value));
            prop.store(new FileOutputStream(configFilePath),null);
         }
         if (e.type == DataType.TRIGGER) {
            Thread.sleep(2000); // Wait before resetting
            // Reset
            e.value = 0;
            holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
         master.disconnect();
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "PCounterMasterRTU::executeCommand>" + ex.getMessage());
        try {
             master.disconnect();
           }
           catch (Exception e1) {}       
     }
   }
}
