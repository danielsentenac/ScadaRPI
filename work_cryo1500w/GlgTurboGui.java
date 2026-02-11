/*
 * This Class is used for Turbo Pump GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgTurboGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054075L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgTurboGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("Turbo.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("Pname", origin, true);

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

       // Turbo Gui case
       STATUS.clear();
       STATUS.put("Pname", CRYO1500WSTATUS.get(origin));
       STATUS.put("P", CRYO1500WSTATUS.get(origin));
       STATUS.put("PCol", CRYO1500WSTATUS.get(origin));
       STATUS.put("PStr", CRYO1500WSTATUS.get(origin));
       STATUS.put("PErr", CRYO1500WSTATUS.get(origin + "Errsub"));
       STATUS.put("PErrCol", CRYO1500WSTATUS.get(origin + "Errsub"));
       STATUS.put("PErrStr", CRYO1500WSTATUS.get(origin + "Errsub"));
       STATUS.put("PSty", CRYO1500WSTATUS.get(origin + "Stysub"));
       STATUS.put("PStyCol", CRYO1500WSTATUS.get(origin + "Stysub"));
       STATUS.put("PStyStr", CRYO1500WSTATUS.get(origin + "Stysub"));
       STATUS.put("PTemp", CRYO1500WSTATUS.get(origin + "Tempsub"));
       STATUS.put("PTempCol", CRYO1500WSTATUS.get(origin + "Tempsub"));
       STATUS.put("PTempStr", CRYO1500WSTATUS.get(origin + "Tempsub"));
       STATUS.put("PBTemp", CRYO1500WSTATUS.get(origin + "BTempsub"));
       STATUS.put("PBTempCol", CRYO1500WSTATUS.get(origin + "BTempsub"));
       STATUS.put("PBTempStr", CRYO1500WSTATUS.get(origin + "BTempsub"));
       STATUS.put("PTempVal", CRYO1500WSTATUS.get(origin + "TempValsub"));
       STATUS.put("PBTempVal", CRYO1500WSTATUS.get(origin + "BTempValsub"));
       STATUS.put("PSpeedVal", CRYO1500WSTATUS.get(origin + "SpeedValsub"));
       STATUS.put("PPowerVal", CRYO1500WSTATUS.get(origin + "PowerValsub"));
       STATUS.put("PFspeedVal", CRYO1500WSTATUS.get(origin + "FspeedValsub"));
       STATUS.put("PHoursVal", CRYO1500WSTATUS.get(origin + "HoursValsub"));      
    }

    public void updateGuiFeatures(String glgName, DataElement dataElement) {

        // Turbo Pump Gui case
        //logger.finer("GlgTurboGui:updateGuiFeatures> UPDATE " + dataElement.name + "(" + glgName + ") with value=" 
        //              + dataElement.value);
        if ( glgName.equals("PCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, TurboONOFFColorSTATUS.get((int)dataElement.value), true);
           
        }
        if ( glgName.equals("PStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  TurboONOFFSTATUS.get((int)dataElement.value), true);
           
        }
        if ( glgName.startsWith("PSty") ) {  // Standby status
           if (glgName.contains("Col") )  // Col is a tag for object status (short type) color property
               glg_bean.SetGTag(glgName, TurboStandbyColorSTATUS.get((int)dataElement.value), true);
           if (glgName.contains("Str") )  // Str is a tag for object status (short type) string property
               glg_bean.SetSTag(glgName, TurboStandbySTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.startsWith("PBTemp") ) {  // Box Temperature status
           if (glgName.contains("Col") )  // Col is a tag for object status (short type) color property
               glg_bean.SetGTag(glgName, TurboTemperatureBoxColorSTATUS.get((int)dataElement.value), true);
           if (glgName.contains("Str") )  // Str is a tag for object status (short type) string property
               glg_bean.SetSTag(glgName, TurboTemperatureBoxSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.startsWith("PTemp") ) {  // Temperature status
           if (glgName.contains("Col") )  // Col is a tag for object status (short type) color property
               glg_bean.SetGTag(glgName, TurboTemperatureColorSTATUS.get((int)dataElement.value), true);
           if (glgName.contains("Str") )  // Str is a tag for object status (short type) string property
               glg_bean.SetSTag(glgName, TurboTemperatureSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.startsWith("PErr") ) {  // Error status
           if (glgName.contains("Col") )  // Col is a tag for object status (short type) color property
               glg_bean.SetGTag(glgName, Turbo2ErrorColorSTATUS.get((int)dataElement.value), true);
           if (glgName.contains("Str") )  // Str is a tag for object status (short type) string property
               glg_bean.SetSTag(glgName, Turbo2ErrorSTATUS.get((int)dataElement.value), true);
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
           // Turbo ON/OFF command
           else if ((action.equals("MouseClick") ) && !origintag.contains("Sty")) {
              String cmdName =  CMD.get(origin);
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              if (origintag.contains("ON")) {     
                 // Update dataElement
                 if ( device != null ) {
                    DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                    dataElement.setvalue = 1;
                    device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
                    parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    parent.toFront();
                    parent.isSuspended = false;
                    child.isSuspended = true;
                 }
              }
              else if (origintag.contains("OFF")) {
                 // Update dataElement
                 if ( device != null ) {
                    DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                    dataElement.setvalue = 2;
                    device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
                    parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    parent.toFront();
                    parent.isSuspended = false;
                    child.isSuspended = true;
                 }
              }
           }
           // Turbo ON/OFF command
           else if ((action.equals("MouseClick") )&& origintag.contains("Sty")) {
              String cmdName =  CMD.get(origin + "Sty");
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              //logger.finer("InputListener:InputCallback> execute " + origintag + " command " + cmdName 
              //+ " (device " + deviceName + ")");
              if (origintag.contains("ON")) {     
                 // Update dataElement
                 //logger.finer("InputListener:InputCallback> device " + device.name);
                 if ( device != null ) {
                    DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                    dataElement.setvalue = 1;
                    device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
                    //logger.finer("InputListener:InputCallback> set data command value " + dataElement.value);
                    parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    parent.toFront();
                    parent.isSuspended = false;
                    child.isSuspended = true;
                 }
              }
              else if (origintag.contains("OFF")) {
                 // Update dataElement
                 if ( device != null ) {
                    DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                    dataElement.setvalue = 2;
                    device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
                    parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    parent.toFront();
                    parent.isSuspended = false;
                    child.isSuspended = true;
                 }
              }
           }
        }
    }
}
