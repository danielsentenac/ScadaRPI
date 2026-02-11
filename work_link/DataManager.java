/*
 * This class is used to for data management
 */
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;
import com.intelligt.modbus.jlibmodbus.data.ModbusHoldingRegisters;

public class DataManager  implements DataTypes {

  public Hashtable<String, DataElement> dataList;
  public ModbusHoldingRegisters holdingRegisters;
  public int mbRegisterStart;
  public int mbRegisterEnd;

  private static final Logger logger = Logger.getLogger("Main");
 
  public DataManager () {
    dataList = new Hashtable<String,DataElement>();
  }

  public void addDataElement(DataElement dataElement) {
    dataList.put(dataElement.name,dataElement);
  }

  public DataElement getDataElement(int mbRegisterOffset) {
   
     for (Map.Entry<String, DataElement > e : dataList.entrySet()) {
        DataElement dataElement = e.getValue();
        if (mbRegisterOffset == dataElement.mbRegisterOffset)
           return (dataElement);
     }
     return null;
  }

  public DataElement getDataElement(String name) {
   
     DataElement dataElement = null;
     dataElement = dataList.get(name);
     return (dataElement);
  }  

  public void setDataElementValue(String name, double value) {
    
     DataElement dataElement = dataList.get(name);
     if ( dataElement != null )
        dataElement.value = value;
  }

  public void setHoldingRegisters(ModbusHoldingRegisters _holdingRegisters) {
     holdingRegisters = _holdingRegisters;
  }

  public void updateModbusRegisters() {

     for (Map.Entry<String, DataElement> e : dataList.entrySet()) {
        DataElement dataElement = e.getValue();
        if ( dataElement.type != DataType.TRIGGER  ) { // Exclude Trigger commands
           try {
             logger.finer("DataManager:updateModbusRegisters> data  name= " + dataElement.name + " type= " + dataElement.type 
                          + " value=" + dataElement.value + " register type=" + dataElement.mbRegisterType);
             switch (dataElement.mbRegisterType) {
              case INT8:  holdingRegisters.setInt8At(dataElement.mbRegisterOffset, ((byte)(dataElement.value)));
                 break;
              case INT16: holdingRegisters.setInt16At(dataElement.mbRegisterOffset, ((int)(dataElement.value))); 
                 break;
              case INT32: holdingRegisters.setInt32At(dataElement.mbRegisterOffset, ((int)(dataElement.value)));
                 break; 
              case FLOAT32: holdingRegisters.setFloat32At(dataElement.mbRegisterOffset, (float) dataElement.value); 
                 break;
              default:
                   logger.log(Level.SEVERE, "DataManager:updateModbusRegisters> Invalid data element type Class");
                 break;
             }
           }
           catch ( IllegalDataAddressException ex) {
              logger.log(Level.SEVERE, "DataManager:updateModbusRegisters> " + dataElement.name + ":" + ex.getMessage());
           }
           catch ( IllegalDataValueException ex) {
              logger.log(Level.SEVERE, "DataManager:updateModbusRegisters> " + dataElement.name + " type=" + dataElement.mbRegisterType + 
                                                                           " value=" + dataElement.value + ":" + ex.getMessage());
           }
           catch ( NullPointerException ex) {
              logger.log(Level.SEVERE, "DataManager:updateModbusRegisters> " + dataElement.name + ":" + ex.getMessage());
           }
        }
     }
  }
}; 
