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
put("OnOff","MODULATOR_MAINONOFF");             // Type 2 (TRIGGER)
}};
/***********************************************************************/
Hashtable<String, String> STATIONSTATUS = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054057L;
{
//
// General Rack
//

//
// Station values
//
put("OnOffVal","MODULATOR_MAINST");              // D resource type
put("NegativeCurVal","MODULATOR_NEGATIVECUR");   // D resource type
put("PositiveCurVal","MODULATOR_POSITIVECUR");   // D resource type
put("UVCurVal","MODULATOR_UVVOLTAGE");           // D resource type
put("NegativeVal","MODULATOR_NEGATIVEVOLT");     // D resource type
put("PositiveVal","MODULATOR_POSITIVEVOLT");     // D resource type
put("FrequencyVal","MODULATOR_FREQUENCY");       // D resource type
put("DutycycleVal","MODULATOR_DUTYCYCLE");       // D resource type

}};

}
