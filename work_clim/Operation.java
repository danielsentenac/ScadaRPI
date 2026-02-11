/*
 * This Class is used for Operation (mode algorithms)
 */
import com.genlogic.*;
import java.util.*;
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
    private OpMode mode = OpMode.AUTO;
    private int cycleFreq;
    public DeviceManager deviceManager;
    private int humDelta = 10;
    private int tempDelta = 1;
    public int humPoint;
    public int tempPoint;
    private String dataLog = "";
    private String dataLogOld = "";

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

    public OpMode getMode() {
       return mode;
    }

    public void setTempPoint(int _tempPoint) {
      tempPoint = _tempPoint;
    }

    public void setHumPoint(int _humPoint) {
      humPoint = _humPoint;
    }

    private boolean operationAuto() {
       boolean isDone = false;
       // automatic fans - clim auto
       logger.finer("Operation:operationAuto> running AUTO Operation");
       
       // Collect useful data
       Device ds2438In = deviceManager.getDevice("DS2438IN");
       DataElement tempIn = ds2438In.getDataElement("TEMP");
       DataElement humIn = ds2438In.getDataElement("HUM");
       DataElement comstIn = ds2438In.getDataElement("COMST");

       Device ds2438Out = deviceManager.getDevice("DS2438OUT");
       DataElement tempOut = ds2438Out.getDataElement("TEMP");
       DataElement humOut = ds2438Out.getDataElement("HUM");
       DataElement comstOut = ds2438Out.getDataElement("COMST");

       PWM pwm = (PWM) deviceManager.getDevice("PWM");
       DataElement vOut = pwm.getDataElement("VOUT");

       IntesisBox intesisBox = (IntesisBox) deviceManager.getDevice("INTESISBOX");
       DataElement aconoff = intesisBox.getDataElement("ACONOFF");
       DataElement acmode = intesisBox.getDataElement("ACMODE");
       DataElement acfan  = intesisBox.getDataElement("ACFAN");
       DataElement acset = intesisBox.getDataElement("ACSET");
       DataElement aceco = intesisBox.getDataElement("ACECO");
       DataElement accomst = intesisBox.getDataElement("COMST");

       if (comstIn.value == 1 || comstOut.value == 1 || accomst.value == 1)
          return false;
       if (humIn.value == 0 || humOut.value == 0)
          return false;
       // Switch on Clim
       setValue(intesisBox,aconoff,1,1);
        
       // Regulate mode depending on in/out temperature
       
      // tempPoint achieved within tempDelta
       if (Math.abs(tempPoint - Math.round(tempIn.value)) <= tempDelta) {
          setValue(pwm,vOut,0,0);
          setValue(intesisBox,aceco,1,1);
          setValue(intesisBox,acfan,1,0);
          setValue(intesisBox,acset,1,tempPoint);
          isDone = true;
       }

       // tempIn < tempPoint; between n & (n+1) tempDelta
       if (isDone == false) {
          for (int n = 1; n < 10; n++) {
             if ((tempPoint - Math.round(tempIn.value)) <= tempDelta*(n+1) && 
                 (tempPoint - Math.round(tempIn.value)) >= tempDelta*n) {
                // Switch heat mode Clim
                setValue(intesisBox,acmode,1,1);
                if (tempOut.value > tempIn.value) {
                   logger.finer("Operation:operationAuto1> tempOut.value = " +tempOut.value +  "tempIn.value = " +tempIn.value);
                   setValue(pwm,vOut,0,Math.min(n,pwm.vMax));
                   setValue(intesisBox,aceco,1,1);
                   setValue(intesisBox,acfan,1,Math.min(n,intesisBox.fanMax));  
                   setValue(intesisBox,acset,1,tempPoint); 
                }
                else  {
                   logger.finer("Operation:operationAuto2> tempOut.value = " +tempOut.value +  "tempIn.value = " +tempIn.value);
                   setValue(pwm,vOut,0,0);
                   setValue(intesisBox,aceco,1,0);
                   setValue(intesisBox,acfan,1,intesisBox.fanMax);
                   setValue(intesisBox,acset,1,Math.min((int)Math.round(tempIn.value)+n,intesisBox.tempMax)); 
                }
                isDone = true;
                break;
             }
          }
       }
       // tempIn > tempPoint; between n & (n+1)
       if (isDone == false) {
          for (int n = 1; n < 10; n++) {
             if ((Math.round(tempIn.value) - tempPoint) <= tempDelta*(n+1) && 
                 (Math.round(tempIn.value) - tempPoint) >= tempDelta*n) {
                // Switch cool mode Clim
                setValue(intesisBox,acmode,1,4);
                if (tempOut.value < tempIn.value) {
                   logger.finer("Operation:operationAuto3> tempOut.value = " +tempOut.value +  "tempIn.value = " +tempIn.value);
                   setValue(pwm,vOut,0,Math.min(n,pwm.vMax));
                   setValue(intesisBox,aceco,1,1);
                   setValue(intesisBox,acfan,1,Math.min(n,intesisBox.fanMax)); 
                   setValue(intesisBox,acset,1,tempPoint); 
                }
                else {
                   logger.finer("Operation:operationAuto4> tempOut.value = " +tempOut.value +  "tempIn.value = " +tempIn.value);
                   setValue(pwm,vOut,0,0);
                   setValue(intesisBox,aceco,1,0);
                   setValue(intesisBox,acfan,1,intesisBox.fanMax);
                   setValue(intesisBox,acset,1,Math.max((int)Math.round(tempIn.value)-n,intesisBox.tempMin));
                }
                break;
             }
          }
       }
       // Regulate Fan depending on in/out humidity

       // humPoint achieved within humDelta
       if (Math.abs(humPoint - humIn.value) <= humDelta) {
          setValue(pwm,vOut,0,0);
          setValue(intesisBox,aceco,1,1);
       }

       // humIn < humPoint; between n & (n+1) humDelta
       for (int n = 1; n < 10; n++) {
          if ((humPoint - humIn.value) <= humDelta*(n+1) && (humPoint - humIn.value) > humDelta*n) {
             logger.finer("Operation:operationAuto50> humOut.value = " +humOut.value +  "humIn.value = " +humIn.value);
             if (humOut.value > humIn.value) 
                setValue(pwm,vOut,0,Math.min(n,pwm.vMax));
             else  {
                logger.finer("Operation:operationAuto5> humOut.value = " +humOut.value +  "humIn.value = " +humIn.value);
                setValue(pwm,vOut,0,0);
                setValue(intesisBox,aceco,1,1);
                setValue(intesisBox,acfan,1,0);
             }
          }
       }
       // humIn > humPoint; between n & (n+1) humDelta
       for (int n = 1; n < 10; n++) {
          if ((humIn.value - humPoint) <= humDelta*(n+1) && (humIn.value - humPoint) > humDelta*n) {
             logger.finer("Operation:operationAuto6> humOut.value = " +humOut.value +  "humIn.value = " +humIn.value);
             if (humOut.value < humIn.value) 
                setValue(pwm,vOut,0,Math.min(n,pwm.vMax)); 
             else 
                setValue(pwm,vOut,0,0);
             setValue(intesisBox,acfan,1,Math.min(n,intesisBox.fanMax));
          }
       }
       return true;
    }

    private boolean operationManual() {
       // manual fans - clim OFF
       logger.finer("Operation:operationManual> running MANUAL Operation");
       // Do nothing
       return true;
    }

    private boolean operationAlone() {
       // automatic fans- clim OFF
       logger.finer("Operation:operationAlone> running ALONE Operation");
       
       // Collect useful data
       Device ds2438In = deviceManager.getDevice("DS2438IN");
       DataElement tempIn = ds2438In.getDataElement("TEMP");
       DataElement humIn = ds2438In.getDataElement("HUM");
       DataElement comstIn = ds2438In.getDataElement("COMST");

       Device ds2438Out = deviceManager.getDevice("DS2438OUT");
       DataElement tempOut = ds2438Out.getDataElement("TEMP");
       DataElement humOut = ds2438Out.getDataElement("HUM");
       DataElement comstOut = ds2438Out.getDataElement("COMST");

       PWM pwm = (PWM) deviceManager.getDevice("PWM");
       DataElement vOut = pwm.getDataElement("VOUT");

       if (comstIn.value == 1 || comstOut.value == 1 )
          return false;
       if (humIn.value == 0 || humOut.value == 0)
          return false;
       // Regulate Fan depending on in/out humidity

       // humPoint achieved within humDelta
       if (Math.abs(humPoint - humIn.value) <= humDelta)
          setValue(pwm,vOut,0,0);

       // humIn < humPoint; between n & (n+1) humDelta
       for (int n = 1; n < 10; n++) {
          if ((humPoint - humIn.value) <= humDelta*(n+1) && (humPoint - humIn.value) > humDelta*n) {
             if (humOut.value > humIn.value) 
                setValue(pwm,vOut,0,Math.min(n,pwm.vMax)); 
             else 
                setValue(pwm,vOut,0,0);
          }
       }
       // humIn > humPoint; between n & (n+1) humDelta
       for (int n = 1; n < 10; n++) {
          if ((humIn.value - humPoint) <= humDelta*(n+1) && (humIn.value - humPoint) > humDelta*n) {
             if (humOut.value < humIn.value) 
                setValue(pwm,vOut,0,Math.min(n,pwm.vMax));
             else 
                setValue(pwm,vOut,0,0);
          }
       }
       return true;
    }
    
    private void setValue(Device d, DataElement e, int type, double value) {
       logger.finer("Operation::setValue> Execute command " + d.name + " " + e.name + " " + value); 
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

    public void setACOFF() {
       IntesisBox intesisBox = (IntesisBox) deviceManager.getDevice("INTESISBOX");
       DataElement aconoff = intesisBox.getDataElement("ACONOFF");
       // Switch OFF Clim/TEMP
       setValue(intesisBox,aconoff,1,0);
    }
    
    private void saveLog() {
       // Log Data
       Device ds2438In = deviceManager.getDevice("DS2438IN");
       Device ds2438Out = deviceManager.getDevice("DS2438OUT");
       PWM pwm = (PWM) deviceManager.getDevice("PWM");
       IntesisBox intesisBox = (IntesisBox) deviceManager.getDevice("INTESISBOX");
       DecimalFormat df = new DecimalFormat("#.#");

       Collection<DataElement> dataElements = pwm.dataList.values();
       switch (mode) {
          case AUTO:   dataLog = "MODE: [ AUTO ]";   break;
          case MANUAL: dataLog = "MODE: [ MANUAL ]"; break;
          case ALONE:  dataLog = "MODE: [ ALONE ]";  break;
       }
       dataLog += " FAN:[";
       dataElements.forEach((d) -> {
            dataLog = dataLog + " " + d.name + "=" + df.format(d.value) + " ";
       });
       dataElements = intesisBox.dataList.values();
       dataLog += "] AC UNIT:[";
       dataElements.forEach((d) -> {
            dataLog = dataLog + " " + d.name + "=" + df.format(d.value) + " ";
       });
       dataLog += "] SET:[ TEMP=" + tempPoint + " HUM=" + humPoint + " ]";
       if ( dataLog.equals(dataLogOld) )  
          return;
       // Log changed: save it with other data
       dataLogOld = dataLog;
       dataElements = ds2438In.dataList.values();
       dataLog += " DS2438IN:[";
       dataElements.forEach((d) -> {
            dataLog = dataLog + " " + d.name + "=" + df.format(d.value) + " ";
       });
       dataElements = ds2438Out.dataList.values();
       dataLog += "] DS243OUT:[";
       dataElements.forEach((d) -> {
            dataLog = dataLog + " " + d.name + "=" + df.format(d.value) + " ";
       });
       dataLog += "]";
       logger.fine(dataLog);
    }
    public void run () {
       boolean isOk = false;
       int saveCnt = 0;
       try {  
          while (true) {
             switch (mode) {
                case AUTO:   isOk = operationAuto();   break;
                case MANUAL: isOk = operationManual(); break;
                case ALONE:  isOk = operationAlone();  break;
             }
             Thread.sleep(cycleFreq * 1000);
             if (isOk == true ) {//&& ++saveCnt >= 12) {
               saveCnt = 0;
               saveLog();
             }
	  }
       }
       catch (InterruptedException ex) {
          logger.log(Level.SEVERE, "Operation:run:InterruptedException> " + ex.getMessage());
       }
       logger.finer("Operation:run> Stop thread Operation");
    }
}
