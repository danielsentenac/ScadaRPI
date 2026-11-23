/*
 * AI assistant window of the HMI. It talks over HTTP to the assistant application running on the
 * Arduino VENTUNO Q (work_ventunoq/vacuum-assistant), reached by the direct Ethernet link.
 *
 * From top to bottom:
 *   - instructions: a text chat (left) and the preset buttons (right):
 *       SAFETY (heating off, valves closed, pumping system stopped),
 *       BACKGROUND BAKING (150 degC, only if the pumping system is at regime),
 *       PLAY (load an experiment instruction file and execute it), STOP EXPERIMENT;
 *   - live log: every action of the assistant;
 *   - system files: the instruction file in use and the log file.
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
    private File lastDirectory = new File(System.getProperty("user.home"));

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
        JPanel presets = titled("BASIC INSTRUCTIONS", new GridLayout(4, 1, 0, 10));
        presets.add(safety); presets.add(baking); presets.add(play); presets.add(stop);
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
        files.add(instructionFile); files.add(logFile);

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

    private void play() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setDialogTitle("Experiment instruction file (*" + EXPERIMENT_EXT + ")");
        chooser.setAcceptAllFileFilterUsed(false);         // only experiment files are offered
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().endsWith(EXPERIMENT_EXT); }
            public String getDescription() { return "Experiment instruction files (*" + EXPERIMENT_EXT + ")"; } });
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        final File file = chooser.getSelectedFile();
        lastDirectory = file.getParentFile();
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
