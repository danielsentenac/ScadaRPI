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

import javax.swing.SwingUtilities;

public class SupervisorClient extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   Vector<String> svrNameList = new Vector<String>();
   Vector<String> svrValueList = new Vector<String>();

   public SupervisorClient (String _name,
                            int _mbRegisterStart) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("SupervisorClient:SupervisorClient> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart-1;

     // PCOUNTER FLAGS CHANNELS
     //    SwingUtilities.invokeLater(new Runnable() {
     //        public void run() {
    
       addDataElement( new DataElement(name, "VAC_PCOUNTER1500N_UM03FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTER1500N_UM05FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTER1500N_UM1FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTER1500N_UM205FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTER1500N_UM5FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTER1500N_UM10FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTER1500N_INSTRST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTER1500N_ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTER1500N_COMST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
    

     // Controller Controllino comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("SupervisorClient:SupervisorClient> " + name + " Modbus registers ends at offset " + mbRegisterEnd);
         // }
   //    });
     

     // Fill svrNameList vector
       svrNameList.addElement("VAC_PCOUNTER1500N_UM03FLG");
       svrNameList.addElement("VAC_PCOUNTER1500N_UM05FLG");
       svrNameList.addElement("VAC_PCOUNTER1500N_UM1FLG");
       svrNameList.addElement("VAC_PCOUNTER1500N_UM205FLG");
       svrNameList.addElement("VAC_PCOUNTER1500N_UM5FLG");
       svrNameList.addElement("VAC_PCOUNTER1500N_UM10FLG");
       svrNameList.addElement("VAC_PCOUNTER1500N_INSTRST");
       svrNameList.addElement("VAC_PCOUNTER1500N_ST");
       svrNameList.addElement("VAC_PCOUNTER1500N_RackStatus");

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
        HttpURLConnection con = (HttpURLConnection) urlServlet.openConnection();
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
        con.disconnect();
        
        logger.finer("SupervisorClient:updateDeviceData> receive from supervisor: " + svrValueList);
        
        
        // Fill svrNameList vector
        int pos = 0;
       
	DataElement d1 = getDataElement("VAC_PCOUNTER1500N_UM03FLG");
	DataElement d2 = getDataElement("VAC_PCOUNTER1500N_UM05FLG");
	DataElement d3 = getDataElement("VAC_PCOUNTER1500N_UM1FLG");
	DataElement d4 = getDataElement("VAC_PCOUNTER1500N_UM205FLG");
	DataElement d5 = getDataElement("VAC_PCOUNTER1500N_UM5FLG");
	DataElement d6 = getDataElement("VAC_PCOUNTER1500N_UM10FLG");
	DataElement d7 = getDataElement("VAC_PCOUNTER1500N_INSTRST");
	DataElement d8 = getDataElement("VAC_PCOUNTER1500N_ST");
	DataElement d9 = getDataElement("VAC_PCOUNTER1500N_COMST");
	
        if (!svrValueList.elementAt(0+pos).equals("NOTEXIST"))
	 d1.value = Double.parseDouble(svrValueList.elementAt(0));
	else
	 d1.value = 255;
	if (!svrValueList.elementAt(1+pos).equals("NOTEXIST"))
	 d2.value = Double.parseDouble(svrValueList.elementAt(1));
	else
	 d2.value = 255;
	if (!svrValueList.elementAt(2+pos).equals("NOTEXIST"))
	 d3.value = Double.parseDouble(svrValueList.elementAt(2));
	else
	 d3.value = 255;
	if (!svrValueList.elementAt(3+pos).equals("NOTEXIST"))
	 d4.value = Double.parseDouble(svrValueList.elementAt(3));
	else
	 d4.value = 255;
	if (!svrValueList.elementAt(4+pos).equals("NOTEXIST"))
	 d5.value = Double.parseDouble(svrValueList.elementAt(4));
	else
	 d5.value = 255;
	if (!svrValueList.elementAt(5+pos).equals("NOTEXIST"))
	 d6.value = Double.parseDouble(svrValueList.elementAt(5));
	else
	 d6.value = 255;
	if (!svrValueList.elementAt(6+pos).equals("NOTEXIST"))
	 d7.value = Double.parseDouble(svrValueList.elementAt(6));
	else
	 d7.value = 255;
	if (!svrValueList.elementAt(7+pos).equals("NOTEXIST"))
	 d8.value = Double.parseDouble(svrValueList.elementAt(7));
	else
	 d8.value = 255;
	if (!svrValueList.elementAt(8+pos).equals("NOTEXIST"))
	 d9.value = Double.parseDouble(svrValueList.elementAt(8));
	else
	 d9.value = 255;
        
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
