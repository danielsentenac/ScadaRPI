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

String mainDrawing1 = "PARTICLECOUNTER.g";
String mainDrawing2 = "WE_SAFETY.g";

/***********************************************************************/
Hashtable<String, String> PANELCMD = new Hashtable<String, String>() {

   private static final long serialVersionUID = 354054054056L;
{
//
// Pump
//
put("Pump","M3_PUMPONOFF");// Type 2 (TRIGGER)
put("Reset3","M3_RESET3");// Type 2 (TRIGGER)

//
// Pump
//
put("Pump","M3_PUMPONOFF");// Type 2 (TRIGGER)
//
// PCounter
//
put("PC1START","PC1_STARTSTOP");// Type 2 (TRIGGER)
put("PC1STOP","PC1_STARTSTOP");// Type 2 (TRIGGER)
put("PC2START","PC2_STARTSTOP");// Type 2 (TRIGGER)
put("PC2STOP","PC2_STARTSTOP");// Type 2 (TRIGGER)



}};
/***********************************************************************/
Hashtable<String, String> PANEL1STATUS = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054057L;
{
//
// General Panel
//

//
// PC1
//
put("WETowerCleanStatus03","SC_VAC_PCOUNTERWE_PC1_UM03FLG");
put("WETowerCleanStatus05","SC_VAC_PCOUNTERWE_PC1_UM05FLG");
put("WETowerCleanStatus1","SC_VAC_PCOUNTERWE_PC1_UM1FLG");
put("WETowerCleanStatus205","SC_VAC_PCOUNTERWE_PC1_UM205FLG");
put("WETowerCleanStatus5","SC_VAC_PCOUNTERWE_PC1_UM5FLG");
put("WETowerCleanStatus10","SC_VAC_PCOUNTERWE_PC1_UM10FLG");
put("WETowerCleanError","SC_VAC_PCOUNTERWE_PC1_INSTRST");
put("WETowerCleanRunning","SC_VAC_PCOUNTERWE_PC1_ST");
put("WETowerCleanCom","SC_VAC_PCOUNTERWE_PC1_COMST");

//
// PC2
//
put("WEHallStatus03","SC_VAC_PCOUNTERWE_PC2_UM03FLG");
put("WEHallStatus05","SC_VAC_PCOUNTERWE_PC2_UM05FLG");
put("WEHallStatus1","SC_VAC_PCOUNTERWE_PC2_UM1FLG");
put("WEHallStatus205","SC_VAC_PCOUNTERWE_PC2_UM205FLG");
put("WEHallStatus5","SC_VAC_PCOUNTERWE_PC2_UM5FLG");
put("WEHallStatus10","SC_VAC_PCOUNTERWE_PC2_UM10FLG");
put("WEHallError","SC_VAC_PCOUNTERWE_PC2_INSTRST");
put("WEHallRunning","SC_VAC_PCOUNTERWE_PC2_ST");
put("WEHallCom","SC_VAC_PCOUNTERWE_PC2_COMST");

}};

Hashtable<String, String> PANEL2STATUS = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054098L;

{
/***********************************************************************
O2 sensors
/***********************************************************************
put("O2_H120_ZOWEBVal","SC2_VAC_CB_O2_ZOWE_B_H120");
put("O2_H120_ZOWEB","SC2_VAC_CB_O2_ZOWE_B_H120_ST");
put("O2_H120_ZOWEBCol","SC2_VAC_CB_O2_ZOWE_B_H120_ST");
put("O2_H170_ZOWEBVal","SC2_VAC_CB_O2_ZOWE_B_H170");
put("O2_H170_ZOWEB","SC2_VAC_CB_O2_ZOWE_B_H170_ST");
put("O2_H170_ZOWEBCol","SC2_VAC_CB_O2_ZOWE_B_H170_ST");
//put("O2_PUSH_ZOWEBVal","SC2_VAC_CB_O2_ZOWE_B_PUSH");
put("O2_PUSH_ZOWEB","SC2_VAC_CB_O2_ZOWE_B_PUSH");
put("O2_PUSH_ZOWEBCol","SC2_VAC_CB_O2_ZOWE_B_PUSH_ST");
put("O2_H60_ZOWECVal","SC2_VAC_CB_O2_ZOWE_C_H60");
put("O2_H60_ZOWEC","SC2_VAC_CB_O2_ZOWE_C_H60_ST");
put("O2_H60_ZOWECCol","SC2_VAC_CB_O2_ZOWE_C_H60_ST");
put("O2_H120_ZOWECVal","SC2_VAC_CB_O2_ZOWE_C_H120");
put("O2_H120_ZOWEC","SC2_VAC_CB_O2_ZOWE_C_H120_ST");
put("O2_H120_ZOWECCol","SC2_VAC_CB_O2_ZOWE_C_H120_ST");
//put("O2_PUSH_ZOWECVal","SC2_VAC_CB_O2_ZOWE_C_PUSH");
put("O2_PUSH_ZOWEC","SC2_VAC_CB_O2_ZOWE_C_PUSH");
put("O2_PUSH_ZOWECCol","SC2_VAC_CB_O2_ZOWE_C_PUSH_ST");
put("O2_H60_ZOWEAVal","SC2_VAC_CB_O2_ZOWE_A_H60");
put("O2_H60_ZOWEA","SC2_VAC_CB_O2_ZOWE_A_H60_ST");
put("O2_H60_ZOWEACol","SC2_VAC_CB_O2_ZOWE_A_H60_ST");
put("O2_H120_ZOWEAVal","SC2_VAC_CB_O2_ZOWE_A_H120");
put("O2_H120_ZOWEA","SC2_VAC_CB_O2_ZOWE_A_H120_ST");
put("O2_H120_ZOWEACol","SC2_VAC_CB_O2_ZOWE_A_H120_ST");
//put("O2_PUSH_ZOWEAVal","SC2_VAC_CB_O2_ZOWE_A_PUSH");
put("O2_PUSH_ZOWEA","SC2_VAC_CB_O2_ZOWE_A_PUSH");
put("O2_PUSH_ZOWEACol","SC2_VAC_CB_O2_ZOWE_A_PUSH_ST");
put("O2_H60_ZOWEEVal","SC2_VAC_CB_O2_ZOWE_E_H60");
put("O2_H60_ZOWEE","SC2_VAC_CB_O2_ZOWE_E_H60_ST");
put("O2_H60_ZOWEECol","SC2_VAC_CB_O2_ZOWE_E_H60_ST");
put("O2_H120_ZOWEEVal","SC2_VAC_CB_O2_ZOWE_E_H120");
put("O2_H120_ZOWEE","SC2_VAC_CB_O2_ZOWE_E_H120_ST");
put("O2_H120_ZOWEECol","SC2_VAC_CB_O2_ZOWE_E_H120_ST");
//put("O2_PUSH_ZOWEEVal","SC2_VAC_CB_O2_ZOWE_E_PUSH");
put("O2_PUSH_ZOWEE","SC2_VAC_CB_O2_ZOWE_E_PUSH");
put("O2_PUSH_ZOWEECol","SC2_VAC_CB_O2_ZOWE_E_PUSH_ST");
put("O2_H170_ZOWEDVal","SC2_VAC_CB_O2_ZOWE_D_H170");
put("O2_H170_ZOWED","SC2_VAC_CB_O2_ZOWE_D_H170_ST");
put("O2_H170_ZOWEDCol","SC2_VAC_CB_O2_ZOWE_D_H170_ST");
put("O2_H120_ZOWEDVal","SC2_VAC_CB_O2_ZOWE_D_H120");
put("O2_H120_ZOWED","SC2_VAC_CB_O2_ZOWE_D_H120_ST");
put("O2_H120_ZOWEDCol","SC2_VAC_CB_O2_ZOWE_D_H120_ST");
//put("O2_PUSH_ZOWED_EXTVal","SC2_VAC_CB_O2_ZOWE_D_PUSH_EXT");
put("O2_PUSH_ZOWED_EXT","SC2_VAC_CB_O2_ZOWE_D_PUSH_EXT");
put("O2_PUSH_ZOWED_EXTCol","SC2_VAC_CB_O2_ZOWE_D_PUSH_EXT_ST");
//put("O2_PUSH_ZOWED_INTVal","SC2_VAC_CB_O2_ZOWE_D_PUSH_INT");
put("O2_PUSH_ZOWED_INT","SC2_VAC_CB_O2_ZOWE_D_PUSH_INT");
put("O2_PUSH_ZOWED_INTCol","SC2_VAC_CB_O2_ZOWE_D_PUSH_INT_ST");
put("O2_H60_ZOWEFVal","SC2_VAC_CB_O2_ZOWE_F_H60");
put("O2_H60_ZOWEF","SC2_VAC_CB_O2_ZOWE_F_H60_ST");
put("O2_H60_ZOWEFCol","SC2_VAC_CB_O2_ZOWE_F_H60_ST");
put("O2_H120_ZOWEFVal","SC2_VAC_CB_O2_ZOWE_F_H120");
put("O2_H120_ZOWEF","SC2_VAC_CB_O2_ZOWE_F_H120_ST");
put("O2_H120_ZOWEFCol","SC2_VAC_CB_O2_ZOWE_F_H120_ST");
put("O2_H60_ZOWEGVal","SC2_VAC_CB_O2_ZOWE_G_H60");
put("O2_H60_ZOWEG","SC2_VAC_CB_O2_ZOWE_G_H60_ST");
put("O2_H60_ZOWEGCol","SC2_VAC_CB_O2_ZOWE_G_H60_ST");
put("O2_H120_ZOWEGVal","SC2_VAC_CB_O2_ZOWE_G_H120");
put("O2_H120_ZOWEG","SC2_VAC_CB_O2_ZOWE_G_H120_ST");
put("O2_H120_ZOWEGCol","SC2_VAC_CB_O2_ZOWE_G_H120_ST");
put("O2_H60_ZOWEHVal","SC2_VAC_CB_O2_CLEANROOM_DOWN");
put("O2_H60_ZOWEH","SC2_VAC_CB_O2_CLEANROOM_DOWN_ST");
put("O2_H60_ZOWEHCol","SC2_VAC_CB_O2_CLEANROOM_DOWN_ST");
put("O2_H120_ZOWEHVal","SC2_VAC_CB_O2_CLEANROOM_UP");
put("O2_H120_ZOWEH","SC2_VAC_CB_O2_CLEANROOM_UP_ST");
put("O2_H120_ZOWEHCol","SC2_VAC_CB_O2_CLEANROOM_UP_ST");
//put("O2_PUSH_ZOWEFVal","SC2_VAC_CB_O2_ZOWE_F_PUSH");
put("O2_PUSH_ZOWEF","SC2_VAC_CB_O2_ZOWE_F_PUSH");
put("O2_PUSH_ZOWEFCol","SC2_VAC_CB_O2_ZOWE_F_PUSH_ST");
//put("O2_PUSH_ZOWEGVal","SC2_VAC_CB_O2_ZOWE_G_PUSH");
put("O2_PUSH_ZOWEG","SC2_VAC_CB_O2_ZOWE_G_PUSH");
put("O2_PUSH_ZOWEGCol","SC2_VAC_CB_O2_ZOWE_G_PUSH_ST");
put("O2_CB_SIREN","SC2_VAC_CB_O2_SIREN_ST");
put("O2_CB_SIRENCol","SC2_VAC_CB_O2_SIREN_ST");
put("O2_CB_LN2VALVE","SC2_VAC_CB_O2_ELECTROVALVE_ST");
put("O2_CB_LN2VALVECol","SC2_VAC_CB_O2_ELECTROVALVE_ST");

put("O2_WE_TOWER_DX_UPVal","SC2_VAC_WE_O2_TOWER_DX_UP");
put("O2_WE_TOWER_DX_UP","SC2_VAC_WE_O2_TOWER_DX_UP_ST");
put("O2_WE_TOWER_DX_UPCol","SC2_VAC_WE_O2_TOWER_DX_UP_ST");
put("O2_WE_TOWER_DX_DOWNVal","SC2_VAC_WE_O2_TOWER_DX_DOWN"); 
put("O2_WE_TOWER_DX_DOWN","SC2_VAC_WE_O2_TOWER_DX_DOWN_ST");
put("O2_WE_TOWER_DX_DOWNCol","SC2_VAC_WE_O2_TOWER_DX_DOWN_ST");
put("O2_WE_BASEROOM_UPVal","SC2_VAC_WE_O2_BASEROOM_UP"); 
put("O2_WE_BASEROOM_UP","SC2_VAC_WE_O2_BASEROOM_UP_ST");
put("O2_WE_BASEROOM_UPCol","SC2_VAC_WE_O2_BASEROOM_UP_ST");
put("O2_WE_BASEROOM_DOWNVal","SC2_VAC_WE_O2_BASEROOM_DOWN"); 
put("O2_WE_BASEROOM_DOWN","SC2_VAC_WE_O2_BASEROOM_DOWN_ST");
put("O2_WE_BASEROOM_DOWNCol","SC2_VAC_WE_O2_BASEROOM_DOWN_ST");
//put("O2_MAIN_PUSHVal","SC2_VAC_WE_O2_MAIN_PUSH");
put("O2_WE_MAIN_PUSH","SC2_VAC_WE_O2_MAIN_PUSH");
put("O2_WE_MAIN_PUSHCol","SC2_VAC_WE_O2_MAIN_PUSH_ST");
//put("O2_TUNNEL_PUSHVal","SC2_VAC_WE_O2_TUNNEL_PUSH");
put("O2_WE_TUNNEL_PUSH","SC2_VAC_WE_O2_TUNNEL_PUSH");
put("O2_WE_TUNNEL_PUSHCol","SC2_VAC_WE_O2_TUNNEL_PUSH_ST");
//put("O2_WE_LN2VALVEPUSHVal","SC2_VAC_WE_O2_LN2VALVE");
put("O2_WE_CLEANROOM_UPVal","SC2_VAC_WE_O2_CLEANROOM_UP");
put("O2_WE_CLEANROOM_UP","SC2_VAC_WE_O2_CLEANROOM_UP_ST");
put("O2_WE_CLEANROOM_UPCol","SC2_VAC_WE_O2_CLEANROOM_UP_ST");
put("O2_WE_CLEANROOM_DOWNVal","SC2_VAC_WE_O2_CLEANROOM_DOWN");
put("O2_WE_CLEANROOM_DOWN","SC2_VAC_WE_O2_CLEANROOM_DOWN_ST");
put("O2_WE_CLEANROOM_DOWNCol","SC2_VAC_WE_O2_CLEANROOM_DOWN_ST");
put("O2_WE_TOWER_SX_UPVal","SC2_VAC_WE_O2_TOWER_SX_UP");
put("O2_WE_TOWER_SX_UP","SC2_VAC_WE_O2_TOWER_SX_UP_ST");
put("O2_WE_TOWER_SX_UPCol","SC2_VAC_WE_O2_TOWER_SX_UP_ST");
put("O2_WE_TOWER_SX_DOWNVal","SC2_VAC_WE_O2_TOWER_SX_DOWN");
put("O2_WE_TOWER_SX_DOWN","SC2_VAC_WE_O2_TOWER_SX_DOWN_ST");
put("O2_WE_TOWER_SX_DOWNCol","SC2_VAC_WE_O2_TOWER_SX_DOWN_ST");
put("O2_WE_TUNNEL_UPVal","SC2_VAC_WE_O2_TUNNEL_UP");
put("O2_WE_TUNNEL_UP","SC2_VAC_WE_O2_TUNNEL_UP_ST");
put("O2_WE_TUNNEL_UPCol","SC2_VAC_WE_O2_TUNNEL_UP_ST");
put("O2_WE_TUNNEL_DOWNVal","SC2_VAC_WE_O2_TUNNEL_DOWN");
put("O2_WE_TUNNEL_DOWN","SC2_VAC_WE_O2_TUNNEL_DOWN_ST");
put("O2_WE_TUNNEL_DOWNCol","SC2_VAC_WE_O2_TUNNEL_DOWN_ST");
put("O2_WE_TUNNEL_DOOR_UPVal","SC2_VAC_WE_O2_TUNNELDOOR_UP");
put("O2_WE_TUNNEL_DOOR_UP","SC2_VAC_WE_O2_TUNNELDOOR_UP_ST");
put("O2_WE_TUNNEL_DOOR_UPCol","SC2_VAC_WE_O2_TUNNELDOOR_UP_ST");
put("O2_WE_TUNNEL_DOOR_DOWNVal","SC2_VAC_WE_O2_TUNNELDOOR_DOWN");
put("O2_WE_TUNNEL_DOOR_DOWN","SC2_VAC_WE_O2_TUNNELDOOR_DOWN_ST");
put("O2_WE_TUNNEL_DOOR_DOWNCol","SC2_VAC_WE_O2_TUNNELDOOR_DOWN_ST");
put("O2_WE_SIREN","SC2_VAC_WE_O2_SIREN_ST");
put("O2_WE_SIRENCol","SC2_VAC_WE_O2_SIREN_ST");
put("O2_WE_LN2VALVE","SC2_VAC_WE_O2_COMMAND_ELEC_ST");
put("O2_WE_LN2VALVECol","SC2_VAC_WE_O2_COMMAND_ELEC_ST");
*/
put("O2_WE_TUNNEL_DOWNVal","SC2_VAC_WE_O2_TUNNEL_DOWN"); 
put("O2_WE_TUNNEL_DOWN","SC2_VAC_WE_O2_TUNNEL_DOWN_ST"); 
put("O2_WE_TUNNEL_DOWNCol","SC2_VAC_WE_O2_TUNNEL_DOWN_ST"); 
put("O2_WE_TUNNEL_UPVal","SC2_VAC_WE_O2_TUNNEL_UP"); 
put("O2_WE_TUNNEL_UP","SC2_VAC_WE_O2_TUNNEL_UP_ST");
put("O2_WE_TUNNEL_UPCol","SC2_VAC_WE_O2_TUNNEL_UP_ST");
put("O2_WE_BASEROOM_DOWNVal","SC2_VAC_WE_O2_BASEROOM_DOWN"); 
put("O2_WE_BASEROOM_DOWN","SC2_VAC_WE_O2_BASEROOM_DOWN_ST");
put("O2_WE_BASEROOM_DOWNCol","SC2_VAC_WE_O2_BASEROOM_DOWN_ST");
put("O2_WE_BASEROOM_UPVal","SC2_VAC_WE_O2_BASEROOM_UP"); 
put("O2_WE_BASEROOM_UP","SC2_VAC_WE_O2_BASEROOM_UP_ST");
put("O2_WE_BASEROOM_UPCol","SC2_VAC_WE_O2_BASEROOM_UP_ST");
//put("O2_MAIN_PUSHVal","SC2_VAC_WE_O2_MAIN_PUSH");
put("O2_WE_MAIN_PUSH","SC2_VAC_WE_O2_MAIN_PUSH");
put("O2_WE_MAIN_PUSHCol","SC2_VAC_WE_O2_MAIN_PUSH_ST");
//put("O2_TUNNEL_PUSHVal","SC2_VAC_WE_O2_TUNNEL_PUSH");
put("O2_WE_TUNNEL_PUSH","SC2_VAC_WE_O2_TUNNEL_PUSH");
put("O2_WE_TUNNEL_PUSHCol","SC2_VAC_WE_O2_TUNNEL_PUSH_ST");
//put("O2_WE_LN2VALVEPUSHVal","SC2_VAC_WE_O2_LN2VALVE");
put("O2_WE_TOWER_DX_UPVal","SC2_VAC_WE_O2_TOWER_DX_UP");
put("O2_WE_TOWER_DX_UP","SC2_VAC_WE_O2_TOWER_DX_UP_ST");
put("O2_WE_TOWER_DX_UPCol","SC2_VAC_WE_O2_TOWER_DX_UP_ST");
put("O2_WE_TOWER_DX_DOWNVal","SC2_VAC_WE_O2_TOWER_DX_DOWN");
put("O2_WE_TOWER_DX_DOWN","SC2_VAC_WE_O2_TOWER_DX_DOWN_ST");
put("O2_WE_TOWER_DX_DOWNCol","SC2_VAC_WE_O2_TOWER_DX_DOWN_ST");
put("O2_WE_TOWER_SX_UPVal","SC2_VAC_WE_O2_TOWER_SX_UP");
put("O2_WE_TOWER_SX_UP","SC2_VAC_WE_O2_TOWER_SX_UP_ST");
put("O2_WE_TOWER_SX_UPCol","SC2_VAC_WE_O2_TOWER_SX_UP_ST");
put("O2_WE_TOWER_SX_DOWNVal","SC2_VAC_WE_O2_TOWER_SX_DOWN");
put("O2_WE_TOWER_SX_DOWN","SC2_VAC_WE_O2_TOWER_SX_DOWN_ST");
put("O2_WE_TOWER_SX_DOWNCol","SC2_VAC_WE_O2_TOWER_SX_DOWN_ST");
put("O2_WE_CLEANROOM_UPVal","SC2_VAC_WE_O2_CLEANROOM_UP");
put("O2_WE_CLEANROOM_UP","SC2_VAC_WE_O2_CLEANROOM_UP_ST");
put("O2_WE_CLEANROOM_UPCol","SC2_VAC_WE_O2_CLEANROOM_UP_ST");
put("O2_WE_CLEANROOM_DOWNVal","SC2_VAC_WE_O2_CLEANROOM_DOWN");
put("O2_WE_CLEANROOM_DOWN","SC2_VAC_WE_O2_CLEANROOM_DOWN_ST");
put("O2_WE_CLEANROOM_DOWNCol","SC2_VAC_WE_O2_CLEANROOM_DOWN_ST");
put("O2_WE_TUNNEL_DOOR_DOWNVal","SC2_VAC_WE_O2_TUNNELDOOR_DOWN");
put("O2_WE_TUNNEL_DOOR_DOWN","SC2_VAC_WE_O2_TUNNELDOOR_DOWN_ST");
put("O2_WE_TUNNEL_DOOR_DOWNCol","SC2_VAC_WE_O2_TUNNELDOOR_DOWN_ST");
put("O2_WE_TUNNEL_DOOR_UPVal","SC2_VAC_WE_O2_TUNNELDOOR_UP");
put("O2_WE_TUNNEL_DOOR_UP","SC2_VAC_WE_O2_TUNNELDOOR_UP_ST");
put("O2_WE_TUNNEL_DOOR_UPCol","SC2_VAC_WE_O2_TUNNELDOOR_UP_ST");
put("O2_WE_SIREN","SC2_VAC_WE_O2_SIREN_ST");
put("O2_WE_SIRENCol","SC2_VAC_WE_O2_SIREN_ST");
put("O2_WE_LN2VALVE","SC2_VAC_WE_O2_COMMAND_ELEC_ST");
put("O2_WE_LN2VALVECol","SC2_VAC_WE_O2_COMMAND_ELEC_ST");

}};

/***********************************************************************/
Hashtable<Integer,GlgPoint> SensorO2ColorSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,1.));     // Ok
put(1,new GlgPoint(1.,1.,0.));     // Alarm Level 1
put(2,new GlgPoint(1.,0.5,0.));    // Alarm Level 2
put(3,new GlgPoint(1.,0.,0.));     // Alarm Level 3
put(4,new GlgPoint(0.5,0.5,0.5));  // Unset
put(5,new GlgPoint(0.5,0.25,0.));  // Faulty
put(6,new GlgPoint(1,0.5,0.5));  // Alarm Reset
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer,GlgPoint> PushO2ColorSTATUS = new Hashtable<Integer, GlgPoint>(){{
put(0,new GlgPoint(0.,1.,0.));     // Ok
put(1,new GlgPoint(1.,0.,0.));     // Alarm Level 1
put(2,new GlgPoint(1.,0.,0.));     // Alarm Level 2
put(3,new GlgPoint(1.,0.,0.));     // Alarm Level 3
put(4,new GlgPoint(0.5,0.5,0.5));  // Unset
put(5,new GlgPoint(0.5,0.25,0.));  // Faulty
put(6,new GlgPoint(1,0.5,0.5));  // Alarm Reset
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer,GlgPoint> RelayO2ColorSTATUS = new Hashtable<Integer, GlgPoint>(){{
put(0,new GlgPoint(0.,1.,0.));     // Ok
put(1,new GlgPoint(1.,0.,0.));     // Alarm Level 1
put(2,new GlgPoint(1.,0.,0.));     // Alarm Level 2
put(3,new GlgPoint(1.,0.,0.));     // Alarm Level 3
put(4,new GlgPoint(0.5,0.5,0.5));  // Unset
put(5,new GlgPoint(0.5,0.25,0.));  // Faulty
put(6,new GlgPoint(1,0.5,0.5));  // Alarm Reset
put(255,new GlgPoint(0.5,0.5,0.5));
}};

//
// PCounter Diagnostics
//
/***********************************************************************/
Hashtable<String, String> PCounterStatusDetails = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054058L;
{

// Particle Counters
put("AlarmComPC1","PC1_COMST");
put("AlarmComPC1Str","PC1_COMST");
put("AlarmComPC1Col","PC1_COMST");
put("AlarmComPC2","PC2_COMST");
put("AlarmComPC2Str","PC2_COMST");
put("AlarmComPC2Col","PC2_COMST");


put("03FLGPC1Val","PC1_UM03FLG");
put("05FLGPC1Val","PC1_UM05FLG");
put("1FLGPC1Val","PC1_UM1FLG");
put("205FLGPC1Val","PC1_UM205FLG");
put("5FLGPC1Val","PC1_UM5FLG");
put("10FLGPC1Val","PC1_UM10FLG");

put("03THRPC1Val","PC1_UM03THR");
put("05THRPC1Val","PC1_UM05THR");
put("1THRPC1Val","PC1_UM1THR");
put("205THRPC1Val","PC1_UM205THR");
put("5THRPC1Val","PC1_UM5THR");
put("10THRPC1Val","PC1_UM10THR");

put("03FLGPC2Val","PC2_UM03FLG");
put("05FLGPC2Val","PC2_UM05FLG");
put("1FLGPC2Val","PC2_UM1FLG");
put("205FLGPC2Val","PC2_UM205FLG");
put("5FLGPC2Val","PC2_UM5FLG");
put("10FLGPC2Val","PC2_UM10FLG");

put("03THRPC2Val","PC2_UM03THR");
put("05THRPC2Val","PC2_UM05THR");
put("1THRPC2Val","PC2_UM1THR");
put("205THRPC2Val","PC2_UM205THR");
put("5THRPC2Val","PC2_UM5THR");
put("10THRPC2Val","PC2_UM10THR");


// Rack
put("AlarmComM3","M3_COMST");
put("AlarmComM3Str","M3_COMST");
put("AlarmComM3Col","M3_COMST");
put("PumpStatus","M3_PUMPST");
put("PumpStatusStr","M3_PUMPST");
put("PumpStatusCol","M3_PUMPST");

}};

//
// RPCounter Temperature sensors
//
/***********************************************************************/
Hashtable<String, String> PCounterTemp = new Hashtable<String, String>(){
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
// PCounter Instrument status
//
Hashtable<Integer, String> PCounterINSTRSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054060L;
{ 
put(0,"OK");
put(1,"Flow Error");
put(2,"Laser Error");
put(4,"RTCC Error");
put(8,"Internal Error");
put(255,"-");                      
}};


Hashtable<Integer,GlgPoint> PCounterColorINSTRSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054062L;
{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.7,0.));
put(2,new GlgPoint(1.,0.,0.));
put(4,new GlgPoint(1.,0.,0.));
put(6,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

//
// PCounter status
//
Hashtable<Integer, String> PCounterSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054060L;
{ 
put(0,"Init next");
put(1,"Stopping");
put(2,"Stopped");
put(3,"Delay before");
put(4,"Holding");
put(5,"Sleeping");
put(6,"Sampling"); 
put(255,"-");                      
}};

Hashtable<Integer,GlgPoint> PCounterColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054062L;
{ 
put(0,new GlgPoint(1.,0.7,0.3));
put(1,new  GlgPoint(1.,0.5,0.));
put(2,new GlgPoint(1.,0.,0.));
put(3,new GlgPoint(1.,0.7,0.5));
put(4,new GlgPoint(1.,0.7,0.));
put(5,new GlgPoint(1.,0.7,0.2));
put(6,new GlgPoint(0.,1.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};
Hashtable<Integer, String> PCounterMobileSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054091L;
{ 
put(0,"Stopped");
put(1,"Delay");
put(2,"Counting");
put(3,"Hold");
put(255,"-");                      
}};

Hashtable<Integer,GlgPoint> PCounterMobileColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054092L;
{ 
put(0,new GlgPoint(1.,0.,0.));
put(1,new  GlgPoint(1.,0.5,0.));
put(2,new GlgPoint(0.,1.,0.));
put(3,new GlgPoint(1.,0.7,0.5));
put(255,new GlgPoint(0.5,0.5,0.5));
}};



//
// PCounter Com status
//
Hashtable<Integer, String> PCounterCOMSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Com OK");
put(1,"Com Error");
put(255,"?");                
}};

Hashtable<Integer,GlgPoint> PCounterColorCOMSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

//
// PCounter Flag status
//
Hashtable<Integer, String> PCounterFLAGSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Count OK");
put(1,"Approaching Threshold Alarm");
put(2,"Threshold reached Alarm");
put(255,"?");                
}};

Hashtable<Integer,GlgPoint> PCounterColorFLAGSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.7,0.));
put(2,new GlgPoint(1.,0.,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};

Hashtable<Integer, String> PCounterMobileFLAGSTATUS = new Hashtable<Integer, String>(){{ 
put(0,"Count OK");
put(1,"Threshold reached Alarm");
put(255,"?");                
}};

Hashtable<Integer,GlgPoint> PCounterMobileColorFLAGSTATUS = new Hashtable<Integer, GlgPoint>(){{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.,0.));
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
// Pump Status
//
Hashtable<Integer, String> PumpSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054066L;
{ 
put(0,"ERROR");
put(1,"ON");
put(2,"OFF");
put(255,"?");
}};
Hashtable<Integer,GlgPoint> PumpColorSTATUS = new Hashtable<Integer, GlgPoint>(){

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
put(0,new GlgPoint(1.,0.7,0.));
put(1,new GlgPoint(0.,1.,0.));
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
