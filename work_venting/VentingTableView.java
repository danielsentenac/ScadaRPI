import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;


/**
 * Encapsulates the venting operations table, DB interactions, and
 * control buttons (add/remove/update/load/execute/stop).
 *
 * GlgGui now just embeds this panel on the left side.
 */
public class VentingTableView {

    private static final Logger logger = Logger.getLogger("Main");

    private final DeviceManager deviceManager;
    private final Hashtable<String,String> cmdMap;      // VENTINGCMD
    private final Hashtable<String,String> viewStatusMap;   // VENTINGVIEWSTATUS (non-GLG channels)

    private final String g2StatusKey;
    private final String mks2000StatusKey;
    private final String mks50000StatusKey;

    private String dbUrl;

    private final JPanel rootPanel;
    private final JTable operationsTable;
    private DefaultTableModel operationsModel;
    private final JPanel tableFrame;

    private final JButton addButton;
    private final JButton removeButton;
    private final JButton updateButton;
    private final JButton loadDbButton;
    private final JButton executeButton;
    private final JButton stopButton;

    private volatile boolean ventingRunning = false;
    private VentingOperations currentVentingOp;
    private Thread ventingThread;
    private volatile boolean stoppedByUser = false;
    private boolean dbEnabled = true;
    private boolean dbWarningLogged = false;
    
    private Timer framePulseTimer;
    private boolean framePulseOn = false;
    private Timer opCommandTimer;
    private boolean executeCommandArmed = false;
    private boolean stopCommandArmed = false;

    private boolean suppressModelEvents = false;
    
    



    private final JFrame ownerForDialogs; // used by DialogSetPoint, JOptionPane, etc.

    public VentingTableView(JFrame ownerForDialogs,
                            DeviceManager deviceManager,
                            Hashtable<String,String> cmdMap,
                            Hashtable<String,String> viewStatusMap,
                            String dbUrl,
                            String g2StatusKey,
                            String mks2000StatusKey,
                            String mks50000StatusKey) {

        this.ownerForDialogs = ownerForDialogs;
        this.deviceManager = deviceManager;
        this.cmdMap = cmdMap;
        this.viewStatusMap = viewStatusMap;
        this.dbUrl = dbUrl;
        this.g2StatusKey = g2StatusKey;
        this.mks2000StatusKey = mks2000StatusKey;
        this.mks50000StatusKey = mks50000StatusKey;

        // DB + model
        operationsModel = initializeModel();

        operationsTable = new JTable(operationsModel);
        VentingLookAndFeel.styleOperationsTable(operationsTable);
        VentingLookAndFeel.packTableColumns(operationsTable);
        VentingLookAndFeel.installEditors(operationsTable);
        VentingLookAndFeel.applyTableBackground(operationsTable); 
        applyScientificRendererToG2();
        
        attachModelListener();
        installNumericEditorHook();

        JScrollPane scrollPane = new JScrollPane(operationsTable);
        tableFrame = VentingLookAndFeel.createFramedPanel(scrollPane);
        tableFrame.setBackground(VentingLookAndFeel.TABLE_BACKGROUND);
        
        // Buttons
        addButton = new JButton("Add Step");
        removeButton = new JButton("Remove Step");
        updateButton = new JButton("Update Step");
        loadDbButton = new JButton("Load DB...");
        executeButton = new JButton("Execute");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);

        Dimension btnSize = new Dimension(90, 60);
        Insets btnMargin = new Insets(10, 10, 10, 10);

        configureButton(addButton, btnSize, btnMargin);
        configureButton(removeButton, btnSize, btnMargin);
        configureButton(updateButton, btnSize, btnMargin);
        configureButton(loadDbButton, new Dimension(110, 60), btnMargin);
        configureButton(executeButton, btnSize, btnMargin);
        configureButton(stopButton, btnSize, btnMargin);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 3, 0));
        buttonPanel.setBackground(VentingLookAndFeel.TABLE_BACKGROUND);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(loadDbButton);
        buttonPanel.add(executeButton);
        buttonPanel.add(stopButton);

        JLabel titleLabel = VentingLookAndFeel.createTitleLabel("VENTING SEQUENCE");
        titleLabel.setOpaque(true);
        titleLabel.setBackground(VentingLookAndFeel.TABLE_BACKGROUND);
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(titleLabel, BorderLayout.NORTH);
        leftPanel.add(tableFrame, BorderLayout.CENTER);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
        leftPanel.setBackground(VentingLookAndFeel.TABLE_BACKGROUND);
        leftPanel.setMinimumSize(new Dimension(50, 50));
        operationsTable.setMinimumSize(new Dimension(50, 50));

        rootPanel = leftPanel;

        // Wire actions
        addButton.addActionListener(e -> addRow());
        removeButton.addActionListener(e -> removeSelectedRow());
        updateButton.addActionListener(e -> updateSelectedRow());
        loadDbButton.addActionListener(e -> loadDbFromFile());
        executeButton.addActionListener(e -> startVentingSequence());
        stopButton.addActionListener(e -> stopVentingSequence());   

        startOperationCommandPolling();
        
    }

    private void applyScientificRendererToG2() {
	    // Column 9 is G2
	    operationsTable.getColumnModel()
		           .getColumn(9)
		           .setCellRenderer(new VentingLookAndFeel.ScientificRenderer());
    }

    /* ===================== PUBLIC API ===================== */

    public JPanel getPanel() {
        return rootPanel;
    }

    public JTable getTable() {
        return operationsTable;
    }

    public DefaultTableModel getModel() {
        return operationsModel;
    }

    /* ===================== TABLE / MODEL SETUP ===================== */

    private DefaultTableModel createEmptyModel() {
        Vector<String> cols = new Vector<>();
        cols.add("STEP");
        cols.add("P1");
        cols.add("V1");
        cols.add("VSOFT");
        cols.add("VMAIN");
        cols.add("VDRYER");
        cols.add("VRP");
        cols.add("MKS2000");
        cols.add("MKS50000");
        cols.add("G2");
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // STEP not editable
            }
        };
    }

    private Object[] defaultRow(int step) {
        return new Object[]{
                step,       // STEP
                "ON",       // P1
                "OPEN",     // V1
                "OPEN",     // VSOFT
                "CLOSE",    // VMAIN
                "CLOSE",    // VDRYER
                "CLOSE",    // VRP
                0,          // MKS2000
                0,          // MKS50000
                0.0         // G2
        };
    }

    private boolean hasDbDriver() {
        if (!dbEnabled) {
            return false;
        }
        try {
            DriverManager.getDriver(dbUrl);
            return true;
        } catch (SQLException e) {
            dbEnabled = false;
            if (!dbWarningLogged) {
                dbWarningLogged = true;
                logger.log(Level.WARNING,
                        "SQLite JDBC driver not found. Venting table will run without DB persistence: " + e.getMessage());
            }
            return false;
        }
    }

    private DefaultTableModel initializeModel() {
        if (hasDbDriver()) {
            createDbIfNeeded();
            insertDefaultRowIfNeeded();
            DefaultTableModel model = loadOperationsFromDb();
            if (model != null) {
                return model;
            }
        }

        DefaultTableModel model = createEmptyModel();
        model.addRow(defaultRow(1));
        return model;
    }

    private void installNumericEditorHook() {
        operationsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int row = operationsTable.rowAtPoint(e.getPoint());
                int col = operationsTable.columnAtPoint(e.getPoint());
                if (row < 0) return;

                // Column indices: 7=MKS2000, 8=MKS50000, 9=G2
                if (col == 7 || col == 8 || col == 9) {
                    editNumericCellWithKeypad(row, col);
                }
            }
        });
    }

    private void startFramePulse() {
	    if (framePulseTimer != null && framePulseTimer.isRunning()) {
		return; // already running
	    }

	    // start from strong phase
	    framePulseOn = true;
	    VentingLookAndFeel.setFrameRunningPhase(tableFrame, framePulseOn);

	    framePulseTimer = new Timer(400, e -> {
		framePulseOn = !framePulseOn;
		VentingLookAndFeel.setFrameRunningPhase(tableFrame, framePulseOn);
		tableFrame.repaint();
	    });
	    framePulseTimer.start();
    }

    private void stopFramePulseAndReset() {
	    if (framePulseTimer != null) {
		framePulseTimer.stop();
		framePulseTimer = null;
	    }
	    VentingLookAndFeel.setFrameIdle(tableFrame);
	    tableFrame.repaint();
    }

     private void attachModelListener() {
		operationsModel.addTableModelListener(new TableModelListener() {
		    @Override
		    public void tableChanged(TableModelEvent e) {
		        if (suppressModelEvents) {
		            return; // ignore internal bulk updates (like renumbering)
		        }
		        if (e.getType() == TableModelEvent.UPDATE) {
		            int row = e.getFirstRow();
		            if (row >= 0) {
		                updateRowInDb(row);
		            }
		        }
		    }
		});
    }

    /* ===================== DB LAYER ===================== */

    private void createDbIfNeeded() {
        if (!hasDbDriver()) {
            return;
        }

        String sql = "CREATE TABLE IF NOT EXISTS operations ("
                + " STEP INTEGER PRIMARY KEY,"
                + " P1 TEXT NOT NULL,"
                + " V1 TEXT NOT NULL,"
                + " VSOFT TEXT NOT NULL,"
                + " VMAIN TEXT NOT NULL,"
                + " VDRYER TEXT NOT NULL,"
                + " VRP TEXT NOT NULL,"
                + " MKS2000 INTEGER,"
                + " MKS50000 INTEGER,"
                + " G2 REAL"
                + ");";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "createDbIfNeeded> " + e.getMessage(), e);
        }
    }

    private void insertDefaultRowIfNeeded() {
        if (!hasDbDriver()) {
            return;
        }

        String sql = "INSERT INTO operations (STEP,P1,V1,VSOFT,VMAIN,VDRYER,VRP,MKS2000,MKS50000,G2) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, 1);
            pstmt.setString(2, "ON");
            pstmt.setString(3, "OPEN");
            pstmt.setString(4, "OPEN");
            pstmt.setString(5, "CLOSE");
            pstmt.setString(6, "CLOSE");
            pstmt.setString(7, "CLOSE");
            pstmt.setInt(8, 0);
            pstmt.setInt(9, 0);
            pstmt.setDouble(10, 0.0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // likely "constraint failed" after first run; not fatal
            logger.finer("insertDefaultRowIfNeeded> " + e.getMessage());
        }
    }

    private DefaultTableModel loadOperationsFromDb() {
        if (!hasDbDriver()) {
            return null;
        }

        DefaultTableModel model = null;
        String sql = "SELECT * FROM operations ORDER BY STEP";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            model = (DefaultTableModel) buildTableModel(columnCount, rs);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "loadOperationsFromDb> " + e.getMessage(), e);
        }
        return model;
    }

    public static TableModel buildTableModel(int columnCount, ResultSet resultSet)
            throws SQLException {

        Vector<String> columnNames = new Vector<>();
        Vector<Vector<Object>> dataVector = new Vector<>();

        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            columnNames.add(resultSet.getMetaData().getColumnName(columnIndex));
        }

        while (resultSet.next()) {
            Vector<Object> rowVector = new Vector<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                rowVector.add(resultSet.getObject(columnIndex));
            }
            dataVector.add(rowVector);
        }

        return new DefaultTableModel(dataVector, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // STEP read-only
            }
        };
    }

    private void insertRowInDb(Object[] row) {
        if (!hasDbDriver()) {
            return;
        }

        String sql = "INSERT INTO operations (STEP,P1,V1,VSOFT,VMAIN,VDRYER,VRP,MKS2000,MKS50000,G2) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ((Number) row[0]).intValue());
            pstmt.setString(2, (String) row[1]);
            pstmt.setString(3, (String) row[2]);
            pstmt.setString(4, (String) row[3]);
            pstmt.setString(5, (String) row[4]);
            pstmt.setString(6, (String) row[5]);
            pstmt.setString(7, (String) row[6]);
            pstmt.setInt(8, ((Number) row[7]).intValue());
            pstmt.setInt(9, ((Number) row[8]).intValue());
            pstmt.setDouble(10, ((Number) row[9]).doubleValue());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "insertRowInDb> " + e.getMessage(), e);
        }
    }

    private void updateRowInDb(int rowIndex) {
	    // Do nothing if we are already updating the model programmatically
	    if (suppressModelEvents) {
		return;
	    }
	    if (rowIndex < 0 || rowIndex >= operationsModel.getRowCount()) {
		return;
	    }

	    Object stepObj = operationsModel.getValueAt(rowIndex, 0);
	    if (!(stepObj instanceof Number)) {
		return;
	    }
	    int step = ((Number) stepObj).intValue();

	    String sql = "UPDATE operations SET "
		    + "P1=?, V1=?, VSOFT=?, VMAIN=?, VDRYER=?, VRP=?, "
		    + "MKS2000=?, MKS50000=?, G2=? "
		    + "WHERE STEP=?";

	    suppressModelEvents = true;
	    try {
		// These may adjust the values in the model (setValueAt)
		int mks2000  = getValidatedInt(rowIndex, 7, 0, 2000,  "MKS2000");
		int mks50000 = getValidatedInt(rowIndex, 8, 0, 50000, "MKS50000");
		double g2    = getValidatedDouble(rowIndex, 9, "G2");

                if (!hasDbDriver()) {
                    return;
                }

		try (Connection conn = DriverManager.getConnection(dbUrl);
		     PreparedStatement pstmt = conn.prepareStatement(sql)) {

		    pstmt.setString(1, (String) operationsModel.getValueAt(rowIndex, 1));
		    pstmt.setString(2, (String) operationsModel.getValueAt(rowIndex, 2));
		    pstmt.setString(3, (String) operationsModel.getValueAt(rowIndex, 3));
		    pstmt.setString(4, (String) operationsModel.getValueAt(rowIndex, 4));
		    pstmt.setString(5, (String) operationsModel.getValueAt(rowIndex, 5));
		    pstmt.setString(6, (String) operationsModel.getValueAt(rowIndex, 6));

		    pstmt.setInt(7, mks2000);
		    pstmt.setInt(8, mks50000);
		    pstmt.setDouble(9, g2);

		    pstmt.setInt(10, step);

		    pstmt.executeUpdate();
		}
	    } catch (SQLException e) {
		logger.log(Level.SEVERE, "updateRowInDb> " + e.getMessage(), e);
	    } finally {
		suppressModelEvents = false;
	    }
    }
    private void deleteRowFromDb(int step) {
        if (!hasDbDriver()) {
            return;
        }

        String sql = "DELETE FROM operations WHERE STEP=?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, step);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "deleteRowFromDb> " + e.getMessage(), e);
        }
    }
    
        /**
     * After removing a row, renumber STEP so that it runs from 1..N
     * in table order and update the DB accordingly.
     */
    private void renumberStepsAndSyncDb() {
        if (!hasDbDriver()) {
            suppressModelEvents = true;
            try {
                for (int row = 0; row < operationsModel.getRowCount(); row++) {
                    operationsModel.setValueAt(row + 1, row, 0);
                }
            } finally {
                suppressModelEvents = false;
            }
            operationsModel.fireTableDataChanged();
            return;
        }

        suppressModelEvents = true;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbUrl);
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt =
                         conn.prepareStatement("UPDATE operations SET STEP=? WHERE STEP=?")) {

                int rowCount = operationsModel.getRowCount();
                for (int row = 0; row < rowCount; row++) {
                    Object stepObj = operationsModel.getValueAt(row, 0);
                    if (!(stepObj instanceof Number)) {
                        continue;
                    }
                    int oldStep = ((Number) stepObj).intValue();
                    int newStep = row + 1; // we want 1..N

                    if (oldStep == newStep) {
                        continue; // no change needed
                    }

                    // Update DB
                    pstmt.setInt(1, newStep);
                    pstmt.setInt(2, oldStep);
                    pstmt.addBatch();

                    // Update model
                    operationsModel.setValueAt(newStep, row, 0);
                }

                pstmt.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollEx) {
                        logger.log(Level.SEVERE, "renumberStepsAndSyncDb rollback> " + rollEx.getMessage(), rollEx);
                    }
                }
                logger.log(Level.SEVERE, "renumberStepsAndSyncDb> " + ex.getMessage(), ex);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException ignore) {
                        // ignore
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "renumberStepsAndSyncDb> " + e.getMessage(), e);
        } finally {
            suppressModelEvents = false;
        }

        // Refresh view once after all internal changes
        operationsModel.fireTableDataChanged();
    }


    /* ===================== BUTTON HANDLERS ===================== */

    private void addRow() {
        int nextStep = findNextStep();
        Object[] newRow = new Object[]{
                nextStep,   // STEP
                "OFF",      // P1
                "CLOSE",    // V1
                "CLOSE",    // VSOFT
                "CLOSE",    // VMAIN
                "CLOSE",    // VDRYER
                "CLOSE",    // VRP
                0,          // MKS2000
                0,          // MKS50000
                0.0         // G2
        };

        insertRowInDb(newRow);
        operationsModel.addRow(newRow);
    }

    private int findNextStep() {
        int max = 0;
        for (int i = 0; i < operationsModel.getRowCount(); i++) {
            Object val = operationsModel.getValueAt(i, 0);
            if (val instanceof Number) {
                max = Math.max(max, ((Number) val).intValue());
            }
        }
        return max + 1;
    }

    private void removeSelectedRow() {
        int row = operationsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(
                    rootPanel,
                    "Select a row to remove.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Object stepObj = operationsModel.getValueAt(row, 0);
        if (stepObj instanceof Number) {
            deleteRowFromDb(((Number) stepObj).intValue());
        }

        // Remove from model
        operationsModel.removeRow(row);

        // Renumber STEP = 1..N and sync with DB
        renumberStepsAndSyncDb();
    }

    private void updateSelectedRow() {
        int row = operationsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(
                    rootPanel,
                    "Select a row to update.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (operationsTable.isEditing()) {
            operationsTable.getCellEditor().stopCellEditing();
        }

        updateRowInDb(row);
    }

    private void loadDbFromFile() {
        JFileChooser chooser;

        try {
            String path = dbUrl.replace("jdbc:sqlite:", "");
            File currentFile = new File(path);
            File parentDir = currentFile.getParentFile();

            if (parentDir != null && parentDir.exists()) {
                chooser = new JFileChooser(parentDir);
            } else {
                chooser = new JFileChooser();
            }

        } catch (Exception ex) {
            chooser = new JFileChooser();
        }

        FileNameExtensionFilter db3Filter =
                new FileNameExtensionFilter("SQLite DB3 files (*.db3)", "db3");
        chooser.setFileFilter(db3Filter);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setDialogTitle("Select venting DB file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = chooser.showOpenDialog(rootPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            dbUrl = "jdbc:sqlite:" + path;
            logger.info("Using DB: " + dbUrl);

            if (!hasDbDriver()) {
                JOptionPane.showMessageDialog(
                        rootPanel,
                        "SQLite JDBC driver is not available. Install sqlite-jdbc to enable DB load/save.",
                        "DB driver missing",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            DefaultTableModel newModel = loadOperationsFromDb();
            if (newModel != null) {
                operationsModel = newModel;
                operationsTable.setModel(operationsModel);
                VentingLookAndFeel.styleOperationsTable(operationsTable);
                VentingLookAndFeel.packTableColumns(operationsTable);
                VentingLookAndFeel.installEditors(operationsTable);
                VentingLookAndFeel.applyTableBackground(operationsTable); 
                applyScientificRendererToG2();
                attachModelListener();
            } else {
                JOptionPane.showMessageDialog(
                        rootPanel,
                        "Cannot load operations table from selected DB.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /* ===================== VALIDATION / EDITOR ===================== */

    private void editNumericCellWithKeypad(int row, int col) {
        Object val = operationsTable.getValueAt(row, col);
        double initial = 0.0;
        if (val instanceof Number) {
            initial = ((Number) val).doubleValue();
        } else if (val != null) {
            try {
                initial = Double.parseDouble(val.toString().trim());
            } catch (NumberFormatException ex) {
                initial = 0.0;
            }
        }

        String colName = operationsTable.getColumnName(col);

        DialogSetPoint dlg = new DialogSetPoint(
                ownerForDialogs,
                colName,
                initial,
                true
        );

        if (!dlg.isOk()) {
            return;
        }

        double newVal = dlg.getResultValue();

        if (col == 7) {         // MKS2000: 0..2000, integer
            int intVal = (int) Math.round(newVal);
            operationsModel.setValueAt(intVal, row, col);
            getValidatedInt(row, 7, 0, 2000, "MKS2000");
        } else if (col == 8) {  // MKS50000: 0..50000, integer
            int intVal = (int) Math.round(newVal);
            operationsModel.setValueAt(intVal, row, col);
            getValidatedInt(row, 8, 0, 50000, "MKS50000");
        } else if (col == 9) {  // G2: double, numeric (supports 3e-4)
            operationsModel.setValueAt(newVal, row, col);
            getValidatedDouble(row, 9, "G2");
        }

        updateRowInDb(row);
    }

    private int getValidatedInt(int row, int col, int min, int max, String name) {
        Object val = operationsModel.getValueAt(row, col);
        int v;

        try {
            if (val instanceof Number) {
                v = ((Number) val).intValue();
            } else {
                v = Integer.parseInt(val.toString().trim());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    rootPanel,
                    name + " must be an integer between " + min + " and " + max + ".",
                    "Invalid value",
                    JOptionPane.ERROR_MESSAGE
            );
            v = min;
        }

        if (v < min || v > max) {
            JOptionPane.showMessageDialog(
                    rootPanel,
                    name + " must be between " + min + " and " + max + ".",
                    "Invalid value",
                    JOptionPane.ERROR_MESSAGE
            );
            v = Math.min(Math.max(v, min), max);
        }

        operationsModel.setValueAt(v, row, col);
        return v;
    }

    private double getValidatedDouble(int row, int col, String name) {
        Object val = operationsModel.getValueAt(row, col);
        double v;

        try {
            if (val instanceof Number) {
                v = ((Number) val).doubleValue();
            } else {
                v = Double.parseDouble(val.toString().trim());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    rootPanel,
                    name + " must be numeric.",
                    "Invalid value",
                    JOptionPane.ERROR_MESSAGE
            );
            v = 0.0;
        }

        operationsModel.setValueAt(v, row, col);
        return v;
    }

    private static void configureButton(JButton btn, Dimension size, Insets margin) {
        btn.setPreferredSize(size);
        btn.setMargin(margin);
    }

    private void startOperationCommandPolling() {
        opCommandTimer = new Timer(250, e -> pollOperationCommands());
        opCommandTimer.start();
    }

    private void pollOperationCommands() {
        DataElement executeCmd = resolveDataElementFromMap(cmdMap.get("EXECUTE_CMD"));
        if (executeCmd != null) {
            boolean active = isCommandActive(executeCmd);
            if (active && !executeCommandArmed) {
                logger.info("VentingTableView: received OP_EXECUTE_CMD");
                startVentingSequence(false, false);
            }
            executeCommandArmed = active;
        } else {
            executeCommandArmed = false;
        }

        DataElement stopCmd = resolveDataElementFromMap(cmdMap.get("STOP_CMD"));
        if (stopCmd != null) {
            boolean active = isCommandActive(stopCmd);
            if (active && !stopCommandArmed) {
                logger.info("VentingTableView: received OP_STOP_CMD");
                stopVentingSequence(false, false);
            }
            stopCommandArmed = active;
        } else {
            stopCommandArmed = false;
        }
    }

    private DataElement resolveDataElementFromMap(String mapping) {
        if (mapping == null || mapping.trim().isEmpty()) {
            return null;
        }

        String[] parts = mapping.split("_", 2);
        if (parts.length < 2) {
            return null;
        }

        Device device = deviceManager.getDevice(parts[0]);
        if (device == null) {
            return null;
        }

        return device.getDataElement(parts[1]);
    }

    private boolean isCommandActive(DataElement command) {
        return ((int)command.value) != 0 || ((int)command.setvalue) != 0;
    }

    private void pulseOperationCommand(String commandKey) {
        DataElement command = resolveDataElementFromMap(cmdMap.get(commandKey));
        if (command == null) {
            return;
        }

        Device device = deviceManager.getDevice(command.deviceName);

        command.setvalue = 1;
        command.value = 1;

        if (device != null) {
            if (!device.commandSetQueue.contains(command)) {
                device.commandSetQueue.add(command);
            }
            if (device.holdingRegisters != null) {
                try {
                    device.holdingRegisters.setInt16At(command.mbRegisterOffset, 1);
                } catch (Exception ex) {
                    logger.finer("VentingTableView:pulseOperationCommand> unable to set holding register: " + ex.getMessage());
                }
            }
        }

        Timer fallbackReset = new Timer(2200, e -> {
            if (isCommandActive(command)) {
                command.value = 0;
                command.setvalue = 0;
                if (device != null && device.holdingRegisters != null) {
                    try {
                        device.holdingRegisters.setInt16At(command.mbRegisterOffset, 0);
                    } catch (Exception ex) {
                        logger.finer("VentingTableView:pulseOperationCommand> unable to clear holding register: " + ex.getMessage());
                    }
                }
            }
            ((Timer)e.getSource()).stop();
        });
        fallbackReset.setRepeats(false);
        fallbackReset.start();
    }

    /* ===================== VENTING SEQUENCE CONTROL ===================== */

    private void startVentingSequence() {
        startVentingSequence(true, true);
    }

    private void startVentingSequence(boolean showDialogs) {
        startVentingSequence(showDialogs, true);
    }

    private void startVentingSequence(boolean showDialogs, boolean emitCommandPulse) {
        if (ventingRunning) {
            if (showDialogs) {
                JOptionPane.showMessageDialog(
                        rootPanel,
                        "Venting sequence is already running.",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
            return;
        }

        if (operationsTable.isEditing()) {
            operationsTable.getCellEditor().stopCellEditing();
        }

        // new run, assume not stopped by user yet
        stoppedByUser = false;

        if (emitCommandPulse) {
            pulseOperationCommand("EXECUTE_CMD");
        }
        
        VentingOperations op = new VentingOperations(
                deviceManager,
                operationsTable.getModel(),
                g2StatusKey,
                mks2000StatusKey,
                mks50000StatusKey,
                viewStatusMap.get("STEP"),
                operationsTable
        );

	ventingRunning = true;
	currentVentingOp = op;

	// start pulsing orange frame
	startFramePulse();

	executeButton.setEnabled(false);
	stopButton.setEnabled(true);

	ventingThread = new Thread(() -> {
	    try {
		op.run();
	    } finally {
		ventingRunning = false;
		currentVentingOp = null;
		ventingThread = null;

		SwingUtilities.invokeLater(() -> {
		    stopFramePulseAndReset();                // <- stop pulse & go back idle
		    VentingLookAndFeel.clearActiveRow(operationsTable);

		    executeButton.setEnabled(true);
		    stopButton.setEnabled(false);

		    // Show completion dialog only if not stopped by user
		    if (!stoppedByUser) {
		        JOptionPane.showMessageDialog(
		                ownerForDialogs,
		                "Venting sequence has reached the end.",
		                "Sequence finished",
		                JOptionPane.INFORMATION_MESSAGE
		        );
		    }
                });
	    }
	}, "VentingOperationsThread");

	ventingThread.start();
    }

    private void stopVentingSequence() {
        stopVentingSequence(true, true);
    }

    private void stopVentingSequence(boolean askConfirmation) {
        stopVentingSequence(askConfirmation, true);
    }

    private void stopVentingSequence(boolean askConfirmation, boolean emitCommandPulse) {
        if (!ventingRunning || currentVentingOp == null) {
            return;
        }

        if (askConfirmation) {
            int res = JOptionPane.showConfirmDialog(
                    rootPanel,
                    "Stop venting sequence?",
                    "Confirm stop",
                    JOptionPane.YES_NO_OPTION
            );
            if (res != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if (emitCommandPulse) {
            pulseOperationCommand("STOP_CMD");
        }

        // remember this run was stopped manually by the user
        stoppedByUser = true;
        
        VentingLookAndFeel.clearActiveRow(operationsTable);
        
         // stop pulsing now
        if (framePulseTimer != null) {
            framePulseTimer.stop();
            framePulseTimer = null;
        }
    
        VentingLookAndFeel.setFrameStopped(tableFrame);

        currentVentingOp.requestStop();
        if (ventingThread != null) {
            ventingThread.interrupt();
        }
    }
}
