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

public class RgaMksSupervisorClient extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   Vector<String> svrNameList = new Vector<String>();
   Vector<String> svrValueList = new Vector<String>();

   public RgaMksSupervisorClient (String _name,
                            int _mbRegisterStart) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("SupervisorClient:SupervisorClient> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;
     
     addDataElement( new DataElement(name, "001", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     svrNameList.addElement("VAC_TUBE1500W_RGAGa3_001");
     for ( int i = 2 ; i <= 200 ; i++ ) {
         if ( i < 10 ) {
           addDataElement( new DataElement(name, "00" + Integer.toString(i),
                                           DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
           svrNameList.addElement("VAC_TUBE1500W_RGAGa3_00" + Integer.toString(i));
         }
         else if ( i < 100 && i >= 10 ) {
           addDataElement( new DataElement(name, "0" + Integer.toString(i),
                                           DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
           svrNameList.addElement("VAC_TUBE1500W_RGAGa3_0" + Integer.toString(i));
         }
         else  {
           addDataElement( new DataElement(name, Integer.toString(i),
                                           DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
           svrNameList.addElement("VAC_TUBE1500W_RGAGa3_" + Integer.toString(i));
         }
     }
 
     // Com Status
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
     
     mbRegisterEnd+=1;

     logger.finer("SupervisorClient:SupervisorClient> " + name + " Modbus registers ends at offset " + mbRegisterEnd);
     
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
   
     // Get monitoring data from device using Supervisor Client Comm
     
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

        DataElement e = null;
        for (int i = 1 ; i <= 200; i++) {
          if ( i < 10 ) 
            e = getDataElement("00" + Integer.toString(i));
          else if ( i < 100 && i >= 10 ) 
            e = getDataElement("0" + Integer.toString(i));
          else  
            e = getDataElement(Integer.toString(i));
          logger.finer("SupervisorClient:updateDeviceData> Treating " + e.name + " receive from supervisor: " + svrValueList.elementAt(i-1));
          e.value = Double.parseDouble(svrValueList.elementAt(i-1));
     }
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
      
   }
}; 
