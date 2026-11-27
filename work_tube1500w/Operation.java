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
    // The ramp is started and stopped from the panel (START/STOP buttons) or through the Modbus device OP
    // (OpDevice.java). Its parameters are kept here; the Sicem data elements TEMPSTEP, TEMPMAX, TIMEINTER
    // and TIME only show them (panel fields and Modbus registers).
    private volatile OpMode mode = OpMode.IDLE;
    private volatile double tempMax = 0;
    private volatile double tempStep = 0;
    private volatile double timeInter = 0;
    private double time = 0;
    private int cycleFreq; 
    private long startTime = 0;
    private long lastIncrementTime = 0;
    private double target = 0;          // last setpoint asked by the running ramp
    private volatile boolean running = false;

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

    /** true while a ramp is incrementing the setpoints */
    public boolean isRunning() {
      return running;
    }

    public double getTempMax() { return tempMax; }
    public double getTempStep() { return tempStep; }
    public double getTimeInter() { return timeInter; }
    
    public void setTempMax(double _tempMax) {
      tempMax = _tempMax;
      show("TEMPMAX", tempMax);
    }

    public void setTempStep(double _tempStep) {
      tempStep = _tempStep;
      show("TEMPSTEP", tempStep);
    }
    
    public void setTimeInter(double _timeInter) {
      timeInter = _timeInter;
      show("TIMEINTER", timeInter);
    }
    
    public void setTime(double _time) {
      time = _time;
      show("TIME", time / 1000);
    }

    private void show(String element, double value) {
      Device sicem = deviceManager.getDevice("SICEM");
      if (sicem == null) return;
      DataElement e = sicem.getDataElement(element);
      if (e != null) e.value = value;
    }

    // Heating channels of the Sicem: CH1..CH9 must always have the same setpoint
    private static final int HEATING_CHANNELS = 9;

    private boolean operationStopRamp() {
       logger.fine("Operation:operationStopRamp> Stop Ramp Operation - resetting all parameters to zero");
       
       Device sicem = deviceManager.getDevice("SICEM");
       // Reset all Operation values to zero
       for (int i = 1; i <= HEATING_CHANNELS; i++)
          setValue(sicem, sicem.getDataElement("SETP" + i), 1, 0);
       setTempStep(0);
       setTempMax(0);
       setTimeInter(0);
       setTime(0);
       endRamp();
     
       return true;
    }

    // Back to IDLE. The times are cleared so that the next START begins a new ramp: otherwise it
    // would take the old start time and increment at every cycle until it has "caught up".
    private void endRamp() {
       mode = OpMode.IDLE;
       running = false;
       startTime = 0;
       lastIncrementTime = 0;
    }

    // First cycle after START: the ramp begins only with usable parameters and a Sicem that answers
    // (with the communication lost its setpoints read 0, and the ramp would restart from 0).
    private boolean beginRamp() {
       Device sicem = deviceManager.getDevice("SICEM");
       DataElement setp1 = sicem.getDataElement("SETP1");
       DataElement com = sicem.getDataElement("COMST");
       String refusal = null;
       if (tempStep <= 0) refusal = "TEMP. STEP must be above 0";
       else if (timeInter <= 0) refusal = "TIME INTER. must be above 0";
       else if (com != null && com.value != 0) refusal = "no communication with the Sicem";
       else if (setp1.value >= tempMax) refusal = "the setpoint (" + setp1.value + ") is already at or above TEMP. MAX (" + tempMax + ")";
       if (refusal != null) {
          logger.log(Level.WARNING, "Operation:beginRamp> Ramp NOT started: " + refusal);
          endRamp();
          return false;
       }
       startTime = System.currentTimeMillis();
       lastIncrementTime = startTime;
       target = setp1.value;
       running = true;
       logger.fine("Operation:beginRamp> Ramp started from " + target + " with tempStep=" + tempStep +
                   ", tempMax=" + tempMax + ", timeInter=" + timeInter);
       return true;
    }
    
    private boolean operationStartRamp() {
       logger.finer("Operation:operationStartRamp> Start Ramp Operation using tempStep=" + 
       							String.valueOf(tempStep) + ", tempMax=" + String.valueOf(tempMax) + 
       							", and timeInter=" + String.valueOf(timeInter));
       
       Device sicem = deviceManager.getDevice("SICEM");

       // The ramp is finished once tempMax has been asked
       if ( target >= tempMax || tempStep <= 0 || timeInter <= 0 ) {
          logger.fine("Operation:operationStartRamp> Start Ramp Operation: Reached tempMax=" + String.valueOf(tempMax) + ", switching to IDLE");
          endRamp();
          return true;
       }
       
       // If interval time is passed, increment all setpoints temp
       long curTime = System.currentTimeMillis();
       logger.finer("Operation:operationStartRamp> Current time " + String.valueOf(curTime) + " compared to lastIncrementTime " + String.valueOf(lastIncrementTime));
       if ( curTime > (lastIncrementTime + (long) (timeInter*1000))) {
          // update lastIncrementTime
          lastIncrementTime += (long) (timeInter*1000);
          // The last step stops at tempMax, also when tempMax is not a whole number of steps away
          double next = Math.min(target + tempStep, tempMax);
          logger.fine("Operation:operationStartRamp> Start Ramp Operation: Incrementing temperature from " + String.valueOf(target) + " to " + String.valueOf(next));
          target = next;
          // All channels get the same setpoint
          for (int i = 1; i <= HEATING_CHANNELS; i++)
             setValue(sicem, sicem.getDataElement("SETP" + i), 1, next);
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
          if (!d.commandSetQueue.contains(e)) // not twice while it waits to be executed
             d.commandSetQueue.add(e);
       }
    }
    
   
    public void run () {
       boolean isOk = false;
       try {  
          while (true) {
             switch (mode) {
                case START: /* Set initial time and start ramp */ 
                            if ( startTime == 0 && !beginRamp() )
                                break;
                            setTime((double)(System.currentTimeMillis() - startTime)); 
                            isOk = operationStartRamp();   
                            break;
                case STOP: /* Stop ramp */ 
                           isOk = operationStopRamp(); 
                           break;
                case IDLE: /* Do nothing */ 
                           break;
             }
             // The fields of the panel always show the parameters kept here (the Sicem clears its
             // values when its communication fails)
             show("TEMPSTEP", tempStep); show("TEMPMAX", tempMax); show("TIMEINTER", timeInter); show("TIME", time / 1000);
             Thread.sleep(cycleFreq * 1000);
	  }
       }
       catch (InterruptedException ex) {
          logger.log(Level.SEVERE, "Operation:run:InterruptedException> " + ex.getMessage());
       }
       logger.finer("Operation:run> Stop thread Operation");
    }
}
