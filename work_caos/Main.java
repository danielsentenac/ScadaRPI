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
    private static final String mainTitle1 = "EST END";
    private static final String mainTitle2 = "BEAM SPLITTER";

    // Chamber 1 (BEAM SPLITTER) pumping module (Controllino_1) I2C address
    private static final int CONTROLLINO_M1_ADDR = 0x08;

    // Chamber 1 (BEAM SPLITTER) venting module (Controllino_2) I2C address
    private static final int CONTROLLINO_M2_ADDR = 0x09;

    // Chamber 2 (EST END) pumping module (Controllino_3) I2C address
    private static final int CONTROLLINO_M3_ADDR = 0x10;

    // Chamber 2 (EST END) venting module (Controllino_4) I2C address
    private static final int CONTROLLINO_M4_ADDR = 0x11;

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
             FileHandler fileHandler = new FileHandler("CAOSRack_" + LocalDate.now() + ".log");
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

           GlgGui mainGui = new GlgGui(deviceManager,mainTitle1,mainTitle2);
           Signal.handle(new Signal("INT"), new SignalHandler () {
              public void handle(Signal sig) {
                 logger.finer("Main: Interrupt received, Exiting program");
                 mainGui.exitProgram();
              }
           });
           return;
        }

        /**********************************************************************************************/
        // ------------------------- Chamber 1 (BEAM SPLITTER) -------------------------
        /**********************************************************************************************/
        // Create MaxiGauge device (chamber 1)
        Device mg1 = new MaxiGauge("MG1",
                                  0, // Modbus start offset (MaxiGauge 1 is the first device created)
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT2Z69WT-if00-port0",
                                  Baud._9600,
                                  DataBits._8,
                                  Parity.NONE,
                                  StopBits._1,
                                  FlowControl.NONE);

        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(mg1);
        /**********************************************************************************************/
        // Create TurboPfeifferDCU device (chamber 1)
        Device dcu1 = new TurboPfeifferDCU("DCU1",
                                          mg1.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_FT79IY3R-if00-port0",
                                          Baud._9600,
                                          DataBits._8,
                                          Parity.NONE,
                                          StopBits._1,
                                          FlowControl.NONE);

        // Add TurboPfeifferDCU to DeviceManager
        deviceManager.addDevice(dcu1);
        /**********************************************************************************************/
        // Create Controllino_1 pumping module device (chamber 1)
        Device controllino_m1 = new Controllino_1("M1",
                                              dcu1.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_M1_ADDR);

        // Add Controllino_1 to DeviceManager
        deviceManager.addDevice(controllino_m1);
        /**********************************************************************************************/
        // Create Controllino_2 venting module device (chamber 1)
        Device controllino_m2 = new Controllino_2("M2",
                                              controllino_m1.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_M2_ADDR);

        // Add Controllino_2 to DeviceManager
        deviceManager.addDevice(controllino_m2);
        /**********************************************************************************************/
        // ------------------------- Chamber 2 (EST END) -------------------------
        /**********************************************************************************************/
        // Create MaxiGauge device (chamber 2)
        // TODO: set the real /dev/serial/by-id path of the chamber-2 MaxiGauge RS485 converter
        Device mg2 = new MaxiGauge("MG2",
                                  controllino_m2.mbRegisterEnd, // Modbus start offset
                                  "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_CHANGEME2-if00-port0",
                                  Baud._9600,
                                  DataBits._8,
                                  Parity.NONE,
                                  StopBits._1,
                                  FlowControl.NONE);

        // Add MaxiGauge to DeviceManager
        deviceManager.addDevice(mg2);
        /**********************************************************************************************/
        // Create TurboPfeifferDCU device (chamber 2)
        // TODO: set the real /dev/serial/by-id path of the chamber-2 turbo DCU RS485 converter
        Device dcu2 = new TurboPfeifferDCU("DCU2",
                                          mg2.mbRegisterEnd, // Modbus start offset
                                          "/dev/serial/by-id/usb-FTDI_USB-COM485_Plus4_CHANGEME3-if00-port0",
                                          Baud._9600,
                                          DataBits._8,
                                          Parity.NONE,
                                          StopBits._1,
                                          FlowControl.NONE);

        // Add TurboPfeifferDCU to DeviceManager
        deviceManager.addDevice(dcu2);
        /**********************************************************************************************/
        // Create Controllino_1 pumping module device (chamber 2)
        Device controllino_m3 = new Controllino_1("M3",
                                              dcu2.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_M3_ADDR);

        // Add Controllino_1 to DeviceManager
        deviceManager.addDevice(controllino_m3);
        /**********************************************************************************************/
        // Create Controllino_2 venting module device (chamber 2, VSPARE slot drives VREM)
        Device controllino_m4 = new Controllino_2("M4",
                                              controllino_m3.mbRegisterEnd, // Modbus start offset
                                              CONTROLLINO_M4_ADDR);

        // Add Controllino_2 to DeviceManager
        deviceManager.addDevice(controllino_m4);
        /**********************************************************************************************/
        // ------------------------- Rack (single devices) -------------------------
        /**********************************************************************************************/
        // Create TCMuxShield device
        Device tcmuxshield = new TCMuxShield("TCMUX",
                                              controllino_m4.mbRegisterEnd); // Modbus start offset

        // Add TCMuxShield to DeviceManager
        deviceManager.addDevice(tcmuxshield);
        /**********************************************************************************************/
        // RGA (QMS) devices commented out - not used at the moment
        // // Create Rga Qms200 (from Pfeiffer) device - chamber 1 (Ge4_1)
        // // NOTE: device name has no '_' so channel parsing (split("_")) stays "<DEVICE>_<REGISTER>"
        // Device qms200 = new RgaPfeifferQms200("RGAGe4",
        //                                       tcmuxshield.mbRegisterEnd, // Modbus start offset
        //                                       "/dev/serial/by-id/usb-Prolific_Technology_Inc._USB-Serial_Controller_D-if00-port0",
        //                                       Baud._9600,
        //                                       DataBits._8,
        //                                       Parity.NONE,
        //                                       StopBits._1,
        //                                       FlowControl.NONE);
        //
        // // Add RgaPfeifferQms200 to DeviceManager
        // deviceManager.addDevice(qms200);
        // /**********************************************************************************************/
        // // Create Rga Qms200 (from Pfeiffer) device - chamber 2 (Ge4_2)
        // // TODO: set the real /dev/serial/by-id path of the chamber-2 RGA RS232/USB converter
        // Device qms200_2 = new RgaPfeifferQms200("RGAGe42",
        //                                       qms200.mbRegisterEnd, // Modbus start offset
        //                                       "/dev/serial/by-id/usb-Prolific_Technology_Inc._USB-Serial_Controller_CHANGEME_RGA2-if00-port0",
        //                                       Baud._9600,
        //                                       DataBits._8,
        //                                       Parity.NONE,
        //                                       StopBits._1,
        //                                       FlowControl.NONE);
        //
        // // Add second RgaPfeifferQms200 to DeviceManager
        // deviceManager.addDevice(qms200_2);
        /**********************************************************************************************/
        // Start ModbusSlave thread
        ModbusSlaveThread modbusSlaveThread = new ModbusSlaveThread(deviceManager);
        /**********************************************************************************************/
        // Start (Maxigauge) MG1 device
        mg1.doStart();
        // Start (TurboPfeifferDCU) dcu1 device
        dcu1.doStart();
        // Start (Controllino_1) chamber-1 pumping module
        controllino_m1.doStart();
        // Start (Controllino_2) chamber-1 venting module
        controllino_m2.doStart();
        // Start (Maxigauge) MG2 device
        mg2.doStart();
        // Start (TurboPfeifferDCU) dcu2 device
        dcu2.doStart();
        // Start (Controllino_3) chamber-2 pumping module
        controllino_m3.doStart();
        // Start (Controllino_4) chamber-2 venting module
        controllino_m4.doStart();
        // Start (TCMuxShield) temperature device
        tcmuxshield.doStart();
        // RGA (QMS) devices commented out - not used at the moment
        // // Start (RgaPfeifferQms200) rga devices (chamber 1 + chamber 2)
        // qms200.doStart();
        // qms200_2.doStart();
        // Start modbusSlaveThread thread
        modbusSlaveThread.doStart();
        /**********************************************************************************************/
        // Start Glg GUI
        GlgGui mainGui = new GlgGui(deviceManager,mainTitle1,mainTitle2);

        // Handle CTRL-C interrupt to end cleanly the program
        Signal.handle(new Signal("INT"), new SignalHandler () {
           public void handle(Signal sig) {
              logger.finer("Main: Interrupt received, Exiting program");
              mainGui.exitProgram();
           }
        });
    }
}
