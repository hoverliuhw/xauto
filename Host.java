/**
 **************************************************************************
 * Name: Host
 * 
 * Description: Derived from BaseHost, provide functions of an mCAS machine 
 * Author: Liu Hongwei
 * 		   hong_wei.hl.liu@alcatel-lucent.com
 * 
 *************************************************************************
 */
 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;

public class Host extends BaseHost {
	public static final String CEPEXEC = "/cs/sn/cr/cepexec";
	private String currentUser;

	public Host(String name, String ip, int port, String username, String passwd) {
		super(name, ip, port, username, passwd);
		setPrompt("-> ");
		currentUser = username;
	}

	public void subshl() {
		setPrompt("< ");
		sendCmd("subshl");
		readUntil(getPrompt());
	}

	public void quitSubshl() {
		setPrompt("-> ");
		sendCmd("quit");
		readUntil(getPrompt());
	}

	public boolean loadFrm(String frmFile) {
		File file = new File(frmFile);
		if (!file.exists()) {			
			System.out.println("frm file " + frmFile + " does not exist!");				
			return true;
		}
		
		System.out.println("load frm " + frmFile);
		
		StringBuilder sbFrm = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				line = line.replaceAll("! ", ",");
				if (line.length() >= 4096) {
					br.close();
					return false;
				}
				sbFrm.append(line + "\n");
			}

			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String[] frmLn = sbFrm.toString().split("\n");
		String oldPrompt = getPrompt();
		setPrompt("> ");
		sendCmd("/cs/sn/cr/cepexec RCV_TEXT \"RCV:TEXT,SPA\"");
		
		for (String str : frmLn) {
			sendCmd(str);
		}
		setPrompt(oldPrompt);
		System.out.println(sendCmd("END;"));
		return true;
	}

	public void loadFrmbk(String frmFile) {
		File file = new File(frmFile);
		if (!file.exists()) {
			System.out.println("frm file " + frmFile + " does not exist!");				
			return;
		}
		
		System.out.println("load frm " + frmFile);
		String oldPrompt = getPrompt();
		setPrompt("> ");
		sendCmd("/cs/sn/cr/cepexec RCV_TEXT \"RCV:TEXT,SPA\"");

		StringBuilder sbFrm = new StringBuilder("\n");

		try {
			BufferedReader br = new BufferedReader(new FileReader(frmFile));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				sbFrm.insert(0, line.replaceAll("! ", ",") + "\n");
			}

			br.close();
		} catch (Exception e) {
			setPrompt(oldPrompt);
			e.printStackTrace();
		}

		String[] frmLn = sbFrm.toString().split("\n");
		for (String str : frmLn) {
			sendCmd(str);
		}

		setPrompt(oldPrompt);
		System.out.println(sendCmd("END;"));
	}

	public String cepexec(String lmtCmd) {
		String cepCmd = CEPEXEC + " SEND_TEXT " + "'" + lmtCmd + "'";
		return sendCmd(cepCmd);
	}

	public void changeDate(String date) {
		String previousUser = whoami();
		if (!previousUser.equals("root")) {
			su();
		}

		String[] bladeList = getBladeInfo();

		for (int i = 0; i < bladeList.length; i++) {
			sendCmd("ssh " + bladeList[i] + " date " + date);
		}
		sendCmd("rm /sn/log/debuglog*");

		if (!previousUser.equals("root")) {
			quitSu();
		}
		
	}

	public String[] getBladeInfo() {
		sendCmd("stty -echo");
		String bladeInfo = sendCmd("echo `ls /opt/config/servers`eof");
		sendCmd("stty echo");

		int end = bladeInfo.indexOf("eof");
		String bladeList = bladeInfo.substring(0, end);
		return bladeList.split(" ");
	}

	public void su() {
		String user = whoami();
		if (user.equals("root")) {
			return;
		}

		String suPasswd = "r00t";
		setPrompt("-# ");
		write("su -");
		readUntil("Password: ");
		write(suPasswd);
		readUntil(getPrompt());
		currentUser = "root";
	}

	public void quitSu() {
		String user = whoami();
		if (!user.equals("root")) {
			return;
		}

		setPrompt("-> ");
		sendCmd("exit");
		currentUser = getUserName();
	}

	public String whoami() {
		//sendCmd("stty -echo");
		/*
		 * We dont use `whoami` directly here, if use it, end =
		 * result.indexof("\n") - 1; or use sendCmd("echo `whoami`eof");
		 
		String result = sendCmd("echo \"$LOGNAME\"eof");
		sendCmd("stty echo");
		int end = result.indexOf("eof");
		return result.substring(0, end);
		*/
		return currentUser;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String hostname = "SPVM53";
		String ip = "135.242.106.116";
		// String hostname = "SPVM138A";
		// String ip = "135.242.17.41";
		int port = 23;
		String username = "ainet";
		String passwd = "ainet1";

		Host host = new Host(hostname, ip, port, username, passwd);
		try {
			host.login();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		host.loadFrm("/home/ainet/hongwehl/automation/R29SUC/BSNL/R29SUB/res/ek1091.frm");
		host.loadFrmbk("/home/ainet/hongwehl/automation/R29SUC/BSNL/R29SUB/res/ek1091.frmbk");
		/*
		host.su();
		String currentUser = host.whoami();

		System.out.println("current user: " + currentUser
				+ currentUser.equals("root"));

		host.quitSu();
		String newuser = host.whoami();

		System.out.println("current user: " + newuser
				+ newuser.equals(username));

		String newDate = "042714452015";
		host.changeDate(newDate);
		System.out.println("who am i? " + host.whoami());
		*/
		host.disconnect();
	}

}
