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
    private long pageTime = 30000;
    private long lastTime = 0;
    private boolean isVimLoaded = true;
    // O2 variables
    private SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss");
    private boolean GeneralOKst = true;
    private boolean NiOKst = true;
    private boolean WiOKst = true;
    private boolean InjOKst = true;
    private boolean DetOKst = true;
    private boolean TankOKst = true;
    private boolean CleanOKst = true;
    private boolean NEGeneralOKst = true;
    private boolean NETuOKst = true;
    private boolean NECryoOKst = true;
    private boolean NEBaseOKst = true;
    private boolean NECleanOKst = true;
    private boolean WEGeneralOKst = true;
    private boolean WETuOKst = true;
    private boolean WECryoOKst = true;
    private boolean WEBaseOKst = true;
    private boolean WECleanOKst = true;
    private String GeneralStr = "";
    private String NiStr = "";
    private String WiStr = "";
    private String InjStr = "";
    private String DetStr = "";
    private String CleanStr = "";
    private String TankStr = "";
    private String NEGeneralStr = "";
    private String NETuStr = "";
    private String NECryoStr = "";
    private String NEBaseStr = "";
    private String NECleanStr = "";
    private String WEGeneralStr = "";
    private String WETuStr = "";
    private String WECryoStr = "";
    private String WEBaseStr = "";
    private String WECleanStr = "";
    
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
	  			     
          
          glg_bean1.SetSTag("title", title, true);
          
          
          
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

       /*logger.finer("GlgGui:createAndShowGui: Stopping devices threads...");
       for (Map.Entry<String, Device> d : deviceManager.deviceList.entrySet())
          (d.getValue()).doStop();
       logger.finer("GlgGui:createAndShowGui: Stopping child gui threads...");
       for (String key: subwindows.keySet())
          subwindows.get(key).doStop();
       logger.finer("GlgGui:createAndShowGui: Stopping main gui thread...");
       this.doStop();*/
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
              glg_bean1.SetGTag( "titleCol", new GlgPoint(0.,1.0,1.0), true ); // Title grey for no connection
             
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
                     glg_bean1.SetGTag(glgName, PCounterColorSTATUS.get((int)dataElement.value), true);
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
       GeneralOKst = true;
       NiOKst = true;
       WiOKst = true;
       InjOKst = true;
       DetOKst = true;
       TankOKst = true;
       CleanOKst = true;
       /*
       NEGeneralOKst = true;
       NETuOKst = true;
       NECryoOKst = true;
       NEBaseOKst = true;
       NECleanOKst = true;
       WEGeneralOKst = true;
       WETuOKst = true;
       WECryoOKst = true;
       WEBaseOKst = true;
       WECleanOKst = true;
       */
       GeneralStr = "";
       NiStr = "";
       WiStr = "";
       InjStr = "";
       DetStr = "";
       CleanStr = "";
       TankStr = "";
       /*
       NEGeneralStr = "";
       NETuStr = "";
       NECryoStr = "";
       NEBaseStr = "";
       NECleanStr = "";
       WEGeneralStr = "";
       WETuStr = "";
       WECryoStr = "";
       WEBaseStr = "";
       WECleanStr = "";
       */
       //
       // Check first that connection is OK
       //
       Device dev = deviceManager.getDevice("SC2");
       if ( dev != null ) {
          DataElement dcom = dev.getDataElement("COMST");
          if ( dcom.value == 1 ) {
              glg_bean2.SetGTag( "GeneralCol", new GlgPoint(0.5,0.5,0.5), true ); // General color Alarm
      	      glg_bean2.SetGTag( "CBZoneFrameCol", new GlgPoint(0.5,0.5,0.5), true ); // General Frame color Alarm
      	      glg_bean2.SetGTag( "TankZoneCol", new GlgPoint(0.5,0.5,0.5), true ); // Tank color Alarm
              glg_bean2.SetGTag( "TankZoneFrameCol", new GlgPoint(0.5,0.5,0.5), true ); // Tank Frame color Alarm
              glg_bean2.SetGTag( "WiZoneCol", new GlgPoint(0.5,0.5,0.5), true ); // Wi color Alarm
              glg_bean2.SetGTag( "WiZoneFrameCol", new GlgPoint(0.5,0.5,0.5), true ); // Wi Frame color Alarm
              glg_bean2.SetGTag( "NiZoneCol", new GlgPoint(0.5,0.5,0.5), true ); // Ni color Alarm
              glg_bean2.SetGTag( "NiZoneFrameCol", new GlgPoint(0.5,0.5,0.5), true ); // Ni Frame color Alarm
              glg_bean2.SetGTag( "InjZoneCol", new GlgPoint(0.5,0.5,0.5), true ); // Inj color Alarm
              glg_bean2.SetGTag( "InjZoneFrameCol", new GlgPoint(0.5,0.5,0.5), true ); // Inj Frame color Alarm
              glg_bean2.SetGTag( "DetZoneCol", new GlgPoint(0.5,0.5,0.5), true ); // Det color Alarm
              glg_bean2.SetGTag( "DetZoneFrameCol", new GlgPoint(0.5,0.5,0.5), true ); // Det Frame color Alarm
              glg_bean2.SetGTag( "CleanZoneCol", new GlgPoint(0.5,0.5,0.5), true ); // Clean color Alarm
              glg_bean2.SetGTag( "CleanZoneFrameCol", new GlgPoint(0.5,0.5,0.5), true ); // Clean Frame color Alarm
              for (Map.Entry<String, String> e : PANEL2STATUS.entrySet()) {
         		   String glgName =  e.getKey();
                           try {
         			if ( glgName.contains("Col") && 
                                     (glgName.contains("ZONE") || 
                                      glgName.contains("LN2VALVE") ||
                                      glgName.contains("SIREN"))) {
		    			    glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(255), true ); 
                                            glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(255), true );
		    		   }
		    	   }
                           catch (Exception ex) {}
              }
              return;
          }
       }
       boolean AlrmSt = false;
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
               
               //
               // SENSORS O2 STATUS UPDATE
               //     
               if ( glgName.contains("Col") && 
                    glgName.contains("ZONE") && 
		    !glgName.contains("SIREN") &&
		    !glgName.contains("VALVE")) { // Central building        
                     //System.out.println("Tag: " + glgName + " (Channel: " + glgName + ")");
                     int[] status = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};// There are 16 status bits
		     for (int k = 0 ; k < 16 ; k++) status[k] = ((int)(dataElement.value) >> k) & 1;
	             if ( status[2] == 1 || status[3] == 1 || status[4] == 1 || status[11] == 1 || status[15] == 1 ) { // Alarm (1,2,3 levels), Alarm to be resetted - Report
			             
			 if ( status[2] == 1 || status[3] == 1 || status[4] == 1) {
			     GeneralOKst = false;
			     GeneralStr = "CENTRAL BUILDING: DO NOT ENTER/NON ENTRARE;CALL/CONTATTARE VACUUM ONCALL/CONTROL ROOM";
			 }
			 if (glgName.contains("ZONE_A")) { // Tank Zone
			     TankOKst = false;
			     TankStr = "WARNING Tank Zone";
			 }
			 if (glgName.contains("ZONE_B") || glgName.contains("ZONE_C")) { // WI Zone
			     WiOKst = false;
			     WiStr = "WARNING West Input Zone";
			 }
			 if (glgName.contains("ZONE_E") || glgName.contains("ZONE_D")) { // NI Zone
			     NiOKst = false;
			     NiStr = "WARNING North Input Zone";
			 }
			 if (glgName.contains("ZONE_F")) { // Detection Zone
			     DetOKst = false;
			     DetStr = "WARNING Detection Zone";
			 }
			 if (glgName.contains("ZONE_G")) { // Injection Zone
			     InjOKst = false;
			     InjStr = "WARNING Injection Zone";
			 }
			 if (glgName.contains("CLEAN")) { // Clean Room Zone
			     CleanOKst = false;
			     CleanStr = "WARNING Clean Room Zone";
			 }
		     }    
		     if ( status[7] == 1 ) {
                         if (!glgName.contains("PUSH")) {
                             glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(0), true ); // Normal Working
                             glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(0), true );
                         }
                         else 
                             glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(0), true ); // Normal Working
                     }
		     if ( status[2] == 1 ) {
                         AlrmSt = true;
                         if (!glgName.contains("PUSH")) {
                             glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(1), true ); // Alarm Level 1
                             glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(1), true );
                         }
                         else 
                             glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(1), true ); // Alarm Level 1
                     }
                     if ( status[3] == 1 ) {
                        AlrmSt = true;
                        if (!glgName.contains("PUSH")) {
                            glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(2), true ); // Alarm Level 2
                            glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(2), true );
                        }
                        else 
                            glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(2), true ); // Alarm Level 2
                     }
                     if ( status[4] == 1 ) {
                         AlrmSt = true;
                         if (!glgName.contains("PUSH")) {
                             glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(3), true ); // Alarm Level 3
                             glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(3), true );
                         }
                         else 
                             glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(3), true ); // Alarm Level 3
                     }
                     if ( status[0] == 1 ){
                          if (!glgName.contains("PUSH")) {
                              glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(5), true ); // Faulty
                              glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(5), true );
                          }
                          else 
                              glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(5), true ); // Faulty
                     }
                     if ( (status[11] == 1 || status[15] == 1  || status[10] == 1 || status[14] == 1) && AlrmSt == false){
                         if (!glgName.contains("PUSH")) {
                             glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(6), true ); // Alarm/Fault to be resetted
                             glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(6), true );
                         }
                         else 
                             glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(6), true ); // Alarm/Fault to be resetted
                     }
                     if ( status[8] == 1 ){
                         if (!glgName.contains("PUSH")) {
                             glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(4), true ); // Unset
                             glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(4), true );
                         }
                         else 
                             glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(4), true ); // Unset
                     }
               }       
               /*     
               if ( glgName.contains("Col") && 
                   glgName.contains("NE_") && 
		   !glgName.contains("SIREN") &&
		   !glgName.contains("VALVE")) { // North End building        
                     //System.out.println("Tag: " + glgName + " (Channel: " + glgName + ")");
                   int[] status = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};// There are 16 status bits
                   for (int k = 0 ; k < 16 ; k++) status[k] = ((int)(dataElement.value) >> k) & 1;
	           if ( status[2] == 1 || status[3] == 1 || status[4] == 1 || status[11] == 1 || status[15] == 1 ) { // Alarm (1,2,3 levels), Alarm to be resetted - Report
			             
	               if ( status[2] == 1 || status[3] == 1 || status[4] == 1) {
		           NEGeneralOKst = false;
			   NEGeneralStr = "NORTH END BUILDING: DO NOT ENTER/NON ENTRARE;CALL/CONTATTARE VACUUM ONCALL/CONTROL ROOM";
		       }
		       if (glgName.contains("TOWER")) { // Cryotrap Zone
		           NECryoOKst = false;
			   NECryoStr = "WARNING Cryotrap Zone";
		       }
		       if (glgName.contains("TUNNEL")) { // Tunnel Zone
		           NETuOKst = false;
		           NETuStr = "WARNING Tunnel Zone";
		       }
		       if (glgName.contains("CLEAN")) { // Cleanroom Zone
		           NECleanOKst = false;
		           NECleanStr = "WARNING Cleanroom Zone";
		       }
	               if (glgName.contains("BASE")) { // Baseroom Zone
			   NEBaseOKst = false;
			   NEBaseStr = "WARNING Baseroom Zone";
		       }
	           }
		   if ( status[7] == 1 ) {
                       if (!glgName.contains("PUSH")) {
                           glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(0), true ); // Normal Working
                           glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(0), true );
                       }
                       else 
                           glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(0), true ); // Normal Working
                   }
		   if ( status[2] == 1 ) {
                       AlrmSt = true;
                       if (!glgName.contains("PUSH")) {
                           glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(1), true ); // Alarm Level 1
                           glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(1), true );
                       }
                       else 
                           glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(1), true ); // Alarm Level 1
                   }
                   if ( status[3] == 1 ) {
                       AlrmSt = true;
                       if (!glgName.contains("PUSH")) {
                           glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(2), true ); // Alarm Level 2
                           glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(2), true );
                       }
                       else  
                           glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(2), true ); // Alarm Level 2
                   }
                   if ( status[4] == 1 ) {
                       AlrmSt = true;
                       if (!glgName.contains("PUSH")) {
                           glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(3), true ); // Alarm Level 3
                           glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(3), true );
                       }
                       else 
                           glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(3), true ); // Alarm Level 3
                   }
                   if ( status[0] == 1 ) {
                       if (!glgName.contains("PUSH")) {
                           glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(5), true ); // Faulty
                           glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(5), true );
                       }
                       else 
                           glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(5), true ); // Faulty
                   }
                   if ( (status[11] == 1 || status[15] == 1  || status[10] == 1 || status[14] == 1) && AlrmSt == false){
                       if (!glgName.contains("PUSH")) {
                           glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(6), true ); // Alarm/Fault to be resetted
                           glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(6), true );
                       }
                       else 
                           glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(6), true ); // Alarm/Fault to be resetted
                  }
                  if ( status[8] == 1 ) {
                       if (!glgName.contains("PUSH")) {
                           glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(4), true ); // Unset
                           glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(4), true );
                       }
                        else 
                           glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(4), true ); // Unset
                  }  
              }
               if ( glgName.contains("Col") && 
                    glgName.contains("WE_") && 
		    !glgName.contains("SIREN") &&
	            !glgName.contains("VALVE")) { // West End building        
                     
                   int[] status = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};// There are 16 status bits
		   for (int k = 0 ; k < 16 ; k++) status[k] = ((int)(dataElement.value) >> k) & 1;
		   if ( status[2] == 1 || status[3] == 1 || status[4] == 1 || status[11] == 1 || status[15] == 1 ) { // Alarm (1,2,3 levels), Alarm to be resetted - Report
		          
		       if ( status[2] == 1 || status[3] == 1 || status[4] == 1) {
			   WEGeneralOKst = false;
			   WEGeneralStr = "WEST END BUILDING: DO NOT ENTER/NON ENTRARE;CALL/CONTATTARE VACUUM ONCALL/CONTROL ROOM";
                       }
		       if (glgName.contains("TOWER")) { // Cryotrap Zone
			   WECryoOKst = false;
			   WECryoStr = "WARNING Cryotrap Zone";
	               }
		       if (glgName.contains("TUNNEL")) { // Tunnel Zone
			   WETuOKst = false;
			   WETuStr = "WARNING Tunnel Zone";
		       }
		       if (glgName.contains("CLEAN")) { // Cleanroom Zone
			   WECleanOKst = false;
			   WECleanStr = "WARNING Cleanroom Zone";
		       }
		       if (glgName.contains("BASE")) { // Baseroom Zone
			   WEBaseOKst = false;
			   WEBaseStr = "WARNING Baseroom Zone";
                       }
			         }
		       if ( status[7] == 1 ) {
                           if (!glgName.contains("PUSH")) {
                               glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(0), true ); // Normal Working
                               glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(0), true );
                           }
                           else 
                               glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(0), true ); // Normal Working
                       }
		       if ( status[2] == 1 ) {
                           AlrmSt = true;
                           if (!glgName.contains("PUSH")) {
                               glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(1), true ); // Alarm Level 1
                               glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(1), true );
                           }
                           else 
                               glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(1), true ); // Alarm Level 1
                       }
                       if ( status[3] == 1 ) {
                           AlrmSt = true;
                           if (!glgName.contains("PUSH")) {
                               glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(2), true ); // Alarm Level 2
                               glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(2), true );
                           }
                           else 
                               glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(2), true ); // Alarm Level 2
                       }
                       if ( status[4] == 1 ) {
                           AlrmSt = true;
                           if (!glgName.contains("PUSH")) {
                               glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(3), true ); // Alarm Level 3
                               glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(3), true );
                           }
                           else 
                               glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(3), true ); // Alarm Level 3
                       }
                       if ( status[0] == 1 ){
                           if (!glgName.contains("PUSH")) {
                               glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(5), true ); // Faulty
                               glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(5), true );
                           }
                           else 
                               glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(5), true ); // Faulty
                       }
                       if ( (status[11] == 1 || status[15] == 1  || status[10] == 1 || status[14] == 1) && AlrmSt == false){
                           if (!glgName.contains("PUSH")) {
                               glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(6), true ); // Alarm/Fault to be resetted
                               glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(6), true );
                           }
                           else 
                              glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(6), true ); // Alarm/Fault to be resetted
                       }
                       if ( status[8] == 1 ){
                           if (!glgName.contains("PUSH")) {
                               glg_bean2.SetGTag( glgName, SensorO2ColorSTATUS.get(4), true ); // Unset
                               glg_bean2.SetGTag( glgName+"Val", SensorO2ColorSTATUS.get(4), true );
                           }
                           else 
                               glg_bean2.SetGTag( glgName, PushO2ColorSTATUS.get(4), true ); // Unset
                       }
                   }   
                   */
                   //
                   // SIREN FOR ALL BUILDINGS
                   //  
                   if (glgName.contains("Col") && 
                       (glgName.contains("SIREN") || glgName.contains("LN2VALVE"))) { // CB,NE,WE buildings siren/ln2valve       
                       
                       int[] status = {0,0,0,0,0,0,0};// There are 7 status bits
		       for (int k = 0 ; k < 7 ; k++) status[k] = ((int)(dataElement.value) >> k) & 1;
                           if ( status[0] == 0 && status[2] == 0 && status[1] != 1 ) 
                               glg_bean2.SetGTag( glgName, RelayO2ColorSTATUS.get(0), true ); // Normal Working
			   else if ( status[0] == 1 || status[5] == 1  ) 
                               glg_bean2.SetGTag( glgName, RelayO2ColorSTATUS.get(3), true ); // Alarm
                           else if ( status[3] == 1 ) 
                               glg_bean2.SetGTag( glgName, RelayO2ColorSTATUS.get(5), true ); // Faulty
                           else if ( status[6] == 1 || status[4] == 1 ) 
                               glg_bean2.SetGTag( glgName, RelayO2ColorSTATUS.get(6), true ); // Alarm/Fault to be resetted
                           else if ( status[2] == 1 ) 
                               glg_bean2.SetGTag( glgName, RelayO2ColorSTATUS.get(4), true ); // Unset
		   }
		   //
                   // Update Visibility 
                   //
                   if ( !glgName.contains("Val") &&
		        !glgName.contains("Col") && 
		        !glgName.equals("")) 
                       glg_bean2.SetDResource( glgName+"/Visibility", 1. ); 
                   //
                   // Update Val
                   //
                   if ( glgName.contains("Val"))
                       glg_bean2.SetDResource( glgName +"/Value", dataElement.value, true );
                   //
                   //  Update time
                   //
                   glg_bean2.SetSTag( "TimeStr",formatter.format(new Date()), true);  
                   
                   //
		   // Update strings
		   //
                   updateStrings();
                       
               }
               catch (Exception ex) {
                   ex.printStackTrace();
               }
          }
       } 
    }
    public void updateStrings() {
    //
    // Update strings
    //
    //System.out.println("UPDATE STRINGS");
    // GeneralText
    if (GeneralOKst == false) {
      glg_bean2.SetSTag( "GeneralStr", GeneralStr, true ); // General alarm
      glg_bean2.SetGTag( "GeneralCol", new GlgPoint(1.,0.,0.), true ); // General color Alarm
      glg_bean2.SetGTag( "CBZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // General Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "GeneralStr", "CENTRAL BUILDING O2 SENSOR MONITORING : OK" , true ); // General OK
      glg_bean2.SetGTag( "GeneralCol", new GlgPoint(0.,1.,1.), true ); // General color OK
      glg_bean2.SetGTag( "CBZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // General Frame color Alarm
    }
    /*
    if (NEGeneralOKst == false) {
      glg_bean2.SetSTag( "NEGeneralStr", NEGeneralStr, true ); // NE General alarm
      glg_bean2.SetGTag( "NEGeneralCol", new GlgPoint(1.,0.,0.), true ); // NE General color Alarm
      glg_bean2.SetGTag( "NEZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // NE Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "NEGeneralStr", "NORTH END BUILDING O2 SENSOR MONITORING : OK" , true ); // NE General OK
      glg_bean2.SetGTag( "NEGeneralCol", new GlgPoint(0.,1.,1.), true ); // NE General color OK
      glg_bean2.SetGTag( "NEZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // NE Frame color Alarm
    }
    if (WEGeneralOKst == false) {
      glg_bean2.SetSTag( "WEGeneralStr", WEGeneralStr, true ); // WE General alarm
      glg_bean2.SetGTag( "WEGeneralCol", new GlgPoint(1.,0.,0.), true ); // WE General color Alarm
      glg_bean2.SetGTag( "WEZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // WE Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "WEGeneralStr", "WEST END BUILDING O2 SENSOR MONITORING : OK" , true ); // WE General OK
      glg_bean2.SetGTag( "WEGeneralCol", new GlgPoint(0.,1.,1.), true ); // WE General color OK
      glg_bean2.SetGTag( "WEZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // WE Frame color Alarm
    }
    */
    // Status Zone in Central building
    if (TankOKst == false) {
      glg_bean2.SetSTag( "TankZoneStr", TankStr, true ); // Tank Alarm
      glg_bean2.SetGTag( "TankZoneCol", new GlgPoint(1.,0.,0.), true ); // Tank color Alarm
      glg_bean2.SetGTag( "TankZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // Tank Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "TankZoneStr", "Tank Zone OK", true ); // Tank OK
      glg_bean2.SetGTag( "TankZoneCol", new GlgPoint(0.,1.,1.), true ); // Tank color OK
      glg_bean2.SetGTag( "TankZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // Tank Frame color OK
    }
    if (WiOKst == false) {
      glg_bean2.SetSTag( "WiZoneStr", WiStr, true ); // Wi Alarm
      glg_bean2.SetGTag( "WiZoneCol", new GlgPoint(1.,0.,0.), true ); // Wi color Alarm
      glg_bean2.SetGTag( "WiZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // Wi Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "WiZoneStr", "West Input Zone OK", true ); // Wi OK
      glg_bean2.SetGTag( "WiZoneCol", new GlgPoint(0.,1.,1.), true ); // Wi color OK
      glg_bean2.SetGTag( "WiZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // Wi Frame color OK
    }
    if (NiOKst == false) {
      glg_bean2.SetSTag( "NiZoneStr", NiStr, true ); // Ni Alarm
      glg_bean2.SetGTag( "NiZoneCol", new GlgPoint(1.,0.,0.), true ); // Ni color Alarm
      glg_bean2.SetGTag( "NiZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // Ni Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "NiZoneStr", "North Input Zone OK", true ); // Ni OK
      glg_bean2.SetGTag( "NiZoneCol", new GlgPoint(0.,1.,1.), true ); // Ni color OK
      glg_bean2.SetGTag( "NiZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // Ni Frame color OK
    }
    if (DetOKst == false) {
      glg_bean2.SetSTag( "DetZoneStr", DetStr, true ); // Detection Alarm
      glg_bean2.SetGTag( "DetZoneCol", new GlgPoint(1.,0.,0.), true ); // Detection color Alarm
      glg_bean2.SetGTag( "DetZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // Detection Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "DetZoneStr", "Detection Zone OK", true ); // Detection OK
      glg_bean2.SetGTag( "DetZoneCol", new GlgPoint(0.,1.,1.), true ); // Detection color OK
      glg_bean2.SetGTag( "DetZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // Detection Frame color OK
    }
    if (InjOKst == false) {
      glg_bean2.SetSTag( "InjZoneStr", InjStr, true ); // Inj Alarm
      glg_bean2.SetGTag( "InjZoneCol", new GlgPoint(1.,0.,0.), true ); // Inj color Alarm
      glg_bean2.SetGTag( "InjZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // Inj Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "InjZoneStr", "Injection Zone OK", true ); // Inj OK
      glg_bean2.SetGTag( "InjZoneCol", new GlgPoint(0.,1.,1.), true ); // Inj color OK
      glg_bean2.SetGTag( "InjZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // Inj Frame color OK
    }
    if (CleanOKst == false) {
      glg_bean2.SetSTag( "CleanZoneStr", CleanStr, true ); // Clean Alarm
      glg_bean2.SetGTag( "CleanZoneCol", new GlgPoint(1.,0.,0.), true ); // Clean color Alarm
      glg_bean2.SetGTag( "CleanZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // Clean Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "CleanZoneStr", "Clean Room Zone OK", true ); // Clean OK
      glg_bean2.SetGTag( "CleanZoneCol", new GlgPoint(0.,1.,1.), true ); // Clean color OK
      glg_bean2.SetGTag( "CleanZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // Clean Frame color OK
    }
    // Status Zone in North End building
    /*
    if (NETuOKst == false) {
      glg_bean2.SetSTag( "NETunnelZoneStr", NETuStr, true ); // NE Tunnel Alarm
      glg_bean2.SetGTag( "NETunnelZoneCol", new GlgPoint(1.,0.,0.), true ); // NE Tunnel color Alarm
      glg_bean2.SetGTag( "NETunnelZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // NE Tunnel Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "NETunnelZoneStr", "Tunnel Zone OK", true ); // NE Tunnel OK
      glg_bean2.SetGTag( "NETunnelZoneCol", new GlgPoint(0.,1.,1.), true ); // NE Tunnel color OK
      glg_bean2.SetGTag( "NETunnelZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // NE Tunnel color OK
    }
    if (NECryoOKst == false) {
      glg_bean2.SetSTag( "NECryoZoneStr", NECryoStr, true ); // NE Cryo Alarm
      glg_bean2.SetGTag( "NECryoZoneCol", new GlgPoint(1.,0.,0.), true ); // NE Cryo color Alarm
      glg_bean2.SetGTag( "NECryoZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // NE Cryo Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "NECryoZoneStr", "Cryotrap Zone OK", true ); // NE Cryo OK
      glg_bean2.SetGTag( "NECryoZoneCol", new GlgPoint(0.,1.,1.), true ); // NE Cryo color OK
      glg_bean2.SetGTag( "NECryoZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // NE Cryo Frame color OK
    }
    if (NECleanOKst == false) {
      glg_bean2.SetSTag( "NECleanZoneStr", NECleanStr, true ); // NE Cleanroom Alarm
      glg_bean2.SetGTag( "NECleanZoneCol", new GlgPoint(1.,0.,0.), true ); // NE Cleanroom color Alarm
      glg_bean2.SetGTag( "NECleanZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // NE Cleanroom Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "NECleanZoneStr", "Cleanroom Zone OK", true ); // NE Cleanroom OK
      glg_bean2.SetGTag( "NECleanZoneCol", new GlgPoint(0.,1.,1.), true ); // NE Cleanroom color OK
      glg_bean2.SetGTag( "NECleanZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // NE Cleanroom Frame color OK
    }
    if (NEBaseOKst == false) {
      glg_bean2.SetSTag( "NEBaseZoneStr", NEBaseStr, true ); // NE Baseroom Alarm
      glg_bean2.SetGTag( "NEBaseZoneCol", new GlgPoint(1.,0.,0.), true ); // NE Baseroom color Alarm
      glg_bean2.SetGTag( "NEBaseZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // NE Baseroom Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "NEBaseZoneStr", "Baseroom Zone OK", true ); // NE Baseroom OK
      glg_bean2.SetGTag( "NEBaseZoneCol", new GlgPoint(0.,1.,1.), true ); // NE Baseroom color OK
      glg_bean2.SetGTag( "NEBaseZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // NE Baseroom Frame color OK
    }
    // Status Zone in West End building
    if (WETuOKst == false) {
      glg_bean2.SetSTag( "WETunnelZoneStr", WETuStr, true ); // WE Tunnel Alarm
      glg_bean2.SetGTag( "WETunnelZoneCol", new GlgPoint(1.,0.,0.), true ); // WE Tunnel color Alarm
      glg_bean2.SetGTag( "WETunnelZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // WE Tunnel Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "WETunnelZoneStr", "Tunnel Zone OK", true ); // WE Tunnel OK
      glg_bean2.SetGTag( "WETunnelZoneCol", new GlgPoint(0.,1.,1.), true ); // WE Tunnel color OK
      glg_bean2.SetGTag( "WETunnelZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // WE Tunnel color OK
    }
    if (WECryoOKst == false) {
      glg_bean2.SetSTag( "WECryoZoneStr", WECryoStr, true ); // WE Cryo Alarm
      glg_bean2.SetGTag( "WECryoZoneCol", new GlgPoint(1.,0.,0.), true ); // WE Cryo color Alarm
      glg_bean2.SetGTag( "WECryoZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // WE Cryo Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "WECryoZoneStr", "Cryotrap Zone OK", true ); // WE Cryo OK
      glg_bean2.SetGTag( "WECryoZoneCol", new GlgPoint(0.,1.,1.), true ); // WE Cryo color OK
      glg_bean2.SetGTag( "WECryoZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // WE Cryo Frame color OK
    }
    if (WECleanOKst == false) {
      glg_bean2.SetSTag( "WECleanZoneStr", WECleanStr, true ); // WE Cleanroom Alarm
      glg_bean2.SetGTag( "WECleanZoneCol", new GlgPoint(1.,0.,0.), true ); // WE Cleanroom color Alarm
      glg_bean2.SetGTag( "WECleanZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // WE Cleanroom Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "WECleanZoneStr", "Cleanroom Zone OK", true ); // WE Cleanroom OK
      glg_bean2.SetGTag( "WECleanZoneCol", new GlgPoint(0.,1.,1.), true ); // WE Cleanroom color OK
      glg_bean2.SetGTag( "WECleanZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // WE Cleanroom Frame color OK
    }
    if (WEBaseOKst == false) {
      glg_bean2.SetSTag( "WEBaseZoneStr", WEBaseStr, true ); // WE Baseroom Alarm
      glg_bean2.SetGTag( "WEBaseZoneCol", new GlgPoint(1.,0.,0.), true ); // WE Baseroom color Alarm
      glg_bean2.SetGTag( "WEBaseZoneFrameCol", new GlgPoint(1.,0.,0.), true ); // WE Baseroom Frame color Alarm
    }
    else {
      glg_bean2.SetSTag( "WEBaseZoneStr", "Baseroom Zone OK", true ); // WE Baseroom OK
      glg_bean2.SetGTag( "WEBaseZoneCol", new GlgPoint(0.,1.,1.), true ); // WE Baseroom color OK
      glg_bean2.SetGTag( "WEBaseZoneFrameCol", new GlgPoint(0.,1.,1.), true ); // WE Baseroom Frame color OK
    }
    */
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
                   
                      /*if ( System.currentTimeMillis() > pageTime + lastTime) {
                         lastTime = System.currentTimeMillis();
                         // Load web page
                         if (isVimLoaded == true) {
                            loadURL("https://dms.virgo-gw.eu/");
                            isVimLoaded = false;
                         }
                         else {
                            loadURL("https://vim.virgo-gw.eu/");
                            isVimLoaded = true;
                         }
                      }*/
                      
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
