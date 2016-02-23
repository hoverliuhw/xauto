import java.io.IOException;
import java.net.SocketException;

/**
 * LogProcesser is run in another thread started by CaseRunner
 * 
 * It contains three parts:
 * 1) Combine log which just run by CaseRunner
 * 		this part has not been carried out yet.
 * 2) Download case log by FTPManager
 * 3) Parse log, determine the case should be PASS or FAIL, 
 * 		and update UI accordingly
 */
public class LogProcesser implements Runnable {
	private XController controller;
	private Host host;
	private Case caseToProcess;
	private int row;
	
	public LogProcesser() {
		controller = null;
		caseToProcess = null;
		row = 0;
	}
	
	public LogProcesser(XController c, Case caseToProcess, int row) {
		controller = c;
		this.caseToProcess = caseToProcess;
		this.row = row;
	}
	
	public void combineLog() throws SocketException, IOException {

		Host host = controller.getHost();
		String hostname = host.getHostName();
		String ip = host.getIP();
		int port = host.getPort();
		String username = host.getUserName();
		String passwd = host.getPasswd();

		Host shell = new Host(hostname, ip, port, username, passwd);
		shell.login();
		shell.sendCmd("LogCMB /tmp/" + caseToProcess.getTID() + ".log");
		shell.disconnect();
	}
	
	public void getCaseLog() throws IOException {
		FTPManager ftpManager = controller.getFTPManager();
		
		ftpManager.connect();
		ftpManager.downloadCaseLog(caseToProcess);
		ftpManager.disconnect();
	}

	public void run() {
		// TODO Auto-generated method stub
		if (controller == null) {
			System.out.println("controller is NULL");
			return;
		}
		LogParser logParser = controller.getLogParser();
		if (logParser == null) {
			controller.printLog("ERROR: LogParser is not set\n");
			return;
		}
		
		CaseTableModel caseModel = (CaseTableModel) controller.gui.getCaseTable().getModel();
		try {
			combineLog();
			getCaseLog();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			caseModel.setValueAt(ParseResult.FAIL_GETLOG, row, CaseTableModel.COLUMN_RESULT);
			controller.printLog("Failed to reach host to combine and download " +
					caseToProcess.getTID() + ".log\n" +
					"Error message: " + e.getMessage());
			return;
		}
		
		boolean isPassed = false;
		ParseResult result;
		try {
			isPassed = logParser.parseCase(caseToProcess);
			result = isPassed ? ParseResult.PASS : ParseResult.FAIL;
		} catch (CantParseException e) {
			controller.printLog("Failed to parse "+ caseToProcess.getTID() + ".log\n");
			result = ParseResult.FAIL_PARSE;
		} 		
		 
		caseModel.setValueAt(result, row, CaseTableModel.COLUMN_RESULT);
	}

}
