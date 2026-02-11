import java.awt.IllegalComponentStateException;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Color;
import java.awt.Point;
import java.awt.Dimension;
import javax.swing.JTextField;       // for GUI management
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JFrame;
import java.util.Vector;
import java.util.Enumeration;
import java.util.UUID;
import javax.swing.BorderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;


public class DialogOp extends JDialog {
    private static final Logger logger = Logger.getLogger("Main");
    private JPanel panel; 
    private RoundJTextField jTextField = new RoundJTextField(5);
    private JLabel jLabelText = new JLabel();
    private JButton jButtonOK = new JButton();
    private JFrame parent;
    private String title;
    private String origin;
    private Operation op;
    private Vector<String> command;
   
    public DialogOp(JFrame _parent, String _origin, Operation _op, String _title, boolean _modal) {
	try {
            parent = _parent;
            title = _title;
	    this.setModal(_modal);
            origin = _origin;
            op = _op;
	    jButtonOK.setVisible(true);
	    jButtonOK.setEnabled(true);
	    Init();
	}
	catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
    
    void Init() throws Exception {
        this.setResizable(false);
	panel  = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(Integer.parseInt("8BC5C5", 16)));
	jLabelText.setFont(new java.awt.Font("Dialog", 1, 22));
	jLabelText.setForeground(Color.red);
        jLabelText.setText(title);
        setComponentSize(jTextField,150,50);
        jTextField.setText("");
        jTextField.setFont(new java.awt.Font("Dialog", 1, 28));
        //jTextField.setBorder(BorderFactory.createLineBorder(Color.white,1,true));
        jTextField.setBackground(new Color(Integer.parseInt("8BC5C5", 16)));
       
	jButtonOK.setFont(new java.awt.Font("Dialog", 1, 20));
	jButtonOK.setText("OK");
        setComponentSize(jButtonOK,150,50);
        jButtonOK.setBorder(new RoundedBorder(30));
        jButtonOK.setBackground(new Color(Integer.parseInt("8BC5C5", 16)));
        this.getRootPane().setDefaultButton(jButtonOK);
        this.getRootPane().setBorder(BorderFactory.createLoweredBevelBorder());
        this.setUndecorated(true);
	jButtonOK.addActionListener(new java.awt.event.ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    jButtonOK_actionPerformed(e);
                    dispose();
		}
	    });
	this.getContentPane().add(panel);
	panel.add(jLabelText, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						     , GridBagConstraints.CENTER, GridBagConstraints.NONE,
						     new Insets(10, 10, 10, 10), 0, 0));
        panel.add(jTextField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						     , GridBagConstraints.CENTER, GridBagConstraints.NONE,
						     new Insets(10, 10, 10, 10), 0, 0));
        panel.add(new Keypad(jTextField), new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0
						    , GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
						    new Insets(0, 0, 0, 0), 0, 0));
	panel.add(jButtonOK, new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0
						    , GridBagConstraints.CENTER, GridBagConstraints.NONE,
						    new Insets(0, 0, 10, 0), 0, 0));
        
	this.setTitle("SET VALUE");
        Dimension dlgSize = this.getPreferredSize();
        Dimension frmSize = parent.getContentPane().getSize();
        Point loc = null;
        while ( loc == null ) loc = GetLocationOnScreen();
        this.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                          (frmSize.height - dlgSize.height) / 2 + loc.y);
	this.pack();
	this.setVisible(true);
    }
    public static void setComponentSize(JComponent component, int width, int height) {
        component.setPreferredSize(new Dimension(width,height));
	component.setMinimumSize(new Dimension(width,height));
	component.setMaximumSize(new Dimension(width,height));
    }
    void jButtonOK_actionPerformed(ActionEvent e) {
        if (!jTextField.getText().equals("")) {
           logger.finer("Set new parameter for " + origin + "=" + jTextField.getText());
           int value = Integer.valueOf(jTextField.getText());
	   // Update parameter
           try {
            if ( origin.contains("FlowMax") ) op.setFlowMax(value);
            else if ( origin.contains("FlowStep") ) op.setFlowStep(value);
            else if ( origin.contains("TimeInter") ) op.setTimeInter(value);
           }
           catch (NumberFormatException ex) {
               logger.log(Level.SEVERE,"DialogOp::jButtonOK_actionPerformed> Not a double:" + jTextField.getText());
           }
	}
    }
    protected void processWindowEvent(WindowEvent e) {
	super.processWindowEvent(e);
	if (e.getID() == WindowEvent.WINDOW_CLOSING) {
	    dispose();
	}
    }
    Point GetLocationOnScreen() {
	Point loc = null;
	try {
	    loc = parent.getContentPane().getLocationOnScreen();
	}
	catch (IllegalComponentStateException il){}
	return (loc);
    }
    public class Keypad extends JPanel implements ActionListener {
   
      private JLabel display;
      private JButton numButton;
      private JButton clearButton;
      private String displayContent = "";
      private String[] numPadContent = {"1","2","3","4","5","6","7","8","9","0",".","E","-"};
      private ArrayList<JButton> buttonList;
	
      // Keypad constructor class
      public Keypad(JTextField _jTextField) {
         // sets the size of the Keypad display
         this.setPreferredSize(new Dimension(300, 300));
	 this.setBackground(new Color(Integer.parseInt("8BC5C5", 16)));
         // initialize the buttonList
         buttonList = new ArrayList<JButton>(13);
         JPanel numberPanel = new JPanel();
         // set the numberPanel to have a 4row by 3col grid layout
         numberPanel.setLayout(new GridLayout(4,3,0,0));
         // set the size of the numberPanel
         numberPanel.setPreferredSize(new Dimension(300,230));
         // create the buttons and add them to the buttonList, properly displaying the numbers 
         for (int i = 0; i < numPadContent.length; i++) {
	    numButton = new JButton(numPadContent[i]);
            numButton.setFont(new java.awt.Font("Dialog", 1, 20));
	    buttonList.add(numButton);
         }
         // add the buttonList to the number panel
         for (int n = 0; n < buttonList.size(); n++) {
	    buttonList.get(n).addActionListener(this);
     	    numberPanel.add(buttonList.get(n));
         }
         // create black border around the number panel
         //numberPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black));
         numberPanel.setBorder(new RoundedBorder(30));
         numberPanel.setBackground(new Color(Integer.parseInt("8BC5C5", 16)));
         // add number panel to center part of display
         this.add(numberPanel, BorderLayout.LINE_END);
	
         // create Clear button that is actionable
         clearButton = new JButton("CLEAR");
         clearButton.setFont(new java.awt.Font("Dialog", 1, 20));
         clearButton.setBorder(new RoundedBorder(30));
         clearButton.setBackground(new Color(Integer.parseInt("8BC5C5", 16)));
         clearButton.setPreferredSize(new Dimension(150,50));
         clearButton.addActionListener(this);
         // add Clear button to bottom of display
         this.add(clearButton, BorderLayout.LINE_END);
      }	
      // update the display depending on clicked button(s)
      public void actionPerformed(ActionEvent e) {
         String textThere = jTextField.getText();
         String additionalText = "";
         // add clicked number button text to display
         for (int a = 0; a < buttonList.size(); a++) {
            if (e.getSource().equals(buttonList.get(a))) {
	       additionalText = buttonList.get(a).getText();
	    }
         }	
         // clear display if "Clear" button is clicked
         if (e.getSource().equals(clearButton)) {
	    textThere = "";
         }
         jTextField.setText(textThere.concat(additionalText));
      }
   }
}
