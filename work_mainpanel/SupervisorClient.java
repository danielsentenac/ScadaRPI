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
     for (int i = 1; i <= 8; i++) {
       addDataElement( new DataElement(name, "VAC_PCOUNTERCB_PC" + i + "_UM03FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERCB_PC" + i + "_UM05FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERCB_PC" + i + "_UM1FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERCB_PC" + i + "_UM205FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERCB_PC" + i + "_UM5FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERCB_PC" + i + "_UM10FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERCB_PC" + i + "_INSTRST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERCB_PC" + i + "_ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERCB_PC" + i + "_COMST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     }
     
     // MOBILE PCOUNTERS
     for (int i = 1; i <= 3; i++) {
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM03FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM05FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM1FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM205FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM5FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM10FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_INSTRST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_RackStatus", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
     }
     // Commands
     addDataElement( new DataElement(name, "VAC_REMOTESCROLL_VLPCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VAC_REMOTESCROLL_VLSCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VAC_REMOTESCROLL_MG3_CH3ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     addDataElement( new DataElement(name, "VAC_REMOTESCROLL_MG1_CH6ONOFF", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));

     // Controller Controllino comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("SupervisorClient:SupervisorClient> " + name + " Modbus registers ends at offset " + mbRegisterEnd);
         // }
   //    });
     

     // Fill svrNameList vector
     for (int i = 1; i <= 8; i++) {
       svrNameList.addElement("VAC_PCOUNTERCB_PC" + i + "_UM03FLG");
       svrNameList.addElement("VAC_PCOUNTERCB_PC" + i + "_UM05FLG");
       svrNameList.addElement("VAC_PCOUNTERCB_PC" + i + "_UM1FLG");
       svrNameList.addElement("VAC_PCOUNTERCB_PC" + i + "_UM205FLG");
       svrNameList.addElement("VAC_PCOUNTERCB_PC" + i + "_UM5FLG");
       svrNameList.addElement("VAC_PCOUNTERCB_PC" + i + "_UM10FLG");
       svrNameList.addElement("VAC_PCOUNTERCB_PC" + i + "_INSTRST");
       svrNameList.addElement("VAC_PCOUNTERCB_PC" + i + "_ST");
       svrNameList.addElement("VAC_PCOUNTERCB_PC" + i + "_COMST");
     }
     for (int i = 1; i <= 3; i++) {
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM03FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM05FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM1FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM205FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM5FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM10FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_INSTRST");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_ST");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_RackStatus");
     }
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
        HttpURLConnection con = getServerConnection("http://example-host:8081/jchv/jchv");
        
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
        for (int i = 1; i <= 8; i++) {
       		DataElement d1 = getDataElement("VAC_PCOUNTERCB_PC" + i + "_UM03FLG");
       		DataElement d2 = getDataElement("VAC_PCOUNTERCB_PC" + i + "_UM05FLG");
      		DataElement d3 = getDataElement("VAC_PCOUNTERCB_PC" + i + "_UM1FLG");
       		DataElement d4 = getDataElement("VAC_PCOUNTERCB_PC" + i + "_UM205FLG");
       		DataElement d5 = getDataElement("VAC_PCOUNTERCB_PC" + i + "_UM5FLG");
       		DataElement d6 = getDataElement("VAC_PCOUNTERCB_PC" + i + "_UM10FLG");
       		DataElement d7 = getDataElement("VAC_PCOUNTERCB_PC" + i + "_INSTRST");
       		DataElement d8 = getDataElement("VAC_PCOUNTERCB_PC" + i + "_ST");
       		DataElement d9 = getDataElement("VAC_PCOUNTERCB_PC" + i + "_COMST");
       		
                if (!svrValueList.elementAt(0+pos).equals("NOTEXIST"))
        	 d1.value = Double.parseDouble(svrValueList.elementAt(0+pos));
        	else
        	 d1.value = 255;
        	if (!svrValueList.elementAt(1+pos).equals("NOTEXIST"))
        	 d2.value = Double.parseDouble(svrValueList.elementAt(1+pos));
        	else
        	 d2.value = 255;
        	if (!svrValueList.elementAt(2+pos).equals("NOTEXIST"))
        	 d3.value = Double.parseDouble(svrValueList.elementAt(2+pos));
        	else
        	 d3.value = 255;
        	if (!svrValueList.elementAt(3+pos).equals("NOTEXIST"))
        	 d4.value = Double.parseDouble(svrValueList.elementAt(3+pos));
        	else
        	 d4.value = 255;
        	if (!svrValueList.elementAt(4+pos).equals("NOTEXIST"))
        	 d5.value = Double.parseDouble(svrValueList.elementAt(4+pos));
        	else
        	 d5.value = 255;
        	if (!svrValueList.elementAt(5+pos).equals("NOTEXIST"))
        	 d6.value = Double.parseDouble(svrValueList.elementAt(5+pos));
        	else
        	 d6.value = 255;
        	if (!svrValueList.elementAt(6+pos).equals("NOTEXIST"))
        	 d7.value = Double.parseDouble(svrValueList.elementAt(6+pos));
        	else
        	 d7.value = 255;
        	if (!svrValueList.elementAt(7+pos).equals("NOTEXIST"))
        	 d8.value = Double.parseDouble(svrValueList.elementAt(7+pos));
        	else
        	 d8.value = 255;
        	if (!svrValueList.elementAt(8+pos).equals("NOTEXIST"))
        	 d9.value = Double.parseDouble(svrValueList.elementAt(8+pos));
        	else
        	 d9.value = 255;
        
        	pos+=9; // jump to next PCOUNTER data set
        }
        
        for (int i = 1; i <= 3; i++) {
       		DataElement d1 = getDataElement("VAC_PCOUNTERMOB" + i + "_UM03FLG");
       		DataElement d2 = getDataElement("VAC_PCOUNTERMOB" + i + "_UM05FLG");
      		DataElement d3 = getDataElement("VAC_PCOUNTERMOB" + i + "_UM1FLG");
       		DataElement d4 = getDataElement("VAC_PCOUNTERMOB" + i + "_UM205FLG");
       		DataElement d5 = getDataElement("VAC_PCOUNTERMOB" + i + "_UM5FLG");
       		DataElement d6 = getDataElement("VAC_PCOUNTERMOB" + i + "_UM10FLG");
       		DataElement d7 = getDataElement("VAC_PCOUNTERMOB" + i + "_INSTRST");
       		DataElement d8 = getDataElement("VAC_PCOUNTERMOB" + i + "_ST");
       		DataElement d9 = getDataElement("VAC_PCOUNTERMOB" + i + "_RackStatus");
       		
                if (!svrValueList.elementAt(0+pos).equals("NOTEXIST")) 
        	 d1.value = Double.parseDouble(svrValueList.elementAt(0+pos));
        	else
        	 d1.value = 255;
        	if (!svrValueList.elementAt(1+pos).equals("NOTEXIST"))
        	 d2.value = Double.parseDouble(svrValueList.elementAt(1+pos));
        	else
        	 d2.value = 255;
        	if (!svrValueList.elementAt(2+pos).equals("NOTEXIST"))
        	 d3.value = Double.parseDouble(svrValueList.elementAt(2+pos));
        	else
        	 d3.value = 255;
        	if (!svrValueList.elementAt(3+pos).equals("NOTEXIST"))
        	 d4.value = Double.parseDouble(svrValueList.elementAt(3+pos));
        	else
        	 d4.value = 255;
        	if (!svrValueList.elementAt(4+pos).equals("NOTEXIST"))
        	 d5.value = Double.parseDouble(svrValueList.elementAt(4+pos));
        	else
        	 d5.value = 255;
        	if (!svrValueList.elementAt(5+pos).equals("NOTEXIST"))
        	 d6.value = Double.parseDouble(svrValueList.elementAt(5+pos));
        	else
        	 d6.value = 255;
        	if (!svrValueList.elementAt(6+pos).equals("NOTEXIST"))
        	 d7.value = Double.parseDouble(svrValueList.elementAt(6+pos));
        	else
        	 d7.value = 255;
        	if (!svrValueList.elementAt(7+pos).equals("NOTEXIST"))
        	 d8.value = Double.parseDouble(svrValueList.elementAt(7+pos));
        	else
        	 d8.value = 255;
        	if (!svrValueList.elementAt(8+pos).equals("NOTEXIST")){
                   double tmpVal = Double.parseDouble(svrValueList.elementAt(8+pos));
                   if (tmpVal < 0) 
        	      d9.value = 1;
                   else d9.value = 0;
                }
        	else
        	 d9.value = 255;
        
        	pos+=9; // jump to next PCOUNTER data set
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
