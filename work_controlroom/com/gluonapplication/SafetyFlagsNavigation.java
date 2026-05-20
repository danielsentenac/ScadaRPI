package com.gluonapplication;

final class SafetyFlagsNavigation {

    private SafetyFlagsNavigation() {
    }

    static void show(String fxml, String viewId) {
        // The control-room screen embeds CB, NE and WE safety flags side by side,
        // so the in-app navigation hooks from VACUUM_SUPERVISOR are intentionally inert here.
    }
}
