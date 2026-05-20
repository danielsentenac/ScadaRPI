package com.gluonapplication;


public class DataSetCBSafety extends DataSet {

   public DataSetCBSafety(String name) {
      super();

      // Valves (V-shape SVGPaths)
      list.addElement(new DataElement("StatusValveSqz300N",        Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_SQZ300N_VPST"));
      list.addElement(new DataElement("StatusValveSqz0N",          Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_SQZ0N_VPST"));
      list.addElement(new DataElement("StatusValveSqzDet2",        Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_SQZDET2_VPST"));
      list.addElement(new DataElement("StatusValveSqzDet1",        Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_SQZDET1_VPST"));
      list.addElement(new DataElement("StatusValveBigNI",          Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_VALVEBIGNI_ST"));
      list.addElement(new DataElement("StatusValveBigWI",          Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_VALVEBIGWI_ST"));
      list.addElement(new DataElement("StatusValveCentralLI",      Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_VALVECENTRAL_VLIST"));
      list.addElement(new DataElement("StatusValveCryoNI",         Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_CRYONI_VCRYOST"));
      list.addElement(new DataElement("StatusValveCryoWI",         Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_CRYOWI_VCRYOST"));
      list.addElement(new DataElement("StatusValveCentralNS",      Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_VALVECENTRAL_VNSST"));
      list.addElement(new DataElement("StatusValveCentralWS",      Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_VALVECENTRAL_VWSST"));
      list.addElement(new DataElement("StatusValveCentralPS",      Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_VALVECENTRAL_VPSST"));
      list.addElement(new DataElement("StatusValveCentralSS",      Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_VALVECENTRAL_VSSST"));
      list.addElement(new DataElement("StatusValveCryoLinkDETVs1", Type.SVGPATH_VALVE_2_STATUS_COLOR, "VAC_CRYOLINKDET_Vs1"));
      list.addElement(new DataElement("StatusValveCryoLinkIBVs2",  Type.SVGPATH_VALVE_2_STATUS_COLOR, "VAC_CRYOLINKIB_Vs2"));
      list.addElement(new DataElement("StatusValveCryoLinkIBVs1",  Type.SVGPATH_VALVE_2_STATUS_COLOR, "VAC_CRYOLINKIB_Vs1"));
      list.addElement(new DataElement("StatusValveCryoLinkDETVs2", Type.SVGPATH_VALVE_2_STATUS_COLOR, "VAC_CRYOLINKDET_Vs2"));

      // Yag laser source status (circle).
      // OFF iff INJ_EIB_POUT_PD_MAX + BsX_QF_DC_MAX + BsX_QN_DC_MAX < 0.1 V; ON otherwise.
      // The three channels share the fx:id; the handler in ViewData.java sums them.
      list.addElement(new DataElement("SourceYag",   Type.CIRCLE_SOURCE_YAG_STATUS_COLOR, "INJ_EIB_POUT_PD_MAX"));
      list.addElement(new DataElement("SourceYag",   Type.CIRCLE_SOURCE_YAG_STATUS_COLOR, "BsX_QF_DC_MAX"));
      list.addElement(new DataElement("SourceYag",   Type.CIRCLE_SOURCE_YAG_STATUS_COLOR, "BsX_QN_DC_MAX"));

      // Yag laser status (circles)
      list.addElement(new DataElement("IBYag",       Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("IBMiniYag",   Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("PRYag",       Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("PRMiniYag",   Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("BSYag",       Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("NIYag",       Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("SRYag",       Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("WIYag",       Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("DETYag",      Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("DETMiniYag",  Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("MCYag",       Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("SQZDET1Yag",  Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("SQZDET2Yag",  Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("SQZ0NYag",    Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("SQZ300NYag",  Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("TUBENYag",    Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("TUBEWYag",    Type.CIRCLE_YAG_STATUS_COLOR, ""));

      // CO2 laser status (circles).
      // Each CO2 source is monitored by two channels sharing the fx:id; the handler in
      // ViewData.java combines them: ON iff either signal is significantly above 0.
      list.addElement(new DataElement("WISourceCO2",       Type.CIRCLE_SOURCE_CO2_STATUS_COLOR, "TCS_WI_CO2_CH_PWRLAS_MEAN"));
      list.addElement(new DataElement("WISourceCO2",       Type.CIRCLE_SOURCE_CO2_STATUS_COLOR, "TCS_WI_CO2_PWRLAS_MEAN"));
      list.addElement(new DataElement("NISourceCO2",       Type.CIRCLE_SOURCE_CO2_STATUS_COLOR, "TCS_NI_CO2_CH_PWRLAS_MEAN"));
      list.addElement(new DataElement("NISourceCO2",       Type.CIRCLE_SOURCE_CO2_STATUS_COLOR, "TCS_NI_CO2_PWRLAS_MEAN"));
      // WICO2 / NICO2 tower indicators reflect the same physics as the source-CO2
      // circles (CO2 lasers are installed at the WI/NI towers themselves, no
      // propagation through the ITF). Two channels per fx:id, same logic as
      // CIRCLE_SOURCE_CO2_STATUS_COLOR.
      list.addElement(new DataElement("WICO2",             Type.CIRCLE_SOURCE_CO2_STATUS_COLOR, "TCS_WI_CO2_CH_PWRLAS_MEAN"));
      list.addElement(new DataElement("WICO2",             Type.CIRCLE_SOURCE_CO2_STATUS_COLOR, "TCS_WI_CO2_PWRLAS_MEAN"));
      list.addElement(new DataElement("NICO2",             Type.CIRCLE_SOURCE_CO2_STATUS_COLOR, "TCS_NI_CO2_CH_PWRLAS_MEAN"));
      list.addElement(new DataElement("NICO2",             Type.CIRCLE_SOURCE_CO2_STATUS_COLOR, "TCS_NI_CO2_PWRLAS_MEAN"));
      
      // Green laser status (circles)
      list.addElement(new DataElement("IBGreen",      Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("PRGreen",      Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("BSGreen",      Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("NIGreen",      Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("WIGreen",      Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("TUBEWGreen",   Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("TUBENGreen",   Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("SRGreen",      Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("DETGreen",     Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("DETMiniGreen", Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("PRMiniGreen",  Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("IBMiniGreen",  Type.CIRCLE_GREEN_STATUS_COLOR, ""));

      // Local control status (rectangles).
      // Two SAT channels per tower (F0_DC_ENBL and F7_DC_ON) share the fx:id;
      // the handler in ViewData.java ORs them: any "1" => ON, all known "0" =>
      // OFF, otherwise no-data. IB/MC/DET expose only the F0 channel.
      list.addElement(new DataElement("NILocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_NI_F0_DC_ENBL"));
      list.addElement(new DataElement("NILocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_NI_F7_DC_ON"));
      list.addElement(new DataElement("WILocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_WI_F0_DC_ENBL"));
      list.addElement(new DataElement("WILocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_WI_F7_DC_ON"));
      list.addElement(new DataElement("BSLocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_BS_F0_DC_ENBL"));
      list.addElement(new DataElement("BSLocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_BS_F7_DC_ON"));
      list.addElement(new DataElement("PRLocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_PR_F0_DC_ENBL"));
      list.addElement(new DataElement("PRLocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_PR_F7_DC_ON"));
      list.addElement(new DataElement("IBLocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_IB_F0_DC_ENBL"));
      list.addElement(new DataElement("MCLocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_MC_F0_DC_ENBL"));
      list.addElement(new DataElement("SRLocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_SR_F0_DC_ENBL"));
      list.addElement(new DataElement("SRLocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_SR_F7_DC_ON"));
      list.addElement(new DataElement("DETLocalCtrl", Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_OB_F0_DC_ENBL"));

      // Shutters (SVGPath flower-shape).
      // Each shutter is the OR of 3 TCS_CO2_REL channels: all "0" => closed; any non-zero => open.
      // Three DataElements per shutter share the same fx:id name; the handler in ViewData.java
      // ORs the values across all entries with that name.
      list.addElement(new DataElement("WICO2Shutter", Type.SHUTTER_CO2_STATUS_COLOR, "TCS_CO2_REL1"));
      list.addElement(new DataElement("WICO2Shutter", Type.SHUTTER_CO2_STATUS_COLOR, "TCS_CO2_REL2"));
      list.addElement(new DataElement("WICO2Shutter", Type.SHUTTER_CO2_STATUS_COLOR, "TCS_CO2_REL3"));
      list.addElement(new DataElement("NICO2Shutter", Type.SHUTTER_CO2_STATUS_COLOR, "TCS_CO2_REL5"));
      list.addElement(new DataElement("NICO2Shutter", Type.SHUTTER_CO2_STATUS_COLOR, "TCS_CO2_REL6"));
      list.addElement(new DataElement("NICO2Shutter", Type.SHUTTER_CO2_STATUS_COLOR, "TCS_CO2_REL7"));

      // ---- Propagation-only channels (no FXML element renders these) ----
      // Cross-panel channels needed by LaserTopology.applyPropagation to compute
      // the Yag/Green source states and end-station valve states from CB.
      // The Type is irrelevant for rendering: the fx:id lookup returns null on
      // this FXML, so no visual side-effect; they're polled only.
      list.addElement(new DataElement("WESourceGreen", Type.LABEL_VALUE_STRING, "ALS_WEB_PD_GREEN_MONI_CALI_MEAN"));
      list.addElement(new DataElement("WESourceGreen", Type.LABEL_VALUE_STRING, "ALS_WEB_REL1"));
      list.addElement(new DataElement("NESourceGreen", Type.LABEL_VALUE_STRING, "ALS_NEB_PD_GREEN_MONI_CALI_MEAN"));
      list.addElement(new DataElement("NESourceGreen", Type.LABEL_VALUE_STRING, "ALS_NEB_REL1"));
      list.addElement(new DataElement("_PropValveBigWE",  Type.LABEL_VALUE_STRING, "VAC_VALVEBIGWE_ST"));
      list.addElement(new DataElement("_PropValveCryoWE", Type.LABEL_VALUE_STRING, "VAC_CRYOWE_VCRYOST"));
      list.addElement(new DataElement("_PropValveBigNE",  Type.LABEL_VALUE_STRING, "VAC_VALVEBIGNE_ST"));
      list.addElement(new DataElement("_PropValveCryoNE", Type.LABEL_VALUE_STRING, "VAC_CRYONE_VCRYOST"));

      Init();
   }
}
