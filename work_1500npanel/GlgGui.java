/*
 * This Class is used for GUI using Glg Toolkit
 */
import com.genlogic.*;
import java.util.*;

import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.awt.Color;
import javax.swing.*;
import java.awt.event.*;
import java.awt.Frame;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BoxLayout;
import java.awt.Insets;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import static javafx.concurrent.Worker.State.FAILED;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker.State;
import javafx.scene.Node;
import javafx.collections.*;
import javafx.collections.ListChangeListener.*;

import java.lang.Runtime;
import java.lang.System;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.SwingUtilities;

import com.gluonapplication.ViewData;
import com.gluonapplication.ViewGlobal;

public class GlgGui extends JFrame implements ChannelList, Runnable  {

    private static final long serialVersionUID = 354054054055L;
    private DeviceManager deviceManager;
    public GlgGui parent;
    private String title;
    private Thread thread;
    private static final Logger logger = Logger.getLogger("Main");
    private Container content;
    private JPanel panel;
    private JFXPanel jfxPanel1;
    private JFXPanel jfxPanel2;
    private JFXPanel jfxPanel3;
    private WebEngine engine1;
    private WebEngine engine2;
    private JPanel panelweb = new JPanel();
    private JPanel panelvac = new JPanel();
    private GlgJLWBean glg_bean1;
    private GlgJLWBean glg_bean2;
    public Hashtable<String, GlgChildGui> subwindows;
    public boolean isSuspended = false;
    private long pageTime = 100000;
    private long lastTime = 0;
    private boolean isVimLoaded = true;
    // O2 variables
    private SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss");
    
    private int sleepTime = 1000;

    public GlgGui (DeviceManager _deviceManager, String _title) {

       title = _title;
       deviceManager = _deviceManager;
       parent = this;
       // Creating the display
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
        // Change the states of variable
        thread = null;
   }

    private void createAndShowGui () {
       
       try {
          logger.finer("GlgGui:createAndShowGui> Start Main Gui");
          
          //BoxLayout box = new BoxLayout(panelweb,BoxLayout.X_AXIS);
          panelweb.setLayout(new GridBagLayout());
          JPanel panel  = new JPanel(new GridBagLayout());
          panel.setBackground(Color.darkGray);
          panelweb.setBackground(Color.darkGray);
          panelvac.setBackground(Color.darkGray);
          this.getContentPane().add(panel);
          this.setExtendedState(JFrame.MAXIMIZED_BOTH);
          this.setUndecorated(true);
          this.setVisible(true);
          // Attach Beans to Frame
          glg_bean1 = new GlgJLWBean();
          glg_bean1.SetDrawingFile(mainDrawing1);
          glg_bean2 = new GlgJLWBean();
          glg_bean2.SetDrawingFile(mainDrawing2);
          
          
          // Add DMS & VIM
          initWebComponents();
          
          // Add Vac supervisor
          initVacComponents();
          
          // Add component to a frame
          panel.add(glg_bean1, new GridBagConstraints(0, 1, 1, 1, 0.48, 0.5
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));
          panel.add(glg_bean2, new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));
	  panel.add(jfxPanel3, new GridBagConstraints(2, 1, 1, 1, 0.54, 0.5
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));
          panel.add(panelweb, new GridBagConstraints(0, 0, 3, 1, 0.95, 0.95
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));	
	  			     
          
          glg_bean1.SetSTag("title", title + "PARTICLE MONITORING", true);
          glg_bean2.SetSTag("title", title + "PRESSURE GAUGES", true);
          
          
          // Start thread
          this.doStart();
          
          // Init Time
          lastTime = System.currentTimeMillis();
       }
       catch (Exception ex) {
          logger.log(Level.SEVERE, "GlgGui:createAndShowGui> " + ex.getMessage());
       }
    }
    private void initWebComponents() {
        jfxPanel1 = new JFXPanel();
        jfxPanel2 = new JFXPanel();
        createWebScene();
        panelweb.add(jfxPanel1, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));	
        panelweb.add(jfxPanel2, new GridBagConstraints(1, 0, 1, 1, 0.7, 0.7
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));	
    }
    
    private void initVacComponents() {
        jfxPanel3 = new JFXPanel();
        createVacScene();
      
    }
    private void createVacScene() {

        Platform.runLater(new Runnable() {
            @Override public void run() {
                ViewData global = new ViewGlobal("GLOBAL", "GLOBAL");
                new Thread(global).start();
                Scene sceneVac = new Scene(global);
                jfxPanel3.setScene(sceneVac);
                }
          });
    }
    private void createWebScene() {

        Platform.runLater(new Runnable() {
            @Override public void run() {
                WebView view1 = new WebView();
                engine1 = view1.getEngine();
               
                view1.setZoom(0.10);
                view1.setFontScale(7);
                
                // hide webview scrollbars whenever they appear.
                view1.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
                  @Override public void onChanged(Change<? extends Node> change) {
                                Set<Node> deadSeaScrolls = view1.lookupAll(".scroll-bar");
                                for (Node scroll : deadSeaScrolls) {
                                    scroll.setVisible(false);
                                }
                            }
                });

                engine1.getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
                            public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
                                if (engine1.getLoadWorker().getState() == FAILED) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override public void run() {
                                            JOptionPane.showMessageDialog(
                                                    panel,
                                                    (value != null) ?
                                                    engine1.getLocation() + "\n" + value.getMessage() :
                                                    engine1.getLocation() + "\nUnexpected error.",
                                                    "Loading error...",
                                                    JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                }
                            }
                        });
                jfxPanel1.setScene(new Scene(view1));
                
                WebView view2 = new WebView();
                engine2 = view2.getEngine();
               
                view2.setZoom(0.10);
                view2.setFontScale(7);
                
                // hide webview scrollbars whenever they appear.
                view2.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
                  @Override public void onChanged(Change<? extends Node> change) {
                                Set<Node> deadSeaScrolls = view2.lookupAll(".scroll-bar");
                                for (Node scroll : deadSeaScrolls) {
                                    scroll.setVisible(false);
                                }
                            }
                });
                engine2.getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
                            public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
                                if (engine2.getLoadWorker().getState() == FAILED) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override public void run() {
                                            JOptionPane.showMessageDialog(
                                                    panel,
                                                    (value != null) ?
                                                    engine2.getLocation() + "\n" + value.getMessage() :
                                                    engine2.getLocation() + "\nUnexpected error.",
                                                    "Loading error...",
                                                    JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                }
                            }
                        });
                jfxPanel2.setScene(new Scene(view2));
            }
        });
    }
    public void loadURL1() {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                engine1.load("https://dms.virgo-gw.eu/");
            }
        });
    }
    
    public void loadURL2() {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                engine2.load("https://vim.virgo-gw.eu/");
            }
        });
    }

    public void exitProgram() {
       System.exit(0);
    }
    public void updateGui() {
    
       //
       // Check first connection OK
       //
       Device dev1 = deviceManager.getDevice("SC");
       if ( dev1 != null ) {
          DataElement dcom = dev1.getDataElement("COMST");
          if ( dcom.value == 1 ) {
              glg_bean1.SetGTag( "titleCol", new GlgPoint(0.5,0.5,0.5), true ); // Title grey for no connection
          }
          else
              glg_bean1.SetGTag( "titleCol", new GlgPoint(0.,1.0,1.0), true ); // Title blue for good connection
             
       }
       // Update PANEL1 data
       for (Map.Entry<String, String> e : PANEL1STATUS.entrySet()) {
         String glgName =  e.getKey();
         String dataName = e.getValue();
         if ( dataName == null ) continue;
         // Get corresponding device from deviceManager
         String deviceName = dataName.split("_",2)[0];
         Device device = deviceManager.getDevice(deviceName);
         // Get corresponding data element from device
         if ( device != null ) {
            DataElement dataElement = device.getDataElement(dataName.split("_",2)[1]);
            if (dataElement == null) {
               logger.finer("GlgGui:updateGui> dataName= " + dataName + " glgName=" + glgName + " device name=" + deviceName + " does not EXIST!");
               continue;
            }
            try {
               logger.finer("GlgGui:updateGui> Treating  dataName= " + dataName + " glgName=" + glgName + " device name=" + deviceName + " dataElement.value=" + dataElement.value);
               String glgNameTmp = glgName.replace("Str", "");
               glgNameTmp = glgNameTmp.replace(deviceName,""); // Remove device name in glgNameTmp String
               if (!glgName.contains("sub")) {
                  if ( glgName.contains("Running")  )// Instrument Running flag
                     glg_bean1.SetGTag(glgName, PCounterMobileColorSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Error") )// Instrument Error flag
                     glg_bean1.SetGTag(glgName, PCounterColorINSTRSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Com") )  // Instrument Com flag
                     glg_bean1.SetGTag(glgName, PCounterColorCOMSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Status") )  // Instrument Channel flag
                     glg_bean1.SetGTag(glgName, PCounterColorFLAGSTATUS.get((int)dataElement.value), true);
               }
               if ( glgName.contains("Val") && // Val is a tag for object value (double type) property
                   !glgName.contains("sub")) {  // sub is reserved for childGui only
                  glg_bean1.SetDTag(glgName, dataElement.value, true);
                  logger.finer("GlgGui:updateGui> UPDATE Val for glgName=" + glgName + " dataElement name=" + 
                    dataElement.name + " type=" + dataElement.type + " value=" + 
                    dataElement.value);
               }
            }
            catch (Exception ex) {
               ex.printStackTrace();
            }
          }
       } 
       // Update PANEL2 data
       
       for (Map.Entry<String, String> e : PANEL2STATUS.entrySet()) {
         String glgName =  e.getKey();
         String dataName = e.getValue();
         if ( dataName == null ) continue;
         // Get corresponding device from deviceManager
         String deviceName = dataName.split("_",2)[0];
         Device device = deviceManager.getDevice(deviceName);
         
         // Get corresponding data element from device
         if ( device != null ) {
            DataElement dataElement = device.getDataElement(dataName.split("_",2)[1]);
            if (dataElement == null) {
               logger.finer("GlgGui:updateGui> dataName= " + dataName + " glgName=" + glgName + " device name=" + deviceName + " does not EXIST!");
               continue;
            }
            try {
               logger.finer("GlgGui:updateGui> Treating  dataName= " + dataName + " glgName=" + glgName + " device name=" + deviceName + " dataElement.value=" + dataElement.value);
               if (glgName.contains("DAQ")) {
		    try {
		          glg_bean2.SetSTag(glgName, dataName.replace(deviceName+"_",""), true);
		        }
		    catch (Exception ex) {
		        ex.printStackTrace();
		    }
                } 
                else if (glgName.contains("VAL")) {
                    try {
                            glg_bean2.SetDTag(glgName, dataElement.value, true);
                        }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                else if (glgName.contains("BUTTON")) {
                    int tmp = 0;
                    if ( (int) dataElement.value == 1 )
                        tmp = 0;
                    else
                        tmp = 1;
                    glg_bean2.SetDTag(glgName, tmp, true);
                }
	        //
		//  Update time
		//
		glg_bean2.SetSTag( "TimeStr",formatter.format(new Date()), true);         
	    }
	    catch (Exception ex) {
		ex.printStackTrace();
	    }
          }
       } 
    }
    
    public void run () {
       
       try {
          loadURL1();
          loadURL2();
          while (true) {
	     // Get rid of GlgLogic popup window
	     Frame[] frames = Frame.getFrames();
             for (Frame frame : frames) {
                if (frame.getClass().getCanonicalName().contains("JFrame")) { 
                   frame.setVisible(false); 
                   frame.dispose();
                }
             }
             if (isSuspended == false) {
                // Update Glg Gui tags
                updateGui();	
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                   public void run() {
                      // Update Bean
                      glg_bean1.validate();
                      glg_bean1.repaint();
                      // Update Bean
                      glg_bean2.validate();
                      glg_bean2.repaint();
                   // Reload page periodically
                      if ( System.currentTimeMillis() > pageTime + lastTime) {
                         lastTime = System.currentTimeMillis();
                         // Load web page
                         loadURL1();
                         loadURL2();
                      }                 
                   }
                });
             }
             Thread.sleep(sleepTime);
	  }
       }
       catch (InterruptedException ex) {
          logger.log(Level.SEVERE, "GlgGui:run:InterruptedException> " + ex.getMessage());
       }
       catch (NullPointerException ex) {
          logger.log(Level.SEVERE, "GlgGui:run:NullPointerException> " + ex.getMessage());
       }
       catch (Exception ex) {
          logger.log(Level.SEVERE, "GlgGui:run:Exception> " + ex.getMessage());
       }    
       logger.finer("GlgGui:run> Exiting Main Gui");
    }    
}
