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
        /**********************************************************************************************/
        // Create Qms200Pfeiffer device
        Device qms = new RgaPfeifferQms200("QMS",
                                          0, // Modbus start offset (RgaPfeifferQms200 is the first device created)
                                          "/dev/serial/by-id/usb-Prolific_Technology_Inc._USB-Serial_Controller_D-if00-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add  Qms200Pfeiffer to DeviceManager
        deviceManager.addDevice(qms);
        /**********************************************************************************************/
        // Create MaxiGauge device
        Device mg = new MaxiGauge("MG",
                                  qms.mbRegisterEnd, // Modbus start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT2AWG94-if00-port0",
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
                                  "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT2AWG94-if01-port0",
                                  Baud._9600, 
                                  DataBits._8,  
                                  Parity.NONE, 
                                  StopBits._1, 
                                  FlowControl.NONE);
        
        // Add XGSGauge to DeviceManager
        deviceManager.addDevice(xgs);
        /**********************************************************************************************/
        // Create TurboPfeifferDCU device
        Device dcu = new TurboPfeifferDCU("DCU",
                                          xgs.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT1KG8VG-if00-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add TurboPfeifferDCU to DeviceManager
        deviceManager.addDevice(dcu);
        /**********************************************************************************************/
        // Create TurboVarianV81AG device
        Device v81 = new TurboVarianV81AG("V81",
                                          dcu.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT1KG8VG-if01-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add TurboVarianV81AG to DeviceManager
        deviceManager.addDevice(v81);
        /**********************************************************************************************/
        // Create IonicAgilentDual device
        Device dual = new IonicAgilentDual("DUAL",
                                          v81.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM232_Plus4_FT2AWG94-if02-port0", 
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add IonicAgilentDual to DeviceManager
        deviceManager.addDevice(dual);
        /**********************************************************************************************/
        // Create TitaneVarianTSP_22 device
        Device tsp = new TitaneVarianTSP_32("TSP",
                                          dual.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT1KG8VG-if02-port0",  
                                          Baud._9600, 
                                          DataBits._8,  
                                          Parity.NONE, 
                                          StopBits._1, 
                                          FlowControl.NONE);
        
        // Add IonicAgilentDual to DeviceManager
        deviceManager.addDevice(tsp);
        /**********************************************************************************************/
        // Create Controllino device
        Device controllino = new Controllino("I2C",
                                              tsp.mbRegisterEnd, // Modbus start offset
                                              0x08);
        
        // Add Controllino to DeviceManager
        deviceManager.addDevice(controllino);
        /**********************************************************************************************/
        // Start ModbusSlave thread
        ModbusSlaveThread modbusSlaveThread = new ModbusSlaveThread(deviceManager);
        /**********************************************************************************************/
        // Start (Maxigauge) MG device
        mg.doStart();
        // Start (XGSgauge) XGS device
        xgs.doStart();
        // Start (TurboPfeifferDCU) dcu device
        dcu.doStart();
        // Start (TurboVarianV81AG) v81 device
        v81.doStart();
        // Start (IonicAgilentDual) dual device
        dual.doStart();
        // Start (TitaneVarianTSP) tsp device
        tsp.doStart();
        // Start (RgaQms200Pfeiffer) qms device
        qms.doStart();
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
