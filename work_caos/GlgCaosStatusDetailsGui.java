/*
 * This Class is used for CaosStatusDetails GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgCaosStatusDetailsGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054072L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgCaosStatusDetailsGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          // CaosStatusDetails display
          glg_bean.SetDrawingFile("CaosStatusDetails.g");
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
       CMD.putAll(CAOSCMD);
    }

    public void createStatusMap(Hashtable < String, String > STATUS) {

       // CaosStatusDetails Gui case
       STATUS.clear();
       STATUS.putAll(CaosStatusDetails);
    }

    protected void updateGuiFeatures(String glgName, DataElement dataElement) {
       //logger.info("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
       //   dataElement.name + " type=" + dataElement.type + " value=" + dataElement.value);
       // CaosStatusDetails Gui case
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
       else { // All other cases (AlarmCom*, AirCompressed*)
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
           // Bypass ON/OFF commands: per-chamber G22 bypass + single Gp bypass
           // (BypassG22_1ON/OFF, BypassG22_2ON/OFF, BypassGpON/OFF)
           else if ((action.equals("MouseClick") ) && origintag.contains("Bypass")) {
              String[] bypassKeys = { "BypassG22_1", "BypassG22_2", "BypassGp" };
              for (String key : bypassKeys) {
                 int value;
                 if (origintag.contains(key + "ON")) value = 1;       // Set ON internal data command trigger
                 else if (origintag.contains(key + "OFF")) value = 2; // Set OFF internal data command trigger
                 else continue;
                 String cmdName =  CMD.get(key);
                 logger.finer("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
                 logger.finer("cmdName=" + cmdName);
                 if ( cmdName == null ) return;
                 // Get corresponding device from deviceManager
                 String deviceName = cmdName.split("_")[0];
                 Device device = deviceManager.getDevice(deviceName);
                 // Update dataElement
                 if ( device != null ) {
                    DataElement dataElement = device.getDataElement(cmdName.split("_")[1]);
                    dataElement.setvalue = value;
                    device.commandSetQueue.add(dataElement);
                    showParent = true;
                 }
                 break;
              }
           }
        }
    }
}
