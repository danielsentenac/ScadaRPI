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

public class GlgGui extends JFrame implements ChannelList, DataTypes, Runnable  {

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

          // Initialize content
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
       // Treat GUI relevant devices data
       for (Map.Entry<String, String> e : STATIONSTATUS.entrySet()) {
          String glgName =  e.getKey();
          String dataName = e.getValue();
          if ( dataName == null ) continue;
          // Get corresponding device from deviceManager
          String deviceName = dataName.split("_")[0];
          Device device = deviceManager.getDevice(deviceName);
          // Get corresponding data element from device
          if ( device != null ) {
             DataElement dataElement = device.getDataElement(dataName.split("_")[1]);
             if (dataElement == null) {
                logger.log(Level.SEVERE,"GlgGui:updateGui> dataName= " + dataName + " glgName=" + glgName + " device name=" + deviceName);
                continue;
             }
             try {
                if ( !glgName.contains("sub") && // sub is reserved for childGui only
                     !glgName.contains("Col") )  // Col is a tag for color property
                   glg_bean.SetDResource(glgName +"/Visibility", 1. );
                if (!glgName.contains("sub") &&  
                     glgName.contains("Col") ) {
                   //logger.finer("GlgGui:updateGui> glgName=" + glgName + " dataElement name=" + 
                   //dataElement.name + " type=" + dataElement.type);
                   // Col is a tag for object status (short type) color property            
                   //logger.finer("GlgGui:updateGui> UPDATE Color of " + dataElement.name + " = "  + dataElement.value);
                }
                if ( glgName.contains("Val") && // Val is a tag for object value (double type) property
                    !glgName.contains("sub"))   // sub is reserved for childGui only
                   //logger.finer("GlgGui:updateGui> UPDATE " + dataElement.name + " with value " + dataElement.value);
                   glg_bean.SetDTag(glgName, dataElement.value, true);
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
             Thread.sleep(100);
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
           logger.fine("GlgGui:InputListener> Origin=" + origin + " Format=" + format + 
                  " Action=" + action + " subAction=" + subaction);
           
           if ((action.equals("MouseClick") || action.equals("Activate")) && origin.equals("EXIT")) { // Cicked on QUIT button
              logger.finer("GlgGui:InputListener: Exiting program...");
              GlgGui:exitProgram();
              System.exit(0);
           }
           else if (action.equals("MouseClick" ) && origin.contains("Val")) {
              Device device = deviceManager.getDevice("MODULATOR");
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(STATIONSTATUS.get(origin).split("_")[1]);
                 if (origin.contains("Negative") )
                    new DialogSetPoint(parent, origin, device, dataElement, "Negative Volt (0V --> 4000V)",true);
                 else if (origin.contains("Positive") )
                    new DialogSetPoint(parent, origin, device, dataElement, "Positive Volt (0V --> 4000V)",true);
                 else if (origin.contains("Frequency") )
                    new DialogSetPoint(parent, origin, device, dataElement, "Frequency (0Hz --> 5Hz)",true);
                 else if (origin.contains("Dutycycle") )
                    new DialogSetPoint(parent, origin, device, dataElement, "Duty Cycle (0% --> 100%)",true);
              }
           }  
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.contains("OnOff")) {
              
              String cmdName =  STATIONCMD.get("OnOff");
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              logger.fine("InputListener:InputCallback> execute " + origin + " command " + cmdName 
              + " (device " + deviceName + ")");
              if ( device != null ) {
                 // Get On/Off status
                 DataElement OnOffStatus = device.getDataElement("MAINST");
                 if (OnOffStatus.value == 1) {     // Switch Off 
                    // Update dataElement
                    //logger.finer("InputListener:InputCallback> device " + device.name);
                    DataElement dataElement = device.getDataElement(cmdName.split("_")[1]);
                    dataElement.setvalue = 2;
                    device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
                    //logger.finer("InputListener:InputCallback> set data command value " + dataElement.value);
                 }
                 else if (OnOffStatus.value == 2) {     // Switch On
                    // Update dataElement
                    //logger.finer("InputListener:InputCallback> device " + device.name);
                    DataElement dataElement = device.getDataElement(cmdName.split("_")[1]);
                    dataElement.setvalue = 1;
                    device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
                    //logger.finer("InputListener:InputCallback> set data command value " + dataElement.value);
                 }
              }
           }
        }
    }
}
