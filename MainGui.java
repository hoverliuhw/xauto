/**
 **************************************************************************
 * Name: MainGui
 * 
 * Description: Main GUI of XAuto 
 * Author: Liu Hongwei
 * 		   hong_wei.hl.liu@alcatel-lucent.com
 * 
 *************************************************************************
 */
 
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

enum ParseResult {
	PASS, FAIL, NO_PARSE, FAIL_PARSE, FAIL_GETLOG, NA
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
		return col == CaseTableModel.COLUMN_SELECTED;
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

class SelectAllTableHeaderRenderer implements TableCellRenderer {
	JCheckBox selectAll;
	JTableHeader tableHeader;
	CaseTableModel tableModel;

	public SelectAllTableHeaderRenderer(JTable table) {
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

class GeneralTableHeaderRenderer implements TableCellRenderer {
	DefaultTableCellRenderer renderer;
	
	GeneralTableHeaderRenderer(JTable caseTable) {
		renderer = (DefaultTableCellRenderer) 
				caseTable.getTableHeader().getDefaultRenderer();
		renderer.setHorizontalAlignment(JLabel.CENTER);		
	}
	
	public Component getTableCellRendererComponent(
	        JTable table, Object value, boolean isSelected,
	        boolean hasFocus, int row, int col) {
		return renderer.getTableCellRendererComponent(
	            table, value, isSelected, hasFocus, row, col);
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
			ParseResult result = (ParseResult) value;
			if (result == ParseResult.PASS) {
				setForeground(Color.GREEN);
			}
			if (result == ParseResult.FAIL) {
				setForeground(Color.RED);
			}
			if (result == ParseResult.NO_PARSE
					|| result == ParseResult.FAIL_PARSE
					|| result == ParseResult.FAIL_GETLOG) {
				setForeground(Color.ORANGE);
			}
		} else {
			setForeground(Color.BLACK);
		}
				
		return super.getTableCellRendererComponent(table, value, 
				isSelected, hasFocus, row, column);		
	}
	
}

public class MainGui {

	public static final int WIDTH = 1024;
	public static final int HEIGHT = 640;
	public static final String DEFAULT_DISPLAY = "135.252.17.202:1.0";

	private XController controller = null;

	/** menu bar */
	private JMenuBar menuBar = null;

	private JMenu menuFile = null;
	private JMenuItem menuOpenFile = null;
	private JMenuItem menuSaveFile = null;
	private JMenuItem menuExit = null;

	private JMenu menuConfig = null;
	private JMenuItem menuSetBaseDir = null;
	private JMenuItem menuHostMgr = null;
	private JMenuItem menuMgtsMgr = null;
	private JCheckBoxMenuItem menuSetLoadData = null;
	private JCheckBoxMenuItem menuReparseLog = null;
	private JCheckBoxMenuItem menuUseDynamicFrmbk = null;
	
	private JMenu menuTools = null;
	private JMenuItem menuLoadResult = null;
	private JMenuItem menuClearResult = null;
	private JMenuItem menuResultStatistics = null;
	private JMenuItem menuShowCaseFrm = null;
	
	private JMenu menuHelp = null;
	private JMenuItem menuManual = null;
	private JMenuItem menuAboutXAuto = null;

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
		
		mainFrame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				mainPane.setDividerLocation((mainFrame.getWidth() * 3) / 4);
				caseRunPane.setDividerLocation((mainFrame.getHeight() * 3) / 4);
				infoPane.setDividerLocation((mainFrame.getHeight() * 2) / 3);
			}
		});
		mainFrame.setVisible(true);
		mainFrame.pack();
	}

	private void initMenu() {
		menuBar = new JMenuBar();

		menuFile = new JMenu("File");
		menuOpenFile = new JMenuItem("Open");
		menuSaveFile = new JMenuItem("Save");
		menuExit = new JMenuItem("Exit");
		menuFile.add(menuOpenFile);
		menuFile.add(menuSaveFile);
		menuFile.add(menuExit);
		menuBar.add(menuFile);

		menuConfig = new JMenu("Configuration");
		menuSetBaseDir = new JMenuItem("Set Base Directory...");		
		menuHostMgr = new JMenuItem("Host Manager...");
		menuMgtsMgr = new JMenuItem("MGTS Manager...");
		menuSetLoadData = new JCheckBoxMenuItem ("Load Data", true);
		menuReparseLog = new JCheckBoxMenuItem ("Reparse Log", false);
		menuUseDynamicFrmbk = new JCheckBoxMenuItem("Use Dynamic Frmbk", false);
		
		menuConfig.add(menuSetBaseDir);
		menuConfig.add(menuHostMgr);
		menuConfig.add(menuMgtsMgr);
		menuConfig.addSeparator();
		menuConfig.add(menuSetLoadData);
		menuConfig.add(menuReparseLog);
		menuConfig.add(menuUseDynamicFrmbk);
		menuBar.add(menuConfig);
		
		menuTools = new JMenu("Tools");
		menuLoadResult = new JMenuItem("Load Result From Disk");
		menuClearResult = new JMenuItem("Clear Result in Table");
		menuResultStatistics = new JMenuItem("Result Statistics");
		menuShowCaseFrm = new JMenuItem("Show Case Frm/Frmbk");
		menuTools.add(menuLoadResult);
		menuTools.add(menuClearResult);
		menuTools.add(menuResultStatistics);
		menuTools.add(menuShowCaseFrm);
		menuBar.add(menuTools);
		
		menuHelp = new JMenu("Help");
		menuManual = new JMenuItem("Manual...");
		menuAboutXAuto = new JMenuItem("About XAuto...");
		menuHelp.add(menuManual);
		menuHelp.add(menuAboutXAuto);
		menuBar.add(menuHelp);

		mainFrame.setJMenuBar(menuBar);

		menuExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainFrame.dispose();
			}
		});
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
		
		menuUseDynamicFrmbk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (controller.isRunning()) {
					boolean oldState = !menuUseDynamicFrmbk.getState();
					menuUseDynamicFrmbk.setState(oldState);
					showMessageDialog("ERROR: RCtracker can't be switched on/off when case is running");
				}
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
		
		menuResultStatistics.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getResultStatistics();
			}
		});
		
		menuShowCaseFrm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int row = caseTable.getSelectedRow();
				if (row < 0) {
					showMessageDialog("ERROR: No case is selected!");
				} else {
					Case caseToShow = getCaseFromGui(row);
					showCaseFrm(caseToShow);
				}
			}
		});
		
		menuAboutXAuto.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showMessageDialog("\nXAuto for Surepay Automation\n\n" +
						"Version: 0.1\n" +
						"Contact: Hong_Wei.hl.Liu@alcatel-lucent.com\n\n" +
						"(c) Copyright XAuto contributors 2015, 2016.  All rights reserved.");
			}
		});

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
				//controller.setRunningFlag(false);
				if (controller.isRunning() && !controller.isStopClicked()) {
					controller.setStopClicked(true);
					printLog("Stop running after finishing current case\n");
				}
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
		caseTable.getTableHeader().setDefaultRenderer(new GeneralTableHeaderRenderer(caseTable));
		caseTable.getColumnModel().getColumn(0)
				.setHeaderRenderer(new SelectAllTableHeaderRenderer(caseTable));

		caseTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int row = caseTable.rowAtPoint(e.getPoint());
				int col = caseTable.columnAtPoint(e.getPoint());
				if ((col != CaseTableModel.COLUMN_RESULT) && (col != CaseTableModel.COLUMN_TID)) {
					return;
				}
				
				if (controller.getBaseDir() == null) {
					showMessageDialog("ERROR: Base directory has NOT been set,\n" +
							"Please set base directory first!");
					return;
				}
				
				Case caseToShow = getCaseFromGui(row);
				if (col == CaseTableModel.COLUMN_TID) {
					showCaseInfo(caseToShow);
					return;
				}

				if (getReparseLogFlag() || (e.getButton() == MouseEvent.BUTTON3)) {
					parseLog(row);
				}
				
				ParseResult result = (ParseResult) caseTable.getValueAt(row, col);
				if (result == ParseResult.FAIL_GETLOG) {
					Case caseToGet = caseToShow;
					String tid = caseToGet.getTID();
					String rel = caseToGet.getRelease();
					String customer = caseToGet.getCustomer();
					String logFileName = controller.getBaseDir() + "/" + customer 
							+ "/" + rel + "/log/" + tid + ".log";
					File logFile = new File(logFileName);
					LogProcesser logProcesser = new LogProcesser(controller, caseToGet, row);
					try {
						logProcesser.getCaseLog();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					if (logFile.exists()) {
						result = ParseResult.NO_PARSE;
					} else {
						caseTable.setValueAt(ParseResult.NA, row, CaseTableModel.COLUMN_RESULT);
						caseToShow.setRunResult(ParseResult.NA);
						showMessageDialog(logFile.getAbsolutePath() + " NOT exist\n");
					}
				}
				if (result == ParseResult.NO_PARSE
						|| result == ParseResult.FAIL_PARSE) {
					parseLog(row);
					result = (ParseResult) caseTable.getValueAt(row, col);
				}
				
				if (!(result == ParseResult.PASS
						|| result== ParseResult.FAIL)) {
					return;
				}
				
				showParseResult(caseToShow);
			}
			
			public void mouseExited(MouseEvent e) {
				mainPane.setCursor(Cursor.getDefaultCursor());
			}
		});
		
		caseTable.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				int row = caseTable.rowAtPoint(e.getPoint());
				int col = caseTable.columnAtPoint(e.getPoint());
				ParseResult result = null;
				if (col == CaseTableModel.COLUMN_RESULT) {
					result = (ParseResult) caseTable.getValueAt(row, col);
				}
								
				if (col == CaseTableModel.COLUMN_RESULT 
						&& result != ParseResult.NA) {
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
		caseToAdd.add(ParseResult.NA);

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
		File hostFile = new File(System.getProperty("user.home") + "/.sptest/host.xml");
		if (hostFile.exists()) {
			new HostManagerGui(controller);
		} else {
			showMessageDialog("ERROR: " + hostFile.getAbsolutePath() + " doesn't exist!\n" +
					"Host is not set");
		}
		
	}

	public void setMgts() {
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
		testCase.setRunResult((ParseResult) caseTable.getValueAt(row, CaseTableModel.COLUMN_RESULT));
		return testCase;
	}
	
	public void showCaseInfo(Case caseToShow) {		
		StringBuilder caseInfo = new StringBuilder();
		
		File cmdFile = new File(controller.getBaseDir()
				+ "/" + caseToShow.getCustomer() 
				+ "/" + caseToShow.getRelease()
				+ "/res/" + caseToShow.getTID()
				+ ".cmd" );		
		try {
			BufferedReader br = new BufferedReader(new FileReader(cmdFile));
			String line = null;
			while ((line = br.readLine()) != null) {
				caseInfo.append(line + "\n");
			}
			br.close();
		} catch (FileNotFoundException e) {
			caseInfo.append("File " + cmdFile.getName()
					+ " NOT exist, run state machine "
					+ caseToShow.getTID());
		} catch (IOException e) {
			caseInfo.append("Read error");			
		}
		
		JTextPane caseInfoPane = new JTextPane();
		caseInfoPane.setEditable(false);
		caseInfoPane.setText(caseInfo.toString());
		
		JScrollPane caseInfoScrollPane = new JScrollPane(caseInfoPane,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		JDialog caseInfoWindow = new JDialog(mainFrame, caseToShow.getTID() + " information", true);
		caseInfoWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		caseInfoWindow.setContentPane(caseInfoScrollPane);
		caseInfoWindow.setSize(600, 400);
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize(); 
		int x = (int) screensize.getWidth() / 2 - 600 / 2;
		int y = (int) screensize.getHeight() / 2 - 400 / 2;
		caseInfoWindow.setLocation(x, y);
		
		caseInfoWindow.setVisible(true);		
	}
	
	public void showCaseFrm(Case caseToShow) {
		String frmFile = controller.getBaseDir()
				+ "/" + caseToShow.getCustomer()
				+ "/" + caseToShow.getRelease()
				+ "/res/" + caseToShow.getTID() + ".frm";
		String frmbkFile = controller.getBaseDir()
				+ "/" + caseToShow.getCustomer()
				+ "/" + caseToShow.getRelease()
				+ "/res/" + caseToShow.getTID() + ".frmbk";
		
		JTextArea frmTextPane = new JTextArea();
		frmTextPane.setEditable(false);
		try {
			frmTextPane.setText(controller.parseFrm(frmFile));
		} catch (Exception e) {
			frmTextPane.setText("frm file read error");
		}
		frmTextPane.setCaretPosition(0);
		JScrollPane frmPane = new JScrollPane(frmTextPane,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		JTextArea frmbkTextPane = new JTextArea();
		frmbkTextPane.setEditable(false);
		try {
			frmbkTextPane.setText(controller.parseFrm(frmbkFile));
		} catch (Exception e) {
			frmbkTextPane.setText("frmbk file read error");
		}
		frmbkTextPane.setCaretPosition(0);
		JScrollPane frmbkPane = new JScrollPane(frmbkTextPane,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		JTabbedPane frmTab = new JTabbedPane();
		frmTab.addTab(caseToShow.getTID() + ".frm", frmPane);
		frmTab.addTab(caseToShow.getTID() + ".frmbk", frmbkPane);
		
		JDialog frmWindow = new JDialog(mainFrame, caseToShow.getTID() + " frm/frmbk", true);
		frmWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		frmWindow.setContentPane(frmTab);
		frmWindow.setSize(600, 600);
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize(); 
		int x = (int) screensize.getWidth() / 2 - frmWindow.getWidth() / 2;
		int y = (int) screensize.getHeight() / 2 - frmWindow.getHeight() / 2;
		frmWindow.setLocation(x, y);
		frmWindow.setVisible(true);
	}
	
	public void showParseResult(Case caseToShow) {		
		ParseResult result = caseToShow.getRunResult();
		String baseDir = controller.getBaseDir();
		
		if (baseDir == null ||
				(result != ParseResult.PASS && result != ParseResult.FAIL)) {
			return;
		}
		
		String tid = caseToShow.getTID();
		String rel = caseToShow.getRelease();
		String customer = caseToShow.getCustomer();
		
		JTextPane resultInfo = new JTextPane();
		resultInfo.setEditable(false);

		String parseResultName = null;
		if (result == ParseResult.PASS) {
			parseResultName = baseDir + "/" + customer 
					+ "/" + rel + "/log/" + tid + ".PASS";
		} else {
			parseResultName = baseDir + "/" + customer 
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
							line.contains("NOTEQUAL") || line.contains("subscript at")) {
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
		JDialog resultWindow = new JDialog(mainFrame, parseResultFile.getAbsolutePath(), true);
		resultWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		resultWindow.add(resultPane);
		resultWindow.setSize(600, 400);
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize(); 
		int x = (int) screensize.getWidth() / 2 - resultWindow.getWidth() / 2;
		int y = (int) screensize.getHeight() / 2 - resultWindow.getHeight() / 2;
		resultWindow.setLocation(x, y);
		resultWindow.setVisible(true);
	}

	public void parseLog(int row) {
		Case caseToParse = getCaseFromGui(row);
		ParseResult result = (ParseResult) caseTable.getValueAt(row, CaseTableModel.COLUMN_RESULT);
		if (result == ParseResult.NA) {
			this.showMessageDialog("Log does not exist for case " +
					caseTable.getValueAt(row, CaseTableModel.COLUMN_TID));
			return;
		}
		
		boolean origParseResult = true;
		if (result == ParseResult.FAIL) {
			origParseResult = false;
		} 
		
		boolean newParseResult = false;
		try {
			newParseResult = controller.parseLog(caseToParse, origParseResult);
			result = newParseResult ? ParseResult.PASS : ParseResult.FAIL;
		} catch (CantParseException e) {
			result = ParseResult.FAIL_PARSE;
			showMessageDialog("Fail to parse " + caseToParse.getTID() + 
					".log, it maybe because mismatch of brackets in trace.");
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
			ParseResult result = ParseResult.NA;
			File successLog = new File(baseDir + "/" + customer
					+ "/" + rel + "/log/" + tid + ".log");
			if (successLog.exists()) {
				File parseResultFile = new File(baseDir + "/" + customer
						+ "/" + rel + "/log/" + tid + ".PASS");
				if (parseResultFile.exists()) {
					result = ParseResult.PASS;
				} else {
					result = ParseResult.NO_PARSE;
				}
				
			} else {
				File failLog = new File(baseDir + "/" + customer 
						+ "/" + rel + "/faillog/" + tid + ".log");
				if (failLog.exists()) {
					result = ParseResult.FAIL;
				}
			}
			
			caseTable.setValueAt(result, row, CaseTableModel.COLUMN_RESULT);
		}		
	}
	
	public void clearResult() {
		int rowCount = caseTable.getRowCount();
		ParseResult result = ParseResult.NA;
		
		for (int row = 0; row < rowCount; row++) {
			caseTable.setValueAt(result, row, CaseTableModel.COLUMN_RESULT);
		}
		CaseTableModel tableModel = (CaseTableModel) caseTable.getModel();
		tableModel.fireTableDataChanged();
	}
	
	public void getResultStatistics() {
		int rowCount = caseTable.getRowCount();
		if (rowCount == 0) {
			showMessageDialog("No case loaded on GUI");
			return;
		}
		int passCount = 0;
		int failCount = 0;
		int otherCount = 0;
		
		ParseResult result = null;
		for (int row = 0; row < rowCount; row++) {
			result = (ParseResult) caseTable.getValueAt(row, CaseTableModel.COLUMN_RESULT);
			switch (result) {
			case PASS:
				passCount++;
				break;
			case FAIL:
				failCount++;
				break;
			default:
				otherCount++;
			}
		}
		
		float passRate = (float) (Math.round(((float) passCount) / ((float) rowCount) * 10000)) / 10000;
		StringBuilder report = new StringBuilder();
		report.append("+++++++++++++++++ Case result statistics ++++++++++++++++++\n\n" +
				"/****************************************/\n");
		report.append(String.format(" * Total Cases: %20d %s", rowCount,"\n"));
		report.append(String.format(" * Passed cases: %14d %s", passCount,"\n"));
		report.append(String.format(" * Failed cases: %16d %s", failCount,"\n"));
		report.append(String.format(" * Other result cases: %8d %s", otherCount,"\n"));
		report.append(String.format(" * Pass Rate: %20.2f%s", passRate * 100,"%\n"));
		report.append("/****************************************/\n");
		
		showMessageDialog(report.toString());
	}
	
	public boolean getLoadDataFlag() {
		return menuSetLoadData.getState();
	}
	
	public boolean getReparseLogFlag() {
		return menuReparseLog.getState();
	}
	
	public boolean useDynamicFrmbkFlag() {
		return menuUseDynamicFrmbk.getState();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/* 
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
		new MainGui();
	}

}
