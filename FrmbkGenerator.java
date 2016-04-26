/**
 **************************************************************************
 * Name: FrmbkGenerator
 * 
 * Description: Generate frmbk file, file is stored under baseDir/frmfile 
 * Author: Liu Hongwei
 * 		   hong_wei.hl.liu@alcatel-lucent.com
 * 
 *************************************************************************
 */
public class FrmbkGenerator implements Runnable {
	private XController controller;
	private String prefix;
	
	public FrmbkGenerator(XController controller, String prefix) {
		this.controller = controller;
		this.prefix = prefix;
	}
	
	public void run() {
		PgClient pgClient = controller.getPgClient();
		if (pgClient == null) {
			return;
		}
		
		pgClient.genFrmbk(prefix);
	}
	
}
