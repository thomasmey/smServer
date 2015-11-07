package smServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Controller implements Runnable {

	private BigDecimal senderNo;
	private Logger log;
	private ShortMessageSender sms;
	private Properties appProps;
	private String baseDir;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		Controller controller = new Controller();
		controller.run();
	}

	public Controller() throws IOException {

		log = Logger.getLogger(Controller.class.getName());

		baseDir = System.getProperty("user.home") + File.separatorChar + "smServer";
		appProps = new Properties();
		appProps.load(new FileReader(new File(baseDir, "AppSender.properties")));
		senderNo = new BigDecimal(appProps.getProperty("senderNo"));
	}

	@Override
	public void run() {
		String username = appProps.getProperty("username");
		String password = appProps.getProperty("password");

		sms = new InnoApi(log, username, password);

		// start sending threads
		Runnable sendingRunnable = (Runnable) sms;

		int noSendingThreads = Integer.valueOf(appProps.getProperty("noSendingThreads"));
		Thread sendThread [] = new Thread[noSendingThreads];
		for (int i = 0; i < sendThread.length; i++) {
			sendThread[i] = new Thread(sendingRunnable);
			sendThread[i].start();
		}

		// process periodic events
		setupPeriodicEvents();

		// start directory watcher
		WatchDirServer server = new WatchDirServer(this, baseDir);
		server.run();

		// server ended, stop sending threads
		for(Thread t : sendThread) {
			t.interrupt();
		}
	}

	private void setupPeriodicEvents() {
	}

	void processFile(File sm) throws FileNotFoundException, IOException {

		assert sm != null;

		log.log(Level.FINE,"Sending file {0}", sm.getName());

		if(!sm.exists())
			return;

		Properties msgProps = new Properties();
		Reader fr = new FileReader(sm);
		msgProps.load(fr);
		String rnSplit[] = msgProps.getProperty("receiverNo").split(",");
		String textMessage = msgProps.getProperty("text");
		String sendDate = msgProps.getProperty("termin");
		fr.close();

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
				if(sm.delete() == false) {
					log.log(Level.SEVERE, "Cannot delete file {0}. Stopping server.", sm.getName());
					throw new IOException();
				}
			}
		}
	}
}
