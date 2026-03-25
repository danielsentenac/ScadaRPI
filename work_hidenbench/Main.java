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
import java.awt.GraphicsEnvironment;

import sun.misc.Signal;
import sun.misc.SignalHandler;


public class Main {

    // Main Title GUI
    private static final String mainTitle = "HIDEN BENCH";
    // Controllino_1 I2C address
    private static final int CONTROLLINO_1_ADDR = 0x08; 

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

         try {
             // create an appending file handler
             FileHandler fileHandler = new FileHandler("HIDENBenchRack_" + LocalDate.now() + ".log");
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
             // add to the desired loggers
             logger.addHandler(fileHandler);
             
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

           GlgGui mainGui = new GlgGui(deviceManager,mainTitle);
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
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus1_FT4263QQ-if00-port0",
                                  Baud._9600, 
                                  DataBits._8,  
                                  Parity.NONE, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(mg);
        /**********************************************************************************************/
        // Create TurboVarianV81AG device
        Device Turbo_a = new TurboVarianV81AG("TurboA",
                                          mg.mbRegisterEnd, // Modbus start offset
                                         "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus1_FT420PC2-if00-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add TurboVarianV81AG to DeviceManager
        deviceManager.addDevice(Turbo_a);
        /**********************************************************************************************/
        // Create TurboPfeifferDCU device
        Device Turbo_b = new TurboPfeifferDCU("TurboB",
                                          Turbo_a.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus1_FT426B90-if00-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add TurboVarianV81AG to DeviceManager
        deviceManager.addDevice(Turbo_b);
        /**********************************************************************************************/
        // Create Controllino_1 device (controls valves, dry pumps and bypass)
        Device controllino_1 = new Controllino_1("M1",
                                              Turbo_b.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_1_ADDR);
        
        // Add Controllino_1 to DeviceManager
        deviceManager.addDevice(controllino_1);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Start ModbusSlave thread
        ModbusSlaveThread modbusSlaveThread = new ModbusSlaveThread(deviceManager);
        /**********************************************************************************************/
        // Start (Maxigauge) MG device
        mg.doStart();
        // Start (TurboVarianV81AG) Turbo_a device
        Turbo_a.doStart();
        // Start (TurboPfeiferDCU) Turbo_b device
        Turbo_b.doStart();
        // Start (Controllino_1) controllino device
        controllino_1.doStart();
        
        // Start modbusSlaveThread thread
        modbusSlaveThread.doStart();
        /**********************************************************************************************/
        // Start Glg GUI
        GlgGui mainGui = new GlgGui(deviceManager,mainTitle);

        // Handle CTRL-C interrupt to end cleanly the program
        Signal.handle(new Signal("INT"), new SignalHandler () {
           public void handle(Signal sig) {
              logger.finer("Main: Interrupt received, Exiting program");
              mainGui.exitProgram();
           }
        });
    }
}
