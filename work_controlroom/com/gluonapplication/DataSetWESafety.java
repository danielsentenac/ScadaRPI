package com.gluonapplication;


public class DataSetWESafety extends DataSet {

   public DataSetWESafety(String name) {
      super();

      // Valves (V-shape SVGPaths)
      list.addElement(new DataElement("StatusValveBigWE",  Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_VALVEBIGWE_ST"));
      list.addElement(new DataElement("StatusValveCryoWE", Type.SVGPATH_VALVE_STATUS_COLOR, "VAC_CRYOWE_VCRYOST"));

      // Yag/CO2/Green laser status (circles)
      list.addElement(new DataElement("WEYag",        Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("WEMiniYag",    Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("TUBEWYag",     Type.CIRCLE_YAG_STATUS_COLOR, ""));
      list.addElement(new DataElement("WEGreen",      Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("TUBEWGreen",   Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      list.addElement(new DataElement("WEMiniGreen",  Type.CIRCLE_GREEN_STATUS_COLOR, ""));
      // WESourceGreen: ON iff ALS_WEB_PD_GREEN_MONI_CALI > 1 AND ALS_WEB_REL == 0.
      // Two DataElements share the fx:id; the handler in ViewData.java combines them.
      list.addElement(new DataElement("WESourceGreen",  Type.CIRCLE_SOURCE_GREEN_STATUS_COLOR, "ALS_WEB_PD_GREEN_MONI_CALI_MEAN"));
      list.addElement(new DataElement("WESourceGreen",  Type.CIRCLE_SOURCE_GREEN_STATUS_COLOR, "ALS_WEB_REL1"));
      list.addElement(new DataElement("WEGreenShutter", Type.SHUTTER_GREEN_STATUS_COLOR, "ALS_WEB_REL1"));
      // Local control status (rectangle).
      // F0_DC_ENBL and F7_DC_ON share the fx:id; the handler in ViewData.java
      // ORs them: any "1" => ON, all known "0" => OFF, otherwise no-data.
      list.addElement(new DataElement("WELocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_WE_F0_DC_ENBL"));
      list.addElement(new DataElement("WELocalCtrl",  Type.RECTANGLE_LOCALCTRL_STATUS_COLOR, "SAT_WE_F7_DC_ON"));

      // ---- Propagation-only channels (no FXML element renders these) ----
      // Cross-panel channels needed by LaserTopology.applyPropagation so the WE
      // panel can compute Yag/Green reachability end-to-end (Yag source at IB,
      // NE Green source, every gating valve along the path from any source to WE).
      list.addElement(new DataElement("_PropYagInjPd",   Type.LABEL_VALUE_STRING, "INJ_EIB_POUT_PD_MAX"));
      list.addElement(new DataElement("_PropYagBsQf",    Type.LABEL_VALUE_STRING, "BsX_QF_DC_MAX"));
      list.addElement(new DataElement("_PropYagBsQn",    Type.LABEL_VALUE_STRING, "BsX_QN_DC_MAX"));
      list.addElement(new DataElement("NESourceGreen",   Type.LABEL_VALUE_STRING, "ALS_NEB_PD_GREEN_MONI_CALI_MEAN"));
      list.addElement(new DataElement("NESourceGreen",   Type.LABEL_VALUE_STRING, "ALS_NEB_REL1"));
      list.addElement(new DataElement("_PropValveCentralLI", Type.LABEL_VALUE_STRING, "VAC_VALVECENTRAL_VLIST"));
      list.addElement(new DataElement("_PropValveCentralPS", Type.LABEL_VALUE_STRING, "VAC_VALVECENTRAL_VPSST"));
      list.addElement(new DataElement("_PropValveCentralNS", Type.LABEL_VALUE_STRING, "VAC_VALVECENTRAL_VNSST"));
      list.addElement(new DataElement("_PropValveCentralWS", Type.LABEL_VALUE_STRING, "VAC_VALVECENTRAL_VWSST"));
      list.addElement(new DataElement("_PropValveCentralSS", Type.LABEL_VALUE_STRING, "VAC_VALVECENTRAL_VSSST"));
      list.addElement(new DataElement("_PropValveCryoNI",    Type.LABEL_VALUE_STRING, "VAC_CRYONI_VCRYOST"));
      list.addElement(new DataElement("_PropValveCryoWI",    Type.LABEL_VALUE_STRING, "VAC_CRYOWI_VCRYOST"));
      list.addElement(new DataElement("_PropValveBigNI",     Type.LABEL_VALUE_STRING, "VAC_VALVEBIGNI_ST"));
      list.addElement(new DataElement("_PropValveBigWI",     Type.LABEL_VALUE_STRING, "VAC_VALVEBIGWI_ST"));
      list.addElement(new DataElement("_PropValveCryoNE",    Type.LABEL_VALUE_STRING, "VAC_CRYONE_VCRYOST"));
      list.addElement(new DataElement("_PropValveBigNE",     Type.LABEL_VALUE_STRING, "VAC_VALVEBIGNE_ST"));

      Init();
   }
}
