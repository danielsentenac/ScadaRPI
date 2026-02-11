/*
 * This is the main class where all threads are configured & started
 */

import java.io.IOException;
import java.util.*;
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


public class Main {

    // Main Title GUI
    private static final String mainTitle = "CENTRAL BUILDING PARTICLE COUNTERS";
 
    // Controllino_3 I2C address
    private static final int CONTROLLINO_3_ADDR = 0x10;

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
             FileHandler fileHandler = new FileHandler("PCOUNTERRack_" + LocalDate.now() + ".log");
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
        // Create Particle Counter device
        Device pc1 = new PCounterMasterRTU("PC1", "Injection L.",
                                  0, // PCounter start offset ( is the first device created)
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT79GFGK-if00-port0",
                                  SerialPort.BaudRate.BAUD_RATE_115200,
                                  8,  
                                  SerialPort.Parity.NONE, 
                                  1);
        // Add Particle Counter to DeviceManager
        deviceManager.addDevice(pc1);
        /**********************************************************************************************/
        // Create Particle Counter device
        Device pc2 = new PCounterMasterRTU("PC2", "Baseroom C.",
                                  pc1.mbRegisterEnd, // PCounter start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT79GFGK-if01-port0",
                                  SerialPort.BaudRate.BAUD_RATE_115200,
                                  8,  
                                  SerialPort.Parity.NONE, 
                                  1);
        
        // Add Particle Counter to DeviceManager
        deviceManager.addDevice(pc2);
        /**********************************************************************************************/
        // Create Particle Counter device
        Device pc3 = new PCounterMasterRTU("PC3", "Detect. Sas",
                                  pc2.mbRegisterEnd, // PCounter start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT79GFGK-if02-port0",
                                  SerialPort.BaudRate.BAUD_RATE_115200,
                                  8,  
                                  SerialPort.Parity.NONE, 
                                  1);
        
        // Add Particle Counter to DeviceManager
        deviceManager.addDevice(pc3);
        /**********************************************************************************************/
        // Create Particle Counter device
        Device pc4 = new PCounterMasterRTU("PC4", "Main Hall",
                                  pc3.mbRegisterEnd, // PCounter start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT79GFGK-if03-port0",
                                  SerialPort.BaudRate.BAUD_RATE_115200,
                                  8,  
                                  SerialPort.Parity.NONE, 
                                  1);
        
        // Add Particle Counter to DeviceManager
        deviceManager.addDevice(pc4);
        /**********************************************************************************************/
        // Create Particle Counter device
        Device pc5 = new PCounterMasterRTU("PC5", "Detection L.",
                                  pc4.mbRegisterEnd, // PCounter start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT79GJAF-if00-port0",
                                  SerialPort.BaudRate.BAUD_RATE_115200,
                                  8,  
                                  SerialPort.Parity.NONE, 
                                  1);
        
        // Add Particle Counter to DeviceManager
        deviceManager.addDevice(pc5);
        /**********************************************************************************************/
        // Create Particle Counter device
        Device pc6 = new PCounterMasterRTU("PC6", "Sas Clean R.",
                                  pc5.mbRegisterEnd, // PCounter start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT79GJAF-if01-port0",
                                  SerialPort.BaudRate.BAUD_RATE_115200,
                                  8,  
                                  SerialPort.Parity.NONE, 
                                  1);
        
        // Add Particle Counter to DeviceManager
        deviceManager.addDevice(pc6);
        /**********************************************************************************************/
        // Create Particle Counter device
        Device pc7 = new PCounterMasterRTU("PC7", "Payload C.",
                                  pc6.mbRegisterEnd, // PCounter start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT79GJAF-if02-port0",
                                  SerialPort.BaudRate.BAUD_RATE_115200,
                                  8,  
                                  SerialPort.Parity.NONE, 
                                  1);
        
        // Add Particle Counter to DeviceManager
        deviceManager.addDevice(pc7);
        /**********************************************************************************************/
        // Create Particle Counter device
        Device pc8 = new PCounterMasterRTU("PC8", "Mirror C.",
                                  pc7.mbRegisterEnd, // PCounter start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT79GJAF-if03-port0",
                                  SerialPort.BaudRate.BAUD_RATE_115200,
                                  8,  
                                  SerialPort.Parity.NONE, 
                                  1);
        
        // Add Particle Counter to DeviceManager
        deviceManager.addDevice(pc8);
        /**********************************************************************************************/
        // Create Controllino_3 device
        Device controllino_3 = new Controllino_3("M3",
                                              pc8.mbRegisterEnd, // Controllino_3 start offset
                                              CONTROLLINO_3_ADDR);
        
        // Add Controllino_3 to DeviceManager
        deviceManager.addDevice(controllino_3);
        /**********************************************************************************************/
        // Create TCMuxShield device
        Device tcmuxshield = new TCMuxShield("TCMUX",
                                              controllino_3.mbRegisterEnd); // TCMuxShield start offset
        
        // Add tcmuxshield to DeviceManager
        deviceManager.addDevice(tcmuxshield);
         /**********************************************************************************************/
        // Create Operation instance
        Operation op = new Operation(deviceManager, 1);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Start ModbusSlave thread
        ModbusSlaveThread modbusSlaveThread = new ModbusSlaveThread(deviceManager);
        /**********************************************************************************************/
        // Start (ParticlePlus) PC1 device
        pc1.doStart();
        // Start (ParticlePlus) PC2 device
        pc2.doStart();
        // Start (ParticlePlus) PC3 device
        pc3.doStart();
        // Start (ParticlePlus) PC4 device
        pc4.doStart();
        // Start (ParticlePlus) PC5 device
        pc5.doStart();
        // Start (ParticlePlus) PC6 device
        pc6.doStart();
        // Start (ParticlePlus) PC7 device
        pc7.doStart();
        // Start (ParticlePlus) PC8 device
        pc8.doStart();
        // Start (Controllino_3) controllino device
        controllino_3.doStart();
        // Start (TCMuxShield) temperature device
        tcmuxshield.doStart();
        // Start modbusSlaveThread thread
        modbusSlaveThread.doStart();
        /**********************************************************************************************/
       
        // Start Glg GUI
        GlgGui mainGui = new GlgGui(op,mainTitle);

        // Handle CTRL-C interrupt to end cleanly the program
        Signal.handle(new Signal("INT"), new SignalHandler () {
           public void handle(Signal sig) {
              logger.finer("Main: Interrupt received, Exiting program");
              mainGui.exitProgram();
           }
        });
    }
}
