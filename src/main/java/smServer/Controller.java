package smServer;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.backend.db.ConMan;
import smServer.backend.db.DbPropsReader;
import smServer.backend.file.FilePropsReader;
import smServer.impl.InnoApi;

public class Controller implements Runnable {

	private BigDecimal senderNo;
	private Logger log;
	private ShortMessageSender sms;
	private Hashtable<String, String> appProps;
	private String baseDir;
	private Thread sendThreads[];

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) {
		Controller controller = new Controller();
		controller.run();
	}

	public Controller() {
		log = Logger.getLogger(Controller.class.getName());
		baseDir = System.getProperty("user.home") + File.separatorChar + "smServer";
	}

	@Override
	public void run() {

		getAppProps();
		senderNo = new BigDecimal(appProps.get("senderNo"));

		sms = new InnoApi(appProps.get("username"), appProps.get("password"));

		// start sending threads
		Runnable sendingRunnable = (Runnable) sms;

		int noSendingThreads = Integer.valueOf(appProps.get("noSendingThreads"));
		sendThreads = new Thread[noSendingThreads];
		for (int i = 0; i < sendThreads.length; i++) {
			sendThreads[i] = new Thread(sendingRunnable);
			sendThreads[i].start();
		}

		// start new message watcher
		NewMessageWatcher nmw = getInstance(appProps.get("newMessageWatcherClass"));
		nmw.run();

		// start periodic message watcher
		PeriodicMessageWatcher pmw = getInstance(appProps.get("periodicMessageWatcherClass"));
		pmw.run();

		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {}

		// stop all timers
		pmw.stop();

		// stop all senders
		stopAllSenders();
	}

	private <T> T getInstance(String className) {
		try {
			Class<T> clazz = (Class<T>) Class.forName(className);
			Constructor<T> c = clazz.getConstructor(Controller.class);
			return c.newInstance(this);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.log(Level.SEVERE, "class not found!", e);
		}
		return null;
	}

	private void getAppProps() {
		String dbUrl = System.getenv(ConMan.ENV_DB_URL);

		PropsReader pr = null;
		if(dbUrl == null) {
			pr = new FilePropsReader(baseDir);
		} else {
			pr = new DbPropsReader();
		}

		appProps = pr.get();
	}

	private void stopAllSenders() {
		for(Thread t : sendThreads) {
			t.interrupt();
		}
	}

	public void sendMessage(Properties msg) {
		String rnSplit[] = msg.getProperty("receiverNo").split(",");
		String textMessage = msg.getProperty("text");
		String sendDate = msg.getProperty("termin");

		for(String rn: rnSplit) {

			if(rn != null && textMessage != null) {
				BigDecimal receiverNo = new BigDecimal(rn);
				ShortMessage message = null;
				try {
					message = new ShortMessage(senderNo,receiverNo,textMessage);
				} catch(Exception e) {
					log.log(Level.SEVERE,"Couldn't create SMS object!", e);
					return;
				}
				message.setSendDate(sendDate);
				sms.send(message);
			}
		}
	}

	public String getBaseDir() {
		return baseDir;
	}
}
