/**
 **************************************************************************
 * Name: StateMachineRunner
 * 
 * Description: Running state machine in an independent thread
 *				finish within a max timer
 * Author: Liu Hongwei
 * 		   hong_wei.hl.liu@alcatel-lucent.com
 * 
 *************************************************************************
 */
 
import java.io.IOException;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;


public class StateMachineRunner implements Runnable {
	public static final int MAX_STATE_MACHINE_DURATION = 375;
	private XController controller;
	private String stateName;
	
	public StateMachineRunner(XController controller, String stateName) {
		this.controller = controller;
		this.stateName = stateName;
	}
	
	public void run() {
		MgtsHost mgts = controller.getMgts();
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				MgtsHost monitor = new MgtsHost(controller.getMgts());
				try {
					monitor.login();
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				monitor.goToDataDir();
				System.out.println("Timeout, stop state machine " + monitor.stopStateMachine(stateName));
				monitor.disconnect();
			}
		}, MAX_STATE_MACHINE_DURATION * 1000);
		
		System.out.println(mgts.runStateMachine(stateName));
		timer.cancel();
		try {
			Thread.sleep(1000 * XController.DEFAULT_INTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
