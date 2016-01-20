
public class SingleCaseRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String hostname = "SPVM53";
		String ip = "135.242.106.116";
		int port = 23;
		String username = "ainet";
		String passwd = "ainet1";

		Host host = new Host(hostname, ip, port, username, passwd);

		XController controller = new XController();
		controller.setBaseDir("D:/Workspace/XAuto/automation/R29SUC");
		controller.setHost(host);
		
		String mgtsHostname = "p250alu";
		String mgtsip = "135.252.170.143";
		String mgtsUsername = "yrli";
		String mgtsPasswd = mgtsUsername;

		MgtsHost mgts = new MgtsHost(mgtsHostname, mgtsip, port, mgtsUsername, mgtsPasswd);
		controller.setMgtsHost(mgts);
		Case c = new Case("bw0346", "72400", "R27SU6", "Base", "Function", "Base");
		controller.runCase(c);
		String tid = c.getTID();
		System.out.println(host.sendCmd("LogCMB /tmp/" + tid + ".log"));
		mgts.stopMgts();
		mgts.disconnectPassThru();
		mgts.disconnect();

	}

}
