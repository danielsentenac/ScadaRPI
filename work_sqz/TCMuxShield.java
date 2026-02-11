/*
 * This Class is the implementation of the TCMuxShield device
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
import com.pi4j.io.gpio.*;

public class TCMuxShield extends Device {

   private I2C_Comm i2c;
   private static final Logger logger = Logger.getLogger("Main");

   private final GpioController gpio = GpioFactory.getInstance();

   private GpioPinDigitalOutput pinen = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02);
   private GpioPinDigitalOutput pina0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05);
   private GpioPinDigitalOutput pina1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06);
   private GpioPinDigitalOutput pina2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_21);
   private GpioPinDigitalInput pinso = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00);
   private GpioPinDigitalOutput pincs = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_22);
   private GpioPinDigitalOutput pinsc = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_23);

   private int OPEN = -1000;
   private int SHORT = -1001;

   public TCMuxShield (String _name,
                       int _mbRegisterStart) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("TCMuxShield:TCMuxShield> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;
  
     // Value
     addDataElement( new DataElement(name, "TEMP0", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     addDataElement( new DataElement(name, "TEMP1", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TEMP2", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TEMP3", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TEMP4", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TEMP5", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TEMP6", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TEMP7", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TEMP8", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));

     // Controller TCMuxShield comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=2));

     mbRegisterEnd+=1;

     logger.finer("TCMuxShield:TCMuxShield> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     // Init SPI communication
     pinen.high();
     pinsc.low();
     pincs.low();
   }
   
   public void updateDeviceData() {
   
     // Get monitoring data from device using SPI Comm
     Vector<DataElement> temp = new Vector<DataElement>();

     for (int i = 0 ; i < 9; i++)
        temp.add(getDataElement("TEMP" + Integer.toString(i)));
     
     DataElement dcom = getDataElement("COMST");
     logger.finer("TCMuxShield:updateDeviceData> GO!");
     try {
        byte ch = 0;
        for (int i = 0 ; i < temp.size(); i++) {
            logger.finer("TCMuxShield:updateDeviceData> ch = " + ch);
           // Temp sensor address select
           if ((ch & 1) != 0) pina0.high();
           else pina0.low();
           if ((ch & 2) != 0) pina1.high();
           else pina1.low();
           if ((ch & 4) != 0) pina2.high();
           else pina2.low();
           // Wait a while for the capacitor on the ADC input to charge
           Thread.sleep(20);
           // Begin conversion
           pincs.high();
           // Wait 100 mS for conversion to complete
           Thread.sleep(200);
           // Stop conversion, start serial interface
           pincs.low();
           int rawTC = Short.toUnsignedInt(readSPI());
           int rawIT = Short.toUnsignedInt(readSPI());
           if ((rawTC & 1) != 0) {
              if ((rawIT & 1 ) != 0) {
                 temp.elementAt(ch).value = OPEN;
              }
              if ((rawIT & 6) != 0) {
                 temp.elementAt(ch).value = SHORT;
              }
           }
           else 
              temp.elementAt(ch).value = (float) rawTC / 16;
           logger.finer("TCMuxShield:updateDeviceData> Temp[" + ch + "]=" +  temp.elementAt(ch).value);
           if (++ch == 8) {
              temp.elementAt(ch).value = (float) rawIT / 256; // internal temperature reduced to quarter degree C
              logger.finer("TCMuxShield:updateDeviceData> INTERNAL Temp[" + ch + "]=" +  temp.elementAt(ch).value);
              ch = 0;
           }
        }
        // Arrived here comm ok
        dcom.value = 0;
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("Controllino_1:updateDeviceData> Communication with " + name + " back!");
        }
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "TCMuxShield:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "TCMuxShield:updateDeviceData>" + ex.getMessage());
        }
        setErrorComStatus();
     }

   }
   
   public void executeCommand( DataElement e ) {
       
   }

  short readSPI() {
     short v = 0;
     for (byte i = 16; i != 0; i--) {
       v <<= 1;
       pinsc.high();
       // 100nS min. delay implied
       if ( pinso.isHigh())
          v |= 1;
       else
          v |=0;
       pinsc.low();   // request next serial bit
       // 100nS min. delay implied
     }
     return v;
   }
}; 
