/*
 * This Class is used for HidenBenchStatusDetails GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgHidenBenchStatusDetailsGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054072L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgHidenBenchStatusDetailsGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          // HidenBenchStatusDetails display
          glg_bean.SetDrawingFile("HidenBenchStatusDetails.g");
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
       CMD.putAll(HIDENBENCHCMD);
    }

    public void createStatusMap(Hashtable < String, String > STATUS) {

       // HidenBenchStatusDetails Gui case
       STATUS.clear();
       STATUS.putAll(HidenBenchStatusDetails);
    }

    protected void updateGuiFeatures(String glgName, DataElement dataElement) {
       //logger.info("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
       //   dataElement.name + " type=" + dataElement.type + " value=" + dataElement.value);
       // HidenBenchStatusDetails Gui case
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
           // Bypass ON command
           else if ((action.equals("MouseClick") ) && origintag.contains("BypassON")) {
              String cmdName =  CMD.get("Bypass");
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
           // Bypass OFF command
           else if ((action.equals("MouseClick") ) && origintag.contains("BypassOFF")) {
              String cmdName =  CMD.get("Bypass");
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
