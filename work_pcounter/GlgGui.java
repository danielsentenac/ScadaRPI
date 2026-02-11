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
    private Operation op;
    private Thread thread;
    private static final Logger logger = Logger.getLogger("Main");
    private Container content;
    private GlgJLWBean glg_bean;
    public Hashtable<String, GlgChildGui> subwindows;
    public boolean isSuspended = false;

    public GlgGui (Operation _op, String _title) {

       op = _op;
       title = _title;
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
          glg_bean.SetSTag("pcounter1", deviceManager.getDevice("PC1").aliasName, true);
          glg_bean.SetSTag("pcounter2", deviceManager.getDevice("PC2").aliasName, true);
          glg_bean.SetSTag("pcounter3", deviceManager.getDevice("PC3").aliasName, true);
          glg_bean.SetSTag("pcounter4", deviceManager.getDevice("PC4").aliasName, true);
          glg_bean.SetSTag("pcounter5", deviceManager.getDevice("PC5").aliasName, true);          
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

       for (Map.Entry<String, String> e : PCOUNTERSTATUS.entrySet()) {
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
               logger.finer("GlgGui:updateGui> Treating  dataName= " + dataName + " glgName=" + glgName + " device name=" + 
                                                                      deviceName + " dataElement.value=" + dataElement.value);
               String glgNameTmp = glgName.replace("Str", "");
               glgNameTmp = glgNameTmp.replace(deviceName,""); // Remove device name in glgNameTmp String
               if ( !glgName.contains("sub") && // sub is reserved for childGui only
                    !glgName.contains("Col") && // Col is a tag for color property
                    !glgName.contains("FLG") &&
                    !glgName.contains("Str") )  // Str is a tag for color property
                  glg_bean.SetDResource(glgName +"/Visibility", 1. );
               if (!glgName.contains("sub")) {
                  if ( glgName.contains("Status") && !glgName.contains("Instr") && glgName.contains("Col") )// Col is a tag for object status (short type) color property
                     glg_bean.SetGTag(glgName, PCounterColorSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Status") && !glgName.contains("Instr") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean.SetSTag(glgName, "Counting: " + PCounterSTATUS.get((int)dataElement.value), true);     
                  if ( glgName.contains("InstrStatus") && glgName.contains("Col") )// Col is a tag for object status (short type) color property
                     glg_bean.SetGTag(glgName, PCounterColorINSTRSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("InstrStatus") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean.SetSTag(glgName, "Status: " + PCounterINSTRSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Sampling") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean.SetSTag(glgName, glgNameTmp + " : " + (int) dataElement.value + " (s)", true);
                  if ( glgName.contains("Holding") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean.SetSTag(glgName, glgNameTmp + " : " + (int) dataElement.value + " (s)", true);
                  if ( glgName.contains("Cycle") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean.SetSTag(glgName, glgNameTmp + " : " + (int) dataElement.value, true);
                  if ( glgName.contains("Flow") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean.SetSTag(glgName, glgNameTmp + " : " + (int) dataElement.value + " (mLPM)", true);
               }
               if ( glgName.contains("Val") && // Val is a tag for object value (double type) property
                   !glgName.contains("Time")) {  // Time is reserved for Operation
                  glg_bean.SetDTag(glgName, dataElement.value, true);
                  logger.finer("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
                    dataElement.name + " type=" + dataElement.type + " value=" + 
                    dataElement.value);
               }
               if ( glgName.equals("TimeVal")) {  // TimeVal is reserved for Operation
                  glg_bean.SetDTag(glgName, op.getTime(), true);
               }
               if ( glgName.equals("TimeInterVal")) {  // TimeInterVal is reserved for Operation
                  glg_bean.SetDTag(glgName, op.getTimeInter(), true);
               }
               if ( glgName.equals("AcqTimeVal")) {  // AcqTimeVal is reserved for Operation
                  glg_bean.SetDTag(glgName, op.getAcqTime(), true);
               }
            }
            catch (Exception ex) {
               if ( !glgName.contains("sub") && // sub is reserved for childGui only
                    !glgName.contains("Col") && // Col is a tag for color property
                    !glgName.contains("FLG"))  
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
           
           if ((action.equals("MouseClick") || action.equals("Activate") ) && origin.equals("EXIT")) { // Cicked on EXIT button
              logger.finer("GlgGui:InputListener: Exiting program...");
              GlgGui:exitProgram();
              System.exit(0);
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.contains("PCounter")) {                     
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
                       if ( origin.equals("PCounterStatusDetails") )
                          child = new GlgPCounterStatusDetailsGui(parent, op, title, origin);
                       else if ( origin.equals("PCounterTemp") )
                          child = new GlgPCounterTempGui(parent, op, title, origin);
                       else if ( origin.equals("PCounter2") )
                          child = new GlgPCounter2Gui(parent, op, "CENTRAL BUILDING", origin);
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
           // START command
           else if (action.equals("MouseClick")  && origin.contains("START") ) {
              String cmdName =  PCOUNTERCMD.get(origin);
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);  
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set START internal data command trigger
              }
           }
           // STOP command
           else if (action.equals("MouseClick")  && origin.contains("STOP")) {
              String cmdName =  PCOUNTERCMD.get(origin);
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);  
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set STOP internal data command trigger
              }
           }
           // Sampling setpoint
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.contains("Sampling") ) { // SetPoint fields
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origin );
              String cmdName =  PCOUNTERSTATUS.get(origin + "Str");
              System.out.println("GlgGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origin.contains("Sampling") )
                    new DialogSetPoint(parent, origin, device, dataElement, "Sampling time (0 -> 65535 s)", true);
              }
           }
           // Holding setpoint
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.contains("Holding") ) { // SetPoint fields
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origin );
              String cmdName =  PCOUNTERSTATUS.get(origin + "Str");
              System.out.println("GlgGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origin.contains("Holding") )
                    new DialogSetPoint(parent, origin, device, dataElement, "Holding time (0 -> 65535 s)", true);
              }
           }
           // Cycle setpoint
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origin.contains("Cycle") ) { // SetPoint fields
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origin );
              String cmdName =  PCOUNTERSTATUS.get(origin + "Str");
              System.out.println("GlgGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origin.contains("Cycle") )
                    new DialogSetPoint(parent, origin, device, dataElement, "Cycle number (0 = continuous)", true);
              }
           }           
        }
    }
}
