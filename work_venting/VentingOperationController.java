/*
 * Virtual device exposing Venting Operation control/status channels on Modbus.
 */
import java.util.logging.Logger;
import java.util.logging.Level;

public class VentingOperationController extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   private static final int COMMAND_PULSE_MS = 2000;

   public VentingOperationController(String _name, int _mbRegisterStart) {

      name = _name;
      mbRegisterStart = _mbRegisterStart;

      logger.finer("VentingOperationController:VentingOperationController> " + name +
                   " Modbus registers starts at offset " + mbRegisterStart);

      mbRegisterEnd = mbRegisterStart;

      addDataElement(new DataElement(name, "EXECUTE_CMD",
                                     DataType.READ_AND_WRITE_STATUS, RegisterType.INT16, mbRegisterEnd));
      addDataElement(new DataElement(name, "STOP_CMD",
                                     DataType.READ_AND_WRITE_STATUS, RegisterType.INT16, mbRegisterEnd += 1));
      addDataElement(new DataElement(name, "STEP",
                                     DataType.READ_ONLY_STATUS, RegisterType.INT16, mbRegisterEnd += 1));
      addDataElement(new DataElement(name, "COMST",
                                     DataType.COM_STATUS, RegisterType.INT16, mbRegisterEnd += 1));

      mbRegisterEnd += 1;

      DataElement com = getDataElement("COMST");
      if (com != null) {
         com.value = 0;
      }

      logger.finer("VentingOperationController:VentingOperationController> " + name +
                   " Modbus registers ends at offset " + mbRegisterEnd);
   }

   public void updateDeviceData() {
      try {
         DataElement com = getDataElement("COMST");
         if (com != null) {
            com.value = 0;
         }
         popCommand();
      } catch (Exception ex) {
         logger.log(Level.SEVERE, "VentingOperationController:updateDeviceData> " + ex.getMessage());
      }
   }

   public void executeCommand(DataElement e) {
      try {
         if (e.name.equals("EXECUTE_CMD") || e.name.equals("STOP_CMD")) {
            // Trigger-like behavior: set command high then auto-reset after pulse.
            if ((int)e.setvalue != 0) {
               e.value = 1;
               Thread.sleep(COMMAND_PULSE_MS);
               e.value = 0;
               e.setvalue = 0;
               if (holdingRegisters != null) {
                  holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
               }
            } else {
               e.value = 0;
            }
            return;
         }

         // Keep non-command channels in sync with their setpoint.
         e.value = e.setvalue;
      } catch (Exception ex) {
         logger.log(Level.SEVERE, "VentingOperationController:executeCommand> " + ex.getMessage());
      }
   }
}
