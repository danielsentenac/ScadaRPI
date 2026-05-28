import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

public class CbsaspanelFx {
    private static final String PANEL_BACKGROUND = "#0f1a30";
    private static final String ACCENT_COLOR = "AQUA";
    private static final String TITLE_BACKGROUND = "#0e1726";
    private static final double CONTENT_MARGIN = 8.0;

    public static Scene buildO2Scene(Pane o2Pane) {
        applyStatusFontOverride(o2Pane, new String[] {"ControllerWE", "AlarmWE"}, 16);
        BorderPane decorated = wrapWithTitle("O₂ SENSORS", o2Pane);
        decorated.setStyle("-fx-background-color: black;");
        return new Scene(decorated);
    }

    private static void applyStatusFontOverride(Pane pane, String[] ids, int fontSize) {
        Runnable apply = () -> {
            for (String id : ids) {
                javafx.scene.Node n = pane.lookup("#" + id);
                if (n instanceof Label) {
                    Label l = (Label) n;
                    boolean[] guard = {false};
                    l.styleProperty().addListener((obs, sOld, sNew) -> {
                        if (guard[0] || sNew == null) return;
                        if (sNew.contains("-fx-font-size: 11")) {
                            guard[0] = true;
                            l.setStyle(sNew.replace("-fx-font-size: 11", "-fx-font-size: " + fontSize));
                            guard[0] = false;
                        }
                    });
                }
            }
        };
        Platform.runLater(apply);
    }

    public static Scene buildSafetyScene(Pane safetyCB) {
        BorderPane decorated = wrapWithTitle("LASER BEAMS WE", safetyCB);
        decorated.setStyle("-fx-background-color: black;");
        return new Scene(decorated);
    }

    public static Scene buildLegendScene(Pane legend) {
        BorderPane decorated = wrapWithTitle("LEGEND", legend);
        decorated.setStyle("-fx-background-color: black;");
        return new Scene(decorated);
    }

    public static Scene buildVacScene(Pane vacPane) {
        // GLOBAL.fxml reserves the top 48 px for its own "VACUUM MONITORING" title bar
        // (Label at layoutY=3, prefHeight=35, plus a 10 px gap before the diagram at y=48).
        // We already render an external title in the wrapper, so crop those 48 px so the
        // diagram fills the cell from the top edge.
        // GLOBAL.fxml already paints its own cyan-bordered frame, so we skip the
        // contentPane border for this scene to avoid a doubled rectangle.
        BorderPane decorated = wrapWithTitle("VACUUM MONITORING", vacPane, 48.0, false);
        decorated.setStyle("-fx-background-color: black;");
        return new Scene(decorated);
    }

    private static BorderPane wrapWithTitle(String title, Pane content) {
        return wrapWithTitle(title, content, 0.0, true);
    }

    private static BorderPane wrapWithTitle(String title, Pane content, double topCrop) {
        return wrapWithTitle(title, content, topCrop, true);
    }

    private static BorderPane wrapWithTitle(String title, Pane content, double topCrop, boolean withBorder) {
        Label titleLabel = new Label(title);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setPrefHeight(42);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setStyle("-fx-text-fill: " + ACCENT_COLOR + "; -fx-font-size: 24; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: " + TITLE_BACKGROUND + "; -fx-background-radius: 5;");

        // Crop the top `topCrop` px of the content directly: clip the band in
        // local coords and translate the content up so the visible portion
        // lands at y=0 of the parent.
        if (topCrop > 0) {
            Rectangle contentClip = new Rectangle();
            contentClip.setX(0);
            contentClip.setY(topCrop);
            contentClip.widthProperty().bind(content.widthProperty());
            contentClip.heightProperty().bind(content.heightProperty().subtract(topCrop));
            content.setClip(contentClip);
            content.setTranslateY(-topCrop);
        }

        Group group = new Group(content);
        Scale scale = new Scale(1, 1, 0, 0);
        group.getTransforms().add(scale);

        StackPane contentPane = new StackPane(group);
        contentPane.setAlignment(Pos.TOP_LEFT);
        if (withBorder) {
            contentPane.setStyle("-fx-background-color: " + PANEL_BACKGROUND + "; -fx-border-color: " + ACCENT_COLOR + "; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
        } else {
            contentPane.setStyle("-fx-background-color: " + PANEL_BACKGROUND + ";");
        }

        final double margin = withBorder ? CONTENT_MARGIN : 0.0;
        final double[] natural = {0, 0};
        Runnable updateScale = () -> {
            double naturalW = natural[0];
            double effectiveH = natural[1] - topCrop;
            double availW = contentPane.getWidth() - 2 * margin;
            double availH = contentPane.getHeight() - 2 * margin;
            if (naturalW > 0 && effectiveH > 0 && availW > 0 && availH > 0) {
                double s = Math.min(availW / naturalW, availH / effectiveH);
                scale.setX(s);
                scale.setY(s);
                group.setTranslateX(margin + Math.max(0, (availW - naturalW * s) / 2));
                group.setTranslateY(margin + Math.max(0, (availH - effectiveH * s) / 2));
            }
        };
        content.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            if (newB.getWidth() > 0 && newB.getHeight() > 0) {
                natural[0] = newB.getWidth();
                natural[1] = newB.getHeight();
                Platform.runLater(updateScale);
            }
        });
        Bounds initial = content.getLayoutBounds();
        if (initial.getWidth() > 0 && initial.getHeight() > 0) {
            natural[0] = initial.getWidth();
            natural[1] = initial.getHeight();
            Platform.runLater(updateScale);
        }
        contentPane.widthProperty().addListener((obs, oldW, newW) -> Platform.runLater(updateScale));
        contentPane.heightProperty().addListener((obs, oldH, newH) -> Platform.runLater(updateScale));

        BorderPane decorated = new BorderPane();
        decorated.setStyle("-fx-background-color: black;");
        decorated.setTop(titleLabel);
        decorated.setCenter(contentPane);
        return decorated;
    }
}
