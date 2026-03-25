
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

/**
 * Centralizes all cosmetic / look & feel aspects of the venting table:
 * fonts, borders, highlight, combo editor, frame colors, etc.
 */
public final class VentingLookAndFeel {

    private static final Color ACTIVE_ROW_COLOR = new Color(255, 222, 173);
    private static final Color ACTIVE_CELL_COLOR = new Color(255, 150, 0);
    private static final String ACTIVE_ROW_PROPERTY = "activeStepRow";
    private static final String ACTIVE_CELL_ROW_PROPERTY = "activeStepCellRow";
    private static final String ACTIVE_CELL_COLUMN_PROPERTY = "activeStepCellColumn";
    public static final Color TABLE_BACKGROUND = new Color(140, 198, 198);
    public static final Color RUNNING_ORANGE_STRONG = new Color(255, 150, 0);
    public static final Color RUNNING_ORANGE_SOFT   = new Color(255, 200, 120);

    private VentingLookAndFeel() {
        // utility class
    }

    /* ===================== TABLE STYLING ===================== */

    public static void styleOperationsTable(JTable table) {
        // Touch friendly base font / row height
        table.setFont(new Font("SansSerif", Font.BOLD, 10));
        table.setRowHeight(45);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 10));
        header.setPreferredSize(
                new Dimension(header.getPreferredSize().width, 30)
        );

        // Base cell padding
        DefaultTableCellRenderer rendererAll =
                (DefaultTableCellRenderer) table.getDefaultRenderer(Object.class);
        rendererAll.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Apply highlight renderer to all columns
        TableCellRenderer highlightRenderer = new HighlightRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setCellRenderer(highlightRenderer);
        }
    }

    /**
     * Simple column auto-pack (for initial layout).
     */
    public static void packTableColumns(JTable table) {
        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumn column = table.getColumnModel().getColumn(col);
            int width = 50; // minimum

            TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                    table, column.getHeaderValue(), false, false, 0, col);
            width = Math.max(width, headerComp.getPreferredSize().width + 10);

            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, col);
                Component comp = renderer.getTableCellRendererComponent(
                        table, table.getValueAt(row, col), false, false, row, col);
                width = Math.max(width, comp.getPreferredSize().width + 10);
            }

            column.setPreferredWidth(width);
        }
    }

    /**
     * Install touch-friendly combo editors for pump/valves.
     */
    public static void installEditors(JTable table) {
        // Column indices:
        // 0: STEP
        // 1: P1 (ON/OFF)
        // 2: V1
        // 3: VBYPASS
        // 4: VSOFT
        // 5: VMAIN
        // 6: VDRYER
        // 7: VRP
        // 8: MKS2000 (numeric, handled elsewhere)
        // 9: MKS50000
        // 10: G2

        // P1: ON/OFF
        table.getColumnModel().getColumn(1)
                .setCellEditor(new TouchComboEditor(new String[]{"ON", "OFF"}));

        // Valves: OPEN/CLOSE (V1, VBYPASS, VSOFT, VMAIN, VDRYER, VRP)
        String[] valveItems = {"OPEN", "CLOSE"};
        int[] valveCols = {2, 3, 4, 5, 6, 7};
        for (int col : valveCols) {
            table.getColumnModel().getColumn(col)
                    .setCellEditor(new TouchComboEditor(valveItems));
        }
    }

    /* ===================== FRAME / PANEL DECORATION ===================== */

    public static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 20));
        label.setForeground(new Color(50,50,50));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedSoftBevelBorder(),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        return label;
    }

    public static JPanel createFramedPanel(JComponent inner) {
        JPanel frame = new JPanel(new BorderLayout());
        setFrameIdle(frame);
        frame.add(inner, BorderLayout.CENTER);
        return frame;
    }

    public static void setFrameIdle(JPanel frame) {
        if (frame == null) return;
        frame.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedSoftBevelBorder(),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
    }

    /**
    * Used by the pulse timer to alternate between strong and soft states.
    */
    public static void setFrameRunningPhase(JPanel frame, boolean strongPhase) {
	    if (frame == null) return;

	    Color c = strongPhase ? RUNNING_ORANGE_STRONG : RUNNING_ORANGE_SOFT;
	    int thickness = strongPhase ? 12 : 12;

	    frame.setBorder(
		BorderFactory.createCompoundBorder(
		    BorderFactory.createLineBorder(c, thickness),
		    BorderFactory.createEmptyBorder(12, 12, 12, 12)
		)
	    );
    }

    public static void setFrameRunning(JPanel frame) {
       // default: strong phase
       setFrameRunningPhase(frame, true);
    }

    public static void setFrameStopped(JPanel frame) {
        // You can make this red or just same as idle; currently same as idle.
        setFrameIdle(frame);
    }

    /* ===================== ACTIVE ROW HIGHLIGHT SUPPORT ===================== */

    public static void setActiveRow(JTable table, int rowIndex) {
        if (table == null) return;
        table.putClientProperty(ACTIVE_ROW_PROPERTY, rowIndex);
        table.repaint();
    }

    public static void setActiveCell(JTable table, int rowIndex, int columnIndex) {
        if (table == null) return;
        table.putClientProperty(ACTIVE_ROW_PROPERTY, rowIndex);
        table.putClientProperty(ACTIVE_CELL_ROW_PROPERTY, rowIndex);
        table.putClientProperty(ACTIVE_CELL_COLUMN_PROPERTY, columnIndex);
        table.repaint();
    }

    public static void clearActiveCell(JTable table) {
        if (table == null) return;
        table.putClientProperty(ACTIVE_CELL_ROW_PROPERTY, -1);
        table.putClientProperty(ACTIVE_CELL_COLUMN_PROPERTY, -1);
        table.repaint();
    }

    public static void clearActiveRow(JTable table) {
        if (table == null) return;
        table.putClientProperty(ACTIVE_ROW_PROPERTY, -1);
        clearActiveCell(table);
        table.clearSelection();
        table.repaint();
    }

    /* ===================== INNER CLASSES ===================== */

    /**
     * Renderer that colors the active step row based on table client property.
     */
    public static class HighlightRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(
                    table, value, false, false, row, column);

            Object rowProp = table.getClientProperty(ACTIVE_ROW_PROPERTY);
            Object activeCellRowProp = table.getClientProperty(ACTIVE_CELL_ROW_PROPERTY);
            Object activeCellColumnProp = table.getClientProperty(ACTIVE_CELL_COLUMN_PROPERTY);
            int activeRow = (rowProp instanceof Integer) ? (Integer) rowProp : -1;
            int activeCellRow = (activeCellRowProp instanceof Integer) ? (Integer) activeCellRowProp : -1;
            int activeCellColumn = (activeCellColumnProp instanceof Integer) ? (Integer) activeCellColumnProp : -1;
            boolean isActiveCell = row == activeCellRow && column == activeCellColumn;

            if (isActiveCell) {
                c.setBackground(ACTIVE_CELL_COLOR);
                c.setForeground(Color.BLACK);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK, 2),
                        BorderFactory.createEmptyBorder(3, 8, 3, 8)
                ));
            } else if (row == activeRow) {
                c.setBackground(ACTIVE_ROW_COLOR);
                c.setForeground(Color.BLACK);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            } else {
                c.setBackground(TABLE_BACKGROUND);
                c.setForeground(Color.BLACK);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            }
            return c;
        }
    }

    public static void applyTableBackground(JTable table) {
	    table.setBackground(TABLE_BACKGROUND);
	    table.setOpaque(true);

	    // Ensure the scroll pane background matches
	    if (table.getParent() instanceof JViewport) {
		JViewport viewport = (JViewport) table.getParent();
		viewport.setBackground(TABLE_BACKGROUND);
	    }

	    // Header background (optional)
	    JTableHeader header = table.getTableHeader();
	    header.setBackground(TABLE_BACKGROUND);

	    // Ensure default renderer respects the background
	    DefaultTableCellRenderer renderer =
		(DefaultTableCellRenderer) table.getDefaultRenderer(Object.class);
	    renderer.setBackground(TABLE_BACKGROUND);
    }
    public static class ScientificRenderer extends DefaultTableCellRenderer {
	    @Override
	    public Component getTableCellRendererComponent(
		    JTable table, Object value, boolean isSelected,
		    boolean hasFocus, int row, int column) {

		Component c = super.getTableCellRendererComponent(
		        table, value, isSelected, hasFocus, row, column);

		if (value instanceof Number) {
		    double d = ((Number) value).doubleValue();
		    setText(String.format("%1.1E", d));   // 1 significant digits scientific notation
		}
		return c;
	    }
    }

    /**
     * Touch-friendly combo editor (moved out of GlgGui).
     */
    public static class TouchComboEditor extends DefaultCellEditor {

        private final JComboBox<?> combo;

        public TouchComboEditor(String[] items) {
            super(new JComboBox<>(items));
            this.combo = (JComboBox<?>) getComponent();

            combo.setMaximumRowCount(items.length);

            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {

                    Component c = super.getListCellRendererComponent(
                            list, value, index, isSelected, cellHasFocus);
                    c.setFont(new Font("SansSerif", Font.BOLD, 10));
                    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                    return c;
                }
            });
        }
    }
}
