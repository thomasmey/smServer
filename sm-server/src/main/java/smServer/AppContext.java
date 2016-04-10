package smServer;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Logger;

public class AppContext extends HashMap<String, Object> {

	private static final long serialVersionUID = 1L;

	public static final String BASE_DIR = "baseDir";
	public static final String SEND_THREADS = "sendThreads";
	public static final String NMW_THREAD = "nmwThread";
	public static final String PMW_THREAD = "pmwThread";
	public static final String SENDER_NO = "senderNo";
	public static final String SMS = "sms";

	public static final String PMW = "periodicMessageWatcher";
	public static final String NMW = "newMessageWatcher";

	private Logger log;

	public AppContext() {
		log = Logger.getLogger(AppContext.class.getName());
	}

	public void transfer(Hashtable<String, String> props, String key) {
		this.put(key, props.get(key));
	}
}
