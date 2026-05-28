package com.gluonapplication;

import java.util.Hashtable;
import javafx.scene.paint.Color;

public interface DataTypes {

   public enum Type {
   /**/  LABEL_VALUE_STRING,
   /**/  LABEL_VALUE_2F_STRING,
   /**/  SVGPATH_VALVE_STATUS_COLOR,
   /**/  SVGPATH_VALVE_2_STATUS_COLOR, // Inversed (Giulio CRYOLINK)
   /**/  SVGPATH_VALVE_3_STATUS_COLOR, // Bit rule (Giulio CRYOLINK)
   /**/  LABEL_VALVE_STATUS_STRING,
   /**/  LABEL_INHIBIT_VALVE_STATUS_STRING,
   /**/  LABEL_VALVE_2_STATUS_STRING,  // Inversed (Giulio CRYOLINK)
   /**/  LABEL_VALVE_3_STATUS_STRING,  // Bit rule (Giulio CRYOLINK)
   /**/  LABEL_INHIBIT_VALVE_2_STATUS_STRING, // Inhibit Valve bit rule (Giulio CRYOLINK)
   /**/  RECTANGLE_BUILDING_STATUS_COLOR,
   /**/  RECTANGLE_VENTING_STATUS_COLOR,
   /**/  LABEL_VENTING_STATUS_STRING,
   /**/  LABEL_OPERATION_STATUS_STRING,
   /**/  RECTANGLE_REMOTE_PUMPING_STATUS_COLOR,
   /**/  LABEL_REMOTE_PUMPING_STATUS_STRING,
   /**/  LABEL_RACK_STATUS_STRING,
   /**/  LABEL_RACK_STATUS_COLOR,
   /**/  LABEL_RACK_2_STATUS_COLOR,
   /**/  CIRCLE_GAUGE_STATUS_COLOR,
   /**/  LABEL_MAXIGAUGE_PRESSURE_STATUS_STRING,
   /**/  LABEL_MAXIGAUGE_SENSOR_STATUS_STRING,
   /**/  CIRCLE_XGSGAUGE_STATUS_COLOR,
   /**/  LABEL_XGSGAUGE_EMISSION_STATUS_STRING,
   /**/  LABEL_XGSGAUGE_FILAMENT_STATUS_STRING,
   /**/  LABEL_XGSGAUGE_DEGAS_STATUS_STRING,
   /**/  LABEL_ITRGAUGE_SENSOR_STATUS_STRING,
   /**/  LABEL_ITRGAUGE_EMISSION_STATUS_STRING,
   /**/  LABEL_ITRGAUGE_DEGAS_STATUS_STRING,
   /**/  CIRCLE_ITRGAUGE_STATUS_COLOR,
   /**/  RECTANGLE_PUMP_STATUS_COLOR,
   /**/  LABEL_DRY_STATUS_STRING,
   /**/  LABEL_TURBO_STATUS_STRING,
   /**/  LABEL_TURBO_STANDBY_STATUS_STRING,
   /**/  LABEL_TURBO_TEMP_STATUS_STRING,
   /**/  LABEL_TURBO_TEMP_BOX_STATUS_STRING,
   /**/  LABEL_TURBO_BOX_STATUS_STRING,
   /**/  LABEL_TURBO_BOX_2_STATUS_STRING,
   /**/  LABEL_TURBO_POWER_STATUS_STRING,
   /**/  LABEL_TURBOT_LOW_SPEED_STATUS_STRING,
   /**/  LABEL_TURBOT_NORMAL_SPEED_STATUS_STRING,
   /**/  LABEL_TURBOT_RAMP_STATUS_STRING,
   /**/  LABEL_TURBOT_BOX_STATUS_STRING,
   /**/  LABEL_IONIC_STATUS_STRING,
   /**/  LABEL_IONIC_STATUS_ERROR_STRING,
   /**/  LABEL_IONIC_COMM_STATUS_STRING,
   /**/  LABEL_IONIC_OPERATING_MODE_STRING,
   /**/  LABEL_IONIC_VOLTAGE_MODE_STRING,
   /**/  LABEL_TITANE_STATUS_STRING,
   /**/  LABEL_TITANE_CONTROLLER_STATUS_STRING,
   /**/  LABEL_TITANE_FILAMENT_STATUS_STRING,
   /**/  LABEL_TITANE_RECOVER_STATUS_STRING,
   /**/  LABEL_TITANE_AUTOSTART_STATUS_STRING,
   /**/  LABEL_TITANE_OPERATING_MODE_STRING,
   /**/  CIRCLE_RGA_STATUS_COLOR,
   /**/  CIRCLE_RGA_2_STATUS_COLOR,
   /**/  RGA_WEB_DATA,
   /**/  RGA_CHART_DATA,
   /**/  RGA_2_CHART_DATA,
   /**/  PLOT_DATA,
   /**/  RECTANGLE_FILL_LEVEL,
   /**/  ELLIPSE_FILL_LEVEL,
   /**/  SVGPATH_FILL_LEVEL,
   /**/  CIRCLE_GOLDEN_STATUS_COLOR,
   /**/  CIRCLE_O2SENSOR_STATUS_COLOR,
   /**/  LABEL_GOLDEN_STATUS_STRING,
   /**/  LABEL_SAFE_STATUS_STRING,
   /**/  LABEL_OKFAILURE_STATUS_STRING,
   /**/  LABEL_OKFAILURE_STATUS_COLOR,
   /**/  LABEL_OKFAILURE_STATUS_COLOR_2,
   /**/  LABEL_FAILUREOK_STATUS_STRING,
   /**/  LABEL_FAILUREOK_STATUS_COLOR,
   /**/  LABEL_FAILUREOK_STATUS_COLOR_2,
   /**/  LABEL_ACTIVEOFF_STATUS_STRING,
   /**/  LABEL_ACTIVEOFF_STATUS_COLOR,
   /**/  LABEL_OFFACTIVE_STATUS_STRING,
   /**/  LABEL_OFFACTIVE_STATUS_COLOR,
   /**/  LABEL_ONOFF_STATUS_STRING,
   /**/  LABEL_ONOFF_STATUS_COLOR,
   /**/  LABEL_ONOFF_2_STATUS_STRING,
   /**/  LABEL_ONOFF_2_STATUS_COLOR,
   /**/  LABEL_ONOFF_3_STATUS_STRING,
   /**/  LABEL_ONOFF_3_STATUS_COLOR,
   /**/  LABEL_ONOFF_4_STATUS_STRING,
   /**/  LABEL_ONOFF_4_STATUS_COLOR,
   /**/  LABEL_OFFON_STATUS_STRING,
   /**/  LABEL_OFFON_STATUS_COLOR,
   /**/  LABEL_VALUE_2E_STRING,
   /**/  LABEL_CRYO_STATUS_STRING,
   /**/  LABEL_REFILL_MODE_STATUS_STRING,
   /**/  LABEL_FANSPEED_STATUS_STRING,
   /**/  LABEL_FANSPEED_STATUS_COLOR,
   /**/  LABEL_OKWORKING_STATUS_STRING,
   /**/  LABEL_OKWORKING_STATUS_COLOR,
   /**/  LABEL_OKLOW_STATUS_STRING,
   /**/  LABEL_OKLOW_STATUS_COLOR,
   /**/  RECTANGLE_LOCALCTRL_STATUS_COLOR,
   /**/  CIRCLE_YAG_STATUS_COLOR,
   /**/  CIRCLE_CO2_STATUS_COLOR,
   /**/  CIRCLE_GREEN_STATUS_COLOR,
   /**/  SHUTTER_GREEN_STATUS_COLOR,
   /**/  SHUTTER_CO2_STATUS_COLOR,
   /**/  CIRCLE_SOURCE_YAG_STATUS_COLOR,
   /**/  CIRCLE_SOURCE_GREEN_STATUS_COLOR,
   /**/  CIRCLE_SOURCE_CO2_STATUS_COLOR,
   /**/  CIRCLE_SQZ_LOCK_STATUS_COLOR,
   /**/  SHUTTER_SQZ_FAST_STATUS_COLOR
   }

   String LABEL_STRING_STYLE = "-fx-border-color: #cfcfcf; -fx-font-size: 11; -fx-border-width: 1; -fx-font-weight: normal; -fx-border-radius: 5; -fx-text-fill: ";
   String LABEL_BACKGROUND_STYLE = "-fx-background-radius: 5; -fx-border-radius: 5;  -fx-background-color: ";
   String RECTANGLE_FILL_STYLE = "-fx-arc-height: 20; -fx-arc-width: 20; -fx-fill: linear-gradient(to top, ";
   String FILL_STYLE = "-fx-fill: linear-gradient(to top, ";

   Hashtable<String, String> DRY_CHANNELS = new Hashtable<String, String>(){{
      // Status
      put("StatusP22", "_P22ST");
      put("StatusP41", "_P41ST");
      put("StatusP61", "_P61ST");
      put("StatusP1", "_P1ST");
      put("StatusPR1", "_PR1ST");
      put("StatusPR2", "_PR2ST");
   }};

   Hashtable<String, String> TURBO_CHANNELS = new Hashtable<String, String>(){{
      // Status
      put("StatusP21", "_P21ST");
      put("StatusP51", "_P51ST");
      put("StatusP71", "_P71ST");
      put("StatusP2", "_P2ST");
      // Status Standby
      put("StatusStandbyP21", "_P21STYST");
      put("StatusStandbyP51", "_P51STYST");
      put("StatusStandbyP71", "_P71STYST");
      put("StatusStandbyP2", "_P2STYST");
      // Status Temperature
      put("StatusTempP21", "_P21TPST");
      put("StatusTempP51", "_P51TPST");
      put("StatusTempP71", "_P71TPST");
      put("StatusTempP2", "_P2TPST");
      // Status Temperature Box
      put("StatusTempBoxP21", "_P21BTPST");
      put("StatusTempBoxP51", "_P51BTPST");
      put("StatusTempBoxP71", "_P71BTPST");
      put("StatusTempBoxP2", "_P2BTPST");
      // Status Box
      put("StatusBoxP21", "_P21BERR");
      put("StatusBoxP51", "_P51BERR");
      put("StatusBoxP71", "_P71BERR");
      put("StatusBoxP2", "_P2BERR");
      // Actual Speed
      put("ActSpeedP21", "_P21_SPEED");
      put("ActSpeedP51", "_P51_SPEED");
      put("ActSpeedP71", "_P71_SPEED");
      put("ActSpeedP2", "_P2_SPEED");
      // Final Speed
      put("FinalSpeedP21", "_P21_FSPEED");
      put("FinalSpeedP51", "_P51_FSPEED");
      put("FinalSpeedP71", "_P71_FSPEED"); 
      put("FinalSpeedP2", "_P2_FSPEED"); 
      // Operating hours 
      put("OperatingTimeP21", "_P21HR");
      put("OperatingTimeP51", "_P51HR");
      put("OperatingTimeP71", "_P71HR");
      put("OperatingTimeP2", "_P2HR");
      // Actual Power
      put("ActPowerP21", "_P21PWR");
      put("ActPowerP51", "_P51W");
      put("ActPowerP71", "_P71W");
      put("ActPowerP2", "_P2W");
   }};
   Hashtable<String, String> TURBO_TUBE_LAL_CHANNELS = new Hashtable<String, String>(){{
      put("StatusBoxP21", "_P21ERR");
      put("ActPowerP21", "_P21W");
   }};

   Hashtable<String, String> TURBOT_CHANNELS = new Hashtable<String, String>(){{
      // Status
      put("StatusTP1", "_TP1ST");
      // Status Box
      put("StatusBoxTP1", "_TP1CTRLST");
      // Status Normal Speed
      put("StatusNormalSpeedTP1", "_TP1NORMSPEEDST");
      // Status Low Speed
      put("StatusLowSpeedTP1", "_TP1LOWSPEEDST");
      // Status Ramp
      put("StatusRampTP1", "_TP1STARTST");
      // Status
      put("StatusTP2", "_TP2ST");
      // Status Box
      put("StatusBoxTP2", "_TP2CTRLST");
      // Status Normal Speed
      put("StatusNormalSpeedTP2", "_TP2NORMSPEEDST");
      // Status Low Speed
      put("StatusLowSpeedTP2", "_TP2LOWSPEEDST");
      // Status Ramp
      put("StatusRampTP2", "_TP2STARTST");
   }};

   Hashtable<String, String> IONIC_CHANNELS = new Hashtable<String, String>(){{
      // Status
      put("StatusP33", "_P33ST");
      put("StatusP23", "_P23ST");
      put("StatusP81", "_P81ST");
      put("StatusP33i", "_P33IST");
      put("StatusP33e", "_P33EST");
      // Status Comm
      put("StatusCommP33", "_P33REMOTEST");
      put("StatusCommP23", "_P23REMOTEST");
      put("StatusCommP81", "_P81REMOTEST");
      put("StatusCommP33i", "_P33IREMOTEST");
      put("StatusCommP33e", "_P33EREMOTEST");
      // StatusError
      put("StatusErrorP33", "_P33ERRORST");
      put("StatusErrorP23", "_P23ERRORST");
      put("StatusErrorP81", "_P81ERRORST");
      put("StatusErrorP33i", "_P33IERRORST"); // CHANNEL DOES NOT EXIST
      put("StatusErrorP33e", "_P33EERRORST"); // CHANNEL DOES NOT EXIST
      // Operating Mode
      put("OperatingModeP33", "_P33OPMODE");
      put("OperatingModeP23", "_P23OPMODE");
      put("OperatingModeP81", "_P81OPMODE");
      put("OperatingModeP33i", "_P33IOPMODE");
      put("OperatingModeP33e", "_P33EOPMODE");
      // Voltage Mode
      put("VoltageModeP33", "_P33VOLTMODE");
      put("VoltageModeP23", "_P23VOLTMODE");
      put("VoltageModeP81", "_P81VOLTMODE");
      put("VoltageModeP33i", "_P33IVOLTMODE");
      put("VoltageModeP33e", "_P33EVOLTMODE");
      // Protect Current
      put("ProtectCurrentP33", "_P33PRTCUR");
      put("ProtectCurrentP23", "_P23PRTCUR");
      put("ProtectCurrentP81", "_P81PRTCUR");
      put("ProtectCurrentP33i", "_P33IPRTCUR");
      put("ProtectCurrentP33e", "_P33EPRTCUR");
      // Pressure
      put("PressureP33", "_P33P");
      put("PressureP23", "_P23P");
      put("PressureP81", "_P81P");
      put("PressureP33i", "_P33IP");
      put("PressureP33e", "_P33EP");
      // Absorbed Current
      put("AbsorbedCurrentP33", "_P33ABSCUR");
      put("AbsorbedCurrentP23", "_P23ABSCUR");
      put("AbsorbedCurrentP81", "_P81ABSCUR");
      put("AbsorbedCurrentP33i", "_P33IABSCUR");
      put("AbsorbedCurrentP33e", "_P33EABSVOLT");
      // Absorbed Voltage
      put("AbsorbedVoltageP33", "_P33ABSVOLT");
      put("AbsorbedVoltageP23", "_P23ABSVOLT");
      put("AbsorbedVoltageP81", "_P81ABSVOLT");
      put("AbsorbedVoltageP33i", "_P33IABSVOLT");
      put("AbsorbedVoltageP33e", "_P33EABSVOLT");
      // Max Current
      put("MaxCurrentP33", "_P33MAXCUR");
      put("MaxCurrentP23", "_P23MAXCUR");
      put("MaxCurrentP81", "_P81MAXCUR");
      put("MaxCurrentP33i", "_P33IMAXCUR");
      put("MaxCurrentP33e", "_P33EMAXCUR");
      // Max Voltage
      put("MaxVoltageP33", "_P33MAXVOLT");
      put("MaxVoltageP23", "_P23MAXVOLT");
      put("MaxVoltageP81", "_P81MAXVOLT");
      put("MaxVoltageP33i", "_P33IMAXVOLT");
      put("MaxVoltageP33e", "_P33EMAXVOLT");
      // Max Power
      put("MaxPowerP33", "_P33MAXW");
      put("MaxPowerP23", "_P23MAXW");
      put("MaxPowerP81", "_P81MAXW");
      put("MaxPowerP33i", "_P33IMAXW");
      put("MaxPowerP33e", "_P33EMAXW");
      // First Step Voltage
      put("FirstStepVoltageP33", "_P331STEPVOLT");
      put("FirstStepVoltageP23", "_P231STEPVOLT");
      put("FirstStepVoltageP81", "_P811STEPVOLT");
      put("FirstStepVoltageP33i", "_P33I1STEPVOLT");
      put("FirstStepVoltageP33e", "_P33E1STEPVOLT");
      // Second Step Voltage
      put("SecondStepVoltageP33", "_P332STEPVOLT");
      put("SecondStepVoltageP23", "_P232STEPVOLT");
      put("SecondStepVoltageP81", "_P812STEPVOLT");
      put("SecondStepVoltageP33i", "_P33I2STEPVOLT");
      put("SecondStepVoltageP33e", "_P33E2STEPVOLT");
      // First Step Current
      put("FirstStepCurrentP33", "_P331STEPCUR");
      put("FirstStepCurrentP23", "_P231STEPCUR");
      put("FirstStepCurrentP81", "_P811STEPCUR");
      put("FirstStepCurrentP33i", "_P33I1STEPCUR");
      put("FirstStepCurrentP33e", "_P33E1STEPCUR");
      // Second Step Current
      put("SecondStepCurrentP33", "_P332STEPCUR");
      put("SecondStepCurrentP23", "_P232STEPCUR");
      put("SecondStepCurrentP81", "_P812STEPCUR");
      put("SecondStepCurrentP33i", "_P33I2STEPCUR");
      put("SecondStepCurrentP33e", "_P33E2STEPCUR");
      
   }};

   Hashtable<String, String> TITANE_CHANNELS = new Hashtable<String, String>(){{
      // Status
      put("StatusP31", "_P31ST");
      put("StatusP32", "_P32ST");
      put("StatusP31e", "_P31EST");
      put("StatusP32e", "_P32EST");
      put("StatusP31i", "_P31IST");
      put("StatusP32i", "_P32IST");
      // Status Filament
      put("StatusFilamentP31", "_P31FILST");
      put("StatusFilamentP32", "_P32FILST");
      put("StatusFilamentP31e", "_P31EFILST");
      put("StatusFilamentP32e", "_P32EFILST");
      put("StatusFilamentP31i", "_P31IFILST");
      put("StatusFilamentP32i", "_P32IFILST");
      // Filament Used
      put("FilamentUsedP31", "_P31FILUSED");
      put("FilamentUsedP32", "_P32FILUSED");
      put("FilamentUsedP31e", "_P31EFILUSED");
      put("FilamentUsedP32e", "_P32EFILUSED");
      put("FilamentUsedP31i", "_P31IFILUSED");
      put("FilamentUsedP32i", "_P32IFILUSED");
      // Status Controller
      put("StatusControllerP31", "_P31CONTROLST");
      put("StatusControllerP32", "_P32CONTROLST");
      put("StatusControllerP31e", "_P31ECONTROLST");
      put("StatusControllerP32e", "_P32ECONTROLST");
      put("StatusControllerP31i", "_P31ICONTROLST");
      put("StatusControllerP32i", "_P32ICONTROLST");
      // Status Recover
      put("StatusRecoverP31", "_P31RECOST");
      put("StatusRecoverP32", "_P32RECOST");
      put("StatusRecoverP31e", "_P31ERECOST");
      put("StatusRecoverP32e", "_P32ERECOST");
      put("StatusRecoverP31i", "_P31IRECOST");
      put("StatusRecoverP32i", "_P32IRECOST");
      // Status Autostart
      put("StatusAutostartP31", "_P31ASTARTST");
      put("StatusAutostartP32", "_P32ASTARTST");
      put("StatusAutostartP31e", "_P31EASTARTST");
      put("StatusAutostartP32e", "_P32EASTARTST");
      put("StatusAutostartP31i", "_P31IASTARTST");
      put("StatusAutostartP32i", "_P32IASTARTST");
      // Operating Mode
      put("OperatingModeP31", "_P31OPMODE");
      put("OperatingModeP32", "_P32OPMODE");
      put("OperatingModeP31e", "_P31EOPMODE");
      put("OperatingModeP32e", "_P32EOPMODE");
      put("OperatingModeP31i", "_P31IOPMODE");
      put("OperatingModeP32i", "_P32IOPMODE");
      // Absorbed Current
      put("AbsorbedCurrentP31", "_P31ABSCUR");
      put("AbsorbedCurrentP32", "_P32ABSCUR");
      put("AbsorbedCurrentP31e", "_P31EABSCUR");
      put("AbsorbedCurrentP32e", "_P32EABSCUR");
      put("AbsorbedCurrentP31i", "_P31IABSCUR");
      put("AbsorbedCurrentP32i", "_P32IABSCUR");
      // Absorbed Voltage
      put("AbsorbedVoltageP31", "_P31ABSVOLT");
      put("AbsorbedVoltageP32", "_P32ABSVOLT");
      put("AbsorbedVoltageP31e", "_P31EABSVOLT");
      put("AbsorbedVoltageP32e", "_P32EABSVOLT");
      put("AbsorbedVoltageP31i", "_P31IABSVOLT");
      put("AbsorbedVoltageP32i", "_P32IABSVOLT");
      // Sublimation Current
      put("SublimationCurrentP31", "_P31SUBLCUR");
      put("SublimationCurrentP32", "_P32SUBLCUR");
      put("SublimationCurrentP31e", "_P31ESUBLCUR");
      put("SublimationCurrentP32e", "_P32ESUBLCUR");
      put("SublimationCurrentP31i", "_P31ISUBLCUR");
      put("SublimationCurrentP32i", "_P32ISUBLCUR");
      // Sublimation Time
      put("SublimationTimeP31", "_P31SUBLTIME");
      put("SublimationTimeP32", "_P32SUBLTIME");
      put("SublimationTimeP31e", "_P31ESUBLTIME");
      put("SublimationTimeP32e", "_P32ESUBLTIME");
      put("SublimationTimeP31i", "_P31ISUBLTIME");
      put("SublimationTimeP32i", "_P32ISUBLTIME");
      // Sublimation Period
      put("SublimationPeriodP31", "_P31SUBLPERIOD");
      put("SublimationPeriodP32", "_P32SUBLPERIOD");
      put("SublimationPeriodP31e", "_P31ESUBLPERIOD");
      put("SublimationPeriodP32e", "_P32ESUBLPERIOD");
      put("SublimationPeriodP31i", "_P31ISUBLPERIOD");
      put("SublimationPeriodP32i", "_P32ISUBLPERIOD");
      
   }};
   
   Hashtable<String, String> TUBESTATION_VALVE_CHANNELS = new Hashtable<String, String>(){{
      // TubeStation Valves
      put("StatusVa1", "_VA1ST");
      put("StatusVa2", "_VA2ST");
      put("StatusV21", "_V21ST");
      put("StatusV22", "_V22ST");
      put("StatusV23", "_V23ST");
      put("StatusV31", "_V31ST");
      put("StatusV32", "_V32ST");
      put("StatusV91", "_V91ST");
      put("StatusV11", "_V11ST");
      put("StatusV25", "_V25ST");
   }};

   Hashtable<String, String> UPS_CHANNELS = new Hashtable<String, String>(){{
      // UPS Racks
      put("NE1", "INF_NE_UPS1_COM_ST");
      put("NE2", "INF_NE_UPS2_COM_ST");
      put("WE1", "INF_WE_UPS1_COM_ST");
      put("WE2", "INF_WE_UPS2_COM_ST");
      put("TOWERMC1", "INF_MC_UPS1_COM_ST");
      put("TOWERMC2", "INF_MC_UPS2_COM_ST");
      put("TUBE300N", "---");
      put("TUBELAL600N", "INF_N600_UPS_COM_ST");
      put("TUBELAL1200N", "INF_N1200_UPS_COM_ST");
      put("TUBELAL1800N", "INF_N1800_UPS_COM_ST");
      put("TUBELAL2400N", "INF_N2400_UPS_COM_ST");
      put("TUBELAL3000N", "---");
      put("TUBE300W", "---");
      put("TUBELAL600W", "INF_W600_UPS_COM_ST");
      put("TUBELAL1200W", "INF_W1200_UPS_COM_ST");
      put("TUBELAL1800W", "INF_W1800_UPS_COM_ST");
      put("TUBELAL2400W", "INF_W2400_UPS_COM_ST");
      put("TUBELAL3000W", "---");
      put("TUBEMC", "---");
      put("TB3", "INF_TB_UPS3_COM_ST");
      put("SQZ300N", "INF_N330_UPS_COM_ST");
      put("SQZ200N", "INF_N230_UPS_COM_ST");
      put("SQZ100N", "INF_N180_UPS_COM_ST");
      put("SQZ0N", "INF_N130_UPS_COM_ST");
      put("SQZDET2", "INF_N65_UPS_COM_ST");      
      // UPS BATTERY STATUS
      put("NE1_BATTERY_WORK", "INF_NE_UPS1_BATTERY_WORK_ST");
      put("NE2_BATTERY_WORK", "INF_NE_UPS2_BATTERY_WORK_ST");
      put("WE1_BATTERY_WORK", "INF_WE_UPS1_BATTERY_WORK_ST");
      put("WE2_BATTERY_WORK", "INF_WE_UPS2_BATTERY_WORK_ST");
      put("TOWERMC1_BATTERY_WORK", "INF_MC_UPS1_BATTERY_WORK_ST");
      put("TOWERMC2_BATTERY_WORK", "INF_MC_UPS2_BATTERY_WORK_ST");
      put("TUBE300N_BATTERY_WORK", "---");
      put("TUBELAL600N_BATTERY_WORK", "INF_N600_UPS_BATTERY_WORK_ST");
      put("TUBELAL1200N_BATTERY_WORK", "INF_N1200_UPS_BATTERY_WORK_ST");
      put("TUBELAL1800N_BATTERY_WORK", "INF_N1800_UPS_BATTERY_WORK_ST");
      put("TUBELAL2400N_BATTERY_WORK", "INF_N2400_UPS_BATTERY_WORK_ST");
      put("TUBELAL3000N_BATTERY_WORK", "---");
      put("TUBE300W_BATTERY_WORK", "---");
      put("TUBELAL600W_BATTERY_WORK", "INF_W600_UPS_BATTERY_WORK_ST");
      put("TUBELAL1200W_BATTERY_WORK", "INF_W1200_UPS_BATTERY_WORK_ST");
      put("TUBELAL1800W_BATTERY_WORK", "INF_W1800_UPS_BATTERY_WORK_ST");
      put("TUBELAL2400W_BATTERY_WORK", "INF_W2400_UPS_BATTERY_WORK_ST");
      put("TUBELAL3000W_BATTERY_WORK", "---");
      put("TUBEMC_BATTERY_WORK", "---");
      put("TB3_BATTERY_WORK", "INF_TB_UPS3_BATTERY_WORK_ST");
      put("SQZ300N_BATTERY_WORK", "INF_N330_UPS_BATTERY_WORK_ST");
      put("SQZ200N_BATTERY_WORK", "INF_N230_UPS_BATTERY_WORK_ST");
      put("SQZ100N_BATTERY_WORK", "INF_N180_UPS_BATTERY_WORK_ST");
      put("SQZ0N_BATTERY_WORK", "INF_N130_UPS_BATTERY_WORK_ST");
      put("SQZDET2_BATTERY_WORK", "INF_N65_UPS_BATTERY_WORK_ST");
      // UPS BATTERY LOW STATUS
      put("NE1_BATTERY_LOW", "INF_NE_UPS1_BATTERY_LOW_ST");
      put("NE2_BATTERY_LOW", "INF_NE_UPS2_BATTERY_LOW_ST");
      put("WE1_BATTERY_LOW", "INF_WE_UPS1_BATTERY_LOW_ST");
      put("WE2_BATTERY_LOW", "INF_WE_UPS2_BATTERY_LOW_ST");
      put("TOWERMC1_BATTERY_LOW", "INF_MC_UPS1_BATTERY_LOW_ST");
      put("TOWERMC2_BATTERY_LOW", "INF_MC_UPS2_BATTERY_LOW_ST");
      put("TUBE300N_BATTERY_LOW", "---");
      put("TUBELAL600N_BATTERY_LOW", "INF_N600_UPS_BATTERY_LOW_ST");
      put("TUBELAL1200N_BATTERY_LOW", "INF_N1200_UPS_BATTERY_LOW_ST");
      put("TUBELAL1800N_BATTERY_LOW", "INF_N1800_UPS_BATTERY_LOW_ST");
      put("TUBELAL2400N_BATTERY_LOW", "INF_N2400_UPS_BATTERY_LOW_ST");
      put("TUBELAL3000N_BATTERY_LOW", "---");
      put("TUBE300W_BATTERY_LOW", "---");
      put("TUBELAL600W_BATTERY_LOW", "INF_W600_UPS_BATTERY_LOW_ST");
      put("TUBELAL1200W_BATTERY_LOW", "INF_W1200_UPS_BATTERY_LOW_ST");
      put("TUBELAL1800W_BATTERY_LOW", "INF_W1800_UPS_BATTERY_LOW_ST");
      put("TUBELAL2400W_BATTERY_LOW", "INF_W2400_UPS_BATTERY_LOW_ST");
      put("TUBELAL3000W_BATTERY_LOW", "---");
      put("TUBEMC_BATTERY_LOW", "---");      
      put("TB3_BATTERY_LOW", "INF_TB_UPS3_BATTERY_LOW_ST");
      put("SQZ300N_BATTERY_LOW", "INF_N330_UPS_BATTERY_LOW_ST");
      put("SQZ200N_BATTERY_LOW", "INF_N230_UPS_BATTERY_LOW_ST");
      put("SQZ100N_BATTERY_LOW", "INF_N180_UPS_BATTERY_LOW_ST");
      put("SQZ0N_BATTERY_LOW", "INF_N130_UPS_BATTERY_LOW_ST");     
      put("SQZDET2_BATTERY_LOW", "INF_N65_UPS_BATTERY_LOW_ST");  
   }};

   Hashtable<String, String> LINK_VALVE_CHANNELS = new Hashtable<String, String>(){{
      // Link Valves
      put("StatusV21", "_V21ST");
      put("StatusV22", "_V22ST");
      put("StatusV23", "_V23ST");
   }};

   Hashtable<String, String> SQZ_VALVE_CHANNELS = new Hashtable<String, String>(){{
      // MiniTowerSqz Valves
      put("StatusVe1", "_VE1ST");
      put("StatusVe2", "_VE2ST");
      put("StatusV1", "_V1ST");
      put("StatusVP", "_VPST");
      put("StatusVrem", "_VSPAREST");
      put("StatusV24", "_V24ST");
      put("StatusV25", "_V24ST");
      // TubeSqz Valves
      put("StatusVa1", "_VA1ST");
      put("StatusVa2", "_VA2ST");
      put("StatusV21", "_V21ST"); // Common with MiniTowerSqz
      put("StatusV22", "_V22ST"); // Common with MiniTowerSqz
      put("StatusV23", "_V23ST"); // Common with MiniTowerSqz
   }};

   Hashtable<String, String> TOWER_VALVE_CHANNELS = new Hashtable<String, String>(){{
      // Tower Valves
      put("StatusVc1", "_Vc1ST");
      put("StatusVc2", "_Vc2ST");
      put("StatusVd1", "_Vd1ST");
      put("StatusVd2", "_Vd2ST");
      put("StatusV31", "_V31ST");
      put("StatusV32", "_V32ST");
      put("StatusV41", "_V41ST");
      put("StatusV42", "_V42ST");
      put("StatusV43", "_V43ST");
      put("StatusV51", "_V51ST");
      put("StatusV52", "_V52ST");
      put("StatusV53", "_V53ST");
      put("StatusV71", "_V71ST");
      put("StatusV72", "_V72ST");
      put("StatusV73", "_V73ST");
      put("StatusV74", "_V74ST");
      put("StatusV75", "_V75ST");
      put("StatusV81", "_V81ST");
      put("StatusV82", "_V82ST");
      put("StatusV91", "_V91ST");
      put("StatusV92", "_V92ST");
   }};

   Hashtable<String, String> CRYO_VALVE_CHANNELS = new Hashtable<String, String>(){{
      // Cryo Valves
      put("StatusVe1", "_VE1ST");
      put("StatusVe2", "_VE2ST");
      put("StatusV31i", "_V31IST");
      put("StatusV32i", "_V32IST");
      put("StatusV31e", "_V31EST");
      put("StatusV32e", "_V32EST");
      put("StatusV21", "_V21ST");
      put("StatusV22", "_V22ST");
      put("StatusV1", "_V1ST");
      put("StatusV23", "_V23ST");
      put("StatusVCryo", "_VCRYOST");
   }};
   
   Hashtable<String, String> CRYOLINK_VALVE_CHANNELS = new Hashtable<String, String>(){{
      // Cryo Valves
      put("StatusV21", "_V21ST");
      put("StatusV22", "_V22ST");
      put("StatusVs1", "_Vs1");
      put("StatusVs2", "_Vs2");
      put("StatusCV1", "_Z_10_DOUT");
      put("StatusCV2", "_Z_10_DOUT");
   }};

   Hashtable<String, String> REMOTEPUMPS_VALVE_CHANNELS = new Hashtable<String, String>(){{
      // RemotePumps Valves
      put("StatusV1","_V1ST");
      put("StatusV2","_V2ST");
      put("StatusVpr","_VPRST");
      put("StatusVls","_VLSST");
      put("StatusVbs","_VBSST");
      put("StatusVlp","_VLPST");
      put("StatusVsr","_VSRST");
      put("StatusVni","_VNIST");
      put("StatusVwi","_VWIST");
      put("StatusVib","_VIBST");
      put("StatusVdt","_VDTST");
      put("StatusVmc","_VMCST");
      put("StatusVs1","_VS1ST");
      put("StatusVs2","_VS2ST");
      put("StatusVs3","_VS3ST");
      put("StatusVb","_VBST");
   }};

   Hashtable<String, String> VENTING_VALVE_CHANNELS = new Hashtable<String, String>(){{
      // Venting valves
      put("StatusVBypass", "_M1_VBYPASSST");
      put("StatusVMain", "_M1_VENTST");
      put("StatusVSoft", "_M1_VENTST");
      put("StatusVRP", "_M1_VRPST");
      put("StatusVDryer", "_M1_VDRYERST");
      put("StatusV1", "_M1_V1ST");
   }};

   Hashtable<String, String> TUBESTATION_PRESSURE_EGO_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PressureGa1", "_PR3");
      put("PressureGa2", "_PR5");
      put("PressureGa3", "_PR4");
      put("PressureG21", "_PR2");
      put("PressureG22", "_PR1");
      put("PressureG31", "_PR6");
      put("StatusPressureGa1", "_PR3ST");
      put("StatusPressureGa2", "_PR5ST");
      put("StatusPressureGa3", "_PR4ST");
      put("StatusPressureG21", "_PR2ST");
      put("StatusPressureG22", "_PR1ST");
      put("StatusPressureG31", "_PR6ST");
      put("StatusSensorGa1", "_PR3SST");
      put("StatusSensorGa2", "_PR5SST");
      put("StatusSensorGa3", "_PR4SST");
      put("StatusSensorG21", "_PR2SST");
      put("StatusSensorG22", "_PR1SST");
      put("StatusSensorG31", "_PR6SST");
      // XGS Gauges
      put("PressureGa4", "_PR7");
      put("StatusEmissionGa4", "_PR7ST");
      put("StatusFilamentGa4", "_PR7FST");
      put("StatusDegasGa4", "_PR7DST");
   }};

   Hashtable<String, String> CRYOLINKDET_PRESSURE_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PressureG21", "_?");
      put("PressureG22", "_PR1");
      put("StatusPressureG21", "_?");
      put("StatusPressureG22", "_PR1ST");
      put("StatusSensorG21", "_?");
      put("StatusSensorG22", "_PR1SST");
   }};

   Hashtable<String, String> CRYOLINKIB_PRESSURE_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PressureG21", "_?");
      put("PressureG22", "_PR2");
      put("StatusPressureG21", "_?");
      put("StatusPressureG22", "_PR2ST");
      put("StatusSensorG21", "_?");
      put("StatusSensorG22", "_PR2SST");
   }};

   Hashtable<String, String> LINKPR_PRESSURE_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PressureG21", "_PR3");
      put("PressureG22", "_PR1");
      put("StatusPressureG21", "_PR3ST");
      put("StatusPressureG22", "_PR1ST");
      put("StatusSensorG21", "_PR3SST");
      put("StatusSensorG22", "_PR1SST");
   }};

   Hashtable<String, String> LINKSR_PRESSURE_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PressureG21", "_PR4");
      put("PressureG22", "_PR2");
      put("StatusPressureG21", "_PR4ST");
      put("StatusPressureG22", "_PR2ST");
      put("StatusSensorG21", "_PR4SST");
      put("StatusSensorG22", "_PR2SST");
   }};

   Hashtable<String, String> SQZ_PRESSURE_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges TUBE SQZ
      put("PressureGa1", "_PR3");
      put("PressureGa2", "_PR5");
      put("PressureGa3", "_PR4");
      put("PressureG21", "_PR2"); // SAME FOR BOTH TUBE & MINITOWER !
      put("PressureG22", "_PR1"); // SAME FOR BOTH TUBE & MINITOWER !
      put("PressureGMid", "_PR6"); // GMid
      put("StatusPressureGa1", "_PR3ST");
      put("StatusPressureGa2", "_PR5ST");
      put("StatusPressureGa3", "_PR4ST");
      put("StatusPressureG21", "_PR2ST"); // SAME FOR BOTH TUBE & MINITOWER !
      put("StatusPressureG22", "_PR1ST"); // SAME FOR BOTH TUBE & MINITOWER !
      put("StatusPressureGMid", "_PR6ST"); // GMid
      put("StatusSensorGa1", "_PR3SST");
      put("StatusSensorGa2", "_PR5SST");
      put("StatusSensorGa3", "_PR4SST");
      put("StatusSensorG21", "_PR2SST"); // SAME FOR BOTH TUBE & MINITOWER !
      put("StatusSensorG22", "_PR1SST"); // SAME FOR BOTH TUBE & MINITOWER !
      put("StatusSensorGMid", "_PR6SST"); // GMid
      // MaxiGauges MINITOWER SQZ
      put("PressureGe1", "_PR3");
      put("PressureGe2", "_PR5");
      put("PressureGe3", "_PR4");
      put("PressureGP", "_PR6"); 
      put("StatusPressureGe1", "_PR3ST");
      put("StatusPressureGe2", "_PR5ST");
      put("StatusPressureGe3", "_PR4ST");
      put("StatusPressureGP", "_PR6ST"); 
      put("StatusSensorGe1", "_PR3SST");
      put("StatusSensorGe2", "_PR5SST");
      put("StatusSensorGe3", "_PR4SST");
      put("StatusSensorGP", "_PR6SST"); 
   }};

   Hashtable<String, String> TUBESTATION_PRESSURE_LAL_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PressureGa1", "_MG1_CH4");
      put("PressureGa2", "_MG1_CH5");
      put("PressureGa3", "_MG1_CH6");
      put("PressureG21", "_MG1_CH1");
      put("PressureG22", "_MG1_CH2");
      put("PressureG31", "_MG1_CH3");
      put("StatusPressureGa1", "_MG1_CH4ST");
      put("StatusPressureGa2", "_MG1_CH5ST");
      put("StatusPressureGa3", "_MG1_CH6ST");
      put("StatusPressureG21", "_MG1_CH1ST");
      put("StatusPressureG22", "_MG1_CH2ST");
      put("StatusPressureG31", "_MG1_CH3ST");
      put("StatusSensorGa1", "_MG1_CH4SST");
      put("StatusSensorGa2", "_MG1_CH5SST");
      put("StatusSensorGa3", "_MG1_CH6SST");
      put("StatusSensorG21", "_MG1_CH1SST");
      put("StatusSensorG22", "_MG1_CH2SST");
      put("StatusSensorG31", "_MG1_CH3SST");
      // XGS Gauges
      put("PressureGa4", "_Ga4");
      put("StatusEmissionGa4", "_Ga4ST");
      put("StatusFilamentGa4", "_Ga4FST");
      put("StatusDegasGa4", "_Ga4DST");
   }};

   Hashtable<String, String> PLOT_CHANNELS = new Hashtable<String, String>(){{
      // Tower Pressures
      put("PlotG41", "_MG1_CH1");
      put("PlotGc1", "_MG1_CH2");
      put("PlotG72", "_MG1_CH3");
      put("PlotGd1", "_MG1_CH4");
      put("PlotGc2", "_MG1_CH5");
      put("PlotG51", "_MG1_CH6");
      put("PlotG61", "_MG2_CH1");
      put("PlotGd2", "_MG2_CH2");
      put("PlotG31", "_MG2_CH3");
      put("PlotG71", "_MG2_CH4");
      put("PlotG81", "_MG2_CH5");
      put("PlotGc3", "_GC3");
      put("PlotGd3", "_GD3");
      // REMOTE SCROLL Pressures
      put("PlotG1","_MG1_CH1");
      put("PlotG2","_MG1_CH2");
      put("PlotG3","_MG1_CH3");
      put("PlotG4","_MG2_CH3");
      put("PlotG5","_MG2_CH4");
      put("PlotGpr","_MG3_CH2");
      put("PlotGbs","_MG3_CH4");
      put("PlotGls","_MG1_CH6");
      put("PlotGlp","_MG3_CH3");
      put("PlotGsr","_MG1_CH5");
      put("PlotGni","_MG2_CH1");
      put("PlotGwi","_MG3_CH5");
      put("PlotGib","_MG3_CH1");
      put("PlotGdt","_MG1_CH4");
      put("PlotGmc","_MG2_CH5");
      put("PlotGs1","_MG2_CH2");
      put("PlotGs2","_MG2_CH6");
      // Turbo speed
      put("PlotP51", "_P51_SPEED");
      put("PlotP71", "_P71_SPEED");
   }};

   Hashtable<String, String> PLOT_BENCHTOWER_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PlotG41", "_MG1_CH1");
      put("PlotGc1", "_MG1_CH2");
      put("PlotGc2", "_MG1_CH3");
      put("PlotG51", "_MG1_CH4");
      put("PlotG61", "_MG1_CH5");
      put("PlotGc3", "_GC3");
      // Turbo speed
      put("PlotP51", "_P51_SPEED");
   }};

   
   Hashtable<String, String> PLOT_TUBE_LAL_CHANNELS = new Hashtable<String, String>(){{
      // TUBE LAL Pressures
      put("PlotGa1", "_MG1_CH4");
      put("PlotGa2", "_MG1_CH5");
      put("PlotGa3", "_MG1_CH6");
      put("PlotG21", "_MG1_CH1");
      put("PlotG22", "_MG1_CH2");
      put("PlotG31", "_MG1_CH3");
      put("PlotGa4", "_Ga4");
      put("PlotP21", "_P21_SPEED");
   }};

   Hashtable<String, String> PLOT_TUBE_EGO_CHANNELS = new Hashtable<String, String>(){{
      // TUBE EGO Pressures
      put("PlotGa1", "_PR3");
      put("PlotGa2", "_PR5");
      put("PlotGa3", "_PR4");
      put("PlotG21", "_PR2");
      put("PlotG22", "_PR1");
      put("PlotG31", "_PR6");
      put("PlotGa4", "_PR7");
      put("PlotP21", "_P21_SPEED");
   }};

   Hashtable<String, String> PLOT_LINKPR_CHANNELS = new Hashtable<String, String>(){{
      // TUBE SQZ Pressures
      put("PlotG21", "_PR3");
      put("PlotG22", "_PR1");
      put("PlotP21", "_P21_SPEED");
   }};

   Hashtable<String, String> PLOT_LINKSR_CHANNELS = new Hashtable<String, String>(){{
      // TUBE SQZ Pressures
      put("PlotG21", "_PR4");
      put("PlotG22", "_PR2");
      put("PlotP21", "_P21_SPEED");
   }};

   Hashtable<String, String> PLOT_CRYOLINK_CHANNELS = new Hashtable<String, String>(){{
      // CRYOLINK Pressures
      put("PlotGs1", "_Gs1");
      put("PlotGs2", "_Gs2");
      put("PlotGs3", "_Gs3");
      put("PlotGs4", "_Gs4");
      put("PlotGs5", "_Gs5");
      put("PlotG21", "_?");
      put("PlotG22DET", "_PR1");
      put("PlotG22IB", "_PR2");
      // CRYOLINK TURBO SPEED
      put("PlotP21", "_P21_SPEED");
      // CRYOLINK TEMPERATURES
      put("PlotTF", "_TF");
      put("PlotTN", "_TN");
      put("PlotTL", "_TL");
      put("PlotT1", "_T1");
      put("PlotT2", "_T2");
      put("PlotT3", "_T3");
      put("PlotT4", "_T4");
      put("PlotT5", "_T5");
      put("PlotT6", "_T6");
      put("PlotT7", "_T7");
      put("PlotT8", "_T8");
      put("PlotT12", "_T12");
      // CRYOLINK LEVEL
      put("PlotLevel", "_LEVEL");
   }};

   Hashtable<String, String> PLOT_SQZ_CHANNELS = new Hashtable<String, String>(){{
      // TUBE SQZ Pressures
      put("PlotGa1", "_PR3");
      put("PlotGa2", "_PR5");
      put("PlotGa3", "_PR4");
      put("PlotGe1", "_PR3");
      put("PlotGe2", "_PR5");
      put("PlotGe3", "_PR4");
      put("PlotGP","_PR6");
      put("PlotG21", "_PR2");
      put("PlotG22", "_PR1");
      put("PlotGMid", "_PR6");
      put("PlotP21", "_P21_SPEED");
   }};

   Hashtable<String, String> PLOT_CRYO_CHANNELS = new Hashtable<String, String>(){{
      // CRYO Pressures
      put("PlotG22", "_MG1_CH1");
      put("PlotG31e", "_MG1_CH2");
      put("PlotGe3", "_MG1_CH3");
      put("PlotG31i", "_MG1_CH4");
      put("PlotG21", "_MG2_CH1");
      put("PlotGe1", "_MG2_CH2");
      put("PlotGe2", "_MG2_CH3");
      put("PlotGe2", "_MG2_CH3");
      put("PlotLN2_PT02","_PT02_CL_LEVEL");
      put("PlotLN2_PT01","_PT01_GAS_OUTLET");
      put("PlotLN2_LT01","_LT01_PS_LEVEL");
      put("PlotLN2_TT04","_TT04_CL_BOTMID");
      put("PlotLN2_TT05","_TT05_CL_MIDMID");
      put("PlotLN2_TT06","_TT06_CL_TOPMID");
      put("PlotLN2_TT07","_TT07_HEATER_OUT");
      put("PlotVALVEBIG_TInput","_CRYO_TEMP");
      put("PlotP2", "_P2_SPEED");
    }};

   Hashtable<String, String> BENCHTOWER_PRESSURE_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PressureG41", "_MG1_CH1");
      put("PressureGc1", "_MG1_CH2");
      put("PressureGc2", "_MG1_CH3");
      put("PressureG51", "_MG1_CH4");
      put("PressureG61", "_MG1_CH5");
      put("StatusPressureG41", "_MG1_CH1ST");
      put("StatusPressureGc1", "_MG1_CH2ST");
      put("StatusPressureGc2", "_MG1_CH3ST");
      put("StatusPressureG51", "_MG1_CH4ST");
      put("StatusPressureG61", "_MG1_CH5ST");
      put("StatusSensorG41", "_MG1_CH1SST");
      put("StatusSensorGc1", "_MG1_CH2SST");
      put("StatusSensorGc2", "_MG1_CH3SST");
      put("StatusSensorG51", "_MG1_CH4SST");
      put("StatusSensorG61", "_MG1_CH5SST");
      // ITR Gauges
      put("PressureGc3", "_GC3");
      put("StatusITRSensorGc3", "_GC3ITRST");
      put("StatusEmissionGc3", "_GC3EST");
      put("StatusDegasGc3", "_GC3DST");
      
   }};

   Hashtable<String, String> TOWER_PRESSURE_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PressureG41", "_MG1_CH1");
      put("PressureGc1", "_MG1_CH2");
      put("PressureG72", "_MG1_CH3");
      put("PressureGd1", "_MG1_CH4");
      put("PressureGc2", "_MG1_CH5");
      put("PressureG51", "_MG1_CH6");
      put("PressureG61", "_MG2_CH1");
      put("PressureGd2", "_MG2_CH2");
      put("PressureG31", "_MG2_CH3");
      put("PressureG71", "_MG2_CH4");
      put("PressureG81", "_MG2_CH5");
      put("StatusPressureG41", "_MG1_CH1ST");
      put("StatusPressureGc1", "_MG1_CH2ST");
      put("StatusPressureG72", "_MG1_CH3ST");
      put("StatusPressureGd1", "_MG1_CH4ST");
      put("StatusPressureGc2", "_MG1_CH5ST");
      put("StatusPressureG51", "_MG1_CH6ST");
      put("StatusPressureG61", "_MG2_CH1ST");
      put("StatusPressureGd2", "_MG2_CH2ST");
      put("StatusPressureG31", "_MG2_CH3ST");
      put("StatusPressureG71", "_MG2_CH4ST");
      put("StatusPressureG81", "_MG2_CH5ST");
      put("StatusSensorG41", "_MG1_CH1SST");
      put("StatusSensorGc1", "_MG1_CH2SST");
      put("StatusSensorG72", "_MG1_CH3SST");
      put("StatusSensorGd1", "_MG1_CH4SST");
      put("StatusSensorGc2", "_MG1_CH5SST");
      put("StatusSensorG51", "_MG1_CH6SST");
      put("StatusSensorG61", "_MG2_CH1SST");
      put("StatusSensorGd2", "_MG2_CH2SST");
      put("StatusSensorG31", "_MG2_CH3SST");
      put("StatusSensorG71", "_MG2_CH4SST");
      put("StatusSensorG81", "_MG2_CH5SST");
      
      // ITR Gauges
      put("PressureGc3", "_GC3");
      put("StatusITRSensorGc3", "_GC3ITRST");
      put("StatusEmissionGc3", "_GC3EST");
      put("StatusDegasGc3", "_GC3DST");
      put("PressureGd3", "_GD3");
      put("StatusITRSensorGd3", "_GD3ITRST");
      put("StatusEmissionGd3", "_GD3EST");
      put("StatusDegasGd3", "_GD3DST");
   }};

   Hashtable<String, String> CRYO_PRESSURE_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PressureG22", "_MG1_CH1");
      put("PressureG31e", "_MG1_CH2");
      put("PressureGe3", "_MG1_CH3");
      put("PressureG31i", "_MG1_CH4");
      put("PressureG21", "_MG2_CH1");
      put("PressureGe1", "_MG2_CH2");
      put("PressureGe2", "_MG2_CH3");
      put("StatusPressureG22", "_MG1_CH1ST");
      put("StatusPressureG31e", "_MG1_CH2ST");
      put("StatusPressureGe3", "_MG1_CH3ST");
      put("StatusPressureG31i", "_MG1_CH4ST");
      put("StatusPressureG21", "_MG2_CH1ST");
      put("StatusPressureGe1", "_MG2_CH2ST");
      put("StatusPressureGe2", "_MG2_CH3ST");
      put("StatusSensorG22", "_MG1_CH1SST");
      put("StatusSensorG31e", "_MG1_CH2SST");
      put("StatusSensorGe3", "_MG1_CH3SST");
      put("StatusSensorG31i", "_MG1_CH4SST");
      put("StatusSensorG21", "_MG2_CH1SST");
      put("StatusSensorGe1", "_MG2_CH2SST");
      put("StatusSensorGe2", "_MG2_CH3SST");
   }};

   Hashtable<String, String> REMOTEPUMPS_PRESSURE_CHANNELS = new Hashtable<String, String>(){{
      // MaxiGauges
      put("PressureG1","_MG1_CH1");
      put("PressureG2","_MG1_CH2");
      put("PressureG3","_MG1_CH3");
      put("PressureG4","_MG2_CH3");
      put("PressureG5","_MG2_CH4");
      put("PressureGpr","_MG3_CH2");
      put("PressureGbs","_MG3_CH4");
      put("PressureGls","_MG1_CH6");
      put("PressureGlp","_MG3_CH3");
      put("PressureGsr","_MG1_CH5");
      put("PressureGni","_MG2_CH1");
      put("PressureGwi","_MG3_CH5");
      put("PressureGib","_MG3_CH1");
      put("PressureGdt","_MG1_CH4");
      put("PressureGmc","_MG2_CH5");
      put("PressureGs1","_MG2_CH2");
      put("PressureGs2","_MG2_CH6");
      put("StatusPressureG1","_MG1_CH1ST");
      put("StatusPressureG2","_MG1_CH2ST");
      put("StatusPressureG3","_MG1_CH3ST");
      put("StatusPressureG4","_MG2_CH3ST");
      put("StatusPressureG5","_MG2_CH4ST");
      put("StatusPressureGpr","_MG3_CH2ST");
      put("StatusPressureGbs","_MG3_CH4ST");
      put("StatusPressureGls","_MG1_CH6ST");
      put("StatusPressureGlp","_MG3_CH3ST");
      put("StatusPressureGsr","_MG1_CH5ST");
      put("StatusPressureGni","_MG2_CH1ST");
      put("StatusPressureGwi","_MG3_CH5ST");
      put("StatusPressureGib","_MG3_CH1ST");
      put("StatusPressureGdt","_MG1_CH4ST");
      put("StatusPressureGmc","_MG2_CH5ST");
      put("StatusPressureGs1","_MG2_CH2ST");
      put("StatusPressureGs2","_MG2_CH6ST");
      put("StatusSensorG1","_MG1_CH1SST");
      put("StatusSensorG2","_MG1_CH2SST");
      put("StatusSensorG3","_MG1_CH3SST");
      put("StatusSensorG4","_MG2_CH3SST");
      put("StatusSensorG5","_MG2_CH4SST");
      put("StatusSensorGpr","_MG3_CH2SST");
      put("StatusSensorGbs","_MG3_CH4SST");
      put("StatusSensorGls","_MG1_CH6SST");
      put("StatusSensorGlp","_MG3_CH3SST");
      put("StatusSensorGsr","_MG1_CH5SST");
      put("StatusSensorGni","_MG2_CH1SST");
      put("StatusSensorGwi","_MG3_CH5SST");
      put("StatusSensorGib","_MG3_CH1SST");
      put("StatusSensorGdt","_MG1_CH4SST");
      put("StatusSensorGmc","_MG2_CH5SST");
      put("StatusSensorGs1","_MG2_CH2SST");
      put("StatusSensorGs2","_MG2_CH6SST");
   }};
   //
   // DRY
   //
   Hashtable<String, String> DRY_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Off (OK)");
      put("1","On (OK)");
      put("2","Off (KO)");
      put("3","On (KO)");
      put("4","Off (?)");
      put("5","On (?)");
      put("6","Off (KO)");
      put("7","On (KO)");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};
   //
   // VALVE
   //
   Hashtable<String, String> VALVE_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0", "Moving (OK)");
      put("1", "Opened (OK)");
      put("2", "Closed (OK)");
      put("3", "Comm Err (OK)");
      put("4", "Moving (KO)");
      put("5", "Opened (KO)");
      put("6", "Closed (KO)");
      put("7", "Com Err (KO)");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> VALVE_2_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0", "Moving");
      put("1", "Closed");
      put("2", "Opened");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> VALVE_3_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0", "Closed");
      put("1", "Open");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> INHIBIT_VALVE_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0", "Free");
      put("1", "Inhibited");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> INHIBIT_VALVE_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "lime");
      put("1", "red");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String,Color> VALVE_STATUS_COLOR = new Hashtable<String, Color>(){{ 
      put("0", Color.rgb(241, 218, 33));
      put("1", Color.rgb(255, 153, 51));
      put("2", Color.rgb(51, 255, 87));
      put("3", Color.rgb(255, 51, 57));
      put("4", Color.rgb(255, 51, 57));
      put("5", Color.rgb(255, 61, 57));
      put("6", Color.rgb(255, 71, 57));
      put("7", Color.rgb(255, 81, 57));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }};

   Hashtable<String,Color> VALVE_2_STATUS_COLOR = new Hashtable<String, Color>(){{ 
      put("0", Color.rgb(241, 218, 33));
      put("1", Color.rgb(51, 255, 87));
      put("2", Color.rgb(255, 153, 51));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }};

   Hashtable<String,Color> VALVE_3_STATUS_COLOR = new Hashtable<String, Color>(){{ 
      put("0", Color.rgb(51, 255, 87));
      put("1", Color.rgb(255, 153, 51));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }};
   //
   // VENTING STATUS
   //
   Hashtable<String, Color> VENTING_STATUS_COLOR = new Hashtable<String, Color>(){{ 
      put("0", Color.rgb(255, 153, 51));
      put("1", Color.rgb(51, 255, 87));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }};

   Hashtable<String,String> VENTING_STATUS_COLOR_2 = new Hashtable<String, String>(){{
      put("0", "#ff9933");
      put("1", "#33ff57");
      put("255", "#7c7c7c");
      put("---", "#7c7c7c");
      put("...", "#7c7c7c");
   }};

   Hashtable<String, String> VENTING_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0", "ON");
      put("1", "OFF");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};
   //
   // OPERATION STATUS
   //
   Hashtable<String, String> OPERATION_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0", "VACUUM");
      put("1", "IN AIR");
      put("2", "INTERMEDIATE");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};
   Hashtable<String,String> OPERATION_STATUS_COLOR = new Hashtable<String, String>(){{
      put("0", "#33ff57");
      put("1", "#ff9933");
      put("2", "#ffF933");
      put("255", "#7c7c7c");
      put("---", "#7c7c7c");
      put("...", "#7c7c7c");
   }};
   //
   // REMOTE PUMPING STATUS
   //
   Hashtable<String, Color> REMOTE_PUMPING_STATUS_COLOR = new Hashtable<String, Color>(){{
      put("0", Color.rgb(241, 218, 33));
      put("1", Color.rgb(51, 255, 87));
      put("2", Color.rgb(255, 153, 51));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }};

   Hashtable<String,String> REMOTE_PUMPING_STATUS_COLOR_2 = new Hashtable<String, String>(){{
      put("0", "f1da21");
      put("1", "#33ff57");
      put("2", "#ff9933");
      put("255", "#7c7c7c");
      put("---", "#7c7c7c");
      put("...", "#7c7c7c");
   }};

   Hashtable<String, String> REMOTE_PUMPING_STATUS_STRING = new Hashtable<String, String>(){{
      put("0", "MOVING");
      put("1", "OPENED");
      put("2", "CLOSED");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};
   //
   // LASER STATUS (Yag / CO2 / Green / LocalCtrl )
   //
   Hashtable<String, Color> LASER_STATUS_COLOR = new Hashtable<String, Color>(){{
      put("0", Color.rgb(78, 81, 78));      // off / safe (dark grey, matches legend)
      put("1", Color.rgb(255, 31, 31));     // on / hazard (red)
      put("255", Color.rgb(0, 0, 0));       // no data (black, distinct from off)
      put("---", Color.rgb(0, 0, 0));
      put("...", Color.rgb(0, 0, 0));
   }};
   
   //
   // GREEN STATUS
   //
   Hashtable<String, Color> GREEN_STATUS_COLOR = new Hashtable<String, Color>(){{
      put("0", Color.rgb(78, 81, 78));    // off / safe (dark grey, matches legend)
      put("1", Color.rgb(33,255,39));     // on / hazard (green)
      put("255", Color.rgb(0, 0, 0));     // no data (black, distinct from off)
      put("---", Color.rgb(0, 0, 0));
      put("...", Color.rgb(0, 0, 0));
   }};
   
   //
   // CO2 STATUS
   //
   Hashtable<String, Color> CO2_STATUS_COLOR = new Hashtable<String, Color>(){{
      put("0", Color.rgb(78, 81, 78));    // off / safe (dark grey, matches legend)
      put("1", Color.rgb(239,255,33));    // on / hazard (yellow)
      put("255", Color.rgb(0, 0, 0));     // no data (black, distinct from off)
      put("---", Color.rgb(0, 0, 0));
      put("...", Color.rgb(0, 0, 0));
   }};
   
   //
   // GENERAL PUMP
   //
   Hashtable<String, Color> PUMP_STATUS_COLOR = new Hashtable<String, Color>(){{
      put("0", Color.rgb(255, 153, 51));
      put("1", Color.rgb(51, 255, 87));
      put("2",Color.rgb(124, 124, 124));
      put("3",Color.rgb(124, 124, 124));
      put("4", Color.rgb(124, 124, 124));
      put("5",Color.rgb(124, 124, 124));
      put("6",Color.rgb(124, 124, 124));
      put("7",Color.rgb(124, 124, 124));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }};
   //
   // RGA
   //
   Hashtable<String,Color> RGA_STATUS_COLOR = new Hashtable<String, Color>(){{
      put("0", Color.rgb(255, 140, 0));
      put("1", Color.rgb(55, 214, 188));
      put("2", Color.rgb(51, 255, 87));
      put("255", Color.rgb(124 ,124 ,124));
      put("---", Color.rgb(124 ,124 ,124));
      put("...", Color.rgb(124, 124, 124));
   }}; 
   
   Hashtable<String,Color> RGA_2_STATUS_COLOR = new Hashtable<String, Color>(){{
      put("0", Color.rgb(255, 140, 0));
      put("1", Color.rgb(51, 255, 87));
      put("255", Color.rgb(124 ,124 ,124));
      put("---", Color.rgb(124 ,124 ,124));
      put("...", Color.rgb(124, 124, 124));
   }}; 
   //
   // RACK STATUS
   //
   Hashtable<String,String> RACK_STATUS_STRING = new Hashtable<String, String>(){{
      put("0", "OK");
      put("-1", "COM ERROR");
      put("-2", "COM ERROR");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};
   Hashtable<String,String> RACK_STATUS_COLOR = new Hashtable<String, String>(){{
      put("0", "#cc66cc");
      put("-1", "#da8a12");
      put("-2", "#d91212");
      put("255", "#7c7c7c");
      put("---", "#7c7c7c");
      put("...", "#7c7c7c");
   }};
   Hashtable<String,String> RACK_2_STATUS_COLOR = new Hashtable<String, String>(){{
      put("0", "lime");
      put("-1", "orange");
      put("-2", "red");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   //
   // SAFE
   //
   Hashtable<String, String> SAFE_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","ACTIVE");
      put("1","OFF");
      put("-1","CH. MISSING");
      put("2","TRIGGERED");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> SAFE_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "red");
      put("1", "lime");
      put("-1","orange");
      put("2", "yellow");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};
   //
   // GOLDEN
   //
    Hashtable<String, String> GOLDEN_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","FAILURE");
      put("1","OK");
      put("-1","CH. MISSING");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String,String> GOLDEN_STATUS_COLOR = new Hashtable<String, String>(){{
      put("0", "red");
      put("1", "lime");
      put("-1","orange");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   //
   // O2 SENSOR STATUS
   //
   Hashtable<String,String> O2SENSOR_STATUS_COLOR = new Hashtable<String, String>(){{
      put("0", "lime");
      put("1", "yellow");
      put("2","orange");
      put("3","red");
      put("4","brown");
      put("5","pink");
      put("6","lightgrey");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};
  
   //
   // GENERAL STATUS
   //
   Hashtable<String, String> OFFON_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","OFF");
      put("1","ON");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> OFFON_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "red");
      put("1", "lime");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String, String> ONOFF_3_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","---");
      put("1","ON");
      put("2","OFF");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> ONOFF_3_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "lightgrey");
      put("1", "red");
      put("2", "lime");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String, String> ONOFF_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","ON");
      put("1","OFF");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> ONOFF_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "lime");
      put("1", "red");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String, String> ONOFF_4_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","OFF");
      put("1","ON");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> ONOFF_4_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "red");
      put("1", "lime");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String, String> ONOFF_2_STATUS_STRING = new Hashtable<String, String>(){{
      put("0","---");
      put("1","ON");
      put("2","OFF");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> ONOFF_2_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "lightgrey");
      put("1", "lime");
      put("2", "red");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String, String> ACTIVEOFF_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","ACTIVE");
      put("1","OFF");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> ACTIVEOFF_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "red");
      put("1", "lime");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String, String> OFFACTIVE_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","OFF");
      put("1","ACTIVE");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> OFFACTIVE_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "lime");
      put("1", "red");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String, String> OKFAILURE_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","OK");
      put("1","FAILURE");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, Color> OKFAILURE_STATUS_COLOR = new Hashtable<String, Color>(){{ 
      put("0", Color.rgb(51, 255, 87));
      put("1", Color.rgb(255, 51, 57));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }};

   Hashtable<String, String> OKFAILURE_STATUS_COLOR_2 = new Hashtable<String, String>(){{ 
      put("0", "lime");
      put("1", "red");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};
   

   Hashtable<String, String> FAILUREOK_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","FAILURE");
      put("1","OK");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, Color> FAILUREOK_STATUS_COLOR = new Hashtable<String, Color>(){{ 
      put("0", Color.rgb(255, 51, 57));
      put("1", Color.rgb(51, 255, 87));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }};

   Hashtable<String, String> FAILUREOK_STATUS_COLOR_2 = new Hashtable<String, String>(){{ 
      put("0", "red");
      put("1", "lime");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};
   //
   // AIR COMPRESS
   //
   Hashtable<String, String> AIRCOMPRESS_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","OK");
      put("1","FAILURE");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};
   //
   // GAUGE
   //
   Hashtable<String,Color> GAUGE_STATUS_COLOR = new Hashtable<String, Color>(){{
      put("0", Color.rgb(51,255,87,0.75));
      put("1", Color.rgb(255, 153, 51));
      put("2", Color.rgb(51,255,87));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }};

   Hashtable<String,Color> XGSGAUGE_STATUS_COLOR = new Hashtable<String, Color>(){{
      put("0", Color.rgb(255, 153, 51));
      put("1", Color.rgb(51, 255, 87));
      put("2", Color.rgb(124, 124, 124));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }}; 

   Hashtable<String,Color> ITRGAUGE_STATUS_COLOR = new Hashtable<String, Color>(){{
      put("0", Color.rgb(255, 153, 51));
      put("1", Color.rgb(51, 255, 87));
      put("2", Color.rgb(124, 124, 124));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }}; 

   Hashtable<String, String> MAXIGAUGE_SENSOR_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","No change");
      put("1","Sensor Off");
      put("2","Sensor On");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> MAXIGAUGE_PRESSURE_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Ok");
      put("1","Underrange");
      put("2","Overrange");
      put("3","Error");
      put("4","Off");
      put("5","No Sensor");
      put("6","Id Error"); 
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> XGSGAUGE_DEGAS_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Degas Off");
      put("1","Degas On");     
      put("255","---");    
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> XGSGAUGE_EMISSION_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Emission Off");
      put("1","Emission On");
      put("255","---");
      put("---", "---");  
      put("...", "...");           
   }};

   Hashtable<String, String> XGSGAUGE_FILAMENT_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Unknown");
      put("1","Filament 1");
      put("2","Filament 2");
      put("255","---");
      put("---", "---"); 
      put("...", "...");                        
   }};
   
   Hashtable<String, String> ITRGAUGE_SENSOR_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Sensor Ok");
      put("1","Electronic Failure");
      put("2","Sensor Warning");
      put("3","Electronic Warning");
      put("4","Sensor Failure");
      put("5","Pressure too high");
      put("6","Over Temperature");                       
      put("255","---");
      put("---", "---"); 
      put("...", "...");
   }};

   Hashtable<String, String> ITRGAUGE_DEGAS_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Degas Off");
      put("1","Degas On");     
      put("255","---");    
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> ITRGAUGE_EMISSION_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Emission Off");
      put("1","Emission On");
      put("255","---");
      put("---", "---");    
      put("...", "...");         
   }};
   //
   // TURBO
   //
   Hashtable<String, String> TURBO_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Pump Off");
      put("1","Pump On");
      put("255","---");     
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> TURBO_STANDBY_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Standby Off");
      put("1","Standby On");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> TURBO_TEMP_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","OK");
      put("1","Excess");
      put("255","---");    
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> TURBO_TEMP_BOX_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","OK");
      put("1","Excess");
      put("255","---");  
      put("---", "---");
      put("...", "...");
   }};
   Hashtable<String, String> TURBO_POWER_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","OK");
      put("1","Excess");
      put("255","---");  
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> TURBO_BOX_2_STATUS_STRING = new Hashtable<String, String>(){{  // Varian pumps
     put("0","No Error");
     put("1","No connection");
     put("2","Pump Overtemperature");
     put("4","Box Overtemperature");
     put("8","Power Failure");  
     put("16","Aux Failure");
     put("32","Over Voltage");
     put("64","Short Circuit");
     put("128","Too High Load");
     put("129","Too Many Errors!!");
     put("255","?");
   }};
   
   Hashtable<String, String> TURBO_BOX_STATUS_STRING = new Hashtable<String, String>(){{ // Pfeiffer pumps
      put("0","No Error");
      put("1","Excess Speed");
      put("2","Over Voltage");
      put("6","Start Up Time Error");
      put("7","Mains failure");
      put("8","Connection Pump Error");
      put("10","Device Fault");
      put("14","Heating Default");
      put("15","TC Controller Error");
      put("16","Brake Current Default");
      put("17","Output3 Error");
      put("21","Unknown Pump");
      put("22","Cable Error");
      put("25","Temperarure Monitoring Error");
      put("26","Temperarure Sensor Error");
      put("33","+15Volt Error");
      put("34","70V Error");
      put("37","Motor Control Error");
      put("40","RAM Error");
      put("41","Excess Current");
      put("43","Config Fault");
      put("44","Excess TempBox");
      put("45","Excess Temp");
      put("46","Init. Fault");
      put("50","Excess Temp. Bearing");
      put("51","Vent Valve Default");
      put("52","Watchdog TMS320");
      put("62","Bearing X Default");
      put("63","Bearing Y Default");
      put("64","Radial Bearing Default");
      put("65","Radial Bearing Default");
      put("66","Axial Bearing Default");
      put("67","Axial Bearing Default");
      put("68","Axial Bearing Default");
      put("69","Axial Bearing Default");
      put("70","Axial Bearing Default");
      put("72","Axial Bearing Default");
      put("73","Overload Axial");
      put("74","Overload Radial");
      put("75","Radial Bearing Default"); 
      put("78","Axial Bearing Default"); 
      put("79","Radial Bearing Default"); 
      put("80","Radial Bearing Default"); 
      put("82","Cable Error");
      put("86","Sensor Error");
      put("87","Sensor Error");
      put("88","Sensor Error");
      put("89","Rotor Error");
      put("91","Device Fault");
      put("92","Unknow Panel");
      put("93","Temp Fault");
      put("94","TempBox Fault");
      put("98","Comm Error");
      put("100","Gauge Error");
      put("101","Valve Error");
      put("102","Valve Default");
      put("103","Valve Error");  
      put("104","Composite Error");
      put("105","TCS Error");
      put("107","Power Fault");
      put("108","Speed Fault");
      put("109","Firmware Error");
      put("114","TempStage Fault");
      put("117","Excess Temp Bottom");
      put("118","Excess Temp Stage");
      put("119","Excess Temp Bearing");
      put("698","TC not Responding");
      put("777","Speed not Confirmed");
      put("800","Excess Current Sensors");
      put("802","Position Sensors Fault");
      put("810","Data missing Pump");
      put("815","Excess Current Bearing");
      put("890","Bearing stress > 100%");
      put("891","Rotor Unbalance > 100%");
      put("913","Sef Test Error");
      put("255","---");   
      put("---", "---");
      put("...", "...");
   }};
   //
   // IONIC
   //
   Hashtable<String,String> IONIC_STATUS_ERROR_STRING = new Hashtable<String, String>(){{ 
      put("0","No Error");
      put("4","Over Temp.");
      put("32","Cable Interlock");
      put("64","Short Circuit");
      put("128","Protect");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};
   Hashtable<String,String> IONIC_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Pump Off");
      put("1","On StepStart");
      put("2","On FixeStart");
      put("3","On ProtectStep");
      put("4","On ProtectFixe");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};
   Hashtable<String, String> IONIC_COMM_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Local");
      put("1","Remote I/O");
      put("2","Serial");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};
   Hashtable<String, String> IONIC_OPERATING_MODE_STRING = new Hashtable<String, String>(){{ 
      put("0","Started");
      put("1","Protected");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};
   Hashtable<String, String> IONIC_VOLTAGE_MODE_STRING = new Hashtable<String, String>(){{ 
      put("0","Fixed");
      put("1","Stepped");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};
   //
   // TITANE
   //
   Hashtable<String,String> TITANE_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Pump Off");
      put("1","Pump On");
      put("2","Pump On");
      put("3","Pump On");
      put("4","Pump On");
      put("5","Pump On");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String,String> TITANE_FILAMENT_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","OK");
      put("1","Over Temperature");
      put("2","MiniTiBall Interrupt");
      put("3","Tsp Interrupt");
      put("4","Tsp Defective");
      put("5","Short Circuit");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String,String> TITANE_CONTROLLER_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","OFF");
      put("1","FAILURE");
      put("2","Wait Interlock");
      put("3","Ramp");
      put("4","Wait Sublimation");
      put("5","Sublimation");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String,String> TITANE_RECOVER_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","Recover Auto");
      put("1","Recover Manual");
      put("255","---");
      put("---", "---");
   }};

   Hashtable<String,String> TITANE_AUTOSTART_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","ON");
      put("1","OFF");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};
   Hashtable<String,String> TITANE_OPERATING_MODE_STRING = new Hashtable<String, String>(){{ 
      put("0","Manual");
      put("1","Auto");
      put("2","Remote");
      put("3","Auto/Remote");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};
   //
   // CRYO
   //
   Hashtable<String,String> CRYO_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0","@ AMBIENT TEMP");
      put("1","COOLING DOWN");
      put("2","FILLING LN2");
      put("3","OPERATION");
      put("4","BOILING DRY");
      put("5","WARMING UP");
      put("6","ERROR");
      put("7","UNKNOWN");
      put("255","---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String,Color> CRYO_STATUS_COLOR = new Hashtable<String, Color>(){{ 
      put("0", Color.rgb(51, 255, 87));
      put("1", Color.rgb(255, 153, 51));
      put("2", Color.rgb(255, 153, 51));
      put("3", Color.rgb(51, 255, 87));
      put("4", Color.rgb(255, 51, 57));
      put("5", Color.rgb(255, 153, 51));
      put("6", Color.rgb(255, 51, 57));
      put("7", Color.rgb(255, 51, 57));
      put("255", Color.rgb(124, 124, 124));
      put("---", Color.rgb(124, 124, 124));
      put("...", Color.rgb(124, 124, 124));
   }};

   Hashtable<String, String> CRYO_STATUS_COLOR_2 = new Hashtable<String, String>(){{ 
      put("0", "lime");
      put("1", "orange");
      put("2", "orange");
      put("3", "lime");
      put("4", "red");
      put("5", "orange");
      put("6","orange");
      put("7", "lightgrey");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String, String> REFILL_MODE_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0", "No refill");
      put("1", "Manual");
      put("2", "Constant");
      put("3", "Auto");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> FANSPEED_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0", "-->");
      put("1", "NORMAL");
      put("2", "LOW");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> FANSPEED_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "yellow");
      put("1", "orange");
      put("2", "lime");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String, String> OKWORKING_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0", "OK");
      put("1", "WORKING");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> OKWORKING_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "lime");
      put("1", "orange");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};

   Hashtable<String, String> OKLOW_STATUS_STRING = new Hashtable<String, String>(){{ 
      put("0", "OK");
      put("1", "LOW");
      put("255", "---");
      put("---", "---");
      put("...", "...");
   }};

   Hashtable<String, String> OKLOW_STATUS_COLOR = new Hashtable<String, String>(){{ 
      put("0", "lime");
      put("1", "orange");
      put("255", "lightgrey");
      put("---", "lightgrey");
      put("...", "lightgrey");
   }};
}
