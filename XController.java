import java.io.*;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JOptionPane;

public class XController {

	public MainGui gui = null;
	private String baseDir = null;
	private Host host = null;
	private MgtsHost mgts = null;
	private PgClient pgclient = null;
	private String display = null;
	private FTPManager ftpManager = null;
	private RTDBManager rtdbManager = null;
	private SPAManager spaManager = null;
	private LogParser logParser = null;

	public static final int DEFAULT_INTERVAL = 4;

	private boolean flagRunning = false;

	public static final String CEPEXEC = "/cs/sn/cr/cepexec";

	public enum CmdType {
		DATE, LMT, SLEEP, STA, BP, UNDEF;
	}

	public XController() {
		display = "135.252.17.202:1.0";
		flagRunning = false;
		logParser = new LogParser(this);
	}

	public void setGui(MainGui gui) {
		this.gui = gui;
	}
	
	public MainGui getGui() {
		return gui;
	}

	public void setBaseDir(String dir) {
		baseDir = dir;
	}

	public String getBaseDir() {
		return baseDir;
	}

	public FTPManager getFTPManager() {
		return ftpManager;
	}

	public PgClient getPgClient() {
		return pgclient;
	}

	public RTDBManager getRTDBManager() {
		return rtdbManager;
	}

	public SPAManager getSPAManager() {
		return spaManager;
	}
	
	public LogParser getLogParser() {
		return logParser;
	}

	public void setDisplay(String display) {
		this.display = display;
	}
	
	public String getDisplay() {
		return display;
	}

	public void setHost(Host host) {
		this.host = host;
		try {
			host.login();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			this.showMessageDialog("ERROR: Fail to connect to Host " + host.getHostName() +
					"\nReason: " + e.getMessage() +
					"\nPlease check: \n" +
					"	1) Host is reachable\n" +
					"	2) Telnet is enabled in /etc/xinet.d/telnet");
			printLog("ERROR: Failed to connect host " + host.getHostName() + "\n");
			host = null;
			return;
		} 
		
		ftpManager = new FTPManager(host);
		ftpManager.setController(this);
		
		pgclient = new PgClient(host);
		pgclient.setController(this);
		pgclient.pgConnect();
		
		rtdbManager = new RTDBManager(this);
		spaManager = new SPAManager(this);
		
		if (gui == null) {
			return;
		}
		
		String message = host.getHostName() + " Connected" +
				"\n*Hostname: " + host.getHostName() +
				"\n*IP Address: " + host.getIP() +
				"\n*Username: " + host.getUserName();		
		gui.setHostInfo(message);
	}

	public void setMgtsHost(MgtsHost mgts) {
		MgtsHost oldMgts = this.mgts;
		this.mgts = mgts;
		
		try {
			this.mgts.login();
			
			if (oldMgts != null) {
				oldMgts.disconnect();
			}			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			this.showMessageDialog("ERROR: Fail to connect MGTS server " + mgts.getHostName() +
					"\nMessage: " + e.getMessage());
			printLog("Failed to connect MGTS server " + mgts.getHostName());
			mgts = oldMgts;
			return;
		}
		
		if (gui == null) {
			return;
		}
		String message = "MGTS server " + mgts.getHostName() + " connected" +
				"\n*IP Address: " + mgts.getIP() +
				"\n*Username: " + mgts.getUserName() +
				"\n*Protocol: " + mgts.getProtocol() +
				"\n*Shelf Name: " + mgts.getShelfName();
		gui.setMgtsInfo(message);
		gui.showMessageDialog(message);
		printLog("Set MGTS SERVER " + mgts.getHostName() + "\n");
	}
	
	public Case getCaseFromGui(int row) {
		return gui.getCaseFromGui(row);
	}
	
	public void startToRunCase() {
		if ((baseDir == null) || baseDir.isEmpty()) {
			showMessageDialog("ERROR: Base directory is not set\n");
			printLog("ERROR: Base directory is not set, NOT start running\n");
			return;
		}
		if (host == null) {
			showMessageDialog("ERROR: Host is not set\n");
			printLog("ERROR: Host is not set, NOT start running\n");
			return;
		}
		
		if (mgts == null) {
			String title = "WARNING: MGTS Host is not set";
			String message = "WARNING: MGTS Host is not set\n" +
					"Still continue to run case, click Yes\n" +
					"Cancel to click No";
			int choice = gui.showConfirmDialog(title, message);
			if (choice == JOptionPane.NO_OPTION) {
				printLog("WARNING: MGTS Host is not set, " +
						"cancelled\n");
				return;
			}
			printLog("WARNING: MGTS Host is not set, " +
					"ignore it and continue to run case\n");
		}

		this.setRunningFlag(true);
		CaseRunner runner = new CaseRunner(this);
		Thread t = new Thread(runner);
		t.start();
	}
	
	public void showMessageDialog(String str) {
		if (gui == null) {
			return;
		}
		gui.showMessageDialog(str);
	}

	/*
	 * Before running each audit case, SIMDB/AIRTDB/UARTDB need to be created
	 * This will ensure only the case's subscriber be audited in log
	 */
	public void prepareAuditCase(Case caseToRun) {
		if (!rtdbManager.createAuditDb()) {
			String sql = "SELECT db_name FROM rtdb_app " +
					"WHERE db_name like 'AIRTDB%' " + 
					"OR db_name LIKE 'SIMDB%' " +
					"OR db_name LIKE 'UARTDB%'";
			String result = pgclient.getSqlResult(sql);
			String[] createList = result.split("\n");

			for (String dbName : createList) {
				rtdbManager.createDB(dbName);
			}
		}
		
		rtdbManager.loadCaseDB(caseToRun);
	}
	
	public void loadFrm(String frmFileName) {
		File frmFile = new File(frmFileName);
		if (!frmFile.exists()) {
			return;
		}
		
		ftpManager.connect();
		ftpManager.uploadFile(frmFile);
		ftpManager.disconnect();
		
		String frmName = frmFile.getName();
		
		host.sendCmd("sed -i \"s/! /,/g\" /tmp/" + frmName);
		System.out.println(host.sendCmd("/cs/sn/cr/cepexec RCV_TEXT \"RCV:TEXT,SPA\" < /tmp/"
				+ frmName));
		//host.sendCmd("rm /tmp/" + frmName);		
	}
	
	/* It used to be boolean runCase(Case caseToRun)
	 * this function has been replaced by newRunCase()
	 * */
	public void runCase(Case caseToRun) {
		try {
			String customer = caseToRun.getCustomer();
			String rel = caseToRun.getRelease();
			String resDir = baseDir + "/" + customer + "/" + rel + "/res/";
			
			if(!host.loadFrm(resDir + caseToRun.getTID() + ".frm")) {
				loadFrm(resDir + caseToRun.getTID() + ".frm");
			}
			
			File cmdFile = new File(resDir + caseToRun.getTID() + ".cmd");
			String pid = startTailerLog(caseToRun);
			Thread stateRunner = null;
			boolean dateChanged = false;
			if (!cmdFile.exists()) {
				printLog("cmd file not exist for "
						+ caseToRun.getTID() + "\n");
				//runStateMachine(caseToRun.getTID());
				//Thread.sleep(DEFAULT_INTERVAL * 1000);
				stateRunner = runState(caseToRun.getTID());
			} else {
				BufferedReader br = new BufferedReader(new FileReader(cmdFile));
				String line, cmdStr;
				CmdType cmdType;
				String lmtCmd = null;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("#") || line.isEmpty()) {
						continue;
					}
					
					int start = 0, end = 0;
					int sleep = -1;
					String cmdTypeStr = null;
					end = line.indexOf("=");
					if (end > -1) {
						cmdTypeStr = line.substring(start, end);
						if (cmdTypeStr.equals(" ")) {  // this if is for a special case's cmd file
							continue;					// once find the case, this if will be deleted
						}
						if (cmdTypeStr.startsWith("snd:text")) {
							cmdTypeStr = new String("LMT");
							start = 0;
						} else {
							start = end + 1;
						}
						
						end = line.indexOf("|");
						if (end > -1) {
							cmdStr = line.substring(start, end);
							start = line.lastIndexOf("|") + 1;
							String sleepStr = line.substring(start);
							if (!sleepStr.isEmpty()) {
								sleep = Integer.parseInt(sleepStr);
							}
							
						} else {
							end = line.length();
							cmdStr = line.substring(start, end);
						}
						
					} else {
						cmdTypeStr = line;
						cmdStr = "";
					}					
					
					if (cmdStr.endsWith(";")) {
						cmdStr = cmdStr.substring(0, cmdStr.length() - 1);
					}					
					 
					cmdType = CmdType.valueOf(cmdTypeStr.toUpperCase());					
					switch (cmdType) {
					case DATE:
						printLog("send " + cmdType.toString() + " cmd: "
								+ cmdStr + "\n");
						host.changeDate(cmdStr);
						stopTailerLog(pid);
						pid = appendTailerLog(caseToRun);
						dateChanged = true;
						//host.sendCmd("touch /tmp/" + caseToRun.getTID() + ".log");
						break;
					case LMT:
						printLog("send " + cmdType.toString() + " cmd: "
								+ cmdStr + "\n");
						if (cmdStr.contains("CCR") && !cmdStr.contains("CCR R")) {
							if (sleep < 0) {
								sleep = 15;
							}
						}
						if (cmdStr.startsWith("create:db=")) {
							createDbInCmd(cmdStr);
						} else {
							lmtCmd = CEPEXEC + " SEND_TEXT " + "'" + cmdStr + "'";
							host.sendCmd(lmtCmd);
						}						
						break;
					case SLEEP:
						if (!cmdStr.isEmpty()) {
							sleep = Integer.parseInt(cmdStr);
						}						
						printLog("send " + cmdType.toString() + " cmd: "
								+ sleep + "\n");
						break;
					case STA:
						if (cmdStr.isEmpty()) {
							cmdStr = caseToRun.getTID();
						}
						printLog("send " + cmdType.toString() + " cmd: "
								+ cmdStr + "\n");
						//runStateMachine(cmdStr);
						if (stateRunner != null && stateRunner.isAlive()) {
							stateRunner.join();
						}
						stateRunner = runState(cmdStr);
						break;
					case BP:
						printLog("load bp: " + cmdStr + "\n");
						loadBpInCmd(caseToRun, cmdStr);
						break;
					default:
						printLog("command is undefined\n");
					}
					if (sleep < 0) {
						sleep = DEFAULT_INTERVAL;
					}
					Thread.sleep(sleep * 1000);
				}

				br.close();
			}
			/* run state machine in another thread, must wait for this thread finish */
			if (stateRunner != null && stateRunner.isAlive()) {
				stateRunner.join();
			}

			stopTailerLog(pid);
			Thread.sleep(1000);
			if (dateChanged) {
				host.changeDate("010109092033");
			}
			host.loadFrmbk(resDir + caseToRun.getTID() + ".frmbk");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private void createDbInCmd(String cmdStr) {
		int start = cmdStr.indexOf("=") + 1;
		int end = cmdStr.indexOf(";");
		if (end == -1) {
			end = cmdStr.length();
		}
		String db = cmdStr.substring(start, end);
		
		String sql = "select db_name from rtdb_app where db_name like '" + db + "%'";
		String[] dbList = pgclient.getSqlResult(sql).split("\n");
		if (dbList.length == 0) {
			printLog("Warning: " + db + "not installed \n");
		}
		
		for (String dbName : dbList) {
			rtdbManager.createDB(dbName);
		}		
	}
	
	public void loadBpInCmd(Case caseToRun, String cmdStr) {
		int semicolon = cmdStr.indexOf(";");
		if (semicolon < 0) {
			return;
		}
		String customer = caseToRun.getCustomer();
		String rel = caseToRun.getRelease();
		String bpFileName = cmdStr.substring(0, semicolon);
		File bpFile = new File(baseDir + "/" + customer + "/" + rel + "/res/" + bpFileName);
		if (!bpFile.exists()) {
			return;
		}
		int underscroll = cmdStr.indexOf("_", semicolon + 1);
		if (underscroll < 0) {
			underscroll = cmdStr.length();
			if (cmdStr.endsWith(";")) {
				underscroll--;
			}
		}
		String spaNamePrefix = cmdStr.substring(semicolon + 1, underscroll);
		String sql = "SELECT span FROM spm_tbl WHERE span LIKE '" + spaNamePrefix + "%'";
		String spaList = this.pgclient.getSqlResult(sql);
		if ((spaList == null) || spaList.isEmpty()) {
			return;
		}
		String spaName = spaList.split("\n")[0];
		if (spaName.isEmpty()) {
			return;
		}
		String tempBpFileName = "/tmp/" + caseToRun.getTID() + ".subshl";
		ftpManager.connect();
		ftpManager.uploadFile(bpFile);
		ftpManager.disconnect();
		
		host.sendCmd("dos2unix /tmp/" + bpFile.getName());
		host.sendCmd("echo debug:spa=" + spaName
				+ ",client=all,source=\\\"/tmp/" + bpFile.getName()
				+ "\\\",ucl > " + tempBpFileName);
		host.sendCmd("subshl -f " + tempBpFileName + " >/dev/null 2>&1");
	}

	/*
	 * this function has been replaced by runState()
	 */
	public void runStateMachine(final String stateName) {
		mgts.startMgts(host.getIP(), this.display);
		
		String assignName = mgts.getSeqName(stateName);
		if (assignName == null || assignName.isEmpty()) {
			printLog("There is no sequence for case " + stateName + "\n");
			return;
		}
		
		printLog("download assignment " + assignName + "\n");
		System.out.println(mgts.downloadAssign(assignName));
		printLog("run state machine " + stateName + "\n");
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			public void run() {
				MgtsHost mgtsMonitor = new MgtsHost(mgts);
				try {
					mgtsMonitor.login();
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mgtsMonitor.setPrompt("datafiles> ");
				mgtsMonitor.goToDataDir();
				mgtsMonitor.stopStateMachine(stateName);
				mgtsMonitor.disconnect();				
			}
		}, 375 * 1000);
		System.out.println(mgts.runStateMachine(stateName));		
		t.cancel();
		mgts.disconnectPassThru();
	}
	
	public Thread runState(String stateName) {
		if (mgts == null) {
			printLog("MGTS host is NOT set\n");
			return null;
		}
		mgts.startMgts(host.getIP(), this.display);
		
		String assignName = mgts.getSeqName(stateName);
		if (assignName == null || assignName.isEmpty()) {
			printLog("There is no sequence for case " + stateName + "\n");
			return null;
		}
		
		printLog("download assignment " + assignName + "\n");
		System.out.println(mgts.downloadAssign(assignName));
		printLog("run state machine " + stateName + "\n");
		StateMachineRunner stateRunner = new StateMachineRunner(this, stateName);
		Thread t = new Thread(stateRunner);
		t.start();
		
		return t;
	}

	public String startTailerLog(Case caseToRun) {
		printLog("start to tailer log\n");
		String tid = caseToRun.getTID();
		String str = null;
		host.sendCmd("stty -echo");
		str = host.sendCmd("tailer de > /tmp/" + tid +".log 2>/dev/null &");
		host.sendCmd("stty echo");
		System.out.println(str);
		String[] array = str.split("\\s+");
		String pid = array[1];
		return pid;
	}
	
	public String appendTailerLog(Case caseToRun) {
		String tid = caseToRun.getTID();
		String str = null;
		host.sendCmd("stty -echo");
		host.sendCmd("for log in `ls -tr /sn/log/debug*`; do touch $log; done");
		str = host.sendCmd("tailer de >> /tmp/" + tid +".log 2>/dev/null &");
		host.sendCmd("stty echo");
		System.out.println(str);
		String[] array = str.split("\\s+");
		String pid = array[1];
		return pid;
	}

	public void stopTailerLog(String pid) {
		if (pid == null || !pid.matches("[0-9]+")) {
			printLog("stop tailering log\n");
			host.sendCmd("pkill tailer");
		} else {
			printLog("stop tailering log, pid is " + pid + "\n");
			host.sendCmd("kill -9 " + pid);
		}
	}
	
	public void combineLog(Case caseToRun) {
		host.sendCmd("LogCMB /tmp/" + caseToRun.getTID() + ".log");
	}

	public boolean parseLog(Case caseToParse, boolean originalResult) throws CantParseException {
		return logParser.reParseCase(caseToParse, originalResult);
	}
	
	public void newRunCase(Case caseToRun) {		
			String customer = caseToRun.getCustomer();
			String rel = caseToRun.getRelease();
			String resDir = baseDir + "/" + customer + "/" + rel + "/res/";
			
			if(!host.loadFrm(resDir + caseToRun.getTID() + ".frm")) {
				loadFrm(resDir + caseToRun.getTID() + ".frm");
			}
			
			File cmdFile = new File(resDir + caseToRun.getTID() + ".cmd");
			String pid = startTailerLog(caseToRun);
			Thread stateRunner = null;
			boolean dateChanged = false;
			if (!cmdFile.exists()) {
				printLog("Run State Machine "
						+ caseToRun.getTID() + "\n");
				stateRunner = runState(caseToRun.getTID());
			} else {				
				BufferedReader br;
				try {
					br = new BufferedReader(new FileReader(cmdFile));
				
					String line, cmdStr;
					CmdType cmdType = CmdType.UNDEF;
					CmdType preCmdType = null;
					String lmtCmd = null;
					String preLmtCmd = null;
					int sleep = -1;
					while ((line = br.readLine()) != null) {
						if (line.startsWith("#") || line.isEmpty()) {
							continue;
						}
						
						int start = 0, end = 0;
						
						String cmdTypeStr = null;
						end = line.indexOf("=");
						if (end > -1) {
							cmdTypeStr = line.substring(start, end);
							if (cmdTypeStr.equals(" ")) {  // this if is for a special case's cmd file
								continue;					// once find the case, this if will be deleted
							}
							if (cmdTypeStr.startsWith("snd:text")) {
								cmdTypeStr = new String("LMT");
								start = 0;
							} else {
								start = end + 1;
							}
							
							try {
								cmdType = CmdType.valueOf(cmdTypeStr.toUpperCase());
							} catch (Exception enumEx) {
								cmdType = CmdType.UNDEF;
							}
							/* Move sleep part to beginning of next command
							 * But NOT end of current command
							 * */
							if (preCmdType != null) { //skip sleep for the first line
								if (sleep < 0 && cmdType != CmdType.SLEEP) {
									if (preLmtCmd != null
											&& preLmtCmd.contains("CCR")
											&& !preLmtCmd.contains("CCR R")) {
										sleep = 15;
									} else {
										sleep = DEFAULT_INTERVAL;
									}
								}

								if (sleep > 0) {
									try {
										Thread.sleep(sleep * 1000);
										sleep = -1;
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
							
							end = line.indexOf("|");
							if (end > -1) {
								cmdStr = line.substring(start, end);
								start = line.lastIndexOf("|") + 1;
								String sleepStr = line.substring(start);
								if (!sleepStr.isEmpty()) {									
									try {
										sleep = Integer.parseInt(sleepStr);
									} catch (NumberFormatException exNumFormat) {
										printLog("fail parse sleep timer " + sleepStr 
												+ "#, set as default\n");
										sleep = DEFAULT_INTERVAL;
									}
								}
								
							} else {
								end = line.length();
								cmdStr = line.substring(start, end);
							}
							
						} else {
							cmdTypeStr = line;
							cmdStr = "";
						}					
						
						if (cmdStr.endsWith(";")) {
							cmdStr = cmdStr.substring(0, cmdStr.length() - 1);
						}					
						
						preCmdType = cmdType;
						preLmtCmd = null;
						switch (cmdType) {
						case DATE:
							printLog("send " + cmdType.toString() + " cmd: "
									+ cmdStr + "\n");
							host.changeDate(cmdStr);

							try {
								Thread.sleep(800);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							System.out.println(host.sendCmd("ls -ltr /sn/log/debuglog*"));
							
							stopTailerLog(pid);
							pid = appendTailerLog(caseToRun);
							dateChanged = true;
							sleep = 0;							
							break;
						case LMT:
							printLog("send " + cmdType.toString() + " cmd: "
									+ cmdStr + "\n");
							if (cmdStr.startsWith("create:db=")) {
								createDbInCmd(cmdStr);
							} else {
								lmtCmd = CEPEXEC + " SEND_TEXT " + "'" + cmdStr + "'";
								host.sendCmd(lmtCmd);
							}
							if (sleep == 0) {
								sleep = -1;
							}
							preLmtCmd = lmtCmd;
							break;
						case SLEEP:
							if (!cmdStr.isEmpty()) {
								try {
									sleep = Integer.parseInt(cmdStr);
								} catch (NumberFormatException exNumFormat) {
									printLog("fail parse sleep timer " + cmdStr 
											+ "#, set as default\n");
									sleep = DEFAULT_INTERVAL;
								}
							} else {
								sleep = DEFAULT_INTERVAL;
							}
							printLog("send " + cmdType.toString() + " cmd: "
									+ sleep + "\n");
							break;
						case STA:
							if (cmdStr.isEmpty()) {
								cmdStr = caseToRun.getTID();
							}
							printLog("send " + cmdType.toString() + " cmd: "
									+ cmdStr + "\n");
							if (stateRunner != null && stateRunner.isAlive()) {
								try {
									stateRunner.join();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							stateRunner = runState(cmdStr);
							sleep = 0;
							break;
						case BP:
							printLog("load bp: " + cmdStr + "\n");
							loadBpInCmd(caseToRun, cmdStr);
							sleep = 0;
							break;
						default:
							printLog(cmdStr + " is undefined\n");
						}
						
					}
					
					// Handle the last line's sleep timer
					// need think about < 0 or <= 0
					// if previous command is SLEEP, then sleep must be > 0
					if (sleep < 0 && preCmdType != CmdType.SLEEP) {
						if (preLmtCmd != null
								&& preLmtCmd.contains("CCR")
								&& !preLmtCmd.contains("CCR R")) {
							sleep = 15;
						} else {
							sleep = DEFAULT_INTERVAL;
						}
					}
					try {
						Thread.sleep(sleep * 1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			/* run state machine in another thread, must wait for this thread finish */
			if (stateRunner != null && stateRunner.isAlive()) {
				try {
					stateRunner.join();
					Thread.sleep(DEFAULT_INTERVAL * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			stopTailerLog(pid);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (dateChanged) {
				host.changeDate("010109092033");
			}
			host.loadFrmbk(resDir + caseToRun.getTID() + ".frmbk");

	}

	public void connectHost() {

	}

	public void disconnectHost() {

	}

	public void connectMgts() {

	}

	public void disconnectMgts() {

	}

	public String getHostInfo() {
		return null;
	}

	public String getMgtsInfo() {
		return null;
	}

	public void loadDbData(Case caseToRun) {

	}

	public void putResultToGui() {

	}

	public void showParseResult(Case caseToRun) {

	}

	public Host getHost() {
		return host;
	}

	public MgtsHost getMgts() {
		return mgts;
	}
	
	public boolean isRunning() {
		return flagRunning;
	}

	public void setRunningFlag(boolean flag) {
		flagRunning = flag;
	}

	public boolean getLoadDataFlag() {
		if (gui == null) {
			return false;
		} 
		
		return gui.getLoadDataFlag();
	}
	
	public boolean getReparseLogFlag() {
		if (gui == null) {
			return false;
		}
		
		return gui.getReparseLogFlag();
	}

	public void printLog(String str) {
		if (gui == null) {
			return;
		}
		gui.printLog(str);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
