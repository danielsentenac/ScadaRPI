/*
 * This Class is used for SqzTemp GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgSqzTempGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054072L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgSqzTempGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          // SqzTemp display
          glg_bean.SetDrawingFile("SqzTemp.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("title", title + " TEMPERATURE SENSORS", true);

          // Create STATUS map origin dependent feature
          createStatusMap(STATUS);
       
          // Create CMD map origin dependent feature
          createCommandMap(CMD);
       } 
      catch (Exception e) {
          logger.log(Level.SEVERE, e.getMessage());
      }
    }

    public void createCommandMap(Hashtable < String, String > CMD) {

       CMD.clear();
       CMD.putAll(SQZCMD);
    }

    public void createStatusMap(Hashtable < String, String > STATUS) {

       // SqzTemp Gui case
       STATUS.clear();
       STATUS.putAll(SqzTemp);
    }

    protected void updateGuiFeatures(String glgName, DataElement dataElement) {
       //logger.info("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
       //   dataElement.name + " type=" + dataElement.type + " value=" + dataElement.value);
       // SqzTemp Gui case
       if ( glgName.contains("Val")) {  // Val is a tag for object value (double type) property
          glg_bean.SetDTag(glgName, dataElement.value, true);
          //logger.info("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
          //dataElement.name + " type=" + dataElement.type + " value=" + dataElement.value);
       }
       else { // All other cases
          if ( glgName.contains("Col") )// Col is a tag for object status (short type) color property
             glg_bean.SetGTag(glgName, OkFailColorSTATUS.get((int)dataElement.value), true);
             //logger.finer("GlgChildGui:updateGui> OkFailColorSTATUS=" + dataElement.value);
          if ( glgName.contains("Str") )  // Col is a tag for object status (short type) string property
             glg_bean.SetSTag(glgName, glgName.replace("Str", "") +" "+ OkFailSTATUS.get((int)dataElement.value), true);
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
           logger.finer("Origin=" + origintag + " Format=" + format + " Action=" + action + " subAction=" + subaction);
           
           // Clicked on MAIN button
           if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("GLOBAL")) {
                parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                parent.toFront();
                parent.isSuspended = false;
                child.isSuspended = true;
           }
        }
    }
}
