package com.gluonapplication;

import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;


public class ViewSafetyFlags extends ViewData {

    private static final Class<?>[] FXML_CONTROLLERS = {
        ControllerSafetyGlobal.class,
        ControllerSafetyNE.class,
        ControllerSafetyWE.class
    };

    public ViewSafetyFlags(String _fxml, String _name) {
        super(_fxml, _name);
        switch (_fxml) {
            case "CBSAFETYFLAGS":
                data = new DataSetCBSafety(name);
                break;
            case "NESAFETYFLAGS":
                data = new DataSetNESafety(name);
                break;
            case "WESAFETYFLAGS":
                data = new DataSetWESafety(name);
                break;
            default:
                data = new DataSet();
                data.Init();
                break;
        }
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        appBar.setStyle("-fx-font-size: 13");
        appBar.setTitleText(name);
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> {
            MobileApplication.getInstance().switchToPreviousView();
        }));
        appBar.getActionItems().addAll(MaterialDesignIcon.KEYBOARD_ARROW_UP.button(e -> {
            Main main = (Main) MobileApplication.getInstance();
            main.switchView(MobileApplication.HOME_VIEW);
        }));
    }
}
