package com.gluonapplication;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

public class ControllerSafetyNE implements ControlTypes {

    @FXML private Label CryoNE;

    @FXML private Label NE;
    @FXML private Circle NEGreen;
    @FXML private SVGPath NEGreenShutter;
    @FXML private Rectangle NELocalCtrl;
    @FXML private Label NEMini;
    @FXML private Circle NEMiniGreen;
    @FXML private Circle NEMiniYag;
    @FXML private Circle NESourceGreen;
    @FXML private Circle NEYag;

    @FXML private SVGPath StatusValveBigNE;
    @FXML private SVGPath StatusValveCryoNE;
    @FXML private SVGPath StatusValveLN2NE1;
    @FXML private SVGPath StatusValveLN2NE2;

    @FXML private Label TUBEN;
    @FXML private Circle TUBENGreen;
    @FXML private Circle TUBENYag;

    private String tubenStyle = "";

    /* --- Click NORTH tube band -> back to the Global (CB) Safety Flags panel --- */

    @FXML
    void TUBEN_Clicked(MouseEvent event) {
        SafetyFlagsNavigation.show("CBSAFETYFLAGS", "CB Safety Flags");
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
}
