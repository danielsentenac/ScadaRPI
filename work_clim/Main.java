/*
 * This is the main class where all threads are configured & started
 */

import java.io.IOException;
import java.util.Arrays;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import com.pi4j.platform.PlatformAlreadyAssignedException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.Date; 
import java.time.LocalDate; // import the LocalDate class

import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.StopBits;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;

import com.intelligt.modbus.jlibmodbus.serial.SerialPort;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import com.pi4j.io.gpio.*;


public class Main {

    private static final Logger logger = Logger.getLogger("Main");

    /**
     * Program Test Main Entry Point
     *
     * @param args
     * @throws InterruptedException
     * @throws PlatformAlreadyAssignedException
     * @throws IOException
     * @throws UnsupportedBusNumberException
     */
    public static void main(String[] args) 
                 throws InterruptedException, 
                        PlatformAlreadyAssignedException, 
                        IOException, 
                        UnsupportedBusNumberException {

        
        logger.setLevel(Level.FINE);

         try {
             // create an appending file handler
             FileHandler fileHandler = new FileHandler("Data_" + LocalDate.now() + ".log");
             fileHandler.setFormatter(new SimpleFormatter() {
                private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";
                @Override
                public synchronized String format(LogRecord lr) {
                   return String.format(format,
                                        new Date(lr.getMillis()),
                                        lr.getLevel().getLocalizedName(),
                                        lr.getMessage());
                }
             });
             //fileHandler.setLevel(Level.FINER);
             // create an appending console handler
             ConsoleHandler consoleHandler = new ConsoleHandler();
             consoleHandler.setFormatter(new SimpleFormatter() {
                private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";
                @Override
                public synchronized String format(LogRecord lr) {
                   return String.format(format,
                                        new Date(lr.getMillis()),
                                        lr.getLevel().getLocalizedName(),
                                        lr.getMessage());
                }
             });
             //consoleHandler.setLevel(Level.FINER);
             // add to the desired loggers
             logger.addHandler(fileHandler);
             logger.addHandler(consoleHandler);
        } catch (IOException e) {
           logger.log(Level.SEVERE, "Unable to setup logging to debug. No logging will be done. Error: ");
           e.printStackTrace();
        }
        // print program title/header
        logger.finer("<-- JPiMain Tests-->");
        
        // Create DeviceManager object
        DeviceManager deviceManager = new DeviceManager();
        /**********************************************************************************************/
        // Create DS2438IN device
        Device DS2438IN = new DS2438("DS2438IN", 5000, 
                                      0,  // Modbus start offset (DS2438IN is the first device created)
                                      "/sys/bus/w1/devices/26-00000213e161");  
        
        // Add DS2438IN to DeviceManager
        deviceManager.addDevice(DS2438IN);

        /**********************************************************************************************/
        // Create DS2438OUT device
        Device DS2438OUT = new DS2438("DS2438OUT", 5000,
                                       DS2438IN.mbRegisterEnd, // Modbus start offset
                                       "/sys/bus/w1/devices/26-00000213df85");
        
        // Add DS2438OUT to DeviceManager
        deviceManager.addDevice(DS2438OUT);
   
        /**********************************************************************************************/
        // Create PWM device
        Device PWM = new PWM("PWM", 1000,
                              DS2438OUT.mbRegisterEnd, // Modbus start offset
                              5, 
                              RaspiPin.GPIO_26);
        
        // Add DS2438OUT to DeviceManager
        deviceManager.addDevice(PWM);

        /**********************************************************************************************/
        // Create IntesisBox device
        Device intesisBox = new IntesisBox("INTESISBOX", 5000, 
                                            PWM.mbRegisterEnd,
                                            "/dev/serial/by-id/usb-Silicon_Labs_CP2102_USB_to_UART_Bridge_Controller_0001-if00-port0",
                                            SerialPort.BaudRate.BAUD_RATE_19200,
                                            8,
                                            SerialPort.Parity.NONE,
                                            1);
        
        // Add IntesisBox to DeviceManager
        deviceManager.addDevice(intesisBox);

        /**********************************************************************************************/
        // Create Operation instance
        Operation op = new Operation(deviceManager, 5);

        /**********************************************************************************************/
        // Start ModbusSlave thread
        ModbusSlaveThread modbusSlaveThread = new ModbusSlaveThread(deviceManager);
        /**********************************************************************************************/
        // Start DS2438IN device
        DS2438IN.doStart();

        // Start DS2438OUT device
        DS2438OUT.doStart();

        // Start PWM device
        PWM.doStart();

        // Start IntesisBox device
        intesisBox.doStart();

        // Start modbusSlaveThread thread
        modbusSlaveThread.doStart();
        /**********************************************************************************************/
        // Start Glg GUI
        GlgGui mainGui = new GlgGui(op, "Temperature & Humidity Control Station");

        // Handle CTRL-C interrupt to end cleanly the program
        Signal.handle(new Signal("INT"), new SignalHandler () {
           public void handle(Signal sig) {
              logger.finer("Main: Interrupt received, Exiting program");
              mainGui.exitProgram();
           }
        });
    }
}
