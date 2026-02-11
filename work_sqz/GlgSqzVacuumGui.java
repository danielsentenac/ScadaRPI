/*
 * This Class is used for RGA GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgSqzVacuumGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054075L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgSqzVacuumGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("VACUUM.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          //glg_bean.SetSTag("title", origin, true);

          // Create STATUS map origin dependent feature
          createStatusMap(STATUS);
       
          // Create CMD map origin dependent feature
          createCommandMap(CMD);
       } 
      catch (Exception e) {
          logger.log(Level.SEVERE, e.getMessage());
          e.printStackTrace();
      }
    }

    protected void createStatusMap(Hashtable < String, String > STATUS) {

       // RGA Gui case
       STATUS.clear();
       STATUS.put("VACUUMStatus", "VAC_ST");
       STATUS.put("VACUUMStatusCol", "VAC_ST");
       STATUS.put("VACUUMStatusStr", "VAC_ST");
       STATUS.put("VACUUMCOMStatus", "VAC_COMST");
       STATUS.put("VACUUMCOMStatusCol", "VAC_COMST");
       STATUS.put("VACUUMCOMStatusStr", "VAC_COMST");
       STATUS.put("P21Status", "DCU_P21ST");
       STATUS.put("P21StatusCol", "DCU_P21ST");
       STATUS.put("P21StatusStr", "DCU_P21ST");
       STATUS.put("P22Status", "M1_P22ST");
       STATUS.put("P21SPEEDVal","DCU_P21SPEED");
       STATUS.put("P22StatusCol", "M1_P22ST");
       STATUS.put("P22StatusStr", "M1_P22ST");
       STATUS.put("V21Status", "M1_V21ST");
       STATUS.put("V21StatusCol", "M1_V21ST");
       STATUS.put("V21StatusStr", "M1_V21ST");
       STATUS.put("V22Status", "M1_V22ST");
       STATUS.put("V22StatusCol", "M1_V22ST");
       STATUS.put("V22StatusStr", "M1_V22ST");
       STATUS.put("V23Status", "M1_V23ST");
       STATUS.put("V23StatusCol", "M1_V23ST");
       STATUS.put("V23StatusStr", "M1_V23ST");
       STATUS.put("V1Status", "M1_V1ST");
       STATUS.put("V1StatusCol", "M1_V1ST");
       STATUS.put("V1StatusStr", "M1_V1ST");
       STATUS.put("V24Status", "M2_V24ST");
       STATUS.put("V24StatusCol", "M2_V24ST");
       STATUS.put("V24StatusStr", "M2_V24ST");
       STATUS.put("V25Status", "M2_V24ST");
       STATUS.put("V25StatusCol", "M2_V24ST");
       STATUS.put("V25StatusStr", "M2_V24ST");
       STATUS.put("VPStatus", "M2_VPST");
       STATUS.put("VPStatusCol", "M2_VPST");
       STATUS.put("VPStatusStr", "M2_VPST");
       STATUS.put("Ge1Val","MG_PR3");
       STATUS.put("Ge2Val","MG_PR5");
       STATUS.put("G22Val","MG_PR1");
       STATUS.put("BypassG22Status","M1_BYPASSST");
       STATUS.put("BypassG22StatusStr","M1_BYPASSST");
       STATUS.put("BypassG22StatusCol","M1_BYPASSST");
    }

    public void updateGuiFeatures(String glgName, DataElement dataElement) {

        // RGA Gui case
        logger.finer("GlgVacuum:updateGuiFeatures> UPDATE " + dataElement.name + "(" + glgName + ") with value=" 
                      + dataElement.value);
       
        if ( glgName.contains("P21") && glgName.contains("Col") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, TurboONOFFColorSTATUS.get((int)dataElement.value), true); 
        }
        if ( glgName.contains("P21") && glgName.contains("Str") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  glgName.replace("Str", "") +" "+ TurboONOFFSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("P22") && glgName.contains("Col")) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName,  RelayGeneralColorSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("P22") && glgName.contains("Str")) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  glgName.replace("Str", "") +" "+ RelayGeneralSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("V") && !glgName.contains("VACUUM") && glgName.contains("Col") ) { // Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, ValveColorSTATUS.get((int)dataElement.value), true);
            //logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
        }
        if ( glgName.contains("V") && !glgName.contains("VACUUM") && glgName.contains("Str") ) {  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName,  glgName.replace("Str", "") +" "+ ValveSTATUS.get((int)dataElement.value), true);
            //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);
        }
        if ( glgName.contains("VACUUM") && !glgName.contains("COM") && glgName.contains("Col") ) { // Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, VacuumColorSTATUS.get((int)dataElement.value), true);
            //logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
        }
        if ( glgName.contains("VACUUM") && !glgName.contains("COM") && glgName.contains("Str") ) {  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName,  glgName.replace("Str", "") +" "+ VacuumSTATUS.get((int)dataElement.value), true);
            //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);
        }
        if ( glgName.contains("VACUUMCOM") && glgName.contains("Col") ) { // Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, VacuumComColorSTATUS.get((int)dataElement.value), true);
            //logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
        }
        if ( glgName.contains("VACUUMCOM") && glgName.contains("Str") ) {  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName,  glgName.replace("Str", "") +" "+ VacuumComSTATUS.get((int)dataElement.value), true);
            //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);
        }
        if (glgName.contains("Bypass")) {
          if ( glgName.contains("Col") )// Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, BypassColorSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> BypassColorSTATUS=" + dataElement.value);
          if ( glgName.contains("Str") )  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName, glgName.replace("Str", "") +" "+ BypassSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);       
       }
        if ( glgName.contains("Val") )   // Val is a tag for object value (double type) property
            glg_bean.SetDTag(glgName, dataElement.value, true);  
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
           // VACUUM START command
           else if ((action.equals("MouseClick") ) && origintag.equals("VACUUMStart") ) {
              Device device =  deviceManager.getDevice("VAC");
              DataElement dataElement = device.getDataElement("STARTSTOP");
              new DialogConfirm(parent, origintag, device, dataElement,"Start", true);
           }
           // VACUUM STOP command
           else if ((action.equals("MouseClick") ) && origintag.equals("VACUUMStop") ) {
              Device device =  deviceManager.getDevice("VAC");
              DataElement dataElement = device.getDataElement("STARTSTOP");
              new DialogConfirm(parent, origintag, device, dataElement,"Stop", true);
              dataElement.setvalue = 2;
              device.commandSetQueue.add(dataElement); // Set STOP internal data command trigger 
           }
        }
    }
}
