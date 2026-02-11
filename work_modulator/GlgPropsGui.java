/*
 * This Class is used for Properties GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class GlgPropsGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054075L;
    private static final Logger logger = Logger.getLogger("Main");

    public GlgPropsGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("Props.g");
          // Add glg_bean component to a frame
	        content.add( glg_bean ); 
          // Create STATUS map origin dependent feature
          createStatusMap(STATUS);
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("title", title, true);
       } 
      catch (Exception e) {
          logger.log(Level.SEVERE, e.getMessage());
          e.printStackTrace();
      }
    }
    
    protected void createStatusMap(Hashtable < String, String > _STATUS) {

       for (Map.Entry<String, String> e : STATIONSTATUS.entrySet()) {
          String glgName =  e.getKey();
          String dataName = e.getValue();
          if (glgName.contains("sub"))
             STATUS.put(glgName,dataName);
       }
       logger.finer("GlgPropsGui:createStatusMap> STATUS=" + STATUS.toString());
    }

    public void updateGuiFeatures(String glgName, DataElement dataElement) {
       // Treat GUI relevant devices data
       try {
          if ( glgName.contains("Val") && // Val is a tag for object value (double type) property
               glgName.contains("sub")) {   // sub is reserved for childGui only
             logger.finer("GlgPropsGui:updateGuiFeatures> UPDATE " + dataElement.name + " with value " + dataElement.value);
             if (glgName.contains("Temp") || 
                 glgName.contains("Fan") ||
                 glgName.contains("ECO") ||
                 glgName.contains("ONOFF") ||
                 glgName.contains("Com"))
                glg_bean.SetDTag(glgName, dataElement.value, true);
             else if (glgName.contains("MODE")) {
                if (dataElement.value == 1) // Heat mode
                   glg_bean.SetDTag(glgName, 0, true);
                else if (dataElement.value == 4)  // Cool mode
                   glg_bean.SetDTag(glgName, 1, true);
             }
          }
       }
       catch (Exception ex) {
          logger.log(Level.WARNING,"GlgPropsGui:updateGuiFeatures> CANNOT UPDATE " + dataElement);
          ex.printStackTrace();
       }
    }
  
    public class InputListener implements GlgInputListener { 
	     public void InputCallback( GlgObject viewport, GlgObject message_obj )  {
          String origintag, format, action, subaction;
          origintag = message_obj.GetSResource( "Origin" );
	        format = message_obj.GetSResource( "Format" );
	        action = message_obj.GetSResource( "Action" );
	        subaction = message_obj.GetSResource( "SubAction" );
          logger.finer("GlgPropsGui::InputListener> Origin=" + origintag + " Format=" + format + 
                      " Action=" + action + " subAction=" + subaction);
           
          // Clicked on BACK button
          if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("BACK")) {
             parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
             parent.toFront();
             parent.isSuspended = false;
             child.isSuspended = true;
          }
       }
    }
}