/**
 **************************************************************************
 * Name: CaseRunner
 * 
 * Description: CaseRunner gets cases from GUI, and run in another thread 
 * Author: Liu Hongwei
 * 		   hong_wei.hl.liu@alcatel-lucent.com
 * 
 *************************************************************************
 */
import java.awt.Rectangle;
import javax.swing.JTable;

public class CaseRunner implements Runnable {

	private XController controller;
	private long start;
	private long end;
	private int totalRunCase;

	public CaseRunner() {
		controller = null;
		start = 0;
		end = 0;
		totalRunCase = 0;
	}

	public CaseRunner(XController c) {
		controller = c;
		start = 0;
		end = 0;
		totalRunCase = 0;
	}

	public void setController(XController c) {
		controller = c;
	}
	
	public void setStartTime(long time) {
		start = time;
	}
	
	public void setEndTime(long time) {
		end = time;
	}
	
	public void resetRunTime() {
		start = 0;
		end = 0;
		totalRunCase = 0;
	}
	
	public void setRunningRow(int row) {
		JTable caseTable = controller.gui.getCaseTable();
		CaseTableModel caseModel = (CaseTableModel) caseTable.getModel();
		CaseTableCellRenderer tcr = (CaseTableCellRenderer) caseTable.getDefaultRenderer(Object.class);
		tcr.setRunningRow(row);		
		/*there maybe some problem here, so comment
		 * the reason may be table is being painted, and the area
		 * is null, add try/catch is a better idea
		 
		if (row > 0) {
			Rectangle currentRowRect = caseTable.getCellRect(row, 1, true);			
			if (currentRowRect != null) {
				caseTable.scrollRectToVisible(currentRowRect);
			}			
		}
		*/
		caseModel.fireTableDataChanged();
	}
	
	public void resetRunningRow() {
		setRunningRow(-1);
	}
	
	public void printReport() {
		long dura = (end - start) / 1000;
		JTable caseTable = controller.getGui().getCaseTable();
		int rowCount = caseTable.getRowCount();
		int passCount = 0;
		int failCount = 0;
		int failParseCount = 0;
		int otherCount = 0;
		ParseResult result = null;
		
		for (int row = 0; row < rowCount; row++) {
			boolean selected = ((Boolean) caseTable.getValueAt(row, CaseTableModel.COLUMN_SELECTED)).booleanValue();
			if (!selected) {
				continue;
			}
			
			result = (ParseResult) caseTable.getValueAt(row, CaseTableModel.COLUMN_RESULT);
			switch (result) {
			case PASS:
				passCount++;
				break;
			case FAIL:
				failCount++;
				break;
			case FAIL_PARSE:
				failParseCount++;
				break;
			default:
				otherCount++;
			}
		}
		
		float passRate = 0;
		if (totalRunCase > 0) {
			passRate = (float) (Math.round(((float) passCount) / ((float) totalRunCase) * 10000)) / 10000;
		}		
		StringBuilder report = new StringBuilder();
		report.append("+++++++++++++++++ All cases finished ++++++++++++++++++\n\n" +
				"/****************************************/\n");
		report.append(String.format(" * Used time: %20d %s", dura,"s\n"));
		report.append(String.format(" * Cases run: %20d %s", totalRunCase,"\n"));
		report.append(String.format(" * Passed cases: %14d %s", passCount,"\n"));
		report.append(String.format(" * Failed cases: %16d %s", failCount,"\n"));
		report.append(String.format(" * Fail to Parse cases: %7d %s", failParseCount,"\n"));
		report.append(String.format(" * Other result cases: %8d %s", otherCount,"\n"));
		report.append(String.format(" * Pass Rate: %20.2f%s", passRate * 100,"%\n"));
		report.append("/****************************************/\n");
		
		controller.printLog(report.toString());
		controller.showMessageDialog(report.toString());
	}
	
	public void run() {
		setStartTime(System.currentTimeMillis());
		
		Thread lastProcesser = null;
		RTDBManager rtdbManager = controller.getRTDBManager();
		SPAManager spaManager = controller.getSPAManager();
		PgClient pgClient = controller.getPgClient();
		CaseTableModel caseModel = (CaseTableModel) controller.gui
				.getCaseTable().getModel();

		String preRelease = null;
		String preCustomer = null;
		String preCaseType = null;

		boolean firstCase = true;
		int len = caseModel.getRowCount();
		if (len == 0) {
			controller.printLog("No case loaded, no case run\n");
			return;
		}
		
		controller.printLog("+++++++++++++++++ Start to run case +++++++++++++++++\n");
		controller.printLog("Collecting cases from GUI...\n");
		for (int row = 0; row < len; row++) {
			if (controller.isStopClicked()) {
				controller.setRunningFlag(false);
				controller.setStopClicked(false);
				break;
			}

			boolean selected = ((Boolean) caseModel.getValueAt(row, CaseTableModel.COLUMN_SELECTED))
					.booleanValue();
			
			if (!selected) {
				continue;
			}
			
			String tid = (String) caseModel.getValueAt(row, CaseTableModel.COLUMN_TID);
			String fid = (String) caseModel.getValueAt(row, CaseTableModel.COLUMN_FEATURE_ID);
			String rel = (String) caseModel.getValueAt(row, CaseTableModel.COLUMN_RELEASE);
			String customer = (String) caseModel.getValueAt(row, CaseTableModel.COLUMN_CUSTOMER);
			String caseType = (String) caseModel.getValueAt(row, CaseTableModel.COLUMN_CASE_TYPE);
			String basedata = (String) caseModel.getValueAt(row, CaseTableModel.COLUMN_BASE_DATA);
			Case caseToRun = new Case(tid, fid, rel, customer, caseType,
					basedata);

			if (!(rel.equals(preRelease) && customer.equals(preCustomer))) {
				String baseDir = controller.getBaseDir();
				String dbDataDir = baseDir + "/" + customer + "/" + rel + "/rtdb_data/";
				String sqlDir = baseDir + "/" + customer + "/" + rel + "/spa_data/";
				if (firstCase) {
					rtdbManager.createInitDb();
					Host host = controller.getHost();
					host.changeDate(XController.DEFAULT_DATE);
					firstCase = false;
				}
				long startSpaTime = 0;
				if (!firstCase || controller.getLoadDataFlag()) {
					spaManager.stopAllSpa();
					if (controller.useDynamicFrmbkFlag()) {
						pgClient.closeRCtracker();
						spaManager.loadAllSpaSql(sqlDir);
						pgClient.openRCtracker();
					} else {
						spaManager.loadAllSpaSql(sqlDir);
					}
					startSpaTime = System.currentTimeMillis();
					spaManager.startSpaInPool();					
				}				
		
				rtdbManager.loadAllDB(dbDataDir);
				long endSpaTime = System.currentTimeMillis();
				long dura = (endSpaTime - startSpaTime) / 1000;
				if (dura < 360) {
					try {
						Thread.sleep((360 - dura) * 1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				preRelease = rel;
				preCustomer = customer;
			} else {
				if (preCaseType != null && preCaseType.equals("Audit") && caseType.equals("Function") ) {
					rtdbManager.loadAllDB(controller.getBaseDir() + "/" + customer + "/" + rel + "/rtdb_data/");
				}
			}

			controller.printLog("+++++++ Start to run case " + tid + "+++++++\n");
				
			setRunningRow(row);
				
			if (caseType.equals("Audit")) {
				controller.prepareAuditCase(caseToRun);
			}
			
			//controller.runCase(caseToRun);
			controller.newRunCase(caseToRun);
			preCaseType = caseType;
			totalRunCase++;
			
			LogProcesser processer = new LogProcesser(controller, caseToRun, row);
			Thread t = new Thread(processer);
			t.start();
			lastProcesser = t;
			controller.printLog("++++++++ Finish running case " + tid + " +++++++++\n\n");
			
			int sleep = 1;
			try {
				Thread.sleep(sleep * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (controller.useDynamicFrmbkFlag()) {
			pgClient.closeRCtracker();
		}
		
		setEndTime(System.currentTimeMillis());
		
		try {
			if (lastProcesser != null) {
				lastProcesser.join();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		MgtsHost mgts = controller.getMgts();
		if (mgts != null) {
			mgts.disconnectPassThru();
			mgts.stopMgts();
		}		
		
		printReport();
		controller.setRunningFlag(false);
		resetRunningRow();
	}
}
