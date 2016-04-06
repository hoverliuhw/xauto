import acm.gui.TableLayout;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.*;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class HostManagerGui {

	public final static int APPROVE_OPTION = 1;
	public final static int CONNECT_OPTION = 2;
	public final static int CANCEL_OPTION = 3;

	public final static int WIDTH = 600;
	public final static int HEIGHT = 500;
	public JDialog mainWindow = null;
	private JTree hostTree = null;
	private JSplitPane mainPane = null;
	private JScrollPane treePane = null;
	private Element hostRoot = null;

	private JPanel displayPane = null;
	private JPanel propertyPane = null;
	private JPanel buttonBar = null;

	/* Components for display */
	private JTextField hostNameText = null;
	private JTextField ipAddrText = null;
	private JTextField userNameText = null;
	private JPasswordField passwdText = null;
	private JPasswordField suPasswdText = null;
	private JTextField ftpUserText = null;
	private JPasswordField ftpPasswdText = null;
	private JTextField conType = null;
	private JTextField hostType = null;
	private JTextField hwType = null;
	private JTextField hdMode = null;
	private JTextField mynode = null;

	private boolean firstDisplayed = true;

	private JButton connectbutton = null;
	private JButton cancelbutton = null;
	private JButton okbutton = null;
	
	private XController controller = null;

	private void initTextFields() {

		hostNameText = new JTextField(15);
		ipAddrText = new JTextField(15);
		userNameText = new JTextField(15);
		passwdText = new JPasswordField(15);
		suPasswdText = new JPasswordField(15);
		ftpUserText = new JTextField(15);
		ftpPasswdText = new JPasswordField(15);
		conType = new JTextField(15);
		hostType = new JTextField(15);
		hwType = new JTextField(15);
		hdMode = new JTextField(15);
		mynode = new JTextField(30);

		propertyPane.add(new JLabel("Host Name:\t"));
		propertyPane.add(hostNameText);
		propertyPane.add(new JLabel("IP Address:\t"));
		propertyPane.add(ipAddrText);
		propertyPane.add(new JLabel("Username:\t"));
		propertyPane.add(userNameText);
		propertyPane.add(new JLabel("Passwd:\t"));
		propertyPane.add(passwdText);
		propertyPane.add(new JLabel("su Passwd:\t"));
		propertyPane.add(suPasswdText);
		propertyPane.add(new JLabel("FTP user:\t"));
		propertyPane.add(ftpUserText);
		propertyPane.add(new JLabel("FTP Passwd:\t"));
		propertyPane.add(ftpPasswdText);
		propertyPane.add(new JLabel("Connection Type:\t"));
		propertyPane.add(conType);
		propertyPane.add(new JLabel("Host Type:\t"));
		propertyPane.add(hostType);
		propertyPane.add(new JLabel("Hardware Type:\t"));
		propertyPane.add(hwType);
		propertyPane.add(new JLabel("Hardware Mode:\t"));
		propertyPane.add(hdMode);
		propertyPane.add(new JLabel("Mynode:\t"));
		propertyPane.add(mynode);

		mainPane.setDividerLocation(mainWindow.getWidth() / 4);

		firstDisplayed = false;
	}

	public void initHostPane() {
		displayPane = new JPanel();
		displayPane.setLayout(new BorderLayout());
		propertyPane = new JPanel();
		propertyPane.setLayout(new TableLayout(13, 2));
		buttonBar = new JPanel();
		displayPane.add(propertyPane, BorderLayout.CENTER);
		displayPane.add(buttonBar, BorderLayout.SOUTH);

		connectbutton = new JButton("Connect");
		okbutton = new JButton("OK");
		cancelbutton = new JButton("Cancel");

		connectbutton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (hostNameText == null || (hostNameText.getText() == null) || hostNameText.getText().isEmpty()) {
					JOptionPane.showMessageDialog(mainWindow, "ERROR: No host is selected");
					return;
				}
				
				String hostname = hostNameText.getText();
				Element selectedHost = hostRoot.getChild(hostname);
				String ip = selectedHost.getChildText("ipaddr");
				String username = selectedHost.getChildText("user");
				String passwd = selectedHost.getChildText("passwd");
				int port = 23;
				
				Host host = new Host(hostname, ip, port, username, passwd);
				
				if (controller == null) {
					return;
				}
				
				controller.setHost(host);
				if (controller.getHost() == host) {
					mainWindow.dispose();
				}
			}
		});
		okbutton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainWindow.dispose();
			}
		});
		cancelbutton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainWindow.dispose();
			}
		});

		buttonBar.add(connectbutton);
		buttonBar.add(okbutton);
		buttonBar.add(cancelbutton);

	}

	public void showHost(String hostName) {
		if (firstDisplayed) {
			initTextFields();
		}

		Element selectedHost = hostRoot.getChild(hostName);
		hostNameText.setText(selectedHost.getChildText("hostname"));
		ipAddrText.setText(selectedHost.getChildText("ipaddr"));
		userNameText.setText(selectedHost.getChildText("user"));
		passwdText.setText(selectedHost.getChildText("passwd"));
		suPasswdText.setText(selectedHost.getChildText("supasswd"));
		ftpUserText.setText(selectedHost.getChildText("ftp_user"));
		ftpPasswdText.setText(selectedHost.getChildText("ftp_passwd"));
		conType.setText(selectedHost.getChildText("conn_type"));
		hostType.setText(selectedHost.getChildText("host_type"));
		hwType.setText(selectedHost.getChildText("hardware_type"));
		hdMode.setText(selectedHost.getChildText("hardware_mode"));
		mynode.setText(selectedHost.getChildText("mynode"));
	}

	public HostManagerGui(XController controller) {
		this.controller = controller;
		JFrame parentWindow = null;
		if (controller !=null && controller.gui != null) {
			parentWindow = controller.gui.getMainFrame();
		}
		mainWindow = new JDialog(parentWindow, "Host Manager", true);
		mainWindow.setSize(WIDTH, HEIGHT);
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize(); 
		int x = (int) screensize.getWidth() / 2 - WIDTH / 2;
		int y = (int) screensize.getHeight() / 2 - HEIGHT / 2;
		mainWindow.setLocation(x, y);

		mainPane = new JSplitPane();
		mainPane.setOneTouchExpandable(false);
		mainPane.setPreferredSize(new Dimension(mainWindow.getWidth(), mainWindow
				.getHeight()));
		mainPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		mainPane.setDividerSize(3);
		mainPane.setDividerLocation(mainWindow.getWidth() / 4);
		mainPane.setEnabled(false);

		/* set left part */
		initTree();

		treePane = new JScrollPane(hostTree,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		mainPane.setLeftComponent(treePane);

		/* set right part */
		initHostPane();
		mainPane.setRightComponent(displayPane);

		mainWindow.setContentPane(mainPane);
		mainWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		mainWindow.setVisible(true);
		mainWindow.dispose();
	}

	public void setVisible(boolean visible) {
		mainWindow.setVisible(visible);
		mainWindow.pack();
	}

	private void initTree() {
		SAXBuilder builder = new SAXBuilder();
		try {
			Document hostFile = builder.build(new File(System.getProperty("user.home") + "/.sptest/host.xml"));
			hostRoot = hostFile.getRootElement();
		} catch (Exception e) {
			e.printStackTrace();
		}

		DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Hosts");
		List<Element> list = hostRoot.getChildren();
		for (int i = 0; i < list.size(); i++) {
			treeRoot.add(new DefaultMutableTreeNode(list.get(i).getName()));
		}

		hostTree = new JTree(treeRoot);

		hostTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) e
						.getPath().getLastPathComponent();
				if (selectedNode != hostTree.getModel().getRoot()) {
					showHost(selectedNode.toString());
				}
			}
		});
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HostManagerGui gui = new HostManagerGui(null);
		//gui.setVisible(true);
	}

}