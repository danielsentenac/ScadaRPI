/*
 * This is the main class where all threads are configured & started
 */

import java.io.IOException;
import java.util.Arrays;
import java.awt.GraphicsEnvironment;

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
import java.util.logging.Handler;
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
    private static final String mainTitle = "CBSAS ";

    private static final Logger logger = Logger.getLogger("Main");
    private static final String DEFAULT_LOG_LEVEL = "FINE";

    private static Level configuredLogLevel() {
        String levelName = System.getProperty("scadarpi.logLevel");
        if (levelName == null || levelName.trim().isEmpty()) {
            levelName = System.getenv("SCADARPI_LOG_LEVEL");
        }
        if (levelName == null || levelName.trim().isEmpty()) {
            levelName = DEFAULT_LOG_LEVEL;
        }

        try {
            return Level.parse(levelName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            System.err.println("Invalid log level '" + levelName + "', using " + DEFAULT_LOG_LEVEL);
            return Level.parse(DEFAULT_LOG_LEVEL);
        }
    }

    private static java.util.logging.Formatter createLogFormatter() {
        return new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                                     new Date(lr.getMillis()),
                                     lr.getLevel().getLocalizedName(),
                                     lr.getMessage());
            }
        };
    }

    private static void configureLogging() throws IOException {
        Level logLevel = configuredLogLevel();
        java.util.logging.Formatter formatter = createLogFormatter();

        logger.setUseParentHandlers(false);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
            handler.close();
        }

        logger.setLevel(logLevel);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(logLevel);
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        FileHandler fileHandler = new FileHandler("CBSAS_" + LocalDate.now() + ".log");
        fileHandler.setLevel(logLevel);
        fileHandler.setFormatter(formatter);
        logger.addHandler(fileHandler);
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

         try {
             configureLogging();
             
        } catch (IOException e) {
           logger.log(Level.SEVERE, "Unable to setup logging to debug. No logging will be done. Error: ");
           e.printStackTrace();
        }
        // print program title/header
        logger.finer("<-- JPiMain Tests-->");
        
        // Create DeviceManager object
        DeviceManager deviceManager = new DeviceManager();
        /**********************************************************************************************/
        // Create SupervisorClient device
        Device supervisorclient = new SupervisorClient("SC",0); // Modbus start offset
        
        // Add SC to DeviceManager
        deviceManager.addDevice(supervisorclient);
        /**********************************************************************************************/
        /**********************************************************************************************/
        // Create SupervisorClient device
        Device supervisorclient2 = new SupervisorClient2("SC2",supervisorclient.mbRegisterEnd); // Modbus start offset
        
        // Add SC to DeviceManager
        deviceManager.addDevice(supervisorclient2);
        /**********************************************************************************************/
        // Start (SupervisorClient) particle counter panel
        supervisorclient.doStart();
	// Start (SupervisorClient2) O2 sensors panel
	supervisorclient2.doStart();
	/**********************************************************************************************/
	final boolean headlessMode = Boolean.getBoolean("scadarpi.headless") || GraphicsEnvironment.isHeadless();
	final GlgGui mainGui;
	if (headlessMode) {
	    logger.info("Main: headless runtime detected; GUI disabled.");
	    mainGui = null;
	}
	else {
	    // Start Glg GUI
	    mainGui = new GlgGui(deviceManager,mainTitle);
	}

	// Handle CTRL-C interrupt to end cleanly the program
	Signal.handle(new Signal("INT"), new SignalHandler () {
	    public void handle(Signal sig) {
	       logger.finer("Main: Interrupt received, Exiting program");
	       if (mainGui != null) {
	          mainGui.exitProgram();
	       }
	       System.exit(0);
	     }
	 });
        }
}
