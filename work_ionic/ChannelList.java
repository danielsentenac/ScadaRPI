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

String mainDrawing = "TUBE.g";

/***********************************************************************/
Hashtable<String, String> TUBECMD = new Hashtable<String, String>(){

   private static final long serialVersionUID = 354054054056L;
{
//
// These are all type 2 (TRIGGERS) commands
//
//
// Gauges
//
put("G31","MG_PR6ONOFF");                   // Type 2 (TRIGGER)
//
// Valves (commands disabled: V31/V32 not operable for now)
//
//put("V31","I2C_V31CMD");                    // Type 2 (TRIGGER)
//put("V32","I2C_V32CMD");                    // Type 2 (TRIGGER)
//
// Ionic Pumps
//
put("IonicP33","DUAL_P33ONOFF");            // Type 2 (TRIGGER)

}};
/***********************************************************************/
Hashtable<String, String> TUBESTATUS = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054057L;
{
//
// Valves (Controllino device)
//
put("V31","I2C_V31ST");    // Visibility D Resource type
put("V31Col","I2C_V31ST"); // Color G Resource type
put("V32","I2C_V32ST");
put("V32Col","I2C_V32ST");

//
// Gauges (Maxigauge device)
//
put("G31","MG_PR6SST");     // D resource type (visibility)
put("G31Psub","MG_PR6ST");     // D resource type (visibility)
put("G31Val","MG_PR6");    // D resource type
put("G31Col","MG_PR6SST");  // G resource type
put("G31PColsub","MG_PR6ST");  // G resource type

//
// Ionic Pumps
//
put("IonicP33","DUAL_P33ST");                         // type 0 (READ_ONLY_STATUS)
put("IonicP33Col","DUAL_P33ST");                      // type 0 (READ_ONLY_STATUS)
put("IonicP33AcurrentValsub","DUAL_P33ABSCUR");       // type 2 (READ_ONLY_VALUE)
put("IonicP33AvoltageValsub","DUAL_P33ABSVOLT");      // type 2 (READ_ONLY_VALUE)
put("IonicP33PressureValsub","DUAL_P33P");            // type 2 (READ_ONLY_VALUE)
put("IonicP33REmodesub","DUAL_P33REMOTEMODE");        // type 5 (READ_AND_WRITE_STATUS)
put("IonicP33OPmodesub","DUAL_P33OPMODE");            // type 5 (READ_AND_WRITE_STATUS)
put("IonicP33Vmodesub","DUAL_P33VOLTMODE");           // type 5 (READ_AND_WRITE_STATUS)
put("IonicP33McurrentValsub","DUAL_P33MAXCUR");       // type 3 (READ_AND_WRITE_VALUE)
put("IonicP33MvoltageValsub","DUAL_P33MAXVOLT");      // type 3 (READ_AND_WRITE_VALUE)
put("IonicP33MpowerValsub","DUAL_P33MAXW");           // type 3 (READ_AND_WRITE_VALUE)
put("IonicP33FcurrentValsub","DUAL_P33STEP1CUR");     // type 3 (READ_AND_WRITE_VALUE)
put("IonicP33FvoltageValsub","DUAL_P33STEP1VOLT");    // type 3 (READ_AND_WRITE_VALUE)
put("IonicP33ScurrentValsub","DUAL_P33STEP2CUR");     // type 3 (READ_AND_WRITE_VALUE)
put("IonicP33SvoltageValsub","DUAL_P33STEP2VOLT");    // type 3 (READ_AND_WRITE_VALUE)
put("IonicP33PcurrentValsub","DUAL_P33PRTCUR");       // type 3 (READ_AND_WRITE_VALUE)
}};


//
// Tube Diagnostics
//
/***********************************************************************/
Hashtable<String, String> TubeStatusDetails = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054058L;
{

put("AlarmComIonic","DUAL_P33COMST");        // type 1 (COM_STATUS)
put("AlarmComIonicStr","DUAL_P33COMST");
put("AlarmComIonicCol","DUAL_P33COMST");
put("AlarmComMaxigauge","MG_COMST");
put("AlarmComMaxigaugeStr","MG_COMST");
put("AlarmComMaxigaugeCol","MG_COMST");
}};

//
// Gauge status
//
Hashtable<Integer, String> GaugeSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054059L;
{
put(0,"undefined");
put(1,"Sensor Off");
put(2,"Sensor On");
put(255,"undefined");
}};

//
// Gauge Pressure status
//
Hashtable<Integer, String> GaugePressureSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054060L;
{ 
put(0,"- Ok");
put(1,"- Underrange");
put(2,"- Overrange");
put(3,"- Error");
put(4,"- Off");
put(5,"- No Sensor");
put(6,"- Id Error"); 
put(255,"- undefined");                      
}};
Hashtable<Integer,GlgPoint> GaugeColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054061L;
{
put(0,new GlgPoint(0.5,0.5,0.5));
put(1,new GlgPoint(1.,0.7,0.));
put(2,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer,GlgPoint> GaugePressureColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054062L;
{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(0.,0.2,0.));
put(2,new GlgPoint(0.,0.4,0.));
put(3,new GlgPoint(1.,0.,0.));
put(4,new GlgPoint(1.,0.7,0.));
put(5,new GlgPoint(1.,0.7,0.));
put(6,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

//
// Valve status
//
Hashtable<Integer, String> ValveSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054063L;
{ 
put(0,"Valve Moving - Ok");
put(1,"Valve Opened - Ok");
put(2,"Valve Closed - Ok");
put(3,"Valve Sw Err - Error");
put(4,"Valve Moving - Discordance");
put(5,"Valve Opened - Discordance");
put(6,"Valve Closed - Discordance");
put(7,"Valve Sw Err - Discordance");
put(255,"undefined");
}};
Hashtable<Integer,GlgPoint> ValveColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054064L;
{ 
put(0,new GlgPoint(1.,1.,0.));
put(1,new GlgPoint(1.,0.7,0.));
put(2,new GlgPoint(0.,1.,0.));
put(3,new GlgPoint(1.,0.,0.));
put(4,new GlgPoint(1.,0.,0.));
put(5,new GlgPoint(1.,0.1,0.));
put(6,new GlgPoint(1.,0.2,0.));
put(7,new GlgPoint(1.,0.3,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

//
// Ionic Pump status
//
Hashtable<Integer, String> IonicONOFFSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Pump Off");
put(1,"On Step/Start");
put(2,"On Fixed/Start");
put(3,"On Protect/Step");
put(4,"On Protect/Fixed");
put(-3,"Off:Interlock panel");
put(-4,"Off:Remote I/O interlock");
put(-5,"Off:Cable interlock");
put(-7,"Off:Remote I/O fault");
put(-8,"Off:HV temperature excess");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> IonicONOFFColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(2,new GlgPoint(0.,1.,0.1));
put(3,new GlgPoint(0.,1.,0.2));
put(4,new GlgPoint(0.,1.,0.3));
put(-3,new GlgPoint(1.,0.,0.));
put(-4,new GlgPoint(1.,0.,0.));
put(-5,new GlgPoint(1.,0.,0.));
put(-7,new GlgPoint(1.,0.,0.));
put(-8,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> IonicRemoteSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Local Status");
put(1,"Remote I/O Status");
put(2,"Serial Status");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> IonicRemoteColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(2,new GlgPoint(1.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> IonicOperatingModeSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Started");
put(1,"Protected");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> IonicOperatingModeColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> IonicVoltageModeSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Fixed");
put(1,"Stepped");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> IonicVoltageModeColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer,GlgPoint> OkFailColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054065L;
{
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer,String> OkFailSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054066L;
{
put(0,"OK");
put(1,"FAIL");
put(255,"undefined");
}};

}
