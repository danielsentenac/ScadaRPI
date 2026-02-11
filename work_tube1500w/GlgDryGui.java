/*
 * This Class is used for Dry Pump GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgDryGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054080L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgDryGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("Dry.g");
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

       // Dry Gui case
       STATUS.clear();
       STATUS.put("Pname", TUBE1500WSTATUS.get(origin));
       STATUS.put("P", TUBE1500WSTATUS.get(origin));
       STATUS.put("PCol", TUBE1500WSTATUS.get(origin + "Col"));
       STATUS.put("PStr", TUBE1500WSTATUS.get(origin));
    }

    public void updateGuiFeatures(String glgName, DataElement dataElement) {

        // Dry Pump Gui case
        //logger.finer("GlgTurboGui:updateGuiFeatures> UPDATE " + dataElement.name + "(" + glgName + ") with value=" 
        //              + dataElement.value);
        if ( glgName.equals("PCol") ) {  // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, RelayGeneralColorSTATUS.get((int)dataElement.value), true);
           
        }
        if ( glgName.equals("PStr") ) { // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  RelayGeneralSTATUS.get((int)dataElement.value), true);
           
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
           //logger.finer("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           
           // Clicked on MAIN button
           if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("GLOBAL")) {
                parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                parent.toFront();
                parent.isSuspended = false;
                child.isSuspended = true;
            }
           // Dry Pump ON/OFF command
           else if (action.equals("MouseClick") ) {
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
                    logger.finer("GlgDryGui:InputListener> set " + dataElement.name + " value to 1");
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
