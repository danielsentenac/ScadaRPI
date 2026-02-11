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

public class GlgRGA3Gui extends GlgChildGui {

    private static final long serialVersionUID = 364054054075L;
    private static final Logger logger = Logger.getLogger("Main");
    
    public GlgRGA3Gui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {
       
       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("RGA3.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean ); 
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );
          glg_bean.SetSTag("title", origin, true);

          // Create STATUS map origin dependent feature
          createStatusMap(STATUS);
       
          // Create CMD map origin dependent feature
          //createCommandMap(CMD);
       } 
      catch (Exception e) {
          logger.log(Level.SEVERE, e.getMessage());
          e.printStackTrace();
      }
    }

    protected void createStatusMap(Hashtable < String, String > STATUS) {

       // RGA Gui case
       STATUS.clear();
       String channelName =  TUBE1500WSTATUS.get("RGAGa3");
       // Get corresponding device from deviceManager
       String deviceName = channelName.split("_",2)[0];
       logger.finer("ORIGIN = " + origin);
       for ( int j = 0 ; j < 199 ; j++) {// RGA data (plot)
          int index = j + 1;
          glg_bean.SetSResource("/RGA/DataGroup/DataSample" + j + "/TooltipLabel", 
                                "Mass:" + Integer.toString(index) ); // Set tooltip label
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
            if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("START")) {
               new DialogStart(parent, "Start RGA scan...", true);
            }
            
            if ((action.equals("MouseClick") || action.equals("Activate")) && origintag.equals("STOP")) {
               new DialogStop(parent, "Stop RGA scan...", true);
            }
        }
        
        
    }
}
