/*
 * This Class is the implementation of the SupervisorClient2 device
 *
 */
import java.util.*;
import java.io.IOException;
import java.lang.Exception;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URLConnection; //for url management
 import java.net.HttpURLConnection; //for url management
import java.net.URL;
import java.net.URI;
import java.io.ObjectInputStream; //for stream management
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ConnectException;
import java.net.CookieManager;
import java.net.CookieHandler;
import java.util.Hashtable;

import javax.swing.SwingUtilities;

public class SupervisorClient2 extends Device implements ChannelList {

   private static final Logger logger = Logger.getLogger("Main");
   Vector<String> svrNameList = new Vector<String>();
   Vector<String> svrValueList = new Vector<String>();

   public SupervisorClient2 (String _name,
                            int _mbRegisterStart) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.fine("SupervisorClient2:SupervisorClient2> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart-1;

    // SwingUtilities.invokeLater(new Runnable() {
     //     public void run() {
              // CREATE O2 CHANNELS
     boolean lastIsVal = false;
     for (Map.Entry<String, String> e : PANEL2STATUS.entrySet()) {
         String key =  e.getKey();
         String value = e.getValue();
         value = value.replace(name+"_","");
         logger.finer("SupervisorClient2:SupervisorClient2> Add " + value);
         svrNameList.addElement(value);
         if ( !key.equals("")) {
            if (key.contains("VAL")) {
                if ( lastIsVal == false ) 
                    addDataElement( new DataElement(name, value, DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=1));
                else
                    addDataElement( new DataElement(name, value, DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
                lastIsVal = true;
            }
            if (key.contains("BUTTON")) {
                 if ( lastIsVal == false ) 
                     addDataElement( new DataElement(name, value, DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
                 else
                     addDataElement( new DataElement(name, value, DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
                 lastIsVal = false;
            }
         }
     }

     // COM Status
     if ( lastIsVal == false ) 
         addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     else
         addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
         
     mbRegisterEnd+=1;

     logger.finer("SupervisorClient2:SupervisorClient2> " + name + " Modbus registers ends at offset " + mbRegisterEnd);
     //     }
     //  });
     

     // Necessary to handle tomcat sessions
       CookieManager cookieManager = new CookieManager();
       CookieHandler.setDefault(cookieManager);
   }
   /**
       A connection method called every time a connection to the servlet is needed. 
       The servlet path points to "jchv" which contains the main servlet 
       for channel visualization.
    */
    protected HttpURLConnection getServerConnection(String url)
            throws MalformedURLException, IOException {
        //System.out.println(getCodeBase() + " " + getDocumentBase());
        URL urlServlet = URI.create(url).toURL();
        HttpURLConnection con = (HttpURLConnection)urlServlet.openConnection();
        con.setConnectTimeout(1000);
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
        return con;
    }
   
   @SuppressWarnings("unchecked")
   public void updateDeviceData() {
   
     // Get monitoring data
     
     DataElement dcom = getDataElement("COMST");
     
     try {
                 
        popCommand();  // Execute commands in the loop is more reactive
   
        // Get data from (tomcat) supervisor server
        HttpURLConnection con = getServerConnection("http://olserver134.virgo.infn.it:8081/jchv/jchv");
        //con.connect();
        OutputStream outstream = con.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(outstream);
        oos.writeObject(svrNameList);
        oos.flush();
        oos.close();
        con.disconnect();
        logger.finer("SupervisorClient2:updateDeviceData> request supervisor: " + svrNameList);

        // receive result from servlet
        InputStream instr = con.getInputStream();
        ObjectInputStream inputFromServlet = new ObjectInputStream(instr);
        svrValueList = (Vector <String>) inputFromServlet.readObject();
        inputFromServlet.close();
        instr.close();
       

        logger.finer("SupervisorClient2:updateDeviceData> receive from supervisor: " + svrValueList);
        
        
        // Fill svrNameList vector
        
        for (int i = 0; i < svrNameList.size() ; i++) {
            logger.finer("SupervisorClient2:updateDeviceData> Assigning value to " + svrNameList.elementAt(i));
            DataElement d = getDataElement(svrNameList.elementAt(i));
            if (d != null) {
                if (!svrValueList.elementAt(i).equals("NOTEXIST") && !svrValueList.elementAt(i).equals("TIMOUT"))
                    d.value = Double.parseDouble(svrValueList.elementAt(i));
                else
        	    d.value = 255;
            }
            else
              logger.warning("SupervisorClient2:updateDeviceData> no data element " + svrNameList.elementAt(i) + "!");
        }
        
        dcom.value = 0; // if arriving here COM OK
        
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("SupervisorClient2:updateDeviceData> Communication with " + name + " back!");
        }
     }
     catch (Exception ex) {
        ex.printStackTrace();
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "SupervisorClient2:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "SupervisorClient2:updateDeviceData>" + ex.getMessage());
        }
        setErrorComStatus();
     }
   }
   @SuppressWarnings("unchecked")
   public void executeCommand( DataElement e ) {
      
     
   }
}; 
