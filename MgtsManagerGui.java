/**
 **************************************************************************
 * Name: MgtsHostGui
 * 
 * Description: Derived from BaseHost, provide configuration of MGTS Client 
 * Author: Liu Hongwei
 * 		   hong_wei.hl.liu@alcatel-lucent.com
 * 
 *************************************************************************
 */

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.swing.*;
import acm.gui.TableLayout;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class MgtsManagerGui {
	public final static int APPROVE_OPTION = 1;
	public final static int CONNECT_OPTION = 2;
	public final static int CANCEL_OPTION = 3;
	public final static int WIDTH = 400;
	public final static int HEIGHT = 300;
	public final static String MGTS_CONFIG_FILE = System.getProperty("user.home") + "/.sptest/mgts.xml";
	
	XController controller = null;
	
	JDialog mainWindow = null;
	JPanel configPane = null;
	JComboBox<String> serverList = null;
	JTextField hostnameTextField = null;
	JTextField ipTextField = null;
	JComboBox<String> protocolList = null;
	JTextField shelfTextField = null;
	JTextField userTextField = null;
	JPasswordField passwdTextField = null;
	JTextField displayTextField = null;
	
	JPanel buttonPane = null;
	JButton setButton = null;
	JButton saveButton = null;
	JButton cancelButton = null;
	
	Document configFile = null;
	Element root = null;
	
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
		mainPane.setDividerLocation((mainWindow.getHeight() * 2) / 3);
		mainPane.setLeftComponent(configPane);
		mainPane.setRightComponent(buttonPane);
		
		mainWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		mainWindow.setContentPane(mainPane);
		mainWindow.setVisible(true);
	}
	
	private void initConfigPane() {
		configPane = new JPanel();
		configPane.setLayout(new TableLayout(8, 2));
		serverList = new JComboBox<String>();
		hostnameTextField = new JTextField(20);
		ipTextField = new JTextField(16);
		protocolList = new JComboBox<String>();
		shelfTextField = new JTextField(10);
		userTextField = new JTextField(10);
		passwdTextField = new JPasswordField(10);
		displayTextField = new JTextField(20);
		
		configPane.add(new JLabel("MGTS Server "));
		configPane.add(serverList);
		configPane.add(new JLabel("Server Hostname "));
		configPane.add(hostnameTextField);
		configPane.add(new JLabel("IP Address "));
		configPane.add(ipTextField);
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
		
		protocolList.addItem("ITU");
		protocolList.addItem("ANSI");
		
		serverList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showHost((String) serverList.getSelectedItem());		
			}
		});
		
		SAXBuilder builder = new SAXBuilder();
		try {
			configFile = builder.build(new File(MGTS_CONFIG_FILE));
			root = configFile.getRootElement();
		} catch (Exception e) {
			setDefaultHost();
			return;
		}
		
		List<Element> hostList = root.getChildren();
		for (int i = 0; i < hostList.size(); i++) {
			serverList.addItem(hostList.get(i).getName());
		}
		showHost(hostList.get(0).getName());
		
	}
	
	public void setDefaultHost() {
		serverList.addItem("p250alu");
		hostnameTextField.setText("p250alu");
		ipTextField.setText("135.252.170.143");
		protocolList.setSelectedItem("ANSI");
		shelfTextField.setText("EE");
		userTextField.setText("yrli");
		passwdTextField.setText("yrli");
		displayTextField.setText(MainGui.DEFAULT_DISPLAY);
	}
	
	public void showHost(String itemName) {
		if (root == null) {
			return;
		}
		Element mgtsHost = root.getChild(itemName);
		hostnameTextField.setText(mgtsHost.getChildText("hostname"));
		ipTextField.setText(mgtsHost.getChildText("ipaddr"));
		protocolList.setSelectedItem(mgtsHost.getChildText("protocol"));
		shelfTextField.setText(mgtsHost.getChildText("shelf"));
		userTextField.setText(mgtsHost.getChildText("user"));
		passwdTextField.setText(mgtsHost.getChildText("passwd"));
		displayTextField.setText(mgtsHost.getChildText("display"));
	}
	
	private void initButtonPane() {
		buttonPane = new JPanel();
		setButton = new JButton("Set");
		saveButton = new JButton("Save");
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
				String hostname = hostnameTextField.getText();
				String ip = ipTextField.getText();
				int port = 23;
				String username = userTextField.getText();
				String passwd = passwdTextField.getText();
				
				MgtsHost mgts = new MgtsHost(hostname, ip, port, username, passwd);
				mgts.setProtocol((String) protocolList.getSelectedItem());
				mgts.setShelfName(shelfTextField.getText());
				mgts.setDisplay(displayTextField.getText());
				controller.setDisplay(displayTextField.getText());
				controller.setMgtsHost(mgts);				
				mainWindow.dispose();
			}
		});
		saveButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				try {
					saveConfigFile();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainWindow.dispose();
			}
		});
		
		buttonPane.add(setButton);
		buttonPane.add(saveButton);
		buttonPane.add(cancelButton);
	}
	
	public void saveConfigFile() throws IOException {
		String selectedHost = (String) serverList.getSelectedItem();
		Element hostToSave = root.getChild(selectedHost);
		
		hostToSave.getChild("hostname").setText(hostnameTextField.getText());
		hostToSave.getChild("ipaddr").setText(ipTextField.getText());
		hostToSave.getChild("shelf").setText(shelfTextField.getText());
		hostToSave.getChild("user").setText(userTextField.getText());
		hostToSave.getChild("passwd").setText(passwdTextField.getText());
		hostToSave.getChild("display").setText(displayTextField.getText());
		hostToSave.getChild("protocol").setText((String) protocolList.getSelectedItem());
		
		XMLOutputter outputter = new XMLOutputter();
		outputter.setFormat(Format.getPrettyFormat());
		outputter.output(configFile, new FileOutputStream(MGTS_CONFIG_FILE));
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
