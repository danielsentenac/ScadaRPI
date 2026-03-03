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

public class SupervisorClient2 extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   Vector<String> svrNameList = new Vector<String>();
   Vector<String> svrValueList = new Vector<String>();

   public SupervisorClient2 (String _name,
                            int _mbRegisterStart) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset

     logger.finer("SupervisorClient:SupervisorClient> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart-2;

     // PCOUNTER FLAGS CHANNELS
     //    SwingUtilities.invokeLater(new Runnable() {
     //        public void run() {
     for ( int i = 1; i < 4 ; i++ ) {
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM03", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM05", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM1", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM205", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM5", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM10", DataType.READ_ONLY_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM03FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=2));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM05FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM1FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM205FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM5FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_UM10FLG", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       // Particle counter sampling time value
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_SAMPLING", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
       // Particle counter holding time value
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_HOLDING", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
       // Particle counter cycle number value
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_CYCLE", DataType.READ_AND_WRITE_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
       // Particle counter flow value
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_FLOW", DataType.READ_ONLY_VALUE,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_INSTRST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));
       addDataElement( new DataElement(name, "VAC_PCOUNTERMOB" + i + "_COMST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));  
    }

    // Controller Controllino comm
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("SupervisorClient:SupervisorClient> " + name + " Modbus registers ends at offset " + mbRegisterEnd);
         // }
   //    });
     

     // Fill svrNameList vector
     for ( int i = 1; i < 4 ; i++ ) {
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM03");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM05");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM1");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM205");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM5");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM10");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM03FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM05FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM1FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM205FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM5FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_UM10FLG");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_SAMPLING");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_HOLDING");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_CYCLE");
       svrNameList.addElement("VAC_PCOUNTERMOB" + i + "_FLOW");
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
        for ( int i = 1; i < 4 ; i++ ) {
          DataElement d1v = getDataElement("VAC_PCOUNTERMOB" + i + "_UM03");
	  DataElement d2v = getDataElement("VAC_PCOUNTERMOB" + i + "_UM05");
	  DataElement d3v = getDataElement("VAC_PCOUNTERMOB" + i + "_UM1");
	  DataElement d4v = getDataElement("VAC_PCOUNTERMOB" + i + "_UM205");
	  DataElement d5v = getDataElement("VAC_PCOUNTERMOB" + i + "_UM5");
	  DataElement d6v = getDataElement("VAC_PCOUNTERMOB" + i + "_UM10");
	  DataElement d1f = getDataElement("VAC_PCOUNTERMOB" + i + "_UM03FLG");
	  DataElement d2f = getDataElement("VAC_PCOUNTERMOB" + i + "_UM05FLG");
	  DataElement d3f= getDataElement("VAC_PCOUNTERMOB" + i + "_UM1FLG");
	  DataElement d4f = getDataElement("VAC_PCOUNTERMOB" + i + "_UM205FLG");
	  DataElement d5f = getDataElement("VAC_PCOUNTERMOB" + i + "_UM5FLG");
	  DataElement d6f = getDataElement("VAC_PCOUNTERMOB" + i + "_UM10FLG");
	  DataElement ds = getDataElement("VAC_PCOUNTERMOB" + i + "_SAMPLING");
	  DataElement dh = getDataElement("VAC_PCOUNTERMOB" + i + "_HOLDING");
	  DataElement dc = getDataElement("VAC_PCOUNTERMOB" + i + "_CYCLE");
	  DataElement df = getDataElement("VAC_PCOUNTERMOB" + i + "_FLOW");
	  DataElement dist = getDataElement("VAC_PCOUNTERMOB" + i + "_INSTRST");
	  DataElement dst = getDataElement("VAC_PCOUNTERMOB" + i + "_ST");
	  DataElement dcomst = getDataElement("VAC_PCOUNTERMOB" + i + "_COMST");
	
        if (!svrValueList.elementAt(0+pos).equals("NOTEXIST"))
	 d1v.value = Double.parseDouble(svrValueList.elementAt(0+pos));
	else
	 d1v.value = 0;
	if (!svrValueList.elementAt(1+pos).equals("NOTEXIST"))
	 d2v.value = Double.parseDouble(svrValueList.elementAt(1+pos));
	else
	 d2v.value = 0;
	if (!svrValueList.elementAt(2+pos).equals("NOTEXIST"))
	 d3v.value = Double.parseDouble(svrValueList.elementAt(2+pos));
	else
	 d3v.value = 0;
	if (!svrValueList.elementAt(3+pos).equals("NOTEXIST"))
	 d4v.value = Double.parseDouble(svrValueList.elementAt(3+pos));
	else
	 d4v.value = 0;
	if (!svrValueList.elementAt(4+pos).equals("NOTEXIST"))
	 d5v.value = Double.parseDouble(svrValueList.elementAt(4+pos));
	else
	 d5v.value = 0;
	if (!svrValueList.elementAt(5+pos).equals("NOTEXIST"))
	 d6v.value = Double.parseDouble(svrValueList.elementAt(5+pos));
	else
	 d6v.value = 0;
	if (!svrValueList.elementAt(6+pos).equals("NOTEXIST"))
	 d1f.value = Double.parseDouble(svrValueList.elementAt(6+pos));
	else
	 d1f.value = 255;
	if (!svrValueList.elementAt(7+pos).equals("NOTEXIST"))
	 d2f.value = Double.parseDouble(svrValueList.elementAt(7+pos));
	else
	 d2f.value = 255;
	if (!svrValueList.elementAt(8+pos).equals("NOTEXIST"))
	 d3f.value = Double.parseDouble(svrValueList.elementAt(8+pos));
	else
	 d3f.value = 255;
	if (!svrValueList.elementAt(9+pos).equals("NOTEXIST"))
	 d4f.value = Double.parseDouble(svrValueList.elementAt(9+pos));
	else
	 d4f.value = 255;
	if (!svrValueList.elementAt(10+pos).equals("NOTEXIST"))
	 d5f.value = Double.parseDouble(svrValueList.elementAt(10+pos));
	else
	 d5f.value = 255;
	if (!svrValueList.elementAt(11+pos).equals("NOTEXIST"))
	 d6f.value = Double.parseDouble(svrValueList.elementAt(11+pos));
	else
	 d6f.value = 255;
	if (!svrValueList.elementAt(12+pos).equals("NOTEXIST"))
	 ds.value = Double.parseDouble(svrValueList.elementAt(12+pos));
	else
	 ds.value = 0;
	if (!svrValueList.elementAt(13+pos).equals("NOTEXIST"))
	 dh.value = Double.parseDouble(svrValueList.elementAt(13+pos));
	else
	 dh.value = 0;
	if (!svrValueList.elementAt(14+pos).equals("NOTEXIST"))
	 dc.value = Double.parseDouble(svrValueList.elementAt(14+pos));
	else
	 dc.value = 0;
	if (!svrValueList.elementAt(15+pos).equals("NOTEXIST"))
	 df.value = Double.parseDouble(svrValueList.elementAt(15+pos));
	else
	 df.value = 0;
	if (!svrValueList.elementAt(16+pos).equals("NOTEXIST"))
	 dist.value = Double.parseDouble(svrValueList.elementAt(16+pos));
	else
	 dist.value = 255;
	if (!svrValueList.elementAt(17+pos).equals("NOTEXIST"))
	 dst.value = Double.parseDouble(svrValueList.elementAt(17+pos));
	else
	 dst.value = 255;
	if (!svrValueList.elementAt(18+pos).equals("NOTEXIST"))
	 dcomst.value = Double.parseDouble(svrValueList.elementAt(18+pos));
	else
	 dcomst.value = 255;
	 
	 pos+=19; // jump to next data set
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
