/*
 * This Class is the implementation of the PWM device (Temperature & Humidity)
 *
 */
import java.util.*;
import java.io.IOException;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;

import com.pi4j.io.gpio.*;

public class PWM extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   private Pin address;
   private double vOut = 0;
   public double vMax;
   private GpioPinPwmOutput pwm;

   public PWM (String _name, 
	       int _delay,
               int _mbRegisterStart, 
               double _vMax, 
               Pin _address) {

     name = _name;       // Device name
     delay = _delay;      // delay between 2 queries
     address = _address; // Device pinout address
     vMax = _vMax;       // Device pinout max voltage
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("PWM:PWM> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // PWM Voltage
     addDataElement( new DataElement(name, "VOUT",DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     // Controller PWM comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("PWM:PWM> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     // Initialize Gpio & PWM
     // create GPIO controller instance
     pwm = gpio.provisionPwmOutputPin(address, "PWM", 0);
     com.pi4j.wiringpi.Gpio.pwmSetMode(com.pi4j.wiringpi.Gpio.PWM_MODE_MS);
     com.pi4j.wiringpi.Gpio.pwmSetRange(100);
     com.pi4j.wiringpi.Gpio.pwmSetClock(500);
     pwm.setPwm(0);
     
     DataElement v = getDataElement("VOUT");   
     v.value = vOut = 0;
     getDataElement("COMST").value = 0;
   }
   public void doStop() {
      if (thread != null) thread.interrupt();
      // Change the states of variable
      thread = null;
      gpio.shutdown();
      gpio.unprovisionPin(pwm);
   }
   public void updateDeviceData() {
     popCommand();  // Execute commands
   }
   
   public void executeCommand( DataElement e ) {
      logger.finer("PWM:executeCommand> change new value " + e.value);
      vOut = 100 * (e.value / vMax);
      logger.finer("PWM:executeCommand> write new vOut " + vOut);
      pwm.setPwm((int)vOut);
   }
}; 
