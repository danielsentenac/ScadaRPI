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

public class GlgRGA2Gui extends GlgChildGui {

    private static final long serialVersionUID = 364054054075L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgRGA2Gui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("RGA3.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("title", origin, true);

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
       STATUS.put("RGAoperation", TUBE1500WSTATUS.get(origin + "operationsub"));
       STATUS.put("RGAoperationCol", TUBE1500WSTATUS.get(origin + "operationsub"));
       STATUS.put("RGAoperationStr", TUBE1500WSTATUS.get(origin + "operationsub"));
       STATUS.put("RGAdegas", TUBE1500WSTATUS.get(origin + "degassub"));
       STATUS.put("RGAdegasCol", TUBE1500WSTATUS.get(origin + "degassub"));
       STATUS.put("RGAdegasStr", TUBE1500WSTATUS.get(origin + "degassub"));
       STATUS.put("RGArunning", TUBE1500WSTATUS.get(origin + "runningsub"));
       STATUS.put("RGArunningCol", TUBE1500WSTATUS.get(origin + "runningsub"));
       STATUS.put("RGArunningStr", TUBE1500WSTATUS.get(origin + "runningsub"));
       STATUS.put("RGAmode", TUBE1500WSTATUS.get(origin + "modesub"));
       STATUS.put("RGAmodeCol", TUBE1500WSTATUS.get(origin + "modesub"));
       STATUS.put("RGAmodeStr", TUBE1500WSTATUS.get(origin + "modesub"));
       STATUS.put("FilamentStatus", TUBE1500WSTATUS.get(origin + "filamentsub"));
       STATUS.put("FilamentStatusCol", TUBE1500WSTATUS.get(origin + "filamentsub"));
       STATUS.put("FilamentStatusStr", TUBE1500WSTATUS.get(origin + "filamentsub"));
       STATUS.put("ElectronEnergy", TUBE1500WSTATUS.get(origin + "electronenergysub"));
       STATUS.put("ElectronEnergyCol", TUBE1500WSTATUS.get(origin + "electronenergysub"));
       STATUS.put("ElectronEnergyStr", TUBE1500WSTATUS.get(origin + "electronenergysub"));
       STATUS.put("Emission", TUBE1500WSTATUS.get(origin + "emissionsub"));
       STATUS.put("EmissionCol", TUBE1500WSTATUS.get(origin + "emissionsub"));
       STATUS.put("EmissionStr", TUBE1500WSTATUS.get(origin + "emissionsub"));
       STATUS.put("Multiplier", TUBE1500WSTATUS.get(origin + "multipliersub"));
       STATUS.put("MultiplierCol", TUBE1500WSTATUS.get(origin + "multipliersub"));
       STATUS.put("MultiplierStr", TUBE1500WSTATUS.get(origin + "multipliersub"));
       STATUS.put("Focus", TUBE1500WSTATUS.get(origin + "focussub"));
       STATUS.put("FocusCol", TUBE1500WSTATUS.get(origin + "focussub"));
       STATUS.put("FocusStr", TUBE1500WSTATUS.get(origin + "focussub"));
       STATUS.put("Cage", TUBE1500WSTATUS.get(origin + "cagesub"));
       STATUS.put("CageCol", TUBE1500WSTATUS.get(origin + "cagesub"));
       STATUS.put("CageStr", TUBE1500WSTATUS.get(origin + "cagesub"));

       String channelName =  STATUS.get("RGAoperation");
       // Get corresponding device from deviceManager
       String deviceName = channelName.split("_",2)[0];
       logger.finer("ORIGIN = " + origin);
       for ( int j = 0 ; j < 199 ; j++) {// RGA data (plot)
          int index = j + 1;
          glg_bean.SetSResource("/RGA/DataGroup/DataSample" + j + "/TooltipLabel", 
                                "Mass:" + Integer.toString(index) ); // Set tooltip label
          if (index < 10 ) 
             STATUS.put("/RGA/DataGroup/DataSample" + j + "/Value", deviceName + "_" + "00" + index);
          else if ( index >= 10 && index < 100 ) 
             STATUS.put("/RGA/DataGroup/DataSample" + j + "/Value", deviceName + "_" + "0" + index);
          else
             STATUS.put("/RGA/DataGroup/DataSample" + j + "/Value", deviceName + "_"  + index);
       }
    }

    public void updateGuiFeatures(String glgName, DataElement dataElement) {

        // RGA Gui case
        logger.finer("GlgRGAGui:updateGuiFeatures> UPDATE " + dataElement.name + "(" + glgName + ") with value=" 
                      + dataElement.value);
        if ( glgName.contains("operationCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, RGAONOFFColorSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("operationStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  RGAOperationONOFFSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("runningCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, RGAONOFFColorSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("runningStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  RGARunningONOFFSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("degasCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, RGAONOFFColorSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("degasStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  RGADegasONOFFSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("modeCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, RGAONOFFColorSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("modeStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  RGAMODESTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("FilamentStatusCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, RGAFilamentColorSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("FilamentStatusStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  RGAFilamentSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("ElectronEnergyCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, new GlgPoint(0.6,0.6,0.6), true);
        }
        if ( glgName.contains("ElectronEnergyStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  "Electron Energy : " + Double.toString(dataElement.value) + " V", true);
        }
        if ( glgName.contains("EmissionCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, new GlgPoint(0.6,0.6,0.6), true);
        }
        if ( glgName.contains("EmissionStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  "Emission Current : " + Double.toString(dataElement.value) + " uA", true);
        }
        if ( glgName.contains("MultiplierCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, new GlgPoint(0.6,0.6,0.6), true);
        }
        if ( glgName.contains("MultiplierStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  "Multiplier Voltage : " + Double.toString(dataElement.value) + " V", true);
        }
        if ( glgName.contains("FocusCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, new GlgPoint(0.6,0.6,0.6), true);
        }
        if ( glgName.contains("FocusStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  "Focus Voltage : " + Double.toString(dataElement.value) + " V", true);
        }
        if ( glgName.contains("CageCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, new GlgPoint(0.6,0.6,0.6), true);
        }
        if ( glgName.contains("CageStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  "Cage Voltage : " + Double.toString(dataElement.value) + " V", true);
        }
        if ( glgName.contains("DataSample") ) { // DataSample is a tag for plot object value (double type) property
           if ( dataElement.value > 0 ) { // Log plot RGA
              logger.finer("UPDATE " + glgName + " dataName " + dataElement.name + " value " + dataElement.value);
              glg_bean.SetDResource(glgName, Math.log10(dataElement.value), true);
              glg_bean.SetDResource(glgName.replace("Value","TooltipValue"),dataElement.value);// Set tooltip value
           }
           else {
              logger.finer("UPDATE " + glgName + " dataName " + dataElement.name + " value " + dataElement.value);
              glg_bean.SetDResource(glgName, Math.log10(0), true);
           }
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
           // RGA Emission ON/OFF command
           else if ((action.equals("MouseClick") ) && origintag.equals("RGAoperationON") ) {
              String cmdName =  CMD.get(origin + "operation");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
              }
           }
           else if ((action.equals("MouseClick") ) && origintag.equals("RGAoperationOFF")) {
              String cmdName =  CMD.get(origin + "operation");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
              }
           }
           // RGA Running ON/OFF command
           else if ((action.equals("MouseClick") ) && origintag.equals("RGArunningON") ) {
              String cmdName =  CMD.get(origin + "running");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
              }
           }
           else if ((action.equals("MouseClick") ) && origintag.equals("RGArunningOFF")) {
              String cmdName =  CMD.get(origin + "running");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
              }
           }
           // RGA Degas ON/OFF command
           else if ((action.equals("MouseClick") ) && origintag.equals("RGAdegasON") ) {
              String cmdName =  CMD.get(origin + "degas");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
              }
           }
           else if ((action.equals("MouseClick") ) && origintag.equals("RGAdegasOFF")) {
              String cmdName =  CMD.get(origin + "degas");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
              }
           }
           else if (action.equals("MouseClick") && origintag.contains("Filament") ) { // SetPoint fields
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                if (origintag.contains("Filament") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Filament (1 OR 2)", true);

              }
           }
           else if (action.equals("MouseClick") && origintag.contains("mode") ) { // SetPoint fields
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("RGAmode") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Mode (0=Faraday, 1=SEM)", true);
              }
           }
           else if (action.equals("MouseClick") && origintag.contains("ElectronEnergy") ) { // SetPoint fields
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("ElectronEnergy") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Electron Energy (4 -> 150 V)", true);
              }
           }
           else if (action.equals("MouseClick") && origintag.contains("Multiplier") ) { // SetPoint fields
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("Multiplier") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Multiplier (0 -> 2000 V)", true);
              }
           }
           else if (action.equals("MouseClick") && origintag.contains("Emission") ) { // SetPoint fields
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("Emission") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Emission Current (20 -> 5000 uA)", true);
              }
           }
           else if (action.equals("MouseClick") && origintag.contains("Focus") ) { // SetPoint fields
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("Focus") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Focus (-200 -> 0 V)", true);
              }
           }
           else if (action.equals("MouseClick") && origintag.contains("Cage") ) { // SetPoint fields
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              logger.finer("GlgRgaGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("Cage") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Cage (-10 -> 10 V)", true);
              }
           }
        }
    }
}
