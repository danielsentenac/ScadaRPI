/*
 * This Class is used for Operation (mode algorithms)
 */
import com.genlogic.*;
import java.util.*;
import java.lang.System;
import java.text.DecimalFormat;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JFrame;
import java.awt.event.*;
import java.awt.Frame;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.GridLayout;

import java.lang.Math;

public class Operation implements DataTypes, Runnable  {

    private static final long serialVersionUID = 354054054055L;
    private Thread thread;
    private static final Logger logger = Logger.getLogger("Main");
    public DeviceManager deviceManager;
    private OpMode mode = OpMode.IDLE;
    private double time = 0;
    private double acqTime = 0;
    private double timeInter = 0;
    private int cycleFreq; 
    private long startTime = 0;
    private long startAcqTime = -1;
    private long lastIncrementTime = 0;
   
    

    public Operation (DeviceManager _deviceManager, int _cycleFreq) {

       deviceManager = _deviceManager;
       cycleFreq = _cycleFreq;
      
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
   
   public void setMode(OpMode _mode) {
      mode = _mode;
    }
    
    public void setAcqTime(int _acqTime) {
      System.out.println("Operation::setAcqTime> Set AcqTime = " + _acqTime);
      acqTime = (double) _acqTime;
      Device m3 = deviceManager.getDevice("M3");
      DataElement acqtime = m3.getDataElement("ACQTIME");
      acqtime.value = acqTime;
    }
    
    public void setTimeInter(int _timeInter) {
      logger.finer("Operation::setTimeInter> Set TimeInter = " + _timeInter);
      timeInter = (double) _timeInter;
      Device m3 = deviceManager.getDevice("M3");
      DataElement timeinter = m3.getDataElement("TIMEINTER");
      timeinter.value = timeInter;
    }
    
    public void setTime(double _time) {
      time = _time;
      Device m3 = deviceManager.getDevice("M3");
      DataElement timev = m3.getDataElement("TIME");
      timev.value = time / 1000;
    }
    
    double getTime() {
       return time;
    }
    
    double getAcqTime() {
       return acqTime;
    }
    
    double getTimeInter() {
       return timeInter;
    }

    
    private boolean stopAcquisition() {
    
       logger.fine("Operation:stopAcquisition> Stop Acquisition");
       
       // Collect useful data
       Device m3 = deviceManager.getDevice("M3");
       Device pc1 = deviceManager.getDevice("PC1");
       Device pc2 = deviceManager.getDevice("PC2");
       Device pc3 = deviceManager.getDevice("PC3");
       Device pc4 = deviceManager.getDevice("PC4");
       Device pc5 = deviceManager.getDevice("PC5");
       Device pc6 = deviceManager.getDevice("PC6");
       Device pc7 = deviceManager.getDevice("PC7");
       Device pc8 = deviceManager.getDevice("PC8");
       
       DataElement pumponoff = m3.getDataElement("PUMPONOFF");
       DataElement startstop1 = pc1.getDataElement("STARTSTOP");
       DataElement startstop2 = pc2.getDataElement("STARTSTOP");
       DataElement startstop3 = pc3.getDataElement("STARTSTOP");
       DataElement startstop4 = pc4.getDataElement("STARTSTOP");
       DataElement startstop5 = pc5.getDataElement("STARTSTOP");
       DataElement startstop6 = pc6.getDataElement("STARTSTOP");
       DataElement startstop7 = pc7.getDataElement("STARTSTOP");
       DataElement startstop8 = pc8.getDataElement("STARTSTOP");
       
       // Reset all Operation values to zero
       setValue(pc1, startstop1, 1, 2);
       setValue(pc2, startstop2, 1, 2);
       setValue(pc3, startstop3, 1, 2);
       setValue(pc4, startstop4, 1, 2);
       setValue(pc5, startstop5, 1, 2);
       setValue(pc6, startstop6, 1, 2);
       setValue(pc7, startstop7, 1, 2);
       setValue(pc8, startstop8, 1, 2);
       setValue(m3, pumponoff, 1, 2);
       
       startAcqTime = -1;
       lastIncrementTime = System.currentTimeMillis() - (long)acqTime;
       
       return true;
       
    }
    
    private boolean startAcquisition() {
    
       logger.finer("Operation:startAcquisition> Start Acquisition");
       
       // Collect useful data
       Device m3 = deviceManager.getDevice("M3");
       Device pc1 = deviceManager.getDevice("PC1");
       Device pc2 = deviceManager.getDevice("PC2");
       Device pc3 = deviceManager.getDevice("PC3");
       Device pc4 = deviceManager.getDevice("PC4");
       Device pc5 = deviceManager.getDevice("PC5");
       Device pc6 = deviceManager.getDevice("PC6");
       Device pc7 = deviceManager.getDevice("PC7");
       Device pc8 = deviceManager.getDevice("PC8");
       
       DataElement pumponoff = m3.getDataElement("PUMPONOFF");
       DataElement startstop1 = pc1.getDataElement("STARTSTOP");
       DataElement startstop2 = pc2.getDataElement("STARTSTOP");
       DataElement startstop3 = pc3.getDataElement("STARTSTOP");
       DataElement startstop4 = pc4.getDataElement("STARTSTOP");
       DataElement startstop5 = pc5.getDataElement("STARTSTOP");
       DataElement startstop6 = pc6.getDataElement("STARTSTOP");
       DataElement startstop7 = pc7.getDataElement("STARTSTOP");
       DataElement startstop8 = pc8.getDataElement("STARTSTOP");
       
       // Start All devices
       setValue(m3, pumponoff, 1, 1);
       setValue(pc1, startstop1, 1, 1);
       setValue(pc2, startstop2, 1, 1);
       setValue(pc3, startstop3, 1, 1);
       setValue(pc4, startstop4, 1, 1);
       setValue(pc5, startstop5, 1, 1);
       setValue(pc6, startstop6, 1, 1);
       setValue(pc7, startstop7, 1, 1);
       setValue(pc8, startstop8, 1, 1);
       
       
       startAcqTime = System.currentTimeMillis();
       lastIncrementTime = -1;
       
       return true;
    }
    
    private boolean operationStopRamp() {
       logger.finer("Operation:operationStopRamp> Stop Acquisition using acqTime=" + String.valueOf(acqTime) + 
       							", and timeInter=" + String.valueOf(timeInter));
       // Stop Acquisition
       stopAcquisition();
       setAcqTime(0);
       setTimeInter(0);
       setTime(0);
       // Resetting all parameters and switch to IDLE state
       mode = OpMode.IDLE;
       startTime = 0;
       startAcqTime = -1;
       lastIncrementTime = -1;
       time = 0;
     
       return true;
    }
    
    private boolean operationStartRamp() {
       logger.finer("Operation:operationStartRamp> Start Ramp Operation using acqTime=" + String.valueOf(acqTime) + 
       							", and timeInter=" + String.valueOf(timeInter));
       // Check if acqTime is reached
       long curTimeAcq = System.currentTimeMillis();
       if ( (curTimeAcq > startAcqTime + (long)(acqTime*1000)) && (startAcqTime >= 0) ) {
          logger.fine("Operation:operationStartRamp>  Stop Acquisition at time=" + String.valueOf(curTimeAcq) + "  (startAcqTime=" + String.valueOf(startAcqTime) + ")");
          // Stop Acquisition
          stopAcquisition();
          return true;
       }
       
       // If interval time is passed, start acquisition again
       long curTime = System.currentTimeMillis();
       logger.finer("Operation:operationStartRamp> Current time " + String.valueOf(curTime) + " compared to lastIncrementTime " + String.valueOf(lastIncrementTime));
       if ( curTime > (lastIncrementTime + (long) (timeInter*1000)) && ( lastIncrementTime >= 0) ) {
          logger.fine("Operation:operationStartRamp> Start Acquisition at time=" + String.valueOf(curTime) + "  (lastIncrementTime=" + String.valueOf(lastIncrementTime) + ")");
          // Start acquisition
          startAcquisition();
       }
       return true;
    }

    private void setValue(Device d, DataElement e, int type, double value) {
       logger.fine("Operation::setValue> Execute command " + d.name + " " + e.name + " " + value); 
       if ( e.value != value ) {
          switch (type) {
             case (0) : e.value = value;
                break;
             case (1) : e.setvalue = value;
                break;
          }
          d.commandSetQueue.add(e);
       }
    }
    
   
    public void run () {
       boolean isOk = false;
       try {  
          Device m3 = deviceManager.getDevice("M3");
          DataElement rearmst = m3.getDataElement("REARMST");
          while (true) {             
             switch (mode) {
                case START: /* Set initial time and start ramp */ 
                            if ( startTime == 0 ) {
                                logger.fine("Operation::run> START Ramp");
                                startTime = System.currentTimeMillis();
                                lastIncrementTime = 0;
                            }
                            setTime((double)(System.currentTimeMillis() - startTime)); 
                            isOk = operationStartRamp();   
                            break;
                case STOP: /* Stop ramp */
                           isOk = operationStopRamp(); 
                           break;
                case IDLE: /* Do nothing */ 
                           break;
             }
             Thread.sleep(cycleFreq * 1000);
	  }
       }
       catch (InterruptedException ex) {
          logger.log(Level.SEVERE, "Operation:run:InterruptedException> " + ex.getMessage());
       }
       logger.finer("Operation:run> Stop thread Operation");
    }
}
