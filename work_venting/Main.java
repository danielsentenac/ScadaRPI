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

import sun.misc.Signal;
import sun.misc.SignalHandler;


public class Main {

    // Main Title GUI
    private static final String mainTitle = "VENTING STATION 1";
    
     // Controllino_1 I2C address
    private static final int CONTROLLINO_1_ADDR = 0x08; 

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
             FileHandler fileHandler = new FileHandler("VENTING_" + LocalDate.now() + ".log");
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
        /**********************************************************************************************/
        // Create FlowMeterModbusMaster device for MKS 2000
        Device fm2000 = new FlowMeterModbusMaster("MKS2000",
        					  "192.168.2.155",
        					  502,
        					  0); // Modbus start offset
        
        // Add fm2000 to DeviceManager
        deviceManager.addDevice(fm2000);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create FlowMeterModbusMaster device for MKS 50000
        Device fm50000 = new FlowMeterModbusMaster("MKS50000",
        					   "192.168.3.155",
        					   502,
        					   fm2000.mbRegisterEnd); // Modbus start offset
        
        // Add fm2000 to DeviceManager
        deviceManager.addDevice(fm50000);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create Controllino_1 device
        Device controllino_1 = new Controllino_1("M1",
                                                 fm50000.mbRegisterEnd, // Modbus start offset
                                                 CONTROLLINO_1_ADDR);
        
        // Add Controllino_1 to DeviceManager
        deviceManager.addDevice(controllino_1);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create MaxiGauge device
        Device mg = new MaxiGauge("MG",
                                  controllino_1.mbRegisterEnd, // Modbus start offset (MaxiGauge is the first device created)
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT79IY3R-if00-port0",
                                  Baud._9600, 
                                  DataBits._8,  
                                  Parity.NONE, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(mg);
        /**********************************************************************************************/
       
        // Create Operation instance
        Operation op2000 = new Operation(fm2000, 5);
        /**********************************************************************************************/
        // Create Operation instance
        Operation op50000 = new Operation(fm50000, 5);
        /**********************************************************************************************/
        // Start ModbusSlave thread
        ModbusSlaveThread modbusSlaveThread = new ModbusSlaveThread(deviceManager);
        /**********************************************************************************************/
        // Start
        fm2000.doStart();
        /**********************************************************************************************/
        // Start  (FlowMeterModbusMaster) MKS50000 device
        fm50000.doStart();
        /**********************************************************************************************/
        // Start Controllino_1 VALVE device
        controllino_1.doStart();
        /**********************************************************************************************/
        // Start mg MAXIGAUGE device
        mg.doStart();
        // Start modbusSlaveThread thread
        modbusSlaveThread.doStart();
        /**********************************************************************************************/
        // Start Glg GUI
        GlgGui mainGui = new GlgGui(deviceManager,op2000,op50000,mainTitle);

        // Handle CTRL-C interrupt to end cleanly the program
        Signal.handle(new Signal("INT"), new SignalHandler () {
           public void handle(Signal sig) {
              logger.finer("Main: Interrupt received, Exiting program");
              mainGui.exitProgram();
           }
        });
    }
}
