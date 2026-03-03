import javax.swing.table.TableModel;
import java.util.*;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;

/**
 * Executes the venting sequence defined in the 'operations' table.
 *
 * One step = one row in the table:
 *  STEP, P1, V1, VSOFT, VMAIN, VDRYER, VRP, MKS2000, MKS50000, G2
 *
 * For each step (in increasing STEP order):
 *   1) Set MKS2000 / MKS50000 setpoints
 *   2) Set valves (V1, VSOFT, VMAIN, VDRYER, VRP)
 *   3) Set pump P1 (ON/OFF)
 *   4) Wait until G2 (chamber pressure) reaches target from that row
 */
public class VentingOperations implements Runnable {

    private static final Logger logger = Logger.getLogger("Main");

    private final DeviceManager deviceManager;
    private final List<Step> steps;
    private final String g2StatusKey;                  // e.g. "G2Val"
    private final String mks2000StatusKey;   // e.g. "FlowSetP2000Val"
    private final String mks50000StatusKey;  // e.g. "FlowSetP50000Val"
    private final String stepStatusKey;      // e.g. "OP_STEP"
    private final JTable table;
    private volatile boolean stopRequested = false;
    
    
    private static class Step {
        int rowIndex;       // index in the JTable model
        int step;
        String p1;          // "ON" / "OFF"
        String v1;          // "OPEN" / "CLOSE"
        String vsoft;
        String vmain;
        String vdryer;
        String vrp;
        int mks2000;        // 0..2000
        int mks50000;       // 0..50000
        double g2Target;    // pressure to reach before next step
    }

    public VentingOperations(DeviceManager deviceManager,
                         TableModel model,
                         String g2StatusKey,
                         String mks2000StatusKey,
                         String mks50000StatusKey,
                         String stepStatusKey,
                         JTable table) {
    this.deviceManager = deviceManager;
    this.g2StatusKey = g2StatusKey;
    this.mks2000StatusKey = mks2000StatusKey;
    this.mks50000StatusKey = mks50000StatusKey;
    this.stepStatusKey = stepStatusKey;
    this.table = table; 
    this.steps = buildStepsFromModel(model);
    }

    public void requestStop() {
        stopRequested = true;
    }
    
    @Override
    public void run() {
	    logger.info("VentingOperations: starting sequence with " + steps.size() + " steps.");
            setOperationStep(0);

	    for (int i = 0; i < steps.size(); i++) {
		// global stop check at top of loop
		if (stopRequested || Thread.currentThread().isInterrupted()) {
		    logger.info("VentingOperations: stop requested before step " + i + ", aborting sequence.");
		    break;
		}

		Step step = steps.get(i);
		boolean isLastStep = (i == steps.size() - 1);
                setOperationStep(step.step);

		// 🔦 highlight active row
		highlightRow(step.rowIndex);
		
		logger.info("VentingOperations: executing step " + step.step +
		            (isLastStep ? " (last step)" : ""));

		// 1) MKS flows
		sendMksCommands(step);
		if (stopRequested || Thread.currentThread().isInterrupted()) break;

		// 2) Valve states
		sendValveCommand("M1_V1CMD",     step.v1);
		sendValveCommand("M1_VSOFTCMD",  step.vsoft);
		sendValveCommand("M1_VMAINCMD",  step.vmain);
		sendValveCommand("M1_VDRYERCMD", step.vdryer);
		sendValveCommand("M1_VRPCMD",    step.vrp);
		if (stopRequested || Thread.currentThread().isInterrupted()) break;

		// 3) Pump P1
		sendPumpCommand("M1_P1ONOFF", step.p1);
		if (stopRequested || Thread.currentThread().isInterrupted()) break;

		// 4) Wait for pressure *only if there is a next step*
		if (!isLastStep && step.g2Target > 0) {
		    boolean stopped = waitForPressure(step.g2Target);
		    if (stopped) {
		        logger.info("VentingOperations: stopped while waiting for pressure.");
		        break;
		    }
		}
	    }

            setOperationStep(0);
	    logger.info("VentingOperations: sequence finished.");
    }

    private void highlightRow(int rowIndex) {
	    if (table == null || rowIndex < 0) {
		return;
	    }

	    SwingUtilities.invokeLater(() -> {
		VentingLookAndFeel.setActiveRow(table, rowIndex);

		table.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
		table.setColumnSelectionInterval(0, table.getColumnCount() - 1);

		Rectangle rect = table.getCellRect(rowIndex, 0, true);
		table.scrollRectToVisible(rect);
	    });
    }


    /* ===================== BUILD STEPS FROM TABLE ===================== */

    private List<Step> buildStepsFromModel(TableModel model) {
        List<Step> list = new ArrayList<>();

        int rowCount = model.getRowCount();
        for (int r = 0; r < rowCount; r++) {
            Step s = new Step();
            try {
                s.rowIndex  = r;  // <-- remember model row
                s.step      = toInt(model.getValueAt(r, 0), 0);
                s.p1        = toString(model.getValueAt(r, 1));
                s.v1        = toString(model.getValueAt(r, 2));
                s.vsoft     = toString(model.getValueAt(r, 3));
                s.vmain     = toString(model.getValueAt(r, 4));
                s.vdryer    = toString(model.getValueAt(r, 5));
                s.vrp       = toString(model.getValueAt(r, 6));
                s.mks2000   = toInt(model.getValueAt(r, 7), 0);
                s.mks50000  = toInt(model.getValueAt(r, 8), 0);
                s.g2Target  = toDouble(model.getValueAt(r, 9), 0.0);
            } catch (Exception ex) {
                logger.warning("VentingOperations: error reading row " + r + " -> " + ex.getMessage());
                continue;
            }
            list.add(s);
        }

        // sort by STEP ascending
        Collections.sort(list, Comparator.comparingInt(s -> s.step));
        return list;
    }

    private static String toString(Object o) {
        return (o == null) ? "" : o.toString().trim();
    }

    private static int toInt(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (Exception ex) {
            return def;
        }
    }

    private static double toDouble(Object o, double def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(o.toString().trim());
        } catch (Exception ex) {
            return def;
        }
    }

    /* ===================== COMMAND SENDING HELPERS ===================== */

    private void sendPumpCommand(String cmdName, String state) {
        if (state == null || state.isEmpty()) return;

        String[] parts = cmdName.split("_", 2);
        if (parts.length < 2) return;

        String deviceName = parts[0];
        String dataName   = parts[1];

        Device device = deviceManager.getDevice(deviceName);
        if (device == null) {
            logger.warning("VentingOperations: pump device '" + deviceName + "' not found");
            return;
        }

        DataElement de = device.getDataElement(dataName);
        if (de == null) {
            logger.warning("VentingOperations: pump dataElement '" + dataName + "' not found in device '" + deviceName + "'");
            return;
        }

        if (state.equalsIgnoreCase("ON")) {
            de.setvalue = 1;
        } else if (state.equalsIgnoreCase("OFF")) {
            de.setvalue = 2;
        } else {
            logger.warning("VentingOperations: unknown P1 state '" + state + "', expected ON/OFF");
            return;
        }
        device.commandSetQueue.add(de);
        logger.info("VentingOperations: pump " + cmdName + " -> " + state);
    }

    private void sendValveCommand(String cmdName, String state) {
        if (state == null || state.isEmpty()) return;

        String[] parts = cmdName.split("_", 2);
        if (parts.length < 2) return;

        String deviceName = parts[0];
        String dataName   = parts[1];

        Device device = deviceManager.getDevice(deviceName);
        if (device == null) {
            logger.warning("VentingOperations: valve device '" + deviceName + "' not found");
            return;
        }

        DataElement de = device.getDataElement(dataName);
        if (de == null) {
            logger.warning("VentingOperations: valve dataElement '" + dataName + "' not found in device '" + deviceName + "'");
            return;
        }

        if (state.equalsIgnoreCase("OPEN")) {
            de.setvalue = 1;
        } else if (state.equalsIgnoreCase("CLOSE")) {
            de.setvalue = 2;
        } else {
            logger.warning("VentingOperations: unknown valve state '" + state + "', expected OPEN/CLOSE");
            return;
        }
        device.commandSetQueue.add(de);
        logger.info("VentingOperations: valve " + cmdName + " -> " + state);
    }

    private void sendMksCommands(Step step) {
	    // For venting, MKS columns are FLOW setpoints.
	    // Use STATUS mapping (FlowSetP...) and send as double,
	    // same behavior as DialogSetPoint.
	    if (mks2000StatusKey != null && step.mks2000 > 0) {
		sendFlowUsingStatusKey(mks2000StatusKey, (double) step.mks2000);
	    }
	    if (mks50000StatusKey != null && step.mks50000 > 0) {
		sendFlowUsingStatusKey(mks50000StatusKey, (double) step.mks50000);
	    }
    }

    private void sendFlowUsingStatusKey(String mapping, double flow) {
	    String[] parts = mapping.split("_", 2);
	    if (parts.length < 2) {
		logger.warning("VentingOperations: bad STATUS mapping for '" + mapping);
		return;
	    }
	    String deviceName = parts[0];
	    String dataName   = parts[1];
	    Device device = deviceManager.getDevice(deviceName);
	    if (device == null) {
		logger.warning("VentingOperations: Flow device '" + deviceName + "' not found");
		return;
	    }
	    DataElement dataElement = device.getDataElement(dataName);
	    if (dataElement == null) {
		logger.warning("VentingOperations: Flow dataElement '" + dataName + "' not found in device '" + deviceName + "'");
		return;
    }

    dataElement.setvalue = flow;
    device.commandSetQueue.add(dataElement);
    logger.info("VentingOperations: " + mapping + " -> flow " + flow);
    }

    /* ===================== PRESSURE WAIT LOGIC ===================== */


/**
 * Wait until G2 >= target.
 * @return true if stop was requested / interrupted, false otherwise.
 */
private boolean waitForPressure(double target) {
    DataElement g2 = getG2DataElement();
    if (g2 == null) {
        logger.warning("VentingOperations: cannot wait for pressure, G2 DataElement not found.");
        return false;
    }

    long timeoutMs = 30L * 60L * 1000L; // 30 minutes
    long start = System.currentTimeMillis();

    logger.info("VentingOperations: waiting for G2 >= " + target);

    while (true) {
        // 1) stop / interrupt check
        if (stopRequested || Thread.currentThread().isInterrupted()) {
            logger.info("VentingOperations: stop requested while waiting for pressure.");
            return true;  // tell caller we were stopped
        }

        // 2) pressure check
        double current = g2.value;
        if (current >= target) {
            logger.info("VentingOperations: G2 reached " + current + " >= " + target);
            return false;
        }

        // 3) timeout
       /* if (System.currentTimeMillis() - start > timeoutMs) {
            logger.warning("VentingOperations: timeout waiting for G2 >= " + target +
                           " (last value=" + current + ")");
            return false;
        }*/

        // 4) sleep, but react to interrupt
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.info("VentingOperations: interrupted while sleeping in waitForPressure.");
            Thread.currentThread().interrupt();  // restore the flag
            return true;                          // treat as stop
        }
    }
}


    private DataElement getG2DataElement() {
        String[] parts = g2StatusKey.split("_", 2);
        if (parts.length < 2) return null;

        String deviceName = parts[0];
        String chName     = parts[1];

        Device device = deviceManager.getDevice(deviceName);
        if (device == null) {
            logger.warning("VentingOperations: G2 device '" + deviceName + "' not found");
            return null;
        }

        return device.getDataElement(chName);
    }

    private void setOperationStep(int step) {
        if (stepStatusKey == null || stepStatusKey.trim().isEmpty()) {
            return;
        }

        String[] parts = stepStatusKey.split("_", 2);
        if (parts.length < 2) {
            logger.warning("VentingOperations: bad STEP STATUS mapping for '" + stepStatusKey + "'");
            return;
        }

        String deviceName = parts[0];
        String dataName = parts[1];
        Device device = deviceManager.getDevice(deviceName);
        if (device == null) {
            logger.warning("VentingOperations: STEP device '" + deviceName + "' not found");
            return;
        }

        DataElement stepDataElement = device.getDataElement(dataName);
        if (stepDataElement == null) {
            logger.warning("VentingOperations: STEP dataElement '" + dataName + "' not found in device '" + deviceName + "'");
            return;
        }
        stepDataElement.value = step;
    }
}
