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
    private Device device;
    private OpMode mode = OpMode.IDLE;
    private double flowMax = 0;
    private double flowStep = 0;
    private double timeInter = 0;
    private double time = 0;
    private int cycleFreq; 
    private long startTime = 0;
    private long lastIncrementTime = 0;

    public Operation (Device _device, int _cycleFreq) {

       device = _device;
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
    
    public OpMode getMode() {
      return mode;
    }
    
    public void setDevice(String devicename) {
        device = deviceManager.getDevice(devicename);
    }
    
    public String  getDeviceName() {
    	return device.name;
    }
    
    public void setFlowMax(int _flowMax) {
      flowMax = _flowMax;
      DataElement flowmax = device.getDataElement("OPFLOWMAX");
      System.out.println("Operation:setFlowMax> Set FlowMax = " + flowMax);
      flowmax.value = flowMax;
    }

    public void setFlowStep(int _flowStep) {
      flowStep = _flowStep;
      DataElement flowstep = device.getDataElement("OPFLOWSTEP");
      flowstep.value = flowStep;
    }
    
    public void setTimeInter(int _timeInter) {
      timeInter = _timeInter;
      DataElement timeinter = device.getDataElement("OPTIMEINTER");
      timeinter.value = timeInter;
    }
    
    public void setTime(double _time) {
      time = _time;
      DataElement timev = device.getDataElement("OPTIME");
      timev.value = time / 1000;
    }

    private boolean operationStopRamp() {
       logger.fine("Operation:operationStopRamp> Stop Ramp Operation - resetting all parameters to zero");
       
       // Collect useful data
       DataElement setp = device.getDataElement("FLOW_SETP");
       DataElement devicecomst = device.getDataElement("COMST");
       
       // Reset all Operation values to zero
       setValue(device, setp, 1, 0);
       
       setFlowStep(0);
       setFlowMax(0);
       setTimeInter(0);
       setTime(0);
       mode = OpMode.IDLE;
       startTime = 0;
       lastIncrementTime = 0;
     
       return true;
    }
    
    private boolean operationStartRamp() {
       System.out.println("Operation:operationStartRamp> Start Ramp Operation using flowStep=" + 
       							String.valueOf(flowStep) + ", flowMax=" + String.valueOf(flowMax) + 
       							", and timeInter=" + String.valueOf(timeInter));
       
       // Collect useful data
       DataElement setp = device.getDataElement("FLOW_SETP");
       DataElement devicecomst = device.getDataElement("COMST");

       // Check if flowMax is reached with first setpoint temp
       if ( setp.value + flowStep > flowMax) {
          System.out.println("Operation:operationStartRamp> Start Ramp Operation: Reached flowMax=" + String.valueOf(flowMax) + ", switching to IDLE");
          // Ramp is finished so set to IDLE state
          mode = OpMode.IDLE;
          return true;
       }
       
       // If interval time is passed, increment all setpoints temp
       long curTime = System.currentTimeMillis();
       System.out.println("Operation:operationStartRamp> Current time " + String.valueOf(curTime) + " compared to lastIncrementTime " + String.valueOf(lastIncrementTime));
       if ( curTime > (lastIncrementTime + (long) (timeInter*1000))) {
          // update lastIncrementTime
          lastIncrementTime += (long) (timeInter*1000);
          logger.fine("Operation:operationStartRamp> Start Ramp Operation: Incrementing FLOW from " + String.valueOf(setp.value) + " to " + String.valueOf(setp.value + flowStep));
          setValue(device, setp, 1, setp.value + flowStep);
          
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
