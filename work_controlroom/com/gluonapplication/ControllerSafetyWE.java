package com.gluonapplication;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

public class ControllerSafetyWE implements ControlTypes {

    @FXML private Label CryoWE;

    @FXML private SVGPath StatusValveBigWE;
    @FXML private SVGPath StatusValveCryoWE;
    @FXML private SVGPath StatusValveLN2WE1;
    @FXML private SVGPath StatusValveLN2WE2;

    @FXML private Label TUBEW;
    @FXML private Circle TUBEWGreen;
    @FXML private Circle TUBEWYag;

    @FXML private Label WE;
    @FXML private Circle WEGreen;
    @FXML private SVGPath WEGreenShutter;
    @FXML private Rectangle WELocalCtrl;
    @FXML private Label WEMini;
    @FXML private Circle WEMiniGreen;
    @FXML private Circle WEMiniYag;
    @FXML private Circle WESourceGreen;
    @FXML private Circle WEYag;

    private String tubewStyle = "";

    /* --- Click WEST tube band -> back to the Global (CB) Safety Flags panel --- */

    @FXML
    void TUBEW_Clicked(MouseEvent event) {
        SafetyFlagsNavigation.show("CBSAFETYFLAGS", "CB Safety Flags");
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
