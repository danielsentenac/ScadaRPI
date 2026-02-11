/*
 * This interface contains channel list correspondence between glg gui and device objects.
The glgName is the Key. 
The (full) DataElement name is the Value: 
It must be composed of the device name prefix + "_" + the DataElement name.
 *
 */

import java.util.Hashtable;
import com.genlogic.GlgPoint;


public interface ChannelList  {

String mainDrawing = "Station.g";

/***********************************************************************/
Hashtable<String, String> STATIONCMD = new Hashtable<String, String>(){

   private static final long serialVersionUID = 354054054056L;
{

}};
/***********************************************************************/
Hashtable<String, String> STATIONSTATUS = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054057L;
{
//
// General Rack
//

//
// Temperature & Humidity (DS2438_IN & DS2438_OUT devices)
//
put("TempInsideVal","DS2438IN_TEMP");    // D resource type
put("HumInsideVal","DS2438IN_HUM");      // D resource type
put("TempOutsideVal","DS2438OUT_TEMP");  // D resource type
put("HumOutsideVal","DS2438OUT_HUM");    // D resource type

//
// PWM data (Fan)
//
put("FanVal","PWM_VOUT");    // D resource type


//
// Props data
//
put("OUTComValsub","DS2438OUT_COMST");         // D resource type
put("INComValsub","DS2438IN_COMST");          // D resource type
put("ACComValsub","INTESISBOX_COMST");        // D resource type
put("ONOFFValsub","INTESISBOX_ACONOFF");      // D resource type
put("MODEValsub","INTESISBOX_ACMODE");        // D resource type
put("ECOValsub","INTESISBOX_ACECO");          // D resource type
put("FanValsub","INTESISBOX_ACFAN");          // D resource type
put("TempPointValsub","INTESISBOX_ACSET");    // D resource type
}};

}
