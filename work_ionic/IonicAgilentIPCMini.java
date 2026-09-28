/*
 * This Class is the implementation of the Ionic Agilent IPCMini device
 * (Agilent "window" serial protocol, RS485 2-wire, see IPCMini manual 87-900-153-01).
 *
 * Frame: <STX 0x02><ADDR 0x80+n><WIN 3 ascii><COM '0'=read,'1'=write><DATA><ETX 0x03><CRC 2 ascii hex>
 * CRC  : XOR of all bytes after STX up to and including ETX.
 *
 * Data elements keep the IonicAgilentDual Modbus layout (offsets and register types). The five Dual-only
 * slots (Max current, Step1/2 voltage and current) are reused for IPCMini values: error code, power section
 * and controller temperatures, set point and pump type.
 */
import java.util.Locale;
import java.util.*;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import com.pi4j.io.serial.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class IonicAgilentIPCMini extends Device {

   private Serial_Comm rs485;
   private static final Logger logger = Logger.getLogger("Main");
   private int com_error_count = 0;
   private final int MAX_COM_ERROR = 5;
   private final int address;                 // RS485 device number (window 503)
   private final int REPLY_TIMEOUT_MS = 500;

   // Reply codes (single data byte)
   private static final int ACK = 0x06, NACK = 0x15, UNKNOWN_WIN = 0x32, DATA_TYPE_ERR = 0x33,
                            OUT_OF_RANGE = 0x34, WIN_DISABLED = 0x35;

   // IPCMini windows
   private static final String WIN_MODE = "008";        // N  0=Serial,1=Remote,2=Local,3=LAN
   private static final String WIN_HV = "011";          // L  HV ON/OFF CH1
   private static final String WIN_ERROR = "206";       // N  error code (bit field)
   private static final String WIN_PROTECT = "602";     // 0=Disabled,1=Enabled
   private static final String WIN_STEP = "603";        // Fixed/Step: 0=Fixed,1=Step
   private static final String WIN_MAXPOWER = "612";    // N  [10,40] W
   private static final String WIN_VTARGET = "613";     // N  [3000,7000] V
   private static final String WIN_IPROTECT = "614";    // N  [1,10000] uA
   private static final String WIN_VMEAS = "810";       // N  V
   private static final String WIN_IMEAS = "811";       // A  A (X.XXe-XX)
   private static final String WIN_PRESSURE = "812";    // A  X.XXe-XX (unit from window 600)
   private static final String WIN_TEMP_PWR = "800";    // N  power section temperature, 0.1 C units
   private static final String WIN_TEMP_INT = "801";    // N  internal controller temperature, 0.1 C units
   private static final String WIN_PUMPTYPE = "610";    // N  device number (pump type)
   private static final String WIN_SETPOINT = "615";    // A  set point [X.XE-XX]

   // Data field length of the last read of each window: 1=logic, 6=numeric, other=alphanumeric
   private final Map<String, Integer> dataLength = new HashMap<String, Integer>();

   // Last raw values needed to build the P33ST status code
   private int hv = -1, protect = -1, step = -1, error = 0;

   public IonicAgilentIPCMini (String _name,
                               int _mbRegisterStart,
                               String serial_port,
                               int _address,
                               Baud baudrate,
                               DataBits databits,
                               Parity parity,
                               StopBits stopbits,
                               FlowControl flowcontrol) {

     name = _name; // Device name
     address = _address;

     // Data types from the manual windows table, refined by each read
     dataLength.put(WIN_HV, 1);
     dataLength.put(WIN_SETPOINT, 7);
     for (String w : new String[] { WIN_MODE, WIN_MAXPOWER, WIN_VTARGET, WIN_IPROTECT })
        dataLength.put(w, 6);

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("IonicAgilentIPCMini:IonicAgilentIPCMini> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Ionic Pump properties (type 1: read only values)
     addDataElement( new DataElement(name, "P33ST",DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));
     addDataElement( new DataElement(name, "P33ABSCUR",DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33ABSVOLT",DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "P33P",DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));

     // Ionic Pump Commands (type 2: command triggers)
     addDataElement( new DataElement(name, "P33ONOFF",DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     //Ionic Pump writable values (type 3: read/writable status or value)
     addDataElement( new DataElement(name, "P33REMOTEMODE",DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33OPMODE",DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33VOLTMODE",DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33PRTCUR",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33ERR",DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));      // was P33MAXCUR
     addDataElement( new DataElement(name, "P33MAXVOLT",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33MAXW",DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "P33TEMPPWR",DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));   // was P33STEP1VOLT
     addDataElement( new DataElement(name, "P33TEMPINT",DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));   // was P33STEP2VOLT
     addDataElement( new DataElement(name, "P33SETPOINT",DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1)); // was P33STEP1CUR
     addDataElement( new DataElement(name, "P33PUMPTYPE",DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2)); // was P33STEP2CUR

     // Com Status
     addDataElement( new DataElement(name, "P33COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=2));

     mbRegisterEnd+=1;

     logger.finer("IonicAgilentIPCMini:IonicAgilentIPCMini> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

      // Instantiate communication channel
     try {
       rs485 = new Serial_Comm(serial_port, baudrate, databits, parity, stopbits, flowcontrol);
       rs485.Open();
     }
     catch (InterruptedException e) {
       logger.log(Level.SEVERE, e.getMessage());
     }
     catch (IOException e) {
       logger.log(Level.SEVERE, e.getMessage());
     }
     catch(UnsatisfiedLinkError ex) {
       logger.log(Level.SEVERE, ex.getMessage());
     }

   }

   public void updateDeviceData() {

     // Windows polled each cycle, in order
     final String[] windows = { WIN_HV, WIN_PROTECT, WIN_STEP, WIN_ERROR, WIN_MODE, WIN_IMEAS,
                                WIN_VMEAS, WIN_IPROTECT, WIN_PRESSURE, WIN_VTARGET, WIN_MAXPOWER,
                                WIN_TEMP_PWR, WIN_TEMP_INT, WIN_SETPOINT, WIN_PUMPTYPE };
     DataElement comst = getDataElement("P33COMST");

     try {
        for (String win : windows) {
           popCommand();  // Execute commands in the loop is more reactive
           String data = readWindow(win);
           if (data == null) { // Com Status Error
              com_error_count+=1;
              if ( com_error_count > MAX_COM_ERROR ) {
                 setErrorComStatus();
                 comst.value = 1; // ERR COM
              }
              continue;
           }
           com_error_count = 0;
           comst.value = 0; // OK COM
           if (data.isEmpty()) continue;
           try {
              switch (win) {
                 case WIN_HV:       hv = Integer.parseInt(data); break;
                 case WIN_PROTECT:  protect = Integer.parseInt(data);
                                    getDataElement("P33OPMODE").value = protect; break;   // 0=Started,1=Protected
                 case WIN_STEP:     step = Integer.parseInt(data);
                                    getDataElement("P33VOLTMODE").value = step; break;    // 0=Fixed,1=Stepped
                 case WIN_ERROR:    int e = Integer.parseInt(data);
                                    if (e != error)
                                       logger.log(Level.WARNING, "IonicAgilentIPCMini:updateDeviceData> " + name + " error code " + e);
                                    error = e;
                                    getDataElement("P33ERR").value = e; break;
                 case WIN_MODE:     getDataElement("P33REMOTEMODE").value = modeToRemoteStatus(Integer.parseInt(data)); break;
                 case WIN_IMEAS:    getDataElement("P33ABSCUR").value = Double.parseDouble(data); break;
                 case WIN_VMEAS:    getDataElement("P33ABSVOLT").value = Double.parseDouble(data); break;
                 case WIN_IPROTECT: getDataElement("P33PRTCUR").value = Double.parseDouble(data); break;
                 case WIN_PRESSURE: getDataElement("P33P").value = Double.parseDouble(data); break;
                 case WIN_VTARGET:  getDataElement("P33MAXVOLT").value = Double.parseDouble(data); break;
                 case WIN_MAXPOWER: getDataElement("P33MAXW").value = Double.parseDouble(data); break;
                 case WIN_TEMP_PWR: getDataElement("P33TEMPPWR").value = Double.parseDouble(data) / 10.; break;
                 case WIN_TEMP_INT: getDataElement("P33TEMPINT").value = Double.parseDouble(data) / 10.; break;
                 case WIN_SETPOINT: getDataElement("P33SETPOINT").value = Double.parseDouble(data); break;
                 case WIN_PUMPTYPE: getDataElement("P33PUMPTYPE").value = Double.parseDouble(data); break;
              }
           }
           catch (NumberFormatException n) {
              logger.log(Level.SEVERE, "IonicAgilentIPCMini:updateDeviceData> Format error window " + win + ":" + data);
           }
        }
        if (comst.value == 0)
           getDataElement("P33ST").value = pumpStatus();
     }
     catch (Exception ex) {
        ex.printStackTrace();
        setErrorComStatus();
        comst.value = 1; // ERR COM
     }
   }

   public void executeCommand (DataElement e) {

      String win = null, data = null;
      int v = (int) e.setvalue;

      if (e.name.contains("P33ONOFF")) { // On/Off command
         win = WIN_HV;
         if ( e.value == 1 ) data = "1";       // On
         else if ( e.value == 2 ) data = "0";  // Off
      }
      else if (e.name.contains("P33REMOTEMODE")) { // Local=0, Remote=1, Serial=2
         win = WIN_MODE;
         if ( v == 0 ) data = "2";             // Local
         else if ( v == 1 ) data = "1";        // Remote I/O
         else if ( v == 2 ) data = "0";        // Serial
      }
      else if (e.name.contains("P33OPMODE")) { // Start=1, Protect=2
         win = WIN_PROTECT;
         if ( v == 1 ) data = "0";
         else if ( v == 2 ) data = "1";
      }
      else if (e.name.contains("P33VOLTMODE")) { // Fixed=1, Step=2
         win = WIN_STEP;
         if ( v == 1 ) data = "0";
         else if ( v == 2 ) data = "1";
      }
      else if (e.name.contains("P33PRTCUR")) { // I protect [1,10000] uA
         win = WIN_IPROTECT;
         if ( v >= 1 && v <= 10000 ) data = Integer.toString(v);
      }
      else if (e.name.contains("P33MAXVOLT")) { // V target [3000,7000] V
         win = WIN_VTARGET;
         if ( v >= 3000 && v <= 7000 ) data = Integer.toString(v);
      }
      else if (e.name.contains("P33MAXW")) { // Max power [10,40] W
         win = WIN_MAXPOWER;
         if ( v >= 10 && v <= 40 ) data = Integer.toString(v);
      }
      else if (e.name.contains("P33SETPOINT")) { // Set point [X.XE-XX]
         win = WIN_SETPOINT;
         if ( e.setvalue > 0 ) data = String.format(Locale.ROOT, "%.1E", e.setvalue);
      }
      else {
         logger.log(Level.WARNING, "IonicAgilentIPCMini:executeCommand> " + e.name + " not supported by IPCMini");
         return;
      }

      try {
         if (data == null)
            logger.log(Level.WARNING, "IonicAgilentIPCMini:executeCommand> " + e.name + " invalid value " + e.setvalue);
         else {
            int code = writeWindow(win, data);
            if (code != ACK)
               logger.log(Level.WARNING, "IonicAgilentIPCMini:executeCommand> " + e.name + " write window " + win
                                         + "=" + data + " refused: " + replyCodeName(code));
            else
               logger.finer("IonicAgilentIPCMini:executeCommand> " + e.name + " write window " + win + "=" + data + " done");
         }
         if (e.type == DataType.TRIGGER) {
            Thread.sleep(2000); // Wait before resetting
            e.value = 0;
            holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
      }
      catch (Exception ex) {
        setErrorComStatus();
      }
   }

   //
   // GUI status code (see ChannelList IonicONOFFSTATUS)
   //
   private int pumpStatus() {
      if ((error & 32) != 0) return -5;   // Interlock cable
      if ((error & 4) != 0) return -8;    // Over temperature
      if (hv != 1) return 0;              // Pump Off
      if (protect == 1) return (step == 1) ? 3 : 4;  // On Protect/Step, On Protect/Fixed
      return (step == 1) ? 1 : 2;                    // On Step/Start, On Fixed/Start
   }

   // IPCMini mode (0=Serial,1=Remote,2=Local,3=LAN) -> GUI code (0=Local,1=Remote I/O,2=Serial)
   private int modeToRemoteStatus(int mode) {
      switch (mode) {
         case 0: return 2;
         case 1: return 1;
         case 2: return 0;
         case 3: return 3;   // LAN
         default: return 255;
      }
   }

   //
   // Protocol helpers
   //
   // Returns the window data, "" if the device answered with a reply code, null if no valid reply
   private String readWindow(String win) throws Exception {
      byte[] reply = transact(frame(win, '0', ""));
      if (reply == null) return null;
      if (reply.length == 3) { // single reply code (unknown window, data type error, ...)
         logger.finer("IonicAgilentIPCMini:readWindow> window " + win + ": " + replyCodeName(reply[2] & 0xFF));
         return "";
      }
      if (reply.length < 6 || !new String(reply, 2, 3, StandardCharsets.ISO_8859_1).equals(win))
         return null;
      String data = new String(reply, 6, reply.length - 6, StandardCharsets.ISO_8859_1).trim();
      dataLength.put(win, reply.length - 6);
      return data;
   }

   private int writeWindow(String win, String value) throws Exception {
      // Format the value like the last read of the window: numeric windows are 6 digits
      Integer len = dataLength.get(win);
      String data = value;
      if (len != null && len == 6)
         data = String.format("%06d", Integer.parseInt(value));
      byte[] reply = transact(frame(win, '1', data));
      if (reply == null || reply.length < 3) return -1;
      return reply[2] & 0xFF;
   }

   private byte[] frame(String win, char com, String data) {
      String body = (char)(0x80 + address) + win + com + data + (char)0x03;
      int crc = 0;
      for (int i = 0; i < body.length(); i++) crc ^= body.charAt(i);
      return ((char)0x02 + body + String.format("%02X", crc & 0xFF)).getBytes(StandardCharsets.ISO_8859_1);
   }

   // Send a frame and return the reply payload (bytes between STX and ETX), or null.
   // The RS485 2-wire port echoes the request: it is skipped.
   private byte[] transact(byte[] request) throws Exception {
      rs485.Write(request);
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      long deadline = System.currentTimeMillis() + REPLY_TIMEOUT_MS;
      while (System.currentTimeMillis() < deadline) {
         Thread.sleep(20);
         while (rs485.BytesAvailable() > 0) {
            byte[] b = rs485.Read();
            buf.write(b, 0, b.length);
         }
         byte[] payload = parseReply(buf.toByteArray(), request);
         if (payload != null) return payload;
      }
      logger.finer("IonicAgilentIPCMini:transact> no valid reply, got " + buf.size() + " bytes");
      return null;
   }

   private byte[] parseReply(byte[] raw, byte[] request) {
      int start = 0;
      if (raw.length >= request.length && Arrays.equals(Arrays.copyOf(raw, request.length), request))
         start = request.length; // skip echo
      for (int i = start; i < raw.length; i++) {
         if (raw[i] != 0x02) continue;
         for (int j = i + 1; j + 2 < raw.length; j++) {
            if (raw[j] != 0x03) continue;
            int crc = 0;
            for (int k = i + 1; k <= j; k++) crc ^= raw[k] & 0xFF;
            String got = new String(raw, j + 1, 2, StandardCharsets.ISO_8859_1);
            if (!got.equalsIgnoreCase(String.format("%02X", crc))) {
               logger.finer("IonicAgilentIPCMini:parseReply> CRC error");
               return null;
            }
            return Arrays.copyOfRange(raw, i, j); // STX ADDR ... (without ETX)
         }
      }
      return null;
   }

   private String replyCodeName(int code) {
      switch (code) {
         case NACK: return "NACK";
         case UNKNOWN_WIN: return "unknown window";
         case DATA_TYPE_ERR: return "data type error";
         case OUT_OF_RANGE: return "out of range";
         case WIN_DISABLED: return "window disabled";
         case -1: return "no reply";
         default: return String.format("code 0x%02X", code);
      }
   }

};
