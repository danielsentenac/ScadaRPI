/*
 * Control Room GUI - O2 sensor side panel (left) + safety flags panels (center)
 */
import java.util.logging.Logger;
import java.util.logging.Level;

import java.awt.Color;
import javax.swing.*;
import java.awt.BorderLayout;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javax.swing.SwingUtilities;

import com.gluonapplication.ViewData;
import com.gluonapplication.ViewSafetyFlags;
import com.gluonapplication.SidePopupViewSensorO2;

public class GlgGui extends JFrame implements ChannelList, Runnable  {

    private static final long serialVersionUID = 354054054056L;
    private static final String PANEL_BACKGROUND = "#21304F";
    private static final String TITLE_BACKGROUND = "#0e1726";
    private static final String ACCENT_COLOR = "#00e5ff";
    private static final double O2_CONTENT_MARGIN = 4.0;
    private static final double SAFETY_CONTENT_MARGIN = 14.0;
    private DeviceManager deviceManager;
    public GlgGui parent;
    private String title;
    private Thread thread;
    private static final Logger logger = Logger.getLogger("Main");
    private JFXPanel jfxPanel;
    public boolean isSuspended = false;
    public java.util.Hashtable<String, GlgChildGui> subwindows = new java.util.Hashtable<>();

    public GlgGui (DeviceManager _deviceManager, String _title) {

       title = _title;
       deviceManager = _deviceManager;
       parent = this;
       SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            createAndShowGui();
          }
       });
    }

    public void doStart() {
      thread = new Thread(this);
      thread.start();
    }

    public void doStop() {
        if (thread != null) thread.interrupt();
        thread = null;
    }

    private void createAndShowGui () {

       try {
          logger.finer("GlgGui:createAndShowGui> Start Control Room Gui");

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

                  // Suppress NPE from embedded Gluon views when MobileApplication is not running
                  Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
                      if (e instanceof NullPointerException &&
                          e.getStackTrace().length > 0 &&
                          (e.getStackTrace()[0].getClassName().contains("SidePopupViewData") ||
                           e.getStackTrace()[0].getClassName().contains("ViewData"))) {
                          return; // swallow silently
                      }
                      e.printStackTrace();
                  });

                  // --- O2 sensor side panel (left) ---
                  SidePopupViewSensorO2 o2 = new SidePopupViewSensorO2("SENSORO2", "SENSORO2");
                  // Embedded side-panel views do not receive popup lifecycle events.
                  // Keep O2 active so it refreshes continuously like the Global view.
                  o2.isSuspended = false;
                  new Thread(o2).start();

                  // Title bar matching the safety panel style
                  Label o2Title = createPanelTitle("O₂ SENSORS");

                  javafx.scene.Group o2Group = new javafx.scene.Group(o2.pane);
                  javafx.scene.transform.Scale o2Scale = new javafx.scene.transform.Scale(1, 1, 0, 0);
                  o2Group.getTransforms().add(o2Scale);

                  // Content with cyan border, aligned top-left
                  StackPane contentArea = new StackPane(o2Group);
                  contentArea.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                  contentArea.setStyle("-fx-background-color: " + PANEL_BACKGROUND + "; -fx-border-color: " + ACCENT_COLOR + "; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");

                  // Assembled decorated panel
                  BorderPane o2Decorated = new BorderPane();
                  o2Decorated.setStyle("-fx-background-color: black;");
                  o2Decorated.setTop(o2Title);
                  o2Decorated.setCenter(contentArea);

                  StackPane o2Pane = new StackPane(o2Decorated);
                  o2Pane.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                  o2Pane.setStyle("-fx-background-color: black; -fx-padding: 8 0 8 0;");
                  double o2PaneWidth = screenW * 0.30;
                  o2Pane.setMinWidth(o2PaneWidth);
                  o2Pane.setPrefWidth(o2PaneWidth);
                  o2Pane.setMaxWidth(o2PaneWidth);

                  // Scale O2 content only, keeping the title aligned with the safety panels.
                  // Also override the embedded FXML background once it has loaded
                  final boolean[] bgOverrideDone = {false};
                  final double[] o2NaturalSize = {0, 0};
                  Runnable updateO2Scale = () -> {
                      double naturalWidth = o2NaturalSize[0];
                      double naturalHeight = o2NaturalSize[1];
                      double availableWidth = contentArea.getWidth() - 2 * O2_CONTENT_MARGIN;
                      double availableHeight = contentArea.getHeight() - 2 * O2_CONTENT_MARGIN;
                      if (naturalWidth > 0 && naturalHeight > 0 && availableWidth > 0 && availableHeight > 0) {
                          o2Scale.setX(availableWidth / naturalWidth);
                          o2Scale.setY(availableHeight / naturalHeight);
                          o2Group.setTranslateX(O2_CONTENT_MARGIN);
                          o2Group.setTranslateY(O2_CONTENT_MARGIN);
                      }
                  };
                  Runnable applyO2Overrides = () -> {
                      if (bgOverrideDone[0] || o2.pane == null) {
                          return;
                      }
                      o2.pane.setStyle("-fx-background-color: " + PANEL_BACKGROUND + ";");
                      String[] statusIds = {"ControllerCB", "ControllerNE", "ControllerWE",
                                            "AlarmCB", "AlarmNE", "AlarmWE"};
                      for (String id : statusIds) {
                          javafx.scene.Node n = o2.pane.lookup("#" + id);
                          if (n instanceof Label) {
                              Label l = (Label) n;
                              boolean[] guard = {false};
                              l.styleProperty().addListener((sObs, sOld, sNew) -> {
                                  if (guard[0] || sNew == null) return;
                                  if (sNew.contains("-fx-font-size: 11")) {
                                      guard[0] = true;
                                      l.setStyle(sNew.replace("-fx-font-size: 11", "-fx-font-size: 22"));
                                      guard[0] = false;
                                  }
                              });
                          }
                      }
                      bgOverrideDone[0] = true;
                  };
                  o2.pane.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
                      if (newB.getWidth() > 0 && newB.getHeight() > 0) {
                          o2NaturalSize[0] = newB.getWidth();
                          o2NaturalSize[1] = newB.getHeight();
                          Platform.runLater(updateO2Scale);
                      }
                      applyO2Overrides.run();
                  });
                  javafx.geometry.Bounds initialO2Bounds = o2.pane.getLayoutBounds();
                  if (initialO2Bounds.getWidth() > 0 && initialO2Bounds.getHeight() > 0) {
                      o2NaturalSize[0] = initialO2Bounds.getWidth();
                      o2NaturalSize[1] = initialO2Bounds.getHeight();
                      Platform.runLater(updateO2Scale);
                  }
                  Platform.runLater(applyO2Overrides);
                  contentArea.widthProperty().addListener((obs, oldW, newW) -> Platform.runLater(updateO2Scale));
                  contentArea.heightProperty().addListener((obs, oldH, newH) -> Platform.runLater(updateO2Scale));

                  // --- Safety flags panels (center) ---
                  ViewData safetyFlagsCB = new ViewSafetyFlags("CBSAFETYFLAGS", "SafetyFlagsCB");
                  ViewData safetyFlagsNE = new ViewSafetyFlags("NESAFETYFLAGS", "SafetyFlagsNE");
                  ViewData safetyFlagsWE = new ViewSafetyFlags("WESAFETYFLAGS", "SafetyFlagsWE");
                  startEmbeddedView(safetyFlagsCB);
                  startEmbeddedView(safetyFlagsNE);
                  startEmbeddedView(safetyFlagsWE);

                  StackPane safetyCBPane = createScaledSafetyPanel(safetyFlagsCB, "LASER BEAMS - CB");
                  StackPane safetyNEPane = createScaledSafetyPanel(safetyFlagsNE, "LASER BEAMS - NE");
                  StackPane safetyWEPane = createScaledSafetyPanel(safetyFlagsWE, "LASER BEAMS - WE");
                  StackPane safetyLegendPane = createLegendPanel();

                  javafx.scene.layout.VBox safetyRightPane = new javafx.scene.layout.VBox(8, safetyNEPane, safetyWEPane);
                  safetyRightPane.setMinSize(0, 0);
                  safetyRightPane.setStyle("-fx-background-color: black;");
                  javafx.scene.layout.VBox.setVgrow(safetyNEPane, javafx.scene.layout.Priority.ALWAYS);
                  javafx.scene.layout.VBox.setVgrow(safetyWEPane, javafx.scene.layout.Priority.ALWAYS);

                  javafx.scene.layout.VBox safetyLeftPane = new javafx.scene.layout.VBox(8, safetyCBPane, safetyLegendPane);
                  safetyLeftPane.setMinSize(0, 0);
                  safetyLeftPane.setStyle("-fx-background-color: black;");
                  safetyCBPane.setMinSize(0, 0);
                  safetyLegendPane.setMinSize(0, 0);
                  javafx.scene.layout.VBox.setVgrow(safetyCBPane, javafx.scene.layout.Priority.ALWAYS);
                  safetyLegendPane.prefHeightProperty().bind(safetyLeftPane.heightProperty().multiply(0.22));
                  safetyLegendPane.maxHeightProperty().bind(safetyLeftPane.heightProperty().multiply(0.22));

                  javafx.scene.layout.HBox safetyPane = new javafx.scene.layout.HBox(8, safetyLeftPane, safetyRightPane);
                  safetyPane.setStyle("-fx-background-color: black; -fx-padding: 8 8 40 8;");
                  javafx.scene.layout.HBox.setHgrow(safetyLeftPane, javafx.scene.layout.Priority.ALWAYS);
                  javafx.scene.layout.HBox.setHgrow(safetyRightPane, javafx.scene.layout.Priority.ALWAYS);
                  safetyLeftPane.prefWidthProperty().bind(safetyPane.widthProperty().multiply(0.64));
                  safetyRightPane.prefWidthProperty().bind(safetyPane.widthProperty().multiply(0.34));

                  // --- Root layout ---
                  BorderPane root = new BorderPane();
                  root.setStyle("-fx-background-color: black;");
                  root.setLeft(o2Pane);
                  root.setCenter(safetyPane);

                  Scene scene = new Scene(root, screenW, screenH);
                  jfxPanel.setScene(scene);
              }
          });

          this.doStart();
       }
       catch (Exception ex) {
          logger.log(Level.SEVERE, "GlgGui:createAndShowGui> " + ex.getMessage());
       }
    }

    public void exitProgram() {
       System.exit(0);
    }

    private void startEmbeddedView(ViewData view) {
       view.isSuspended = false;
       new Thread(view).start();
    }

    private Label createPanelTitle(String text) {
       Label titleLabel = new Label(text);
       titleLabel.setMaxWidth(Double.MAX_VALUE);
       titleLabel.setPrefHeight(42);
       titleLabel.setAlignment(javafx.geometry.Pos.CENTER);
       titleLabel.setStyle("-fx-text-fill: AQUA; -fx-font-size: 24; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: " + TITLE_BACKGROUND + "; -fx-background-radius: 5;");
       return titleLabel;
    }

    private StackPane createScaledSafetyPanel(ViewData view, String panelTitle) {
       view.setStyle("-fx-background-color: " + PANEL_BACKGROUND + ";");
       return createScaledNodePanel(view, panelTitle, SAFETY_CONTENT_MARGIN);
    }

    private StackPane createLegendPanel() {
       try {
          javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/SAFETYFLAGSLEGEND.fxml"));
          javafx.scene.layout.Pane legend = loader.load();
          return createScaledNodePanel(legend, "LEGEND", SAFETY_CONTENT_MARGIN);
       }
       catch (java.io.IOException ex) {
          logger.log(Level.SEVERE, "GlgGui:createLegendPanel> " + ex.getMessage());
          return createScaledNodePanel(new javafx.scene.layout.Pane(), "LEGEND", SAFETY_CONTENT_MARGIN);
       }
    }

    private StackPane createScaledNodePanel(javafx.scene.Node content, String panelTitle, double contentMargin) {
       Label titleLabel = createPanelTitle(panelTitle);

       javafx.scene.Group group = new javafx.scene.Group(content);
       javafx.scene.transform.Scale scale = new javafx.scene.transform.Scale(1, 1, 0, 0);
       group.getTransforms().add(scale);

       StackPane contentPane = new StackPane(group);
       contentPane.setAlignment(javafx.geometry.Pos.TOP_LEFT);
       contentPane.setStyle("-fx-background-color: " + PANEL_BACKGROUND + "; -fx-border-color: " + ACCENT_COLOR + "; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");

       final double[] naturalSize = {0, 0};
       Runnable updateScale = () -> {
          double naturalWidth = naturalSize[0];
          double naturalHeight = naturalSize[1];
          double availableWidth = contentPane.getWidth() - 2 * contentMargin;
          double availableHeight = contentPane.getHeight() - 2 * contentMargin;
          if (naturalWidth > 0 && naturalHeight > 0 && availableWidth > 0 && availableHeight > 0) {
             double newScale = Math.min(availableWidth / naturalWidth, availableHeight / naturalHeight);
             scale.setX(newScale);
             scale.setY(newScale);
             group.setTranslateX(contentMargin + Math.max(0, (availableWidth - naturalWidth * newScale) / 2));
             group.setTranslateY(contentMargin + Math.max(0, (availableHeight - naturalHeight * newScale) / 2));
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

    public void run () {
       try {
          while (true) {
             Thread.sleep(1000);
          }
       }
       catch (InterruptedException ex) {
          logger.log(Level.SEVERE, "GlgGui:run:InterruptedException> " + ex.getMessage());
       }
       logger.finer("GlgGui:run> Exiting Control Room Gui");
    }
}
