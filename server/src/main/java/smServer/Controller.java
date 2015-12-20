package smServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import smServer.backend.db.ConMan;
import smServer.backend.db.DbPropsReader;
import smServer.backend.file.FilePropsReader;
import smServer.impl.InnoApi;

public class Controller implements BundleActivator {

	private Logger log;

	private Hashtable<String, String> appProps;
	private BigDecimal senderNo;
	private String baseDir;

	private ShortMessageSender sms;
	
	private Thread sendThreads[];
	private NewMessageWatcher nmw;
	private PeriodicMessageWatcher pmw;

	private Charset charAscii = Charset.forName("ASCII");
	final byte[] command = "?command=".getBytes(charAscii);
	final byte[] cmdRefreshPeriodic = "rfsh".getBytes(charAscii);
	final byte[] cmdSendMessages = "send".getBytes(charAscii);
	final byte[] cmdStopServer = "stop".getBytes(charAscii);

	public Controller() {
		log = Logger.getLogger(Controller.class.getName());
		baseDir = System.getProperty("user.home") + File.separatorChar + "smServer";
		log.log(Level.INFO, "Bundel starting!");
	}

	@Override
	public void start(BundleContext context) throws Exception {

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

		nmw = getInstance(appProps.get("newMessageWatcherClass"));
		nmw.run();

		pmw = getInstance(appProps.get("periodicMessageWatcherClass"));
		pmw.run();

		// start command listener

		try {
			commandServerLoop();
		} catch (InterruptedException e) {
		} finally {

			// stop all timers
			pmw.stop();

			// stop all senders
			stopAllSenders();
		}
	}

	private void commandServerLoop() throws InterruptedException {

		byte[] buffer = new byte[1024];
		byte[] httpRespOk = "HTTP/1.0 200 OK\r\n\r\n".getBytes(charAscii);

		int port = Integer.valueOf(System.getenv("PORT"));
		ServerSocket ss;
		try {
			ss = ServerSocketFactory.getDefault().createServerSocket(port);
		} catch (IOException e) {
			log.log(Level.SEVERE,"failed to reserve port!", e);
			return;
		}

		int errorCount = 0;
		while(true) {

			try {
				Socket s = ss.accept();
				InputStream in = s.getInputStream();
				int n = in.read(buffer);

				String rc = processRequest(buffer, n);
				if(rc != null) {
					switch (rc) {
					case "refreshPeriodic":
						pmw.refresh();
						break;
					case "sendMessages":
						nmw.refresh();
						break;
					case "stopServer":
						throw new InterruptedException();
					}
				}

				OutputStream out = s.getOutputStream();
				out.write(httpRespOk);
				s.close();
			} catch(RuntimeException | IOException e) {
				log.log(Level.SEVERE, "command server failed!", e);
				errorCount++;
				if(errorCount > 10)
					throw new InterruptedException();
			}
		}
	}

	private String processRequest(byte[] buffer, final int n) {

		try {
			int i = 0, ic = 0;

			final int cmdLen = 4;

			boolean found = false;
			for(; i < n; i++) {
				if(buffer[i] == command[ic]) {
					ic++;
					if(ic == command.length) {
						found = true;
						break;
					}
				} else {
					ic = 0;
				}
			}
			if(found) {
				i++; // '='
				byte[] cmd = Arrays.copyOfRange(buffer, i, i+cmdLen);
				if(Arrays.equals(cmd, cmdRefreshPeriodic)) {
					return "refreshPeriodic";
				} else if(Arrays.equals(cmd, cmdSendMessages)) {
					return "sendMessages";
				} else if(Arrays.equals(cmd, cmdStopServer)) {
					return "stopServer";
				}
			}

		} catch(RuntimeException e) {
			log.log(Level.INFO, "failed to parse command request", e);
		}
		return null;
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

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}
}
