/*
 * This Class is used for FlowMeter GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgFlowMeterGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054071L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgFlowMeterGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("FLOWMETER.g");
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
      }
    }

    protected void createCommandMap(Hashtable < String, String > CMD) {

       CMD.clear();
       CMD.put("START_RAMP", origin+VENTINGCMD.get("START_RAMP"));
       CMD.put("STOP_RAMP", origin+VENTINGCMD.get("STOP_RAMP"));       
    }

    protected void createStatusMap(Hashtable < String, String > STATUS) {

       // FlowMeter Gui case
       STATUS.clear();
       
       STATUS.put("FlowVal", origin+VENTINGSTATUS.get("FlowVal"));
       STATUS.put("TempVal", origin+VENTINGSTATUS.get("TempVal"));
       STATUS.put("FlowSetPVal", origin+VENTINGSTATUS.get("FlowSetPVal"));
       STATUS.put("ValveposVal", origin+VENTINGSTATUS.get("ValveposVal"));
       STATUS.put("RampVal", origin+VENTINGSTATUS.get("RampVal"));
       STATUS.put("ValveOpenVal", origin+VENTINGSTATUS.get("ValveOpenVal"));
       STATUS.put("ValveCloseVal", origin+VENTINGSTATUS.get("ValveCloseVal"));
       STATUS.put("FlowZero", origin+VENTINGSTATUS.get("FlowZero"));
       // Operation display values
       STATUS.put("FlowStepVal", origin+VENTINGSTATUS.get("FlowStepVal"));
       STATUS.put("FlowMaxVal", origin+VENTINGSTATUS.get("FlowMaxVal"));
       STATUS.put("TimeInterVal", origin+VENTINGSTATUS.get("TimeInterVal"));
       STATUS.put("TimeVal", origin+VENTINGSTATUS.get("TimeVal"));
      

    }
    protected void updateGuiFeatures(String glgName, DataElement dataElement) {
    
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
           //logger.finer("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           //System.out.println("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           // Clicked on MAIN button
           if ((action.equals("MouseClick") || action.equals("Activate") )&& origintag.equals("GLOBAL")) {
                parent.toFront();
                parent.isSuspended = false;
                child.isSuspended = true;
            }
            // FlowMeter command
            else if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.contains("FlowSetP")) {
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origin  );
              String deviceName = STATUS.get(origintag).split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(STATUS.get(origintag).split("_",2)[1]);
              if ( dataElement != null ) {
                 logger.fine("Open DialogSetPoint for device = " + device.name + " DataElement = " + dataElement.name);
                 new DialogSetPoint(parent, origintag, device, dataElement, "Set Flow (sccm)", true); 
              }
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.contains("Ramp")) {
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origintag  );
              String deviceName = STATUS.get(origintag).split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(STATUS.get(origintag).split("_")[1]);
              if ( dataElement != null ) {
                 new DialogSetPoint(parent, origintag, device, dataElement, "Set Ramp (msec) ", true); 
              }
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.contains("ValveOpen")) {
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origintag  );
              String deviceName = STATUS.get(origintag).split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(STATUS.get(origintag).split("_",2)[1]);
              if ( dataElement != null ) {
                 new DialogSetPoint(parent, origintag, device, dataElement, "Set Valve Open (0 or 1) ", true); 
              }
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.contains("ValveClose")) {
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origintag  );
              String deviceName = STATUS.get(origintag).split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(STATUS.get(origintag).split("_",2)[1]);
              if ( dataElement != null ) {
                 new DialogSetPoint(parent, origintag, device, dataElement, "Set Valve Close (0 or 1) ", true); 
              }
           }
           else if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.contains("FlowZero")) {
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origintag  );
              String deviceName = STATUS.get(origintag).split("_")[0];
              Device device = deviceManager.getDevice(deviceName);
              DataElement dataElement = device.getDataElement(STATUS.get(origintag).split("_",2)[1]);
              if ( dataElement != null ) {
                 new DialogSetPoint(parent, origintag, device, dataElement, "Set Flow Zero (0 or 1) ", true); 
              }
           }
           else if (action.equals("MouseClick") && (origintag.contains("FlowMax") || origintag.contains("FlowStep") || origintag.contains("TimeInter" )) ) { // Operation parameter fields
              System.out.println("GlgGui::InputListener> Open Dialog command for " + origintag );
              String cmdName =  STATUS.get(origintag);
              System.out.println("GlgGui::InputListener> Open Dialog command for " + cmdName );
              new DialogOp(parent, origintag.replace("Val",""), op, origin + ": Set " + origintag.replace("Val",""), true);
           }
           else if (action.equals("MouseClick") && (origintag.contains("START") || origintag.contains("STOP"))  ) { // Operation Ramp START/STOP commands 
              String cmdName =  CMD.get(origintag);
              String deviceName = cmdName.split("_",2)[0];
              logger.fine("GlgGui::InputListener> Operation Start/Stop command for " + origintag + " cmdName = " + cmdName + " deviceName = " + deviceName);
              if ( op != null) {
                 if ( origintag.contains("START")) 
                     op.setMode(OpMode.START);
                 else if ( origintag.contains("STOP"))
                     op.setMode(OpMode.STOP);
              }
           }
        }
    }
}
