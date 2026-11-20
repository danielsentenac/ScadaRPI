/*
 * This Class is used for the MKS Rotor Gauge GUI using Glg Toolkit.
 * It reuses the generic gauge drawing (Gauge.g); the rotor gauge has no
 * ON/OFF command, so the ON/OFF buttons of the drawing have no effect.
 */

import com.genlogic.*;
import java.util.*;

import java.util.logging.Logger;

public class GlgROTGaugeGui extends GlgGaugeGui {

    private static final long serialVersionUID = 354054054090L;
    private static final Logger logger = Logger.getLogger("Main");

    // Rotor gauge reading status (ROT_PR1ST)
    private static final Hashtable<Integer, String> ROTGaugeReadingSTATUS = new Hashtable<Integer, String>(){
    private static final long serialVersionUID = 354054054091L;
    {
       put(0,"- no reading");
       put(1,"- measuring");
       put(255,"- ?");
    }};

    // Rotor gauge communication status (ROT_GA5COMST)
    private static final Hashtable<Integer, String> ROTGaugeComSTATUS = new Hashtable<Integer, String>(){
    private static final long serialVersionUID = 354054054092L;
    {
       put(0,"Communication Ok");
       put(1,"Communication Fail");
       put(255,"?");
    }};

    public GlgROTGaugeGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {
       super(_parent, _deviceManager, _title, _origin);
       // Show the sensor name as on the main panel (ROTGa5 -> Ga5)
       glg_bean.SetSTag("Gename", origin.replaceFirst("^ROT", ""), true);
    }

    protected void createStatusMap(Hashtable < String, String > STATUS) {

       // Rotor Gauge Gui case
       String statusName = TUBE1500WSTATUS.get(origin);           // ROT_PR1ST
       String deviceName = statusName.split("_",2)[0];
       String comName = deviceName + "_GA5COMST";
       STATUS.clear();
       STATUS.put("Gename", statusName);
       STATUS.put("Gestatus", statusName);
       STATUS.put("GePstatus", comName);
       STATUS.put("GeVal", TUBE1500WSTATUS.get(origin + "Val"));  // ROT_PR1
       STATUS.put("GeCol1", statusName);
       STATUS.put("GePCol1", comName);
       STATUS.put("GeCol2", statusName);
       STATUS.put("GeStr", statusName);
       STATUS.put("GePStr", comName);
    }

    public void updateGuiFeatures(String glgName, DataElement dataElement) {

        // Rotor Gauge Gui case ("P" tags show the communication status)
        if ( glgName.contains("Col") && !glgName.contains("P") )
            glg_bean.SetGTag(glgName, ROTGaugeEmissionColorSTATUS.get((int)dataElement.value), true);
        if ( glgName.contains("Str") && !glgName.contains("P") )
            glg_bean.SetSTag(glgName, ROTGaugeReadingSTATUS.get((int)dataElement.value), true);
        if ( glgName.contains("Col") && glgName.contains("P") )
            glg_bean.SetGTag(glgName, OkFailColorSTATUS.get((int)dataElement.value), true);
        if ( glgName.contains("Str") && glgName.contains("P") )
            glg_bean.SetSTag(glgName, ROTGaugeComSTATUS.get((int)dataElement.value), true);
        if ( glgName.contains("Val") )
            glg_bean.SetDTag(glgName, dataElement.value, true);
    }
}
