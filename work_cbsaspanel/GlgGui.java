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
import java.lang.reflect.Method;

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
import java.awt.Toolkit;
import java.awt.GraphicsEnvironment;

import java.lang.Runtime;
import java.lang.System;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.SwingUtilities;

public class GlgGui extends JFrame implements ChannelList, Runnable  {

    private static final long serialVersionUID = 354054054055L;
    private DeviceManager deviceManager;
    public GlgGui parent;
    private String title;
    private Thread thread;
    private static final Logger logger = Logger.getLogger("Main");
    private Container content;
    private JPanel panel;
    private JComponent jfxPanel1;
    private JComponent jfxPanel2;
    private JComponent jfxPanel3;
    private Object engine1;
    private Object engine2;
    private JPanel panelweb = new JPanel();
    private JPanel panelvac = new JPanel();
    private GlgJLWBean glg_bean1;
    private GlgJLWBean glg_bean2;
    public Hashtable<String, GlgChildGui> subwindows;
    public boolean isSuspended = false;
    private long pageTime = 100000;
    private long lastTime = 0;
    private boolean isVimLoaded = true;
    private boolean javaFxEnabled = detectJavaFxAvailability();
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

    private boolean detectJavaFxAvailability() {
       try {
          Class.forName("javafx.application.Platform");
          Class.forName("javafx.embed.swing.JFXPanel");
          Class.forName("javafx.scene.Scene");
          Class.forName("javafx.scene.web.WebView");
          return true;
       }
       catch (ClassNotFoundException ex) {
          logger.warning("GlgGui: JavaFX runtime unavailable, embedded views disabled.");
          return false;
       }
    }

    private JComponent createEmbeddedPanel(String fallbackMessage) {
       if (!javaFxEnabled) {
          return createFallbackPanel(fallbackMessage);
       }
       try {
          return (JComponent) Class.forName("javafx.embed.swing.JFXPanel")
                  .getDeclaredConstructor().newInstance();
       }
       catch (ReflectiveOperationException | LinkageError ex) {
          javaFxEnabled = false;
          logger.log(Level.WARNING, "GlgGui:createEmbeddedPanel> Falling back to Swing panel", ex);
          return createFallbackPanel(fallbackMessage);
       }
    }

    private JComponent createFallbackPanel(String message) {
       JPanel placeholder = new JPanel(new GridBagLayout());
       placeholder.setBackground(Color.black);
       JLabel label = new JLabel(message);
       label.setForeground(Color.lightGray);
       placeholder.add(label);
       return placeholder;
    }

    private void runOnFxThread(Runnable task) {
       if (!javaFxEnabled) {
          return;
       }
       try {
          Class<?> platformClass = Class.forName("javafx.application.Platform");
          Method runLater = platformClass.getMethod("runLater", Runnable.class);
          runLater.invoke(null, task);
       }
       catch (ReflectiveOperationException | LinkageError ex) {
          javaFxEnabled = false;
          logger.log(Level.WARNING, "GlgGui:runOnFxThread> Disabling JavaFX integration", ex);
       }
    }

    private Object createFxScene(Object root) throws ReflectiveOperationException {
       Class<?> parentClass = Class.forName("javafx.scene.Parent");
       Class<?> sceneClass = Class.forName("javafx.scene.Scene");
       return sceneClass.getConstructor(parentClass).newInstance(root);
    }

    private void setFxScene(JComponent panel, Object scene) throws ReflectiveOperationException {
       if (!javaFxEnabled || panel == null) {
          return;
       }
       Class<?> sceneClass = Class.forName("javafx.scene.Scene");
       panel.getClass().getMethod("setScene", sceneClass).invoke(panel, scene);
    }

    private Object createWebEngine(JComponent panel) throws ReflectiveOperationException {
       Object view = Class.forName("javafx.scene.web.WebView").getDeclaredConstructor().newInstance();
       Method setZoom = view.getClass().getMethod("setZoom", double.class);
       setZoom.invoke(view, 0.10d);
       Method setFontScale = view.getClass().getMethod("setFontScale", double.class);
       setFontScale.invoke(view, 7.0d);
       Object engine = view.getClass().getMethod("getEngine").invoke(view);
       setFxScene(panel, createFxScene(view));
       return engine;
    }

    private void loadUrl(final Object engine, final String url) {
       if (!javaFxEnabled || engine == null) {
          return;
       }
       runOnFxThread(new Runnable() {
          @Override
          public void run() {
             try {
                engine.getClass().getMethod("load", String.class).invoke(engine, url);
             }
             catch (ReflectiveOperationException | LinkageError ex) {
                logger.log(Level.WARNING, "GlgGui:loadUrl> Unable to load " + url, ex);
             }
          }
       });
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
          this.setUndecorated(true);
          // Belt-and-suspenders fullscreen: explicitly size to screen in case
          // the window manager (e.g. Wayfire/labwc on RPi) ignores setExtendedState.
          Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
          this.setSize(screenSize);
          this.setLocation(0, 0);
          this.setExtendedState(JFrame.MAXIMIZED_BOTH);
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
          panel.add(glg_bean1, new GridBagConstraints(0, 1, 1, 1, 1.1, 1.0
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));
          panel.add(glg_bean2, new GridBagConstraints(1, 1, 1, 1, 0.4, 0.5
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));
	  panel.add(jfxPanel3, new GridBagConstraints(2, 1, 1, 1, 0.55, 0.5
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));
          panel.add(panelweb, new GridBagConstraints(0, 0, 3, 1, 0.95, 0.95
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));	
	  			     
          
          glg_bean1.SetSTag("title", "PARTICLE MONITORING CEB", true);
          glg_bean2.SetSTag("title", "PARTICLE MONITORING MOBILE", true);
          
          glg_bean1.SetSTag("pcounter1", "Injection L.", true);
          glg_bean1.SetSTag("pcounter2", "Baseroom C.", true);
          glg_bean1.SetSTag("pcounter3", "Detect. Sas", true);
          glg_bean1.SetSTag("pcounter4", "Main Hall", true);
          glg_bean1.SetSTag("pcounter5", "Detection L.", true);
          glg_bean1.SetSTag("pcounter6", "Sas Clean R.", true);
          glg_bean1.SetSTag("pcounter7", "Payload C.", true);
          glg_bean1.SetSTag("pcounter8", "Mirror C.", true);
          
          glg_bean2.SetSTag("pcountermob1", "Mobile 1", true);
          glg_bean2.SetSTag("pcountermob2", "Mobile 2", true);
          glg_bean2.SetSTag("pcountermob3", "Mobile 3", true);
          
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
        jfxPanel1 = createEmbeddedPanel("DMS view unavailable (JavaFX missing)");
        jfxPanel2 = createEmbeddedPanel("VIM view unavailable (JavaFX missing)");
        createWebScene();
        panelweb.add(jfxPanel1, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));	
        panelweb.add(jfxPanel2, new GridBagConstraints(1, 0, 1, 1, 0.7, 0.7
						     , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						     new Insets(1, 1, 1, 1), 0, 0));	
    }
    
    private void initVacComponents() {
        jfxPanel3 = createEmbeddedPanel("Vacuum supervisor unavailable (JavaFX missing)");
        createVacScene();
      
    }
    private void createVacScene() {
        if (!javaFxEnabled) {
           return;
        }
        runOnFxThread(new Runnable() {
           @Override
           public void run() {
              try {
                 Object global = Class.forName("com.gluonapplication.ViewGlobal")
                         .getDeclaredConstructor(String.class, String.class)
                         .newInstance("GLOBAL", "GLOBAL");
                 if (global instanceof Runnable) {
                    new Thread((Runnable) global).start();
                 }
                 setFxScene(jfxPanel3, createFxScene(global));
              }
              catch (ReflectiveOperationException | LinkageError ex) {
                 logger.log(Level.WARNING, "GlgGui:createVacScene> Unable to start vacuum supervisor view", ex);
              }
           }
        });
    }
    private void createWebScene() {
        if (!javaFxEnabled) {
           return;
        }
        runOnFxThread(new Runnable() {
           @Override
           public void run() {
              try {
                 engine1 = createWebEngine(jfxPanel1);
                 engine2 = createWebEngine(jfxPanel2);
              }
              catch (ReflectiveOperationException | LinkageError ex) {
                 engine1 = null;
                 engine2 = null;
                 logger.log(Level.WARNING, "GlgGui:createWebScene> Unable to start embedded web views", ex);
              }
           }
        });
    }
    public void loadURL1() {
        loadUrl(engine1, "https://dms.virgo-gw.eu/");
    }
    
    public void loadURL2() {
        loadUrl(engine2, "https://vim.virgo-gw.eu/");
    }

    public void exitProgram() {
       System.exit(0);
    }
    public void updateGui() {
    
    
       //
       //  Update time
       //
	glg_bean2.SetSTag( "TimeStr",formatter.format(new Date()), true);      
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
       // Update PCOUNTER data
       for (Map.Entry<String, String> e : PCOUNTERSTATUS.entrySet()) {
         String glgName =  e.getKey();
         String dataName = e.getValue();
         if ( dataName == null ) continue;
         // Get corresponding device from deviceManager
         String deviceName = dataName.split("_",2)[0];
         Device device = deviceManager.getDevice(deviceName);
         // Get corresponding pcounter PCX name
         String pcounterName = dataName.split("_")[3];
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
               glgNameTmp = glgNameTmp.replace(pcounterName,""); // Remove device name in glgNameTmp String
               if (!glgName.contains("sub")) {
                  if ( glgName.contains("Status") && !glgName.contains("Instr") && glgName.contains("Col") )// Col is a tag for object status (short type) color property
                     glg_bean1.SetGTag(glgName, PCounterColorSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Status") && !glgName.contains("Instr") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean1.SetSTag(glgName, "Count: " + PCounterSTATUS.get((int)dataElement.value), true);     
                  if ( glgName.contains("InstrStatus") && glgName.contains("Col") )// Col is a tag for object status (short type) color property
                     glg_bean1.SetGTag(glgName, PCounterColorINSTRSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("InstrStatus") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean1.SetSTag(glgName, "Status: " + PCounterINSTRSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Sampling") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean1.SetSTag(glgName, glgNameTmp.replace("Sampling","Sample") + " : " + (int) dataElement.value + "s", true);
                  if ( glgName.contains("Holding") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean1.SetSTag(glgName, glgNameTmp.replace("Holding","Hold") + ":" + (int) dataElement.value + "s", true);
                  if ( glgName.contains("Cycle") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean1.SetSTag(glgName, glgNameTmp + ":" + (int) dataElement.value, true);
                  if ( glgName.contains("Flow") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean1.SetSTag(glgName, glgNameTmp + ":" + (int) dataElement.value + "mLPM", true);
               }
               if ( glgName.contains("Val") && // Val is a tag for object value (double type) property
                   !glgName.contains("sub") && // Val is a tag for object value (double type) property
                   !glgName.contains("Time")) {  // sub is reserved for childGui only
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
       // Update PCOUNTERMOB data
       
       for (Map.Entry<String, String> e : PCOUNTERMOBSTATUS.entrySet()) {
         String glgName =  e.getKey();
         String dataName = e.getValue();
         if ( dataName == null ) continue;
         // Get corresponding device from deviceManager
         String deviceName = dataName.split("_",2)[0];
         Device device = deviceManager.getDevice(deviceName);
         // Get corresponding pcounter MOBX name
         String pcounterName = glgName.substring(0,4);
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
               glgNameTmp = glgNameTmp.replace(pcounterName,""); // Remove device name in glgNameTmp String
               if (!glgName.contains("sub")) {
                  if ( glgName.contains("Status") && !glgName.contains("Instr") && glgName.contains("Col") )// Col is a tag for object status (short type) color property
                     glg_bean2.SetGTag(glgName, PCounterColorSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Status") && !glgName.contains("Instr") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean2.SetSTag(glgName, "Count: " + PCounterSTATUS.get((int)dataElement.value), true);     
                  if ( glgName.contains("InstrStatus") && glgName.contains("Col") )// Col is a tag for object status (short type) color property
                     glg_bean2.SetGTag(glgName, PCounterColorINSTRSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("InstrStatus") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean2.SetSTag(glgName, "Status: " + PCounterINSTRSTATUS.get((int)dataElement.value), true);
                  if ( glgName.contains("Sampling") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean2.SetSTag(glgName, glgNameTmp.replace("Sampling","Sample") + ":" + (int) dataElement.value + "s", true);
                  if ( glgName.contains("Holding") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean2.SetSTag(glgName, glgNameTmp.replace("Holding","Hold") + ":" + (int) dataElement.value + "s", true);
                  if ( glgName.contains("Cycle") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean2.SetSTag(glgName, glgNameTmp + " : " + (int) dataElement.value, true);
                  if ( glgName.contains("Flow") && glgName.contains("Str") )  // Str is a tag for object status (short type) string property
                     glg_bean2.SetSTag(glgName, glgNameTmp + ":" + (int) dataElement.value + "mLPM", true);
               }
               if ( glgName.contains("Val") && // Val is a tag for object value (double type) property
                   !glgName.contains("sub") && // Val is a tag for object value (double type) property
                   !glgName.contains("Time")) {  // sub is reserved for childGui only
                  glg_bean2.SetDTag(glgName, dataElement.value, true);
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
