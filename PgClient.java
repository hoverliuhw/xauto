import java.net.SocketException;
import java.sql.*;
import java.io.*;

class SymFileFilter implements FilenameFilter {
	String spaName;
	public SymFileFilter(String spaName) {
		this.spaName = spaName;
	}
	
	public boolean accept(File dir, String name) {
		return name.equalsIgnoreCase(spaName + ".sym");
	}
}

class SuffixFileFilter implements FilenameFilter {
	String suffix;
	public SuffixFileFilter(String suffix) {
		this.suffix = suffix;
	}
	
	public boolean accept(File dir, String name) {
		return name.endsWith(suffix);
	}
}

class PrefixFileFilter implements FilenameFilter {
	String prefix;
	public PrefixFileFilter (String prefix) {
		this.prefix = prefix;
	}
	
	public boolean accept(File dir, String name) {
		return name.startsWith(prefix);
	}
}

public class PgClient {

	/**
	 * @param args
	 */
	private String ip;
	private int port;
	private String serverUrl;
	private Connection pgConnection;
	private Statement pgStatement;

	private XController controller;
	private boolean isRCtrackerOpen;
	
	/**
	 * Strings for RCtracker, because they are long,
	 * so make them instance variables
	 * */
	private String sqlCreateRCDataFunction;
	private String sqlAddToRCDataFunction;
	private String sqlCreateTriggerFunction;
	private String sqlCreateTableRCData;
	private String sqlDropFunctionCreateRCData;
	private String sqlDropFunctionAddToRCdata;
	private String sqlDropFunctionCreateTrigger;
	private String sqlDropTableRCData;
	private String[] spaRCtableList;

	private static final String DB_USER = "scncraft";
	private static final String DB_PASSWD = "scncraft123";

	public PgClient(String ip, int port) {
		controller = null;
		isRCtrackerOpen = false;
		try {
			this.ip = ip;
			this.port = port;
			Class.forName("org.postgresql.Driver").newInstance();
			serverUrl = "jdbc:postgresql://" + this.ip + ":" + port + "/A?useUnicode=true&characterEncoding=sql_ascii";
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		sqlCreateRCDataFunction = null;
		sqlAddToRCDataFunction = null;
		sqlCreateTriggerFunction = null;
		sqlCreateTableRCData = null;
		sqlDropFunctionCreateRCData = null;
		sqlDropFunctionAddToRCdata = null;
		sqlDropFunctionCreateTrigger = null;
		sqlDropTableRCData = null;
		spaRCtableList = null;
	}

	public PgClient(String ip) {
		controller = null;
		isRCtrackerOpen = false;
		try {
			this.ip = ip;
			this.port = 5333;
			Class.forName("org.postgresql.Driver").newInstance();
			serverUrl = "jdbc:postgresql://" + this.ip + ":" + port + "/A?useUnicode=true&characterEncoding=sql_ascii";
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		sqlCreateRCDataFunction = null;
		sqlAddToRCDataFunction = null;
		sqlCreateTriggerFunction = null;
		sqlCreateTableRCData = null;
		sqlDropFunctionCreateRCData = null;
		sqlDropFunctionAddToRCdata = null;
		sqlDropFunctionCreateTrigger = null;
		sqlDropTableRCData = null;
		spaRCtableList = null;
	}

	public PgClient(Host host) {
		controller = null;
		isRCtrackerOpen = false;
		try {
			this.ip = host.getIP();
			this.port = 5333;
			Class.forName("org.postgresql.Driver").newInstance();
			serverUrl = "jdbc:postgresql://" + this.ip + ":" + port + "/A?useUnicode=true&characterEncoding=sql_ascii";
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		sqlCreateRCDataFunction = null;
		sqlAddToRCDataFunction = null;
		sqlCreateTriggerFunction = null;
		sqlCreateTableRCData = null;
		sqlDropFunctionCreateRCData = null;
		sqlDropFunctionAddToRCdata = null;
		sqlDropFunctionCreateTrigger = null;
		sqlDropTableRCData = null;
		spaRCtableList = null;
	}

	public void pgConnect() {
		try {
			pgConnection = DriverManager.getConnection(serverUrl, DB_USER,
					DB_PASSWD);
			pgStatement = pgConnection.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void pgDisconnect() {
		try {
			pgStatement.close();
			pgConnection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setController(XController controller) {
		this.controller = controller;
	}
	
	public void setRCtrackerFlag(boolean flag) {
		isRCtrackerOpen = flag;
	}
	
	public boolean isRCtrackerOpen() {
		return isRCtrackerOpen;
	}
	
	public void genRCtrackerInstall() throws SQLException {
		sqlCreateRCDataFunction = "create or replace function create_rcdata() returns text as\n" +
				"$BODY$\n" +
				"begin\n" +
				"perform * from pg_tables where tablename='rcdata' and tableowner='scncraft';\n" +
				"    if found then\n" +
				"        drop table rcdata;\n" +
				"    end if;\n" +
				"    create table rcdata (\n" +
				"        id serial primary key,   -- id, table name'_'column name'_'seq\n" +
				"        form text not null,      -- form name\n" +
				"        op char(6) not null,     -- operation type\n" +
				"        old_data text,           -- old rc data\n" +
				"        new_data text,           -- new rc data\n" +
				"        time timestamp not null  -- date and time\n" +
				"    );\n" +
				"    return 'success';\n" +
				"end;\n" +
				"$BODY$ language 'plpgsql';\n";
		
		sqlAddToRCDataFunction = "create or replace function addto_rcdata() returns trigger as\n" +
				"$BODY$\n" +
				"declare\n" +
				"    old_row text;\n" +
				"    new_row text;\n" +
				"begin\n" +
				"    if TG_OP = 'INSERT'    -- insert\n" +
				"    then\n" +
				"        select into new_row NEW;\n" +
				"        insert into rcdata(form, op, time, new_data)\n" +
				"            select upper(TG_RELNAME), TG_OP, now(), new_row;\n" +
				"        return NEW;\n" +
				"    elsif TG_OP = 'UPDATE' -- update\n" +
				"    then\n" +
				"        select into old_row OLD;\n" +
				"        select into new_row NEW;\n" +
				"        insert into rcdata(form, op, time, old_data, new_data)\n" +
				"            select upper(TG_RELNAME), TG_OP, now(), old_row, new_row;\n" +
				"        return NEW;\n" +
				"    elsif TG_OP = 'DELETE' -- delete\n" +
				"    then\n" +
				"        select into old_row OLD;\n" +
				"        insert into rcdata(form, op, time, old_data)\n" +
				"            select upper(TG_RELNAME), TG_OP, now(), old_row;\n" +
				"        return OLD;\n" +
				"    end if;\n" +
				"end;\n" +
				"$BODY$ language 'plpgsql';";
		sqlCreateTriggerFunction = "create or replace function create_trigger(table_name text) returns text as\n" +
				"$BODY$\n" +
				"declare\n" +
				"    trigger_name text := table_name || '_trigger';\n" +
				"begin\n" +
				"perform * from pg_trigger where tgname=quote_ident(lower(trigger_name));\n" +
				"    if found then\n" +
				"        execute 'drop trigger ' || trigger_name || ' on ' || table_name;\n" +
				"    end if;\n" +
				"    execute 'create trigger ' || trigger_name || ' before insert or update or delete on ' || table_name || ' for each row execute procedure addto_rcdata();';\n" +
				"    return 'success';\n" +
				"end;\n" +
				"$BODY$ language 'plpgsql';\n";
		sqlCreateTableRCData = "select create_rcdata() as \"create table rcdata: \"";
		
		ResultSet rs = pgStatement.executeQuery("select table_name from cat " +
				"where table_name like 'SPA\\_%' " +
				"and table_type='TABLE' " +
				"and table_name not in ('SPA_PARAMS', 'SPA_PROCESS', 'SPA_TBL') " +
				"order by table_name");
		StringBuilder sb = new StringBuilder();
		while(rs.next()) {
			sb.append(rs.getString(1) + "\n");
		}
		spaRCtableList = sb.toString().split("\n");
		
		rs.close();
	}

	public void genRCtrackerUninstall() {
		sqlDropFunctionCreateRCData = "drop function create_rcdata()";
		sqlDropFunctionAddToRCdata = "drop function addto_rcdata()";
		sqlDropFunctionCreateTrigger = "drop function create_trigger(text)";
		sqlDropTableRCData = "drop table rcdata";
	}

	public void openRCtracker() {
		if (isRCtrackerOpen()) {
			return;
		}
		try {
			if (sqlCreateRCDataFunction == null) {
				genRCtrackerInstall();
				genRCtrackerUninstall();
			}

			pgStatement.execute(sqlCreateRCDataFunction);
			pgStatement.execute(sqlCreateTableRCData);
			pgStatement.execute(sqlAddToRCDataFunction);
			pgStatement.execute(sqlCreateTriggerFunction);

			for (String tableName : spaRCtableList) {
				pgStatement.execute("select create_trigger('" + tableName + "') " +
							"as \"create trigger " + tableName + "_trigger: \";");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		setRCtrackerFlag(true);
	}

	public void closeRCtracker() {
		if (!isRCtrackerOpen()) {
			return;
		}
		try {
			if (sqlCreateRCDataFunction == null) {
				genRCtrackerInstall();
				genRCtrackerUninstall();
			}

			pgStatement.execute(sqlDropTableRCData);
			for (String tableName : spaRCtableList) {
				pgStatement.execute("drop trigger " + tableName + "_trigger on " +
							tableName +";");
			}
			pgStatement.execute(sqlDropFunctionCreateTrigger);
			pgStatement.execute(sqlDropFunctionAddToRCdata);
			pgStatement.execute(sqlDropFunctionCreateRCData);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		setRCtrackerFlag(false);
	}

	
	/**
	 * From rcdata, all fields' value are stored in one string separated by
	 * comma, Because field value may contain special character such as comma or
	 * quote, So we can't simply use String.split by comma to separate field
	 * value, In this Function, comma is replaced by "\n", then split
	 */
	public String[] splitByComma(String str) {
		String[] splitResult = null;
		StringBuilder strBuffer = new StringBuilder();

		int start = 0, end = 0, index = 0, len = 0;
		String subStr;
		String sep = "\n";

		while ((end = str.indexOf(",", index)) > -1) {
			subStr = str.substring(start, end);
			if (subStr.startsWith("\"") && !subStr.endsWith("\"")) {
				index = end + 1;
			} else {
				strBuffer.append(subStr + sep);
				len++;
				start = end + 1;
				index = start;
			}
		}
		// To process the field after the last comma
		end = str.length();
		subStr = str.substring(start, end);
		strBuffer.append(subStr);
		len++;

		// Because the last field value maybe null, the length of the array has
		// to be specified
		splitResult = strBuffer.toString().split(sep, len);
		return splitResult;
	}
	
	/***
	 * Get RC field list, tableName should be SPA_SPANAME_XXX
	 * Because some SPA like AethosTest is not in upper cases
	 * So it called psql table spa_tbl to get real SPA name
	 */
	public String[] getFieldList(String tableName) {
		StringBuilder sb = new StringBuilder();
		int start = tableName.indexOf("_") + 1;
		int end = tableName.indexOf("_", start);
		String spaName = tableName.substring(start, end);
		
		File sym = new File(controller.getBaseDir() + "/sym/" + spaName + ".sym");
		if (!sym.exists()) {
			File symDir = new File(controller.getBaseDir() + "/sym");
			File[] symList = symDir.listFiles(new SymFileFilter(spaName));
			if (symList.length > 0) {
				sym = symList[0];
			} else {
				spaName = getSqlResult("select span from spa_tbl where upper(span)='" + spaName + "'");
				
				if (!spaName.isEmpty()) {
					spaName = spaName.substring(0, spaName.indexOf("\n"));
				} else {
					return null;
				}
				
				FTPManager ftpManager = controller.getFTPManager();
				ftpManager.connect();
				ftpManager.downloadFile("/sn/sps/" + spaName + "/" + spaName + ".sym", sym.getAbsolutePath());
				ftpManager.disconnect();
			}
			
			if (!sym.exists()) {
				return null;
			}
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(sym));
			String line = null;
			String nameLine = null;
			while ((line = br.readLine()) != null) {
				if (line.startsWith(tableName)) {
					nameLine = line;
					break;
				}
			}
			
			if (nameLine == null || 
					nameLine.contains(";static")) {
				br.close();
				return null;
			}
		
			start = nameLine.indexOf(";") + 1;
			end = nameLine.indexOf(";", start);
			int length = Integer.parseInt(nameLine.substring(start, end));
			
			for (int i = 0; i < length; i++) {
				line = br.readLine();
				start = line.indexOf(";") + 1;
				end = line.indexOf(";", start);
				String fieldName = line.substring(start, end);
				start = fieldName.indexOf("[].");
				if (start >= 0) {
					fieldName = fieldName.substring(start + "[].".length());
				}
				if (fieldName.equals("SEQ")) {
					fieldName = "sequence";
				}
				sb.append(fieldName + "\n");
			}
		
			br.close();
		} catch (IOException e) {
			return null;
		}
		return sb.toString().split("\n");
	}

	/**
	 * Generate one update record. Some table's key can be NULL and updated,
	 * such as TF, RID, or key is sequence such as fsm,
	 * in this case, string "GETDATA" should be appended after
	 * old keys
	 */
	public String genUpd(String tableName, String oldData, String newData) {
		String[] fieldList = getFieldList(tableName);
		if (fieldList == null) {
			return "";
		}
		String sep = "! ";
		StringBuilder frmRec = new StringBuilder("FORM=" + tableName + "&CHG" + sep);
		StringBuilder changedKey = new StringBuilder("GETDATA" + sep);
		boolean keyChanged = false;
		
		String[] oldValue = splitByComma(oldData);
		String[] newValue = splitByComma(newData);
		for (int i = 0; i < fieldList.length; i++) {
			String fieldName = fieldList[i];
			if (fieldName.startsWith("index") || fieldName.equals("sequence")) {
				frmRec.append(fieldName + "=\"" + oldValue[i] + "\"" + sep);
				if (!newValue[i].equals(oldValue[i])) {
					changedKey.append(fieldName + "=\"" + newValue[i]
							+ "\"" + sep);
					keyChanged = true;
				}
				if (fieldName.equals("sequence")) {
					keyChanged = true;
				}
			} else {
				if (keyChanged) {
					frmRec.append(changedKey.toString());
					keyChanged = false;
				}
				if (!newValue[i].equals(oldValue[i])) {
					if (newValue[i].startsWith("\"")
							&& newValue[i].endsWith("\"")) {
						frmRec.append(fieldName + "=" + newValue[i] + sep);
					} else {
						frmRec.append(fieldName + "=\"" + newValue[i]
								 + "\"" + sep);
					}
				}
			}
		}
		frmRec.append("CHG!\n");
		return frmRec.toString();
	}

	/**
	 * To generate a insert record in frm if the value is NULL, it won't be in
	 * the record
	 */
	public String genNew(String tableName, String newData) {
		String[] fieldList = getFieldList(tableName);
		if (fieldList == null) {
			return "";
		}
		
		String sep = "! ";
		String[] newValue = splitByComma(newData);
				
		StringBuilder frmRec = new StringBuilder("FORM=" + tableName + "&NEW" + sep);
		for (int i = 0; i < fieldList.length; i++) {
			String fieldName = fieldList[i];
			// If field value starts and ends with quote, add it directly
			// if not, add wrap it with quote and add into frm record
			if (newValue[i].startsWith("\"") && newValue[i].endsWith("\"")) {
				frmRec.append(fieldName + "=" + newValue[i] + sep);
			} else if (!newValue[i].isEmpty()) {
				frmRec.append(fieldName + "=\"" + newValue[i] + "\"" + sep);
			}
		}
		frmRec.append("NEW!\n");
		
		return frmRec.toString();
	}

	/**
	 * This function is to generate a delete record in frm, for deleting, only
	 * need get Non NULL index fields
	 */
	public String genOut(String tableName, String oldData) {
		String[] fieldList = getFieldList(tableName);
		if (fieldList == null) {
			return "";
		}
		
		String sep = "! ";
		StringBuilder frmRec = new StringBuilder("FORM=" + tableName + "&OUT" + sep);
		String[] oldValue = splitByComma(oldData);
		
		for (int i = 0; i < fieldList.length; i++) {
			String fieldName = fieldList[i];
			// When the field's name is not started with index
			// it means all indexes have been added into frm, so break
			// This condition can also be moved to while condition,
			// but that will mean to read the whole map file
			if (!fieldName.startsWith("index")) {
				break;
			}
			if (oldValue[i].isEmpty()) {
				continue;
			}

			frmRec.append(fieldName + "=\"" + oldValue[i] + "\"" + sep);
		}
		frmRec.append("OUT!\n");

		return frmRec.toString();
	}

	public void genFrm(String frmNamePrefix) {
		File frmdir = new File(controller.getBaseDir() + "/" + XController.CACHE_FRM_DIR);
		if (!frmdir.exists()) {
			frmdir.mkdir();
		}
		String frmName = frmdir + "/" + frmNamePrefix + ".frm";
		StringBuilder frmRecord = new StringBuilder();

		String sql = "SELECT * FROM rcdata";
		try {
			PrintWriter frmWriter = new PrintWriter(new BufferedWriter(
					new FileWriter(frmName)));
			ResultSet rs = pgStatement.executeQuery(sql);

			String tableName = null;
			String operation = null;
			String oldData = null;
			String newData = null;

			while (rs.next()) {
				String frmLine = null;
				tableName = rs.getString("form");
				operation = rs.getString("op");
				if (operation.equals("UPDATE")) {
					oldData = rs.getString("old_data").replaceAll("[()]", "");
					newData = rs.getString("new_data").replaceAll("[()]", "");
					frmLine = genUpd(tableName, oldData, newData);
				} else if (operation.equals("INSERT")) {
					newData = rs.getString("new_data").replaceAll("[()]", "");
					frmLine =  genNew(tableName, newData);
				} else if (operation.equals("DELETE")) {
					oldData = rs.getString("old_data").replaceAll("[()]", "");
					frmLine = genOut(tableName, oldData);
				} else {
					System.out.println("invalid operation: " + operation);
					continue;
				}

				frmRecord.append(frmLine);
			}

			frmWriter.print(frmRecord.toString());
			rs.close();
			frmWriter.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}

	public void genFrmbk(String frmNamePrefix) {
		File frmdir = new File(controller.getBaseDir() + "/" + XController.CACHE_FRM_DIR);
		if (!frmdir.exists()) {
			frmdir.mkdir();
		}
		String frmName = frmdir + "/" + frmNamePrefix + ".frmbk";
		StringBuilder frmbkRecord = new StringBuilder();

		String sql = "SELECT * FROM rcdata";
		try {
			PrintWriter frmWriter = new PrintWriter(new BufferedWriter(
					new FileWriter(frmName)));
			ResultSet rs = pgStatement.executeQuery(sql);

			String tableName = null;
			String operation = null;
			String oldData = null;
			String newData = null;
			while (rs.next()) {
				String frmbkLine = null;
				tableName = rs.getString("form");
				operation = rs.getString("op");
				
				if (operation.equals("UPDATE")) {
					oldData = rs.getString("old_data").replaceAll("[()]", "");
					newData = rs.getString("new_data").replaceAll("[()]", "");
					frmbkLine = genUpd(tableName, newData, oldData);
				} else if (operation.equals("INSERT")) {
					newData = rs.getString("new_data").replaceAll("[()]", "");
					frmbkLine = genOut(tableName, newData);
				} else if (operation.equals("DELETE")) {
					oldData = rs.getString("old_data").replaceAll("[()]", "");
					frmbkLine = genNew(tableName, oldData);
				} else {
					System.out.println("invalid operation: " + operation);
					continue;
				}
				
				frmbkRecord.append(frmbkLine);
			}
			
			frmWriter.print(frmbkRecord.toString());
			rs.close();
			frmWriter.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void clearRCdata(){
		if (isRCtrackerOpen()) {
			try {
				pgStatement.execute("TRUNCATE TABLE rcdata");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}

	public String getDbList() {
		String sql = "SELECT db_name FROM rtdb_app WHERE db_name NOT IN ('NDB','BDB','HLRV','HLRNV') ORDER BY db_name";
		StringBuilder dbList = new StringBuilder();
		try {
			ResultSet rs = pgStatement.executeQuery(sql);
			while (rs.next()) {
				dbList.append(rs.getString(1) + "\n");
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dbList.toString();
	}

	public String getSpaList() {
		String sql = "SELECT span FROM spa_tbl ORDER BY span";
		StringBuilder spaList = new StringBuilder();
		try {
			ResultSet rs = pgStatement.executeQuery(sql);
			while (rs.next()) {
				spaList.append(rs.getString(1) + "\n");
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return spaList.toString();
	}

	public String getISSpaList() {
		String sql = "SELECT span FROM spm_tbl ORDER BY span";
		StringBuilder spaList = new StringBuilder();
		try {
			ResultSet rs = pgStatement.executeQuery(sql);
			while (rs.next()) {
				spaList.append(rs.getString(1) + "\n");
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return spaList.toString();
	}

	public String getSqlResult(String sql) {
		if (sql == null || sql.isEmpty()) {
			return "";
		}

		StringBuilder resultList = new StringBuilder();
		try {
			ResultSet rs = pgStatement.executeQuery(sql);
			while (rs.next()) {
				resultList.append(rs.getString(1) + "\n");
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
			return resultList.toString();
		}

		return resultList.toString();
	}

	public void loadSql(File sqlFile) {
		if (!sqlFile.exists()) {
			System.out.println("sql file " + sqlFile.getAbsoluteFile()
					+ " does not exist!");
			return;
		}
		System.out.println("Loading sql file: " + sqlFile.getAbsolutePath());
		try {
			//BufferedReader sqlReader = new BufferedReader(new FileReader(
				//	sqlFile));
			BufferedReader sqlReader = new BufferedReader(
					new InputStreamReader(
							new FileInputStream(sqlFile), "ascii"));
			
			String line = null;
			int index = 0;
			pgConnection.setAutoCommit(false);
			while ((line = sqlReader.readLine()) != null) {
				if ((index % 10000) == 0) {
					System.out.println("index: " + index);
				}
				index++;

				if (line.startsWith("psql") || line.startsWith("COMMIT")
						|| line.startsWith("!eof") || line.startsWith("BEGIN")
						|| line.startsWith("END") || line.isEmpty()) {

					continue;
				}
				
				String encodingLine = new String(line.getBytes(), "utf8");
				pgStatement.addBatch(encodingLine);
			}
			pgStatement.executeBatch();
			pgConnection.commit();
			pgConnection.setAutoCommit(true);

			sqlReader.close();
		} catch (Exception e) {
			try {
				pgConnection.setAutoCommit(true);
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println(sqlFile.getName());
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws SocketException, IOException {
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();
		
		String hostname = "SPVM53";
		String ip = "135.242.106.116";
		int port = 23;
		String username = "ainet";
		String passwd = "ainet1";
		Host host = new Host(hostname, ip, port, username, passwd);
		host.login();
		
		XController controller = new XController();
		controller.setBaseDir("D:/Automation/R29SUC");
		controller.setHost(host);
		PgClient pgClient = controller.getPgClient();
		
		/*******************************************************************
		 * Test RCtracker
		 */
		String tid = "complex";
		String customer = "VFI";
		String rel = "R28SU6";
		String frm = controller.getBaseDir() + "/" + customer + "/"+ rel + "/res/" + tid + ".frm";
		String frmbk = frm + "bk";
		pgClient.openRCtracker();
		host.loadFrm(frm);
		
		pgClient.genFrm(tid);
		pgClient.genFrmbk(tid);
		
		host.loadFrmbk(frmbk);
		pgClient.clearRCdata();
		
		pgClient.closeRCtracker();
		/*******************************************************************
		 * Load sql files for all customers and all releases, 
		 * this is to test syntax errors in sql
		String[] customerList = {"BSNL", "BSNLVPN", "Base", "EU", "Eplus", "VFCZ", 
				"VFDE", "VFGH", "VFGR", "VFHU", "VFI", "VFNL", "VFP", "VFQ", "VFUK"};
		
		for (String customer : customerList) {
			File dir = new File("D:/Workspace/XAuto/automation/R29SUC/" + customer);
			String[] releaseList = dir.list();
			for (String rel : releaseList) {
				File dataDir = new File(dir + "/" + rel + "/spa_data");
				System.out.println("+++++++++++++++++++++++ " + dataDir.getAbsolutePath() + " +++++++++++++++++++++++++");
				for (File spaSql : dataDir.listFiles()) {
					System.out.println("loading " + spaSql.getAbsolutePath());
					sqlServer.loadSql(spaSql);
				}			
			}
		}
		********************************************************************/
		pgClient.pgDisconnect();
		host.disconnect();
		long end = System.currentTimeMillis();
		long duration = (end - start) / 1000;
		System.out.println("time used: " + duration);

	}

}
