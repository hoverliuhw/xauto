public class Case {

	/**
	 * @param args
	 */
	// tid fid release customer casetype basedata neededtime runresult usedtime/
	private String tid;
	private String fid;
	private String release;
	private String customer;
	private String casetype;
	private String basedata;
	private int needtime;
	private ParseResult runresult;
	private int usedtime;

	public Case(String tid, String fid, String release, String customer,
			String casetype, String basedata, int needtime) {
		this.tid = tid;
		this.fid = fid;
		this.release = release;
		this.customer = customer;
		this.casetype = casetype;
		this.basedata = basedata;
		this.needtime = needtime;
		runresult = ParseResult.NA;
		usedtime = 0;
	}

	public Case(String tid, String fid, String release, String customer,
			String casetype, String basedata) {
		this.tid = tid;
		this.fid = fid;
		this.release = release;
		this.customer = customer;
		this.casetype = casetype;
		this.basedata = basedata;
		needtime = 60;
		runresult = ParseResult.NA;
		usedtime = 0;
	}

	public void setTID(String tid) {
		this.tid = tid;
	}

	public String getTID() {
		return tid;
	}

	public void setFid(String fid) {
		this.fid = fid;
	}

	public String getFid() {
		return fid;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public String getRelease() {
		return release;
	}

	public void setCustomer(String customer) {
		this.customer = customer;
	}

	public String getCustomer() {
		return customer;
	}

	public void setCaseType(String casetype) {
		this.casetype = casetype;
	}

	public String getCaseType() {
		return casetype;
	}

	public void setBaseData(String basedata) {
		this.basedata = basedata;
	}

	public String getBaseData() {
		return basedata;
	}

	public void setNeededTime(int time) {
		this.needtime = time;
	}

	public int getNeededTime() {
		return needtime;
	}

	public void setRunResult(ParseResult result) {
		this.runresult = result;
	}

	public ParseResult getRunResult() {
		return runresult;
	}

	public void setUsedTime(int time) {
		this.usedtime = time;
	}

	public int getUsedTime() {
		return this.usedtime;
	}

	public void showCaseInfo() {
		System.out.println("tid -> " + this.getTID());
		System.out.println("fid -> " + this.getFid());
		System.out.println("release -> " + this.getRelease());
		System.out.println("customer -> " + this.getCustomer());
		System.out.println("casetype -> " + this.getCaseType());
		System.out.println("basedata -> " + this.getBaseData());
		System.out.println("needtime -> " + this.getNeededTime());
		System.out.println("runresult -> " + this.getRunResult());
		System.out.println("usedtime -> " + this.getUsedTime());
	}
}
