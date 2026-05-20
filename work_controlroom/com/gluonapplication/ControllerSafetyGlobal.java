package com.gluonapplication;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

public class ControllerSafetyGlobal implements ControlTypes {

    @FXML private Label BS;
    @FXML private Circle BSGreen;
    @FXML private Rectangle BSLocalCtrl;
    @FXML private Circle BSYag;

    @FXML private Label CryoLinkDet;
    @FXML private Label CryoLinkIB;
    @FXML private Label CryoNI;
    @FXML private Label CryoWI;

    @FXML private Circle DETGreen;
    @FXML private Rectangle DETLocalCtrl;
    @FXML private Circle DETMiniGreen;
    @FXML private Circle DETMiniYag;
    @FXML private Circle DETYag;
    @FXML private Label Det;
    @FXML private Label DetMini;

    @FXML private Label IB;
    @FXML private Circle IBGreen;
    @FXML private Rectangle IBLocalCtrl;
    @FXML private Label IBMini;
    @FXML private Circle IBMiniGreen;
    @FXML private Circle IBMiniYag;
    @FXML private Circle IBYag;

    @FXML private Label MC;
    @FXML private Rectangle MCLocalCtrl;
    @FXML private Circle MCYag;

    @FXML private Label NI;
    @FXML private Circle NICO2;
    @FXML private SVGPath NICO2Shutter;
    @FXML private Circle NIGreen;
    @FXML private Rectangle NILocalCtrl;
    @FXML private Circle NISourceCO2;
    @FXML private Circle NIYag;

    @FXML private Label PR;
    @FXML private Circle PRGreen;
    @FXML private Rectangle PRLocalCtrl;
    @FXML private Label PRMini;
    @FXML private Circle PRMiniGreen;
    @FXML private Circle PRMiniYag;
    @FXML private Circle PRYag;

    @FXML private Circle SQZ0NYag;
    @FXML private Circle SQZ300NYag;
    @FXML private Circle SQZDET1Yag;
    @FXML private Circle SQZDET2Yag;

    @FXML private Label SR;
    @FXML private Circle SRGreen;
    @FXML private Rectangle SRLocalCtrl;
    @FXML private Circle SRYag;

    @FXML private Circle SourceYag;

    @FXML private Label Sqz;
    @FXML private Label Sqz0N;
    @FXML private Label Sqz300N;
    @FXML private Label SqzDet1;
    @FXML private Label SqzDet2;

    @FXML private SVGPath StatusValveBigNI;
    @FXML private SVGPath StatusValveBigWI;
    @FXML private SVGPath StatusValveCentralLI;
    @FXML private SVGPath StatusValveCentralNS;
    @FXML private SVGPath StatusValveCentralPS;
    @FXML private SVGPath StatusValveCentralSS;
    @FXML private SVGPath StatusValveCentralWS;
    @FXML private SVGPath StatusValveCryoLinkDETVs1;
    @FXML private SVGPath StatusValveCryoLinkDETVs2;
    @FXML private SVGPath StatusValveCryoLinkIBVs1;
    @FXML private SVGPath StatusValveCryoLinkIBVs2;
    @FXML private SVGPath StatusValveCryoNI;
    @FXML private SVGPath StatusValveCryoWI;
    @FXML private SVGPath StatusValveLN2NE1;
    @FXML private SVGPath StatusValveLN2NE2;
    @FXML private SVGPath StatusValveSqz0N;
    @FXML private SVGPath StatusValveSqz300N;
    @FXML private SVGPath StatusValveSqzDet1;
    @FXML private SVGPath StatusValveSqzDet2;

    @FXML private Label TUBEMC;
    @FXML private Label TUBEN;
    @FXML private Circle TUBENGreen;
    @FXML private Circle TUBENYag;
    @FXML private Label TUBEW;
    @FXML private Circle TUBEWGreen;
    @FXML private Circle TUBEWYag;

    @FXML private Label WI;
    @FXML private Circle WICO2;
    @FXML private SVGPath WICO2Shutter;
    @FXML private Circle WIGreen;
    @FXML private Rectangle WILocalCtrl;
    @FXML private Circle WISourceCO2;
    @FXML private Circle WIYag;

    private String tubenStyle = "";
    private String tubewStyle = "";

    @FXML void MC_Clicked(MouseEvent event)    { }
    @FXML void MC_Pressed(MouseEvent event)    { }
    @FXML void MC_Released(MouseEvent event)   { }

    @FXML void Sqz_Clicked(MouseEvent event)   { }
    @FXML void Sqz_Pressed(MouseEvent event)   { }
    @FXML void Sqz_Released(MouseEvent event)  { }

    @FXML void TUBEMC_Clicked(MouseEvent event)  { }
    @FXML void TUBEMC_Pressed(MouseEvent event)  { }
    @FXML void TUBEMC_Released(MouseEvent event) { }

    /* --- Click NORTH tube band -> NE Safety Flags panel --- */

    @FXML
    void TUBEN_Clicked(MouseEvent event) {
        SafetyFlagsNavigation.show("NESAFETYFLAGS", "NE Safety Flags");
    }

    @FXML
    void TUBEN_Pressed(MouseEvent event) {
        if (TUBEN == null) return;
        tubenStyle = TUBEN.getStyle();
        TUBEN.setStyle(DECORATION_STYLE_PUSHED);
    }

    @FXML
    void TUBEN_Released(MouseEvent event) {
        if (TUBEN == null) return;
        TUBEN.setStyle(tubenStyle);
    }

    /* --- Click WEST tube band -> WE Safety Flags panel --- */

    @FXML
    void TUBEW_Clicked(MouseEvent event) {
        SafetyFlagsNavigation.show("WESAFETYFLAGS", "WE Safety Flags");
    }

    @FXML
    void TUBEW_Pressed(MouseEvent event) {
        if (TUBEW == null) return;
        tubewStyle = TUBEW.getStyle();
        TUBEW.setStyle(DECORATION_STYLE_PUSHED);
    }

    @FXML
    void TUBEW_Released(MouseEvent event) {
        if (TUBEW == null) return;
        TUBEW.setStyle(tubewStyle);
    }
}
