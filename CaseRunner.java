import java.awt.Rectangle;

import javax.swing.JTable;

/**
 * CaseRunner is to get cases from GUI, and run
 * It is run in another thread started by GUI/controller(currently by GUI)
 */

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
		int pass = countPassCase();
		int fail = totalRunCase - pass;
		
		StringBuilder report = new StringBuilder();
		report.append("+++++++++++++++++ All cases finished ++++++++++++++++++\n\n" +
				"/****************************************/\n");
		report.append(String.format(" * Cases run: %20d %s", totalRunCase,"\n"));
		report.append(String.format(" * Passed cases: %14d %s", pass,"\n"));
		report.append(String.format(" * Failed cases: %16d %s", fail,"\n"));
		report.append(String.format(" * Used time: %20d %s", dura,"s\n"));
		report.append("/****************************************/\n");
		
		controller.printLog(report.toString());
		controller.showMessageDialog(report.toString());
	}
	
	public int countPassCase() {
		int count = 0;
		CaseTableModel caseModel = (CaseTableModel) controller.gui
				.getCaseTable().getModel();
		int len = caseModel.getRowCount();
		
		for (int i = 0; i < len; i++) {
			boolean selected = ((Boolean) caseModel.getValueAt(i, 0))
					.booleanValue();
			if (selected) {
				String result = (String) caseModel.getValueAt(i, 10);
				if (result.equals("PASS")) {
					count++;
				}
			}
		}
		return count;
	}

	public void run() {
		setStartTime(System.currentTimeMillis());
		
		Thread lastProcesser = null;
		RTDBManager rtdbManager = controller.getRTDBManager();
		SPAManager spaManager = controller.getSPAManager();
		CaseTableModel caseModel = (CaseTableModel) controller.gui
				.getCaseTable().getModel();

		String preRelease = null;
		String preCustomer = null;

		boolean firstCase = true;
		int len = caseModel.getRowCount();
		if (len == 0) {
			controller.printLog("WARNING: No case loaded, no case run\n");
		}
		
		controller.printLog("+++++++++++++++++ Start to run case +++++++++++++++++\n");
		controller.printLog("Collecting cases from GUI...\n");
		for (int row = 0; row < len; row++) {
			if (!controller.isRunning()) {
				controller.printLog("!!! Stop running cases !!!\n");
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
					firstCase = false;
				}
				if (!firstCase || controller.getLoadDataFlag()) {						
					spaManager.stopAllSpa();
					spaManager.loadAllSpaSql(sqlDir);
					spaManager.startSpaInPool();
				}				
		
				rtdbManager.loadAllDB(dbDataDir);

				preRelease = rel;
				preCustomer = customer;
			}

			controller.printLog("+++++++ Start to run case " + tid + "+++++++\n");
				
			setRunningRow(row);
				
			if (caseType.equals("Audit")) {
				controller.prepareAuditCase(caseToRun);
			}
			//controller.runCase(caseToRun);
			controller.newRunCase(caseToRun);
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