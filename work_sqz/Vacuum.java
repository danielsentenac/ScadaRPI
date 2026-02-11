/*
 * This Class is the implementation of the Vacuum device
 *
 */
import java.util.*;
import java.io.IOException;
import java.lang.Exception;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URLConnection; //for url management 
import java.net.URL;
import java.io.ObjectInputStream; //for stream management
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ConnectException;
import java.net.CookieManager;
import java.net.CookieHandler;

public class Vacuum extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   Vector<String> svrNameList = new Vector<String>();
   Vector<String> svrValueList = new Vector<String>();
   private DeviceManager deviceManager;
   private boolean vac_Start = false; // 'Vacuum operation start' boolean; init to false
   private boolean vac_Stop = false; // 'Vacuum operation stop' boolean; init to false
   private boolean vac_isStarted = false; // 'Vacuum operation started' boolean; init to false
   private boolean vac_isStopped = false; // 'Vacuum operation stopped' boolean; init to false
   private int vac_step = -1;  // Vacuum operation progress step
   private double g22tmp = 0;
   private int g22wait_st = 0;
   private int g22wait_ust = 0;
   private int v1wait = 0;
   private int v21wait = 0;
   private int p21wait = 0;
   
   public Vacuum (String _name,
                  int _mbRegisterStart,
                  DeviceManager _deviceManager) {

     name = _name; // Device name
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset
     deviceManager = _deviceManager; // Get deviceManager to retrieve other devices values

     logger.finer("Vacuum:Vacuum> " + name + " Modbus registers starts at offset " + mbRegisterStart);

     mbRegisterEnd = mbRegisterStart;

     // Vacuum Status
     addDataElement( new DataElement(name, "ST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd));

     // Commands
     addDataElement( new DataElement(name, "STARTSTOP", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=1));
     
     // Com Status
     addDataElement( new DataElement(name, "COMST", DataType.COM_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("Vacuum:Vacuum> " + name + " Modbus registers ends at offset " + mbRegisterEnd);
    
   }
   
   @SuppressWarnings("unchecked")
   public void updateDeviceData() {
   
     // Get data elements

     DataElement vacuum = getDataElement("ST");
     DataElement dcom = getDataElement("COMST");
     DataElement m1comst = deviceManager.getDevice("M1").getDataElement("COMST");
     DataElement m2comst = deviceManager.getDevice("M2").getDataElement("COMST");
     DataElement dcucomst = deviceManager.getDevice("DCU").getDataElement("P21COMST");
     DataElement mgcomst = deviceManager.getDevice("MG").getDataElement("COMST");
     DataElement v21st = deviceManager.getDevice("M1").getDataElement("V21ST");
     DataElement v21cmd = deviceManager.getDevice("M1").getDataElement("V21CMD");
     DataElement v22st = deviceManager.getDevice("M1").getDataElement("V22ST");
     DataElement v22cmd = deviceManager.getDevice("M1").getDataElement("V22CMD");
     DataElement v1st = deviceManager.getDevice("M1").getDataElement("V1ST");
     DataElement v1cmd = deviceManager.getDevice("M1").getDataElement("V1CMD");
     DataElement v23st = deviceManager.getDevice("M1").getDataElement("V23ST");
     DataElement bypassg22st = deviceManager.getDevice("M1").getDataElement("BYPASSST");
     DataElement bypassg22onoff = deviceManager.getDevice("M1").getDataElement("BYPASSONOFF");
     DataElement vpst = deviceManager.getDevice("M2").getDataElement("VPST");
     DataElement vpcmd = deviceManager.getDevice("M2").getDataElement("VPCMD");
     DataElement v24st = deviceManager.getDevice("M2").getDataElement("V24ST");
     DataElement v25st = deviceManager.getDevice("M2").getDataElement("V25ST");
     DataElement p21st = deviceManager.getDevice("DCU").getDataElement("P21ST");
     DataElement p21onoff = deviceManager.getDevice("DCU").getDataElement("P21ONOFF");
     DataElement p21speed = deviceManager.getDevice("DCU").getDataElement("P21SPEED");
     DataElement p22onoff = deviceManager.getDevice("M1").getDataElement("P22ONOFF");
     DataElement p22st = deviceManager.getDevice("M1").getDataElement("P22ST");
     DataElement ge2 = deviceManager.getDevice("MG").getDataElement("PR5");
     DataElement ge1 = deviceManager.getDevice("MG").getDataElement("PR3");
     DataElement g22 = deviceManager.getDevice("MG").getDataElement("PR1");
     try {
                 
        popCommand();  // Execute commands in the loop is more reactive
      
        // Check if instruments are all OK
        int allinstrOK = (int)(m1comst.value) | (int)(m2comst.value)|(int)(mgcomst.value)|(int)(dcucomst.value);
        if ( allinstrOK != 0 ) {
           dcom.value = 1; // COM NOT OK
           vacuum.value = 250; // CHECK INSTRUMENT COM
           return;
        }
        dcom.value = 0; // if arriving here COM OK
        
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Check vacuum status 
        // Check if Chamber is in AIR or VACUUM based on V24 and P21 Speed
        if ( v24st.value == 1 ) {
           vacuum.value = 252; // IN AIR
           vac_Start = false; // reset
           vac_isStarted = false; // reset
           return; // NOT ALLOWING STARTING VACUUM!!
        }
        else if ( p21speed.value > 400 && v21st.value == 1) {
           vacuum.value = 253; // IN VACUUM
           return; // ALREADY IN VACUUM OR VACUUM REACHED !!
        }
        else if (p21st.value == 1 || vpst.value == 1 || v23st.value == 1) {
           vacuum.value = 251; // MISSING INITAL CONDITION
           vac_Start = false; // reset
           return; // NOT ALLOWING STARTING VACUUM!!
        }
        else {
           vacuum.value = 0;  // READY FOR VACUUM OPERATION 
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Start command received; Check vacuum status
        if (vac_Start == true) {
           if (vacuum.value == 251 || vacuum.value == 252 || vacuum.value == 253) {
              vac_Start = false; // reset
              return; // NOT ALLOWING STARTING VACUUM!!
           }
           else {
              vacuum.value = 1; // VACUUM STARTED
              vac_isStarted = true;
              return;
              }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Stop command received; Check vacuum status
        if (vac_Stop == true) {
           if (vacuum.value == 252 || vacuum.value == 253) {
              vac_Stop = false; // reset
              return; // NOTHING TO DO!!
           }
           else vac_isStopped = true;
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 1: P22 Switch ON command
        if (vac_isStarted == true && vac_step == -1) {
           // Switch ON P22 command
           p22onoff.setvalue = 1;
           deviceManager.getDevice("M1").commandSetQueue.add(p22onoff); // Set ON internal data command trigger
           vac_step = 0; // P22 SWITCH ON COMMAND SENT
           return;
        }
        // Vacuum Operation; Validate Step 0
        if (vac_isStarted == true && vac_step == 0) {
           if (p22st.value == 1) {
              vacuum.value = 2; // P22 SWITCHED ON
              vac_step = 1; // P22 SWITCHED ON
              return;
           }
           else {
              vacuum.value = 22; // Wait P22 to switch ON
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 2: Bypass G22 Switch ON command
        if (vac_isStarted == true && vac_step == 1) {
           // Switch ON BypassG22 command
           bypassg22onoff.setvalue = 1;
           deviceManager.getDevice("M1").commandSetQueue.add(bypassg22onoff); // Set ON internal data command trigger
           vac_step = 2; // BYPASS G22 SWITCH ON COMMAND SENT
           return;
        }
        // Vacuum Operation; Validate Step 2
        if (vac_isStarted == true && vac_step == 2) {
           if (bypassg22st.value == 1) {
              vacuum.value = 3; // BYPASS G22 SWITCHED ON
              vac_step = 3; // BYPASS G22 SWITCHED ON
              return;
           }
           else {
              vacuum.value = 33; // Wait Bypass G22 to switch ON
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 4: V1 Open command
        if (vac_isStarted == true && vac_step == 3) {
           // Open V1 command
           v1cmd.setvalue = 1;
           deviceManager.getDevice("M1").commandSetQueue.add(v1cmd); // Set OPEN internal data command trigger
           vac_step = 4; // V1 OPEN COMMAND SENT
           return;
        }
        // Vacuum Operation; Validate Step 4
        if (vac_isStarted == true && vac_step == 4) {
           if (v1st.value == 1) {
              vacuum.value = 4; // V1 OPENED
              vac_step = 5; // V1 OPENED
              return;
           }
           else {
              vacuum.value = 44; // V1 not yet Opened
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 5: V22 Open command
        if (vac_isStarted == true && vac_step == 5) {
           // Open V22 command
           v22cmd.setvalue = 1;
           deviceManager.getDevice("M1").commandSetQueue.add(v22cmd); // Set OPEN internal data command trigger
           vac_step = 6; // V22 OPEN COMMAND SENT
           return;
        }
        // Vacuum Operation; Validate Step 6
        if (vac_isStarted == true && vac_step == 6) {
           if (v22st.value == 1) {
              vacuum.value = 5; // V22 OPENED
              vac_step = 7; // V22 OPENED
              return;
           }
           else {
              vacuum.value = 55; // V22 not yet Opened
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 7: Validate Ge2 < 800mbar
        if (vac_isStarted == true && vac_step == 7) {
           // Check Ge2 value
           if (ge2.value >= 800) {
              vacuum.value = 66; // Ge2 not yet at 800mbar
              return;
           }
           else {
              vacuum.value = 6; // Ge2 reached 800mbar
              vac_step = 8; // Ge2 reached 800mbar
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 8: V22 Close command
        if (vac_isStarted == true && vac_step == 8) {
           // Close V22 command
           v22cmd.setvalue = 2;
           deviceManager.getDevice("M1").commandSetQueue.add(v22cmd); // Set CLOSE internal data command trigger
           vac_step = 9; // V22 CLOSE COMMAND SENT
           return;
        }
        // Vacuum Operation; Validate Step 9
        if (vac_isStarted == true && vac_step == 9) {
           if (v22st.value == 2) {
              vacuum.value = 7; // V22 CLOSED
              vac_step = 10; // V22 CLOSED
              g22wait_st = 0; // reset
              g22wait_ust = 0; // reset
              return;
           }
           else {
              vacuum.value = 77; // V22 not yet Closed
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 10: Wait for G22 stabilization +/- 10mbar
        if (vac_isStarted == true && vac_step == 10) {
           if (Math.abs(g22.value - g22tmp) > 10 ) {
              g22tmp = g22.value; // Update g22tmp
              vacuum.value = 88; // Wait for G22 stabilization
              g22wait_ust++;
              return;
           }           
           else g22wait_st++;
           
           if (g22wait_st > 60) {
              vacuum.value = 8; // G22 stable for 1 minute long
              vac_step = 11;
              return;
           }   
           if (g22wait_ust > 60) {
              vac_Start = false;
              vac_isStarted = false;
              vacuum.value = 254; // G22 not stable for 1 minute long
              return;
           }        
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 5: V22 Open command
        if (vac_isStarted == true && vac_step == 11) {
           // Open V22 command
           v22cmd.setvalue = 1;
           deviceManager.getDevice("M1").commandSetQueue.add(v22cmd); // Set OPEN internal data command trigger
           vac_step = 12; // V22 OPEN COMMAND SENT
           return;
        }
        // Vacuum Operation; Validate Step 12
        if (vac_isStarted == true && vac_step == 12) {
           if (v22st.value == 1) {
              vacuum.value = 9; // V22 OPENED
              vac_step = 13; // V22 OPENED
              return;
           }
           else {
              vacuum.value = 99; // V22 not yet Opened
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 13: Validate Ge1 < 0.5 mbar
        if (vac_isStarted == true && vac_step == 13) {
           // Check Ge1 value
           if (ge1.value >= 0.5) {
              vacuum.value = 100; // Ge1 not yet at 0.5 mbar
              return;
           }
           else {
              vacuum.value = 10; // Ge1 reached 0.5 mbar
              vac_step = 14; // Ge1 reached 0.5 mbar
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 14: V1 Close command
        if (vac_isStarted == true && vac_step == 14) {
           // Close V1 command
           v1cmd.setvalue = 2;
           deviceManager.getDevice("M1").commandSetQueue.add(v1cmd); // Set CLOSE internal data command trigger
           vac_step = 15; // V1 CLOSE COMMAND SENT
           v1wait = 0; // reset
           return;
        }
        // Vacuum Operation; Validate Step 15
        if (vac_isStarted == true && vac_step == 15) {
           if (v1st.value == 2) {
              vacuum.value = 11; // V1 CLOSED
              vac_step = 16; // V1 CLOSED
           }
           else {
              vacuum.value = 111; // V1 not yet Closed
              v1wait++;
           }
           if (v1wait > 60) {
              vacuum.value = 255;
              vac_Start = false;
              vac_isStarted = false;
              // Close V22 command
              v22cmd.setvalue = 2;
              deviceManager.getDevice("M1").commandSetQueue.add(v22cmd); // Set CLOSE internal data command trigger
           }
           return;
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 1: P21 Switch ON command
        if (vac_isStarted == true && vac_step == 16) {
           // Switch ON P21 command
           p21onoff.setvalue = 1;
           deviceManager.getDevice("M1").commandSetQueue.add(p21onoff); // Set ON internal data command trigger
           vac_step = 17; // P21 SWITCH ON COMMAND SENT
           p21wait = 0; // reset
           return;
        }
        // Vacuum Operation; Validate Step 17
        if (vac_isStarted == true && vac_step == 17) {
           if (p21st.value == 1) {
              vacuum.value = 12; // P21 SWITCHED ON
              vac_step = 18; // P21 SWITCHED ON
              return;
           }
           else {
              vacuum.value = 122; // Wait P21 to switch ON
              p21wait++;
           }
           if ( p21wait > 60 ) {
              // P21 COULD NOT SWITCH ON -- STOP VACUUM
              // Close V22 command
              vacuum.value = 256;
              vac_Start = false;
              vac_isStarted = false;
              v22cmd.setvalue = 2;
              deviceManager.getDevice("M1").commandSetQueue.add(v22cmd); // Set CLOSE internal data command trigger
              // P21 OFF command
              p21onoff.setvalue = 2;
              deviceManager.getDevice("M1").commandSetQueue.add(p21onoff); // Set ON internal data command trigger
           }
           return;
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 18: P21 must reach 400 Hz
        if (vac_isStarted == true && vac_step == 18) {
           if (p21speed.value < 400) {
              // P21 NOT AT 400HZ YET
              vacuum.value = 133;
              return;
           }
           else {
              vacuum.value = 13; // P21 REACHED 400HZ
              vac_step = 19;  // P21 REACHED 400HZ
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 19: V21 Open command
        if (vac_isStarted == true && vac_step == 19) {
           // Open V22 command
           v21cmd.setvalue = 1;
           deviceManager.getDevice("M1").commandSetQueue.add(v21cmd); // Set OPEN internal data command trigger
           vac_step = 20; // V21 OPEN COMMAND SENT
           return;
        }
        // Vacuum Operation; Validate Step 20
        if (vac_isStarted == true && vac_step == 20) {
           if (v21st.value == 1) {
              vacuum.value = 14; // V21 OPENED
              vac_step = 21; // V21 OPENED
              return;
           }
           else {
              vacuum.value = 144; // V21 not yet Opened
              v21wait++;
           }
           if (v21wait > 60) {
              // V21 COULD NOT OPEN -- STOP VACUUM
              // Close V22 command
              vacuum.value = 257;
              vac_Start = false;
              vac_isStarted = false;
              v22cmd.setvalue = 2;
              deviceManager.getDevice("M1").commandSetQueue.add(v22cmd); // Set CLOSE internal data command trigger
           }
           return;
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 21: Validate G22 < 0.5 mbar
        if (vac_isStarted == true && vac_step == 21) {
           // Check G22 value
           if (g22.value >= 0.5) {
              vacuum.value = 155; // G22 not yet at 0.5 mbar
              return;
           }
           else {
              vacuum.value = 15; // G22 reached 0.5 mbar
              vac_step = 22; // G22 reached 0.5 mbar
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Vacuum Operation Step 22: Bypass G22 Switch OFF command
        if (vac_isStarted == true && vac_step == 22) {
           // Switch OFF BypassG22 command
           bypassg22onoff.setvalue = 2;
           deviceManager.getDevice("M1").commandSetQueue.add(bypassg22onoff); // Set OFF internal data command trigger
           vac_step = 23; // BYPASS G22 SWITCH OFF COMMAND SENT
           return;
        }
        // Vacuum Operation; Validate Step 23
        if (vac_isStarted == true && vac_step == 23) {
           if (bypassg22st.value == 2) {
              vacuum.value = 16; // BYPASS G22 SWITCHED OFF
              vac_step = 24; // BYPASS G22 SWITCHED ON
              return;
           }
           else {
              vacuum.value = 166; // Wait Bypass G22 to switch OFF
              return;
           }
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        
        if ( hasWarned == true ) {
           hasWarned = false;
           logger.info("Vacuum:updateDeviceData> Communication with " + name + " back!");
        }
     }
     catch (Exception ex) {
        ex.printStackTrace();
        if ( hasWarned == false ) {
           logger.log(Level.WARNING, "Vacuum:updateDeviceData> Communication with " + name + " interrupted");
           logger.log(Level.SEVERE, "Vacuum:updateDeviceData>" + ex.getMessage());
        }
        setErrorComStatus();
     }
   }
   @SuppressWarnings("unchecked")
   public void executeCommand( DataElement e ) {
      try {
         if ( e.name.contains("STARTSTOP") ) { // Vacuum Start/Stop command
            if ( e.value == 1 )  {    // Start Vacuum
                //if ( vac_isStarted == false ) vac_Start = true;
            }
           // else if ( e.value == 2 ) // Stop Vacuum
               // vac_Stop = true;
         }
         Thread.sleep(2000);
         e.value = 0;
         holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
      }
      catch (Exception ex) {
        ex.printStackTrace();
        logger.log(Level.SEVERE, "Vacuum>executeCommand:" + ex.getMessage());
        setErrorComStatus();
        logger.log(Level.WARNING, "Vacuum:executeCommand> Communication with " + name + " interrupted");
     }
   }
         
}; 
