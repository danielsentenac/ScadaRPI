/*
 * This Class is the implementation of the Leybold LEYSPEC RGA device
 * (LEYSPEC view 100/100S/200/200S, LEYSPEC ultra 200S/300S).
 *
 * Communication: TCP/IP (port 1024) on the fixed IP address selected by the
 * DIP switch of the RGA electronics unit (192.168.250.11 ... 192.168.250.26).
 * Protocol: "RGA Communication Protocol" Leybold 300958778_002_C0.
 *
 *   PC -> RGA : "#" + address(2) + command(2) + parameter + CR LF
 *   RGA -> PC : "#" + address(2) + data + checksum(4 hex) + CR LF
 *   ACK/NAK   : "#" + address(2) + ACK(0x06)/NAK(0x15) + error code(2) + checksum + CR LF
 *
 * The device runs a continuous step scan ("scan mode", recipe G0) over the
 * mass range [1, lastMass]; the last complete values are read with DD.
 * Pressures are requested in Pa (RGA default unit) and stored in mbar.
 */
import java.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class RgaLeyspec extends Device {

   private static final Logger logger = Logger.getLogger("Main");

   public static final int MAX_MASS = 199;           // Number of mass channels published (001..199), as the RGA plot
   private static final int CONNECT_TIMEOUT_MS = 3000;
   private static final int REPLY_TIMEOUT_MS = 3000;
   private static final int RECONNECT_PAUSE_MS = 5000;
   private static final char ACK = 0x06;
   private static final char NAK = 0x15;
   private static final double PA_TO_MBAR = 0.01;
   private static final double NOT_MEASURED_PA = 1.0e-14; // Value returned by the RGA for a channel not measured (yet)

   private String host;
   private int port;
   private String address;        // RGA address number "01".."16" (sensor number, same as the DIP switch address)
   private int requestedLastMass; // Last mass asked by the user (limited by the model range and MAX_MASS)
   private int lastMass;          // Last mass really scanned
   private int speed;             // Measurement speed SD 1..6 (50, 100, 200, 500, 1000, 2000 ms/mass)
   private String ionSourceRecipe;// Ion source part of the recipe ("EE50" for the view series)

   private Socket socket;
   private InputStream in;
   private OutputStream out;
   private boolean hasSEM = true;       // From CS answer
   private boolean hasTwoFilaments = true;

   /**
    * @param _name            Device name (no '_' allowed)
    * @param _mbRegisterStart Starting Modbus register offset
    * @param _host            Fixed IP address of the RGA (192.168.250.11 ... 26, see DIP switch)
    * @param _port            TCP port (1024)
    * @param _address         RGA address number (1..16)
    * @param _lastMass        Last mass of the scan (first mass is 1)
    * @param _speed           Measurement speed 1..6 (3 = 200 ms/mass)
    * @param _ionSourceRecipe Ion source settings appended to the scan recipe:
    *                         "EE50" for the LEYSPEC view series,
    *                         "EE70VS140VG160" (see protocol table 3) for the LEYSPEC ultra series
    */
   public RgaLeyspec (String _name,
                      int _mbRegisterStart,
                      String _host,
                      int _port,
                      int _address,
                      int _lastMass,
                      int _speed,
                      String _ionSourceRecipe) {

     name = _name; // Device name
     host = _host;
     port = _port;
     address = String.format("%02d", _address);
     requestedLastMass = _lastMass;
     lastMass = Math.max(2, Math.min(_lastMass, MAX_MASS));
     speed = Math.max(1, Math.min(_speed, 6));
     ionSourceRecipe = _ionSourceRecipe;

     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("RgaLeyspec:RgaLeyspec> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // RGA data (partial pressures in mbar)
     addDataElement( new DataElement(name, massName(1), DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     for ( int i = 2 ; i <= MAX_MASS ; i++ )
        addDataElement( new DataElement(name, massName(i), DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     // Total pressure (mbar)
     addDataElement( new DataElement(name, "TP", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));

     // RGA properties (read only)
     addDataElement( new DataElement(name, "RUNNING", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));  // 0: stopped, 1: measuring
     addDataElement( new DataElement(name, "EMISSION", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1)); // 0: filament off, 1: on
     addDataElement( new DataElement(name, "SEMHV", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));    // 0: SEM high voltage off, 1: on
     addDataElement( new DataElement(name, "RF", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));       // 0: RF off, 1: on
     addDataElement( new DataElement(name, "ERROR", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));    // 0: normal, 1: error
     // Error detail bits : 1 filament, 2 SEM, 4 RF, 8 total pressure interlock, 16 external interlock
     addDataElement( new DataElement(name, "ERRORCODE", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     // Commands triggers (1 = ON, 2 = OFF)
     addDataElement( new DataElement(name, "EMISSIONONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1)); // Filament ON/OFF
     addDataElement( new DataElement(name, "SEMONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));      // Detector: 1 = SEM, 2 = Faraday
     addDataElement( new DataElement(name, "RUNNINGONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));  // Scan START/STOP

     // RGA writable values
     addDataElement( new DataElement(name, "FILAMENT", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1)); // 0: off, 1: filament 1 on, 2: filament 2 on (write 1/2 = select)
     addDataElement( new DataElement(name, "MODE", DataType.READ_AND_WRITE_STATUS,RegisterType.INT16,mbRegisterEnd+=1));     // 0: Faraday, 1: SEM

     // Com Status
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.fine("RgaLeyspec:RgaLeyspec> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     // Start in communication failure state: the connection is made by the device thread
     for (Map.Entry<String, DataElement> e : this.dataList.entrySet()) {
        DataElement dataElement = e.getValue();
        if ( dataElement.type == DataType.READ_ONLY_STATUS || dataElement.type == DataType.READ_AND_WRITE_STATUS )
           dataElement.value = 255;
        else if ( dataElement.type == DataType.COM_STATUS )
           dataElement.value = 1;
     }
   }

   public static String massName(int mass) {
      return String.format("%03d", mass);
   }

   /*******************************************************************************************/
   /* Monitoring loop                                                                         */
   /*******************************************************************************************/
   public void updateDeviceData() {

     DataElement dcom = getDataElement("COMST");

     try {
        if ( !isConnected() ) {
           try {
              connect();
           }
           catch (IOException ex) {
              if ( hasWarned == false )
                 logger.log(Level.WARNING, "RgaLeyspec:updateDeviceData> No connection with " + name + " (" + host + ":" + port + "): " + ex.getMessage());
              disconnect();
              setErrorComStatus();
              commandSetQueue.clear(); // Commands cannot be executed
              Thread.sleep(RECONNECT_PAUSE_MS);
              return;
           }
        }

        popCommand();  // Execute commands in the loop is more reactive

        readStatus();

        if ( getDataElement("RUNNING").value == 1 )
           readScanData();

        dcom.value = 0; // OK COM
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("RgaLeyspec:updateDeviceData> Communication with " + name + " back!");
        }
     }
     catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
     }
     catch (Exception ex) {
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "RgaLeyspec:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "RgaLeyspec:updateDeviceData>" + ex);
        }
        disconnect();
        setErrorComStatus();
     }
   }

   // Request operating condition (DS) and error condition (ES)
   private void readStatus() throws IOException {

      // DS answer : ExFxSxRxDxAx
      String ds = query("DS");
      int error    = flag(ds, 'E');
      int filament = flag(ds, 'F'); // 0: off, 1: Fil1 on, 2: Fil2 on
      int sem      = flag(ds, 'S'); // 0: off, 1: on
      int rf       = flag(ds, 'R'); // 0: off, 1: on
      int detector = flag(ds, 'D'); // 0: SEM, 1: FC
      int action   = flag(ds, 'A'); // 0: Measuring, 1: Stop

      getDataElement("ERROR").value = error;
      getDataElement("FILAMENT").value = filament;
      getDataElement("EMISSION").value = (filament > 0) ? 1 : 0;
      getDataElement("SEMHV").value = sem;
      getDataElement("RF").value = rf;
      getDataElement("MODE").value = (detector == 0) ? 1 : 0;

      DataElement running = getDataElement("RUNNING");
      double wasRunning = running.value;
      running.value = (action == 0) ? 1 : 0;
      if ( running.value == 0 && wasRunning != 0 )
         resetScanData(); // SCAN is OFF : reset all values

      // ES answer : ERabcde (a filament, b SEM, c RF, d TP interlock, e external interlock)
      DataElement errorCode = getDataElement("ERRORCODE");
      if ( error == 0 )
         errorCode.value = 0;
      else {
         String es = query("ES");
         int code = 0;
         if ( es.startsWith("ER") ) {
            for (int i = 0; i < 5 && 2 + i < es.length(); i++)
               if ( es.charAt(2 + i) == '1' ) code |= (1 << i);
         }
         if ( code != errorCode.value )
            logger.log(Level.WARNING, "RgaLeyspec:readStatus> " + name + " error condition " + es
                       + " (filament, SEM, RF, total pressure interlock, external interlock)");
         errorCode.value = code;
      }
   }

   // Request measurement data (DD) in scan mode :
   // Ch1,Ch2,...,Chn,TP,AN1,AN2,ERR,TPSET,   Chx = abcde (a.bc x 10^-de), TP = abcd (a.bc x 10^-d)
   private void readScanData() throws IOException {

      String dd = query("DD");
      String[] fields = dd.split(",");
      int nChannels = fields.length - 5;
      if ( nChannels < 1 )
         throw new IOException("unexpected DD answer [" + printable(dd) + "]");
      try {
         for (int i = 0; i < nChannels && i < MAX_MASS; i++) {
            String f = fields[i].trim();
            double pa = Integer.parseInt(f.substring(0, 3)) / 100.0 * Math.pow(10, -Integer.parseInt(f.substring(3)));
            getDataElement(massName(i + 1)).value = (pa <= NOT_MEASURED_PA) ? 0.0 : pa * PA_TO_MBAR;
         }
         String tp = fields[nChannels].trim();
         double tpPa = Integer.parseInt(tp.substring(0, 3)) / 100.0 * Math.pow(10, -Integer.parseInt(tp.substring(3)));
         getDataElement("TP").value = tpPa * PA_TO_MBAR;
      }
      catch (RuntimeException ex) { // NumberFormat / StringIndexOutOfBounds
         throw new IOException("malformed DD answer [" + printable(dd) + "]");
      }
   }

   private void resetScanData() {
      for (int i = 1 ; i <= MAX_MASS; i++)
         getDataElement(massName(i)).value = 0.0;
      getDataElement("TP").value = 0.0;
   }

   /*******************************************************************************************/
   /* Commands                                                                                */
   /*******************************************************************************************/
   public void executeCommand (DataElement e) {

      logger.fine("RgaLeyspec:executeCommand> command = " + e.name + " value = " + e.setvalue);
      try {
         if ( !isConnected() ) {
            logger.log(Level.WARNING, "RgaLeyspec:executeCommand> " + name + " not connected, command " + e.name + " ignored");
         }
         else if (e.name.equals("EMISSIONONOFF")) { // Filament ON/OFF
            if ( e.setvalue == 1 )
               filamentOn();
            else if ( e.setvalue == 2 ) {
               if ( isMeasuring() ) stopScan(); // Never leave a measurement running without filament
               command("F0");
            }
         }
         else if (e.name.equals("RUNNINGONOFF")) { // Scan START/STOP
            if ( e.setvalue == 1 )
               startScan();
            else if ( e.setvalue == 2 )
               stopScan();
         }
         else if (e.name.equals("SEMONOFF")) { // Detector selection : 1 = SEM, 2 = Faraday
            if ( e.setvalue == 1 )
               setDetector(true);
            else if ( e.setvalue == 2 )
               setDetector(false);
         }
         else if (e.name.equals("MODE")) { // Detector selection : 0 = Faraday, 1 = SEM
            if ( e.setvalue == 0 )
               setDetector(false);
            else if ( e.setvalue == 1 )
               setDetector(true);
         }
         else if (e.name.equals("FILAMENT")) { // Filament selection : 1 or 2 (filament must be off)
            if ( !hasTwoFilaments )
               logger.log(Level.WARNING, "RgaLeyspec:executeCommand> " + name + " has only one filament");
            else if ( flag(query("DS"), 'F') != 0 )
               logger.log(Level.WARNING, "RgaLeyspec:executeCommand> " + name + " turn off the filament before selecting another one");
            else if ( e.setvalue == 1 )
               command("FA");
            else if ( e.setvalue == 2 )
               command("FB");
         }

         readStatus(); // Refresh status at once

         // Reset TRIGGER value
         if (e.type == DataType.TRIGGER) {
            Thread.sleep(2000); // Wait before resetting
            e.value = 0;
            if ( holdingRegisters != null )
               holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "RgaLeyspec>executeCommand:" + ex);
        disconnect();
        setErrorComStatus();
        logger.log(Level.WARNING, "RgaLeyspec:executeCommand> Communication with " + name + " interrupted");
     }
   }

   private boolean isMeasuring() throws IOException {
      return flag(query("DS"), 'A') == 0;
   }

   private boolean filamentOn() throws IOException, InterruptedException {
      if ( flag(query("DS"), 'F') != 0 ) return true; // Already on
      if ( !command("F1") ) return false;
      Thread.sleep(3000); // Let the emission settle and the RGA check its interlocks
      String ds = query("DS");
      if ( flag(ds, 'F') == 0 ) {
         logger.log(Level.WARNING, "RgaLeyspec:filamentOn> " + name + " filament did not turn on (" + ds + ", " + query("ES") + ")");
         return false;
      }
      return true;
   }

   // Start a continuous step scan : recipe, filament, RF, (SEM high voltage), start
   private void startScan() throws IOException, InterruptedException {

      if ( isMeasuring() ) return; // Already running

      String recipe = String.format("G0FM001LM%03dSD%d", lastMass, speed) + ionSourceRecipe;
      if ( !command(recipe) ) return;
      if ( !filamentOn() ) return;
      if ( !command("R1") ) return;
      Thread.sleep(1000);
      if ( hasSEM && flag(query("DS"), 'D') == 0 ) { // SEM detector selected : turn on its high voltage
         if ( !command("S1") ) { command("R0"); return; }
         Thread.sleep(1000);
      }
      if ( !command("ST") ) {
         if ( hasSEM ) command("S0");
         command("R0");
         return;
      }
      resetScanData();
      logger.info("RgaLeyspec:startScan> " + name + " scan started (" + recipe + ")");
   }

   // Stop the scan : stop, SEM high voltage off, RF off (the filament is left as it is)
   private void stopScan() throws IOException, InterruptedException {
      command("SP");
      if ( hasSEM ) command("S0");
      command("R0");
      resetScanData();
      logger.info("RgaLeyspec:stopScan> " + name + " scan stopped");
   }

   // Select the detector (measurement is stopped and restarted if needed)
   private void setDetector(boolean sem) throws IOException, InterruptedException {

      if ( sem && !hasSEM ) {
         logger.log(Level.WARNING, "RgaLeyspec:setDetector> " + name + " is not equipped with a SEM");
         return;
      }
      boolean isSem = flag(query("DS"), 'D') == 0;
      if ( isSem == sem ) return; // Nothing to do
      boolean wasMeasuring = isMeasuring();
      if ( wasMeasuring ) stopScan();
      if ( hasSEM ) command("S0"); // SEM high voltage always off before switching
      if ( !command(sem ? "SS" : "SF") ) return;
      if ( wasMeasuring ) startScan();
   }

   /*******************************************************************************************/
   /* TCP communication                                                                       */
   /*******************************************************************************************/
   private boolean isConnected() {
      return socket != null && socket.isConnected() && !socket.isClosed();
   }

   private void connect() throws IOException {

      disconnect();
      socket = new Socket();
      socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
      socket.setSoTimeout(REPLY_TIMEOUT_MS);
      socket.setTcpNoDelay(true);
      in = socket.getInputStream();
      out = socket.getOutputStream();

      // Connect with RGA (the RGA is in compatible mode after power on or disconnection)
      if ( !command("CN") ) throw new IOException("CN refused");
      // Extended mode (normal operation)
      if ( !command("CM2") ) throw new IOException("CM2 refused");
      // RGA type : abcde (a electronics, b mass range, c SEM, d filaments, e degas)
      String cs = query("CS");
      if ( cs.length() >= 5 ) {
         int[] ranges = {50, 100, 200, 300};
         int b = cs.charAt(1) - '0';
         int range = (b >= 0 && b < ranges.length) ? ranges[b] : MAX_MASS;
         lastMass = Math.max(2, Math.min(Math.min(requestedLastMass, range), MAX_MASS));
         hasSEM = cs.charAt(2) == '1';
         hasTwoFilaments = cs.charAt(3) == '1';
      }
      String version = query("VR");
      // Pressure unit Pa (RGA default, values are converted to mbar here)
      if ( !command("UC0") ) throw new IOException("UC0 refused");
      logger.info("RgaLeyspec:connect> Connected to " + name + " (" + host + ":" + port + ") type=" + cs + " version=" + version
                  + " scan range=[1," + lastMass + "] SEM=" + hasSEM + " two filaments=" + hasTwoFilaments);
   }

   private void disconnect() {
      try { if ( socket != null ) socket.close(); } catch (IOException ex) { }
      socket = null;
      in = null;
      out = null;
   }

   // Send a setting command, return true if acknowledged
   private boolean command(String cmd) throws IOException {
      String answer = query(cmd);
      if ( answer.length() > 0 && answer.charAt(0) == ACK )
         return true;
      if ( answer.length() > 0 && answer.charAt(0) == NAK ) {
         String code = answer.substring(1);
         String reason = code.equals("01") ? "unknown command" : code.equals("02") ? "parameter error"
                       : code.equals("03") ? "condition error" : "error " + code;
         logger.log(Level.WARNING, "RgaLeyspec:command> " + name + " refused command " + cmd + " : " + reason);
         return false;
      }
      throw new IOException("unexpected answer to " + cmd + " [" + printable(answer) + "]");
   }

   // Send a command and return the data part of the answer (without header, address, checksum, delimiter)
   private synchronized String query(String cmd) throws IOException {

      if ( !isConnected() ) throw new IOException("not connected");

      // Drop anything left over from a previous (late) answer
      while ( in.available() > 0 ) in.read();

      String frame = "#" + address + cmd + "\r\n";
      logger.finer("RgaLeyspec:query> write to device:" + printable(frame));
      out.write(frame.getBytes("ISO-8859-1"));
      out.flush();

      // Read up to the LF delimiter
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      int c;
      try {
         while ( (c = in.read()) != -1 ) {
            if ( c == '\n' ) break;
            buffer.write(c);
         }
      }
      catch (SocketTimeoutException ex) {
         throw new IOException("timeout waiting answer to " + cmd + " [" + printable(buffer.toString("ISO-8859-1")) + "]");
      }
      if ( c == -1 ) throw new IOException("connection closed by " + name);

      String answer = buffer.toString("ISO-8859-1");
      if ( answer.endsWith("\r") ) answer = answer.substring(0, answer.length() - 1);
      logger.finer("RgaLeyspec:query> answer from device:" + printable(answer));

      // Check header, address and checksum (sum of all bytes from '#' to the last parameter byte)
      if ( answer.length() < 7 || !answer.startsWith("#" + address) )
         throw new IOException("bad answer to " + cmd + " [" + printable(answer) + "]");
      String body = answer.substring(0, answer.length() - 4);
      String checksum = answer.substring(answer.length() - 4);
      int sum = 0;
      for (int i = 0; i < body.length(); i++) sum += body.charAt(i);
      if ( !String.format("%04X", sum & 0xFFFF).equalsIgnoreCase(checksum) )
         throw new IOException("bad checksum in answer to " + cmd + " [" + printable(answer) + "]");

      return body.substring(3);
   }

   // Value of a one-digit flag in a "ExFxSxRxDxAx" like answer (-1 if missing)
   private static int flag(String answer, char key) throws IOException {
      int pos = answer.indexOf(key);
      if ( pos < 0 || pos + 1 >= answer.length() || !Character.isDigit(answer.charAt(pos + 1)) )
         throw new IOException("unexpected status answer [" + printable(answer) + "]");
      return answer.charAt(pos + 1) - '0';
   }

   // Make control characters visible in log messages
   private static String printable(String str) {
      StringBuilder sb = new StringBuilder();
      for (char ch : str.toCharArray()) {
         if (ch == '\r') sb.append("\\r");
         else if (ch == '\n') sb.append("\\n");
         else if (ch == ACK) sb.append("[ACK]");
         else if (ch == NAK) sb.append("[NAK]");
         else if (ch < 32 || ch > 126) sb.append(String.format("\\x%02X", (int) ch));
         else sb.append(ch);
      }
      return sb.toString();
   }
};
