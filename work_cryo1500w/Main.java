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
    private static final String mainTitle = "CRYO 1500W Station";
    // Controllino_1 I2C address
    private static final int CONTROLLINO_1_ADDR = 0x08; 
    
    // Controllino_2 I2C address
    private static final int CONTROLLINO_2_ADDR = 0x09;

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
             FileHandler fileHandler = new FileHandler("CRYO1500WRack_" + LocalDate.now() + ".log");
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
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT7WE0LG-if00-port0",
                                  Baud._9600, 
                                  DataBits._8,  
                                  Parity.NONE, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(mg);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create LakeShore device
        Device ls1 = new LakeShore("LS1",
                                  mg.mbRegisterEnd, // Modbus start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT3N55N1-if00-port0",
                                  Baud._9600, 
                                  DataBits._7,  
                                  Parity.ODD, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(ls1);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create LakeShore device
        Device ls2 = new LakeShore("LS2",
                                  ls1.mbRegisterEnd, // Modbus start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT3N55N1-if01-port0",
                                  Baud._9600, 
                                  DataBits._7,  
                                  Parity.ODD, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(ls2);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create LakeShore device
        Device ls3 = new LakeShore("LS3",
                                  ls2.mbRegisterEnd, // Modbus start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT3N55N1-if02-port0",
                                  Baud._9600, 
                                  DataBits._7,  
                                  Parity.ODD, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(ls3);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create TurboVarianV81AG device
        Device tpi = new TurboVarianV81AG("TPI",
                                          ls3.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT3N55N1-if03-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add TurboPfeifferDCU to DeviceManager
        deviceManager.addDevice(tpi);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create TurboVarianV81AG device
        Device tpo = new TurboVarianV81AG("TPO",
                                          tpi.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT7WE0LG-if01-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add TurboPfeifferDCU to DeviceManager
        deviceManager.addDevice(tpo);
        /**********************************************************************************************/
        // Create Controllino_1 device for pumping controls (Valves & Dry pump)
        Device controllino_1 = new Controllino_1("M1",
                                              tpo.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_1_ADDR);
        
        // Add Controllino_1 to DeviceManager
        deviceManager.addDevice(controllino_1);
        /**********************************************************************************************/
        // Create Controllino_2 device for Chiller controls
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
        /**********************************************************************************************/
        // Create Chiller device
        Device chiller = new Chiller("CHILLER",
                                 tcmuxshield.mbRegisterEnd, // Modbus start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT7WE0LG-if02-port0",
                                  Baud._9600, 
                                  DataBits._8,  
                                  Parity.NONE, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add XGSGauge to DeviceManager
        deviceManager.addDevice(chiller);
        /**********************************************************************************************/
        // Start ModbusSlave thread
        ModbusSlaveThread modbusSlaveThread = new ModbusSlaveThread(deviceManager);
        /**********************************************************************************************/
        // Start (Maxigauge) MG device
        mg.doStart();
        // Start (LakeShore) LS1 device
        ls1.doStart();
        // Start (LakeShore) LS2 device
        ls2.doStart();
        // Start (LakeShore) LS3 device
        ls3.doStart();
        // Start (TurboVarianV81AG) TPO device
        tpo.doStart();
        // Start (TurboVarianV81AG) TPI device
        tpi.doStart();
        // Start (Controllino_1) controllino device
        controllino_1.doStart();
        // Start (Controllino_2) controllino device
        controllino_2.doStart();
        // Start (TCMuxShield) temperature device
        tcmuxshield.doStart();
        // Start (Chiller) temperature device
        chiller.doStart();
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
