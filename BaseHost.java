import java.io.*;
import java.net.SocketException;

import org.apache.commons.net.telnet.*;

public class BaseHost {

	/**
	 * Members: HostName, IP address, Port, UserName, Passwd, prompt
	 */
	private String name;
	private String ip;
	private int port = 23;
	private String username;
	private String passwd;
	private String prompt;

	/*
	 * Members for telnet to this host
	 */
	private TelnetClient telnet;
	private InputStream input;
	private PrintStream output;

	/**
	 * Construction function, just set Host name of this host
	 */
	public BaseHost(String name) {
		this.name = name;
		telnet = new TelnetClient();
	}

	public BaseHost() {
		name = null;
		telnet = new TelnetClient();
	}

	public BaseHost(String name, String ip, int port, String username,
			String passwd) {
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.username = username;
		this.passwd = passwd;
		//prompt = new String(">");
		prompt = ">";
		telnet = new TelnetClient();
	}

	/*
	 * Initialize this host, call it after new this host
	 */
	public void initialize(String ip, int port, String username, String passwd) {
		this.ip = ip;
		this.port = port;
		this.username = username;
		this.passwd = passwd;
		//prompt = new String(">");
		prompt = ">";
		telnet = new TelnetClient();
	}

	public void setHostName(String name) {
		if (name != null) {
			this.name = name;
		}
	}

	public String getHostName() {
		return name;
	}

	public void setIP(String ip) {
		if (ip != null) {
			this.ip = ip;
		}
	}

	public String getIP() {
		return ip;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setUserName(String usr) {
		if (usr != null) {
			username = usr;
		}
	}

	public String getUserName() {
		return username;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public String getPasswd() {
		return passwd;
	}
	
	public int getPort() {
		return port;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public String getPrompt() {
		return prompt;
	}

	public void login() throws SocketException, IOException {
		telnet.connect(ip, port);
		input = telnet.getInputStream();
		output = new PrintStream(telnet.getOutputStream());
		readUntil("login: ");
		write(username);
		readUntil("Password: ");
		write(passwd);
		readUntil(prompt);
	}

	public String readUntil(String pattern) {
		try {
			char lastchar = pattern.charAt(pattern.length() - 1);
			StringBuffer sb = new StringBuffer();
			char ch = (char) input.read();

			while (true) {
				sb.append(ch);
				if ((ch == lastchar) && (sb.toString().endsWith(pattern))) {
					return sb.toString();
				}
				ch = (char) input.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void write(String pattern) {
			output.println(pattern);
			output.flush();
	}

	public String sendCmd(String cmd) {
		try {
			write(cmd);
			return readUntil(prompt);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public boolean isConnected() {
		return telnet.isConnected();
	}

	public void disconnect() {
		try {
			telnet.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
