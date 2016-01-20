import org.apache.commons.net.ftp.*;
import java.io.*;

public class FTPManager {

	private FTPClient ftp = null;
	private XController controller = null;
	private String ip = null;
	private String username = null;
	private String passwd = null;
	private String localDir = null;
	public static final String remoteDir = "/tmp";

	/**
	 * @param args
	 */

	public FTPManager() {

		ftp = new FTPClient();
		ip = "135.242.106.118";
		username = "ainet";
		passwd = "ainet1";
		localDir = "conf/";
	}

	public FTPManager(Host host) {
		ftp = new FTPClient();

		ip = host.getIP();
		username = host.getUserName();
		passwd = host.getPasswd();
	}

	public void setController(XController c) {
		controller = c;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public void setLocalDir(String dir) {
		this.localDir = dir;
	}

	/*
	 * public void setRemoteDir(String dir) { this.remoteDir = dir; }
	 */

	public void connect() {
		try {
			int port = 21;
			ftp.connect(ip, port);
			// System.out.println("ftp client connected");

			ftp.login(username, passwd);

			ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
			// FTPClientConfig conf = new
			// FTPClientConfig(FTPClientConfig.SYST_UNIX);
			// ftp.configure(conf);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void downloadFile() {
		try {
			boolean changedir = ftp.changeWorkingDirectory(remoteDir);
			if (changedir) {
				ftp.setControlEncoding("UTF-8");
				String remoteFileName = "test.log";
				String localFileName = "localtest.log";
				System.out.println("filename: " + localFileName);
				ftp.setBufferSize(1024);
				System.out.println("buffer size is: " + ftp.getBufferSize());

				OutputStream out = null;
				File localFile = new File(localDir + localFileName);
				out = new FileOutputStream(localFile);
				
				ftp.retrieveFile(remoteFileName, out);
				System.out.println("get log done");
				//System.out.println("delete file success? " + ftp.deleteFile(remoteFileName));
				out.flush();
				out.close();
			} else {
				System.out.println("dir does not exist!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void downloadCaseLog(Case c) {
		StringBuilder sb = new StringBuilder();
		sb.append(controller.getBaseDir());
		// sb.append("D:\\Workspace\\XAuto");
		// sb.append("/conf"); // for testing
		sb.append("/");
		sb.append(c.getCustomer()).append("/").append(c.getRelease())
				.append("/log");
		File logPath = new File(sb.toString());
		if (!logPath.exists()) {
			logPath.mkdirs();
		}

		File localLogFile = new File(logPath.toString() + "/" + c.getTID()
				+ ".log");

		try {
			boolean isDirChanged = ftp.changeWorkingDirectory(remoteDir);
			if (isDirChanged) {
				ftp.setControlEncoding("UTF8");
				String remoteFileName = c.getTID() + ".log";
				OutputStream out = new FileOutputStream(localLogFile);
				ftp.setBufferSize(1024);
				ftp.retrieveFile(remoteFileName, out);			
				
				out.flush();
				out.close();
				//ftp.deleteFile(remoteFileName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void uploadFile(File file) {
		if (!file.exists()) {
			return;
		}

		String fileName = file.getName();
		try {
			boolean isDirChanged = ftp.changeWorkingDirectory(remoteDir);

			if (isDirChanged) {
				FileInputStream in = new FileInputStream(file);
				ftp.setBufferSize(1024);
				ftp.storeFile(fileName, in);
				in.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean isConnected() {
		return ftp.isConnected();
	}

	public void disconnect() {
		try {
			ftp.logout();
			ftp.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Case c = new Case("dy6253", "74077", "R29SUC", "VFDE", "Function",
				"VFDE");
		File f = new File("D:/workspace/XAuto/conf/caselist.txt");
		long start = System.currentTimeMillis();
		FTPManager ftpMgr = new FTPManager();
		ftpMgr.connect();
		ftpMgr.downloadFile();
		// ftpMgr.downloadCaseLog(c);
		/*
		if (f.exists()) {
			System.out.println("filename: " + f.getName());
			ftpMgr.uploadFile(f);
		}
		*/
		ftpMgr.disconnect();
		
		long end = System.currentTimeMillis();
		
		System.out.println("duration is: " + (end - start) / 1000);

	}

}
