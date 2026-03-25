/*
 * This Class is used for Valve GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgValveGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054071L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgValveGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("Valve.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );

          glg_bean.SetSTag("Vname", origin, true);

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
       CMD.putAll(HIDENBENCHCMD);
    }

    protected void createStatusMap(Hashtable < String, String > STATUS) {

       // Valve Gui case
       STATUS.clear();
       STATUS.put("Vname", HIDENBENCHSTATUS.get(origin));
       STATUS.put("Vstatus", HIDENBENCHSTATUS.get(origin));
       STATUS.put("VCol", HIDENBENCHSTATUS.get(origin + "Col"));
       STATUS.put("VStr", HIDENBENCHSTATUS.get(origin));

    }
    protected void updateGuiFeatures(String glgName, DataElement dataElement) {
       
       // Valve Gui case
        if ( glgName.contains("Col") ) { // Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, ValveColorSTATUS.get((int)dataElement.value), true);
            //logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
        }
        if ( glgName.contains("Str") ) {  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName,  ValveSTATUS.get((int)dataElement.value), true);
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
           //logger.finer("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           
           // Clicked on MAIN button
           if ((action.equals("MouseClick") || action.equals("Activate") )&& origintag.equals("GLOBAL")) {
                parent.toFront();
                parent.isSuspended = false;
                child.isSuspended = true;
            }
           // Valve command
           else if (action.equals("MouseClick")) {
              String cmdName =  CMD.get(origin);
              if ( cmdName == null ) return; 
              // Get corresponding device from deviceManager
              String deviceName = cmdName.split("_",2)[0];
              Device device = deviceManager.getDevice(deviceName);
              if (origintag.contains("Open")) {     
                 // Update dataElement
                 if ( device != null ) {
                    logger.finer("GlgValveGui:InputListener> Setting valve Open");
                    DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                    dataElement.setvalue = 1;
                    device.commandSetQueue.add(dataElement); // Set OPEN internal data command trigger
                    parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    parent.toFront();
                    parent.isSuspended = false;
                    child.isSuspended = true;
                 }
              }
              else if (origintag.contains("Close")) {
                 // Update dataElement
                 if ( device != null ) {
                    logger.finer("GlgValveGui:InputListener> Setting valve Close");
                    DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                    dataElement.setvalue = 2;
                    device.commandSetQueue.add(dataElement); // Set CLOSE internal data command trigger
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
