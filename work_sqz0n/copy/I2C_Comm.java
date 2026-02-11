/*
 * This Class is used to communicate through I2C wire
 */

import java.io.IOException;
import java.util.Arrays;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import com.pi4j.platform.PlatformAlreadyAssignedException;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.concurrent.TimeUnit;

public class I2C_Comm {

    // I2C address
    private int device_addr; 
    private static final Logger logger = Logger.getLogger("Main");

    // create an instance of the I2C communication class
    private I2CDevice device = null;

    /**
     * Constructor
     *
     * @param args
     * @throws InterruptedException
     * @throws PlatformAlreadyAssignedException
     * @throws IOException
     * @throws UnsupportedBusNumberException
     */
    public I2C_Comm(int _device_addr) 
       throws InterruptedException, PlatformAlreadyAssignedException, IOException, UnsupportedBusNumberException {

        device_addr = _device_addr;
        
        // print program title/header
        logger.finer("<-- I2C Communication -->");

        // fetch all available busses
        try {
            int[] ids = I2CFactory.getBusIds();
            logger.finer("Found following I2C busses: " + Arrays.toString(ids));
        } 
        catch (IOException exception) {
            logger.log(Level.SEVERE,"I/O error during fetch of I2C busses occurred");
        }
        catch (NumberFormatException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
        // find available busses
        for (int number = I2CBus.BUS_0; number <= I2CBus.BUS_17; ++number) {
            try {
                @SuppressWarnings("unused")
	        I2CBus bus = I2CFactory.getInstance(number);
                logger.finer("Supported I2C bus " + number + " found");
            } 
            catch (IOException exception) {
                logger.log(Level.WARNING,"I/O error on I2C bus " + number + " occurred");
            } 
            catch (UnsupportedBusNumberException exception) {
                logger.log(Level.WARNING,"Unsupported I2C bus " + number);
            }
        }
        try {
          // get the I2C bus to communicate on
          I2CBus i2c = I2CFactory.getInstance(I2CBus.BUS_1,5000,TimeUnit.MILLISECONDS);

          // create an I2C device for an individual device on the bus that you want to communicate with
          device = i2c.getDevice(device_addr);
          if ( device == null )
             logger.log(Level.SEVERE,"Could not create device");
        }
        catch (UnsupportedBusNumberException exception) {
                logger.log(Level.WARNING,"Unsupported I2C bus BUS_1");
        }
    }
    /**
     * Read Methods
     *
     */
     public byte[] Read() 
       throws InterruptedException, PlatformAlreadyAssignedException, IOException, UnsupportedBusNumberException {
        
        // Device byte registers array
        byte[] i2c_array = new byte[]{0,0,0,0};
        // Perform a I2C READ operation
        //logger.finer("... reading array registers from device");
        int response = device.read(i2c_array,0,4);
        //logger.finer("Reads number of bytes = " + response);
        if ( response < 4 )
           logger.log(Level.SEVERE,"Missing bytes. Got only " + response + " bytes");
        //for (int i = 0; i < 4; ++i) 
        //  logger.finer("Device register = " + i + ":" + String.format("0x%02x", i2c_array[i]));
        return i2c_array;
    }
    public byte[] Read8bytes() 
       throws InterruptedException, PlatformAlreadyAssignedException, IOException, UnsupportedBusNumberException {
        
        // Device byte registers array
        byte[] i2c_array = new byte[]{0,0,0,0,0,0,0,0};
        // Perform a I2C READ operation
        //logger.finer("... reading array registers from device");
        int response = device.read(i2c_array,0,8);
        //logger.finer("Reads number of bytes = " + response);
        if ( response < 8 )
           logger.log(Level.SEVERE,"Missing bytes. Got only " + response + " bytes");
        //for (int i = 0; i < 4; ++i) 
        //  logger.finer("Device register = " + i + ":" + String.format("0x%02x", i2c_array[i]));
        return i2c_array;
    }
    /**
     * Write Method
     *
     */
     public void Write(byte[] mesg) 
       throws InterruptedException, PlatformAlreadyAssignedException, IOException, UnsupportedBusNumberException {
        
        // Perform a I2C WRITE operation
        //logger.finer("... writing array registers to device");
        device.write(mesg);
    }
}
