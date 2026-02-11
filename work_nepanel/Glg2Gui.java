/*
 * This Class is used for 2 GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public class Glg2Gui extends GlgChildGui {

    private static final long serialVersionUID = 354054054071L;
    private static final Logger logger = Logger.getLogger("Main");

    public Glg2Gui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("PANEL2.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("title", title, true);

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

       // 2 Gui case
       STATUS.clear();
       STATUS.putAll(PANEL2STATUS);
    }
    public void updateGuiFeatures(String glgName, DataElement dataElement) {

        try {
            logger.finer("Glg2Gui:updateGui> Treating  glgName=" + glgName + " dataElement.value=" + dataElement.value);
            if (!glgName.contains("sub")) {
               if ( glgName.contains("Running")  )// Instrument Running flag
                  glg_bean.SetGTag(glgName, PCounterMobileColorSTATUS.get((int)dataElement.value), true);
               if ( glgName.contains("Error") )// Instrument Error flag
                  glg_bean.SetGTag(glgName, PCounterColorINSTRSTATUS.get((int)dataElement.value), true);
               if ( glgName.contains("Com") )  // Instrument Com/RackStatus flag
                  glg_bean.SetGTag(glgName, PCounterColorCOMSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Status") )  // Instrument Channel flag
                  glg_bean.SetGTag(glgName, PCounterMobileColorFLAGSTATUS.get((int)dataElement.value), true);
            }
            if ( glgName.contains("Val") && // Val is a tag for object value (double type) property
                !glgName.contains("sub")) {  // sub is reserved for childGui only
               glg_bean.SetDTag(glgName, dataElement.value, true);
               logger.finer("Glg2Gui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
                 dataElement.name + " type=" + dataElement.type + " value=" + 
                 dataElement.value);
            }
         }
         catch (Exception ex) {
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
           //logger.finer("Glg2Gui>Origin=" + origintag + " Format=" + format + " Action=" 
           //              + action + " subAction=" + subaction);
           
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
