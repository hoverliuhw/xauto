import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import acm.gui.TableLayout;


public class MgtsManagerGui {
	public final static int APPROVE_OPTION = 1;
	public final static int CONNECT_OPTION = 2;
	public final static int CANCEL_OPTION = 3;
	public final static int WIDTH = 400;
	public final static int HEIGHT = 300;
	
	XController controller = null;
	
	JDialog mainWindow = null;
	JPanel configPane = null;
	JComboBox<String> serverList = null;
	JComboBox<String> protocolList = null;
	JTextField shelfTextField = null;
	JTextField userTextField = null;
	JPasswordField passwdTextField = null;
	JTextField displayTextField = null;
	
	JPanel buttonPane = null;
	JButton setButton = null;
	JButton cancelButton = null;
	
	public MgtsManagerGui(XController controller) {
		this.controller = controller;
		JFrame parentWindow = null;
		if (controller.gui != null) {
			parentWindow = controller.gui.getMainFrame();
		}
		
		mainWindow = new JDialog(parentWindow, "MGTS Manager", true);
		mainWindow.setSize(WIDTH, HEIGHT);
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize(); 
		int x = (int) screensize.getWidth() / 2 - WIDTH / 2;
		int y = (int) screensize.getHeight() / 2 - HEIGHT / 2;
		mainWindow.setLocation(x, y);
		
		initConfigPane();
		initButtonPane();
		
		JSplitPane mainPane = new JSplitPane();
		mainPane.setPreferredSize(new Dimension(mainWindow.getWidth(), mainWindow.getHeight()));
		mainPane.setOneTouchExpandable(false);
		mainPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		mainPane.setDividerSize(0);
		mainPane.setDividerLocation((mainWindow.getHeight() * 3) / 5);
		mainPane.setLeftComponent(configPane);
		mainPane.setRightComponent(buttonPane);
		
		mainWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		mainWindow.setContentPane(mainPane);
		mainWindow.setVisible(true);
	}
	
	private void initConfigPane() {
		configPane = new JPanel();
		configPane.setLayout(new TableLayout(6, 2));
		serverList = new JComboBox<String>();
		protocolList = new JComboBox<String>();
		shelfTextField = new JTextField(10);
		userTextField = new JTextField(10);
		passwdTextField = new JPasswordField(10);
		displayTextField = new JTextField(20);
		
		serverList.addItem("p250alu");
		serverList.addItem("p200alu");
		protocolList.addItem("ITU");
		protocolList.addItem("ANSI");
		shelfTextField.setText("EE");
		userTextField.setText("yrli");
		passwdTextField.setText("yrli");
		displayTextField.setText(MainGui.DEFAULT_DISPLAY);
		
		configPane.add(new JLabel("MGTS Server "));
		configPane.add(serverList);
		configPane.add(new JLabel("Protocol "));
		configPane.add(protocolList);
		configPane.add(new JLabel("Shelf Name "));
		configPane.add(shelfTextField);
		configPane.add(new JLabel("User Name "));
		configPane.add(userTextField);
		configPane.add(new JLabel("Password "));
		configPane.add(passwdTextField);
		configPane.add(new JLabel("Display "));
		configPane.add(displayTextField);
	}
	
	private void initButtonPane() {
		buttonPane = new JPanel();
		setButton = new JButton("Set");
		cancelButton = new JButton("Cancel");
		
		setButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MgtsHost oldMgts = controller.getMgts();
				if (oldMgts != null) {
					String message = "MGTS has been connected, do you want to disconnect current connection? ";
					int choice = JOptionPane.showConfirmDialog(mainWindow, message, "Warning", JOptionPane.YES_NO_OPTION);
					if (choice == JOptionPane.NO_OPTION) {
						return;
					}
				}
				String hostname = (String) serverList.getSelectedItem();
				String ip = null;
				if (hostname.equals("p250alu")) {
					ip = "135.252.170.143";
				} else {
					ip = "135.252.170.118";
				}
				int port = 23;
				String username = userTextField.getText();
				String passwd = passwdTextField.getText();
				
				MgtsHost mgts = new MgtsHost(hostname, ip, port, username, passwd);
				mgts.setProtocol((String) protocolList.getSelectedItem());
				mgts.setShelfName(shelfTextField.getText());
				controller.setMgtsHost(mgts);
				controller.setDisplay(displayTextField.getText());
				mainWindow.dispose();
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainWindow.dispose();
			}
		});
		
		buttonPane.add(setButton);
		buttonPane.add(cancelButton);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		XController c = new XController();
		MgtsManagerGui mgtsMgr = new MgtsManagerGui(c);
		System.out.println("finish");
	}

}
