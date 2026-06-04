package com.gluonapplication;


public class DataSetNESafety extends DataSet {

   public DataSetNESafety(String name) {
      super();

      // Valves (V-shape SVGPaths)
      list.addElement(new DataElement("StatusValveBigNE",  Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_VALVEBIGNE_ST"));
      list.addElement(new DataElement("StatusValveCryoNE", Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_CRYONE_VCRYOST"));

      // Yag/CO2/Green laser status (circles)
      list.addElement(new DataElement("NEYag",        Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("NEMiniYag",    Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("TUBENYag",     Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("NEGreen",      Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("TUBENGreen",   Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("NEMiniGreen",   Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      // NESourceGreen: ON iff ALS_NEB_PD_GREEN_MONI_CALI > 1 AND ALS_NEB_REL == 0.
      // Two DataElements share the fx:id; the handler in ViewData.java combines them.
      list.addElement(new DataElement("NESourceGreen",  Type.CIRCLE_SOURCE_GREEN_STATUS_COLOR, "ALS_NEB_PD_GREEN_MONI_CALI_MEAN"));
      list.addElement(new DataElement("NESourceGreen",  Type.CIRCLE_SOURCE_GREEN_STATUS_COLOR, "ALS_NEB_REL1"));
      list.addElement(new DataElement("NEGreenShutter", Type.SHUTTER_GREEN_STATUS_COLOR, "ALS_NEB_REL1"));
      // PCAL (1047 Hz) laser status. Both the Source circle and the Tower beam
      // circle read the same on/off channel, so the tower mirrors the source.
      list.addElement(new DataElement("NESourcePCAL", Type.CIRCLE_PCAL_STATUS_COLOR, "PCAL_NE_laser_on_20kHz_50Hz_MAX"));
      list.addElement(new DataElement("NEPCAL",       Type.CIRCLE_PCAL_STATUS_COLOR, "PCAL_NE_laser_on_20kHz_50Hz_MAX"));
      // Local control status (rectangle).
      // F0_DC_ENBL and F7_DC_ON share the fx:id; the handler in ViewData.java
      // ORs them: any "1" => ON, all known "0" => OFF, otherwise no-data.
      list.addElement(new DataElement("NELocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_NE_F0_DC_ENBL"));
      list.addElement(new DataElement("NELocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_NE_F7_DC_ON"));

      // ---- Propagation-only channels (no FXML element renders these) ----
      // Cross-panel channels needed by LaserTopology.applyPropagation so the NE
      // panel can compute Yag/Green reachability end-to-end (Yag source at IB,
      // WE Green source, every gating valve along the path from any source to NE).
      list.addElement(new DataElement("_PropYagInjPd",   Type.LABEL_VALUE_STRING, "INJ_EIB_POUT_PD_MAX"));
      list.addElement(new DataElement("_PropYagBsQf",    Type.LABEL_VALUE_STRING, "BsX_QF_DC_MAX"));
      list.addElement(new DataElement("_PropYagBsQn",    Type.LABEL_VALUE_STRING, "BsX_QN_DC_MAX"));
      list.addElement(new DataElement("WESourceGreen",   Type.LABEL_VALUE_STRING, "ALS_WEB_PD_GREEN_MONI_CALI_MEAN"));
      list.addElement(new DataElement("WESourceGreen",   Type.LABEL_VALUE_STRING, "ALS_WEB_REL1"));
      list.addElement(new DataElement("_PropValveCentralLI", Type.LABEL_VALUE_STRING, "VAC_VALVECENTRAL_VLIST"));
      list.addElement(new DataElement("_PropValveCentralPS", Type.LABEL_VALUE_STRING, "VAC_VALVECENTRAL_VPSST"));
      list.addElement(new DataElement("_PropValveCentralNS", Type.LABEL_VALUE_STRING, "VAC_VALVECENTRAL_VNSST"));
      list.addElement(new DataElement("_PropValveCentralWS", Type.LABEL_VALUE_STRING, "VAC_VALVECENTRAL_VWSST"));
      list.addElement(new DataElement("_PropValveCentralSS", Type.LABEL_VALUE_STRING, "VAC_VALVECENTRAL_VSSST"));
      list.addElement(new DataElement("_PropValveCryoNI",    Type.LABEL_VALUE_STRING, "VAC_CRYONI_VCRYOST"));
      list.addElement(new DataElement("_PropValveCryoWI",    Type.LABEL_VALUE_STRING, "VAC_CRYOWI_VCRYOST"));
      list.addElement(new DataElement("_PropValveBigNI",     Type.LABEL_VALUE_STRING, "VAC_VALVEBIGNI_ST"));
      list.addElement(new DataElement("_PropValveBigWI",     Type.LABEL_VALUE_STRING, "VAC_VALVEBIGWI_ST"));
      list.addElement(new DataElement("_PropValveCryoWE",    Type.LABEL_VALUE_STRING, "VAC_CRYOWE_VCRYOST"));
      list.addElement(new DataElement("_PropValveBigWE",     Type.LABEL_VALUE_STRING, "VAC_VALVEBIGWE_ST"));

      Init();
   }
}
