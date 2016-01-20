import java.io.IOException;
import java.net.SocketException;

public class MgtsHost extends BaseHost {

	public static final String DEFAULT_DISPLAY = "135.252.17.202:1.0";
	private String dataDir = null;
	private String protocol = null;
	private String shelfName = null;

	public MgtsHost(String name, String ip, int port, String username,
			String passwd) {
		super(name, ip, port, username, passwd);
		setPrompt("> ");
		dataDir = "/home/catapult/USERS/" + username + "/datafiles";
	}
	
	public MgtsHost(MgtsHost mgts) {
		 super(mgts.getHostName(), mgts.getIP(), mgts.getPort(), 
				 mgts.getUserName(), mgts.getPasswd());
			setPrompt("> ");
			dataDir = mgts.getDataDir();
	}

	private void setInitialEnv() {
		sendCmd("setenv DISPLAY " + DEFAULT_DISPLAY);
		sendCmd("setenv MGTS_CLIENT /home/MGTS/17.1/MGTS");
		sendCmd("setenv OS_TYPE Linux");
		sendCmd("cd " + dataDir);
	}

	public String getDataDir() {
		return dataDir;
	}

	public String goToDataDir() {
		setPrompt("datafiles> ");
		return sendCmd("cd " + dataDir);
	}
	
	public String goToHomedir() {
		setPrompt(this.getUserName() + "> ");
		return sendCmd("cd ~");
	}

	public void startMgtsSession(String protocol) {
		setInitialEnv();
		if ((protocol == null) || protocol.isEmpty()) {
			protocol = "ITU";
		}

		disconnectPassThru();
		stopMgts();

		sendCmd("source /home/catapult/USERS/" + getUserName()
				+ "/mgts_cit_csh");
		sendCmd("/home/MGTS/17.1/MGTS/bin/run_mgts_script");
		connectShelf("EE");
	}
	public void startMgts(String ip, String display) {		
		//this.login(); // move to xcontroller
		this.goToHomedir();
		String protocolEnv = null;
		if (protocol.equals("ITU")) {
			protocolEnv = "mgts_cit_csh";
		} else if (protocol.equals("ANSI")){
			protocolEnv = "mgts_bel_csh";
		} else {
			protocolEnv = "mgts_cit_csh"; // for future use
		}
		
		System.out.println(this.disconnectPassThru());
		this.sendCmd("setenv DISPLAY " + display) ;
		this.sendCmd("setenv MGTS_CLIENT /home/MGTS/17.1/MGTS");
		this.sendCmd("setenv OS_TYPE Linux");
		this.sendCmd("source /home/catapult/USERS/" + this.getUserName()
				+ "/" + protocolEnv);

		this.sendCmd("/home/MGTS/17.1/MGTS/scripts/stop_mgts_script");		
		this.sendCmd("/home/MGTS/17.1/MGTS/bin/run_mgts_script");
		this.connectShelf(shelfName);
		System.out.println(this.connectPassThru(ip));
	}

	public String connectShelf(String shelfName) {
		return sendCmd("shelfConnect " + shelfName);
	}

	public String getSeqName(String stateName) {
		goToDataDir();
		String seqName = null;
		String cmd = "grep -l " + stateName + " sequence*.sequenceGroup";
		String result = sendCmd(cmd);
		System.out.println("Seq list: " + result);
		int offset = result.indexOf("grep -l ") + cmd.length();
		int start = result.indexOf("sequence_", offset);
		int end = result.indexOf(".sequence", offset);

		if ((start >= 0) && (end >= start)) {
			seqName = result.substring(start, end);
		}

		return seqName;
	}

	public String downloadAssign(String assignName) {
		return sendCmd("networkExecute " + assignName + " -download >/dev/null");
	}

	public String runStateMachine(String stateName) {
		String runlog = stateName + ".mgts.runlog";
		String result = sendCmd("[ -f " + runlog + " ] && rm " + runlog);
		System.out.println("clear mgts.runlog : " + result);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sendCmd("shelfPASM " + shelfName + " -node SSP -machine \"" + stateName
				+ " State Machine\" -run -wait -log $MGTS_DATA/" + runlog);
	}
	
	public String stopStateMachine(String stateName) {
		return sendCmd("pkill -u " + getUserName() + " shelfPASM");
	}

	public boolean isStateMachineStopped(String stateName) {
		String result = sendCmd("grep Stop " + stateName + ".mgts.runlog");

		return result.contains("to Stop");
	}

	public String connectPassThru(String ipAddr) {
		return sendCmd("/home/yrli/vmware/passThru -mgtshost p250alu " + ipAddr
				+ " -debug >/dev/null &");
	}

	public String disconnectPassThru() {
		return sendCmd("pkill -u " + getUserName() + " passThru");
	}

	public String stopMgts() {
		return sendCmd("/home/MGTS/17.1/MGTS/scripts/stop_mgts_script");
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public String getShelfName() {
		return shelfName;
	}
	
	public void setShelfName(String shelfName) {
		this.shelfName = shelfName;
	}
	
	public static void main(String[] args) {
		String hostname = "SPVM53";
		String ip = "135.242.106.116";
		// String hostname = "SPVM138A";
		// String ip = "135.242.17.41";
		int port = 23;
		String username = "ainet";
		String passwd = "ainet1";

		Host host = new Host(hostname, ip, port, username, passwd);

		XController controller = new XController();
		controller.setBaseDir("D:/Workspace/XAuto/automation/R29SUC");
		controller.setHost(host);
		
		String[] stateList = {"dt9777", "dt9778"};
		//, "dt9781", "dt9782", "dr1060"};
		String mgtsHostname = "p250alu";
		String mgtsip = "135.252.170.143";
		String mgtsUsername = "yrli";
		String mgtsPasswd = mgtsUsername;

		MgtsHost mgts = new MgtsHost(mgtsHostname, mgtsip, port, mgtsUsername, mgtsPasswd);
		System.out.println(mgts.getHostName() + mgts.getIP() + mgts.getPort() +
				 mgts.getUserName() + mgts.getPasswd());
		mgts.setProtocol("ITU");
		mgts.setShelfName("EE");
		controller.setMgtsHost(mgts);
		/*
		try {
			mgts.login();
			// mgts.startMgts("135.242.106.116", "135.252.17.202:1.0");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		mgts.startMgts("135.242.106.116", "135.252.17.202:1.0");
		mgts.setPrompt("datafiles> ");
		mgts.goToDataDir();
		System.out.println(mgts.downloadAssign("sequence_5_itu"));
		StateMachineRunner stateRunner = new StateMachineRunner(controller, "MNP_SRI_LHW");
		Thread t = new Thread(stateRunner);
		t.start();
		
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		mgts.startMgtsSession("ITU");
		mgts.connectPassThru("135.242.106.116");
		mgts.goToDataDir();
		*/
		/*for (String state : stateList) {
			controller.runStateMachine(state);
			
			String assign = mgts.getSeqName(state);
			System.out.println("---> assign name: " + assign);
			if (assign == null || assign.isEmpty()) {
				System.out.println("---> there is no assignment for " + state);
				continue;
			}
			String result = mgts.downloadAssign(assign);			
			System.out.println("---> download result: " + result);
			
			//String stateResult = mgts.runStateMachine(state);
			//System.out.println("---> state run result: " + stateResult);
			
			
		}*/
				
		mgts.stopMgts();
		mgts.disconnectPassThru();
		mgts.disconnect();
	}
}