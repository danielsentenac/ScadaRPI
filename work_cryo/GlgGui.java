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
        
       for (Map.Entry<String, String> e : CRYOIBDETSTATUS.entrySet()) {
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
               logger.fine("GlgGui:updateGui> dataName= " + dataName + " glgName=" + glgName + " device name=" + deviceName + " does not EXIST!");
               continue;
            }
            try {
               if ( !glgName.contains("sub") && // sub is reserved for childGui only
                    !glgName.contains("Col") )  // Col is a tag for color property
                  glg_bean.SetDResource(glgName +"/Visibility", 1. );
               if (!glgName.contains("sub") &&  
                    glgName.contains("Col") ) {
                  logger.finer("GlgGui:updateGui> glgName=" + glgName + " dataElement name=" + 
                  dataElement.name + " type=" + dataElement.type + " value=" + 
                  dataElement.value);
                  // Col is a tag for object status (short type) color property
                  if (glgName.startsWith("G")) {
                    logger.finer("GlgGui:updateGui> UPDATE Col for glgName=" + glgName + " dataElement name=" + 
                    dataElement.name + " type=" + dataElement.type + " value=" + dataElement.value);
                    glg_bean.SetGTag(glgName, GaugeColorSTATUS.get((int)dataElement.value), true);
                  }
                  else if (glgName.startsWith("XGS"))
                    glg_bean.SetGTag(glgName, XGSGaugeEmissionColorSTATUS.get((int)dataElement.value), true);
                  else if (glgName.startsWith("V"))
                    glg_bean.SetGTag(glgName, ValveColorSTATUS.get((int)dataElement.value), true);
                  else if (glgName.startsWith("Turbo"))
                    glg_bean.SetGTag(glgName, TurboONOFFColorSTATUS.get((int)dataElement.value), true);
                  else if (glgName.startsWith("Ionic"))
                    glg_bean.SetGTag(glgName, IonicONOFFColorSTATUS.get((int)dataElement.value), true);
                  else if (glgName.startsWith("Dry")) 
                    glg_bean.SetGTag(glgName, RelayGeneralColorSTATUS.get((int)dataElement.value), true);
                  else if (glgName.startsWith("Titane")) 
                    glg_bean.SetGTag(glgName, RelayGeneralColorSTATUS.get((int)dataElement.value), true);
                  else if (glgName.startsWith("RGA"))
                    glg_bean.SetGTag(glgName, RGAONOFFColorSTATUS.get((int)dataElement.value), true);                  
            
                  logger.finer("GlgGui:updateGui> UPDATE Color of " + dataElement.name + " = "  + dataElement.value);
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
               if ( !glgName.contains("sub") && // sub is reserved for childGui only
                    !glgName.contains("Col") )  // Col is a tag for color property
                  glg_bean.SetDResource(glgName +"/Visibility", 0.1 );
               logger.log(Level.SEVERE,"GlgGui:updateGui> UPDATE " + dataElement);
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
           //logger.finer("GlgGui:InputListener> Origin=" + origin + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           if ((action.equals("MouseClick") || action.equals("Activate") )&& origin.equals("EXIT")) { // Cicked on EXIT button
              logger.finer("GlgGui:InputListener: Exiting program...");
              GlgGui:exitProgram();
              System.exit(0);
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && !origin.equals("")) {
              double visibility =  glg_bean.GetDResource( origin +"/Visibility" );
              //logger.finer("GlgGui:InputListener> Origin=" + origin + " Format=" + format + " Action=" + action +
              //                " subAction=" + subaction + " visibility=" + visibility);
              if ( visibility > 0.8)  {
                 boolean showWindow = true;
                 GlgChildGui child = null;
                 if (showWindow == true) {
                    child = subwindows.get(origin);
                    if ( child == null ) {
                       //logger.finer("GlgGui:InputListener> Create window : " + origin);
                       if ( origin.equals("CryoIbDetStatusDetails") )
                          child = new GlgCryoIbDetStatusDetailsGui(parent, deviceManager, title, origin);
                       else if ( origin.startsWith("G") && !origin.contains("Val") )
                          child = new GlgGaugeGui(parent, deviceManager, title, origin);
                       else if ( origin.startsWith("XGS") && !origin.contains("Val") )
                          child = new GlgXGSGaugeGui(parent, deviceManager, title, origin);
                       else if ( origin.startsWith("V") )
                          child = new GlgValveGui(parent, deviceManager, title, origin);
                       else if ( origin.startsWith("Turbo") )
                          child = new GlgTurboGui(parent, deviceManager, title, origin);
                       else if ( origin.startsWith("Ionic") )
                          child = new GlgIonicGui(parent, deviceManager, title, origin);
                       else if ( origin.startsWith("TitaneP31") )
                          child = new GlgTitane_22Gui(parent, deviceManager, title, origin);
                       else if ( origin.startsWith("TitaneP32") )
                          child = new GlgTitane_32Gui(parent, deviceManager, title, origin);
                       else if ( origin.startsWith("Dry") )
                          child = new GlgDryGui(parent, deviceManager, title, origin);
                       else if ( origin.startsWith("RGA") )
                          child = new GlgRGAGui(parent, deviceManager, title, origin);
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
    }
}
