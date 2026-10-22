/*
 * Vacuum Supervisor GUI - the global vacuum view (GLOBAL.fxml / ViewGlobal from
 * MainApp.jar, the same one embedded at the bottom right of work_mainpanel),
 * shown alone and scaled to fill the screen.
 */
import java.util.logging.Logger;
import java.util.logging.Level;

import java.awt.Color;
import javax.swing.*;
import java.awt.BorderLayout;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javax.swing.SwingUtilities;

import com.gluonapplication.ViewData;
import com.gluonapplication.ViewGlobal;

public class GlgGui extends JFrame {

    private static final long serialVersionUID = 354054054057L;
    // Same as the GLOBAL.fxml / ViewData background so the view blends in.
    private static final String PANEL_BACKGROUND = "#333333";
    private static final String TITLE_BACKGROUND = "#0e1726";
    private static final String ACCENT_COLOR = "#00e5ff";
    private static final double CONTENT_MARGIN = 14.0;
    private static final Logger logger = Logger.getLogger("Main");
    private String title;
    private JFXPanel jfxPanel;

    public GlgGui (String _title) {

       title = _title;
       SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            createAndShowGui();
          }
       });
    }

    private void createAndShowGui () {

       try {
          logger.finer("GlgGui:createAndShowGui> Start Vacuum Supervisor Gui");

          jfxPanel = new JFXPanel();
          this.getContentPane().setBackground(Color.black);
          this.getContentPane().setLayout(new BorderLayout());
          this.getContentPane().add(jfxPanel, BorderLayout.CENTER);
          this.setExtendedState(JFrame.MAXIMIZED_BOTH);
          this.setUndecorated(true);
          this.setVisible(true);

          java.awt.Rectangle maxBounds = java.awt.GraphicsEnvironment
                  .getLocalGraphicsEnvironment().getMaximumWindowBounds();
          final int screenW = maxBounds.width;
          final int screenH = maxBounds.height;

          Platform.runLater(new Runnable() {
              @Override public void run() {

                  // Suppress NPE from the embedded Gluon view when MobileApplication is not
                  // running (ViewData shows/hides its error layers through it).
                  Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
                      if (e instanceof NullPointerException &&
                          e.getStackTrace().length > 0 &&
                          e.getStackTrace()[0].getClassName().contains("ViewData")) {
                          return; // swallow silently
                      }
                      e.printStackTrace();
                  });

                  ViewData global = new ViewGlobal("GLOBAL", "GLOBAL");
                  // Embedded views do not receive Gluon show/hide events: keep polling.
                  global.isSuspended = false;
                  new Thread(global).start();

                  StackPane panel = createScaledNodePanel(global, title);
                  panel.setStyle("-fx-background-color: black; -fx-padding: 8;");

                  Scene scene = new Scene(panel, screenW, screenH);
                  jfxPanel.setScene(scene);
              }
          });
       }
       catch (Exception ex) {
          logger.log(Level.SEVERE, "GlgGui:createAndShowGui> " + ex.getMessage());
       }
    }

    public void exitProgram() {
       System.exit(0);
    }

    private Label createPanelTitle(String text) {
       Label titleLabel = new Label(text);
       titleLabel.setMaxWidth(Double.MAX_VALUE);
       titleLabel.setPrefHeight(42);
       titleLabel.setAlignment(javafx.geometry.Pos.CENTER);
       titleLabel.setStyle("-fx-text-fill: AQUA; -fx-font-size: 24; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: " + TITLE_BACKGROUND + "; -fx-background-radius: 5;");
       return titleLabel;
    }

    // Uniform scale of the view's natural (FXML) size into the available area, centered.
    private StackPane createScaledNodePanel(javafx.scene.Node content, String panelTitle) {
       Label titleLabel = createPanelTitle(panelTitle);

       Group group = new Group(content);
       Scale scale = new Scale(1, 1, 0, 0);
       group.getTransforms().add(scale);

       StackPane contentPane = new StackPane(group);
       contentPane.setAlignment(javafx.geometry.Pos.TOP_LEFT);
       contentPane.setStyle("-fx-background-color: " + PANEL_BACKGROUND + "; -fx-border-color: " + ACCENT_COLOR + "; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");

       final double[] naturalSize = {0, 0};
       Runnable updateScale = () -> {
          double naturalWidth = naturalSize[0];
          double naturalHeight = naturalSize[1];
          double availableWidth = contentPane.getWidth() - 2 * CONTENT_MARGIN;
          double availableHeight = contentPane.getHeight() - 2 * CONTENT_MARGIN;
          if (naturalWidth > 0 && naturalHeight > 0 && availableWidth > 0 && availableHeight > 0) {
             double newScale = Math.min(availableWidth / naturalWidth, availableHeight / naturalHeight);
             scale.setX(newScale);
             scale.setY(newScale);
             group.setTranslateX(CONTENT_MARGIN + Math.max(0, (availableWidth - naturalWidth * newScale) / 2));
             group.setTranslateY(CONTENT_MARGIN + Math.max(0, (availableHeight - naturalHeight * newScale) / 2));
          }
       };
       content.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
          if (newB.getWidth() > 0 && newB.getHeight() > 0) {
             naturalSize[0] = newB.getWidth();
             naturalSize[1] = newB.getHeight();
             Platform.runLater(updateScale);
          }
       });
       javafx.geometry.Bounds initialBounds = content.getLayoutBounds();
       if (initialBounds.getWidth() > 0 && initialBounds.getHeight() > 0) {
          naturalSize[0] = initialBounds.getWidth();
          naturalSize[1] = initialBounds.getHeight();
          Platform.runLater(updateScale);
       }
       contentPane.widthProperty().addListener((obs, oldW, newW) -> Platform.runLater(updateScale));
       contentPane.heightProperty().addListener((obs, oldH, newH) -> Platform.runLater(updateScale));

       BorderPane decorated = new BorderPane();
       decorated.setStyle("-fx-background-color: black;");
       decorated.setTop(titleLabel);
       decorated.setCenter(contentPane);

       StackPane panel = new StackPane(decorated);
       panel.setStyle("-fx-background-color: black;");
       return panel;
    }
}
