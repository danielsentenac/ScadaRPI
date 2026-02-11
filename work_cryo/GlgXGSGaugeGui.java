/*
 * This Class is used for XGS Gauge GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgXGSGaugeGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054071L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgXGSGaugeGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("XGSGauge.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("XGSGename", origin, true);

          // Create STATUS map origin dependent feature
          createStatusMap(STATUS);
       
          // Create CMD map origin dependent feature
          createCommandMap(CMD);
       } 
      catch (Exception e) {
          logger.log(Level.SEVERE, e.getMessage());
      }
    }

    protected void createStatusMap(Hashtable < String, String > STATUS) {

       // Gauge Gui case
       STATUS.clear();
       STATUS.put("XGSGename", CRYOIBDETSTATUS.get(origin));
       STATUS.put("XGSGeEstatus", CRYOIBDETSTATUS.get(origin));
       STATUS.put("XGSGeDstatus", CRYOIBDETSTATUS.get(origin));
       STATUS.put("XGSGeVal", CRYOIBDETSTATUS.get(origin + "Val"));
       STATUS.put("XGSGeECol", CRYOIBDETSTATUS.get(origin + "EColsub"));
       STATUS.put("XGSGeEStr", CRYOIBDETSTATUS.get(origin + "Esub"));
       STATUS.put("XGSGeDCol", CRYOIBDETSTATUS.get(origin + "DColsub"));
       STATUS.put("XGSGeDStr", CRYOIBDETSTATUS.get(origin + "Dsub"));
       STATUS.put("XGSGeFCol", CRYOIBDETSTATUS.get(origin + "FColsub"));
       STATUS.put("XGSGeFStr", CRYOIBDETSTATUS.get(origin + "Fsub"));
    }
    public void updateGuiFeatures(String glgName, DataElement dataElement) {

        // XGS Gauge Gui case
        if (glgName.contains("E")) {
           if ( glgName.contains("Col")) { // Col is a tag for object status (short type) color property
              glg_bean.SetGTag(glgName, XGSGaugeEmissionColorSTATUS.get((int)dataElement.value), true);
              //logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
           }
           if ( glgName.contains("Str")) { // Str is a tag for object status (short type) string property
              glg_bean.SetSTag(glgName,  XGSGaugeEmissionSTATUS.get((int)dataElement.value), true);
              //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);
           }
        }
        if (glgName.contains("F")) {
           if ( glgName.contains("Col")) {  // Col is a tag for object status (short type) color property
              glg_bean.SetGTag(glgName, XGSGaugeFilamentColorSTATUS.get((int)dataElement.value), true);
              //logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
           }
           if ( glgName.contains("Str")) {  // Str is a tag for object status (short type) string property
              glg_bean.SetSTag(glgName,  XGSGaugeFilamentSTATUS.get((int)dataElement.value), true);
              //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);
           }
        }
        if (glgName.contains("D")) {
           if ( glgName.contains("Col")) {  // Col is a tag for object status (short type) color property
              glg_bean.SetGTag(glgName, XGSGaugeDegasColorSTATUS.get((int)dataElement.value), true);
              //logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
           }
           if ( glgName.contains("Str")) {  // Str is a tag for object status (short type) string property
              glg_bean.SetSTag(glgName,  XGSGaugeDegasSTATUS.get((int)dataElement.value), true);
              //logger.finer("GlgChildGui:updateGui> OkFailSTATUS=" + dataElement.value);
           }
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
           //logger.finer("GlgXGSGaugeGui>Origin=" + origintag + " Format=" + format + " Action=" 
           //              + action + " subAction=" + subaction);
           
           // Clicked on MAIN button
           if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("GLOBAL")) {
                parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                parent.toFront();
                parent.isSuspended = false;
                child.isSuspended = true;
           }
           // XGSGauge command
           else if (action.equals("MouseClick") ) {
              logger.finer("GlgXGSGaugeGui> Origin=" + origin + " Origintag=" + origintag + " Format=" + format + " Action="
                           + action + " subAction=" + subaction);
              String fulltag = origin;
              if (origintag.contains("Emult1"))
                 fulltag += "Emult1";
              if (origintag.contains("Emult2"))
                 fulltag += "Emult2";
              if (origintag.contains("Degas"))
                 fulltag += "Degas";
              String cmdName =  CMD.get(fulltag);
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              if ( device != null ) {
                 if (origintag.contains("ON")) {     
                 // Update dataElement
                    logger.finer("GlgXGSGaugeGui> set sensor " + cmdName + " ON");
                    DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                    dataElement.setvalue = 1;
                    device.commandSetQueue.add(dataElement); // Set ON internal data command trigger
                    parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    parent.toFront();
                    parent.isSuspended = false;
                    child.isSuspended = true;
                 }
                 else if (origintag.contains("OFF")) {
                 // Update dataElement
                    logger.finer("GlgXGSGaugeGui> set sensor " + cmdName + " OFF");
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
