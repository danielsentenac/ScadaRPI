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
    private double tempMax = 0;
    private double tempStep = 0;
    private double timeInter = 0;
    private double time = 0;
    private int cycleFreq; 
    private long startTime = 0;
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
    
    public void setTempMax(int _tempMax) {
      tempMax = _tempMax;
      Device sicem = deviceManager.getDevice("SICEM");
      DataElement tempmax = sicem.getDataElement("TEMPMAX");
      tempmax.value = tempMax;
    }

    public void setTempStep(int _tempStep) {
      tempStep = _tempStep;
      Device sicem = deviceManager.getDevice("SICEM");
      DataElement tempstep = sicem.getDataElement("TEMPSTEP");
      tempstep.value = tempStep;
    }
    
    public void setTimeInter(int _timeInter) {
      timeInter = _timeInter;
      Device sicem = deviceManager.getDevice("SICEM");
      DataElement timeinter = sicem.getDataElement("TIMEINTER");
      timeinter.value = timeInter;
    }
    
    public void setTime(double _time) {
      time = _time;
      Device sicem = deviceManager.getDevice("SICEM");
      DataElement timev = sicem.getDataElement("TIME");
      timev.value = time / 1000;
    }

    private boolean operationStopRamp() {
       logger.fine("Operation:operationStopRamp> Stop Ramp Operation - resetting all parameters to zero");
       
       // Collect useful data
       Device sicem = deviceManager.getDevice("SICEM");
       DataElement setp1 = sicem.getDataElement("SETP1");
       DataElement setp2 = sicem.getDataElement("SETP2");
       DataElement setp3 = sicem.getDataElement("SETP3");
       DataElement setp4 = sicem.getDataElement("SETP4");
       DataElement setp5 = sicem.getDataElement("SETP5");
       DataElement setp6 = sicem.getDataElement("SETP6");
       DataElement setp7 = sicem.getDataElement("SETP7");
       DataElement sicemcomst = sicem.getDataElement("COMST");
       
       // Reset all Operation values to zero
       setValue(sicem, setp1, 1, 0);
       setValue(sicem, setp2, 1, 0);
       setValue(sicem, setp3, 1, 0);
       setValue(sicem, setp4, 1, 0);
       setValue(sicem, setp5, 1, 0);
       setValue(sicem, setp6, 1, 0);
       setValue(sicem, setp7, 1, 0);
       setTempStep(0);
       setTempMax(0);
       setTimeInter(0);
       setTime(0);
       mode = OpMode.IDLE;
       startTime = 0;
       lastIncrementTime = 0;
     
       return true;
    }
    
    private boolean operationStartRamp() {
       logger.finer("Operation:operationStartRamp> Start Ramp Operation using tempStep=" + 
       							String.valueOf(tempStep) + ", tempMax=" + String.valueOf(tempMax) + 
       							", and timeInter=" + String.valueOf(timeInter));
       
       // Collect useful data
       Device sicem = deviceManager.getDevice("SICEM");
       DataElement setp1 = sicem.getDataElement("SETP1");
       DataElement setp2 = sicem.getDataElement("SETP2");
       DataElement setp3 = sicem.getDataElement("SETP3");
       DataElement setp4 = sicem.getDataElement("SETP4");
       DataElement setp5 = sicem.getDataElement("SETP5");
       DataElement setp6 = sicem.getDataElement("SETP6");
       DataElement setp7 = sicem.getDataElement("SETP7");
       DataElement sicemcomst = sicem.getDataElement("COMST");

       // Check if tempMax is reached with first setpoint temp
       if ( setp1.value + tempStep > tempMax) {
          logger.fine("Operation:operationStartRamp> Start Ramp Operation: Reached tempMax=" + String.valueOf(tempMax) + ", switching to IDLE");
          // Ramp is finished so set to IDLE state
          mode = OpMode.IDLE;
          return true;
       }
       
       // If interval time is passed, increment all setpoints temp
       long curTime = System.currentTimeMillis();
       logger.finer("Operation:operationStartRamp> Current time " + String.valueOf(curTime) + " compared to lastIncrementTime " + String.valueOf(lastIncrementTime));
       if ( curTime > (lastIncrementTime + (long) (timeInter*1000))) {
          // update lastIncrementTime
          lastIncrementTime += (long) (timeInter*1000);
          logger.fine("Operation:operationStartRamp> Start Ramp Operation: Incrementing temperature from " + String.valueOf(setp1.value) + " to " + String.valueOf(setp1.value + tempStep));
          setValue(sicem, setp1, 1, setp1.value + tempStep);
          setValue(sicem, setp2, 1, setp2.value + tempStep);
          setValue(sicem, setp3, 1, setp3.value + tempStep);
          setValue(sicem, setp4, 1, setp4.value + tempStep);
          setValue(sicem, setp5, 1, setp5.value + tempStep);
          setValue(sicem, setp6, 1, setp6.value + tempStep);
          setValue(sicem, setp7, 1, setp7.value + tempStep);
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
          while (true) {
             switch (mode) {
                case START: /* Set initial time and start ramp */ 
                            if ( startTime == 0 ) {
                                startTime = System.currentTimeMillis();
                                lastIncrementTime = startTime;
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
