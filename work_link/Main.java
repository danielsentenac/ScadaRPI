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
    private static final String mainTitle = "LINK";
    // Controllino_1 I2C address
    private static final int CONTROLLINO_1_ADDR = 0x08; 
    
    // Controllino_2 I2C address
    private static final int CONTROLLINO_2_ADDR = 0x09;
 
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
             FileHandler fileHandler = new FileHandler("LINKRack_" + LocalDate.now() + ".log");
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
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT3N28AS-if00-port0",
                                  Baud._9600, 
                                  DataBits._8,  
                                  Parity.NONE, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(mg);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create TurboPfeifferDCU device
        Device dculp = new TurboPfeifferDCU("DCULP",
                                          mg.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT3G3OAU-if00-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add TurboPfeifferDCU to DeviceManager
        deviceManager.addDevice(dculp);
        /**********************************************************************************************/
        // Create TurboPfeifferDCU device
        Device dculs = new TurboPfeifferDCU("DCULS",
                                          dculp.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT3G3OAU-if01-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add TurboPfeifferDCU to DeviceManager
        deviceManager.addDevice(dculs);
        /**********************************************************************************************/
        // Create Controllino_1 device
        Device controllino_1 = new Controllino_1("PRLINK",
                                              dculs.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_1_ADDR);
        
        // Add Controllino_1 to DeviceManager
        deviceManager.addDevice(controllino_1);
        /**********************************************************************************************/
        // Create Controllino_2 device
        Device controllino_2 = new Controllino_2("SRLINK",
                                              controllino_1.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_2_ADDR);
        
        // Add Controllino_2 to DeviceManager
        deviceManager.addDevice(controllino_2);
        /**********************************************************************************************/
        // Create Controllino_3 device
        Device controllino_3 = new Controllino_3("RACK",
                                              controllino_2.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_3_ADDR);
        
        // Add Controllino_3 to DeviceManager
        deviceManager.addDevice(controllino_3);
        /**********************************************************************************************/
        // Create TCMuxShield device
        Device tcmuxshield = new TCMuxShield("TCMUX",
                                              controllino_3.mbRegisterEnd); // Modbus start offset
        
        // Add Controllino_3 to DeviceManager
        deviceManager.addDevice(tcmuxshield);
        /**********************************************************************************************/
        // Create SupervisorClient device
        Device supervisorclient = new SupervisorClient("SC",
                                                       tcmuxshield.mbRegisterEnd); // Modbus start offset
        
        // Add Controllino_3 to DeviceManager
        deviceManager.addDevice(supervisorclient);
        /**********************************************************************************************/
        // Create Rga HAL201RC (from Hiden) device for LINK PR
        Device hal201rc = new RgaHidenHal201RC("RGAGe4",
        				      200,
                                              supervisorclient.mbRegisterEnd, // Modbus start offset
                                              "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT3N28AS-if03-port0",
                                              Baud._115200,
                                              DataBits._8,
                                              Parity.NONE,
                                              StopBits._1,
                                              FlowControl.NONE);

        // Add Hal201RC to DeviceManager
        deviceManager.addDevice(hal201rc);
        /**********************************************************************************************/
        // Create Rga HAL201RC (from Hiden) device for DET
        Device hal201rc2 = new RgaHidenHal201RC("RGAGc4",
                                              300,
                                              hal201rc.mbRegisterEnd, // Modbus start offset
                                              "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT3N28AS-if01-port0",
                                              Baud._115200,
                                              DataBits._8,
                                              Parity.NONE,
                                              StopBits._1,
                                              FlowControl.NONE);

        // Add Hal201RC2 to DeviceManager
        deviceManager.addDevice(hal201rc2);
        /**********************************************************************************************/
        // Start ModbusSlave thread
        ModbusSlaveThread modbusSlaveThread = new ModbusSlaveThread(deviceManager);
        /**********************************************************************************************/
        // Start (Maxigauge) MG device
        mg.doStart();
        // Start (TurboPfeifferDCU) dculp device
        dculp.doStart();
        // Start (TurboPfeifferDCU) dculs device
        dculs.doStart();
        // Start (Controllino_1) controllino device
        controllino_1.doStart();
        // Start (Controllino_2) controllino device
        controllino_2.doStart();
        // Start (Controllino_3) controllino device
        controllino_3.doStart();
        // Start (TCMuxShield) temperature device
        tcmuxshield.doStart();
        // Start (SupervisorClient) temperature device
        supervisorclient.doStart();
        // Start (RgaHidenHal201RC) rga device pr link
        hal201rc.doStart();
        // Start (RgaHidenHal201RC) rga device det
        hal201rc2.doStart();
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
