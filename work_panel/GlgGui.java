/*
 * This Class is used for GUI using Glg Toolkit
 */
import com.genlogic.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.event.*;
import java.awt.Frame;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.GridLayout;

import java.lang.Runtime;

public class GlgGui extends JFrame implements ChannelList, Runnable  {

    private static final long serialVersionUID = 354054054055L;
    private DeviceManager deviceManager;
    public GlgGui parent;
    private String title;
    private Thread thread;
    private static final Logger logger = Logger.getLogger("Main");
    private Container content;
    private GlgJLWBean glg_bean;
    public Hashtable<String, GlgChildGui> subwindows;
    public boolean isSuspended = false;

    public GlgGui (DeviceManager _deviceManager, String _title) {

       title = _title;
       deviceManager = _deviceManager;
       parent = this;
       // Creating the display
       createAndShowGui();
       // Start thread
       this.doStart();
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

    private void createAndShowGui () {
       
       try {
          logger.finer("GlgGui:createAndShowGui> Start Main Gui");
          subwindows = new Hashtable < String, GlgChildGui > ();
          //
          // Add (Anonymous) Window Closing Listener
	  //
          this.addWindowListener(new WindowAdapter() { 
             public void windowClosing(WindowEvent e) {
                GlgGui:exitProgram();
             }
          });
          content = this.getContentPane();
          content.setLayout(new GridLayout(1,1));
          this.setExtendedState(JFrame.MAXIMIZED_BOTH);
          this.setUndecorated(true);
          this.setVisible(true);
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile(mainDrawing);
          // Add glg_bean component to a frame
	  content.add( glg_bean );
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("title", title, true);
         
       }
       catch (Exception ex) {
          logger.log(Level.SEVERE, "GlgGui:createAndShowGui> " + ex.getMessage());
       }
    }

    public void exitProgram() {

       /*logger.finer("GlgGui:createAndShowGui: Stopping devices threads...");
       for (Map.Entry<String, Device> d : deviceManager.deviceList.entrySet())
          (d.getValue()).doStop();
       logger.finer("GlgGui:createAndShowGui: Stopping child gui threads...");
       for (String key: subwindows.keySet())
          subwindows.get(key).doStop();
       logger.finer("GlgGui:createAndShowGui: Stopping main gui thread...");
       this.doStop();*/
       System.exit(0);
    }
    public void updateGui() {

       for (Map.Entry<String, String> e : PANELSTATUS.entrySet()) {
         String glgName =  e.getKey();
         String dataName = e.getValue();
         if ( dataName == null ) continue;
         // Get corresponding device from deviceManager
         String deviceName = dataName.split("_",2)[0];
         Device device = deviceManager.getDevice(deviceName);
         // Get corresponding data element from device
         if ( device != null ) {
            DataElement dataElement = device.getDataElement(dataName.split("_",2)[1]);
            if (dataElement == null) {
               logger.finer("GlgGui:updateGui> dataName= " + dataName + " glgName=" + glgName + " device name=" + deviceName + " does not EXIST!");
               continue;
            }
            try {
               logger.finer("GlgGui:updateGui> Treating  dataName= " + dataName + " glgName=" + glgName + " device name=" + deviceName + " dataElement.value=" + dataElement.value);
               String glgNameTmp = glgName.replace("Str", "");
               glgNameTmp = glgNameTmp.replace(deviceName,""); // Remove device name in glgNameTmp String
               if (!glgName.contains("sub")) {
                  if ( glgName.contains("Running")  )// Instrument Running flag
                     glg_bean.SetGTag(glgName, PCounterColorSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Error") )// Instrument Error flag
                     glg_bean.SetGTag(glgName, PCounterColorINSTRSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Com") )  // Instrument Com flag
                     glg_bean.SetGTag(glgName, PCounterColorCOMSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Status") )  // Instrument Channel flag
                     glg_bean.SetGTag(glgName, PCounterColorFLAGSTATUS.get((int)dataElement.value), true);
               }
               if ( glgName.contains("Val") && // Val is a tag for object value (double type) property
                   !glgName.contains("sub")) {  // sub is reserved for childGui only
                  glg_bean.SetDTag(glgName, dataElement.value, true);
                  logger.finer("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
                    dataElement.name + " type=" + dataElement.type + " value=" + 
                    dataElement.value);
               }
            }
            catch (Exception ex) {
               ex.printStackTrace();
            }
          }
       } 
    }

    public void run () {
       
       try {  
          while (true) {
	     // Get rid of GlgLogic popup window
	     Frame[] frames = Frame.getFrames();
             for (Frame frame : frames) {
                if (frame.getClass().getCanonicalName().contains("JFrame")) { 
                   frame.setVisible(false); 
                   frame.dispose();
                }
             }
             if (isSuspended == false) {
                // Update Glg Gui tags
                updateGui();	
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                   public void run() {
                   // Update Bean
                   glg_bean.validate();
                   glg_bean.repaint();
                   }
                });
             }
             Thread.sleep(1000);
	  }
       }
       catch (InterruptedException ex) {
          logger.log(Level.SEVERE, "GlgGui:run:InterruptedException> " + ex.getMessage());
       }
       catch (NullPointerException ex) {
          logger.log(Level.SEVERE, "GlgGui:run:NullPointerException> " + ex.getMessage());
       }
       catch (Exception ex) {
          logger.log(Level.SEVERE, "GlgGui:run:Exception> " + ex.getMessage());
       }    
       logger.finer("GlgGui:run> Exiting Main Gui");
    }

    class InputListener implements GlgInputListener { 

	public void InputCallback( GlgObject viewport, GlgObject message_obj )  {

           String origin, format, action, subaction;
           origin = message_obj.GetSResource( "Origin" );
	   format = message_obj.GetSResource( "Format" );
	   action = message_obj.GetSResource( "Action" );
	   subaction = message_obj.GetSResource( "SubAction" );
           //System.out.println("GlgGui:InputListener> Origin=" + origin + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           
           if ((action.equals("MouseClick") || action.equals("Activate") ) && origin.equals("EXIT")) { // Cicked on EXIT button
              logger.finer("GlgGui:InputListener: Exiting program...");
              GlgGui:exitProgram();
              System.exit(0);
           }
           else if ((action.equals("MouseClick") ) && origin.equals("SETTINGS")) { // Cicked on SETTINGS button
              logger.finer("GlgGui:InputListener: Open Settings VNC viewer program...");
              try {
                 String[] cmd = {"vncviewer", "partcount-ceb"};
                 Runtime.getRuntime().exec(cmd);
              }
              catch (Exception e) { 
                 e.printStackTrace(); 
              } 
           }
           else if ((action.equals("MouseClick") ) && origin.equals("MOBILE")) { // Cicked on MOBILE button
              logger.finer("GlgGui:InputListener: Open Settings MOBILE page...");
              GlgChildGui child = null;
              child = subwindows.get(origin);
              if ( child == null ) {
                  child = new Glg2Gui(parent, deviceManager, "MOBILE PARTICLE COUNTERS", origin);
                  subwindows.put(origin,child);
              }
              //logger.finer("GlgGui:InputListener> child subwindow=" + child.origin);
              child.setExtendedState(JFrame.MAXIMIZED_BOTH);
              child.toFront();
              child.isSuspended = false;
              parent.isSuspended = true;
           }
        }
    }
}
