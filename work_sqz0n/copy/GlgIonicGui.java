/*
 * This Class is used for Ionic Pump GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgIonicGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054075L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgIonicGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("Ionic.g");
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

       // Ionic Gui case
       STATUS.clear();
       STATUS.put("Pname", SQZSTATUS.get(origin));
       STATUS.put("P", SQZSTATUS.get(origin));
       STATUS.put("PCol", SQZSTATUS.get(origin));
       STATUS.put("PStr", SQZSTATUS.get(origin));
       STATUS.put("PREmode", SQZSTATUS.get(origin + "REmodesub"));
       STATUS.put("PREmodeCol", SQZSTATUS.get(origin + "REmodesub"));
       STATUS.put("PREmodeStr", SQZSTATUS.get(origin + "REmodesub"));
       STATUS.put("POPmode", SQZSTATUS.get(origin + "OPmodesub"));
       STATUS.put("POPmodeCol", SQZSTATUS.get(origin + "OPmodesub"));
       STATUS.put("POPmodeStr", SQZSTATUS.get(origin + "OPmodesub"));
       STATUS.put("PVmode", SQZSTATUS.get(origin + "Vmodesub"));
       STATUS.put("PVmodeCol", SQZSTATUS.get(origin + "Vmodesub"));
       STATUS.put("PVmodeStr", SQZSTATUS.get(origin + "Vmodesub"));
       STATUS.put("PAcurrentVal", SQZSTATUS.get(origin + "AcurrentValsub"));
       STATUS.put("PcurrentVal", SQZSTATUS.get(origin + "PcurrentValsub"));
       STATUS.put("PAvoltageVal", SQZSTATUS.get(origin + "AvoltageValsub"));
       STATUS.put("PressureVal", SQZSTATUS.get(origin + "PressureValsub"));
       STATUS.put("PMcurrentVal", SQZSTATUS.get(origin + "McurrentValsub"));
       STATUS.put("PMvoltageVal", SQZSTATUS.get(origin + "MvoltageValsub")); 
       STATUS.put("PMpowerVal", SQZSTATUS.get(origin + "MpowerValsub"));
       STATUS.put("PFvoltageVal", SQZSTATUS.get(origin + "FvoltageValsub"));
       STATUS.put("PSvoltageVal", SQZSTATUS.get(origin + "SvoltageValsub"));
       STATUS.put("PFcurrentVal", SQZSTATUS.get(origin + "FcurrentValsub"));
       STATUS.put("PScurrentVal", SQZSTATUS.get(origin + "ScurrentValsub"));
    }

    public void updateGuiFeatures(String origintag, DataElement dataElement) {

        // Ionic Pump Gui case
        //logger.finer("GlgIonicGui:updateGuiFeatures> UPDATE " + dataElement.name + "(" + origintag + ") with value=" 
        //              + dataElement.value);
        if ( origintag.equals("PCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, IonicONOFFColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  IonicONOFFSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PREmodeCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, IonicRemoteColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PREmodeStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  IonicRemoteSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("POPmodeCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, IonicOperatingModeColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("POPmodeStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  IonicOperatingModeSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PVmodeCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(origintag, IonicVoltageModeColorSTATUS.get((int)dataElement.value), true);
        }
        if ( origintag.equals("PVmodeStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(origintag,  IonicVoltageModeSTATUS.get((int)dataElement.value), true);
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
           logger.finer("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           
           // Clicked on MAIN button
           if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("GLOBAL")) {
                parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                parent.toFront();
                parent.isSuspended = false;
                child.isSuspended = true;
            }
           // Ionic ON/OFF command
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
                 parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                 parent.toFront();
                 parent.isSuspended = false;
                 child.isSuspended = true;
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
                 parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                 parent.toFront();
                 parent.isSuspended = false;
                 child.isSuspended = true;
              }
           }
           else if (action.equals("MouseClick") && origintag.contains("Val") || origintag.contains("mode")) { // SetPoint fields
              System.out.println("GlgIonicGui::InputListener> Open Dialog command for " + origin + "," + origintag );
              String cmdName =  STATUS.get(origintag);
              System.out.println("GlgIonicGui::InputListener> Open Dialog command for " + cmdName );
              String deviceName = cmdName.split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(cmdName.split("_")[1]);
              if (dataElement != null) {
                 if (origintag.contains("Pcurrent") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Protect Current",true);
                 else if (origintag.contains("Mcurrent") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Max Current",true);
                 else if (origintag.contains("Fcurrent") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "First Step Current",true);
                 else if (origintag.contains("Fcurrent") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "First Step Current",true);
                 else if (origintag.contains("Scurrent") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Second Step Current",true);
                 else if (origintag.contains("Mvoltage") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Max Voltage",true);
                 else if (origintag.contains("Fvoltage") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "First Step Voltage",true);
                 else if (origintag.contains("Svoltage") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Second Step Voltage",true);
                 else if (origintag.contains("Mpower") )
                    new DialogSetPoint(parent, origintag, device, dataElement, "Max Power",true);
                 else if (origintag.contains("REmode") ) 
                    new DialogSetPoint(parent, origintag, device, dataElement, "Local=0, Remote=1, Serial=2", true);
                 else if (origintag.contains("OPmode") ) 
                    new DialogSetPoint(parent, origintag, device, dataElement, "Start=1, Protect=2", true);
                 else if (origintag.contains("Vmode") ) 
                    new DialogSetPoint(parent, origintag, device, dataElement, "Fixed=1, Step=2", true);
              }
           }
        }
    }
}
