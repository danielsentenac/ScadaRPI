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

public class GlgRGAGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054075L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgRGAGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("RGA.g");
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
       STATUS.put("RGAemission", RGASTATUS.get(origin + "emissionsub"));
       STATUS.put("RGAemissionCol", RGASTATUS.get(origin + "emissionsub"));
       STATUS.put("RGAemissionStr", RGASTATUS.get(origin + "emissionsub"));
       STATUS.put("RGAdegas", RGASTATUS.get(origin + "degassub"));
       STATUS.put("RGAdegasCol", RGASTATUS.get(origin + "degassub"));
       STATUS.put("RGAdegasStr", RGASTATUS.get(origin + "degassub"));
       STATUS.put("RGArunning", RGASTATUS.get(origin + "runningsub"));
       STATUS.put("RGArunningCol", RGASTATUS.get(origin + "runningsub"));
       STATUS.put("RGArunningStr", RGASTATUS.get(origin + "runningsub"));
       STATUS.put("RGAmode", RGASTATUS.get(origin + "modesub"));
       STATUS.put("RGAmodeCol", RGASTATUS.get(origin + "modesub"));
       STATUS.put("RGAmodeStr", RGASTATUS.get(origin + "modesub"));
       STATUS.put("RGAfilament", RGASTATUS.get(origin + "filamentsub"));
       STATUS.put("RGAfilamentCol", RGASTATUS.get(origin + "filamentsub"));
       STATUS.put("RGAfilamentStr", RGASTATUS.get(origin + "filamentsub"));
       String channelName =  STATUS.get("RGAemission");
       // Get corresponding device from deviceManager
       String deviceName = channelName.split("_",2)[0];
       logger.finer("ORIGIN = " + origin);
       for ( int j = 0 ; j < 199 ; j++) {// RGA data (plot)
          int index = j + 1;
          glg_bean.SetSResource("/RGA/DataGroup/DataSample" + j + "/TooltipLabel", 
                                "Mass:" + Integer.toString(j+1) ); // Set tooltip label
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
        if ( glgName.contains("emissionCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, RGAONOFFColorSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("emissionStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  RGAEmissionONOFFSTATUS.get((int)dataElement.value), true);
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
        if ( glgName.contains("filamentCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, RGAFilamentColorSTATUS.get((int)dataElement.value), true);
        }
        if ( glgName.contains("filamentStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  RGAFilamentSTATUS.get((int)dataElement.value), true);
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
           else if ((action.equals("MouseClick") ) && origintag.equals("RGAemissionON") ) {
              String cmdName =  CMD.get(origin + "emission");
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
           else if ((action.equals("MouseClick") ) && origintag.equals("RGAemissionOFF")) {
              String cmdName =  CMD.get(origin + "emission");
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
           // RGA SEM ON/OFF command
           else if ((action.equals("MouseClick") ) && origintag.equals("RGASEMON") ) {
              String cmdName =  CMD.get(origin + "SEM");
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
           else if ((action.equals("MouseClick") ) && origintag.equals("RGASEMOFF")) {
              String cmdName =  CMD.get(origin + "SEM");
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
           else if (action.equals("MouseClick") && origintag.contains("filament") ) { // SetPoint fields
              System.out.println("GlgRgaGui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              System.out.println("GlgRgaGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("RGAfilament") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Filament (0=0, 1=1, 1&2=2)", true);
              }
           }
           else if (action.equals("MouseClick") && origintag.contains("mode") ) { // SetPoint fields
              System.out.println("GlgRgaGui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              System.out.println("GlgRgaGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
              if (dataElement != null) {
                 if (origintag.contains("RGAmode") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Mode (0=Faraday, 1=SEM)", true);
              }
           }
        }
    }
}
