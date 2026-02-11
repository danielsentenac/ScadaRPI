/*
 * This Class is the implementation of the SupervisorClient device
 *
 */
import java.util.*;
import java.io.IOException;
import java.lang.Exception;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URLConnection; //for url management 
import java.net.URL;
import java.io.ObjectInputStream; //for stream management
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ConnectException;
import java.net.CookieManager;
import java.net.CookieHandler;

public class SupervisorClient extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   Vector<String> svrNameList = new Vector<String>();
   Vector<String> svrValueList = new Vector<String>();

   public SupervisorClient (String _name,
                            int _mbRegisterStart) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("SupervisorClient:SupervisorClient> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Valves
     addDataElement( new DataElement(name, "VAC_SQZDET1_VSPAREST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));

     // Commands
     addDataElement( new DataElement(name, "VAC_SQZDET1_VSPARECMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // Controller Controllino comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("SupervisorClient:SupervisorClient> " + name + " Modbus registers ends at offset " + mbRegisterEnd);

     // Fill svrNameList vector
     svrNameList.addElement("VAC_SQZDET1_VSPAREST");
    
     
     // Necessary to handle tomcat sessions
       CookieManager cookieManager = new CookieManager();
       CookieHandler.setDefault(cookieManager);
   }
   /**
       A connection method called every time a connection to the servlet is needed. 
       The servlet path points to "jchv" which contains the main servlet 
       for channel visualization.
    */
    protected URLConnection getServerConnection(String url)
            throws MalformedURLException, IOException {
        //System.out.println(getCodeBase() + " " + getDocumentBase());
        URL urlServlet = new URL(url);
        URLConnection con = urlServlet.openConnection();
        con.setConnectTimeout(5000);
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
        return con;
    }
   
   @SuppressWarnings("unchecked")
   public void updateDeviceData() {
   
     // Get monitoring data from device using RS232 Comm

     DataElement vspare = getDataElement("VAC_SQZDET1_VSPAREST");
     
     DataElement dcom = getDataElement("COMST");
     
     try {
                 
        popCommand();  // Execute commands in the loop is more reactive
   
        // Get data from (tomcat) supervisor server
        URLConnection con = getServerConnection("http://olserver135.virgo.infn.it:8081/jchv/jchv");
        OutputStream outstream = con.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(outstream);
        oos.writeObject(svrNameList);
        oos.flush();
        oos.close();

        logger.finer("SupervisorClient:updateDeviceData> request supervisor: " + svrNameList);

        // receive result from servlet
        InputStream instr = con.getInputStream();
        ObjectInputStream inputFromServlet = new ObjectInputStream(instr);
        svrValueList = (Vector <String>) inputFromServlet.readObject();
        inputFromServlet.close();
        instr.close();

        logger.finer("SupervisorClient:updateDeviceData> receive from supervisor: " + svrValueList);

        vspare.value = Double.parseDouble(svrValueList.elementAt(0));

        dcom.value = 0; // if arriving here COM OK
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("SupervisorClient:updateDeviceData> Communication with " + name + " back!");
        }
     }
     catch (Exception ex) {
        ex.printStackTrace();
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "SupervisorClient:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "SupervisorClient:updateDeviceData>" + ex.getMessage());
        }
        setErrorComStatus();
     }
   }
   @SuppressWarnings("unchecked")
   public void executeCommand( DataElement e ) {
      
      try {
         String value = "";
         if ( e.name.contains("VSPARECMD") ) { // Valve Vspare Open/Close command
            if ( e.value == 1 )      // Open Valve
              value = "1";
            else if ( e.value == 2 ) // Close Valve
              value = "2";
         }
         // Prepare command to (tomcat) supervisor
         Vector<String> command = new Vector<String>();
         command.addElement("ModbusVac");
         command.addElement("SETREGISTER");
         command.addElement("-t");
         command.addElement("VAC_SQZDET1");
         command.addElement("-t");
         command.addElement(e.name.replace("VAC_SQZDET1",""));
         command.addElement("-d");
         command.addElement(value);
         logger.fine("SupervisorClient:executeCommand> Send command = " + command);
         // Send command to (tomcat) supervisor
         try {
            // send data to the servlet
	    URLConnection con = getServerConnection("http://olserver135.virgo.infn.it:9081/jcmd/jcmd");
	    OutputStream outstream = con.getOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(outstream);
	    oos.writeObject(command);
	    oos.flush();
	    oos.close();
	    
	    // receive result from servlet
	    InputStream instr = con.getInputStream();
	    ObjectInputStream inputFromServlet = new ObjectInputStream(instr);
	    String status = (String) inputFromServlet.readObject();
	    inputFromServlet.close();
	    instr.close();
	    logger.fine("SupervisorClient:executeCommand> Response from jcmd servlet: " + status);
	    
         } catch (ConnectException ex) {
            ex.printStackTrace();
         } catch (IOException ex) {
            ex.printStackTrace();
         } catch (Exception ex) {
            ex.printStackTrace();
         }
         Thread.sleep(2000); // Wait before resetting (All channels are trigger that must be ressetted by the Client)
         command.setElementAt("0",7); // Reset value
         logger.fine("SupervisorClient:executeCommand> Send RESET command = " + command);
         // Send Reset command to (tomcat) supervisor
         try {
            // send data to the servlet
	    URLConnection con = getServerConnection("http://olserver135.virgo.infn.it:8081/jcmd/jcmd");
	    OutputStream outstream = con.getOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(outstream);
	    oos.writeObject(command);
	    oos.flush();
	    oos.close();
	    
	    // receive result from servlet
	    InputStream instr = con.getInputStream();
	    ObjectInputStream inputFromServlet = new ObjectInputStream(instr);
	    String status = (String) inputFromServlet.readObject();
	    inputFromServlet.close();
	    instr.close();
	    logger.fine("SupervisorClient:executeCommand> Response from jcmd servlet: " + status);
	    
         } catch (ConnectException ex) {
            ex.printStackTrace();
         } catch (IOException ex) {
            ex.printStackTrace();
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
      catch (Exception ex) {
        logger.log(Level.SEVERE, "SupervisorClient:executeCommand>" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "SupervisorClient:executeCommand> Communication with " + name + " interrupted");
     }
   }
}; 
