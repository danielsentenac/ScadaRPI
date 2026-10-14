/*
 * This Class is the implementation of the Cryomech CPA helium compressor device
 * (PT410 cold head, touch screen generation) through Modbus TCP (RJ45 port, static IP)
 *
 * Cryomech register map (Modbus protocol addresses, unit id 1):
 *   Input registers (FC4), starting at address 1 (30001 in Cryomech doc)
 *     1      Operating State  (0 Idling, 2 Starting, 3 Running, 5 Stopping, 6 Error Lockout,
 *                              7 Error, 8 Helium Cool Down, 9 Power Related Error, 15 Recovered From Error)
 *     2      Compressor Running (0 Off, 1 On)
 *     3-4    Warning State (32 bits)
 *     5-6    Alarm State (32 bits)
 *     7-8    Coolant In Temp          (float, word swapped CDAB)
 *     9-10   Coolant Out Temp
 *     11-12  Oil Temp
 *     13-14  Helium Temp
 *     15-16  Low Pressure
 *     17-18  Low Pressure Average
 *     19-20  High Pressure
 *     21-22  High Pressure Average
 *     23-24  Delta Pressure Average
 *     25-26  Motor Current
 *     27-28  Hours of Operation
 *     29     Pressure Scale (0 PSI, 1 Bar, 2 KPa)
 *     30     Temperature Scale (0 F, 1 C, 2 K)
 *     31     Panel Serial Number
 *     32     Model (major high byte, minor low byte)
 *     33     Software Revision
 *   Holding register (FC6)
 *     1      Control: 0x0001 start compressor, 0x00FF stop compressor
 *
 * Temperatures are converted to C and pressures to bar whatever the panel display scale.
 */
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.InetAddress;

// jlibmodbus
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

public class CryomechCPA extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   private static final int slaveID = 1;
   private static final int INPUT_START = 1;        // Operating State register address
   private static final int INPUT_QUANTITY = 33;    // Operating State .. Software Revision
   private static final int CONTROL_REGISTER = 1;   // Start/Stop holding register address
   private static final int CMD_START = 0x0001;
   private static final int CMD_STOP = 0x00FF;
   private String ipAddress;
   private int port;
   private ModbusMaster m;

   public CryomechCPA (String _name,
                       String _ipAddress,
                       int _port,
                       int _mbRegisterStart) {

     pause = 2000; // pause for Device query thread cycle
     name = _name; // Device name
     ipAddress = _ipAddress; // Compressor Modbus TCP address
     port = _port;   // Compressor Modbus TCP port
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("CryomechCPA:CryomechCPA> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Status elements
     addDataElement( new DataElement(name, "OPSTATE", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "RUNST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1)); // 0 ON, 1 OFF (as M2_CP900ST)
     addDataElement( new DataElement(name, "WARNING", DataType.READ_ONLY_STATUS,RegisterType.INT32,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "ALARM", DataType.READ_ONLY_STATUS,RegisterType.INT32,mbRegisterEnd+=2));

     // Read only values
     addDataElement( new DataElement(name, "TCOOLIN", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TCOOLOUT", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TOIL", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "THELIUM", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PLOW", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PLOWAVG", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PHIGH", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PHIGHAVG", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "PDELTAAVG", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "MOTCURRENT", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "HOURS", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));

     // Commands
     addDataElement( new DataElement(name, "ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=2)); // 1 ON, 2 OFF

     // Compressor comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("CryomechCPA:CryomechCPA> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     // Init Master modbus
     try {
        TcpParameters tcpParameters = new TcpParameters();
        tcpParameters.setHost(InetAddress.getByName(ipAddress));
        tcpParameters.setPort(port);
        tcpParameters.setKeepAlive(true);

        m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        m.setResponseTimeout(1500);
        Modbus.setAutoIncrementTransactionId(true);
     }
     catch(Exception e) {
        logger.log(Level.SEVERE, "CryomechCPA:CryomechCPA>" + e.getMessage());
     }

     // No communication until first successful read
     setErrorComStatus();
     hasWarned = false;
   }

   // Cryomech floats are sent low word first
   private static double toFloat(int [] r, int i) {
      return (double) Float.intBitsToFloat(((r[i+1] & 0xFFFF) << 16) | (r[i] & 0xFFFF));
   }

   private static double toInt32(int [] r, int i) {
      return (double) (((r[i+1] & 0xFFFF) << 16) | (r[i] & 0xFFFF));
   }

   private static double toCelsius(double t, int scale) {
      if ( scale == 0 ) return (t - 32.) * 5. / 9.;   // F
      if ( scale == 2 ) return t - 273.15;            // K
      return t;                                       // C
   }

   private static double toBar(double p, int scale) {
      if ( scale == 0 ) return p * 0.0689476;         // PSI
      if ( scale == 2 ) return p / 100.;              // KPa
      return p;                                       // Bar
   }

   public void updateDeviceData() {

     DataElement dcom = getDataElement("COMST");
     boolean linkUp = false; // Connection already established at cycle start

     try {
        linkUp = m.isConnected();
        if (!linkUp) {
           m.connect();
        }

        popCommand();  // Execute commands in the loop is more reactive

        int [] r = m.readInputRegisters(slaveID, INPUT_START, INPUT_QUANTITY);
        int pscale = r[28];
        int tscale = r[29];

        getDataElement("OPSTATE").value = r[0];
        getDataElement("RUNST").value = (r[1] == 1) ? 0 : 1;
        getDataElement("WARNING").value = toInt32(r, 2);
        getDataElement("ALARM").value = toInt32(r, 4);
        getDataElement("TCOOLIN").value = toCelsius(toFloat(r, 6), tscale);
        getDataElement("TCOOLOUT").value = toCelsius(toFloat(r, 8), tscale);
        getDataElement("TOIL").value = toCelsius(toFloat(r, 10), tscale);
        getDataElement("THELIUM").value = toCelsius(toFloat(r, 12), tscale);
        getDataElement("PLOW").value = toBar(toFloat(r, 14), pscale);
        getDataElement("PLOWAVG").value = toBar(toFloat(r, 16), pscale);
        getDataElement("PHIGH").value = toBar(toFloat(r, 18), pscale);
        getDataElement("PHIGHAVG").value = toBar(toFloat(r, 20), pscale);
        getDataElement("PDELTAAVG").value = toBar(toFloat(r, 22), pscale);
        getDataElement("MOTCURRENT").value = toFloat(r, 24);
        getDataElement("HOURS").value = toFloat(r, 26);

        logger.finer("CryomechCPA:updateDeviceData> state=" + r[0] + " running=" + r[1] +
                     " pscale=" + pscale + " tscale=" + tscale +
                     " THe=" + getDataElement("THELIUM").value + " PHigh=" + getDataElement("PHIGH").value);

        dcom.value = 0; // if arriving here COM OK
        comm_cnt_failure = 0;
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("CryomechCPA:updateDeviceData> Communication with " + name + " back!");
        }
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "CryomechCPA:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "CryomechCPA:updateDeviceData>" + ex.getMessage());
        }
        try { m.disconnect(); } catch (Exception e) {} // Force reconnection on next cycle
        // Connection failure is reported at once, read failures on an established link after comm_max_failure
        if (!linkUp || ++comm_cnt_failure > comm_max_failure) setErrorComStatus();
     }
   }

   public void executeCommand( DataElement e ) {

      try {
         if ( e.name.contains("ONOFF") ) { // Compressor On/Off command
            logger.fine("CryomechCPA:executeCommand> ONOFF =" + e.value);
            if ( e.value == 1 ) // Start compressor
               m.writeSingleRegister(slaveID, CONTROL_REGISTER, CMD_START);
            else if ( e.value == 2 ) // Stop compressor
               m.writeSingleRegister(slaveID, CONTROL_REGISTER, CMD_STOP);
            // Reset trigger
            e.value = 0;
            holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "CryomechCPA:executeCommand>" + ex.getMessage());
        logger.log(Level.WARNING, "CryomechCPA:executeCommand> Communication with " + name + " interrupted");
     }
   }
};
