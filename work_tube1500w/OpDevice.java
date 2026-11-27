/*
 * Modbus device OP: the setpoint ramp of Operation.java (RAMP PROGRAMMATION box of the panel) driven
 * through Modbus, with the same parameters and the same START/STOP as the panel.
 *
 *   TEMPSTEP, TEMPMAX, TIMEINTER   FLOAT32, read/write   ramp parameters (degC, degC, s)
 *   RAMPCMD                        INT16, trigger        1 = START, 2 = STOP (as the buttons of the panel)
 *   RAMPST                         INT16, read only      0 = no ramp running, 1 = ramp running
 *
 * It has no hardware. Its registers are placed after those of the last device, so that the offsets of
 * the other devices do not change.
 */
import java.util.logging.Logger;
import java.util.logging.Level;

public class OpDevice extends Device {

   private static final Logger logger = Logger.getLogger("Main");
   private Operation op;

   public OpDevice (String _name, int _mbRegisterStart, Operation _op) {

     name = _name; // Device name
     op = _op;
     pause = 500;
     mbRegisterStart = _mbRegisterStart;  // Starting Modbus register offset
     mbRegisterEnd = mbRegisterStart;

     addDataElement( new DataElement(name, "TEMPSTEP", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd));
     addDataElement( new DataElement(name, "TEMPMAX", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "TIMEINTER", DataType.READ_AND_WRITE_VALUE,RegisterType.FLOAT32,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "RAMPCMD", DataType.TRIGGER,RegisterType.INT16,mbRegisterEnd+=2));
     addDataElement( new DataElement(name, "RAMPST", DataType.READ_ONLY_STATUS,RegisterType.INT16,mbRegisterEnd+=1));

     mbRegisterEnd+=1;

     logger.finer("OpDevice:OpDevice> " + name + " Modbus registers ends at offset " + mbRegisterEnd);
   }

   public void updateDeviceData() {
     if (op == null) return;
     getDataElement("TEMPSTEP").value = op.getTempStep();
     getDataElement("TEMPMAX").value = op.getTempMax();
     getDataElement("TIMEINTER").value = op.getTimeInter();
     getDataElement("RAMPST").value = op.isRunning() ? 1 : 0;
   }

   public void executeCommand( DataElement e ) {
      if (op == null) return;
      try {
         if (e.name.equals("RAMPCMD")) {
            if (e.value == 1) {
               logger.info("OpDevice:executeCommand> Ramp START asked through Modbus");
               op.setMode(OpMode.START);
            }
            else if (e.value == 2) {
               logger.info("OpDevice:executeCommand> Ramp STOP asked through Modbus");
               op.setMode(OpMode.STOP);
            }
            // Reset
            e.value = 0;
            holdingRegisters.setInt16At(e.mbRegisterOffset, 0);
         }
         else if (e.setvalue < 0 || Double.isNaN(e.setvalue) || Double.isInfinite(e.setvalue))
            logger.log(Level.WARNING, "OpDevice:executeCommand> refused " + e.name + " = " + e.setvalue);
         else if (e.name.equals("TEMPSTEP")) op.setTempStep(e.setvalue);
         else if (e.name.equals("TEMPMAX")) op.setTempMax(e.setvalue);
         else if (e.name.equals("TIMEINTER")) op.setTimeInter(e.setvalue);
      }
      catch (Exception ex) {
         logger.log(Level.SEVERE, "OpDevice:executeCommand>" + ex.getMessage());
      }
   }

   // Every command received is executed, then the registers are refreshed, all under the Modbus lock:
   // a value written by the Modbus master is never replaced by the old one before it has been applied.
   public void run() {
     try {
        while(true) {
           modbusmutex.lock();
           try {
              while (!commandSetQueue.isEmpty()) popCommand();
              updateDeviceData();
              if (holdingRegisters != null) updateModbusRegisters();
           }
           finally {
              modbusmutex.unlock();
           }
           Thread.sleep(pause);
        }
     }
     catch (InterruptedException e) {
        logger.log(Level.SEVERE, "OpDevice:run> " + e.getMessage());
     }
   }
};
