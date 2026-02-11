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

String mainDrawing = "TUBE1500W.g";

/***********************************************************************/
Hashtable<String, String> TUBE1500WCMD = new Hashtable<String, String>(){

   private static final long serialVersionUID = 354054054056L;
{
//
// These are all type 2 (TRIGGERS) commands
//
//
// Gauges
//
put("G22","MG_PR1ONOFF");        // Type 2 (TRIGGER)
put("G21","MG_PR2ONOFF");        // Type 2 (TRIGGER)
put("Ga4","MG_PR3ONOFF");       // Type 2 (TRIGGER)
put("Ga2","XGS_PR7ONOFF");       // Type 2 (TRIGGER)


//
// Valves
//
put("V1","M1_V1CMD");          // Type 2 (TRIGGER)
put("V21","M1_V21CMD");           // Type 2 (TRIGGER)
put("V22","M1_V22CMD");          // Type 2 (TRIGGER)

//
// Bypass
//
put("BypassG22","M1_BYPASSONOFF");// Type 2 (TRIGGER)

//
// Dry Pumps
//
put("DryP22","M1_P22ONOFF");      // Type 2 (TRIGGER)

//
// Turbo Pumps
//
put("TurboP21","DCU_P21ONOFF");             // Type 2 (TRIGGER)
put("TurboP21Sty","DCU_P21STYONOFF");       // Type 2 (TRIGGER)

// 
// Sicem Channels commands
//
put("CH1_ON","M2_CHANNEL_1_ONOFF_CMD");   // Type 2 (TRIGGER)
put("CH1_OFF","M2_CHANNEL_1_ONOFF_CMD");   // Type 2 (TRIGGER)
put("CH2_ON","M2_CHANNEL_2_ONOFF_CMD");   // Type 2 (TRIGGER)
put("CH2_OFF","M2_CHANNEL_2_ONOFF_CMD");   // Type 2 (TRIGGER)

//
// Sicem Ramp Programation commands
//
put("START_RAMP","OP_STARTRAMP_CMD");   // Type 2 (TRIGGER)
put("STOP_RAMP","OP_STOPRAMP_CMD");   // Type 2 (TRIGGER)

}};
/***********************************************************************/
Hashtable<String, String> TUBE1500WSTATUS = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054057L;
{
//
// General Rack
//

//
// Valves (Controllino device)
//
put("V21","M1_V21ST");    // Visibility D Resource type
put("V21Col","M1_V21ST"); // Color G Resource type
put("V22","M1_V22ST");    // Visibility D Resource type
put("V22Col","M1_V22ST"); // Color G Resource type
put("V23","M1_V23ST");    // Visibility D Resource type
put("V23Col","M1_V23ST"); // Color G Resource type
put("V1","M1_V1ST");    // Visibility D Resource type
put("V1Col","M1_V1ST"); // Color G Resource type


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
put("Ga4","MG_PR3SST");     // D resource type (visibility)
put("Ga4Psub","MG_PR3ST");     // D resource type (visibility)
put("Ga4Val","MG_PR3");    // D resource type
put("Ga4Col","MG_PR3SST");  // G resource type
put("Ga4PColsub","MG_PR3ST");  // G resource type

//
// XGS Gauge
//
put("XGSGa2","XGS_PR7ST");
put("XGSGa2Val","XGS_PR7");
put("XGSGa2Col","XGS_PR7ST");
put("XGSGa2Esub","XGS_PR7ST");
put("XGSGa2EColsub","XGS_PR7ST");
put("XGSGa2Dsub","XGS_PR7DST");
put("XGSGa2DColsub","XGS_PR7DST");
put("XGSGa2Fsub","XGS_PR7FST");
put("XGSGa2FColsub","XGS_PR7FST");

//
// MksRotor Gauge
//
put("ROTGa5","ROT_PR1ST");
put("ROTGa5Val","ROT_PR1");
put("ROTGa5Col","ROT_PR1ST");
//
// SICEM & TCMUX Temperatures (Collected by SICEM module)
//
put("Temp1Val","SICEM_T1");
put("Setpoint1Val","SICEM_SETP1");
put("Temp2Val","SICEM_T2");
put("Setpoint2Val","SICEM_SETP2");
put("Temp3Val","SICEM_T3");
put("Setpoint3Val","SICEM_SETP3");
put("Temp4Val","SICEM_T4");
put("Setpoint4Val","SICEM_SETP4");
put("Temp5Val","SICEM_T5");
put("Setpoint5Val","SICEM_SETP5");
put("Temp6Val","SICEM_T6");
put("Setpoint6Val","SICEM_SETP6");
put("Temp7Val","SICEM_T7");
put("Setpoint7Val","SICEM_SETP7");
put("Temp1mVal","SICEM_TCMUX_T1");
put("Temp2mVal","SICEM_TCMUX_T2");
put("Temp3mVal","SICEM_TCMUX_T3");
put("Temp4mVal","SICEM_TCMUX_T4");
put("Temp5mVal","SICEM_TCMUX_T5");

//
// SICEM RAMP PROGRAMMATION (Collected by SICEM module)
//
put("TempStepVal","SICEM_TEMPSTEP");
put("TempMaxVal","SICEM_TEMPMAX");
put("TimeInterVal","SICEM_TIMEINTER");
put("TimeVal","SICEM_TIME");
//
// Dry Pump
//
put("DryP22","M1_P22ST");
put("DryP22Col","M1_P22ST");

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

//
// Bypass
//
put("BypassG22Val","M1_BYPASSST");

//
// Sicem Channels ON/OFF & INTERLOCKS
//
put("CH1STVal","M2_CHANNEL_1_ONOFF_ST");
put("CH2STVal","M2_CHANNEL_2_ONOFF_ST");
put("SICEMSTVal","M2_SICEM_ALRM_ST");


//
// RGA
//
put("RGAGa3","MKS_COMST");
put("RGAGa3Col","MKS_COMST");

}};

//
// TUBE1500W Diagnostics
//
/***********************************************************************/
Hashtable<String, String> Tube1500wStatusDetails = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054058L;
{
put("AlarmComMaxigauge","MG_COMST");
put("AlarmComMaxigaugeStr","MG_COMST");
put("AlarmComMaxigaugeCol","MG_COMST");
put("AlarmComXGSgauge","XGS_GA4COMST");
put("AlarmComXGSgaugeStr","XGS_GA4COMST");
put("AlarmComXGSgaugeCol","XGS_GA4COMST");
put("AlarmComTurboP21","DCU_P21COMST");
put("AlarmComTurboP21Str","DCU_P21COMST");
put("AlarmComTurboP21Col","DCU_P21COMST");
put("AlarmComRS232M2","SICEM_COMST");  // COM ERROR between Raspberry and M2 Module (via RS232)
put("AlarmComRS232M2Str","SICEM_COMST");
put("AlarmComRS232M2Col","SICEM_COMST");
put("AlarmComRS232SicemM2","M2_SICEM_COMERR_ST"); // COM ERROR between M2 Module and Sicem Instrument (via RS232)
put("AlarmComRS232SicemM2Str","M2_SICEM_COMERR_ST");
put("AlarmComRS232SicemM2Col","M2_SICEM_COMERR_ST");
put("AlarmComTCMuxShieldM2","M2_MUXSHIELD_COMERR_ST"); // COM ERROR between M2 Module and TCMuxShield (via SPI)
put("AlarmComTCMuxShieldM2Str","M2_MUXSHIELD_COMERR_ST");
put("AlarmComTCMuxShieldM2Col","M2_MUXSHIELD_COMERR_ST");
put("AlarmComM1","M1_COMST");
put("AlarmComM1Str","M1_COMST");
put("AlarmComM1Col","M1_COMST");
put("AlarmComM2","M2_COMST"); // COM ERROR between Raspberry and M2 Module (via I2C)
put("AlarmComM2Str","M2_COMST");
put("AlarmComM2Col","M2_COMST");
put("BypassG22","M1_BYPASSST");
put("BypassG22Str","M1_BYPASSST");
put("BypassG22Col","M1_BYPASSST");
put("AirCompressed","M1_COMPRESSAIRST");
put("AirCompressedStr","M1_COMPRESSAIRST");
put("AirCompressedCol","M1_COMPRESSAIRST");

}};

//
// TUBE1500W Rack Temperature sensors
//
/***********************************************************************/
Hashtable<String, String> TCMuxTemp = new Hashtable<String, String>(){
private static final long serialVersionUID = 354054054063L;
{
put("Temp0Val","TCMUX_TEMP0");
put("Temp1Val","TCMUX_TEMP1");
put("Temp2Val","TCMUX_TEMP2");
put("Temp3Val","TCMUX_TEMP3");
put("Temp4Val","TCMUX_TEMP4");
put("Temp5Val","TCMUX_TEMP5");
put("Temp6Val","TCMUX_TEMP6");
put("Temp7Val","TCMUX_TEMP7");
put("Temp8Val","TCMUX_TEMP8");
put("AlarmComTCMuxShield","TCMUX_COMST");
put("AlarmComTCMuxShieldStr","TCMUX_COMST");
put("AlarmComTCMuxShieldCol","TCMUX_COMST");
}};

//
// Gauge status
//
Hashtable<Integer, String> GaugeSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054059L;
{
put(0,"- switched OFF/ON unchanged");
put(1,"- switched OFF");
put(2,"- switched ON");
put(255,"- ?");
}};

//
// Gauge Pressure status
//
Hashtable<Integer, String> GaugePressureSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054060L;
{ 
put(0,"Pressure Ok");
put(1,"Pressure Underrange");
put(2,"Pressure Overrange");
put(3,"Sensor Error");
put(4,"Sensor OFF");
put(5,"No Sensor");
put(6,"Sensor Id Error"); 
put(255,"?");                      
}};

Hashtable<Integer,GlgPoint> GaugeColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054061L;
{
put(0,new GlgPoint(0.5,0.7,0.5));
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
put(0,"Degas OFF");
put(1,"Degas ON");     
put(255,"?");           
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
put(0,"Emission OFF");
put(1,"Emission ON");
put(255,"?");                
}};
Hashtable<Integer,GlgPoint> XGSGaugeEmissionColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};
//
// MKS ROTOR Gauge Emission status
//
Hashtable<Integer, String> ROTGaugeEmissionSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Emission OFF");
put(1,"Emission ON");
put(255,"?");                
}};
Hashtable<Integer,GlgPoint> ROTGaugeEmissionColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};
//
// XGS Gauge Filament status
//
Hashtable<Integer, String> XGSGaugeFilamentSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"?");
put(1,"Filament 1");
put(2,"Filament 2");
put(255,"?");                
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
put(255,"?");
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
// Bypass status
//
Hashtable<Integer, String> BypassSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054063L;
{ 
put(0,"Bypass Error");
put(1,"Bypass ON");
put(2,"Bypass OFF");
put(255,"?");
}};
Hashtable<Integer,GlgPoint> BypassColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054064L;
{ 
put(0,new GlgPoint(0.5,0.5,0.5));
put(1,new GlgPoint(1.,0.,0.));
put(2,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

//
// Fan Speed
//
Hashtable<Integer, String> FanSpeedSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054065L;
{ 
put(0,"...");
put(1,"NORMAL");
put(2,"LOW");
put(255,"?");
}};
Hashtable<Integer,GlgPoint> FanSpeedColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054066L;
{ 
put(0,new GlgPoint(1.,1.,0.1));
put(1,new GlgPoint(1.,0.7,0.));
put(2,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

//
// Fan Status
//
Hashtable<Integer, String> FanSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054066L;
{ 
put(0,"ERROR");
put(1,"ON");
put(2,"OFF");
put(255,"?");
}};
Hashtable<Integer,GlgPoint> FanColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054067L;
{ 
put(0,new GlgPoint(1.,0.,0.));
put(1,new GlgPoint(0.,1.,0.));
put(2,new GlgPoint(1.,0.7,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};


//
// Turbo Pump status
//
Hashtable<Integer, String> TurboONOFFSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Pump OFF");
put(1,"Pump ON");
put(255,"?");
}};

Hashtable<Integer, GlgPoint> TurboONOFFColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(2,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TurboStandbySTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Standby OFF");
put(1,"Standby ON");
put(255,"?");
}};

Hashtable<Integer, GlgPoint> TurboStandbyColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};
Hashtable<Integer, String> TurboTemperatureSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Temperature OK");
put(1,"Temperature Excess");
put(255,"?");
}};

Hashtable<Integer, GlgPoint> TurboTemperatureColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TurboTemperatureBoxSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Temperature Box OK");
put(1,"Temperature Box Excess");
put(255,"?");
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
put(255,"?");
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
put(102,"High vacuum valve in ? position ");
put(103,"High vacuum valve does not close ");  
put(104,"Composite error backing pump");
put(105,"TCS Disconnected ");
put(255,"?");
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
put(0,"Pump OFF");
put(1,"ON Step/Start");
put(2,"ON Fixed/Start");
put(3,"ON Protect/Step");
put(4,"ON Protect/Fixed");
put(-3,"OFF:Interlock panel");
put(-4,"OFF:Remote I/O interlock");
put(-5,"OFF:Cable interlock");
put(-7,"OFF:Remote I/O fault");
put(-8,"OFF:HV temperature excess");
put(255,"?");
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
put(255,"?");
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
put(255,"?");
}};

Hashtable<Integer, GlgPoint> IonicOperatingModeColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> IonicVoltageModeSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Fixed");
put(1,"Stepped");
put(255,"?");
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
put(255,"?");
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
put(255,"?");
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
put(255,"?");
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
put(255,"?");
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
put(255,"?");
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
put(255,"?");
}};

Hashtable<Integer, GlgPoint> TitaneAutoStartModeColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.7,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> TitaneRecoverModeSTATUS = new Hashtable<Integer, String>(){{
put(0,"Recover : AUTO");
put(1,"Recover : MANUAL");
put(255,"?");
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
put(255,"?");
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
put(255,"?");
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
put(0,"Emission OFF");
put(1,"Emission ON");
put(255,"?");
}};

Hashtable<Integer, String> RGAOperationONOFFSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"SHUTDOWN MODE");
put(1,"OPERATION MODE");
put(255,"?");
}};

Hashtable<Integer, String> RGARunningONOFFSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"SCANNING OFF");
put(1,"SCANNING ON");
put(255,"?");
}};

Hashtable<Integer, String> RGADegasONOFFSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"DEGAS OFF");
put(1,"DEGAS ON");
put(255,"?");
}};

Hashtable<Integer, String> RGAMODESTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Faraday Mode");
put(1,"SEM Mode");
put(255,"?");
}};

Hashtable<Integer, GlgPoint> RGAONOFFColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.7,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> RGAFilamentSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Filaments OFF");
put(1,"Filament 1 ON");
put(2,"Filament 2 ON");
put(255,"?");
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
put(1,"ON");
put(0,"OFF");
put(255,"?");
}};

Hashtable<Integer, GlgPoint> RelayGeneralColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(1,new GlgPoint(0.,1.,0.));
put(0,new GlgPoint(1.,0.7,0.));
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
put(255,"?");
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
put(255,"?");
}};

}
