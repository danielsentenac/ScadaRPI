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
import java.time.LocalDateTime; // import the LocalDate class
import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.StopBits;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;

import sun.misc.Signal;
import sun.misc.SignalHandler;


public class Main {

    // Main Title GUI
    private static final String mainTitle = "TUBE 1500W Station";
    // Controllino_1 I2C address
    private static final int CONTROLLINO_1_ADDR = 0x08; // Pumping Module
    
    // Controllino_2 I2C address
    private static final int CONTROLLINO_2_ADDR = 0x09; // Sicem Module

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
             FileHandler fileHandler = new FileHandler("TUBE1500WRack_" + LocalDateTime.now() + ".log");
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
        // Create MaxiGauge device
        Device mg = new MaxiGauge("MG",
                                  0, // Modbus start offset (MaxiGauge is the first device created)
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT5NPQL3-if02-port0",
                                  Baud._9600, 
                                  DataBits._8,  
                                  Parity.NONE, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(mg);
        /**********************************************************************************************/
        // Create XGSGauge device
        Device xgs = new XGSGauge("XGS",
                                  mg.mbRegisterEnd, // Modbus start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT5NPQL3-if00-port0",
                                  Baud._9600, 
                                  DataBits._8,  
                                  Parity.NONE, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add XGSGauge to DeviceManager
        deviceManager.addDevice(xgs);
        /**********************************************************************************************/
        // Create Sicem device
        Device sicem = new Sicem("SICEM",
                                 xgs.mbRegisterEnd, // Modbus start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT5NPQL3-if01-port0",
                                  Baud._9600, 
                                  DataBits._8,  
                                  Parity.NONE, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add XGSGauge to DeviceManager
        deviceManager.addDevice(sicem);
        /**********************************************************************************************
        /**********************************************************************************************/
        // Create TurboPfeifferDCU device
        Device dcu = new TurboPfeifferDCU("DCU",
                                          sicem.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT4JD8P2-if01-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add TurboPfeifferDCU to DeviceManager
        deviceManager.addDevice(dcu);
        /**********************************************************************************************/
        // Create Controllino_1 device
        Device controllino_1 = new Controllino_1("M1",
                                              dcu.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_1_ADDR);
        
        // Add Controllino_1 to DeviceManager
        deviceManager.addDevice(controllino_1);
        /**********************************************************************************************/
        // Create Controllino_2 device
        Device controllino_2 = new Controllino_2("M2",
                                              controllino_1.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_2_ADDR);
        
        // Add Controllino_2 to DeviceManager
        deviceManager.addDevice(controllino_2);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create TCMuxShield device
        Device tcmuxshield = new TCMuxShield("TCMUX",
                                              controllino_2.mbRegisterEnd); // Modbus start offset
        
        // Add Controllino_3 to DeviceManager
        deviceManager.addDevice(tcmuxshield);
         /**********************************************************************************************/
        // Create MksRotor device
        Device rot = new MksRotor("ROT",
                                  tcmuxshield.mbRegisterEnd, // Modbus start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT5NPQL3-if03-port0",
                                  Baud._9600, 
                                  DataBits._8,  
                                  Parity.NONE, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add XGSGauge to DeviceManager
        deviceManager.addDevice(rot);
        /**********************************************************************************************/
        /**********************************************************************************************
        // Create SupervisorClient device
        Device rgamks = new RgaMksSupervisorClient("MKS",
                                             rot.mbRegisterEnd); // Modbus start offset
        
        // Add Controllino_3 to DeviceManager
        deviceManager.addDevice(rgamks);*/
        /**********************************************************************************************/
        // Create Operation instance
        Operation op = new Operation(deviceManager, 5);
        /**********************************************************************************************/
        // Start ModbusSlave thread
        ModbusSlaveThread modbusSlaveThread = new ModbusSlaveThread(deviceManager);
        /**********************************************************************************************/
        // Start (Maxigauge) MG device
        mg.doStart();
        // Start (XGSgauge) XGS device
        xgs.doStart();
        // Start (SICEM) Sicem device
        sicem.doStart();
        // Start (TurboPfeifferDCU) TP device
        dcu.doStart();
        // Start (Controllino_1) controllino device (Pumping)
        controllino_1.doStart();
        // Start (Controllino_2) controllino device (Sicem and internal TCMuxShield digital control)
        controllino_2.doStart();
        // Start (TCMuxShield) temperature device
        tcmuxshield.doStart();
        // Start (MksRotor) ROT device
        rot.doStart();
        // Start (RGAMKS) temperature device
        //rgamks.doStart();
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
