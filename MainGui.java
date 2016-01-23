import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import java.awt.event.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainGui {

	public static final int WIDTH = 1000;
	public static final int HEIGHT = 600;
	public static final String DEFAULT_DISPLAY = "135.252.17.202:1.0";

	private XController controller = null;
	//private String baseDir = null;

	/** menu bar */
	private JMenuBar menuBar = null;

	private JMenu menuFile = null;
	private JMenuItem menuOpenFile = null;
	private JMenuItem menuSaveFile = null;

	private JMenu menuConfig = null;
	private JMenuItem menuSetBaseDir = null;	
	private JMenuItem menuHostMgr = null;
	private JMenuItem menuMgtsMgr = null;
	private JMenuItem menuLoadResult = null;
	private JMenuItem menuClearResult = null;
	private JCheckBoxMenuItem  menuSetLoadData = null;
	private JCheckBoxMenuItem  menuReparseLog = null;

	/**
	 * main panel Left part is case run pane, Right part is information pane, to
	 * show host information, and mgts information
	 * */
	private JFrame mainFrame = null;
	private JSplitPane mainPane = null;

	/*
	 * case run pane, includes two parts, top pane is case information in a
	 * table bottom pane is for log output, names console pane
	 */
	private JSplitPane caseRunPane = null;
	private JPanel caseInfoPane = null;
	private JTable caseTable = null;
	private JPanel caseBtnBar = null;
	private JButton loadBtn = null;
	private JButton startBtn = null;
	private JButton stopBtn = null;
	private JLabel baseDirLabel = null;

	private JScrollPane consolePane = null;
	private JTextArea logArea = null;

	/*
	 * info pane, includes two parts top pane is Host information bottom pane is
	 * MGTS information
	 */
	private JSplitPane infoPane = null;
	private JTextArea hostInfo = null;
	private JTextArea mgtsInfo = null;

	public MainGui() {
		controller = new XController();
		// move MgtsManagerGui, because display is useful only for MGTS
		// controller.setDisplay(DEFAULT_DISPLAY);
		controller.setGui(this);

		mainFrame = new JFrame();
		mainFrame.setSize(WIDTH, HEIGHT);
		mainFrame.setTitle("SurePay XAuto");
		
		Image icon = Toolkit.getDefaultToolkit().getImage(
				System.getProperty("user.home") + "/.sptest/logo.gif");
		mainFrame.setIconImage(icon);
		
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize(); 
		int x = (int) screensize.getWidth() / 2 - WIDTH / 2;
		int y = (int) screensize.getHeight() / 2 - HEIGHT / 2;
		mainFrame.setLocation(x, y);

		initMenu();
		initMainPane();

		mainFrame.setContentPane(mainPane);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setVisible(true);
		mainFrame.pack();
	}

	private void initMenu() {
		menuBar = new JMenuBar();

		menuFile = new JMenu("File");
		menuOpenFile = new JMenuItem("Open");
		menuSaveFile = new JMenuItem("Save");
		menuFile.add(menuOpenFile);
		menuFile.add(menuSaveFile);
		menuBar.add(menuFile);

		menuConfig = new JMenu("Configuration");
		menuSetBaseDir = new JMenuItem("Set Base Directory...");		
		menuHostMgr = new JMenuItem("Host Manager...");
		menuMgtsMgr = new JMenuItem("MGTS Manager...");
		menuLoadResult = new JMenuItem("Load Result From Disk");
		menuClearResult = new JMenuItem("Clear Result in Table");
		menuSetLoadData = new JCheckBoxMenuItem ("Load Data", true);
		menuReparseLog = new JCheckBoxMenuItem ("Reparse Log", false);
		menuConfig.add(menuSetBaseDir);
		menuConfig.add(menuHostMgr);
		menuConfig.add(menuMgtsMgr);
		menuConfig.addSeparator();
		menuConfig.add(menuLoadResult);
		menuConfig.add(menuClearResult);
		menuConfig.addSeparator();
		menuConfig.add(menuSetLoadData);
		menuConfig.add(menuReparseLog);
		menuBar.add(menuConfig);

		mainFrame.setJMenuBar(menuBar);

		menuSetBaseDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (controller.isRunning()) {
					showMessageDialog("ERROR: Case is running, stop running case first!");
					return;
				}
				String baseDir = setBaseDir();

				if (baseDir != null) {
					baseDirLabel.setText(baseDir);
					controller.setBaseDir(baseDir);
					printLog("Base Directory is set to: " + baseDir + "\n");
				}
			}

		});

		menuHostMgr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (controller.isRunning()) {
					showMessageDialog("ERROR: Case is running, stop running case first!");
					return;
				}
				setHost();
			}
		});

		menuMgtsMgr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (controller.isRunning()) {
					showMessageDialog("ERROR: Case is running, stop running case first!");
					return;
				}
				setMgts();
			}
		});
		
		menuLoadResult.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (controller.getBaseDir() == null) {
					showMessageDialog("ERROR: Base directory has NOT been set,\n" +
							"Please set base directory first!");
					return;
				}
				
				loadResult();
			}
		});
		
		menuClearResult.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (controller.isRunning()) {
					showMessageDialog("ERROR: Can't clear table when cases are running!");
					return;
				}
				clearResult();
			}
		});
		/*
		menuSetLoadData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.toggleFlagLoadData();
				menuSetLoadData.setText("LoadData :  "
						+ controller.getLoadDataFlag());
			}
		});
		
		menuReparseLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.toggleReparseLogFlag();
				menuReparseLog.setText("Reparse Log :  "
						+ controller.getReparseLogFlag());
			}
		});
		 */
	}

	private void initMainPane() {
		mainPane = new JSplitPane();

		mainPane.setOneTouchExpandable(true);
		mainPane.setContinuousLayout(true);
		mainPane.setPreferredSize(new Dimension(mainFrame.getWidth(), mainFrame
				.getHeight()));
		mainPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		mainPane.setDividerSize(3);
		mainPane.setDividerLocation((mainFrame.getWidth() * 3) / 4);

		initCaseRunPane();
		initInfoPane();

		mainPane.setLeftComponent(caseRunPane);
		mainPane.setRightComponent(infoPane);
	}

	private void initCaseRunPane() {
		/**
		 * Initialize case run pane, includes two parts: caseInfo on top,
		 * console pane(log Area) at the bottom
		 */
		caseRunPane = new JSplitPane();
		caseRunPane.setOneTouchExpandable(true);
		caseRunPane.setContinuousLayout(true);
		caseRunPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		caseRunPane.setDividerSize(2);
		caseRunPane.setDividerLocation((mainFrame.getHeight() * 3) / 4);

		caseInfoPane = new JPanel();
		caseInfoPane.setLayout(new BorderLayout());

		/* Add case table to top of case run pane */
		initCaseTable();
		JScrollPane caseTablePane = new JScrollPane(caseTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		caseInfoPane.add(caseTablePane, BorderLayout.CENTER);

		/* Add the button bar to the bottom of case table */
		caseBtnBar = new JPanel();
		loadBtn = new JButton("Load Case");
		startBtn = new JButton("Start");
		stopBtn = new JButton("Stop");

		loadBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (controller.isRunning()) {
					showMessageDialog("ERROR: Case is running, stop running case first!");
					return;
				}
				String baseDir = controller.getBaseDir();
				JFileChooser caseFileChooser = new JFileChooser(baseDir);
				caseFileChooser.setDialogTitle("Choose Case File");
				int chooseResult = caseFileChooser
						.showOpenDialog(caseFileChooser);
				if (chooseResult == JFileChooser.APPROVE_OPTION) {
					File caseListFile = caseFileChooser.getSelectedFile();
					loadCaseFile(caseListFile);
					printLog("Load caselist\n");
				}
			}
		});
		
		startBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!controller.isRunning()) {
					clearResult();
					controller.startToRunCase();
				} else {
					showMessageDialog("ERROR: Case is running!");
				}
			}
		});
		
		stopBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.setRunningFlag(false);
			}
		});

		baseDirLabel = new JLabel("Base Directory is not set!");
		caseBtnBar.add(loadBtn);
		caseBtnBar.add(startBtn);
		caseBtnBar.add(stopBtn);
		caseBtnBar.add(new JLabel("Base Dir: "));
		caseBtnBar.add(baseDirLabel);
		caseInfoPane.add(caseBtnBar, BorderLayout.SOUTH);

		caseRunPane.setLeftComponent(caseInfoPane);

		/* Add consolePane to the bottom */
		logArea = new JTextArea();
		logArea.setEditable(false);
		logArea.setText("Log will be output here\n");
		logArea.setCaretPosition(logArea.getText().length());
		consolePane = new JScrollPane(logArea,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		caseRunPane.setRightComponent(consolePane);
	}

	private void initCaseTable() {
		/* Define a table cell renderer 
		DefaultTableCellRenderer tcr = new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {

				if (row % 2 == 0) {
					setBackground(Color.WHITE);
				} else {
					setBackground(Color.LIGHT_GRAY);
				}
				setHorizontalAlignment(JLabel.CENTER);

				return super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);

			}
		};
		*/
		CaseTableCellRenderer tcr = new CaseTableCellRenderer();
		CaseTableModel tableModel = new CaseTableModel();
		caseTable = new JTable(tableModel);
		caseTable.setShowVerticalLines(false);
		caseTable.setAutoscrolls(true);
		caseTable.setAlignmentX(Component.CENTER_ALIGNMENT);
		caseTable.setAlignmentY(Component.CENTER_ALIGNMENT);
		caseTable.setDefaultRenderer(Object.class, tcr);
		caseTable.getTableHeader().setReorderingAllowed(false);
		caseTable.getColumnModel().getColumn(0)
				.setHeaderRenderer(new TableHeaderRenderer(caseTable));
		caseTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (controller.getBaseDir() == null) {
					showMessageDialog("ERROR: Base directory has NOT been set,\n" +
							"Please set base directory first!");
					return;
				}
				
				int row = caseTable.rowAtPoint(e.getPoint());
				int col = caseTable.columnAtPoint(e.getPoint());
				if ((col != CaseTableModel.COLUMN_RESULT) && (col != CaseTableModel.COLUMN_TID)) {
					return;
				}
				
				if (col == CaseTableModel.COLUMN_TID) {
					showCaseInfo(row);
					return;
				}

				if (getReparseLogFlag()) {
					parseLog(row);
				}
				
				String result = (String) caseTable.getValueAt(row, col);
				if (result.equals("NO_PARSE") || result.equals("FAIL_PARSE")) {
					parseLog(row);
					result = (String) caseTable.getValueAt(row, col);
				}
				
				if (!(result.equals("PASS") || result.equals("FAIL"))) {
					return;
				}
				
				JTextPane resultInfo = new JTextPane();
				resultInfo.setEditable(false);
				String tid = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_TID);
				String rel = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_RELEASE);
				String customer = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_CUSTOMER);

				String parseResultName = null;
				if (result.equals("PASS")) {
					parseResultName = controller.getBaseDir() + "/" + customer 
							+ "/" + rel + "/log/" + tid + ".PASS";
				} else {
					parseResultName = controller.getBaseDir() + "/" + customer 
							+ "/" + rel + "/faillog/" + tid + ".FAIL";
				}
				File parseResultFile = new File(parseResultName);
					
				Document doc = resultInfo.getDocument();
				SimpleAttributeSet attrSuc = new SimpleAttributeSet();
				SimpleAttributeSet attrFail = new SimpleAttributeSet();						
				StyleConstants.setForeground(attrSuc, Color.BLACK);
				StyleConstants.setForeground(attrFail, Color.RED);
					
				if (parseResultFile.exists()) {
					try {
						BufferedReader br = new BufferedReader(new FileReader(parseResultFile));
						Pattern p = Pattern.compile("^Z[0-9]+-[0-9]+:");
						String line = null;
						while ((line = br.readLine()) != null) {
							SimpleAttributeSet attrSet = attrSuc;
							Matcher m = p.matcher(line);
							boolean isZEqual = true;
							if (m.find()) {
								int scroll = line.indexOf("-");
								int total = Integer.parseInt(line.substring(1, scroll));
								int number = Integer.parseInt(line.substring(scroll + 1, m.end() - 1));
								isZEqual = number == total ? true : false;
							}
							if (!isZEqual || line.endsWith("NOTFOUND") ||
									line.endsWith("NOTEQUAL") || line.contains("subscript at")) {
								attrSet = attrFail;
							}
							doc.insertString(doc.getLength(), line + "\n", attrSet);
						}
						br.close();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else {
					SimpleAttributeSet attrSet = attrFail;
					try {
						String str = "ERROR: Result parse file not exist!\n" +
								"   Please check result file's existence under res dir\n" +
								"   if exist, open \"Reparse Log\" under Configuration menu, " +
								"then click again.";
						doc.insertString(doc.getLength(), str, attrSet);
					} catch (BadLocationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}	
				
				JScrollPane resultPane = new JScrollPane(resultInfo,
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				//JFrame resultWindow = new JFrame(parseResultFile.getAbsolutePath());
				JDialog resultWindow = new JDialog(mainFrame, parseResultFile.getAbsolutePath(), true);
				resultWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				resultWindow.add(resultPane);
				resultWindow.setSize(600, 400);
				Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize(); 
				int x = (int) screensize.getWidth() / 2 - 600 / 2;
				int y = (int) screensize.getHeight() / 2 - 400 / 2;
				resultWindow.setLocation(x, y);
				resultWindow.setVisible(true);
			}
			
			public void mouseExited(MouseEvent e) {
				mainPane.setCursor(Cursor.getDefaultCursor());
			}
		});
		
		caseTable.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				int row = caseTable.rowAtPoint(e.getPoint());
				int col = caseTable.columnAtPoint(e.getPoint());
				String result = null;
				if (col == CaseTableModel.COLUMN_RESULT) {
					result = (String) caseTable.getValueAt(row, col);
				}
								
				if (col == CaseTableModel.COLUMN_RESULT 
						&& (result.equals("PASS") 
								|| result.equals("FAIL") 
								|| result.equals("NO_PARSE")
								|| result.equals("FAIL_PARSE"))) {
					mainPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				} else if (col == CaseTableModel.COLUMN_TID){
					mainPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				} else {
					mainPane.setCursor(Cursor.getDefaultCursor());
				}
			}
		});
	}

	private void initInfoPane() {
		infoPane = new JSplitPane();
		infoPane.setContinuousLayout(true);
		infoPane.setOneTouchExpandable(true);
		infoPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		infoPane.setDividerSize(3);
		infoPane.setDividerLocation((mainFrame.getHeight() * 2) / 3);

		// Because host information is long
		// So put it into a scroll pane first
		hostInfo = new JTextArea("No host connected!");
		hostInfo.setAutoscrolls(true);
		hostInfo.setEditable(false);
		JScrollPane hostInfoPane = new JScrollPane(hostInfo,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		mgtsInfo = new JTextArea("No MGTS server configured");
		mgtsInfo.setEditable(false);

		infoPane.setLeftComponent(hostInfoPane);
		infoPane.setRightComponent(mgtsInfo);

	}

	public String setBaseDir() {
		JFileChooser baseDirChooser = new JFileChooser(".");
		baseDirChooser.setDialogTitle("Choose Base Dir");
		baseDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		baseDirChooser.setApproveButtonText("Set");
		int chooseResult = baseDirChooser.showOpenDialog(baseDirChooser);
		String baseDir = null;
		if (chooseResult == JFileChooser.APPROVE_OPTION) {
			baseDir = baseDirChooser.getSelectedFile().toString();
		}
		
		return baseDir;
	}

	public void loadCaseFile(File caseListFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(caseListFile));
			String line = null;
			Vector<Object> caseToAdd = null;
			CaseTableModel caseTableModel = (CaseTableModel) caseTable
					.getModel();

			clearCaseTable();

			while ((line = br.readLine()) != null) {
				if (line.startsWith("#") || line.isEmpty()) {
					continue;
				}

				caseToAdd = getCaseFromStr(line);
				caseTableModel.addRow(caseToAdd);
				// caseTableModel.fireTableDataChanged();
			}

			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void clearCaseTable() {
		CaseTableModel caseTableModel = (CaseTableModel) caseTable.getModel();
		while (caseTableModel.getRowCount() != 0) {
			caseTableModel.removeRow(caseTableModel.getRowCount() - 1);
		}
		CaseTableCellRenderer tcr = 
				(CaseTableCellRenderer) caseTable.getDefaultRenderer(Object.class);
		tcr.resetRunningRow();
		caseTableModel.fireTableDataChanged();
	}

	private Vector<Object> getCaseFromStr(String str) {
		Vector<Object> caseToAdd = new Vector<Object>();
		caseToAdd.add(new Boolean(false));
		int rowCount = caseTable.getModel().getRowCount();
		int columnCount = caseTable.getModel().getColumnCount();
		caseToAdd.add(Integer.toString(rowCount + 1));

		String[] sArray = str.split("\\s+", columnCount - 5);

		for (int i = 0; i < sArray.length; i++) {
			caseToAdd.add(sArray[i]);
		}

		caseToAdd.add("Not Run");
		caseToAdd.add("10s");
		caseToAdd.add("NA");

		return caseToAdd;
	}

	public void setCaseStatus(int x, int y, String status) {

	}

	public void setCaseRunTime(int x, int y, int time) {

	}

	public void setCaseResult(int x, int y, String result) {

	}

	public void printLog(String str) {
		logArea.append(str);
		logArea.setCaretPosition(logArea.getText().length());
	}

	public void clearLog() {
		logArea.setText("");
	}

	public void setHostInfo(String info) {
		hostInfo.setText(info);
	}

	public void setMgtsInfo(String info) {
		mgtsInfo.setText(info);
	}

	public void setController(XController c) {
		controller = c;
	}
	
	public XController getController() {
		return controller;
	}
	
	public JFrame getMainFrame() {
		return mainFrame;
	}

	public void setHost() {
		/*
		if (controller.getHost() == null) {
			String hostname = "SPVM53";
			String ip = "135.242.106.116";
			int port = 23;
			String username = "ainet";
			String passwd = "ainet1";

			Host host = new Host(hostname, ip, port, username, passwd);
			controller.setHost(host);
		} else {
			printLog("host has been set to "
					+ controller.getHost().getHostName() + "\n");
		}
		*/
		File hostFile = new File(System.getProperty("user.home") + "/.sptest/host.xml");
		if (hostFile.exists()) {
			new HostManagerGui(controller);
		} else {
			showMessageDialog("ERROR: " + hostFile.getAbsolutePath() + " doesn't exist!\n" +
					"Host is not set");
		}
		
	}

	public void setMgts() {
		/*
		if (controller.getMgts() == null) {
			String hostname = "p250alu";
			String ip = "135.252.170.143";
			int port = 23;
			String username = "yrli";
			String passwd = "yrli";

			MgtsHost mgts = new MgtsHost(hostname, ip, port, username, passwd);
			mgts.setProtocol("ITU");
			mgts.setShelfName("EE");
			controller.setMgtsHost(mgts);
		} else {
			showMessageDialog("MGTS SERVER has been set to "
					+ controller.getMgts().getHostName() + "\n");
		}
		*/
		new MgtsManagerGui(controller);

	}

	public JTable getCaseTable() {
		return caseTable;
	}
	
	public void showMessageDialog(String str) {
		JOptionPane.showMessageDialog(mainFrame, str);
	}
	
	public int showConfirmDialog(String title, String message) {
		return JOptionPane.showConfirmDialog(mainFrame, message, title, JOptionPane.YES_NO_OPTION);
	}
	
	public Case getCaseFromGui(int row) {
		String tid = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_TID);
		String fid = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_FEATURE_ID);
		String rel = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_RELEASE);
		String customer = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_CUSTOMER);
		String caseType = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_CASE_TYPE);
		String basedata = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_BASE_DATA);
		Case testCase = new Case(tid, fid, rel, customer, caseType,
				basedata);
		
		return testCase;
	}
	
	public void showCaseInfo(int row) {
		JTextPane caseInfoPane = new JTextPane();
		caseInfoPane.setEditable(false);
		String tid = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_TID);
		String rel = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_RELEASE);
		String customer = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_CUSTOMER);
		StringBuilder caseInfo = new StringBuilder();
		
		File cmdFile = new File(controller.getBaseDir() + "/" + customer 
				+ "/" + rel + "/res/" + tid + ".cmd" );		
		try {
			BufferedReader br = new BufferedReader(new FileReader(cmdFile));
			String line = null;
			while ((line = br.readLine()) != null) {
				caseInfo.append(line + "\n");
			}
			br.close();
		} catch (FileNotFoundException e) {
			caseInfo.append("File " + cmdFile.getName() + " NOT exist, run state machine " + tid);
		} catch (IOException e) {
			caseInfo.append("Read error");			
		}
		
		caseInfoPane.setText(caseInfo.toString());
		JDialog caseInfoWindow = new JDialog(mainFrame,tid + " information", true);
		caseInfoWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		caseInfoWindow.add(caseInfoPane);
		caseInfoWindow.setSize(600, 400);
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize(); 
		int x = (int) screensize.getWidth() / 2 - 600 / 2;
		int y = (int) screensize.getHeight() / 2 - 400 / 2;
		caseInfoWindow.setLocation(x, y);
		
		caseInfoWindow.setVisible(true);		
	}
	
	public void parseLog(int row) {
		Case caseToParse = getCaseFromGui(row);
		String result = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_RESULT);
		if (result.equals("NA")) {
			this.showMessageDialog("Log does not exist for case " +
					caseTable.getValueAt(row, CaseTableModel.COLUMN_TID));
			return;
		}
		
		boolean origParseResult = true;
		if (result.equals("FAIL")) {
			origParseResult = false;
		} 
		
		boolean newParseResult = false;
		try {
			newParseResult = controller.parseLog(caseToParse, origParseResult);
			result = newParseResult ? "PASS" : "FAIL";
		} catch (CantParseException e) {
			result = "FAIL_PARSE";
		}
		
		caseTable.setValueAt(result, row, CaseTableModel.COLUMN_RESULT);		
	}
	
	public void loadResult() {
		String baseDir = controller.getBaseDir();
		int rowCount = caseTable.getRowCount();
		for (int row = 0; row < rowCount; row++) {
			String tid = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_TID);
			String rel = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_RELEASE);
			String customer = (String) caseTable.getValueAt(row, CaseTableModel.COLUMN_CUSTOMER);
			String result = "NA";
			File successLog = new File(baseDir + "/" + customer
					+ "/" + rel + "/log/" + tid + ".log");
			if (successLog.exists()) {
				File parseResultFile = new File(baseDir + "/" + customer
						+ "/" + rel + "/log/" + tid + ".PASS");
				if (parseResultFile.exists()) {
					result = "PASS";
				} else {
					result = "NO_PARSE";
				}
				
			} else {
				File failLog = new File(baseDir + "/" + customer 
						+ "/" + rel + "/faillog/" + tid + ".log");
				if (failLog.exists()) {
					result = "FAIL";
				}
			}
			
			caseTable.setValueAt(result, row, CaseTableModel.COLUMN_RESULT);
		}		
	}
	
	public void clearResult() {
		int rowCount = caseTable.getRowCount();
		String result = "NA";
		
		for (int row = 0; row < rowCount; row++) {
			caseTable.setValueAt(result, row, CaseTableModel.COLUMN_RESULT);
		}
		CaseTableModel tableModel = (CaseTableModel) caseTable.getModel();
		tableModel.fireTableDataChanged();
	}
	
	public boolean getLoadDataFlag() {
		return menuSetLoadData.getState();
	}
	
	public boolean getReparseLogFlag() {
		return menuReparseLog.getState();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new MainGui();
	}

}

enum ParseResult {
	PASS, FAIL, NO_PARSE, FAIL_PARSE, NA
}

class CaseTableModel extends AbstractTableModel {
	public static final int COLUMN_SELECTED = 0;
	public static final int COLUMN_NO = 1;
	public static final int COLUMN_TID = 2;
	public static final int COLUMN_FEATURE_ID = 3;
	public static final int COLUMN_RELEASE = 4;
	public static final int COLUMN_CUSTOMER = 5;
	public static final int COLUMN_CASE_TYPE = 6;
	public static final int COLUMN_BASE_DATA = 7;
	public static final int COLUMN_STATUS = 8;
	public static final int COLUMN_TIME_USED = 9;
	public static final int COLUMN_RESULT = 10;

	Object[] colNameSource = { "", "No.", "Tid", "Fid", "Release", "Customer",
			"CaseType", "BaseData", "Status", "Time Used", "Result" };
	Vector<Object> colName = new Vector<Object>();
	Vector<Object> data;

	public CaseTableModel() {
		data = new Vector<Object>();

		for (int i = 0; i < colNameSource.length; i++) {
			colName.add(colNameSource[i]);
		}

	}

	public int getColumnCount() {
		return colName.size();
	}

	public int getRowCount() {
		return data.size();
	}

	public Object getValueAt(int row, int col) {
		return ((Vector<Object>) data.get(row)).get(col);
	}

	public Class<?> getColumnClass(int col) {
		return getValueAt(0, col).getClass();
	}

	public String getColumnName(int column) {
		if (column == CaseTableModel.COLUMN_SELECTED) {
			return "Select All";
		}

		return colName.get(column).toString();
	}

	public void setValueAt(Object value, int row, int col) {
		((Vector<Object>) data.get(row)).set(col, value);
		fireTableCellUpdated(row, col);
	}

	public boolean isCellEditable(int row, int col) {

		if (col == CaseTableModel.COLUMN_SELECTED) {
			return true;
		}

		return false;
	}

	public void selectAll(boolean selected) {
		int len = getRowCount();
		for (int i = 0; i < len; i++) {
			setValueAt(Boolean.valueOf(selected), i, CaseTableModel.COLUMN_SELECTED);
		}
	}

	public void addRow(Vector<Object> rowToAdd) {
		if (rowToAdd.size() == colName.size()) {
			data.add(rowToAdd);
			fireTableDataChanged();
		} else {
			System.out
					.println("invalid row to add: number of column doesnt match!");
		}
	}

	public void removeRow(int row) {
		if (row >= 0 && row < getRowCount()) {
			data.remove(row);
			fireTableDataChanged();
		}
	}
}

class TableHeaderRenderer implements TableCellRenderer {
	JCheckBox selectAll;
	JTableHeader tableHeader;
	CaseTableModel tableModel;

	public TableHeaderRenderer(JTable table) {
		selectAll = new JCheckBox();
		this.tableHeader = table.getTableHeader();
		tableModel = (CaseTableModel) table.getModel();

		table.getTableHeader().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int column = tableHeader.columnAtPoint(e.getPoint());
				if (column == CaseTableModel.COLUMN_SELECTED) {
					boolean selected = !selectAll.isSelected();
					selectAll.setSelected(selected);
					tableHeader.repaint();
					tableModel.selectAll(selected);
				}
			}
		});
		table.getColumnModel().getColumn(CaseTableModel.COLUMN_SELECTED)
				.setPreferredWidth(selectAll.getWidth());
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		JComponent component;

		if (column == CaseTableModel.COLUMN_SELECTED) {
			component = selectAll;
			selectAll.setHorizontalAlignment(SwingConstants.CENTER);
		} else {
			component = (JLabel) value;
		}

		return component;
	}

}

class CaseTableCellRenderer extends DefaultTableCellRenderer {
	private int runningRow;
	
	public CaseTableCellRenderer() {
		runningRow = -1;
	}
	
	public int getRunningRow() {
		return runningRow;
	}
	
	public void setRunningRow(int row) {
		runningRow = row;
	}
	
	public void resetRunningRow() {
		runningRow = -1;
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		if(row % 2 == 0) {
			setBackground(Color.white);
		} else{
			setBackground(Color.LIGHT_GRAY);
		}	
		
		setHorizontalAlignment(JLabel.CENTER);
		
		if (row == runningRow) {
			setBackground(Color.GRAY);
		}
		
		if (column == CaseTableModel.COLUMN_RESULT) {
			String result = (String) value;
			if (result.equals("PASS")) {
				setForeground(Color.GREEN);
			}
			if (result.equals("FAIL")) {
				setForeground(Color.RED);
			}
			if (result.equals("NO_PARSE") || result.equals("FAIL_PARSE")) {
				setForeground(Color.ORANGE);
			}
		} else {
			setForeground(Color.BLACK);
		}
				
		return super.getTableCellRendererComponent(table, value, 
				isSelected, hasFocus, row, column);		
	}
	
}
