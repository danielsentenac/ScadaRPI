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
put("G22","MG_PR1ONOFF");                   // Type 2 (TRIGGER)
put("G21","MG_PR2ONOFF");                   // Type 2 (TRIGGER)
put("Ga1","MG_PR3ONOFF");                   // Type 2 (TRIGGER)
put("Ga3","MG_PR4ONOFF");                   // Type 2 (TRIGGER)
put("Ga2","MG_PR5ONOFF");                   // Type 2 (TRIGGER)
put("G31","MG_PR6ONOFF");                   // Type 2 (TRIGGER)
put("XGSGa4Emult1","XGS_PR7EMULT1ONOFF");   // Type 2 (TRIGGER)
put("XGSGa4Emult2","XGS_PR7EMULT2ONOFF");   // Type 2 (TRIGGER)
put("XGSGa4Degas","XGS_PR7DEGASONOFF");     // Type 2 (TRIGGER)
//
// Valves
//
put("V21","I2C_V21CMD");                    // Type 2 (TRIGGER)
put("V22","I2C_V22CMD");                    // Type 2 (TRIGGER)

//
// Dry Pumps
//
put("DryP22","I2C_P22ONOFF");               // Type 2 (TRIGGER)
//
// Turbo Pumps
//
put("TurboP21","DCU_P21ONOFF");             // Type 2 (TRIGGER)
put("TurboP21Sty","DCU_P21STYONOFF");       // Type 2 (TRIGGER)

put("TurboP1","V81_P21ONOFF");              // Type 2 (TRIGGER)
put("TurboP1Sty","V81_P21STYONOFF");        // Type 2 (TRIGGER)
//
// Ionic Pumps
//
put("IonicP33","DUAL_P33ONOFF");            // Type 2 (TRIGGER)

//
// Titane Pumps
//
put("TitaneP31","I2C_P3132ONOFF");          // Type 2 (TRIGGER)
put("TitaneP31Subl","TSP_P31SUBLONOFF");    // Type 2 (TRIGGER)

put("TitaneP32","I2C_P3132ONOFF");          // Type 2 (TRIGGER)
put("TitaneP32Subl","TSP_P32SUBLONOFF");    // Type 2 (TRIGGER)

//
// RGA (QMS)
//
put("RGAGa5running","QMS_RGAGA5RUNNINGONOFF");   // Type 2 (TRIGGER)
put("RGAGa5emission","QMS_RGAGA5EMISSIONONOFF"); // Type 2 (TRIGGER)
put("RGAGa5degas","QMS_RGAGA5DEGASONOFF");       // Type 2 (TRIGGER)
put("RGAGa5SEM","QMS_RGAGA5SEMONOFF");           // Type 2 (TRIGGER)

}};
/***********************************************************************/
Hashtable<String, String> TUBESTATUS = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054057L;
{
//
// General Rack
//

//
// Valves (Controllino device)
//
put("V21","I2C_V21ST");    // Visibility D Resource type
put("V21Col","I2C_V21ST"); // Color G Resource type
put("V22","I2C_V22ST");
put("V22Col","I2C_V22ST");
put("V23","I2C_V23ST");
put("V23Col","I2C_V23ST");
put("Va1","I2C_VA1ST");
put("Va1Col","I2C_VA1ST");
put("Va2","I2C_VA2ST");
put("Va2Col","I2C_VA2ST");
put("V31","I2C_V31ST");
put("V31Col","I2C_V31ST");

//
// Gauges (Maxigauge device)
//
put("G22","MG_PR1SST");     // D resource type (visibility)
put("G22Psub","MG_PR1ST");     // D resource type (visibility)
put("G22Val","MG_PR1");    // D resource type
put("G22Col","MG_PR1SST");  // G resource type
put("G22PColsub","MG_PR1ST");  // G resource type
put("G21","MG_PR2SST");     // D resource type (visibility)
put("G21Psub","MG_PR2ST");     // D resource type (visibility)
put("G21Val","MG_PR2");    // D resource type
put("G21Col","MG_PR2SST");  // G resource type
put("G21PColsub","MG_PR2ST");  // G resource type
put("Ga1","MG_PR3SST");     // D resource type (visibility)
put("Ga1Psub","MG_PR3ST");     // D resource type (visibility)
put("Ga1Val","MG_PR3");    // D resource type
put("Ga1Col","MG_PR3SST");  // G resource type
put("Ga1PColsub","MG_PR3ST");  // G resource type
put("Ga3","MG_PR4SST");     // D resource type (visibility)
put("Ga3Psub","MG_PR4ST");     // D resource type (visibility)
put("Ga3Val","MG_PR4");    // D resource type
put("Ga3Col","MG_PR4SST");  // G resource type
put("Ga3PColsub","MG_PR4ST");  // G resource type
put("Ga2","MG_PR5SST");     // D resource type (visibility)
put("Ga2Psub","MG_PR5ST");     // D resource type (visibility)
put("Ga2Val","MG_PR5");    // D resource type
put("Ga2Col","MG_PR5SST");  // G resource type
put("Ga2PColsub","MG_PR5ST");  // G resource type
put("G31","MG_PR6SST");     // D resource type (visibility)
put("G31Psub","MG_PR6ST");     // D resource type (visibility)
put("G31Val","MG_PR6");    // D resource type
put("G31Col","MG_PR6SST");  // G resource type
put("G31PColsub","MG_PR6ST");  // G resource type

//
// XGS Gauges
//
put("XGSGa4","XGS_PR7ST");
put("XGSGa4Val","XGS_PR7");
put("XGSGa4Col","XGS_PR7ST");
put("XGSGa4Esub","XGS_PR7ST");
put("XGSGa4EColsub","XGS_PR7ST");
put("XGSGa4Dsub","XGS_PR7DST");
put("XGSGa4DColsub","XGS_PR7DST");
put("XGSGa4Fsub","XGS_PR7FST");
put("XGSGa4FColsub","XGS_PR7FST");

//
// RGA
//
put("RGAGa5","QMS_RGAGA5EMISSION");
put("RGAGa5Col","QMS_RGAGA5EMISSION");
put("RGAGa5runningsub","QMS_RGAGA5RUNNING");
put("RGAGa5emissionsub","QMS_RGAGA5EMISSION");
put("RGAGa5degassub","QMS_RGAGA5DEGAS");
put("RGAGa5modesub","QMS_RGAGA5MODE");
put("RGAGa5filamentsub","QMS_RGAGA5FILAMENT");

//
// Dry Pump
//
put("DryP22","I2C_P22ST");
put("DryP22Col","I2C_P22ST");

//
// Turbo Pumps
//
put("TurboP21","DCU_P21ST");
put("TurboP21Col","DCU_P21ST");
put("TurboP21SpeedVal","DCU_P21SPEED");
put("TurboP21PowerVal","DCU_P21PWR");
put("TurboP21Stysub","DCU_P21STYST");
put("TurboP21Tempsub","DCU_P21TPST");
put("TurboP21BTempsub","DCU_P21BTPST");
put("TurboP21Errsub","DCU_P21BERR");
put("TurboP21TempValsub","NULL");
put("TurboP21BTempValsub","NULL");
put("TurboP21SpeedValsub","DCU_P21SPEED");
put("TurboP21PowerValsub","DCU_P21PWR");
put("TurboP21FspeedValsub","DCU_P21FSPEED");
put("TurboP21HoursValsub","DCU_P21HR");

put("TurboP1","V81_P1ST");
put("TurboP1Col","V81_P1ST");
put("TurboP1SpeedVal","V81_P1SPEED");
put("TurboP1PowerVal","V81_P1PWR");
put("TurboP1Stysub","V81_P1STYST");
put("TurboP1Tempsub","NULL");
put("TurboP1BTempsub","NULL");
put("TurboP1Errsub","V81_P1BERR");
put("TurboP1TempValsub","V81_P1TEMP");
put("TurboP1BTempValsub","V81_P1BTEMP");
put("TurboP1SpeedValsub","V81_P1SPEED");
put("TurboP1PowerValsub","V81_P1PWR");
put("TurboP1FspeedValsub","V81_P1FSPEED");
put("TurboP1HoursValsub","V81_P1HR");

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

//
// Titane Pumps
//
// Type 22 pump
put("TitaneP31","I2C_P3132ST");                       // type 0 (READ_ONLY_STATUS)
put("TitaneP31Col","I2C_P3132ST");                    // type 0 (READ_ONLY_STATUS)
put("TitaneP31Startsub","TSP_P31STARTST");            // type 0 (READ_ONLY_STATUS)
put("TitaneP31Ctrlsub","TSP_P31CTRLST");              // type 0 (READ_ONLY_STATUS)
put("TitaneP31Errsub","TSP_P31ERR");                  // type 0 (READ_ONLY_STATUS)
put("TitaneP31AcurrentValsub","TSP_P31ABSCUR");       // type 2 (READ_ONLY_VALUE)
put("TitaneP31AvoltageValsub","TSP_P31ABSVOLT");      // type 2 (READ_ONLY_VALUE)
put("TitaneP31FILmodesub","TSP_P31FILAMENT");         // type 5 (READ_AND_WRITE_STATUS)
put("TitaneP31AUTOSTARTmodesub","TSP_P31AUTOSTARTMODE"); // type 5 (READ_AND_WRITE_STATUS)
put("TitaneP31RECOVERmodesub","TSP_P31RECOVERMODE");  // type 5 (READ_AND_WRITE_STATUS)
put("TitaneP31CTRLOPmodesub","TSP_P31CTRLOPMODE");    // type 5 (READ_AND_WRITE_STATUS)
put("TitaneP31SublcurrentValsub","TSP_P31SUBLCUR");   // type 3 (READ_AND_WRITE_VALUE)
put("TitaneP31SubltimeValsub","TSP_P31SUBLTIME");     // type 3 (READ_AND_WRITE_VALUE)
put("TitaneP31SublperiodValsub","TSP_P31SUBLPERIOD"); // type 3 (READ_AND_WRITE_VALUE)
//Type 32 Pump
put("TitaneP32","I2C_P3132ST");                       // type 0 (READ_ONLY_STATUS)
put("TitaneP32Col","I2C_P3132ST");                    // type 0 (READ_ONLY_STATUS)
put("TitaneP32Startsub","TSP_P32STARTST");            // type 0 (READ_ONLY_STATUS)
put("TitaneP32Ctrlsub","TSP_P32CTRLST");              // type 0 (READ_ONLY_STATUS)
put("TitaneP32Errsub","TSP_P32ERR");                  // type 0 (READ_ONLY_STATUS)
put("TitaneP32AcurrentValsub","TSP_P32ABSCUR");       // type 2 (READ_ONLY_VALUE)
put("TitaneP32AvoltageValsub","TSP_P32ABSVOLT");      // type 2 (READ_ONLY_VALUE)
put("TitaneP32BTempValsub","TSP_P32BTEMP");           // type 2 (READ_ONLY_VALUE)   
put("TitaneP32HrValsub","TSP_P32HR");                 // type 2 (READ_ONLY_VALUE)
put("TitaneP32CycleValsub","TSP_P32CYCLE");           // type 2 (READ_ONLY_VALUE)
put("TitaneP32REmodesub","TSP_P32REMOTEMODE");        // type 5 (READ_AND_WRITE_STATUS)
put("TitaneP32FILmodesub","TSP_P32FILAMENT");         // type 5 (READ_AND_WRITE_STATUS)
put("TitaneP32OPmodesub","TSP_P32OPMODE");            // type 5 (READ_AND_WRITE_STATUS)
put("TitaneP32CTRLOPmodesub","TSP_P32CTRLOPMODE");    // type 5 (READ_AND_WRITE_STATUS)
put("TitaneP32SublcurrentValsub","TSP_P32SUBLCUR");   // type 3 (READ_AND_WRITE_VALUE)
put("TitaneP32SubltimeValsub","TSP_P32SUBLTIME");     // type 3 (READ_AND_WRITE_VALUE)
put("TitaneP32SublperiodValsub","TSP_P32SUBLPERIOD"); // type 3 (READ_AND_WRITE_VALUE)
put("TitaneP32SublwaitValsub","TSP_P32SUBLWAIT");     // type 3 (READ_AND_WRITE_VALUE)
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
put("AlarmComTitane","TSP_P31COMST");        // type 1 (COM_STATUS)
put("AlarmComTitaneStr","TSP_P31COMST");
put("AlarmComTitaneCol","TSP_P31COMST");
put("AlarmComRga","QMS_RGAGA5COMST");        // type 1 (COM_STATUS)
put("AlarmComRgaStr","QMS_RGAGA5COMST");
put("AlarmComRgaCol","QMS_RGAGA5COMST");
put("AlarmComMaxigauge","MG_COMST");
put("AlarmComMaxigaugeStr","MG_COMST");
put("AlarmComMaxigaugeCol","MG_COMST");
put("AlarmComXGSgauge","XGS_GA4COMST");
put("AlarmComXGSgaugeStr","XGS_GA4COMST");
put("AlarmComXGSgaugeCol","XGS_GA4COMST");
put("AlarmComTurboP21","DCU_P21COMST");
put("AlarmComTurboP21Str","DCU_P21COMST");
put("AlarmComTurboP21Col","DCU_P21COMST");
put("AlarmComTurboP1","V81_P1COMST");
put("AlarmComTurboP1Str","V81_P1COMST");
put("AlarmComTurboP1Col","V81_P1COMST");
put("AlarmComI2C","I2C_COMST");
put("AlarmComI2CStr","I2C_COMST");
put("AlarmComI2CCol","I2C_COMST");
put("AirCompressed","I2C_COMPRESSAIRST");
put("AirCompressedStr","I2C_COMPRESSAIRST");
put("AirCompressedCol","I2C_COMPRESSAIRST");
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
// XGS Gauge Degas status
//
Hashtable<Integer, String> XGSGaugeDegasSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Degas Off");
put(1,"Degas On");     
put(255,"undefined");           
}};
Hashtable<Integer,GlgPoint> XGSGaugeDegasColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};
//
// XGS Gauge Emission status
//
Hashtable<Integer, String> XGSGaugeEmissionSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Emission Off");
put(1,"Emission On");
put(255,"undefined");                
}};
Hashtable<Integer,GlgPoint> XGSGaugeEmissionColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};
//
// XGS Gauge Filament status
//
Hashtable<Integer, String> XGSGaugeFilamentSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"undefined");
put(1,"Filament 1");
put(2,"Filament 2");
put(255,"undefined");                
}};
Hashtable<Integer,GlgPoint> XGSGaugeFilamentColorSTATUS = new Hashtable<Integer, GlgPoint>(){{
put(0,new GlgPoint(0.5,0.5,0.5));
put(1,new GlgPoint(1.,1.,0.));
put(2,new GlgPoint(0.,0.7,0.));
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
// Turbo Pump status
//
Hashtable<Integer, String> TurboONOFFSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Pump Off");
put(1,"Pump On");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TurboONOFFColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(2,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TurboStandbySTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Standby Off");
put(1,"Standby On");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TurboStandbyColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};
Hashtable<Integer, String> TurboTemperatureSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Temperature OK");
put(1,"Temperature Excess");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TurboTemperatureColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TurboTemperatureBoxSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Temperature Box OK");
put(1,"Temperature Box Excess");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TurboTemperatureBoxColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> Turbo1ErrorSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Box Ok");
put(1,"Rotation Speed Excess");
put(2,"OvertVoltage");
put(6,"RunUp Time Error");
put(8,"Connection ElectronicDriveUnit Fail");
put(10,"Internal Device Fault");
put(21,"Electronic DriveUnit not Reconize pump");
put(41,"Excess Current Motor");
put(43,"Internal configuration Fault");
put(44,"Excess Temperature Electronic");
put(45,"Excess Temperature Motor");
put(46,"Internal Initialization Fault");
put(73,"Overload Axial Bearing");
put(74,"Overload Radial Bearing");
put(89,"Rotor out of Target area");
put(91,"Internal Device Fault");
put(92,"Unknow Connection Panel");
put(93,"Temperature analysis motor Fault");
put(94,"Temperature analysis Electronic Fault");
put(98,"Internal communication Fault");
put(107,"Collective Fault Power Stage");
put(108,"Rotation Speed Measurement Fault");
put(109,"Firmware not confirmed");
put(114,"Temperature analysis Power Stage Fault");
put(117,"Excess Temperature pump Bottom part");
put(118,"Excess Temperature Power Stage");
put(119,"Excess Temperature Bearing");
put(777,"Nominal Rotation Speed not Confirmed");
put(800,"Excess Current position Sensors");
put(802,"Calibration of Position Sensors Fault");
put(810,"Data Set Missing in Pump");
put(815,"Excess Current Magnetic Bearing output stage");
put(890,"Safety Bearing stress > 100%");
put(891,"Rotor Unbalance > 100%");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> Turbo1ErrorColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.,0.));
put(2,new GlgPoint(1.,0.,0.));
put(6,new GlgPoint(1.,0.,0.));
put(8,new GlgPoint(1.,0.,0.));
put(10,new GlgPoint(1.,0.,0.));
put(21,new GlgPoint(1.,0.,0.));
put(41,new GlgPoint(1.,0.,0.));
put(43,new GlgPoint(1.,0.,0.));
put(44,new GlgPoint(1.,0.,0.));
put(45,new GlgPoint(1.,0.,0.));
put(46,new GlgPoint(1.,0.,0.));
put(73,new GlgPoint(1.,0.,0.));
put(74,new GlgPoint(1.,0.,0.));
put(89,new GlgPoint(1.,0.,0.));
put(91,new GlgPoint(1.,0.,0.));
put(92,new GlgPoint(1.,0.,0.));
put(93,new GlgPoint(1.,0.,0.));
put(94,new GlgPoint(1.,0.,0.));
put(98,new GlgPoint(1.,0.,0.));
put(107,new GlgPoint(1.,0.,0.));
put(108,new GlgPoint(1.,0.,0.));
put(109,new GlgPoint(1.,0.,0.));
put(114,new GlgPoint(1.,0.,0.));
put(117,new GlgPoint(1.,0.,0.));
put(118,new GlgPoint(1.,0.,0.));
put(119,new GlgPoint(1.,0.,0.));
put(777,new GlgPoint(1.,0.,0.));
put(800,new GlgPoint(1.,0.,0.));
put(802,new GlgPoint(1.,0.,0.));
put(810,new GlgPoint(1.,0.,0.));
put(815,new GlgPoint(1.,0.,0.));
put(890,new GlgPoint(1.,0.,0.));
put(891,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> Turbo2ErrorSTATUS = new Hashtable<Integer, String>(){{
put(0,"No Box Error");
put(1,"Excess Rotation Speed");
put(2,"OverVoltage");
put(6,"RunUp Time Error");  
put(8,"Connection ElectronicDriveUnit Fail");
put(10,"Internal Device Fault");
put(14,"Heating rate modified by pulling or malfunction");
put(16,"Brake reduction current defectice");
put(17,"Non Conformity in setting switching output3");
put(20,"Emergency current supply malfunction");
put(21,"Electronic DriveUnit not Reconize pump");
put(22,"Pump Cable not connected or impedance defective");
put(33,"+15Volt Malfunction");
put(34,"Motor Voltage 70V malfunction");
put(40,"RAM Module defective");
put(41,"Excess Current Motor");
put(43,"Data Storage Malfunction");  
put(50,"Excess Temperature Magnetic Bearing amplifier");
put(51,"Vent valve defective");
put(52,"Watchdog TMS320");
put(62,"Magnetic Bearing X unstabil");
put(63,"Magnetic Bearing Y unstabil");
put(64,"Radial Emergency Bearing clearance too small");
put(65,"Radial Emergency Bearing clearance too big");
put(66,"Lower Axial Bearing clearance too small");
put(67,"Lower Axial Bearing clearance too big");
put(68,"Upper Axial Bearing clearance too small");
put(69,"Upper Axial Bearing clearance too big");
put(70,"Axial Bearing unstabil");
put(72,"Axial Bearing Current too low");
put(73,"Axial Bearing Current too high");
put(74,"Radial Bearing Current X different"); 
put(75,"Radial Bearing Current Y different"); 
put(78,"Axial Bearing Amplifier Defective"); 
put(79,"Radial Bearing Amplifier X Defective"); 
put(80,"Radial Bearing Amplifier Y Defective"); 
put(82,"Pump Cable disconnected ");
put(86,"Sensor Voltage X Malfunctioning ");
put(87,"Sensor Voltage Y Malfunctioning ");
put(88,"Sensor Voltage axial bearing Malfunctioning ");
put(89,"Rotor out of Target area ");
put(100,"Pressure gauge defective");
put(101,"High vacuum valve bridge not Connected");
put(102,"High vacuum valve in undefined position ");
put(103,"High vacuum valve does not close ");  
put(104,"Composite error backing pump");
put(105,"TCS Disconnected ");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> Turbo2ErrorColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.,0.));
put(2,new GlgPoint(1.,0.,0.));
put(6,new GlgPoint(1.,0.,0.));
put(8,new GlgPoint(1.,0.,0.));
put(10,new GlgPoint(1.,0.,0.));
put(14,new GlgPoint(1.,0.,0.));
put(16,new GlgPoint(1.,0.,0.));
put(17,new GlgPoint(1.,0.,0.));
put(20,new GlgPoint(1.,0.,0.));
put(21,new GlgPoint(1.,0.,0.));
put(22,new GlgPoint(1.,0.,0.));
put(33,new GlgPoint(1.,0.,0.));
put(34,new GlgPoint(1.,0.,0.));
put(40,new GlgPoint(1.,0.,0.));
put(41,new GlgPoint(1.,0.,0.));
put(43,new GlgPoint(1.,0.,0.));  
put(50,new GlgPoint(1.,0.,0.));
put(51,new GlgPoint(1.,0.,0.));
put(52,new GlgPoint(1.,0.,0.));
put(62,new GlgPoint(1.,0.,0.));
put(63,new GlgPoint(1.,0.,0.));
put(64,new GlgPoint(1.,0.,0.));
put(65,new GlgPoint(1.,0.,0.));
put(66,new GlgPoint(1.,0.,0.));
put(67,new GlgPoint(1.,0.,0.));
put(68,new GlgPoint(1.,0.,0.));
put(69,new GlgPoint(1.,0.,0.));
put(70,new GlgPoint(1.,0.,0.));
put(72,new GlgPoint(1.,0.,0.));
put(73,new GlgPoint(1.,0.,0.));
put(74,new GlgPoint(1.,0.,0.)); 
put(75,new GlgPoint(1.,0.,0.)); 
put(78,new GlgPoint(1.,0.,0.)); 
put(79,new GlgPoint(1.,0.,0.)); 
put(80,new GlgPoint(1.,0.,0.)); 
put(82,new GlgPoint(1.,0.,0.));
put(86,new GlgPoint(1.,0.,0.));
put(87,new GlgPoint(1.,0.,0.));
put(88,new GlgPoint(1.,0.,0.));
put(89,new GlgPoint(1.,0.,0.));
put(100,new GlgPoint(1.,0.,0.));
put(101,new GlgPoint(1.,0.,0.));
put(102,new GlgPoint(1.,0.,0.));
put(103,new GlgPoint(1.,0.,0.));  
put(104,new GlgPoint(1.,0.,0.));
put(105,new GlgPoint(1.,0.,0.));
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

//
// Titane pump status
//
Hashtable<Integer, String> TitaneStartSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Stop");
put(1,"Start");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TitaneStartColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(2,new GlgPoint(1.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TitaneRemoteSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Serial Status");
put(2,"Local Status");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TitaneRemoteColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,1.,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TitaneControllerSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Controller: Stop");
put(1,"Controller: Fail");
put(2,"Controller: Wait interlock");
put(3,"Controller: Ramp");
put(4,"Controller: Wait sublimation");
put(5,"Controller: Sublimation");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TitaneControllerColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(1.,0.,0.));
put(2,new GlgPoint(1.,0.4,0.));
put(3,new GlgPoint(1.,1.,0.));
put(4,new GlgPoint(1.,0.4,0.));
put(5,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TitaneErrorSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"No Error");
put(1,"Temperature excess");
put(2,"Mini Ti-Ball interrupt");
put(3,"TSP filament interrupt");
put(4,"TSP defective");
put(5,"Short circuit");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TitaneErrorColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.,0.));
put(2,new GlgPoint(1.,0.,0.));
put(3,new GlgPoint(1.,0.,0.));
put(4,new GlgPoint(1.,0.,0.));
put(5,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};


Hashtable<Integer, String> TitaneOperatingModeSTATUS = new Hashtable<Integer, String>(){{
put(0,"Autostart - Recover auto");
put(1,"Autostart - Recover manual");
put(2,"Manual - Recover auto");
put(3,"Manual - Recover manual");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TitaneOperatingModeColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.7,0.));
put(2,new GlgPoint(1.,1.,0.));
put(3,new GlgPoint(1.,0.4,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TitaneAutoStartModeSTATUS = new Hashtable<Integer, String>(){{
put(0,"Autostart : ON");
put(1,"Autostart : OFF");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TitaneAutoStartModeColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.7,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TitaneRecoverModeSTATUS = new Hashtable<Integer, String>(){{
put(0,"Recover : AUTO");
put(1,"Recover : MANUAL");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TitaneRecoverModeColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.7,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TitaneControllerOperatingModeSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Manual");
put(1,"Automatic");
put(2,"Remote");
put(3,"Automatic - Remote");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TitaneControllerOperatingModeColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(2,new GlgPoint(0.,1.,0.1));
put(3,new GlgPoint(0.,1.,0.2));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TitaneFilamentSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Filament: Mini Ti-Ball");
put(1,"Filament TSP: 1");
put(2,"Filament TSP: 2");
put(3,"Filament TSP: 3");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> TitaneFilamentColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.1,0.));
put(2,new GlgPoint(1.,0.2,0.));
put(3,new GlgPoint(1.,0.3,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

//
// RGA status
//
Hashtable<Integer, String> RGAEmissionONOFFSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Emission Off");
put(1,"Emission On");
put(255,"undefined");
}};

Hashtable<Integer, String> RGARunningONOFFSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Scanning Off");
put(1,"Scanning On");
put(255,"undefined");
}};

Hashtable<Integer, String> RGADegasONOFFSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Degas Off");
put(1,"Degas On");
put(255,"undefined");
}};

Hashtable<Integer, String> RGAMODESTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Faraday");
put(1,"SEM");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> RGAONOFFColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> RGAFilamentSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Filament 0");
put(1,"Filament 1");
put(2,"Filament 1&2");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> RGAFilamentColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(2,new GlgPoint(1.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

//
// Relay (On/Off) Pump status
//
Hashtable<Integer, String> RelayGeneralSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"On");
put(1,"Off");
put(255,"undefined");
}};

Hashtable<Integer, GlgPoint> RelayGeneralColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.7,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer,GlgPoint> FailOkColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer,String> FailOkSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"FAIL");
put(1,"OK");
put(255,"undefined");
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
