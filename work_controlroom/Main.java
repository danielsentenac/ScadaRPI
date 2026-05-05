/*
 * Control Room - vacuum supervisor panel only
 */

import java.io.IOException;

import com.pi4j.platform.PlatformAlreadyAssignedException;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.Date;
import java.time.LocalDate;
import java.awt.GraphicsEnvironment;

import sun.misc.Signal;
import sun.misc.SignalHandler;


public class Main {

    private static final Logger logger = Logger.getLogger("Main");

    public static void main(String[] args)
                 throws InterruptedException,
                        PlatformAlreadyAssignedException,
                        IOException,
                        UnsupportedBusNumberException {

         logger.setLevel(Level.FINE);

         try {
             FileHandler fileHandler = new FileHandler("CONTROLROOM_" + LocalDate.now() + ".log");
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
             logger.addHandler(fileHandler);
         } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to setup logging. Error: ");
            e.printStackTrace();
         }

         logger.finer("<-- Control Room -->");

         DeviceManager deviceManager = new DeviceManager();

         boolean headlessMode = Boolean.getBoolean("scadarpi.headless") || GraphicsEnvironment.isHeadless();
         if (headlessMode) {
            logger.warning("Main: running in headless mode, GUI initialization skipped.");
         } else {
            GlgGui mainGui = new GlgGui(deviceManager, "CONTROL ROOM");

            Signal.handle(new Signal("INT"), new SignalHandler () {
               public void handle(Signal sig) {
                  logger.finer("Main: Interrupt received, Exiting program");
                  mainGui.exitProgram();
               }
            });
         }
    }
}
