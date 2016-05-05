/**
 **************************************************************************
 * Name: RTDBManager
 * 
 * Description: Provides RTDB related operation 
 * Author: Liu Hongwei
 * 		   hong_wei.hl.liu@alcatel-lucent.com
 * 
 *************************************************************************
 */
import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RTDBManager {

	private XController controller;
	private static final String auditDbList = System.getProperty("user.home") + "/.sptest/audit.dblist";
	private static final String initDbList = System.getProperty("user.home") + "/.sptest/init.dblist";
	private Set<String> auditDbSet;
	private Set<String> initDbSet;

	public RTDBManager() {
		controller = null;
		initDbSet();
	}

	public RTDBManager(XController c) {
		this.controller = c;
		auditDbSet = new HashSet<String>();
		initDbSet = new HashSet<String>();
		initDbSet();
	}

	public void setController(XController c) {
		controller = c;
	}
	
	public void initDbSet() {
		File fileAuditDb = new File(auditDbList);
		File fileInitDb = new File(initDbList);
		
		if (fileAuditDb.exists()) {
			if (auditDbSet == null) {
				auditDbSet = new HashSet<String>();
			}
			
			try {
				BufferedReader br = new BufferedReader(
						new FileReader(fileAuditDb));
				String db;
				while ((db = br.readLine()) != null) {
					if (db.startsWith("#")) {
						continue;
					}
					auditDbSet.add(db);
				}
				br.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (fileInitDb.exists()) {
			if (initDbSet == null) {
				initDbSet = new HashSet<String>();
			}
			
			try {
				BufferedReader br = new BufferedReader(
						new FileReader(fileInitDb));
				String db;
				while ((db = br.readLine()) != null) {
					if (db.startsWith("#")) {
						continue;
					}
					initDbSet.add(db);
				}
				br.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public boolean createAuditDb() {
		if (auditDbSet == null || auditDbSet.isEmpty()) {
			return false;
		}
		
		Iterator<String> it = auditDbSet.iterator();
		while (it.hasNext()) {
			String db = it.next();
			createDB(db);
		}
		return true;
	}
	
	public void createInitDb() {
		if (initDbSet == null || initDbSet.isEmpty()) {
			return;
		}
		Iterator<String> it = initDbSet.iterator();
		while (it.hasNext()) {
			String db = it.next();
			createDB(db);
		}
	}

	public String[] getDbList() {
		PgClient pgClient = controller.getPgClient();
		String sql = "SELECT db_name FROM rtdb_app " +
				"WHERE db_name NOT IN ('NDB','BDB','HLRV','HLRNV')" +
				" ORDER BY db_name";
		String dbList = pgClient.getSqlResult(sql);
		return dbList.split("\n");
	}

	public boolean isDbInstalled(String dbName) {
		PgClient pgClient = controller.getPgClient();
		String result = pgClient
				.getSqlResult("SELECT db_name FROM rtdb_app WHERE db_name='"
						+ dbName + "'");
		return !result.isEmpty();
	}

	public void createDB(String dbName) {
		if (dbName == null || dbName.isEmpty()) {
			return;
		}
		
		controller.printLog("CREATE DB " + dbName + "\n");
		Host host = controller.getHost();
		String cmd = "/cs/sn/cr/cepexec CREAT_DB \"create:db=" + dbName + "\"";
		host.sendCmd(cmd);
	}

	public void createAllDB() {
		String[] dbList = getDbList();
		String baseDir = controller.getBaseDir();
		File shellScript = new File(baseDir + "/conf/create_all_db.sh");
		if (shellScript.exists()) {
			createDbInParallel(dbList);
		} else {
			for (String dbName : dbList) {
				createDB(dbName);
			}
		}
		
	}
	
	public void createDbInParallel(String[] dbList) {
		if (dbList.length == 0) {
			return;
		}
		
		StringBuilder dbListStr = new StringBuilder();
		for (String db : dbList) {
			dbListStr.append(db).append(" ");
		}
		String baseDir = controller.getBaseDir();
		File shellScript = new File(baseDir + "/conf/create_all_db.sh");
		if (!shellScript.exists()) {
			System.out.println("CREATE DB FAIL: " + shellScript.getAbsolutePath() + " NOT EXIST\n");
			return;
		}
		FTPManager ftpManager = controller.getFTPManager();
		ftpManager.connect();
		ftpManager.uploadFile(shellScript);
		ftpManager.disconnect();
		
		String remoteDir = new String(ftpManager.remoteDir);
		String script = remoteDir + "/create_all_db.sh";
		String cmd = "sed -i 's,DBLIST," +	dbListStr.toString() + ",g' " + script;
		Host host = controller.getHost();
		host.sendCmd(cmd);
		host.sendCmd("chmod 755 " + script);
		host.sendCmd("dos2unix " + script);
		controller.printLog("CREATE DB: " + dbListStr.toString() + "\n");
		host.sendCmd(script);		
	}

	public void loadSingleDB(String dbName, File dbFile) {
		Host host = controller.getHost();
		FTPManager ftpManager = controller.getFTPManager();
		ftpManager.connect();
		ftpManager.uploadFile(dbFile);
		ftpManager.disconnect();

		controller.printLog("LOAD:DB=" + dbName + ": " + dbFile.getAbsolutePath() + "\n");
		String cmd = "/cs/sn/cr/cepexec LOAD_DB \"LOAD:DB=" + dbName
				+ ",file=\\\"/tmp/" + dbFile.getName() + "\\\",UCL\"";
		host.sendCmd(cmd);
	}

	public void loadAllDB(String dataDir) {
		String[] dbList = getDbList();
		String baseDir = controller.getBaseDir();
		File shellScript = new File(baseDir + "/conf/xauto_load_all_db.sh");
		
		if (shellScript.exists()){
			loadDbInParallel(dataDir, dbList);
		} else {
			for (String dbName : dbList) {
				File dbFile = new File(dataDir + dbName + ".data");
				if (dbFile.exists()) {
					loadSingleDB(dbName, dbFile);
				}
			}
		}
	}
	
	public void loadDbInParallel(String dataDir, String[] dbList) {
		if (dbList.length == 0) {
			return;
		}
		StringBuilder dbListStr = new StringBuilder();
		String baseDir = controller.getBaseDir();
		File shellScript = new File(baseDir + "/conf/xauto_load_all_db.sh");
		if (!shellScript.exists()) {
			System.out.println("LOAD DB FAIL: " + shellScript.getAbsolutePath() + " NOT EXIST\n");
		}
		FTPManager ftpManager = controller.getFTPManager();
		ftpManager.connect();
		ftpManager.uploadFile(shellScript);
		for (String dbName : dbList) {			
			File dbFile = new File(dataDir + "/" + dbName + ".data");
			if (!dbFile.exists()) {
				continue;
			}
			dbListStr.append(dbName + " ");
			ftpManager.uploadFile(dbFile);
		}
		ftpManager.disconnect();
		
		String remoteDir = new String(ftpManager.remoteDir);
		String script = remoteDir + "/xauto_load_all_db.sh";
		Host host = controller.getHost();
		host.sendCmd("sed -i 's,DBLIST," + dbListStr.toString() + ",g' " + script);
		host.sendCmd("sed -i 's,REMOTEDIR," + remoteDir + ",g' " + script);
		host.sendCmd("chmod 755 " + script);
		host.sendCmd("dos2unix " + script);
		controller.printLog("LOAD DB : " + dbList + "\n");
		host.sendCmd(script);
	}

	public void loadCaseDB(Case c) {
		String baseDir = controller.getBaseDir();
		String tid = c.getTID();
		String customer = c.getCustomer();
		String rel = c.getRelease();

		String dataDir = baseDir + "/" + customer + "/" + rel + "/rtdb_data/";
		String[] dbList = getDbList();
		for (String dbName : dbList) {
			File dbFile = new File(dataDir + tid + "." + dbName);
			if (dbFile.exists()) {
				loadSingleDB(dbName, dbFile);
			}
		}
	}
	
	public void loadCaseDB1(Case caseToLoad) {
		String baseDir = controller.getBaseDir();
		String tid = caseToLoad.getTID();
		String customer = caseToLoad.getCustomer();
		String rel = caseToLoad.getRelease();

		String dataDir = baseDir + "/" + customer + "/" + rel + "/rtdb_data/";
		String[] dbList = getDbList();
		StringBuilder dbListStr = new StringBuilder();
		
		File shellScript = new File(baseDir + "/conf/xauto_load_all_db.sh");
		FTPManager ftpManager = controller.getFTPManager();
		ftpManager.connect();
		ftpManager.uploadFile(shellScript);
		for (String dbName : dbList) {			
			File dbFile = new File(dataDir + "/" + tid + "." + dbName);
			if (!dbFile.exists()) {
				continue;
			}
			dbListStr.append(dbName + " ");
			ftpManager.uploadFile(dbFile);
		}
		ftpManager.disconnect();
		
		String remoteDir = new String(ftpManager.remoteDir);
		String script = remoteDir + "/xauto_load_all_db.sh";
		Host host = controller.getHost();
		host.sendCmd("sed -i 's,DBLIST," + dbListStr.toString() + ",g' " + script);
		host.sendCmd("sed -i 's,REMOTEDIR," + remoteDir + ",g' " + script);
		host.sendCmd("chmod 755 " + script);
		host.sendCmd("dos2unix " + script);
		controller.printLog("LOAD DB for case "+ tid + " : " + dbList + "\n");
		host.sendCmd(script + " " + tid);
	}

	public void backupDB(String dbName, String destFile) {

	}

	public static void main(String[] args) {
		String hostname = "SPVM53";
		String ip = "135.242.106.118";
		// String hostname = "SPVM138A";
		// String ip = "135.242.17.41";
		int port = 23;
		String username = "ainet";
		String passwd = "ainet1";

		Host host = new Host(hostname, ip, port, username, passwd);

		XController controller = new XController();
		controller.setBaseDir("D:/Workspace/XAuto/automation/R29SUC");
		controller.setHost(host);
		RTDBManager rtdbManager = controller.getRTDBManager();
		String dataDir = controller.getBaseDir() + "/Base/R27SU6/rtdb_data";
		rtdbManager.createAllDB();
		rtdbManager.loadAllDB(dataDir);
		Case c = new Case("ep5099", "725726", "R28SU5", "Eplus", "Audit", "Eplus");
		rtdbManager.loadCaseDB1(c);
		// File dbFile = new
		// File("D:/Workspace/XAuto/conf/VFUK/R28SUE/rtdb_data/SIMDB28D.data");
		// rdbMgr.loadSingleDB("SIMDB28D", dbFile);
	}

}
