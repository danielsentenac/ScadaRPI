/*
 * This Class is used for PCounterStatusDetails GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgPCounterStatusDetailsGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054072L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgPCounterStatusDetailsGui (GlgGui _parent, Operation _op, String _title, String _origin) {
       super(_parent, _op, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          // PCounterStatusDetails display
          glg_bean.SetDrawingFile("PCounterStatusDetails.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("title", title + " RACK STATUS DETAILS", true);
          glg_bean.SetSTag("pcounter1", deviceManager.getDevice("PC1").aliasName, true);
          glg_bean.SetSTag("pcounter2", deviceManager.getDevice("PC2").aliasName, true);
          glg_bean.SetSTag("pcounter3", deviceManager.getDevice("PC3").aliasName, true);
          glg_bean.SetSTag("pcounter4", deviceManager.getDevice("PC4").aliasName, true);
          glg_bean.SetSTag("pcounter5", deviceManager.getDevice("PC5").aliasName, true);
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
       CMD.putAll(PCOUNTERCMD);
    }

    public void createStatusMap(Hashtable < String, String > STATUS) {

       // PCounterStatusDetails Gui case
       STATUS.clear();
       STATUS.putAll(PCounterStatusDetails);
    }

    protected void updateGuiFeatures(String glgName, DataElement dataElement) {
       //logger.info("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
       //   dataElement.name + " type=" + dataElement.type + " value=" + dataElement.value);
       // PCounterStatusDetails Gui case
       if ( glgName.contains("Val")) {  // Val is a tag for object value (double type) property
          glg_bean.SetDTag(glgName, dataElement.value, true);
          //logger.info("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
          //dataElement.name + " type=" + dataElement.type + " value=" + dataElement.value);
       }
       else if (glgName.contains("PumpStatus")) {
          if ( glgName.contains("Col") )// Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, PumpColorSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> PumpStatusColorSTATUS=" + dataElement.value);
          if ( glgName.contains("Str") )  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName, glgName.replace("Str", "").replace("Status","") + " "+ PumpSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);       
       }
       else if (glgName.contains("IPSStatus")) {
          if ( glgName.contains("Col") )// Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, IPSColorSTATUS.get((int)dataElement.value), true);
             logger.finer("GlgChildGui:updateGui> IPSColorSTATUS=" + dataElement.value);
          if ( glgName.contains("Str") )  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName, glgName.replace("Str", "").replace("Status","") +" "+ IPSSTATUS.get((int)dataElement.value), true);
             logger.finer("GlgChildGui:updateGui> IPSSTATUS=" + dataElement.value);       
       }
       else if (glgName.contains("RearmStatus")) {
          if ( glgName.contains("Col") )// Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, RearmColorSTATUS.get((int)dataElement.value), true);
             logger.finer("GlgChildGui:updateGui> RearmColorSTATUS=" + dataElement.value);
          if ( glgName.contains("Str") )  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName, glgName.replace("Str", "").replace("Status","") +" "+ RearmSTATUS.get((int)dataElement.value), true);
             logger.finer("GlgChildGui:updateGui> RearmSTATUS=" + dataElement.value);       
       }
       else if (glgName.contains("ACStatus")) {
          if ( glgName.contains("Col") )// Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, ACColorSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> PumpStatusColorSTATUS=" + dataElement.value);
          if ( glgName.contains("Str") )  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName, glgName.replace("Str", "").replace("Status","") + " " + ACSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);       
       }
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
           //System.out.println("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           
           // Clicked on MAIN button
           if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("GLOBAL")) {
                parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                parent.toFront();
                parent.isSuspended = false;
                child.isSuspended = true;
           }
           // AC OFF command
           else if ((action.equals("MouseClick") ) && origintag.contains("ACOFF")) {
              String cmdName =  CMD.get("ACOFF");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);  
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
                 showParent = false;
              }
           }
           // AC LOW command
           else if ((action.equals("MouseClick") ) && origintag.contains("ACLOW")) {
              String cmdName =  CMD.get("ACLOW");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);  
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set LOW noise AC internal data command trigger
                 showParent = false;
              }
           }
           // AC LOW command
           else if ((action.equals("MouseClick") ) && origintag.contains("ACHIGH")) {
              String cmdName =  CMD.get("ACHIGH");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);  
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 3;
                 device.commandSetQueue.add(dataElement); // Set High noise AC internal data command trigger
                 showParent = false;
              }
           }
           // Pump ON command
           else if ((action.equals("MouseClick") ) && origintag.contains("PumpON")) {
              String cmdName =  CMD.get("Pump");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);  
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
                 showParent = false;
              }
           }
           // Pump OFF command
           else if ((action.equals("MouseClick") ) && origintag.contains("PumpOFF")) {
              String cmdName =  CMD.get("Pump");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
                 showParent = false;
              }
           }
           // Threshold setpoint
           else if (action.equals("MouseClick") && origintag.contains("THR") ) { // Threshold fields
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origintag );
              String cmdName =  PCounterStatusDetails.get(origintag);
              System.out.println("GlgGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              showParent = false;
              if (dataElement != null) {
                    new DialogSetPoint(parent, origintag, device, dataElement, "Threshold value", true);
              }
              
           }
           /* RESET command*/
           else if ((action.equals("MouseClick") ) && origintag.contains("Reset3")) {
              String cmdName =  CMD.get("Reset3");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);  
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set NORMAL internal data command trigger
                 showParent = false;
              }
           }
           // Pump ON command
           else if ((action.equals("MouseClick") ) && origintag.contains("RearmON")) {
              String cmdName =  CMD.get("Rearm");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);  
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
                 showParent = false;
              }
           }
           // Pump OFF command
           else if ((action.equals("MouseClick") ) && origintag.contains("RearmOFF")) {
              String cmdName =  CMD.get("Rearm");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
                 showParent = false;
              }
           }
        }
    }
}
