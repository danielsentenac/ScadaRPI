/*
 * This Class is used for the Leybold LEYSPEC RGA GUI using Glg Toolkit
 * (drawing RGALeyspec.g : scan START/STOP, filament ON/OFF, detector
 * SEM/FARADAY and online histogram plot of the step scan).
 */

import com.genlogic.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;

public class GlgRGALeyspecGui extends GlgChildGui {

    private static final long serialVersionUID = 354054054095L;
    private static final Logger logger = Logger.getLogger("Main");
    private static final int NB_SAMPLES = RgaLeyspec.MAX_MASS; // Number of bars of the plot (mass 1 to 100; DataGroup/Factor in RGALeyspec.g)

    public GlgRGALeyspecGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
    }

    protected void createAndShowGui () {

       try {
          // Attach Bean to Frame
          glg_bean = new GlgJLWBean();
          glg_bean.SetDrawingFile("RGALeyspec.g");
          // Add glg_bean component to a frame
	  content.add( glg_bean );
          glg_bean.AddListener( GlgObject.INPUT_CB, new InputListener() );

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

       // RGA Gui case
       STATUS.clear();
       STATUS.put("RGAemission", TUBE1500WSTATUS.get(origin + "emissionsub"));
       STATUS.put("RGAemissionCol", TUBE1500WSTATUS.get(origin + "emissionsub"));
       STATUS.put("RGAemissionStr", TUBE1500WSTATUS.get(origin + "emissionsub"));
       STATUS.put("RGArunning", TUBE1500WSTATUS.get(origin + "runningsub"));
       STATUS.put("RGArunningCol", TUBE1500WSTATUS.get(origin + "runningsub"));
       STATUS.put("RGArunningStr", TUBE1500WSTATUS.get(origin + "runningsub"));
       STATUS.put("RGAmode", TUBE1500WSTATUS.get(origin + "modesub"));
       STATUS.put("RGAmodeCol", TUBE1500WSTATUS.get(origin + "modesub"));
       STATUS.put("RGAmodeStr", TUBE1500WSTATUS.get(origin + "modesub"));
       STATUS.put("RGAfilament", TUBE1500WSTATUS.get(origin + "filamentsub"));
       STATUS.put("RGAfilamentCol", TUBE1500WSTATUS.get(origin + "filamentsub"));
       STATUS.put("RGAfilamentStr", TUBE1500WSTATUS.get(origin + "filamentsub"));
       STATUS.put("RGAtp", TUBE1500WSTATUS.get(origin + "tpsub"));
       STATUS.put("RGAtpStr", TUBE1500WSTATUS.get(origin + "tpsub"));
       String channelName =  STATUS.get("RGAemission");
       // Get corresponding device from deviceManager
       String deviceName = channelName.split("_",2)[0];
       for ( int j = 0 ; j < NB_SAMPLES ; j++) {// RGA data (plot)
          int mass = j + 1;
          glg_bean.SetSResource("/RGA/DataGroup/DataSample" + j + "/TooltipLabel",
                                "Mass:" + Integer.toString(mass) ); // Set tooltip label
          STATUS.put("/RGA/DataGroup/DataSample" + j + "/Value", deviceName + "_" + RgaLeyspec.massName(mass));
       }
    }

    public void updateGuiFeatures(String glgName, DataElement dataElement) {

        // RGA Gui case
        int status = (int)dataElement.value;
        if ( glgName.contains("emissionCol") )   // Col is a tag for object status (short type) color property
            glg_bean.SetGTag(glgName, RGAFilamentColorSTATUS.get(status), true);
        if ( glgName.contains("emissionStr") )   // Str is a tag for object status (short type) string property
            glg_bean.SetSTag(glgName,  RGAEmissionONOFFSTATUS.get(status), true);
        if ( glgName.contains("runningCol") )
            glg_bean.SetGTag(glgName, RGAFilamentColorSTATUS.get(status), true);
        if ( glgName.contains("runningStr") )
            glg_bean.SetSTag(glgName,  RGARunningONOFFSTATUS.get(status), true);
        if ( glgName.contains("modeCol") )
            glg_bean.SetGTag(glgName, RGAONOFFColorSTATUS.get(status), true);
        if ( glgName.contains("modeStr") )
            glg_bean.SetSTag(glgName,  RGAMODESTATUS.get(status), true);
        if ( glgName.contains("filamentCol") )
            glg_bean.SetGTag(glgName, RGAFilamentColorSTATUS.get(status), true);
        if ( glgName.contains("filamentStr") )
            glg_bean.SetSTag(glgName,  RGAFilamentSTATUS.get(status), true);
        if ( glgName.contains("tpStr") )         // Total pressure
            glg_bean.SetSTag(glgName, dataElement.value > 0 ? String.format("Total: %.2E mbar", dataElement.value)
                                                            : "Total: -", true);
        if ( glgName.contains("DataSample") ) { // DataSample is a tag for plot object value (double type) property
           if ( dataElement.value > 0 ) { // Log plot RGA
              glg_bean.SetDResource(glgName, Math.log10(dataElement.value), true);
              glg_bean.SetDResource(glgName.replace("Value","TooltipValue"),dataElement.value);// Set tooltip value
           }
           else
              glg_bean.SetDResource(glgName, Math.log10(0), true);
        }
    }

    // Queue a trigger command (1 = ON, 2 = OFF) for the command channel "origin + key"
    private void sendTrigger(String key, int value) {
       String cmdName =  CMD.get(origin + key);
       if ( cmdName == null ) return;
       // Get corresponding device from deviceManager
       String deviceName = cmdName.split("_",2)[0];
       Device device = deviceManager.getDevice(deviceName);
       // Update dataElement
       if ( device != null ) {
          DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
          if ( dataElement != null ) {
             dataElement.setvalue = value;
             device.commandSetQueue.add(dataElement); // Set internal data command trigger
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
           else if ( !action.equals("MouseClick") )
              return;
           // Filament ON/OFF command
           else if ( origintag.equals("RGAemissionON") )
              sendTrigger("emission", 1);
           else if ( origintag.equals("RGAemissionOFF") )
              sendTrigger("emission", 2);
           // Scan START/STOP command
           else if ( origintag.equals("RGArunningON") )
              sendTrigger("running", 1);
           else if ( origintag.equals("RGArunningOFF") )
              sendTrigger("running", 2);
           // Detector SEM/FARADAY command
           else if ( origintag.equals("RGASEMON") )
              sendTrigger("SEM", 1);
           else if ( origintag.equals("RGASEMOFF") )
              sendTrigger("SEM", 2);
           // Filament selection field
           else if ( origintag.equals("RGAfilament") ) {
              String cmdName =  STATUS.get(origintag);
              Device device = deviceManager.getDevice(cmdName.split("_",2)[0]);
              if ( device != null ) {
                 DataElement dataElement = device.getDataElement(cmdName.split("_",2)[1]);
                 if (dataElement != null)
                    new DialogSetPoint(parent, origintag, device, dataElement, "Select filament (1 or 2), filament must be OFF", true);
              }
           }
        }
    }
}
