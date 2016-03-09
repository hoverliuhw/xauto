import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SPAManager {
	private XController controller;
	private Set<String> dataSpaPool;
	private List<String> startSpaPool;

	public SPAManager() {
		controller = null;
		dataSpaPool = null;
		startSpaPool = null;
	}

	public SPAManager(Host host) {
		controller = null;
		dataSpaPool = null;
		startSpaPool = null;
	}

	public SPAManager(XController c) {
		controller = c;
		dataSpaPool = new HashSet<String>();
		initDataSpaPool();
		startSpaPool = new ArrayList<String>();
	}

	public void setController(XController c) {
		controller = c;
	}
	
	public Set<String> getdataSpaPool() {
		return dataSpaPool;
	}
	
	public List<String> getStartSpaPool() {
		return startSpaPool;
	}
	
	public void initDataSpaPool() {
		if (dataSpaPool == null) {
			dataSpaPool = new HashSet<String>();
		}

		PgClient pgClient = controller.getPgClient();
		if (pgClient == null) {
			return;
		}
		
		String sql = "SELECT version_name FROM SA_NAME_MAP"; 
		String spaList = pgClient.getSqlResult(sql);
		for (String spa : spaList.split("\n")) {
			dataSpaPool.add(spa);
		}
	}
	
	public boolean isSvcSpa(String spaName) {
		return !dataSpaPool.contains(spaName);
	}
	
	public boolean isDataSpa(String spaName) {
		return dataSpaPool.contains(spaName);
	}
	
	public boolean needStartSpa(String spaName) {
		return startSpaPool.contains(spaName);
	}
	
	public Iterator<String> getStartSpaIterator() {
		return startSpaPool.iterator();
	}
	
	public void clearStartSpaPool() {
		startSpaPool.clear();
	}
	
	public boolean addToStartSpaPool(String spaName) {
		return startSpaPool.add(spaName);
	}
	
	public boolean removeFromStartSpaPool(String spaName) {
		return startSpaPool.remove(spaName);
	}

	public boolean startSpa(String spaName) {
		Host host = controller.getHost();
		boolean result = false;
		host.sendCmd("stty -echo");
		String returnStr = host
				.sendCmd("/sn/cr/cepexec INSTALL_SPA \"INSTALL:SPA=" + spaName
						+ ",PROC\";echo $?");
		char resultCode = returnStr.charAt(0);
		if (resultCode == '0') {
			result = true;
			controller.printLog("INSTALL:PROC " + spaName + " SUCCESS\n");
			host.sendCmd("/sn/cr/cepexec RST_SPA \"RST:SPA=" + spaName
					+ "\";echo $?");
		} else {
			result = false;
			controller.printLog("INSTALL:PROC " + spaName + " FAIL\n");
		}
		host.sendCmd("stty echo");

		return result;
	}
	
	public void startSpaInPool() {
		Iterator<String> it = startSpaPool.iterator();
	
		it = startSpaPool.iterator();
		while (it.hasNext()) {
			String spaName = it.next();
			startSpa(spaName);
			applyTrace1(spaName);
			applyBp(spaName);
		}
		
		clearStartSpaPool();
	}

	public void stopSpa(String spaName) {
		Host host = controller.getHost();
		host.sendCmd("/sn/cr/cepexec ABORT_SPA \"ABT:SPA=" + spaName
				+ "\";echo $?");
		controller.printLog("Stop SPA " + spaName + "\n");
	}
	
	public void stopAllSpa() {
		String[] spaList = listISSpa();
		for (String spa : spaList) {
			stopSpa(spa);
		}
	}

	public boolean restartSpa(String spaName) {
		stopSpa(spaName);
		return startSpa(spaName);
	}

	public void restartAllSpa() {
		String[] spaList = listISSpa();

		for (String spaName : spaList) {
			restartSpa(spaName);
			applyTrace1(spaName);
			applyBp(spaName);
		}
	}

	/*
	 * replaced by applyTrace1()
	 */
	public void applyTrace(String spaName) {
		Host host = controller.getHost();
		
		host.sendCmd("/sn/cr/cepexec TRACE \"TRACE:PROC=" + spaName
				+ ",mode=1178&1179,client=all,ucl\"");
		host.sendCmd("/sn/cr/cepexec TRACE \"TRACE:PROC=" + spaName
				+ ",mode=1178&1179,machine=0-0-1,ucl\"");
		host.sendCmd("/sn/cr/cepexec TRACE \"TRACE:PROC=" + spaName
				+ ",mode=1178&1179,machine=0-0-9,ucl\"");
	}
	
	public void applyTrace1(String spaName) {
		Host host = controller.getHost();
		File traceDir = new File(controller.getBaseDir() + "/tracedir");
		if (!traceDir.exists()) {
			controller.printLog("TRACE directory: " + traceDir.getAbsolutePath() + " does NOT exist\n");
			return;
		}
		File[] traceFileList = traceDir.listFiles();
		for (File traceFile : traceFileList) {
			String traceFileName = traceFile.getName();
			if (!traceFileName.startsWith(spaName)) {
				continue;
			}
			System.out.println("trace: " + traceFile.getAbsolutePath());
			StringBuilder mode = new StringBuilder();
			try {
				BufferedReader br = new BufferedReader(new FileReader(traceFile));
				String line = null;
				while ((line = br.readLine()) != null) {
					if (mode.length() == 0) {
						mode.append(line);
					} else {
						mode.append("&").append(line);
					}
				}
				br.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (traceFileName.contains("_")) {
				host.sendCmd("/sn/cr/cepexec TRACE \"TRACE:PROC=" + spaName
						+ ",mode=" + mode + ",client=all,ucl\"");
			} else {
				host.sendCmd("/sn/cr/cepexec TRACE \"TRACE:PROC=" + spaName
						+ ",mode=" + mode + ",machine=0-0-1,ucl\"");
				host.sendCmd("/sn/cr/cepexec TRACE \"TRACE:PROC=" + spaName
						+ ",mode=" + mode + ",machine=0-0-9,ucl\"");
			}
		}
	}

	public void closeTrace(String spaName) {
		Host host = controller.getHost();

		host.sendCmd("/sn/cr/cepexec TRACE \"TRACE:PROC=" + spaName
				+ ",mode=off,client=all,ucl\"");
		host.sendCmd("/sn/cr/cepexec TRACE \"TRACE:PROC=" + spaName
				+ ",mode=off,machine=0-0-1,ucl\"");
		host.sendCmd("/sn/cr/cepexec TRACE \"TRACE:PROC=" + spaName
				+ ",mode=off,machine=0-0-9,ucl\"");
	}

	public void applyBp(String spaName) {
		Host host = controller.getHost();
		FTPManager ftpManager = controller.getFTPManager();
		String bpDir = controller.getBaseDir() + "/breakpoints/";
		String bpFileName = bpDir + spaName + ".bp";
		
		File bpFile = new File(bpFileName);
		String tempBpFileName = "/tmp/" + spaName + ".subshl";  // temp bp file on Host
		ftpManager.connect();
		if (bpFile.exists()) {
			ftpManager.uploadFile(bpFile);
			host.sendCmd("dos2unix /tmp/" + bpFile.getName());
			
			System.out.println("echo debug:spa=" + spaName
					+ ",client=all,source=\\\"/tmp/" + bpFile.getName()
					+ "\\\",ucl > " + tempBpFileName);
			host.sendCmd("echo debug:spa=" + spaName
					+ ",client=all,source=\\\"/tmp/" + bpFile.getName()
					+ "\\\",ucl > " + tempBpFileName);
		}		

		if (spaName.startsWith("EPPSA")) {
			File clientBpFile = new File(bpDir + "client.bkpoint");
			File serverBpFile = new File(bpDir + "server.bkpoint");
	
			if (clientBpFile.exists()) {
				ftpManager.uploadFile(clientBpFile);
				host.sendCmd("dos2unix /tmp/" + clientBpFile.getName());
				host.sendCmd("echo debug:spa=" + spaName
						+ ",client=all,source=\\\"/tmp/"
						+ clientBpFile.getName() + "\\\",ucl >> " + tempBpFileName);
			}

			if (serverBpFile.exists()) {
				ftpManager.uploadFile(serverBpFile);
				host.sendCmd("dos2unix /tmp/" + serverBpFile.getName());
				host.sendCmd("echo debug:spa=" + spaName + ",source=\\\"/tmp/"
						+ serverBpFile.getName()
						+ "\\\",machine=0-0-1,ucl >> " + tempBpFileName);
				host.sendCmd("echo debug:spa=" + spaName + ",source=\\\"/tmp/"
						+ serverBpFile.getName()
						+ "\\\",machine=0-0-9,ucl >> " + tempBpFileName);
			}

		}

		host.sendCmd("subshl -f " + tempBpFileName + " >/dev/null 2>&1");
		ftpManager.disconnect();
	}

	public String getSpaRelease() {
		PgClient pgClient = controller.getPgClient();
		String result = pgClient
				.getSqlResult("SELECT version_name FROM SA_NAME_MAP WHERE spa_base='ENWTPPS'");
		String release = result.substring(7, result.length() - 1);

		return release;
	}

	public void deleteBp(String spaName) {

	}

	public boolean isSpaInstalled(String spaName) {
		PgClient pgClient = controller.getPgClient();
		String result = pgClient
				.getSqlResult("SELECT span FROM spa_tbl WHERE span='" + spaName
						+ "'");
		return !result.isEmpty();
	}

	public String[] listAllSpa() {
		PgClient pgClient = controller.getPgClient();
		String spaList = pgClient
				.getSqlResult("SELECT span FROM spa_tbl ORDER BY span");

		return spaList.split("\n");
	}

	public String[] listISSpa() {
		PgClient pgClient = controller.getPgClient();
		String spaList = pgClient.getSqlResult("SELECT span FROM spm_tbl");
		return spaList.split("\n");
	}

	public boolean isSpaIS(String spaName) {
		PgClient pgClient = controller.getPgClient();
		String result = pgClient
				.getSqlResult("SELECT span FROM spm_tbl WHERE span='" + spaName
						+ "'");
		return !result.isEmpty();
	}
	
	public void loadSpaSql(String spaName, String dataDir) {
		PgClient pgClient = controller.getPgClient();
		String sqlDir = dataDir.endsWith("/") ? dataDir : (dataDir + "/");
		
		File sqlFile = new File(sqlDir + spaName + ".sql");
		if (sqlFile.exists()) {
			controller.printLog("Loading " + sqlFile.getAbsolutePath() + "\n");
			pgClient.loadSql(sqlFile);
		}
	}

	public void loadAllSpaSql(String dataDir) {
		if (!startSpaPool.isEmpty()) {
			startSpaPool.clear();
		}
		
		File dir = new File(dataDir);
		if (!dir.exists()) {
			return;
		}
		
		PgClient pgClient = controller.getPgClient();
		
		String[] spaList = listAllSpa();
		for (String spaName : spaList) {
			File sqlFile = new File(dir.getAbsoluteFile() + "/" + spaName + ".sql");
			if (sqlFile.exists()) {
				controller.printLog("Loading " + sqlFile.getAbsolutePath() + "\n");
				pgClient.loadSql(sqlFile);
				
				if (isSvcSpa(spaName)) {
					if (spaName.startsWith("EPAY")) {
						startSpaPool.add(0, spaName);
					} else {
						startSpaPool.add(spaName);
					}					
				}
			}
		}
	}

	public String installSPA(String spaName) {
		return null;
	}

	public void deleteSPA(String spaName) {

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
		//SPAManager spaManager = new SPAManager(controller);
		SPAManager spaManager = controller.getSPAManager();

		//String spaName = "EPAY29C";
		//spaManager.stopSpa(spaName);
		//spaManager.startSpa(spaName);
		//spaManager.applyTrace1(spaName);
		//spaManager.applyBp(spaName);

		//System.out.println(spaManager.getSpaRelease());
		// spaManager.startSpa(spaName);
		// spaManager.applyBp(spaName);
		Case c = new Case("cg3629", "72400", "R27SU6", "Base", "Function", "Base");
		String bpcmd = "ck6692.bp;EPAY_1";
		controller.loadBpInCmd(c, bpcmd);
	}
}
