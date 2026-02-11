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
String mainDrawing2 = "PARTICLECOUNTERMOB.g";

/***********************************************************************/
Hashtable<String, String> PCOUNTERCMD = new Hashtable<String, String>(){

   private static final long serialVersionUID = 354054054056L;
{
//
// Reset
//
put("Reset3","M3_RESET3");// Type 2 (TRIGGER)

//
// Pump
//
put("Pump","M3_PUMPONOFF");// Type 2 (TRIGGER)

//
// Rearm ON/OFF
//
put("Rearm","M3_REARMONOFF");// Type 2 (TRIGGER)

//
// AC OFF/LOW/HIGH
//
put("ACOFF","M3_ACCMD");// Type 2 (TRIGGER)
put("ACLOW","M3_ACCMD");// Type 2 (TRIGGER)
put("ACHIGH","M3_ACCMD");// Type 2 (TRIGGER)
//
// PCounter
//
put("PC1START","PC1_STARTSTOP");// Type 2 (TRIGGER)
put("PC1STOP","PC1_STARTSTOP");// Type 2 (TRIGGER)
put("PC2START","PC2_STARTSTOP");// Type 2 (TRIGGER)
put("PC2STOP","PC2_STARTSTOP");// Type 2 (TRIGGER)
put("PC3START","PC3_STARTSTOP");// Type 2 (TRIGGER)
put("PC3STOP","PC3_STARTSTOP");// Type 2 (TRIGGER)
put("PC4START","PC4_STARTSTOP");// Type 2 (TRIGGER)
put("PC4STOP","PC4_STARTSTOP");// Type 2 (TRIGGER)
put("PC5START","PC5_STARTSTOP");// Type 2 (TRIGGER)
put("PC5STOP","PC5_STARTSTOP");// Type 2 (TRIGGER)

}};
/***********************************************************************/
Hashtable<String, String> PCOUNTERSTATUS = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054057L;
{
//
// General Rack
//
//
// PC1
//
put("PC1InstrStatusStr","SC_VAC_PCOUNTERCB_PC1_INSTRST");
put("PC1InstrStatusCol","SC_VAC_PCOUNTERCB_PC1_INSTRST");
put("PC1StatusStr","SC_VAC_PCOUNTERCB_PC1_ST");
put("PC1StatusCol","SC_VAC_PCOUNTERCB_PC1_ST");
put("PC1FlowStr","SC_VAC_PCOUNTERCB_PC1_FLOW");
put("PC1SamplingStr","SC_VAC_PCOUNTERCB_PC1_SAMPLING");
put("PC1HoldingStr","SC_VAC_PCOUNTERCB_PC1_HOLDING");
put("PC1CycleStr","SC_VAC_PCOUNTERCB_PC1_CYCLE");
put("PC1UM03Val","SC_VAC_PCOUNTERCB_PC1_UM03");
put("PC1UM05Val","SC_VAC_PCOUNTERCB_PC1_UM05");
put("PC1UM1Val","SC_VAC_PCOUNTERCB_PC1_UM1");
put("PC1UM205Val","SC_VAC_PCOUNTERCB_PC1_UM205");
put("PC1UM5Val","SC_VAC_PCOUNTERCB_PC1_UM5");
put("PC1UM10Val","SC_VAC_PCOUNTERCB_PC1_UM10");
put("PC1UM10Val","SC_VAC_PCOUNTERCB_PC1_UM10");

put("03FLGPC1Val","SC_VAC_PCOUNTERCB_PC1_UM03FLG");
put("05FLGPC1Val","SC_VAC_PCOUNTERCB_PC1_UM05FLG");
put("1FLGPC1Val","SC_VAC_PCOUNTERCB_PC1_UM1FLG");
put("205FLGPC1Val","SC_VAC_PCOUNTERCB_PC1_UM205FLG");
put("5FLGPC1Val","SC_VAC_PCOUNTERCB_PC1_UM5FLG");
put("10FLGPC1Val","SC_VAC_PCOUNTERCB_PC1_UM10FLG");
//
// PC2
//
put("PC2InstrStatusStr","SC_VAC_PCOUNTERCB_PC2_INSTRST");
put("PC2InstrStatusCol","SC_VAC_PCOUNTERCB_PC2_INSTRST");
put("PC2StatusStr","SC_VAC_PCOUNTERCB_PC2_ST");
put("PC2StatusCol","SC_VAC_PCOUNTERCB_PC2_ST");
put("PC2FlowStr","SC_VAC_PCOUNTERCB_PC2_FLOW");
put("PC2SamplingStr","SC_VAC_PCOUNTERCB_PC2_SAMPLING");
put("PC2HoldingStr","SC_VAC_PCOUNTERCB_PC2_HOLDING");
put("PC2CycleStr","SC_VAC_PCOUNTERCB_PC2_CYCLE");
put("PC2UM03Val","SC_VAC_PCOUNTERCB_PC2_UM03");
put("PC2UM05Val","SC_VAC_PCOUNTERCB_PC2_UM05");
put("PC2UM1Val","SC_VAC_PCOUNTERCB_PC2_UM1");
put("PC2UM205Val","SC_VAC_PCOUNTERCB_PC2_UM205");
put("PC2UM5Val","SC_VAC_PCOUNTERCB_PC2_UM5");
put("PC2UM10Val","SC_VAC_PCOUNTERCB_PC2_UM10");

put("03FLGPC2Val","SC_VAC_PCOUNTERCB_PC2_UM03FLG");
put("05FLGPC2Val","SC_VAC_PCOUNTERCB_PC2_UM05FLG");
put("1FLGPC2Val","SC_VAC_PCOUNTERCB_PC2_UM1FLG");
put("205FLGPC2Val","SC_VAC_PCOUNTERCB_PC2_UM205FLG");
put("5FLGPC2Val","SC_VAC_PCOUNTERCB_PC2_UM5FLG");
put("10FLGPC2Val","SC_VAC_PCOUNTERCB_PC2_UM10FLG");
//
// PC3
//
put("PC3InstrStatusStr","SC_VAC_PCOUNTERCB_PC3_INSTRST");
put("PC3InstrStatusCol","SC_VAC_PCOUNTERCB_PC3_INSTRST");
put("PC3StatusStr","SC_VAC_PCOUNTERCB_PC3_ST");
put("PC3StatusCol","SC_VAC_PCOUNTERCB_PC3_ST");
put("PC3FlowStr","SC_VAC_PCOUNTERCB_PC3_FLOW");
put("PC3SamplingStr","SC_VAC_PCOUNTERCB_PC3_SAMPLING");
put("PC3HoldingStr","SC_VAC_PCOUNTERCB_PC3_HOLDING");
put("PC3CycleStr","SC_VAC_PCOUNTERCB_PC3_CYCLE");
put("PC3UM03Val","SC_VAC_PCOUNTERCB_PC3_UM03");
put("PC3UM05Val","SC_VAC_PCOUNTERCB_PC3_UM05");
put("PC3UM1Val","SC_VAC_PCOUNTERCB_PC3_UM1");
put("PC3UM205Val","SC_VAC_PCOUNTERCB_PC3_UM205");
put("PC3UM5Val","SC_VAC_PCOUNTERCB_PC3_UM5");
put("PC3UM10Val","SC_VAC_PCOUNTERCB_PC3_UM10");

put("03FLGPC3Val","SC_VAC_PCOUNTERCB_PC3_UM03FLG");
put("05FLGPC3Val","SC_VAC_PCOUNTERCB_PC3_UM05FLG");
put("1FLGPC3Val","SC_VAC_PCOUNTERCB_PC3_UM1FLG");
put("205FLGPC3Val","SC_VAC_PCOUNTERCB_PC3_UM205FLG");
put("5FLGPC3Val","SC_VAC_PCOUNTERCB_PC3_UM5FLG");
put("10FLGPC3Val","SC_VAC_PCOUNTERCB_PC3_UM10FLG");
//
// PC4
//
put("PC4InstrStatusStr","SC_VAC_PCOUNTERCB_PC4_INSTRST");
put("PC4InstrStatusCol","SC_VAC_PCOUNTERCB_PC4_INSTRST");
put("PC4StatusStr","SC_VAC_PCOUNTERCB_PC4_ST");
put("PC4StatusCol","SC_VAC_PCOUNTERCB_PC4_ST");
put("PC4FlowStr","SC_VAC_PCOUNTERCB_PC4_FLOW");
put("PC4SamplingStr","SC_VAC_PCOUNTERCB_PC4_SAMPLING");
put("PC4HoldingStr","SC_VAC_PCOUNTERCB_PC4_HOLDING");
put("PC4CycleStr","SC_VAC_PCOUNTERCB_PC4_CYCLE");
put("PC4UM03Val","SC_VAC_PCOUNTERCB_PC4_UM03");
put("PC4UM05Val","SC_VAC_PCOUNTERCB_PC4_UM05");
put("PC4UM1Val","SC_VAC_PCOUNTERCB_PC4_UM1");
put("PC4UM205Val","SC_VAC_PCOUNTERCB_PC4_UM205");
put("PC4UM5Val","SC_VAC_PCOUNTERCB_PC4_UM5");
put("PC4UM10Val","SC_VAC_PCOUNTERCB_PC4_UM10");

put("03FLGPC4Val","SC_VAC_PCOUNTERCB_PC4_UM03FLG");
put("05FLGPC4Val","SC_VAC_PCOUNTERCB_PC4_UM05FLG");
put("1FLGPC4Val","SC_VAC_PCOUNTERCB_PC4_UM1FLG");
put("205FLGPC4Val","SC_VAC_PCOUNTERCB_PC4_UM205FLG");
put("5FLGPC4Val","SC_VAC_PCOUNTERCB_PC4_UM5FLG");
put("10FLGPC4Val","SC_VAC_PCOUNTERCB_PC4_UM10FLG");
//
// PC5
//
put("PC5InstrStatusStr","SC_VAC_PCOUNTERCB_PC5_INSTRST");
put("PC5InstrStatusCol","SC_VAC_PCOUNTERCB_PC5_INSTRST");
put("PC5StatusStr","SC_VAC_PCOUNTERCB_PC5_ST");
put("PC5StatusCol","SC_VAC_PCOUNTERCB_PC5_ST");
put("PC5FlowStr","SC_VAC_PCOUNTERCB_PC5_FLOW");
put("PC5SamplingStr","SC_VAC_PCOUNTERCB_PC5_SAMPLING");
put("PC5HoldingStr","SC_VAC_PCOUNTERCB_PC5_HOLDING");
put("PC5CycleStr","SC_VAC_PCOUNTERCB_PC5_CYCLE");
put("PC5UM03Val","SC_VAC_PCOUNTERCB_PC5_UM03");
put("PC5UM05Val","SC_VAC_PCOUNTERCB_PC5_UM05");
put("PC5UM1Val","SC_VAC_PCOUNTERCB_PC5_UM1");
put("PC5UM205Val","SC_VAC_PCOUNTERCB_PC5_UM205");
put("PC5UM5Val","SC_VAC_PCOUNTERCB_PC5_UM5");
put("PC5UM10Val","SC_VAC_PCOUNTERCB_PC5_UM10");

put("03FLGPC5Val","SC_VAC_PCOUNTERCB_PC5_UM03FLG");
put("05FLGPC5Val","SC_VAC_PCOUNTERCB_PC5_UM05FLG");
put("1FLGPC5Val","SC_VAC_PCOUNTERCB_PC5_UM1FLG");
put("205FLGPC5Val","SC_VAC_PCOUNTERCB_PC5_UM205FLG");
put("5FLGPC5Val","SC_VAC_PCOUNTERCB_PC5_UM5FLG");
put("10FLGPC5Val","SC_VAC_PCOUNTERCB_PC5_UM10FLG");
//
// PC6
//
put("PC6InstrStatusStr","SC_VAC_PCOUNTERCB_PC6_INSTRST");
put("PC6InstrStatusCol","SC_VAC_PCOUNTERCB_PC6_INSTRST");
put("PC6StatusStr","SC_VAC_PCOUNTERCB_PC6_ST");
put("PC6StatusCol","SC_VAC_PCOUNTERCB_PC6_ST");
put("PC6FlowStr","SC_VAC_PCOUNTERCB_PC6_FLOW");
put("PC6SamplingStr","SC_VAC_PCOUNTERCB_PC6_SAMPLING");
put("PC6HoldingStr","SC_VAC_PCOUNTERCB_PC6_HOLDING");
put("PC6CycleStr","SC_VAC_PCOUNTERCB_PC6_CYCLE");
put("PC6UM03Val","SC_VAC_PCOUNTERCB_PC6_UM03");
put("PC6UM05Val","SC_VAC_PCOUNTERCB_PC6_UM05");
put("PC6UM1Val","SC_VAC_PCOUNTERCB_PC6_UM1");
put("PC6UM205Val","SC_VAC_PCOUNTERCB_PC6_UM205");
put("PC6UM5Val","SC_VAC_PCOUNTERCB_PC6_UM5");
put("PC6UM10Val","SC_VAC_PCOUNTERCB_PC6_UM10");

put("03FLGPC6Val","SC_VAC_PCOUNTERCB_PC6_UM03FLG");
put("05FLGPC6Val","SC_VAC_PCOUNTERCB_PC6_UM05FLG");
put("1FLGPC6Val","SC_VAC_PCOUNTERCB_PC6_UM1FLG");
put("205FLGPC6Val","SC_VAC_PCOUNTERCB_PC6_UM205FLG");
put("5FLGPC6Val","SC_VAC_PCOUNTERCB_PC6_UM5FLG");
put("10FLGPC6Val","SC_VAC_PCOUNTERCB_PC6_UM10FLG");
//
// PC7
//
put("PC7InstrStatusStr","SC_VAC_PCOUNTERCB_PC7_INSTRST");
put("PC7InstrStatusCol","SC_VAC_PCOUNTERCB_PC7_INSTRST");
put("PC7StatusStr","SC_VAC_PCOUNTERCB_PC7_ST");
put("PC7StatusCol","SC_VAC_PCOUNTERCB_PC7_ST");
put("PC7FlowStr","SC_VAC_PCOUNTERCB_PC7_FLOW");
put("PC7SamplingStr","SC_VAC_PCOUNTERCB_PC7_SAMPLING");
put("PC7HoldingStr","SC_VAC_PCOUNTERCB_PC7_HOLDING");
put("PC7CycleStr","SC_VAC_PCOUNTERCB_PC7_CYCLE");
put("PC7UM03Val","SC_VAC_PCOUNTERCB_PC7_UM03");
put("PC7UM05Val","SC_VAC_PCOUNTERCB_PC7_UM05");
put("PC7UM1Val","SC_VAC_PCOUNTERCB_PC7_UM1");
put("PC7UM205Val","SC_VAC_PCOUNTERCB_PC7_UM205");
put("PC7UM5Val","SC_VAC_PCOUNTERCB_PC7_UM5");
put("PC7UM10Val","SC_VAC_PCOUNTERCB_PC7_UM10");

put("03FLGPC7Val","SC_VAC_PCOUNTERCB_PC7_UM03FLG");
put("05FLGPC7Val","SC_VAC_PCOUNTERCB_PC7_UM05FLG");
put("1FLGPC7Val","SC_VAC_PCOUNTERCB_PC7_UM1FLG");
put("205FLGPC7Val","SC_VAC_PCOUNTERCB_PC7_UM205FLG");
put("5FLGPC7Val","SC_VAC_PCOUNTERCB_PC7_UM5FLG");
put("10FLGPC7Val","SC_VAC_PCOUNTERCB_PC7_UM10FLG");
//
// PC8
//
put("PC8InstrStatusStr","SC_VAC_PCOUNTERCB_PC8_INSTRST");
put("PC8InstrStatusCol","SC_VAC_PCOUNTERCB_PC8_INSTRST");
put("PC8StatusStr","SC_VAC_PCOUNTERCB_PC8_ST");
put("PC8StatusCol","SC_VAC_PCOUNTERCB_PC8_ST");
put("PC8FlowStr","SC_VAC_PCOUNTERCB_PC8_FLOW");
put("PC8SamplingStr","SC_VAC_PCOUNTERCB_PC8_SAMPLING");
put("PC8HoldingStr","SC_VAC_PCOUNTERCB_PC8_HOLDING");
put("PC8CycleStr","SC_VAC_PCOUNTERCB_PC8_CYCLE");
put("PC8UM03Val","SC_VAC_PCOUNTERCB_PC8_UM03");
put("PC8UM05Val","SC_VAC_PCOUNTERCB_PC8_UM05");
put("PC8UM1Val","SC_VAC_PCOUNTERCB_PC8_UM1");
put("PC8UM205Val","SC_VAC_PCOUNTERCB_PC8_UM205");
put("PC8UM5Val","SC_VAC_PCOUNTERCB_PC8_UM5");
put("PC8UM10Val","SC_VAC_PCOUNTERCB_PC8_UM10");

put("03FLGPC8Val","SC_VAC_PCOUNTERCB_PC8_UM03FLG");
put("05FLGPC8Val","SC_VAC_PCOUNTERCB_PC8_UM05FLG");
put("1FLGPC8Val","SC_VAC_PCOUNTERCB_PC8_UM1FLG");
put("205FLGPC8Val","SC_VAC_PCOUNTERCB_PC8_UM205FLG");
put("5FLGPC8Val","SC_VAC_PCOUNTERCB_PC8_UM5FLG");
put("10FLGPC8Val","SC_VAC_PCOUNTERCB_PC8_UM10FLG");

}};

/***********************************************************************/
Hashtable<String, String> PCOUNTERMOBSTATUS = new Hashtable<String, String>(){

private static final long serialVersionUID = 354054054157L;
{
//
// General Rack
//
//
// PCMOB1
//
put("MOB1InstrStatusStr","SC2_VAC_PCOUNTERMOB1_INSTRST");
put("MOB1InstrStatusCol","SC2_VAC_PCOUNTERMOB1_INSTRST");
put("MOB1StatusStr","SC2_VAC_PCOUNTERMOB1_ST");
put("MOB1StatusCol","SC2_VAC_PCOUNTERMOB1_ST");
put("MOB1FlowStr","SC2_VAC_PCOUNTERMOB1_FLOW");
put("MOB1SamplingStr","SC2_VAC_PCOUNTERMOB1_SAMPLING");
put("MOB1HoldingStr","SC2_VAC_PCOUNTERMOB1_HOLDING");
put("MOB1CycleStr","SC2_VAC_PCOUNTERMOB1_CYCLE");
put("MOB1UM03Val","SC2_VAC_PCOUNTERMOB1_UM03");
put("MOB1UM05Val","SC2_VAC_PCOUNTERMOB1_UM05");
put("MOB1UM1Val","SC2_VAC_PCOUNTERMOB1_UM1");
put("MOB1UM205Val","SC2_VAC_PCOUNTERMOB1_UM205");
put("MOB1UM5Val","SC2_VAC_PCOUNTERMOB1_UM5");
put("MOB1UM10Val","SC2_VAC_PCOUNTERMOB1_UM10");
put("MOB1UM10Val","SC2_VAC_PCOUNTERMOB1_UM10");

put("03FLGMOB1Val","SC2_VAC_PCOUNTERMOB1_UM03FLG");
put("05FLGMOB1Val","SC2_VAC_PCOUNTERMOB1_UM05FLG");
put("1FLGMOB1Val","SC2_VAC_PCOUNTERMOB1_UM1FLG");
put("205FLGMOB1Val","SC2_VAC_PCOUNTERMOB1_UM205FLG");
put("5FLGMOB1Val","SC2_VAC_PCOUNTERMOB1_UM5FLG");
put("10FLGMOB1Val","SC2_VAC_PCOUNTERMOB1_UM10FLG");
//
// PCMOB2
//
put("MOB2InstrStatusStr","SC2_VAC_PCOUNTERMOB2_INSTRST");
put("MOB2InstrStatusCol","SC2_VAC_PCOUNTERMOB2_INSTRST");
put("MOB2StatusStr","SC2_VAC_PCOUNTERMOB2_ST");
put("MOB2StatusCol","SC2_VAC_PCOUNTERMOB2_ST");
put("MOB2FlowStr","SC2_VAC_PCOUNTERMOB2_FLOW");
put("MOB2SamplingStr","SC2_VAC_PCOUNTERMOB2_SAMPLING");
put("MOB2HoldingStr","SC2_VAC_PCOUNTERMOB2_HOLDING");
put("MOB2CycleStr","SC2_VAC_PCOUNTERMOB2_CYCLE");
put("MOB2UM03Val","SC2_VAC_PCOUNTERMOB2_UM03");
put("MOB2UM05Val","SC2_VAC_PCOUNTERMOB2_UM05");
put("MOB2UM1Val","SC2_VAC_PCOUNTERMOB2_UM1");
put("MOB2UM205Val","SC2_VAC_PCOUNTERMOB2_UM205");
put("MOB2UM5Val","SC2_VAC_PCOUNTERMOB2_UM5");
put("MOB2UM10Val","SC2_VAC_PCOUNTERMOB2_UM10");

put("03FLGMOB2Val","SC2_VAC_PCOUNTERMOB2_UM03FLG");
put("05FLGMOB2Val","SC2_VAC_PCOUNTERMOB2_UM05FLG");
put("1FLGMOB2Val","SC2_VAC_PCOUNTERMOB2_UM1FLG");
put("205FLGMOB2Val","SC2_VAC_PCOUNTERMOB2_UM205FLG");
put("5FLGMOB2Val","SC2_VAC_PCOUNTERMOB2_UM5FLG");
put("10FLGMOB2Val","SC2_VAC_PCOUNTERMOB2_UM10FLG");
//
// PCMOB3
//
put("MOB3InstrStatusStr","SC2_VAC_PCOUNTERMOB3_INSTRST");
put("MOB3InstrStatusCol","SC2_VAC_PCOUNTERMOB3_INSTRST");
put("MOB3StatusStr","SC2_VAC_PCOUNTERMOB3_ST");
put("MOB3StatusCol","SC2_VAC_PCOUNTERMOB3_ST");
put("MOB3FlowStr","SC2_VAC_PCOUNTERMOB3_FLOW");
put("MOB3SamplingStr","SC2_VAC_PCOUNTERMOB3_SAMPLING");
put("MOB3HoldingStr","SC2_VAC_PCOUNTERMOB3_HOLDING");
put("MOB3CycleStr","SC2_VAC_PCOUNTERMOB3_CYCLE");
put("MOB3UM03Val","SC2_VAC_PCOUNTERMOB3_UM03");
put("MOB3UM05Val","SC2_VAC_PCOUNTERMOB3_UM05");
put("MOB3UM1Val","SC2_VAC_PCOUNTERMOB3_UM1");
put("MOB3UM205Val","SC2_VAC_PCOUNTERMOB3_UM205");
put("MOB3UM5Val","SC2_VAC_PCOUNTERMOB3_UM5");
put("MOB3UM10Val","SC2_VAC_PCOUNTERMOB3_UM10");

put("03FLGMOB3Val","SC2_VAC_PCOUNTERMOB3_UM03FLG");
put("05FLGMOB3Val","SC2_VAC_PCOUNTERMOB3_UM05FLG");
put("1FLGMOB3Val","SC2_VAC_PCOUNTERMOB3_UM1FLG");
put("205FLGMOB3Val","SC2_VAC_PCOUNTERMOB3_UM205FLG");
put("5FLGMOB3Val","SC2_VAC_PCOUNTERMOB3_UM5FLG");
put("10FLGMOB3Val","SC2_VAC_PCOUNTERMOB3_UM10FLG");

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
put(1,"Flow Err");
put(2,"Laser Err");
put(4,"Error");
put(8,"Int. Err");
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
// IPS Status
//
Hashtable<Integer, String> IPSSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054166L;
{ 
put(0,"ERROR");
put(1,"ON");
put(2,"OFF");
put(255,"?");
}};
Hashtable<Integer,GlgPoint> IPSColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054167L;
{ 
put(0,new GlgPoint(1.,0.,0.));
put(1,new GlgPoint(0.,1.,0.));
put(2,new GlgPoint(1.,0.7,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};


//
// Rearm Status
//
Hashtable<Integer, String> RearmSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054266L;
{ 
put(0,"ERROR");
put(1,"ON");
put(2,"OFF");
put(255,"?");
}};
Hashtable<Integer,GlgPoint> RearmColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054267L;
{ 
put(0,new GlgPoint(1.,0.,0.));
put(1,new GlgPoint(0.,1.,0.));
put(2,new GlgPoint(1.,0.7,0.));
put(255,new GlgPoint(0.5,0.5,0.5));
}};


//
// AC Status
//
Hashtable<Integer, String> ACSTATUS = new Hashtable<Integer, String>(){

private static final long serialVersionUID = 354054054966L;
{ 
put(0,"OFF");
put(1,"LOW NOISE");
put(2,"HIGH NOISE");
put(3,"ERROR");
put(255,"?");
}};
Hashtable<Integer,GlgPoint> ACColorSTATUS = new Hashtable<Integer, GlgPoint>(){

private static final long serialVersionUID = 354054054967L;
{ 
put(0,new GlgPoint(0.,1.,0.));
put(1,new GlgPoint(1.,0.7,0.));
put(2,new GlgPoint(1.,0.2,0.));
put(3,new GlgPoint(1.,0.,0.));
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
