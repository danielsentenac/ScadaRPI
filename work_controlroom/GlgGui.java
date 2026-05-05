/*
 * Control Room GUI - O2 sensor side panel (left) + vacuum panel (center)
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
import com.gluonapplication.ViewGlobal;
import com.gluonapplication.SidePopupViewSensorO2;

public class GlgGui extends JFrame implements ChannelList, Runnable  {

    private static final long serialVersionUID = 354054054056L;
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

                  // Suppress NPE from SidePopupViewData when MobileApplication is not running
                  Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
                      if (e instanceof NullPointerException &&
                          e.getStackTrace().length > 0 &&
                          e.getStackTrace()[0].getClassName().contains("SidePopupViewData")) {
                          return; // swallow silently
                      }
                      e.printStackTrace();
                  });

                  // --- O2 sensor side panel (left) ---
                  SidePopupViewSensorO2 o2 = new SidePopupViewSensorO2("SENSORO2", "SENSORO2");
                  new Thread(o2).start();

                  // Title bar matching vacuum panel style
                  Label o2Title = new Label("O2 SENSOR MONITORING");
                  o2Title.setStyle("-fx-text-fill: #00e5ff; -fx-font-size: 20; -fx-font-weight: bold;");
                  StackPane titleBar = new StackPane(o2Title);
                  titleBar.setStyle("-fx-background-color: #21304F; -fx-padding: 14 0 14 0;");

                  // Content with cyan border, aligned top-left
                  StackPane contentArea = new StackPane(o2.pane);
                  contentArea.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                  contentArea.setStyle("-fx-border-color: #00e5ff; -fx-border-width: 2; -fx-padding: 4;");

                  // Assembled decorated panel
                  BorderPane o2Decorated = new BorderPane();
                  o2Decorated.setStyle("-fx-background-color: #21304F;");
                  o2Decorated.setTop(titleBar);
                  o2Decorated.setCenter(contentArea);

                  // Scale from top-left corner (pivot 0,0) to avoid center-offset shift
                  javafx.scene.transform.Scale o2Scale = new javafx.scene.transform.Scale(1, 1, 0, 0);
                  o2Decorated.getTransforms().add(o2Scale);

                  StackPane o2Pane = new StackPane(o2Decorated);
                  o2Pane.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                  o2Pane.setStyle("-fx-background-color: #21304F;");

                  // Scale O2: uniform scale (min of X/Y) so text is not distorted
                  // Also override the hardcoded #333333 backgroundPane once FXML has loaded
                  final boolean[] bgOverrideDone = {false};
                  o2Decorated.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
                      if (newB.getWidth() > 0 && newB.getHeight() > 0) {
                          double scaleX = (screenW * 0.20) / newB.getWidth();
                          double scaleY = (double) screenH / newB.getHeight();
                          double scale = Math.min(scaleX, scaleY);
                          o2Scale.setX(scale);
                          o2Scale.setY(scale);
                          double scaledW = newB.getWidth() * scale;
                          o2Pane.setMinWidth(scaledW);
                          o2Pane.setPrefWidth(scaledW);
                          o2Pane.setMaxWidth(scaledW);
                          if (!bgOverrideDone[0] && o2.pane != null) {
                              o2.pane.setStyle("-fx-background-color: #21304F;");
                              bgOverrideDone[0] = true;
                          }
                      }
                  });

                  // --- Vacuum panel (center) ---
                  ViewData global = new ViewGlobal("GLOBAL", "GLOBAL");
                  new Thread(global).start();

                  // Wrap in Group so layoutBounds = content bounds (no internal layout interference)
                  javafx.scene.Group vacGroup = new javafx.scene.Group(global);
                  // Scale from top-left corner (pivot 0,0) applied to the Group
                  javafx.scene.transform.Scale vacScale = new javafx.scene.transform.Scale(1, 1, 0, 0);
                  vacGroup.getTransforms().add(vacScale);

                  StackPane vacPane = new StackPane(vacGroup);
                  vacPane.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                  vacPane.setStyle("-fx-background-color: black;");

                  final double[] vacNatural = {0, 0};
                  Runnable updateVacScale = () -> {
                      double nW = vacNatural[0], nH = vacNatural[1];
                      double aW = vacPane.getWidth();
                      if (nW > 0 && nH > 0 && aW > 0) {
                          vacScale.setX(aW / nW);
                          vacScale.setY((double) screenH / nH);
                      }
                  };
                  global.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
                      if (newB.getWidth() > 0 && newB.getHeight() > 0) {
                          vacNatural[0] = newB.getWidth();
                          vacNatural[1] = newB.getHeight();
                          Platform.runLater(updateVacScale);
                      }
                  });
                  vacPane.widthProperty().addListener((obs, o, n) -> Platform.runLater(updateVacScale));

                  // --- Root layout ---
                  BorderPane root = new BorderPane();
                  root.setStyle("-fx-background-color: black;");
                  root.setLeft(o2Pane);
                  root.setCenter(vacPane);

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
