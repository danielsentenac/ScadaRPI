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


public class DialogConfirm extends JDialog {
    private static final Logger logger = Logger.getLogger("Main");
    private JPanel panel; 
    private RoundJTextField jTextField = new RoundJTextField(5);
    private JLabel jLabelText = new JLabel();
    private JButton jButtonOK = new JButton();
    private JButton jButtonCancel = new JButton();
    private JFrame parent;
    private String title;
    private String glgName;
    private Device device;
    private DataElement dataElement;
    private Vector<String> command;
   
    public DialogConfirm(JFrame _parent, String _glgName, Device _device, DataElement _dataElement, String _title, boolean _modal) {
	try {
            parent = _parent;
            title = _title + " Vacuum ?";
	    this.setModal(_modal);
            glgName = _glgName;
            device = _device;
            dataElement = _dataElement;
	    jButtonOK.setVisible(true);
	    jButtonOK.setEnabled(true);
	    jButtonCancel.setVisible(true);
	    jButtonCancel.setEnabled(true);
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
        
	jButtonOK.setFont(new java.awt.Font("Dialog", 1, 20));
	jButtonOK.setText("OK");
        setComponentSize(jButtonOK,150,50);
        jButtonOK.setBorder(new RoundedBorder(30));
        jButtonOK.setBackground(new Color(Integer.parseInt("8BC5C5", 16)));
        this.getRootPane().setDefaultButton(jButtonOK);
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
           public void actionPerformed(ActionEvent e) {
              jButtonOK_actionPerformed(e);
              dispose();
           } 
	});
        jButtonCancel.setFont(new java.awt.Font("Dialog", 1, 20));
	jButtonCancel.setText("OK");
        setComponentSize(jButtonCancel,150,50);
        jButtonCancel.setBorder(new RoundedBorder(30));
        jButtonCancel.setBackground(new Color(Integer.parseInt("8BC5C5", 16)));
        this.getRootPane().setDefaultButton(jButtonCancel);
	jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    jButtonCancel_actionPerformed(e);
                    dispose();
		}
	    });
	    
	this.getRootPane().setBorder(BorderFactory.createLoweredBevelBorder());
        this.setUndecorated(true);
	this.getContentPane().add(panel);
	
	panel.add(jLabelText, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						     , GridBagConstraints.CENTER, GridBagConstraints.NONE,
						     new Insets(10, 10, 10, 10), 0, 0));
	panel.add(jButtonOK, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0
						    , GridBagConstraints.CENTER, GridBagConstraints.NONE,
						    new Insets(0, 0, 10, 0), 0, 0));
	panel.add(jButtonCancel, new GridBagConstraints(0, 1, 2, 2, 1.0, 1.0
						    , GridBagConstraints.CENTER, GridBagConstraints.NONE,
						    new Insets(0, 0, 10, 0), 0, 0));					    
						    
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
        logger.finer("DialogOK::jButtonOK_actionPerformed> OK ");
	// Update DataElement
        if ( title.equals("Start") )
           dataElement.setvalue = 1;
        else if ( title.equals("Stop") )
           dataElement.setvalue = 2;
        device.commandSetQueue.add(dataElement); // queue new value
    }
    void jButtonCancel_actionPerformed(ActionEvent e) {
        logger.finer("DialogCancel::jButtonCancel_actionPerformed> Cancel ");
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
   
}
