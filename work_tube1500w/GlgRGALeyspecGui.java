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
       // Total pressure : the tube gauge G21 (the total pressure gauge of the RGA does not go below 1E-7 mbar)
       STATUS.put("RGAtp", G21_CHANNEL);
       STATUS.put("RGAtpStr", G21_CHANNEL);
       STATUS.put("RGAtpCol", G21_CHANNEL);
       // Plot unit switch (click) : ion current or partial pressure
       STATUS.put("RGAunit", TUBE1500WSTATUS.get(origin + "runningsub"));
       STATUS.put("RGAunitStr", TUBE1500WSTATUS.get(origin + "runningsub"));
       STATUS.put("RGAunitCol", TUBE1500WSTATUS.get(origin + "runningsub"));
       STATUS.put("RGAsemvoltStr", TUBE1500WSTATUS.get(origin + "semvoltsub"));
       STATUS.put("RGAspeedStr", TUBE1500WSTATUS.get(origin + "speedsub"));
       STATUS.put("RGAsemvolt", TUBE1500WSTATUS.get(origin + "semvoltsub"));  // Object name : full visibility
       STATUS.put("RGAspeed", TUBE1500WSTATUS.get(origin + "speedsub"));
       STATUS.put("RGAsemvoltCol", TUBE1500WSTATUS.get(origin + "semvoltsub"));
       STATUS.put("RGAspeedCol", TUBE1500WSTATUS.get(origin + "speedsub"));
       String channelName =  STATUS.get("RGAemission");
       // Get corresponding device from deviceManager
       String deviceName = channelName.split("_",2)[0];
       glg_bean.SetSResource("/RGA/YAxisLabel/String", "Ion current (A)");
       glg_bean.SetSTag("RGAunitStr", "Plot: ion current (A)", true);
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
            glg_bean.SetSTag(glgName, dataElement.value > 0 ? String.format("G21: %.2E mbar", dataElement.value)
                                                            : "G21: -", true);
        if ( glgName.contains("unitStr") )       // Plot unit (click to switch)
            glg_bean.SetSTag(glgName, !pressureMode ? "Plot: ion current (A)"
                                      : totalPressure() > 0 ? "Plot: partial pressure" : "Plot: no G21 value", true);
        if ( glgName.contains("unitCol") )
            glg_bean.SetGTag(glgName, new GlgPoint(1., 1., 1.), true);
        if ( glgName.contains("semvoltStr") )    // SEM high voltage setting (click to change)
            glg_bean.SetSTag(glgName, status > 0 ? "SEM: " + status + " V" : "SEM: -", true);
        if ( glgName.contains("speedStr") )      // Measurement speed setting (click to change)
            glg_bean.SetSTag(glgName, status >= 1 && status <= 6 ? "Speed: " + SPEED_MS[status - 1] + " ms/mass" : "Speed: -", true);
        if ( glgName.contains("semvoltCol") || glgName.contains("speedCol") ) // Editable fields : white
            glg_bean.SetGTag(glgName, new GlgPoint(1., 1., 1.), true);
        if ( glgName.contains("tpCol") )         // Total pressure : plain dark text, as the labels
            glg_bean.SetGTag(glgName, new GlgPoint(0., 0., 0.23), true);
        if ( glgName.contains("DataSample") ) { // DataSample is a tag for plot object value (double type) property
           double shown = plotValue(dataElement.value);
           adjustYAxis(shown);
           if ( shown > 0 ) { // Log plot RGA
              glg_bean.SetDResource(glgName, Math.log10(shown), true);
              glg_bean.SetDResource(glgName.replace("Value","TooltipValue"), shown);// Set tooltip value
           }
           else
              glg_bean.SetDResource(glgName, Math.log10(0), true);
        }
    }

    // Plot unit : ion current (A) as measured, or partial pressure (mbar) = current / sum of the
    // currents x total pressure given by G21 (method of the LEYSPEC manual, paragraph 9.6; it
    // ignores the different sensitivity of each gas).
    private static final String G21_CHANNEL = "MG_PR2";
    private boolean pressureMode = false;
    private double passSum = 0, currentSum = 0; // Sum of the ion currents : being built / of the last pass

    private double totalPressure() {
       Device mg = deviceManager.getDevice(G21_CHANNEL.split("_",2)[0]);
       DataElement p = (mg == null) ? null : mg.getDataElement(G21_CHANNEL.split("_",2)[1]);
       return (p == null) ? 0 : p.value;
    }

    private double plotValue(double current) {
       if ( current > 0 ) passSum += current;
       if ( !pressureMode ) return current;
       double total = totalPressure();
       return (total > 0 && currentSum > 0) ? current / currentSum * total : 0;
    }

    private void switchUnit() {
       pressureMode = !pressureMode;
       glg_bean.SetSResource("/RGA/YAxisLabel/String", pressureMode ? "Partial pressure (mbar)" : "Ion current (A)");
       glg_bean.SetSTag("RGAunitStr", pressureMode ? "Plot: partial pressure" : "Plot: ion current (A)", true);
    }

    // Y axis of the plot : DECADES decades, starting one decade below the smallest value measured
    // (the drawing has 7 decade labels; the range moves, the number of decades does not)
    private static final int DECADES = 7, DEFAULT_LOW = -14;
    private int samplesSeen = 0, axisLow = DEFAULT_LOW;
    private double scanMin = Double.MAX_VALUE;

    // Called for each mass of the plot; the axis is adjusted once per pass over all the masses
    private void adjustYAxis(double value) {
       if ( value > 0 && value < scanMin ) scanMin = value;
       if ( ++samplesSeen < NB_SAMPLES ) return;
       currentSum = passSum; // All the masses seen : sum used for the partial pressures of the next pass
       passSum = 0;
       int low = (scanMin == Double.MAX_VALUE) ? axisLow    // Nothing measured : keep the axis
                 : Math.max(-16, Math.min((int) Math.floor(Math.log10(scanMin)) - 1, -6));
       samplesSeen = 0;
       scanMin = Double.MAX_VALUE;
       if ( low == axisLow ) return;
       axisLow = low;
       glg_bean.SetDResource("RGA/Low", low);
       glg_bean.SetDResource("RGA/High", low + DECADES);
       for (int i = 0; i <= DECADES; i++)
          glg_bean.SetSResource("RGA/YLabelGroup/YLabel" + i + "/String", String.format("1e%03d", low + i));
       glg_bean.Update();
    }

    private static final int[] SPEED_MS = { 50, 100, 200, 500, 1000, 2000 };

    private void openSetPoint(String statusKey, String title) {
       String channelName = STATUS.get(statusKey);
       if ( channelName == null ) return;
       Device device = deviceManager.getDevice(channelName.split("_",2)[0]);
       if ( device == null ) return;
       DataElement dataElement = device.getDataElement(channelName.split("_",2)[1]);
       if ( dataElement != null )
          new DialogSetPoint(parent, statusKey, device, dataElement, title, true);
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
           else if ( origintag.equals("RGAunit") )
              switchUnit();
           else if ( origintag.equals("RGAsemvolt") )
              openSetPoint("RGAsemvoltStr", "SEM high voltage in V (1000 to 3000)");
           else if ( origintag.equals("RGAspeed") )
              openSetPoint("RGAspeedStr", "Speed in ms/mass (50, 100, 200, 500, 1000 or 2000)");
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
