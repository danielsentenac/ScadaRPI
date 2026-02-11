import java.awt.IllegalComponentStateException;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Color;
import java.awt.Point;
import java.awt.Dimension;
import javax.swing.JTextField;       // for GUI management
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JFrame;
import java.util.Vector;
import java.util.Enumeration;
import java.util.UUID;
import javax.swing.BorderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;
import java.lang.Exception;

import java.net.URLConnection; //for url management 
import java.net.URL;
import java.io.ObjectInputStream; //for stream management
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ConnectException;


public class DialogStart extends JDialog implements Runnable {
    private static final Logger logger = Logger.getLogger("Main");
    private JPanel panel; 
    private JLabel jLabelText = new JLabel();
    private JFrame parent;
    private String title;
    private Thread thread;
   
    public DialogStart(JFrame _parent, String _title, boolean _modal) {
	try {
            parent = _parent;
            title = _title;
	    this.setModal(_modal);
	    // Start thread
            this.doStart();
	    Init();
	    
	}
	catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
    
     public void doStart() {
      thread = new Thread(this);
      thread.start();
    }
    
    public void doStop() {
        if (thread != null) thread.interrupt();
        // Change the states of variable
        thread = null;
   }
   
    void Init() throws Exception {
        this.setResizable(false);
	panel  = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(Integer.parseInt("8BC5C5", 16)));
	jLabelText.setFont(new java.awt.Font("Dialog", 1, 22));
	jLabelText.setForeground(Color.red);
        jLabelText.setText(title);
      
        this.getRootPane().setBorder(BorderFactory.createLoweredBevelBorder());
        this.setUndecorated(true);
	
	this.getContentPane().add(panel);
	panel.add(jLabelText, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
						     , GridBagConstraints.CENTER, GridBagConstraints.NONE,
						     new Insets(100, 100, 100, 100), 0, 0));
        Dimension dlgSize = this.getPreferredSize();
        Dimension frmSize = parent.getContentPane().getSize();
        Point loc = null;
        while ( loc == null ) loc = GetLocationOnScreen();
        this.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                          (frmSize.height - dlgSize.height) / 2 + loc.y);
	this.pack();
	this.setVisible(true);

    }
    
    public void run () {
       
       try {  
            StartScan();
            dispose();
       }
       catch (Exception ex) {
          logger.log(Level.SEVERE, "DialogStop:run:Exception> " + ex.getMessage());
       }    
    }
    
     private void StartScan() {
        // Launch sequence commands to RGA instrument
        // Take control
        SendCommandToSupervisor("Control Mks v0r0");
        // Configure Scan
        SendCommandToSupervisor("AddBarChart Bar1 1 200 PeakCenter 8 0 0 3");
        // Add Scan
        SendCommandToSupervisor("ScanAdd Bar1");
        // Lightup filament
        SendCommandToSupervisor("FilamentControl On");
        // Start Scan
        SendCommandToSupervisor("ScanStart 32000");
	
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
    
   
    protected void processWindowEvent(WindowEvent e) {
	super.processWindowEvent(e);
	if (e.getID() == WindowEvent.WINDOW_CLOSING) {
	    dispose();
	}
    }
    Point GetLocationOnScreen() {
	Point loc = null;
	try {
	    loc = parent.getContentPane().getLocationOnScreen();
	}
	catch (IllegalComponentStateException il){}
	return (loc);
    }
    private void SendCommandToSupervisor(String command) {
        
            Vector<String> commandVect = new Vector<String>();
            String server = "Mks1500W";
            String device = "VAC_TUBE1500W_RGAGa3";
            String type = "COMMAND";
            
            // Setup command to be sent
            commandVect.addElement(server);
            commandVect.addElement(type);
            commandVect.addElement("-t");
            commandVect.addElement(device);
            commandVect.addElement("-t");
            commandVect.addElement(command);
            commandVect.addElement("-t");
            commandVect.addElement("RPLY");
               
            try {
              // Send command to (tomcat) supervisor server
              URLConnection con = getServerConnection("http://olserver135.virgo.infn.it:9081/jcmd/jcmd");
              OutputStream outstream = con.getOutputStream();
              ObjectOutputStream oos = new ObjectOutputStream(outstream);
              oos.writeObject(commandVect);
              oos.flush();
              oos.close();

              logger.fine("SupervisorClient:updateDeviceData> send supervisor: " + commandVect);

              // receive result from servlet
              InputStream instr = con.getInputStream();
              ObjectInputStream inputFromServlet = new ObjectInputStream(instr);
              String status = (String) inputFromServlet.readObject();
              inputFromServlet.close();
              instr.close();
        
              logger.fine("SupervisorClient:updateDeviceData> receive from supervisor: " + status);
              //
              // Mark a pause
              //
              Thread.sleep(1000);
            }
            catch (Exception ex) {
               ex.printStackTrace();
            }
        }
    
}
