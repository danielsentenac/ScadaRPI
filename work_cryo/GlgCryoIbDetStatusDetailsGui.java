/*
 * This Class is used for CryoIbDetStatusDetails GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgCryoIbDetStatusDetailsGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054072L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgCryoIbDetStatusDetailsGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          // CryoIbDetStatusDetails display
          glg_bean.SetDrawingFile("CryoIbDetStatusDetails.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("title", title + " RACK STATUS DETAILS", true);

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
       CMD.putAll(CRYOIBDETCMD);
    }

    public void createStatusMap(Hashtable < String, String > STATUS) {

       // CryoIbDetStatusDetails Gui case
       STATUS.clear();
       STATUS.putAll(CryoIbDetStatusDetails);
    }

    protected void updateGuiFeatures(String glgName, DataElement dataElement) {
       //logger.info("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
       //   dataElement.name + " type=" + dataElement.type + " value=" + dataElement.value);
       // CryoIbDetStatusDetails Gui case
       if ( glgName.contains("Val")) {  // Val is a tag for object value (double type) property
          glg_bean.SetDTag(glgName, dataElement.value, true);
          //logger.info("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
          //dataElement.name + " type=" + dataElement.type + " value=" + dataElement.value);
       }
       if (glgName.contains("Bypass")) {
          if ( glgName.contains("Col") )// Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, BypassColorSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> BypassColorSTATUS=" + dataElement.value);
          if ( glgName.contains("Str") )  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName, glgName.replace("Str", "") +" "+ BypassSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);       
       }
       else if (glgName.contains("FanSpeed")) {
          if ( glgName.contains("Col") )// Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, FanSpeedColorSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> FanSpeedColorSTATUS=" + dataElement.value);
          if ( glgName.contains("Str") )  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName, glgName.replace("Str", "") +" "+ FanSpeedSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);       
       }
       else if (glgName.contains("FanStatus")) {
          if ( glgName.contains("Col") )// Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, FanColorSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> FanStatusColorSTATUS=" + dataElement.value);
          if ( glgName.contains("Str") )  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName, glgName.replace("Str", "") +" "+ FanSTATUS.get((int)dataElement.value), true);
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
           logger.finer("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           
           // Clicked on MAIN button
           if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("GLOBAL")) {
                parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                parent.toFront();
                parent.isSuspended = false;
                child.isSuspended = true;
           }
           // Bypass G22 ON command
           else if ((action.equals("MouseClick") ) && origintag.contains("BypassG22ON")) {
              String cmdName =  CMD.get("BypassG22");
              logger.finer("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
              logger.finer("cmdName=" + cmdName);
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName); 
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
                 showParent = true;
              }
           }
           // Bypass G22 OFF command
           else if ((action.equals("MouseClick") ) && origintag.contains("BypassG22OFF")) {
              String cmdName =  CMD.get("BypassG22");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
                 showParent = true;
              }
           }
           // Bypass Gp ON command
           else if ((action.equals("MouseClick") ) && origintag.contains("BypassGpON")) {
              String cmdName =  CMD.get("BypassGp");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);  
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
                 showParent = true;
              }
           }
           // Bypass Gp OFF command
           else if ((action.equals("MouseClick") ) && origintag.contains("BypassGpOFF")) {
              String cmdName =  CMD.get("BypassGp");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
                 showParent = true;
              }
           }
           // Fan ON command
           else if ((action.equals("MouseClick") ) && origintag.contains("FanON")) {
              String cmdName =  CMD.get("Fan");
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
           // Fan OFF command
           else if ((action.equals("MouseClick") ) && origintag.contains("FanOFF")) {
              String cmdName =  CMD.get("Fan");
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
           // FanSpeed NORMAL command
           else if ((action.equals("MouseClick") ) && origintag.contains("FanNormal")) {
              String cmdName =  CMD.get("FanSpeed");
              logger.finer("Origin=FanSpeedNormal");
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
           // FanSpeed Low command
           else if ((action.equals("MouseClick") ) && origintag.contains("FanLow")) {
              String cmdName =  CMD.get("FanSpeed");
              logger.finer("Origin=FanSpeedLow");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set LOW internal data command trigger
                 showParent = false;
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
        }
    }
}
