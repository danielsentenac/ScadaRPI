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
 *  STEP, P1, V1, VBYPASS, VSOFT, VMAIN, VDRYER, VRP, MKS2000, MKS50000, G2
 *
 * For each step (in increasing STEP order):
 *   1) Send each valve / pump command to the PLC
 *   2) Read PLC feedback and wait until the requested state is reported
 *   3) Set MKS2000 / MKS50000 after VRP and wait for MKS setpoint feedback
 *   4) Wait until G2 (chamber pressure) reaches target from that row
 */
public class VentingOperations implements Runnable {

    private static final Logger logger = Logger.getLogger("Main");
    private static final long WAIT_POLL_MS = 200L;
    private static final long WAIT_LOG_INTERVAL_MS = 5000L;
    private static final int P1_COLUMN_INDEX = 1;
    private static final int V1_COLUMN_INDEX = 2;
    private static final int VBYPASS_COLUMN_INDEX = 3;
    private static final int VSOFT_COLUMN_INDEX = 4;
    private static final int VMAIN_COLUMN_INDEX = 5;
    private static final int VDRYER_COLUMN_INDEX = 6;
    private static final int VRP_COLUMN_INDEX = 7;
    private static final int MKS2000_COLUMN_INDEX = 8;
    private static final int MKS50000_COLUMN_INDEX = 9;
    private static final int G2_COLUMN_INDEX = 10;
    private static final double MKS_SETPOINT_TOLERANCE = 1.0e-3;

    private final DeviceManager deviceManager;
    private final List<Step> steps;
    private final String g2StatusKey;                  // e.g. "G2Val"
    private final String mks2000StatusKey;   // e.g. "MKS2000_FLOW_SETP"
    private final String mks50000StatusKey;  // e.g. "MKS50000_FLOW_SETP"
    private final String stepStatusKey;      // e.g. "OP_STEP"
    private final JTable table;
    private volatile boolean stopRequested = false;
    
    
    private static class Step {
        int rowIndex;       // index in the JTable model
        int step;
        String p1;          // "ON" / "OFF"
        String v1;          // "OPEN" / "CLOSE"
        String vbypass;
        String vsoft;
        String vmain;
        String vdryer;
        String vrp;
        int mks2000;        // 0..2000
        int mks50000;       // 0..50000
        double g2Target;    // pressure to reach before next step
    }

    private static class IssuedCommand {
        final Device device;
        final DataElement commandElement;
        final String commandKey;
        final String requestedState;
        final String statusKey;
        final DataElement statusElement;
        final int expectedStatusValue;
        final String expectedStatusLabel;
        final int tableColumnIndex;

        IssuedCommand(Device device,
                      DataElement commandElement,
                      String commandKey,
                      String requestedState,
                      String statusKey,
                      DataElement statusElement,
                      int expectedStatusValue,
                      String expectedStatusLabel,
                      int tableColumnIndex) {
            this.device = device;
            this.commandElement = commandElement;
            this.commandKey = commandKey;
            this.requestedState = requestedState;
            this.statusKey = statusKey;
            this.statusElement = statusElement;
            this.expectedStatusValue = expectedStatusValue;
            this.expectedStatusLabel = expectedStatusLabel;
            this.tableColumnIndex = tableColumnIndex;
        }
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
        if (steps.isEmpty()) {
            logger.warning("VentingOperations: no steps available, nothing to execute.");
            setOperationStep(0);
            return;
        }

	    int startIndex = determineStartIndexByPressure();
	    logger.info("VentingOperations: starting sequence with " + steps.size() + " steps from index "
	            + startIndex + " (step " + steps.get(startIndex).step + ").");
            setOperationStep(0);

	    for (int i = startIndex; i < steps.size(); i++) {
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

		// 1) Execute P1 first so the step follows table order. This still
		//    guarantees the ON interlock before any valve opening.
		boolean stopped = false;
		if ("ON".equalsIgnoreCase(step.p1) || "OFF".equalsIgnoreCase(step.p1)) {
		    stopped = executeIssuedCommand(step.step, step.rowIndex, queuePumpCommand("M1_P1ONOFF", step.p1));
		}

		// 2) Send valve commands sequentially, waiting for PLC feedback after
		//    each command before issuing the next one.
		if (!stopped) stopped = executeIssuedCommand(step.step, step.rowIndex, queueValveCommand("M1_V1CMD", step.v1));
		if (!stopped) stopped = executeVentPathCommands(step);
		if (!stopped) stopped = executeIssuedCommand(step.step, step.rowIndex, queueValveCommand("M1_VDRYERCMD", step.vdryer));
		if (!stopped) stopped = executeIssuedCommand(step.step, step.rowIndex, queueValveCommand("M1_VRPCMD", step.vrp));

		// 3) Send MKS setpoints only after the PLC path is in place, then wait
		//    for the MKS setpoint readback before continuing.
		if (!stopped) stopped = executeMksCommands(step);

		if (stopped) {
		    logger.info("VentingOperations: stopped while waiting for PLC feedback.");
		    break;
		}

		// 5) Wait for pressure *only if there is a next step*
		if (!isLastStep && step.g2Target > 0) {
		    stopped = waitForPressure(step.rowIndex, step.g2Target);
		    if (stopped) {
		        logger.info("VentingOperations: stopped while waiting for pressure.");
		        break;
		    }
		}
	    }

            clearFeedbackCell();
            setOperationStep(0);
	    logger.info("VentingOperations: sequence finished.");
    }

    private int determineStartIndexByPressure() {
        if (steps.isEmpty()) {
            return 0;
        }

        DataElement g2 = getG2DataElement();
        if (g2 == null) {
            logger.warning("VentingOperations: G2 not available, starting from step 1.");
            return 0;
        }

        double currentG2 = g2.value;
        int bestIndex = 0;
        double bestTarget = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            if (step.g2Target <= currentG2 && step.g2Target >= bestTarget) {
                bestIndex = i;
                bestTarget = step.g2Target;
            }
        }

        Step bestStep = steps.get(bestIndex);
        logger.info("VentingOperations: current G2=" + currentG2
                + ", resuming from step " + bestStep.step
                + " with G2 target " + bestStep.g2Target);
        return bestIndex;
    }

    private void highlightRow(int rowIndex) {
	    if (table == null || rowIndex < 0) {
		return;
	    }

	    SwingUtilities.invokeLater(() -> {
		VentingLookAndFeel.clearActiveCell(table);
		VentingLookAndFeel.setActiveRow(table, rowIndex);

		table.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
		table.setColumnSelectionInterval(0, table.getColumnCount() - 1);

		Rectangle rect = table.getCellRect(rowIndex, 0, true);
		table.scrollRectToVisible(rect);
	    });
    }

    private void highlightFeedbackCell(int rowIndex, int columnIndex) {
	    if (table == null || rowIndex < 0 || columnIndex < 0) {
		return;
	    }

	    SwingUtilities.invokeLater(() -> {
		VentingLookAndFeel.setActiveCell(table, rowIndex, columnIndex);
		Rectangle rect = table.getCellRect(rowIndex, columnIndex, true);
		table.scrollRectToVisible(rect);
	    });
    }

    private void clearFeedbackCell() {
	    if (table == null) {
		return;
	    }
	    SwingUtilities.invokeLater(() -> VentingLookAndFeel.clearActiveCell(table));
    }

    private boolean executeVentPathCommands(Step step) {
        String requestedVBypass = normalizeValveState(step.vbypass);
        String requestedVSoft = normalizeValveState(step.vsoft);
        String requestedVMain = normalizeValveState(step.vmain);

        if (isOpenState(requestedVMain) && !isOpenState(requestedVSoft)) {
            logger.info("VentingOperations: step " + step.step
                    + " promotes VSOFT to OPEN because VMAIN requires the soft path to be open.");
            requestedVSoft = "OPEN";
        }
        if ((isOpenState(requestedVSoft) || isOpenState(requestedVMain)) && !isOpenState(requestedVBypass)) {
            logger.info("VentingOperations: step " + step.step
                    + " promotes VBYPASS to OPEN because the vent path requires it before VSOFT/VMAIN.");
            requestedVBypass = "OPEN";
        }

        int targetVentStatus = getTargetVentStatusValue(requestedVSoft, requestedVMain);
        String targetVentLabel = getTargetVentStatusLabel(targetVentStatus);
        int currentVentStatus = getCurrentStatusValue("M1_VENTST", 2);

        logger.info("VentingOperations: step " + step.step + " vent path target -> VBYPASS="
                + requestedVBypass + ", VSOFT=" + requestedVSoft + ", VMAIN=" + requestedVMain
                + " (VENTST=" + targetVentLabel + ")");

        if (targetVentStatus != 2) {
            if (executeIssuedCommand(step.step, step.rowIndex, queueValveCommand("M1_VBYPASSCMD", "OPEN"))) {
                return true;
            }
        }

        if (targetVentStatus == 1) {
            if (currentVentStatus == 2) {
                if (executeIssuedCommand(step.step, step.rowIndex, queueVentCommand("M1_VSOFTCMD", "OPEN", 0, "MOVING"))) {
                    return true;
                }
                currentVentStatus = 0;
            }
            if (currentVentStatus == 0) {
                return executeIssuedCommand(step.step, step.rowIndex, queueVentCommand("M1_VMAINCMD", "OPEN", 1, "OPEN"));
            }
            return false;
        }

        if (targetVentStatus == 0) {
            if (currentVentStatus == 1) {
                if (executeIssuedCommand(step.step, step.rowIndex, queueVentCommand("M1_VMAINCMD", "CLOSE", 0, "MOVING"))) {
                    return true;
                }
                currentVentStatus = 0;
            }
            if (currentVentStatus == 2) {
                return executeIssuedCommand(step.step, step.rowIndex, queueVentCommand("M1_VSOFTCMD", "OPEN", 0, "MOVING"));
            }
            return false;
        }

        if (currentVentStatus == 1) {
            if (executeIssuedCommand(step.step, step.rowIndex, queueVentCommand("M1_VMAINCMD", "CLOSE", 0, "MOVING"))) {
                return true;
            }
            currentVentStatus = 0;
        }
        if (currentVentStatus == 0) {
            if (executeIssuedCommand(step.step, step.rowIndex, queueVentCommand("M1_VSOFTCMD", "CLOSE", 2, "CLOSE"))) {
                return true;
            }
        }

        if (isOpenState(requestedVBypass)) {
            return executeIssuedCommand(step.step, step.rowIndex, queueValveCommand("M1_VBYPASSCMD", "OPEN"));
        }
        return executeIssuedCommand(step.step, step.rowIndex, queueValveCommand("M1_VBYPASSCMD", "CLOSE"));
    }

    private IssuedCommand queueVentCommand(String cmdName, String state, int expectedVentStatusValue, String expectedVentStatusLabel) {
        return queueValveCommand(cmdName, state, "M1_VENTST", expectedVentStatusValue, expectedVentStatusLabel);
    }

    private int getCurrentStatusValue(String statusKey, int fallbackValue) {
        DataElement statusElement = getDataElementByKey(statusKey);
        if (statusElement == null) {
            logger.warning("VentingOperations: missing status element '" + statusKey + "', assuming " + fallbackValue);
            return fallbackValue;
        }

        int currentValue = (int) statusElement.value;
        if (currentValue == 0 || currentValue == 1 || currentValue == 2) {
            return currentValue;
        }

        logger.warning("VentingOperations: unexpected status value " + currentValue + " for '"
                + statusKey + "', assuming " + fallbackValue);
        return fallbackValue;
    }

    private String normalizeValveState(String state) {
        if (state == null) {
            return "CLOSE";
        }
        if ("OPEN".equalsIgnoreCase(state.trim())) {
            return "OPEN";
        }
        if ("CLOSE".equalsIgnoreCase(state.trim())) {
            return "CLOSE";
        }

        logger.warning("VentingOperations: unexpected valve state '" + state + "', defaulting to CLOSE.");
        return "CLOSE";
    }

    private boolean isOpenState(String state) {
        return "OPEN".equalsIgnoreCase(state);
    }

    private int getTargetVentStatusValue(String requestedVSoft, String requestedVMain) {
        if (isOpenState(requestedVMain)) {
            return 1;
        }
        if (isOpenState(requestedVSoft)) {
            return 0;
        }
        return 2;
    }

    private String getTargetVentStatusLabel(int statusValue) {
        if (statusValue == 1) return "OPEN";
        if (statusValue == 0) return "MOVING";
        return "CLOSE";
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
                s.vbypass   = toString(model.getValueAt(r, 3));
                s.vsoft     = toString(model.getValueAt(r, 4));
                s.vmain     = toString(model.getValueAt(r, 5));
                s.vdryer    = toString(model.getValueAt(r, 6));
                s.vrp       = toString(model.getValueAt(r, 7));
                s.mks2000   = toInt(model.getValueAt(r, 8), 0);
                s.mks50000  = toInt(model.getValueAt(r, 9), 0);
                s.g2Target  = toDouble(model.getValueAt(r, 10), 0.0);
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

    private boolean executeIssuedCommand(int stepNumber, int rowIndex, IssuedCommand issuedCommand) {
        if (issuedCommand == null) {
            return false;
        }
        highlightFeedbackCell(rowIndex, issuedCommand.tableColumnIndex);
        if (waitForCommandExecution(stepNumber, issuedCommand)) {
            return true;
        }
        if (issuedCommand.statusKey != null && issuedCommand.statusElement != null) {
            return waitForStatusMatch(stepNumber, issuedCommand);
        }
        return false;
    }

    private IssuedCommand queuePumpCommand(String cmdName, String state) {
        if (state == null || state.isEmpty()) return null;

        String[] parts = cmdName.split("_", 2);
        if (parts.length < 2) return null;

        String deviceName = parts[0];
        String dataName   = parts[1];

        Device device = deviceManager.getDevice(deviceName);
        if (device == null) {
            logger.warning("VentingOperations: pump device '" + deviceName + "' not found");
            return null;
        }

        DataElement de = device.getDataElement(dataName);
        if (de == null) {
            logger.warning("VentingOperations: pump dataElement '" + dataName + "' not found in device '" + deviceName + "'");
            return null;
        }

        int expectedStatusValue;
        if (state.equalsIgnoreCase("ON")) {
            de.setvalue = 1;
            expectedStatusValue = 1;
        } else if (state.equalsIgnoreCase("OFF")) {
            de.setvalue = 2;
            expectedStatusValue = 0;
        } else {
            logger.warning("VentingOperations: unknown P1 state '" + state + "', expected ON/OFF");
            return null;
        }
        device.commandSetQueue.add(de);
        logger.info("VentingOperations: pump " + cmdName + " -> " + state);
        return buildIssuedCommand(device, de, cmdName, state, getStatusKeyForCommand(cmdName), expectedStatusValue, state.toUpperCase(Locale.ROOT));
    }

    private IssuedCommand queueValveCommand(String cmdName, String state) {
        return queueValveCommand(cmdName, state, null, null, null);
    }

    private IssuedCommand queueValveCommand(String cmdName,
                                            String state,
                                            String statusKeyOverride,
                                            Integer expectedStatusValueOverride,
                                            String expectedStatusLabelOverride) {
        if (state == null || state.isEmpty()) return null;

        String[] parts = cmdName.split("_", 2);
        if (parts.length < 2) return null;

        String deviceName = parts[0];
        String dataName   = parts[1];

        Device device = deviceManager.getDevice(deviceName);
        if (device == null) {
            logger.warning("VentingOperations: valve device '" + deviceName + "' not found");
            return null;
        }

        DataElement de = device.getDataElement(dataName);
        if (de == null) {
            logger.warning("VentingOperations: valve dataElement '" + dataName + "' not found in device '" + deviceName + "'");
            return null;
        }

        int expectedStatusValue;
        if (state.equalsIgnoreCase("OPEN")) {
            de.setvalue = 1;
            expectedStatusValue = 1;
        } else if (state.equalsIgnoreCase("CLOSE")) {
            de.setvalue = 2;
            expectedStatusValue = 2;
        } else {
            logger.warning("VentingOperations: unknown valve state '" + state + "', expected OPEN/CLOSE");
            return null;
        }
        device.commandSetQueue.add(de);
        logger.info("VentingOperations: valve " + cmdName + " -> " + state);

        String statusKey = statusKeyOverride != null ? statusKeyOverride : getStatusKeyForCommand(cmdName);
        int feedbackValue = expectedStatusValueOverride != null ? expectedStatusValueOverride : expectedStatusValue;
        String feedbackLabel = expectedStatusLabelOverride != null
                ? expectedStatusLabelOverride
                : state.toUpperCase(Locale.ROOT);

        return buildIssuedCommand(device, de, cmdName, state, statusKey, feedbackValue, feedbackLabel);
    }

    private IssuedCommand buildIssuedCommand(Device device,
                                             DataElement commandElement,
                                             String commandKey,
                                             String requestedState,
                                             String statusKey,
                                             int expectedStatusValue,
                                             String expectedStatusLabel) {
        DataElement statusElement = getDataElementByKey(statusKey);
        if (statusKey == null || statusElement == null) {
            if (commandKey != null && commandKey.endsWith("VSOFTCMD")) {
                logger.info("VentingOperations: VSOFT has no dedicated PLC status feedback; waiting only for command completion.");
            } else {
                logger.warning("VentingOperations: no PLC status feedback mapped for '" + commandKey
                        + "', the sequence will wait only for the command pulse to complete.");
            }
        }
        return new IssuedCommand(
                device,
                commandElement,
                commandKey,
                requestedState,
                statusKey,
                statusElement,
                expectedStatusValue,
                expectedStatusLabel,
                getColumnIndexForCommand(commandKey)
        );
    }

    private boolean waitForCommandExecution(int stepNumber, IssuedCommand issuedCommand) {
        boolean seenPending = false;
        long nextLogAt = System.currentTimeMillis() + WAIT_LOG_INTERVAL_MS;

        logger.info("VentingOperations: step " + stepNumber + " waiting for command "
                + issuedCommand.commandKey + " -> " + issuedCommand.requestedState);

        while (true) {
            if (isStopRequested()) {
                return true;
            }

            boolean queued = issuedCommand.device.commandSetQueue.contains(issuedCommand.commandElement);
            boolean active = issuedCommand.commandElement.type == DataTypes.DataType.TRIGGER
                    && issuedCommand.commandElement.value != 0.0;
            if (queued || active) {
                seenPending = true;
            }
            if (seenPending && !queued && !active) {
                return false;
            }

            long now = System.currentTimeMillis();
            if (now >= nextLogAt) {
                logger.info("VentingOperations: still waiting for command "
                        + issuedCommand.commandKey + " -> " + issuedCommand.requestedState
                        + " (queued=" + queued + ", active=" + active + ")");
                nextLogAt = now + WAIT_LOG_INTERVAL_MS;
            }

            if (sleepForWaitPoll()) {
                return true;
            }
        }
    }

    private boolean waitForStatusMatch(int stepNumber, IssuedCommand issuedCommand) {
        long nextLogAt = System.currentTimeMillis() + WAIT_LOG_INTERVAL_MS;

        logger.info("VentingOperations: step " + stepNumber + " waiting for status "
                + issuedCommand.statusKey + " -> " + issuedCommand.expectedStatusLabel);

        while (true) {
            if (isStopRequested()) {
                return true;
            }

            int currentValue = (int) issuedCommand.statusElement.value;
            if (currentValue == issuedCommand.expectedStatusValue) {
                logger.info("VentingOperations: status " + issuedCommand.statusKey + " reached "
                        + issuedCommand.expectedStatusLabel + " (" + currentValue + ")");
                return false;
            }

            long now = System.currentTimeMillis();
            if (now >= nextLogAt) {
                logger.info("VentingOperations: still waiting for status "
                        + issuedCommand.statusKey + " -> " + issuedCommand.expectedStatusLabel
                        + " (current=" + formatObservedState(issuedCommand, currentValue) + ")");
                nextLogAt = now + WAIT_LOG_INTERVAL_MS;
            }

            if (sleepForWaitPoll()) {
                return true;
            }
        }
    }

    private boolean sleepForWaitPoll() {
        try {
            Thread.sleep(WAIT_POLL_MS);
            return false;
        } catch (InterruptedException e) {
            logger.info("VentingOperations: interrupted while waiting for PLC feedback.");
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private boolean isStopRequested() {
        return stopRequested || Thread.currentThread().isInterrupted();
    }

    private String getStatusKeyForCommand(String commandKey) {
        if (commandKey == null || commandKey.isEmpty()) {
            return null;
        }
        if (commandKey.endsWith("VBYPASSCMD")) return "M1_VBYPASSST";
        if (commandKey.endsWith("VRPCMD")) return "M1_VRPST";
        if (commandKey.endsWith("VDRYERCMD")) return "M1_VDRYERST";
        if (commandKey.endsWith("V1CMD")) return "M1_V1ST";
        if (commandKey.endsWith("VSOFTCMD")) return null;
        if (commandKey.endsWith("VMAINCMD")) return "M1_VENTST";
        if (commandKey.endsWith("P1ONOFF")) return "M1_P1ST";
        return null;
    }

    private int getColumnIndexForCommand(String commandKey) {
        if (commandKey == null || commandKey.isEmpty()) {
            return -1;
        }
        if (commandKey.endsWith("P1ONOFF")) return P1_COLUMN_INDEX;
        if (commandKey.endsWith("V1CMD")) return V1_COLUMN_INDEX;
        if (commandKey.endsWith("VBYPASSCMD")) return VBYPASS_COLUMN_INDEX;
        if (commandKey.endsWith("VSOFTCMD")) return VSOFT_COLUMN_INDEX;
        if (commandKey.endsWith("VMAINCMD")) return VMAIN_COLUMN_INDEX;
        if (commandKey.endsWith("VDRYERCMD")) return VDRYER_COLUMN_INDEX;
        if (commandKey.endsWith("VRPCMD")) return VRP_COLUMN_INDEX;
        return -1;
    }

    private DataElement getDataElementByKey(String dataKey) {
        if (dataKey == null || dataKey.trim().isEmpty()) {
            return null;
        }

        String[] parts = dataKey.split("_", 2);
        if (parts.length < 2) {
            logger.warning("VentingOperations: bad data mapping for '" + dataKey + "'");
            return null;
        }

        Device device = deviceManager.getDevice(parts[0]);
        if (device == null) {
            logger.warning("VentingOperations: device '" + parts[0] + "' not found for '" + dataKey + "'");
            return null;
        }
        return device.getDataElement(parts[1]);
    }

    private String formatObservedState(IssuedCommand issuedCommand, int currentValue) {
        if ("ON".equalsIgnoreCase(issuedCommand.requestedState) || "OFF".equalsIgnoreCase(issuedCommand.requestedState)) {
            if (currentValue == 1) return "ON";
            if (currentValue == 0) return "OFF";
            if (currentValue == 255) return "ERROR";
            return Integer.toString(currentValue);
        }

        if (currentValue == 1) return "OPEN";
        if (currentValue == 2) return "CLOSE";
        if (currentValue == 0) return "MOVING";
        if (currentValue == 255) return "ERROR";
        return Integer.toString(currentValue);
    }

    private boolean executeMksCommands(Step step) {
        if (mks2000StatusKey != null && step.mks2000 > 0) {
            if (executeMksCommand(step.step, step.rowIndex, mks2000StatusKey, (double) step.mks2000, MKS2000_COLUMN_INDEX)) {
                return true;
            }
        }
        if (mks50000StatusKey != null && step.mks50000 > 0) {
            if (executeMksCommand(step.step, step.rowIndex, mks50000StatusKey, (double) step.mks50000, MKS50000_COLUMN_INDEX)) {
                return true;
            }
        }
        return false;
    }

    private boolean executeMksCommand(int stepNumber,
                                      int rowIndex,
                                      String flowKey,
                                      double requestedFlow,
                                      int tableColumnIndex) {
        IssuedCommand issuedCommand = queueMksFlowCommand(flowKey, requestedFlow, tableColumnIndex);
        if (issuedCommand == null) {
            return false;
        }

        highlightFeedbackCell(rowIndex, tableColumnIndex);
        if (waitForCommandExecution(stepNumber, issuedCommand)) {
            return true;
        }
        return waitForMksSetpoint(stepNumber, rowIndex, flowKey, requestedFlow, tableColumnIndex);
    }

    private IssuedCommand queueMksFlowCommand(String flowKey, double requestedFlow, int tableColumnIndex) {
        if (flowKey == null || flowKey.trim().isEmpty()) {
            return null;
        }

        String[] parts = flowKey.split("_", 2);
        if (parts.length < 2) {
            logger.warning("VentingOperations: bad MKS flow mapping for '" + flowKey + "'");
            return null;
        }

        String deviceName = parts[0];
        Device device = deviceManager.getDevice(deviceName);
        if (device == null) {
            logger.warning("VentingOperations: MKS device '" + deviceName + "' not found");
            return null;
        }

        DataElement setpointElement = device.getDataElement("FLOW_SETP");
        if (setpointElement == null) {
            logger.warning("VentingOperations: FLOW_SETP not found in device '" + deviceName + "'");
            return null;
        }

        setpointElement.setvalue = requestedFlow;
        device.commandSetQueue.add(setpointElement);
        logger.info("VentingOperations: " + deviceName + "_FLOW_SETP -> flow " + requestedFlow);

        return new IssuedCommand(
                device,
                setpointElement,
                deviceName + "_FLOW_SETP",
                Double.toString(requestedFlow),
                null,
                null,
                0,
                null,
                tableColumnIndex
        );
    }

    private boolean waitForMksSetpoint(int stepNumber,
                                   int rowIndex,
                                   String flowKey,
                                   double requestedFlow,
                                   int tableColumnIndex) {
        DataElement setpointElement = getDataElementByKey(flowKey);
        if (setpointElement == null) {
            logger.warning("VentingOperations: missing MKS setpoint feedback for '" + flowKey + "'");
            return false;
        }

        long nextLogAt = System.currentTimeMillis() + WAIT_LOG_INTERVAL_MS;

        highlightFeedbackCell(rowIndex, tableColumnIndex);
        logger.info("VentingOperations: step " + stepNumber + " waiting for MKS setpoint "
                + flowKey + " -> " + requestedFlow + " sccm");

        while (true) {
            if (isStopRequested()) {
                return true;
            }

            double currentSetpoint = setpointElement.value;
            if (Math.abs(currentSetpoint - requestedFlow) <= MKS_SETPOINT_TOLERANCE) {
                logger.info("VentingOperations: MKS setpoint " + flowKey + " reached "
                        + currentSetpoint + " sccm for target " + requestedFlow);
                return false;
            }

            long now = System.currentTimeMillis();
            if (now >= nextLogAt) {
                logger.info("VentingOperations: still waiting for MKS setpoint "
                        + flowKey + " -> " + requestedFlow
                        + " sccm (current=" + currentSetpoint + ")");
                nextLogAt = now + WAIT_LOG_INTERVAL_MS;
            }

            if (sleepForWaitPoll()) {
                return true;
            }
        }
    }

    /* ===================== PRESSURE WAIT LOGIC ===================== */


/**
 * Wait until G2 >= target.
 * @return true if stop was requested / interrupted, false otherwise.
 */
private boolean waitForPressure(int rowIndex, double target) {
    DataElement g2 = getG2DataElement();
    if (g2 == null) {
        logger.warning("VentingOperations: cannot wait for pressure, G2 DataElement not found.");
        return false;
    }

    long timeoutMs = 30L * 60L * 1000L; // 30 minutes
    long start = System.currentTimeMillis();

    highlightFeedbackCell(rowIndex, G2_COLUMN_INDEX);

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
