/*
 * This Class is used to communicate through SPI wire
 */

import java.io.IOException;
import java.util.Arrays;

import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.concurrent.TimeUnit;


public class SPI_Comm {

    private static final Logger logger = Logger.getLogger("Main");

    // create an instance of the I2C communication class
    private SpiDevice spi = null;

    /**
     * Constructor
     *
     * @param args
     * @throws InterruptedException
     * @throws PlatformAlreadyAssignedException
     * @throws IOException
     * @throws UnsupportedBusNumberException
     */
    public SPI_Comm() 
       throws InterruptedException, IOException {
        
        // print program title/header
        logger.finer("<-- SPI Communication -->");

        // create SPI object instance for SPI for communication
        spi = SpiFactory.getInstance(SpiChannel.CS0,
                                     SpiDevice.DEFAULT_SPI_SPEED, // default spi speed 1 MHz
                                     SpiDevice.DEFAULT_SPI_MODE); // default spi mode 0
    }
    /**
     * Write Method
     *
     */
     public byte[] Write(byte[] mesg) 
       throws InterruptedException, IOException {
          byte[] result = null;
        try {
          // Perform a I2C WRITE operation
          logger.finer("... writing array registers to device");
          result = spi.write(mesg);
        }
        catch (Exception e) {
           logger.log(Level.WARNING, e.getMessage());
           e.printStackTrace(); 
        }
        return result;
    }
}
