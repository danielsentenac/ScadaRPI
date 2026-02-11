/*
 * This Class is used for Titane Pump GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgTitane_22Gui extends GlgChildGui {

    private static final long serialVersionUID = 354054054075L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgTitane_22Gui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("Titane_22.g");
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

       // Titane Gui case
       STATUS.clear();
       STATUS.put("Pname", TUBESTATUS.get(origin));
       STATUS.put("P", TUBESTATUS.get(origin));
       STATUS.put("PCol", TUBESTATUS.get(origin));
       STATUS.put("PStr", TUBESTATUS.get(origin));
       STATUS.put("PStart", TUBESTATUS.get(origin + "Startsub"));
       STATUS.put("PStartCol", TUBESTATUS.get(origin + "Startsub"));
       STATUS.put("PStartStr", TUBESTATUS.get(origin + "Startsub"));
       STATUS.put("PCtrl", TUBESTATUS.get(origin + "Ctrlsub"));
       STATUS.put("PCtrlCol", TUBESTATUS.get(origin + "Ctrlsub"));
       STATUS.put("PCtrlStr", TUBESTATUS.get(origin + "Ctrlsub"));
       STATUS.put("PErr", TUBESTATUS.get(origin + "Errsub"));
       STATUS.put("PErrCol", TUBESTATUS.get(origin + "Errsub"));
       STATUS.put("PErrStr", TUBESTATUS.get(origin + "Errsub"));
       STATUS.put("PREmode", TUBESTATUS.get(origin + "REmodesub"));
       STATUS.put("PREmodeCol", TUBESTATUS.get(origin + "REmodesub"));
       STATUS.put("PREmodeStr", TUBESTATUS.get(origin + "REmodesub"));
       STATUS.put("PFILmode", TUBESTATUS.get(origin + "FILmodesub"));
       STATUS.put("PFILmodeCol", TUBESTATUS.get(origin + "FILmodesub"));
       STATUS.put("PFILmodeStr", TUBESTATUS.get(origin + "FILmodesub"));
       STATUS.put("POPmode", TUBESTATUS.get(origin + "OPmodesub"));
       STATUS.put("POPmodeCol", TUBESTATUS.get(origin + "OPmodesub"));
       STATUS.put("POPmodeStr", TUBESTATUS.get(origin + "OPmodesub"));
       STATUS.put("PCTRLOPmode", TUBESTATUS.get(origin + "CTRLOPmodesub"));
       STATUS.put("PCTRLOPmodeCol", TUBESTATUS.get(origin + "CTRLOPmodesub"));
       STATUS.put("PCTRLOPmodeStr", TUBESTATUS.get(origin + "CTRLOPmodesub"));
       STATUS.put("PAcurrentVal", TUBESTATUS.get(origin + "AcurrentValsub"));
       STATUS.put("PAvoltageVal", TUBESTATUS.get(origin + "AvoltageValsub"));
       STATUS.put("PSublcurrentVal", TUBESTATUS.get(origin + "SublcurrentValsub"));
       STATUS.put("PSubltimeVal", TUBESTATUS.get(origin + "SubltimeValsub"));
       STATUS.put("PSublperiodVal", TUBESTATUS.get(origin + "SublperiodValsub"));
       STATUS.put("PSublwaitVal", TUBESTATUS.get(origin + "SublwaitValsub"));
       STATUS.put("PCycleVal", TUBESTATUS.get(origin + "CycleValsub"));
       STATUS.put("PHrVal", TUBESTATUS.get(origin + "HrValsub"));
       STATUS.put("PBTempVal", TUBESTATUS.get(origin + "BTempValsub"));
    }

    public void updateGuiFeatures(String origintag, DataElement dataElement) {

        // Titane Pump Gui case
        //logger.finer("GlgTitane_22Gui:updateGuiFeatures> UPDATE " + dataElement.name + " (" + origintag + ") with value=" 
        //              + dataElement.value);
        if ( origintag.equals("PCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, RelayGeneralColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  RelayGeneralSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PStartCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, TitaneStartColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PStartStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  TitaneStartSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PCtrlCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, TitaneControllerColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PCtrlStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  TitaneControllerSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PErrCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, TitaneErrorColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PErrStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  TitaneErrorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PFILmodeCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, TitaneFilamentColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PFILmodeStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  TitaneFilamentSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PAUTOSTARTmodeCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, TitaneAutoStartModeColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PAUTOSTARTmodeStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  TitaneAutoStartModeSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PRECOVERmodeCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, TitaneRecoverModeColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PRECOVERmodeStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  TitaneRecoverModeSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PCTRLOPmodeCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, TitaneControllerOperatingModeColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PCTRLOPmodeStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  TitaneControllerOperatingModeSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.contains("Val") )   // Val is a tag for object value (double type) property
            glg_bean.SetDTag(origintag, dataElement.value, true);    
    }
  
    public class InputListener implements GlgInputListener { 

	public void InputCallback( GlgObject viewport, GlgObject message_obj )  {

           String origintag, format, action, subaction;
           origintag = message_obj.GetSResource( "Origin" );
	   format = message_obj.GetSResource( "Format" );
	   action = message_obj.GetSResource( "Action" );
	   subaction = message_obj.GetSResource( "SubAction" );
           logger.finer("GlgTitane_22Gui::InputListener> Origin=" + origintag + " Format=" + format + 
                       " Action=" + action + " subAction=" + subaction);
           
           // Clicked on MAIN button
           if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("GLOBAL")) {
                parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                parent.toFront();
                parent.isSuspended = false;
                child.isSuspended = true;
           }
           // Titane ON/OFF command
           else if ((action.equals("MouseClick") ) && origintag.equals("PON") ) {
              String cmdName =  CMD.get(origin);
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_")[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
              }
           }
           else if ((action.equals("MouseClick") ) && origintag.equals("POFF")) {
              String cmdName =  CMD.get(origin);
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_")[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
              }
           }
           else if ((action.equals("MouseClick") ) && origintag.equals("PSublON")) {
              String cmdName =  CMD.get(origin + "Subl");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_")[0];
              Device device = deviceManager.getDevice(deviceName);   
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_")[1]);
                 dataElement.setvalue = 1;
                 device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
              }
           }
           else if ((action.equals("MouseClick") ) && origintag.equals("PSublOFF")) {
              String cmdName =  CMD.get(origin + "Subl");
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_")[0];
              Device device = deviceManager.getDevice(deviceName);   
              // Update dataElement
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_")[1]);
                 dataElement.setvalue = 2;
                 device.commandSetQueue.add(dataElement); // Set OFF internal data command trigger
              }
           }
           else if (action.equals("MouseClick") && origintag.contains("Val") || origintag.contains("mode") ) { // SetPoint fields
              System.out.println("GlgTitane_22Gui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              System.out.println("GlgTitane_22Gui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_")[1]);
              if ( dataElement != null ) {
                 if (origintag.contains("Sublcurrent") ) 
                    new DialogSetPoint(parent, origintag, device, dataElement,"Sublimation Current (x0.1) A", true);
                 else if (origintag.contains("Subltime") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Sublimation Time (x0.1) mn", true);
                 else if (origintag.contains("Sublperiod") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Sublimation Period (x0.1) mn", true);
                 else if (origintag.contains("FILmode") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Ti-Ball=0, TSP(1)=1, TSP(2)=2, TSP(3)=3", true);
                 else if (origintag.contains("AUTOSTARTmode"))
                    new DialogSetPoint(parent, origintag, device, dataElement, "<html>YES=0,<br/>NO=1", true);
                 else if (origintag.contains("RECOVERmode"))
                    new DialogSetPoint(parent, origintag, device, dataElement, "<html>AUTO=0,<br/>MANUAL=1", true);
                 else if (origintag.contains("CTRLOPmode") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "<html>Manual=0, Auto=1,<br/>Remote=2, Auto/Remote=3</html>", true);
                 
              }
           }
        }
    }
}
