/*
 * This Class is used for GUI using Glg Toolkit
 */

import com.genlogic.*;
import java.util.*;

import java.awt.event.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.GridLayout;

public abstract class GlgChildGui extends JFrame implements ChannelList, Runnable {

    private static final long serialVersionUID = 354054054054L;
    protected DeviceManager deviceManager;
    protected String origin;
    protected String title;
    public GlgGui parent;
    public GlgChildGui child;
    private Thread thread;
    private static final Logger logger = Logger.getLogger("Main");
    protected Container content;
    protected GlgJLWBean glg_bean;
    protected String glgdrawing;
    protected Hashtable < String, String > STATUS = new Hashtable < String, String > ();
    protected Hashtable < String, String > CMD = new Hashtable < String, String > ();
    public boolean isSuspended = false;
    protected boolean showParent = false;
    private int count = 0;

    public GlgChildGui (GlgGui _parent, DeviceManager _deviceManager, String _title, String _origin) {

       deviceManager = _deviceManager;
       origin = _origin;
       title = _title;
       parent = _parent;
       child = this;
       this.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
             logger.finer("GlgChildGui:GlgChildGui> Closing Window " + origin);
             GlgChildGui child = parent.subwindows.get(origin);
             if ( child != null) {
                parent.subwindows.remove(origin);
             }
          }
       });
       content = getContentPane();
       content.setLayout(new GridLayout(1,1));
       // Creating the display
       this.setExtendedState(JFrame.MAXIMIZED_BOTH);
       this.setUndecorated(true);
       this.setVisible(true);
       createAndShowGui();
      
       // Start thread
       this.doStart();
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
    
    protected abstract void createAndShowGui ();

    protected void updateGui() {

       // Loop over STATUS elements
      for (Map.Entry<String, String> e : STATUS.entrySet()) {
         String glgName =  e.getKey();
         String dataName = e.getValue();
         logger.finer("GlgChildGui:updateGui> glgName=" + glgName + " dataName=" + dataName);
         if ( dataName == null ) continue;
         // Get corresponding device from deviceManager
         String deviceName = dataName.split("_",2)[0];
         //logger.finer("GlgChildGui:updateGui> glgName=" + glgName + " deviceName=" + deviceName);
         Device device = deviceManager.getDevice(deviceName);
         if (device == null) {
            //logger.finer("GlgChildGui:updateGui> glgName=" + glgName + " deviceName=" + deviceName);
            // Set visibility property for all resources
            if ( !glgName.contains("Col") && // Col is a tag for color property
                 !glgName.contains("Str") && // Str is a tag for string property
                 !glgName.contains("DataSample") ) // Datasample is a tag for plot property
               glg_bean.SetDResource(glgName +"/Visibility", 0.1 );
         }
         // Get corresponding data element from device
         else {
            DataElement dataElement = device.getDataElement(dataName.split("_",2)[1]);
            logger.finer("GlgChildGui:updateGui> glgName=" + glgName + " dataElement name=" + 
                          dataElement.name + " type=" + dataElement.type);
            try {
               // Set visibility property for all resources
               if ( !glgName.contains("Col") && // Col is a tag for color property
                    !glgName.contains("Str") && // Str is a tag for string property
                    !glgName.contains("DataSample") ) // Datasample is a tag for plot property
                  glg_bean.SetDResource(glgName +"/Visibility", 1. );

               // Update origin dependent Guifeatures 
               updateGuiFeatures(glgName, dataElement);
            }
            catch (Exception ex) {
               // Set visibility property for all resources
               if ( !glgName.contains("Col") && // Col is a tag for color property
                    !glgName.contains("Str") && // Str is a tag for string property
                    !glgName.contains("DataSample") ) // Datasample is a tag for plot property
                  glg_bean.SetDResource(glgName +"/Visibility", 0.1 );
              //logger.log(Level.SEVERE, "GlgChildGui:updateGui:Exception> " + ex.getMessage());
            }
         }
      } 
    }

    protected void createCommandMap(Hashtable < String, String > CMD) {
       
       CMD.clear();
       CMD.putAll(HIDENBENCHCMD);
    }

    protected abstract void createStatusMap(Hashtable < String, String > STATUS);

    protected abstract void updateGuiFeatures(String glgName, DataElement dataElement);

    public void run () {

       try {
          while (true) {
             // Update Glg Gui tags
             //logger.finer("GlgChildGui:run>" + origin);
             if ( isSuspended == false ) {
                updateGui();	
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                   public void run() {
                   // Update Bean
                   glg_bean.validate();
                   glg_bean.repaint();
                   }
                });
             }
             Thread.sleep(1000);
             if (showParent == true) {
                count++;
                if ( count >= 6 ) {
                   count = 0;
                   showParent = false;
                   parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
                   parent.toFront();
                   parent.isSuspended = false;
                   this.isSuspended = true;
                }
             }
	  }
       }
       catch (InterruptedException ex) {
          logger.log(Level.SEVERE, "GlgChildGui:run:InterruptedException> " + ex.getMessage());
       }
       catch (NullPointerException ex) {
          logger.log(Level.SEVERE, "GlgChildGui:run:NullPointerException> " + ex.getMessage());
       }
       catch (Exception ex) {
          logger.log(Level.SEVERE, "GlgChildGui:run:Exception> " + ex.getMessage());
       }    
       logger.finer("GlgChildGui:run> Exiting " + origin);
    }
}
