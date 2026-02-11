/*
 * This Class is used to communicate through Serial wire
 */

import java.io.IOException;
import java.util.Arrays;

import com.pi4j.io.serial.*;
import com.pi4j.util.CommandArgumentParser;
import com.pi4j.platform.PlatformAlreadyAssignedException;
import java.util.logging.Logger;
import java.util.logging.Level;


public class Serial_Comm {

    // Serial port
    private String serial_port; 
    private Baud baudrate;
    private DataBits databits;
    private Parity parity;
    private StopBits stopbits;
    private FlowControl flowcontrol;
    private SerialConfig config;
    private static final Logger logger = Logger.getLogger("Main");

    // create an instance of the serial communication class
    private final Serial serial = SerialFactory.createInstance();
    /**
     * Constructor
     *
     * @param args
     * @throws InterruptedException
     * @throws PlatformAlreadyAssignedException
     * @throws IOException
     * @throws UnsupportedBusNumberException
     */
    public Serial_Comm(String _serial_port, 
                      Baud _baudrate, 
                      DataBits _databits, 
                      Parity _parity, 
                      StopBits _stopbits, 
                      FlowControl _flowcontrol) 
       throws InterruptedException, IOException {

        serial_port = _serial_port;
        baudrate = _baudrate;
        databits = _databits;
        parity = _parity;
        stopbits = _stopbits;
        flowcontrol = _flowcontrol;
        // print program title/header
        logger.finer("<-- Serial Communication -->");

        try {
            // create serial config object
            config = new SerialConfig();
            config.device(serial_port)
                  .baud(baudrate)
                  .dataBits(databits)
                  .parity(parity)
                  .stopBits(stopbits)
                  .flowControl(flowcontrol);
         }
         catch(UnsatisfiedLinkError ex) {
            logger.log(Level.SEVERE, ex.getMessage());
            return;
         }
    }
    /**
     * Open Method
     *
     */
     public void Open() 
       throws InterruptedException, IOException  {
  
        // display connection details
        //logger.finer(" Connecting to: " + config.toString());
        // open the default serial device/port with the configuration settings
        serial.open(config);
    }
    /**
     * Close Method
     *
     */
     public void Close() 
       throws InterruptedException, IOException  {
  
        // display connection details
        //logger.finer(" Closing: " + config.toString());
        // close the default serial device/port with the configuration settings
        if ( serial.isOpen() ) serial.close();
    }
    /**
     * isOpen Method
     *
     */
     public boolean isOpen() 
       throws InterruptedException, IOException  {
        return (serial.isOpen());
    }
    /**
     * Read Method
     *
     */
     public byte[] Read() 
       throws InterruptedException, IOException {
  
        // Perform a Serial READ operation
        //logger.finer("... reading from serial port");
        return (serial.read());
    }
    /**
     * Write Method
     *
     */
     public void Write(String  mesg) 
       throws InterruptedException, IOException {
        
        // Perform a Serial WRITE operation
        //logger.finer("... writing to serial port");
        serial.write(mesg);
    }
    /**
     * Write Method
     *
     */
     public void Write(byte[]  mesg) 
       throws InterruptedException, IOException {
        
        // Perform a Serial WRITE operation
        //logger.finer("... writing to serial port");
        serial.write(mesg);
    }
    /**
     * Available Method
     *
     */
     public int BytesAvailable() 
       throws IllegalStateException, IOException {
        
        // Perform a Serial Available operation
        return (serial.available());
    }
    
}
