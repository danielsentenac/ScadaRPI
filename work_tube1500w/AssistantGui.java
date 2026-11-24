/*
 * AI assistant window of the HMI. It talks over HTTP to the assistant application running on the
 * Arduino VENTUNO Q (work_ventunoq/vacuum-assistant), reached by the direct Ethernet link.
 *
 * From top to bottom:
 *   - instructions: a text chat (left) and the preset buttons (right):
 *       SAFETY (heating off, valves closed, pumping system stopped),
 *       BACKGROUND BAKING (150 degC, only if the pumping system is at regime),
 *       PLAY (load an experiment instruction file, *.exp, from the "play" folder and execute it),
 *       VACUUM REPORT (record the RGA until stable, then save a status report), STOP EXPERIMENT;
 *   - live log: every action of the assistant;
 *   - system files: the instruction file in use, the log file and the saved vacuum reports.
 * The chat is typed with the on-screen keyboard (KEYBOARD button, or a touch in the text field),
 * as the panel has no physical keyboard.
 *
 * This window sends no command to the devices itself: the buttons ask the assistant to run a
 * sequence, and the assistant writes to the panel's Modbus server.
 * Standalone test: java AssistantGui http://<assistant address>:7000
 */
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class AssistantGui extends JFrame {

    private static final long serialVersionUID = 354054054090L;
    private static final Color BACKGROUND = new Color(139, 197, 197);
    private static final Font TEXT = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font BUTTON = new Font("SansSerif", Font.BOLD, 17);
    private static final Font MONO = new Font("Monospaced", Font.PLAIN, 14);

    static final String EXPERIMENT_EXT = ".exp";   // extension of the experiment instruction files (PLAY)
    private final String base;
    private final JTextArea chat = new JTextArea();
    private final JTextArea log = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton send = new JButton("SEND");
    private final JLabel state = new JLabel("connecting...");
    private final JPanel keyboard = new JPanel(new GridLayout(4, 1, 0, 4));
    private final java.util.List<JButton> letterKeys = new java.util.ArrayList<JButton>();
    private boolean shift = false;
    private int nextLine = -1;                 // next line of the action log to ask for
    /** Folder of the experiment files offered by PLAY: "play" in the panel folder, or -Dscadarpi.playDir=<folder>. */
    private final File playDirectory = new File(System.getProperty("scadarpi.playDir", "play")).getAbsoluteFile();

    public AssistantGui(String title, String assistantUrl) {
        super("AI assistant - " + title);
        base = assistantUrl.endsWith("/") ? assistantUrl.substring(0, assistantUrl.length() - 1) : assistantUrl;
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        setContentPane(root);

        // ---- title line: name, link state, back
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        JLabel name = new JLabel("AI ASSISTANT - " + title);
        name.setFont(new Font("SansSerif", Font.BOLD, 24));
        name.setForeground(Color.BLUE);
        state.setFont(TEXT);
        JButton back = button("BACK", new Color(235, 235, 245));
        back.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setVisible(false); } });
        top.add(name, BorderLayout.WEST); top.add(state, BorderLayout.CENTER); top.add(back, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        // ---- instructions: chat and preset buttons
        chat.setEditable(false); chat.setLineWrap(true); chat.setWrapStyleWord(true); chat.setFont(TEXT);
        input.setFont(TEXT);
        JPanel entry = new JPanel(new BorderLayout(6, 0));
        entry.setOpaque(false);
        send.setFont(BUTTON);
        JButton keys = button("KEYBOARD", new Color(235, 235, 245));
        keys.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            showKeyboard(!keyboard.isVisible()); } });
        input.addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { showKeyboard(true); } });
        JPanel entryButtons = new JPanel(new GridLayout(1, 2, 6, 0));
        entryButtons.setOpaque(false);
        entryButtons.add(send); entryButtons.add(keys);
        entry.add(input, BorderLayout.CENTER); entry.add(entryButtons, BorderLayout.EAST);
        JPanel chatPanel = titled("CHAT WITH THE ASSISTANT", new BorderLayout(0, 6));
        chatPanel.add(new JScrollPane(chat), BorderLayout.CENTER); chatPanel.add(entry, BorderLayout.SOUTH);
        ActionListener ask = new ActionListener() { public void actionPerformed(ActionEvent e) { ask(); } };
        send.addActionListener(ask); input.addActionListener(ask);

        JButton safety = button("SAFETY", new Color(230, 70, 60));
        safety.setForeground(Color.WHITE);
        safety.setToolTipText("Heating off, valves closed, pumping system stopped");
        JButton baking = button("BACKGROUND BAKING", new Color(245, 170, 60));
        baking.setToolTipText("Baking at 150 degC, only if the pumping system is at regime");
        JButton play = button("PLAY", new Color(90, 190, 100));
        play.setToolTipText("Load an experiment instruction file and execute it");
        JButton report = button("VACUUM REPORT", new Color(120, 170, 240));
        report.setToolTipText("Record the RGA until the main masses are stable, then save a vacuum status report");
        report.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            if (confirm("Record the RGA until it is stable and save a vacuum status report?\n"
                        + "The RGA scan is started if it is not running."))
                request("VACUUM REPORT", "/action?name=report"); } });
        JButton history = button("HISTORICAL REPORT", new Color(150, 190, 245));
        history.setToolTipText("Report of what happened since one event (a valve closed...), from the DAQ trend data");
        history.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { historicalReport(); } });
        JButton stop = button("STOP EXPERIMENT", new Color(235, 235, 245));
        stop.setToolTipText("Stop the running sequence; the system is left as it is");
        safety.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            request("SAFETY", "/action?name=safety"); } });      // no confirmation: it must be immediate
        baking.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            if (confirm("Start the background baking at 150 degC?\nIt is refused if the pumping system is not at regime."))
                request("BACKGROUND BAKING", "/action?name=baking"); } });
        stop.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            request("STOP EXPERIMENT", "/action?name=stop"); } });
        play.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { play(); } });
        JPanel presets = titled("BASIC INSTRUCTIONS", new GridLayout(6, 1, 0, 8));
        presets.add(safety); presets.add(baking); presets.add(play); presets.add(report); presets.add(history); presets.add(stop);
        presets.setPreferredSize(new Dimension(300, 10));
        JPanel instructions = new JPanel(new BorderLayout(8, 0));
        instructions.setOpaque(false);
        instructions.add(chatPanel, BorderLayout.CENTER); instructions.add(presets, BorderLayout.EAST);

        // ---- live log
        log.setEditable(false); log.setFont(MONO); log.setLineWrap(true); log.setWrapStyleWord(true);
        JPanel logPanel = titled("LIVE LOG - every action of the assistant", new BorderLayout());
        logPanel.add(new JScrollPane(log), BorderLayout.CENTER);

        // ---- system files
        JButton instructionFile = button("INSTRUCTION FILE", new Color(235, 235, 245));
        JButton logFile = button("LOG FILE", new Color(235, 235, 245));
        instructionFile.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { showFile("instructions"); } });
        logFile.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { showFile("log"); } });
        JPanel files = titled("SYSTEM FILES", new FlowLayout(FlowLayout.LEFT, 12, 2));
        JButton reports = button("REPORTS", new Color(235, 235, 245));
        reports.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { showReports(); } });
        files.add(instructionFile); files.add(logFile); files.add(reports);

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.fill = GridBagConstraints.BOTH; c.weightx = 1; c.insets = new Insets(0, 0, 6, 0);
        c.gridy = 0; c.weighty = 0.50; centre.add(instructions, c);
        c.gridy = 1; c.weighty = 0.50; centre.add(logPanel, c);
        c.gridy = 2; c.weighty = 0; c.insets = new Insets(0, 0, 0, 0); centre.add(files, c);
        root.add(centre, BorderLayout.CENTER);
        buildKeyboard();
        root.add(keyboard, BorderLayout.SOUTH);

        chat.append("Ask about the state of the system, or use the buttons on the right.\n");
        Thread poller = new Thread(new Runnable() { public void run() { poll(); } }, "assistant-log");
        poller.setDaemon(true);
        poller.start();
    }

    public void showWindow() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
        toFront();
        input.requestFocusInWindow();
    }

    // ------------------------------------------------------------------ on-screen keyboard
    private static final String[] KEY_ROWS = { "1234567890", "qwertyuiop", "asdfghjkl?", "zxcvbnm,.-" };
    private static final String SHIFT_DIGITS = "<>=%+/():'";

    /* The keys never take the focus, so the caret stays in the chat text field. */
    private JButton key(String text, double weight, JPanel row, GridBagConstraints c, ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(BUTTON); b.setFocusable(false); b.setMargin(new Insets(9, 2, 9, 2));
        b.setBackground(new Color(245, 245, 250));
        b.addActionListener(action);
        c.weightx = weight;
        row.add(b, c);
        return b;
    }

    private void buildKeyboard() {
        keyboard.setOpaque(false);
        keyboard.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        ActionListener type = new ActionListener() { public void actionPerformed(ActionEvent e) {
            type(((JButton) e.getSource()).getText()); } };
        for (int r = 0; r < KEY_ROWS.length; r++) {
            JPanel row = new JPanel(new GridBagLayout());
            row.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH; c.weighty = 1; c.insets = new Insets(0, 2, 0, 2);
            if (r == 3) key("SHIFT", 1.6, row, c, new ActionListener() { public void actionPerformed(ActionEvent e) {
                setShift(!shift); } });
            for (char ch : KEY_ROWS[r].toCharArray()) letterKeys.add(key(String.valueOf(ch), 1, row, c, type));
            if (r == 0) key("DELETE", 1.6, row, c, new ActionListener() { public void actionPerformed(ActionEvent e) {
                backspace(); } });
            if (r == 1) key("CLEAR", 1.6, row, c, new ActionListener() { public void actionPerformed(ActionEvent e) {
                input.setText(""); } });
            if (r == 2) key("SEND", 1.6, row, c, new ActionListener() { public void actionPerformed(ActionEvent e) {
                ask(); } });
            if (r == 3) {
                key("SPACE", 4, row, c, new ActionListener() { public void actionPerformed(ActionEvent e) { type(" "); } });
                key("HIDE", 1.6, row, c, new ActionListener() { public void actionPerformed(ActionEvent e) {
                    showKeyboard(false); } });
            }
            keyboard.add(row);
        }
        keyboard.setVisible(false);
    }

    private void showKeyboard(boolean on) {
        keyboard.setVisible(on);
        getContentPane().revalidate();
        input.requestFocusInWindow();
    }

    private void setShift(boolean on) {
        shift = on;
        int i = 0;
        for (String rowKeys : KEY_ROWS)
            for (char ch : rowKeys.toCharArray()) {
                String t = String.valueOf(ch);
                if (on) t = Character.isDigit(ch) ? String.valueOf(SHIFT_DIGITS.charAt(ch == '0' ? 9 : ch - '1'))
                                                  : t.toUpperCase();
                letterKeys.get(i++).setText(t);
            }
    }

    private void type(String text) {
        if (!input.isEnabled()) return;
        input.replaceSelection(text);
        if (shift) setShift(false);                // shift holds for one key
    }

    private void backspace() {
        if (!input.isEnabled()) return;
        if (input.getSelectedText() != null) { input.replaceSelection(""); return; }
        int at = input.getCaretPosition();
        if (at == 0) return;
        try { input.getDocument().remove(at - 1, 1); } catch (javax.swing.text.BadLocationException ignored) { }
    }

    // ------------------------------------------------------------------ widgets
    private static JButton button(String text, Color colour) {
        JButton b = new JButton(text);
        b.setFont(BUTTON); b.setBackground(colour); b.setFocusPainted(false);
        b.setMargin(new Insets(8, 16, 8, 16));
        return b;
    }

    private static JPanel titled(String title, LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(220, 255, 200), 2), title);
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 15));
        p.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        return p;
    }

    private boolean confirm(String question) {
        return JOptionPane.showConfirmDialog(this, question, "Confirm", JOptionPane.OK_CANCEL_OPTION,
                                             JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void say(final String text) {
        SwingUtilities.invokeLater(new Runnable() { public void run() {
            chat.append(text + "\n"); chat.setCaretPosition(chat.getDocument().getLength()); } });
    }

    // ------------------------------------------------------------------ actions
    private void ask() {
        final String q = input.getText().trim();
        if (q.isEmpty()) return;
        input.setText(""); input.setEnabled(false); send.setEnabled(false);
        say("\nYou: " + q);
        say("Assistant: (thinking...)");
        new Thread(new Runnable() { public void run() {
            String answer;
            try {
                String reply = http("GET", base + "/ask?q=" + URLEncoder.encode(q, "UTF-8"), null, 240000);
                answer = jsonString(reply, "answer");
                if (answer == null) answer = "error: " + jsonString(reply, "error");
            } catch (Exception ex) { answer = "the assistant cannot be reached (" + ex.getMessage() + ")"; }
            say("Assistant: " + answer.replace("**", ""));
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                input.setEnabled(true); send.setEnabled(true); input.requestFocusInWindow(); } });
        } }, "assistant-ask").start();
    }

    /** Ask the assistant to run a preset sequence; its progress appears in the live log. */
    private void request(final String label, final String path) {
        new Thread(new Runnable() { public void run() {
            try { say("\n[" + label + "] " + jsonString(http("GET", base + path, null, 10000), "message")); }
            catch (Exception ex) { say("\n[" + label + "] NOT SENT: the assistant cannot be reached (" + ex.getMessage() + ")"); }
        } }, "assistant-request").start();
    }

    /** HISTORICAL REPORT, from a START to a STOP. START is an event (an item taking a state, or a
     *  set point changing; the assistant searches its times in the DAQ trend data) or a date;
     *  STOP is the present, an event after START, or a date. */
    private void historicalReport() {
        new Thread(new Runnable() { public void run() {
            final java.util.List<String[]> items = new java.util.ArrayList<String[]>();   // name, channel, states
            try {
                String reply = http("GET", base + "/event_items", null, 10000);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "\\{\"name\":\"([^\"]+)\",\"channel\":\"([^\"]+)\",\"states\":\\{([^}]*)\\}").matcher(reply);
                while (m.find()) items.add(new String[] { m.group(1), m.group(2), m.group(3) });
            } catch (Exception ex) { say("\n[HISTORICAL REPORT] the assistant cannot be reached (" + ex.getMessage() + ")"); return; }
            if (items.isEmpty()) { say("\n[HISTORICAL REPORT] the assistant gave no item to choose"); return; }
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                chooseBound(true, items, 0, new Bound() { public void chosen(final String channel, final String code, final String t, final String text) {
                    chooseBound(false, items, Long.parseLong(t), new Bound() { public void chosen(String channel2, String code2, String t2, String text2) {
                        request("HISTORICAL REPORT from " + text + " to " + text2, "/action?name=history&channel=" + channel
                                + "&code=" + code + "&t=" + t + "&channel2=" + channel2 + "&code2=" + code2 + "&t2=" + t2);
                    } });
                } });
            } });
        } }, "assistant-history").start();
    }

    /** What chooseBound gives: the event (channel, code) and its unix time; an empty channel is a
     *  plain date, and the time 0 the present. */
    private interface Bound { void chosen(String channel, String code, String t, String text); }

    private static final int[] SEARCH_DAYS = { 2, 7, 30, 60 };

    /** Dialog of one bound of the historical report. START (start = true): an event searched in
     *  the last days, or a date. STOP: the present, an event searched after START (`after`), or a date. */
    private void chooseBound(final boolean start, final java.util.List<String[]> items, final long after, final Bound then) {
        final String title = "HISTORICAL REPORT - " + (start ? "START" : "STOP");
        final JComboBox<String> kind = new JComboBox<String>(start ? new String[] { "an event", "a date" }
                                                                   : new String[] { "the present", "an event", "a date" });
        final JComboBox<String> item = new JComboBox<String>(), state = new JComboBox<String>(), days = new JComboBox<String>();
        final JComboBox<String> day = new JComboBox<String>(), hour = new JComboBox<String>(), minute = new JComboBox<String>();
        final java.util.List<String> codes = new java.util.ArrayList<String>();
        for (String[] i : items) item.addItem(i[0]);
        for (int d : SEARCH_DAYS) days.addItem("last " + d + " days");
        days.setSelectedIndex(2);
        final java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        for (int d = 0; d <= SEARCH_DAYS[SEARCH_DAYS.length - 1]; d++) { day.addItem(dayFormat.format(cal.getTime())); cal.add(java.util.Calendar.DATE, -1); }
        for (int h = 0; h < 24; h++) hour.addItem(String.format("%02d h", h));
        for (int m = 0; m < 60; m += 5) minute.addItem(String.format("%02d min", m));
        ActionListener fill = new ActionListener() { public void actionPerformed(ActionEvent e) {
            state.removeAllItems(); codes.clear();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"(-?\\d+)\":\"([^\"]*)\"")
                .matcher(items.get(Math.max(0, item.getSelectedIndex()))[2]);
            while (m.find()) { codes.add(m.group(1)); state.addItem(m.group(2)); } } };
        item.addActionListener(fill);
        fill.actionPerformed(null);
        ActionListener enable = new ActionListener() { public void actionPerformed(ActionEvent e) {
            boolean event = "an event".equals(kind.getSelectedItem()), date = "a date".equals(kind.getSelectedItem());
            item.setEnabled(event); state.setEnabled(event); days.setEnabled(event && start);
            day.setEnabled(date); hour.setEnabled(date); minute.setEnabled(date); } };
        kind.addActionListener(enable);
        enable.actionPerformed(null);
        JPanel time = new JPanel(new GridLayout(1, 3, 6, 0));
        for (JComponent c : new JComponent[] { day, hour, minute }) { c.setFont(TEXT); time.add(c); }
        java.util.List<JComponent> rows = new java.util.ArrayList<JComponent>(java.util.Arrays.asList(new JComponent[] {
            new JLabel(start ? "START at" : "STOP at"), kind, new JLabel("Event: item"), item, new JLabel("became"), state }));
        if (start) rows.addAll(java.util.Arrays.asList(new JComponent[] { new JLabel("searched in the"), days }));
        rows.addAll(java.util.Arrays.asList(new JComponent[] { new JLabel("Date"), time }));
        JPanel panel = new JPanel(new GridLayout(rows.size() / 2, 2, 8, 8));
        for (JComponent c : rows) { if (c != time) c.setFont(TEXT); panel.add(c); }
        if (JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)
                != JOptionPane.OK_OPTION) return;
        if ("the present".equals(kind.getSelectedItem())) { then.chosen("", "-1", "0", "the present"); return; }
        if ("a date".equals(kind.getSelectedItem())) {
            String text = day.getSelectedItem() + " " + String.format("%02d:%02d", hour.getSelectedIndex(), minute.getSelectedIndex() * 5);
            try { then.chosen("", "-1", String.valueOf(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse(text).getTime() / 1000), text); }
            catch (java.text.ParseException ex) { say("\n[HISTORICAL REPORT] date not understood: " + text); }
            return;
        }
        if (state.getSelectedIndex() < 0) return;
        final String[] chosen = items.get(item.getSelectedIndex());
        final String code = codes.get(state.getSelectedIndex()), what = chosen[0] + " " + state.getSelectedItem();
        final String period = start ? "&days=" + SEARCH_DAYS[days.getSelectedIndex()] : "&days=" + SEARCH_DAYS[SEARCH_DAYS.length - 1] + "&after=" + after;
        say("\n[HISTORICAL REPORT] searching the DAQ data for \"" + what + "\" (this can take a minute or two)...");
        new Thread(new Runnable() { public void run() {
            final java.util.List<String[]> found = new java.util.ArrayList<String[]>();       // unix time, text
            String message = "";
            try {
                String reply = http("GET", base + "/events?channel=" + chosen[1] + "&code=" + code + period, null, 400000);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"t\":(\\d+),\"text\":\"([^\"]+)\"").matcher(reply);
                while (m.find()) found.add(new String[] { m.group(1), m.group(2) });
                message = jsonString(reply, "message");
            } catch (Exception ex) { message = "the assistant cannot be reached (" + ex.getMessage() + ")"; }
            if (found.isEmpty()) { say("[HISTORICAL REPORT] " + what + ": " + message); return; }
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                String[] times = new String[found.size()];
                for (int i = 0; i < times.length; i++) times[i] = found.get(i)[1];
                Object pick = JOptionPane.showInputDialog(AssistantGui.this, "\"" + what + "\" happened at these times"
                    + (start ? " (newest first).\nChoose the START of the report." : " after START (oldest first).\nChoose the STOP of the report."),
                    title, JOptionPane.QUESTION_MESSAGE, null, times, times[0]);
                if (pick == null) return;
                for (String[] f : found)
                    if (f[1].equals(pick)) { then.chosen(chosen[1], code, f[0], what + " " + f[1]); return; }
            } });
        } }, "assistant-history-search").start();
    }

    private void play() {
        playDirectory.mkdirs();                              // PLAY always opens in the play folder
        JFileChooser chooser = new JFileChooser(playDirectory);
        chooser.setDialogTitle("Experiment instruction file (*" + EXPERIMENT_EXT + ")");
        chooser.setAcceptAllFileFilterUsed(false);         // only experiment files are offered
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().endsWith(EXPERIMENT_EXT); }
            public String getDescription() { return "Experiment instruction files (*" + EXPERIMENT_EXT + ")"; } });
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        final File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(EXPERIMENT_EXT)) {
            say("\n[PLAY] " + file.getName() + " refused: an experiment file must end with " + EXPERIMENT_EXT); return; }
        final String text;
        try { text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
        catch (IOException ex) { say("\n[PLAY] cannot read " + file + ": " + ex.getMessage()); return; }
        JTextArea preview = new JTextArea(text, 18, 60);
        preview.setEditable(false); preview.setFont(MONO);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(preview), "Execute " + file.getName() + " ?",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        new Thread(new Runnable() { public void run() {
            try {
                String body = "{\"name\":" + jsonQuote(file.getName()) + ",\"text\":" + jsonQuote(text) + "}";
                say("\n[PLAY " + file.getName() + "] " + jsonString(http("POST", base + "/play", body, 10000), "message"));
            } catch (Exception ex) { say("\n[PLAY] NOT SENT: the assistant cannot be reached (" + ex.getMessage() + ")"); }
        } }, "assistant-play").start();
    }

    private void showFile(final String which) {
        new Thread(new Runnable() { public void run() {
            String name, text;
            try {
                String reply = http("GET", base + "/file?name=" + which, null, 10000);
                name = jsonString(reply, "name"); text = jsonString(reply, "text");
            } catch (Exception ex) { name = which; text = "The assistant cannot be reached (" + ex.getMessage() + ")"; }
            final String fname = name, ftext = text;
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                JTextArea area = new JTextArea(ftext, 26, 100);
                area.setEditable(false); area.setFont(MONO); area.setCaretPosition(area.getDocument().getLength());
                JOptionPane.showMessageDialog(AssistantGui.this, new JScrollPane(area), fname + " (on the assistant)",
                                              JOptionPane.PLAIN_MESSAGE);
            } });
        } }, "assistant-file").start();
    }

    /** List of the saved vacuum reports (newest first); the chosen one is shown as an image. */
    private void showReports() {
        new Thread(new Runnable() { public void run() {
            final java.util.List<String> names = new java.util.ArrayList<String>();
            try {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-Za-z0-9]+_report_\\d{4}-\\d{2}-\\d{2}_\\d{6}")
                        .matcher(http("GET", base + "/reports", null, 10000));
                while (m.find()) names.add(m.group());
            } catch (Exception ex) { say("\n[REPORTS] the assistant cannot be reached (" + ex.getMessage() + ")"); return; }
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                if (names.isEmpty()) {
                    JOptionPane.showMessageDialog(AssistantGui.this, "No report saved yet.", "Vacuum reports",
                                                  JOptionPane.PLAIN_MESSAGE);
                    return;
                }
                JList<String> list = new JList<String>(names.toArray(new String[0]));
                list.setFont(TEXT); list.setSelectedIndex(0); list.setVisibleRowCount(12);
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                if (JOptionPane.showConfirmDialog(AssistantGui.this, new JScrollPane(list), "Vacuum reports (newest first)",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION
                        && list.getSelectedValue() != null)
                    showReport(list.getSelectedValue());
            } });
        } }, "assistant-reports").start();
    }

    private void showReport(final String name) {
        new Thread(new Runnable() { public void run() {
            final java.awt.image.BufferedImage page;
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(base + "/report?name=" + name + "&format=png").openConnection();
                con.setConnectTimeout(4000); con.setReadTimeout(20000);
                InputStream in = con.getInputStream();
                try { page = javax.imageio.ImageIO.read(in); } finally { in.close(); }
                if (page == null) throw new IOException("not an image");
            } catch (Exception ex) { say("\n[REPORTS] cannot read " + name + " (" + ex.getMessage() + ")"); return; }
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                int width = Math.min(page.getWidth(), getWidth() - 170);        // fit the window width
                int height = page.getHeight() * width / page.getWidth();
                JLabel picture = new JLabel(new ImageIcon(page.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
                JScrollPane scroll = new JScrollPane(picture);
                scroll.setPreferredSize(new Dimension(width + 30, Math.min(height + 10, getHeight() - 140)));
                scroll.getVerticalScrollBar().setUnitIncrement(40);
                scroll.getVerticalScrollBar().setPreferredSize(new Dimension(28, 0));  // wide enough for a finger
                JOptionPane.showMessageDialog(AssistantGui.this, scroll,
                        name + ".xlsx   (Excel file: " + base + "/report?name=" + name + "&format=xlsx)", JOptionPane.PLAIN_MESSAGE);
            } });
        } }, "assistant-report").start();
    }

    /** Background loop: new lines of the action log and the state of the assistant, every 2 s. */
    private void poll() {
        while (true) {
            String label; Color colour = Color.BLACK;
            try {
                String reply = http("GET", base + "/log?since=" + nextLine, null, 5000);
                final String text = jsonString(reply, "text");
                nextLine = Integer.parseInt(jsonNumber(reply, "next"));
                if (text != null && !text.isEmpty())
                    SwingUtilities.invokeLater(new Runnable() { public void run() {
                        log.append(text + "\n"); log.setCaretPosition(log.getDocument().getLength()); } });
                String status = http("GET", base + "/status", null, 5000);
                String job = jsonString(status, "job");
                boolean enabled = status.contains("\"commands_enabled\":true");
                label = "   connected to " + base + "   |   commands " + (enabled ? "ENABLED" : "DISABLED (dry run)")
                        + "   |   " + (job == null || job.isEmpty() ? "idle" : "running: " + job);
            } catch (Exception ex) {
                label = "   NOT CONNECTED to " + base + " (" + ex.getMessage() + ")"; colour = new Color(170, 0, 0);
            }
            final String flabel = label; final Color fcolour = colour;
            SwingUtilities.invokeLater(new Runnable() { public void run() { state.setText(flabel); state.setForeground(fcolour); } });
            try { Thread.sleep(2000); } catch (InterruptedException ex) { return; }
        }
    }

    // ------------------------------------------------------------------ HTTP and JSON helpers
    static String http(String method, String url, String body, int timeoutMs) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod(method); con.setConnectTimeout(4000); con.setReadTimeout(timeoutMs);
        if (body != null) {
            con.setDoOutput(true); con.setRequestProperty("Content-Type", "application/json");
            OutputStream out = con.getOutputStream();
            out.write(body.getBytes(StandardCharsets.UTF_8)); out.close();
        }
        InputStream in = con.getResponseCode() < 400 ? con.getInputStream() : con.getErrorStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192]; int n;
        while (in != null && (n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
        if (con.getResponseCode() >= 400) throw new IOException("HTTP " + con.getResponseCode());
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    /** Value of a string member of a flat JSON object, or null. */
    static String jsonString(String json, String key) {
        int i = json.indexOf("\"" + key + "\":");
        if (i < 0) return null;
        i = json.indexOf('"', i + key.length() + 3);
        if (i < 0) return null;
        StringBuilder out = new StringBuilder();
        for (i++; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '"') return out.toString();
            if (ch != '\\') { out.append(ch); continue; }
            ch = json.charAt(++i);
            switch (ch) {
                case 'n': out.append('\n'); break;
                case 't': out.append('\t'); break;
                case 'r': break;
                case 'b': case 'f': break;
                case 'u': out.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16)); i += 4; break;
                default: out.append(ch);
            }
        }
        return out.toString();
    }

    static String jsonNumber(String json, String key) {
        int i = json.indexOf("\"" + key + "\":");
        if (i < 0) return "-1";
        int j = i + key.length() + 3;
        while (j < json.length() && (Character.isDigit(json.charAt(j)) || json.charAt(j) == '-')) j++;
        return json.substring(i + key.length() + 3, j);
    }

    static String jsonQuote(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            if (ch == '"' || ch == '\\') out.append('\\').append(ch);
            else if (ch == '\n') out.append("\\n");
            else if (ch == '\r') continue;
            else if (ch == '\t') out.append("\\t");
            else if (ch < 0x20) out.append(String.format("\\u%04x", (int) ch));
            else out.append(ch);
        }
        return out.append('"').toString();
    }

    public static void main(String[] a) {
        final String url = a.length > 0 ? a[0] : "http://192.168.251.2:7000";
        SwingUtilities.invokeLater(new Runnable() { public void run() {
            AssistantGui g = new AssistantGui("TEST", url);
            g.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            g.showWindow();
        } });
    }
}
