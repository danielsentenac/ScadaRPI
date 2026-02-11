/*
 * This Class is used for PCounter2 GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgPCounter2Gui extends GlgChildGui {

    private static final long serialVersionUID = 354054054072L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgPCounter2Gui (GlgGui _parent, Operation _op, String _title, String _origin) {
       super(_parent, _op, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          // PCounter2 display
          glg_bean.SetDrawingFile("PCOUNTER2.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("title", title + " NEXT", true);
          glg_bean.SetSTag("pcounter6", deviceManager.getDevice("PC6").aliasName, true);
          glg_bean.SetSTag("pcounter7", deviceManager.getDevice("PC7").aliasName, true);
          glg_bean.SetSTag("pcounter8", deviceManager.getDevice("PC8").aliasName, true);
          // Create STATUS map origin dependent feature
          createStatusMap(STATUS);
       
          // Create CMD map origin dependent feature
          createCommandMap(CMD);
       } 
      catch (Exception e) {
          logger.log(Level.SEVERE, e.getMessage());
      }
    }

    public void createCommandMap(Hashtable < String, String > CMD) {

       CMD.clear();
       CMD.putAll(PCOUNTER2CMD);
    }

    public void createStatusMap(Hashtable < String, String > STATUS) {

       // PCounter2 Gui case
       STATUS.clear();
       STATUS.putAll(PCOUNTER2STATUS);
    }

    protected void updateGuiFeatures(String glgName, DataElement dataElement) {
       //logger.info("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
       //   dataElement.name + " type=" + dataElement.type + " value=" + dataElement.value);
       // PCounter2 Gui case
       String glgNameTmp = glgName.replace("Str", "");
       if ( glgName.contains("Val") && // Val is a tag for object value (double type) property
            !glgName.contains("sub")) {  // sub is reserved for ChildGui items
             glg_bean.SetDTag(glgName, dataElement.value, true);
             logger.finer("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
             dataElement.name + " type=" + dataElement.type + " value=" + 
             dataElement.value);
       }
       
       else if ( glgName.contains("Status") && !glgName.contains("Instr") && glgName.contains("Col") )// Col is a tag for object status (short type) color property
          glg_bean.SetGTag(glgName, PCounterColorSTATUS.get((int)dataElement.value), true);
       else if ( glgName.contains("Status") && !glgName.contains("Instr") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
          glg_bean.SetSTag(glgName, "Counting: " + PCounterSTATUS.get((int)dataElement.value), true);     
       else if ( glgName.contains("InstrStatus") && glgName.contains("Col") )// Col is a tag for object status (short type) color property
          glg_bean.SetGTag(glgName, PCounterColorINSTRSTATUS.get((int)dataElement.value), true);
       else if ( glgName.contains("InstrStatus") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
          glg_bean.SetSTag(glgName, "Status: " + PCounterINSTRSTATUS.get((int)dataElement.value), true);
       else if ( glgName.contains("Sampling") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
          glg_bean.SetSTag(glgName, "Sampling : " + (int) dataElement.value + " (s)", true);
       else if ( glgName.contains("Holding") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
          glg_bean.SetSTag(glgName, "Holding : " + (int) dataElement.value + " (s)", true);
       else if ( glgName.contains("Cycle") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
          glg_bean.SetSTag(glgName, "Cycle : " + (int) dataElement.value, true);
       else if ( glgName.contains("Flow") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
          glg_bean.SetSTag(glgName, "Flow : " + (int) dataElement.value + " (mLPM)", true);
       else { // All other cases
          if ( glgName.contains("Col") )// Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, OkFailColorSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
          if ( glgName.contains("Str") )  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName, glgName.replace("Str", "") +" "+ OkFailSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);       
       }
    }

    public class InputListener implements GlgInputListener { 

	public void InputCallback( GlgObject viewport, GlgObject message_obj )  {

           String origintag, format, action, subaction;
           origintag = message_obj.GetSResource( "Origin" );
	   format = message_obj.GetSResource( "Format" );
	   action = message_obj.GetSResource( "Action" );
	   subaction = message_obj.GetSResource( "SubAction" );
           System.out.println("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           
           // Clicked on MAIN button
           if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("GLOBAL")) {
                parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                parent.toFront();
                parent.isSuspended = false;
                child.isSuspended = true;
           }
           // START command
           else if (action.equals("MouseClick")  && origintag.contains("START") && origintag.contains("PC") ) {
              String cmdName =  PCOUNTER2CMD.get(origintag);
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
           else if (action.equals("MouseClick")  && origintag.contains("STOP") && origintag.contains("PC") ) {
              String cmdName =  PCOUNTER2CMD.get(origintag);
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
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.contains("Sampling") ) { // SetPoint fields
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origintag );
              String cmdName =  PCOUNTER2STATUS.get(origintag + "Str");
              System.out.println("GlgGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("Sampling") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Sampling time (0 -> 65535 s)", true);
              }
           }
           // Holding setpoint
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.contains("Holding") ) { // SetPoint fields
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origintag );
              String cmdName =  PCOUNTER2STATUS.get(origintag + "Str");
              System.out.println("GlgGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("Holding") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Holding time (0 -> 65535 s)", true);
              }
           }
           // Cycle setpoint
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.contains("Cycle") ) { // SetPoint fields
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origintag );
              String cmdName =  PCOUNTER2STATUS.get(origintag + "Str");
              System.out.println("GlgGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("Cycle") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Cycle number (0 = continuous)", true);
              }
           }
           // Programmation Operation Parameters
           else if (action.equals("MouseClick") && (origintag.contains("AcqTime") || origintag.contains("TimeInter" )) ) { // Operation parameter fields
              logger.finer("GlgGui::InputListener> Open Dialog command for " + origintag );
              new DialogOp(parent, origintag.replace("Val",""), op, "Set " + origintag.replace("Val",""), true);
           }
           else if (action.equals("MouseClick") && (origintag.contains("START_RAMP") || origintag.contains("STOP_RAMP"))  ) { // Operation Ramp START/STOP commands
              logger.finer("GlgGui::InputListener> Open Dialog command for 2 " + origintag );
              if ( op != null) {
                 if ( origintag.contains("START"))
                     op.setMode(OpMode.START);
                 else if ( origintag.contains("STOP"))
                     op.setMode(OpMode.STOP);
              }
           }
        }
    }
}
