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
import java.util.logging.Handler;
import java.awt.GraphicsEnvironment;

import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.StopBits;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;

import sun.misc.Signal;
import sun.misc.SignalHandler;


public class Main {

    // Controllino I2C address
    public static final int CONTROLLINO_ADDR = 0x08; 
    private static final Logger logger = Logger.getLogger("Main");

    private static boolean isPi4jHardwareHost() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        return arch.contains("arm") || arch.contains("aarch64");
    }

    private static boolean shouldRunPi4jHardware() {
        return Boolean.getBoolean("scadarpi.forceHardware") || isPi4jHardwareHost();
    }

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
        logger.setUseParentHandlers(false);
        for (Handler handler : logger.getHandlers()) {
           logger.removeHandler(handler);
        }

         try {
             // create an appending file handler
             FileHandler fileHandler = new FileHandler("JPiTest.log");
             fileHandler.setFormatter(new SimpleFormatter());
             fileHandler.setLevel(Level.FINE);
             ConsoleHandler consoleHandler = new ConsoleHandler();
             consoleHandler.setLevel(Level.FINE);
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

        if (!shouldRunPi4jHardware()) {
           logger.log(Level.WARNING,
                      "Main: non-ARM host detected ({0}); hardware startup skipped. Set -Dscadarpi.forceHardware=true to override.",
                      System.getProperty("os.arch", ""));

           if (GraphicsEnvironment.isHeadless()) {
              logger.log(Level.WARNING, "Main: headless mode detected; GUI will not be created.");
              return;
           }

           // Start GUI-only mode for development hosts without Pi4J hardware.
           GlgGui mainGui = new GlgGui(deviceManager,"TUBE 600 WEST");
           Signal.handle(new Signal("INT"), new SignalHandler () {
              public void handle(Signal sig) {
                 logger.finer("Main: Interrupt received, Exiting program");
                 mainGui.exitProgram();
              }
           });
           return;
        }

        /**********************************************************************************************/
        // Create MaxiGauge device
        Device mg = new MaxiGauge("MG",
                                  0, // Modbus start offset (MaxiGauge is the first device created)
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT2AWG94-if00-port0",
                                  Baud._9600,
                                  DataBits._8,
                                  Parity.NONE,
                                  StopBits._1,
                                  FlowControl.NONE);

        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(mg);
        /**********************************************************************************************/
        // Create IonicAgilentDual device
        Device dual = new IonicAgilentDual("DUAL",
                                          mg.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT2AWG94-if02-port0",
                                          Baud._9600,
                                          DataBits._8,
                                          Parity.NONE,
                                          StopBits._1,
                                          FlowControl.NONE);

        // Add IonicAgilentDual to DeviceManager
        deviceManager.addDevice(dual);
        /**********************************************************************************************/
        // Create Controllino device (PLC interfacing the V31/V32 valves)
        Device controllino = new Controllino("I2C",
                                              dual.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_ADDR);

        // Add Controllino to DeviceManager
        deviceManager.addDevice(controllino);
        /**********************************************************************************************/
        // Start ModbusSlave thread
        ModbusSlaveThread modbusSlaveThread = new ModbusSlaveThread(deviceManager);
        /**********************************************************************************************/
        // Start (Maxigauge) MG device
        mg.doStart();
        // Start (IonicAgilentDual) dual device
        dual.doStart();
        // Start (Controllino) controllino device
        controllino.doStart();
        // Start modbusSlaveThread thread
        modbusSlaveThread.doStart();
        /**********************************************************************************************/
        // Start Glg GUI
        GlgGui mainGui = new GlgGui(deviceManager,"TUBE 600 WEST");

        // Handle CTRL-C interrupt to end cleanly the program
        Signal.handle(new Signal("INT"), new SignalHandler () {
           public void handle(Signal sig) {
              logger.finer("Main: Interrupt received, Exiting program");
              mainGui.exitProgram();
           }
        });
    }
}
