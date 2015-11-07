package smServer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.backend.file.WatchDirPeriodicServer;
import smServer.backend.file.WatchDirServer;
import smServer.impl.InnoApi;

public class Controller implements Runnable {

	private BigDecimal senderNo;
	private Logger log;
	private ShortMessageSender sms;
	private Properties appProps;
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
		appProps = new Properties();
	}

	@Override
	public void run() {

		getAppProps();
		senderNo = new BigDecimal(appProps.getProperty("senderNo"));

		sms = new InnoApi(appProps.getProperty("username"), appProps.getProperty("password"));

		// start sending threads
		Runnable sendingRunnable = (Runnable) sms;

		int noSendingThreads = Integer.valueOf(appProps.getProperty("noSendingThreads"));
		sendThreads = new Thread[noSendingThreads];
		for (int i = 0; i < sendThreads.length; i++) {
			sendThreads[i] = new Thread(sendingRunnable);
			sendThreads[i].start();
		}

		// start new message watcher
		NewMessageWatcher nmw = new WatchDirServer(this);
		nmw.run();

		// start periodic message watcher
		PeriodicMessageWatcher pmw = new WatchDirPeriodicServer(this);
		pmw.run();

		// stop all timers
		pmw.stop();

		// stop all senders
		stopAllSenders();
	}

	private void getAppProps() {
		try {
			appProps.load(new FileReader(new File(baseDir, "AppSender.properties")));
		} catch (IOException e) {
			log.log(Level.SEVERE,"Failed to load app props!", e);
		}
	}

	private void stopAllSenders() {
		for(Thread t : sendThreads) {
			t.interrupt();
		}
	}

	public void sendMessage(Properties msgProps) {
		String rnSplit[] = msgProps.getProperty("receiverNo").split(",");
		String textMessage = msgProps.getProperty("text");
		String sendDate = msgProps.getProperty("termin");

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
