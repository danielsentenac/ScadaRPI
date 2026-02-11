/*
 * This Class is used to deinfo a data element
 * data type (1=read data,2=trigger command data,3=read/write data)
 * data register type (1=byte,2=short,3=int,4=float)
 */
import java.util.logging.Logger;

public interface DataTypes {

   public enum DataType {
   /*0*/   READ_ONLY_STATUS,
   /*1*/   COM_STATUS,
   /*2*/   READ_ONLY_VALUE,
   /*3*/   TRIGGER,
   /*4*/   READ_AND_WRITE_VALUE,
   /*5*/   READ_AND_WRITE_STATUS
   }

   public enum RegisterType {
   /*0*/   INT8,
   /*1*/   INT16,
   /*2*/   INT32,
   /*3*/   FLOAT32
   }

   public enum OpMode {
   /*0*/   AUTO,
   /*1*/   MANUAL,
   /*2*/   ALONE
   }
}; 
