/*
 * This Class is used to deinfo a data element
 * data type (1=read data,2=trigger command data,3=read/write data)
 * data register type (1=byte,2=short,3=int,4=float)
 */
import java.util.logging.Logger;

public class DataElement implements DataTypes {

   public String deviceName;
   public String name;
   public DataType type;
   public RegisterType mbRegisterType;
   public int mbRegisterOffset;
   public int mbRegisterLength;
   public double value;
   public double setvalue; // type=3 case: data element reference to be written
   private static final Logger logger = Logger.getLogger("Main");

   public DataElement(String _deviceName, String _name, DataType _type, RegisterType _mbRegisterType, int _mbRegisterOffset) {
      deviceName = _deviceName;
      name = _name;
      type = _type;
      mbRegisterType = _mbRegisterType;
      mbRegisterOffset = _mbRegisterOffset;
      value = setvalue = 0; // Initialization set to 0
      if ( mbRegisterType == RegisterType.INT16 )
         logger.config("_" + deviceName + "_" + name + "   " + mbRegisterOffset + "   1");
      else if ( mbRegisterType == RegisterType.INT8 )
         logger.config("_" + deviceName + "_" + name + "   " + mbRegisterOffset + "   1");
      else if ( mbRegisterType == RegisterType.INT32 )
         logger.config("_" + deviceName + "_" + name + "   " + mbRegisterOffset + "   2");
      else if ( mbRegisterType == RegisterType.FLOAT32 )
         logger.config("_" + deviceName + "_" + name + "   " + mbRegisterOffset + "   2");
   }

}; 
