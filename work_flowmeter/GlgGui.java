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
    private Operation op;
    public GlgGui parent;
    private String title;
    private Thread thread;
    private static final Logger logger = Logger.getLogger("Main");
    private Container content;
    private GlgJLWBean glg_bean;
    public Hashtable<String, GlgChildGui> subwindows;
    public boolean isSuspended = false;

    public GlgGui (Operation _op, String _title) {

       title = _title;
       op = _op;
       deviceManager = op.deviceManager;
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
        
       for (Map.Entry<String, String> e : FLOWMETERSTATUS.entrySet()) {
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
               logger.log(Level.WARNING,"GlgGui:updateGui> dataName= " + dataName + " glgName=" + glgName + " device name=" + deviceName + " does not EXIST!");
               continue;
            }
            logger.finer("GlgGui:updateGui> dataElement name= " + dataElement.name + " glgName=" + glgName + " device name=" + deviceName );
            try {
               if ( !glgName.contains("sub") && // sub is reserved for childGui only
                    !glgName.contains("Col") &&
                    !glgName.contains("Str"))  {// Col is a tag for color property
                  glg_bean.SetDResource(glgName +"/Visibility", 1. );
                  logger.finer("GlgGui:updateGui> UPDATE VISIBILITY=1 for dataElement name= " + dataElement.name + 
                              " glgName=" + glgName + " device name=" + deviceName );
               }
               if (!glgName.contains("sub") &&  
                    glgName.contains("Col") ) {
                  logger.finer("GlgGui:updateGui> glgName=" + glgName + " dataElement name=" + 
                  dataElement.name + " type=" + dataElement.type + " value=" +  dataElement.value);
                  // Col is a tag for object status (short type) color property
                  
                  if (glgName.startsWith("XGS"))
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
               	// Gauge Gui case
               	if ( glgName.contains("Col") &&  // Col is a tag for object status (short type) color property
             	     !glgName.contains("P") ) {  // P refers to pressure status
            		glg_bean.SetGTag(glgName, GaugeColorSTATUS.get((int)dataElement.value), true);
            		//logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
        	}
        	if ( glgName.contains("Str") &&  // Str is a tag for object status (short type) string property
             	     !glgName.contains("P") ) {  // P refers to pressure status
            		glg_bean.SetSTag(glgName,  GaugeSTATUS.get((int)dataElement.value), true);
            		//logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);
        	}
        	if ( glgName.contains("Col") &&  // Col is a tag for object status (short type) color property
             	     glgName.contains("P") ) {  // P refers to pressure status
            		glg_bean.SetGTag(glgName, GaugePressureColorSTATUS.get((int)dataElement.value), true);
            		//logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
        	}
        	if ( glgName.contains("Str") &&  // Str is a tag for object status (short type) string property
               	     glgName.contains("P") ) {  // P refers to pressure status
            		glg_bean.SetSTag(glgName,  GaugePressureSTATUS.get((int)dataElement.value), true);
            		//logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);
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
                    !glgName.contains("Col") &&
                    !glgName.contains("Str"))  // Col is a tag for color property
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
           //System.out.println("GlgGui:InputListener> Origin=" + origin + " Format=" + format + " Action=" + action + " subAction=" + subaction);
          
           if ((action.equals("MouseClick") || action.equals("Activate") )&& origin.equals("EXIT")) { // Cicked on EXIT button
              logger.finer("GlgGui:InputListener: Exiting program...");
              GlgGui:exitProgram();
              System.exit(0);
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.equals("V22")) {
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
                       if ( origin.startsWith("V") )
                          child = new GlgValveGui(parent, deviceManager, title, origin);
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
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.contains("FlowSetP")) {
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origin  );
              String deviceName = FLOWMETERSTATUS.get(origin).split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(FLOWMETERSTATUS.get(origin).split("_",2)[1]);
              if ( dataElement != null ) {
                 logger.fine("Open DialogSetPoint for device = " + device.name + " DataElement = " + dataElement.name);
                 new DialogSetPoint(parent, origin, device, dataElement, "Set Flow (sccm)", true); 
              }
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.contains("Ramp")) {
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origin  );
              String deviceName = FLOWMETERSTATUS.get(origin).split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(FLOWMETERSTATUS.get(origin).split("_")[1]);
              if ( dataElement != null ) {
                 new DialogSetPoint(parent, origin, device, dataElement, "Set Ramp (msec) ", true); 
              }
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.contains("ValveOpen")) {
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origin  );
              String deviceName = FLOWMETERSTATUS.get(origin).split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(FLOWMETERSTATUS.get(origin).split("_",2)[1]);
              if ( dataElement != null ) {
                 new DialogSetPoint(parent, origin, device, dataElement, "Set Valve Open (0 or 1) ", true); 
              }
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.contains("ValveClose")) {
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origin  );
              String deviceName = FLOWMETERSTATUS.get(origin).split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(FLOWMETERSTATUS.get(origin).split("_",2)[1]);
              if ( dataElement != null ) {
                 new DialogSetPoint(parent, origin, device, dataElement, "Set Valve Close (0 or 1) ", true); 
              }
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.contains("FlowZero")) {
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origin  );
              String deviceName = FLOWMETERSTATUS.get(origin).split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(FLOWMETERSTATUS.get(origin).split("_",2)[1]);
              if ( dataElement != null ) {
                 new DialogSetPoint(parent, origin, device, dataElement, "Set Flow Zero (0 or 1) ", true); 
              }
           }
           else if (action.equals("MouseClick") && (origin.contains("FlowMax") || origin.contains("FlowStep") || origin.contains("TimeInter" )) ) { // Operation parameter fields
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origin );
              String cmdName =  FLOWMETERSTATUS.get(origin);
              System.out.println("GlgGui::InputListener> Open Dialog command for " + cmdName );
              new DialogOp(parent, origin.replace("Val",""), op, "Set " + origin.replace("Val",""), true);
           }
           else if (action.equals("MouseClick") && (origin.contains("START") || origin.contains("STOP"))  ) { // Operation Ramp START/STOP commands 
              String cmdName =  FLOWMETERCMD.get(origin);
              String deviceName = cmdName.split("_",2)[0];
              logger.fine("GlgGui::InputListener> Operation Start/Stop command for " + origin + " cmdName = " + cmdName + " deviceName = " + deviceName);
              if ( op != null) {
                 if ( origin.contains("START"))
                     op.setMode(OpMode.START);
                 else if ( origin.contains("STOP"))
                     op.setMode(OpMode.STOP);
              }
           }
        }
    }
}
