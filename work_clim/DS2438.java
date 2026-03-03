/*
 * This Class is the implementation of the DS2438 device (Temperature & Humidity)
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
import java.io.FileReader;
import java.io.BufferedReader;

import com.pi4j.io.gpio.*;

public class DS2438 extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   private String address;
   private int errCnt = 0;
   private GpioPinDigitalOutput onoff;
   private static boolean onoffInit = false;
   private static boolean onoffReset = false;
   private boolean hasOffOn = false;

   public DS2438 (String _name, 
		  int _delay,
                  int _mbRegisterStart,
                  String _address) {

     name = _name; // Device name
     delay = _delay;      // delay between 2 queries
     address = _address;  // Device address
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("DS2438:DS2438> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Temperature & Humidity 
     addDataElement( new DataElement(name, "TEMP",DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "HUM",DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     // Controller DS2438 comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("DS2438:DS2438> " + name + " Modbus registers ends at offset " + mbRegisterEnd);
    
     if ( onoffInit == false ) {
        if (gpio != null) {
           // Switch on sensor with GPIO_20
           logger.fine("DS2438:DS2438> Init Switch On/Off..." + name);
           onoff = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_28, PinState.HIGH);
           onoffInit = true;
           hasOffOn = true;
        } else {
           logger.log(Level.INFO, "DS2438:DS2438> GPIO unavailable; OneWire power toggle disabled.");
        }
     }
   }
   public void doStop() {
      if (thread != null) thread.interrupt();
      // Change the states of variable
      thread = null;
      if (gpio != null && onoff != null) {
         gpio.shutdown();
         gpio.unprovisionPin(onoff);
      }
   }
   public void updateDeviceData() {
   
     // Get monitoring data from device using OneWire Comm

     DataElement t = getDataElement("TEMP");
     DataElement h = getDataElement("HUM");
     DataElement dcom = getDataElement("COMST");

     // Read Temperature
     BufferedReader reader = null;
     FileReader file = null;
     try {
        file = new FileReader(address + "/temperature");
        reader = new BufferedReader(file);
        String valueStr = "";
        valueStr = reader.readLine();
        try {
           t.value = Integer.parseInt(valueStr);
           t.value/=256;
        }
        catch (NumberFormatException ex) {
           logger.log(Level.SEVERE,"DS2438::updateDeviceData> Not a double:" + valueStr);
           t.value = 0;
        }  	
        if (comErr == true) {
           logger.log(Level.WARNING, "DS2438:updateDeviceData> Communication with " + name + " is back");
           comErr = false;
           dcom.value = 0; //ERR COM
           errCnt = 0;
        }
     }
     catch (Exception ex) {
        // Switch Off & On the device
        try {
           if ( onoffReset == false && hasOffOn == true ) {
              onoffReset = true;
              logger.log(Level.WARNING, "DS2438:updateDeviceData> Switching On/Off OneWire..." + name);
              onoff.low();
              Thread.sleep(3000);
              onoff.high();
              Thread.sleep(5000);
              onoffReset = false;
           }
        }
        catch (Exception e) {
           logger.log(Level.SEVERE, "DS2438:updateDeviceData> " + e.getMessage());
        }
        
        if (comErr == false && ++errCnt >= 10) {
           logger.log(Level.WARNING, "DS2438:updateDeviceData> Communication with " + name + " is interrupted");
           comErr = true;
           dcom.value = 1; //ERR COM
           setErrorComStatus();
           ex.printStackTrace();
        }
     }
     finally {
        try {
           if (reader != null)
              reader.close();
           if (file != null)
              file.close();
        } catch (Exception ex) {
           System.err.format("Exception: %s%n", ex);
        }
     }
     // Read Humidity
     reader = null;
     file = null;
     float vad = 0;
     try {
        file = new FileReader(address + "/vad");
        reader = new BufferedReader(file);
        String valueStr = "";
        valueStr = reader.readLine();
        try {
           vad = Integer.parseInt(valueStr);
        }
        catch (NumberFormatException ex) {
           logger.log(Level.SEVERE,"DS2438::updateDeviceData> Not a double:" + valueStr);
           vad = 0;
        }  	
        if (comErr == true) {
           logger.log(Level.WARNING, "DS2438:updateDeviceData> Communication with " + name + " is back");
           comErr = false;
           dcom.value = 0; //ERR COM
        }
     }
     catch (Exception ex) {
        // Switch Off & On the device
        try {
           if ( onoffReset == false && hasOffOn == true) {
              onoffReset = true;
              logger.log(Level.WARNING, "DS2438:updateDeviceData> Switching On/Off OneWire..." + name);
              onoff.low();
              Thread.sleep(3000);
              onoff.high();
              Thread.sleep(5000);
              onoffReset = false;
           }
        }
        catch (Exception e) {
           logger.log(Level.SEVERE, "DS2438:updateDeviceData> " + e.getMessage());
        }
        if (comErr == false && ++errCnt >= 10) {
           logger.log(Level.WARNING, "DS2438:updateDeviceData> Communication with " + name + " is interrupted");
           comErr = true;
           dcom.value = 1; //ERR COM
           setErrorComStatus();
           ex.printStackTrace();
        }
     }
     finally {
        try {
           if (reader != null)
              reader.close();
           if (file != null)
              file.close();
        } catch (Exception ex) {
           System.err.format("Exception: %s%n", ex);
        }
     }
     float vdd = 0;
     reader = null;
     file = null;
     try {
        file = new FileReader(address + "/vdd");
        reader = new BufferedReader(file);
        String valueStr = "";
        valueStr = reader.readLine();
        try {
           vdd = Integer.parseInt(valueStr);
        }
        catch (NumberFormatException ex) {
           logger.log(Level.SEVERE,"DS2438::updateDeviceData> Not a double:" + valueStr);
           vdd = 0;
        }
        if (comErr == true) {
           logger.log(Level.WARNING, "DS2438:updateDeviceData> Communication with " + name + " is back");
           comErr = false;
           dcom.value = 0; //ERR COM
        }
     }
     catch (Exception ex) {
        // Switch Off & On the device
        try {
           if ( onoffReset == false && hasOffOn == true ) {
              onoffReset = true;
              logger.log(Level.WARNING, "DS2438:updateDeviceData> Switching On/Off OneWire..." + name);
              onoff.low();
              Thread.sleep(3000);
              onoff.high();
              Thread.sleep(5000);
              onoffReset = false;
           }
        }
        catch (Exception e) {
           logger.log(Level.SEVERE, "DS2438:updateDeviceData> " + e.getMessage());
        }
        if (comErr == false && ++errCnt >= 10) {
           logger.log(Level.WARNING, "DS2438:updateDeviceData> Communication with " + name + " is interrupted");
           comErr = true;
           dcom.value = 1; //ERR COM
           setErrorComStatus();
           ex.printStackTrace();
        }
     }
     finally {
        try {
           if (reader != null)
              reader.close();
           if (file != null)
              file.close();
        } catch (Exception ex) {
           System.err.format("Exception: %s%n", ex);
        }
     }
     if (vad != 0 && vdd != 0 && t.value != 0) {
        vad/=100;
        vdd/=100;
        double value = (vad / vdd - 0.1515) / 0.00636 / (1.0546 - 0.00216 * t.value);
        if (value >= 0 && value <= 100) // Humidity must lie within this limits
           h.value = value;
     }
   }
   
   public void executeCommand( DataElement e ) {
   }
}; 
