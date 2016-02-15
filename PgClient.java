import java.sql.*;
import java.io.*;

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

	public static final String DB_USER = "scncraft";
	public static final String DB_PASSWD = "scncraft123";

	public PgClient(String ip, int port) {
		try {
			this.ip = ip;
			this.port = port;
			Class.forName("org.postgresql.Driver").newInstance();
			serverUrl = "jdbc:postgresql://" + this.ip + ":" + port + "/A?useUnicode=true&characterEncoding=sql_ascii";
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public PgClient(String ip) {
		try {
			this.ip = ip;
			this.port = 5333;
			Class.forName("org.postgresql.Driver").newInstance();
			serverUrl = "jdbc:postgresql://" + this.ip + ":" + port + "/A?useUnicode=true&characterEncoding=sql_ascii";
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public PgClient(Host host) {
		try {
			this.ip = host.getIP();
			this.port = 5333;
			Class.forName("org.postgresql.Driver").newInstance();
			serverUrl = "jdbc:postgresql://" + this.ip + ":" + port + "/A?useUnicode=true&characterEncoding=sql_ascii";
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public void setController(XController c) {
		controller = c;
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
		String sep = new String("\n");

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

	/**
	 * Generate one update record. Some table's key can be NULL and updated,
	 * such as TF, RID, in this case, string "GETDATA" should be appended after
	 * old keys
	 */
	public String genUpd(String tableName, String oldData, String newData) {
		StringBuilder frmRec = new StringBuilder("FORM=" + tableName + "&CHG,");
		StringBuilder changedKey = new StringBuilder("GETDATA,");
		boolean keyChanged = false;

		String fieldRec, fieldName;
		String[] fieldMapPair;
		String[] oldValue = splitByComma(oldData);
		String[] newValue = splitByComma(newData);

		try {
			// To get field name from pre processed file <SPA_NAME>.map
			File tableMap = new File("spamap/" + tableName + ".map");
			BufferedReader tableBuffer = new BufferedReader(new FileReader(
					tableMap));

			int i = 0;
			while ((fieldRec = tableBuffer.readLine()) != null) {
				fieldMapPair = fieldRec.split(" ", 2);
				fieldName = fieldMapPair[0];

				if (fieldName.startsWith("index")) {
					frmRec.append(fieldName + "=\"" + oldValue[i] + "\",");
					if (!newValue[i].equals(oldValue[i])) {
						changedKey.append(fieldName + "=\"" + newValue[i]
								+ "\",");
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
							frmRec.append(fieldName + "=" + newValue[i] + ",");
						} else {
							frmRec.append(fieldName + "=\"" + newValue[i]
									+ "\",");
						}
					}
				}
				i++;
			}

			frmRec.append("CHG!\n");
			tableBuffer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return frmRec.toString();
	}

	/**
	 * To generate a insert record in frm if the value is NULL, it won't be in
	 * the record
	 */
	public String genNew(String tableName, String newData) {
		StringBuilder frmRec = new StringBuilder("FORM=" + tableName + "&NEW,");
		String[] newValue = splitByComma(newData);

		String[] fieldMapPair;
		String fieldName, fieldRec;

		try {
			File tableMap = new File("spamap/" + tableName + ".map");
			BufferedReader tableBuffer = new BufferedReader(new FileReader(
					tableMap));

			int i = 0;
			while ((fieldRec = tableBuffer.readLine()) != null) {
				fieldMapPair = fieldRec.split(" ", 2);
				fieldName = fieldMapPair[0];
				// If field value starts and ends with quote, add it directly
				// if not, add wrap it with quote and add into frm record
				if (newValue[i].startsWith("\"") && newValue[i].endsWith("\"")) {
					frmRec.append(fieldName + "=" + newValue[i] + ",");
				} else if (!newValue[i].isEmpty()) {
					frmRec.append(fieldName + "=\"" + newValue[i] + "\",");
				}

				i++;
			}

			frmRec.append("NEW!\n");
			tableBuffer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return frmRec.toString();
	}

	/**
	 * This function is to generate a delete record in frm, for deleting, only
	 * need get Non NULL index fields
	 */
	public String genOut(String tableName, String oldData) {
		StringBuilder frmRec = new StringBuilder("FORM=" + tableName + "&OUT,");
		String[] oldValue = splitByComma(oldData);

		String[] fieldMapPair;
		String fieldName, fieldRec;
		try {
			File tableMap = new File("spamap/" + tableName + ".map");
			BufferedReader tableBuffer = new BufferedReader(new FileReader(
					tableMap));

			int i = 0;
			while ((fieldRec = tableBuffer.readLine()) != null) {
				fieldMapPair = fieldRec.split(" ", 2);
				fieldName = fieldMapPair[0];
				// When the field's name is not started with index
				// it means all indexes have been added into frm, so break
				// This condition can also be moved to while condition,
				// but that will mean to read the whole map file
				if (!fieldName.startsWith("index")) {
					break;
				}
				if (oldValue[i].isEmpty()) {
					i++;
					continue;
				}

				frmRec.append(fieldName + "=\"" + oldValue[i] + "\",");
				i++;
			}

			frmRec.append("OUT!\n");
			tableBuffer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return frmRec.toString();
	}

	public void genFrm(String frmNamePrefix) {
		String frmName = "frmdir/" + frmNamePrefix + ".frm";
		StringBuilder frmRecord = new StringBuilder();

		String sql = "SELECT * FROM rcdata";
		try {
			PrintWriter frmWriter = new PrintWriter(new BufferedWriter(
					new FileWriter(new File(frmName))));
			ResultSet rs = pgStatement.executeQuery(sql);

			String tableName = null;
			String operation = null;
			String oldData = null;
			String newData = null;

			while (rs.next()) {
				tableName = rs.getString("form");
				operation = rs.getString("op");

				if (operation.equals("UPDATE")) {
					oldData = rs.getString("old_data").replaceAll("[()]", "");
					newData = rs.getString("new_data").replaceAll("[()]", "");
					frmRecord.append(genUpd(tableName, oldData, newData));
				} else if (operation.equals("INSERT")) {
					newData = rs.getString("new_data").replaceAll("[()]", "");
					frmRecord.append(genNew(tableName, newData));
				} else if (operation.equals("DELETE")) {
					oldData = rs.getString("old_data").replaceAll("[()]", "");
					frmRecord.append(genOut(tableName, oldData));
				} else {
					System.out.println("invalid operation: " + operation);
					continue;
				}
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
		String frmName = "frmdir/" + frmNamePrefix + ".frmbk";
		StringBuilder frmbkRecord = new StringBuilder();

		String sql = "SELECT * FROM rcdata";
		try {
			PrintWriter frmWriter = new PrintWriter(new BufferedWriter(
					new FileWriter(new File(frmName))));
			ResultSet rs = pgStatement.executeQuery(sql);

			String tableName = null;
			String operation = null;
			String oldData = null;
			String newData = null;

			while (rs.next()) {
				tableName = rs.getString("form");
				operation = rs.getString("op");

				if (operation.equals("UPDATE")) {
					oldData = rs.getString("old_data").replaceAll("[()]", "");
					newData = rs.getString("new_data").replaceAll("[()]", "");
					frmbkRecord.insert(0, genUpd(tableName, newData, oldData));
				} else if (operation.equals("INSERT")) {
					newData = rs.getString("new_data").replaceAll("[()]", "");
					frmbkRecord.insert(0, genOut(tableName, newData));
				} else if (operation.equals("DELETE")) {
					oldData = rs.getString("old_data").replaceAll("[()]", "");
					frmbkRecord.insert(0, genNew(tableName, oldData));
				} else {
					System.out.println("invalid operation: " + operation);
					continue;
				}
			}

			frmWriter.print(frmbkRecord.toString());
			rs.close();
			frmWriter.close();
			// clearRCdata();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		} catch (Exception e) {
			e.printStackTrace();
		}

		return spaList.toString();
	}

	public String getSqlResult(String sql) {
		if (sql == null || sql.isEmpty()) {
			return null;
		}

		StringBuilder resultList = new StringBuilder();
		try {
			ResultSet rs = pgStatement.executeQuery(sql);
			while (rs.next()) {
				resultList.append(rs.getString(1) + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultList.toString();
	}

	public boolean clearRCdata() throws SQLException {
		return pgStatement.execute("DELETE FROM rcdata");
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

	public void genRCtrackerInstall() {

	}

	public void genRCtrackerUninstall() {

	}

	public void openRCtracker() {

	}

	public void closeRCtracker() {

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String caseId = "et1234";
		PgClient sqlServer = new PgClient("135.242.106.118", 5333);
		// PgClient sqlServer = new PgClient("135.242.17.41", 5333);
		sqlServer.pgConnect();
		// sqlServer.genFrm(caseId);
		// sqlServer.genFrmbk(caseId);
		long start = System.currentTimeMillis();
		String db = "TIDRTDB";
		String sql = "select db_name from rtdb_app where db_name like '" + db + "%'";
		System.out.println(sqlServer.getSqlResult(sql).split("\n").length);
		File sqlFile = new File("D:/Workspace/XAuto/automation/R29SUC/Base/R27SU6/spa_data/EPAY29C.sql");
		
		sqlServer.loadSql(sqlFile);
		/*
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
		*/
		long end = System.currentTimeMillis();
		long duration = (end - start) / 1000;
		System.out.println("time used: " + duration);
		
		/*
		 * System.out.println(sqlServer.getSpaList());
		 * System.out.println(sqlServer.getISSpaList()); String spaName =
		 * "EPPSM29C"; String result =
		 * sqlServer.getSqlResult("select span from spm_tbl where span='" +
		 * spaName + "'"); System.out.println(spaName + " status is: " +
		 * !result.isEmpty()); long end=System.currentTimeMillis(); long
		 * duration = end - start; System.out.println("time used: " + duration);
		 * 
		 * String sql = "SELECT db_name FROM rtdb_app " +
		 * "WHERE db_name like 'AIRTDB%' " + "OR db_name LIKE 'SIMDB%' " +
		 * "OR db_name LIKE 'UARTDB%'";
		 * System.out.println(sqlServer.getSqlResult(sql));
		 */
		sqlServer.pgDisconnect();

	}

}
