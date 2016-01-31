package smServer.listener;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import smServer.AbstractNewMessageWatcher;
import smServer.AppContext;
import smServer.AbstractPeriodicMessageWatcher;
import smServer.PropsReader;
import smServer.api.ShortMessageSender;
import smServer.backend.db.DbPropsReader;
import smServer.backend.file.FilePropsReader;
import smServer.impl.InnoApi;

@WebListener("app-context-listener")
public class AppListener implements ServletContextListener {

	public static final String ATTRIBUTE_CONTEXT = "appContext";
	private static final String ENV_ACCESS_MODE = "ACCESS_MODE";

	private AppContext ctx;
	private Logger log;

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		try {
			stop();
		} catch (Exception e) {
			log.log(Level.SEVERE, "stop failed!", e);
		}
		ctx.clear();
		ctx = null;
		sce.getServletContext().removeAttribute(ATTRIBUTE_CONTEXT);
		log = null;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ctx = new AppContext();
		log = Logger.getLogger(AppListener.class.getName());
		ctx.put(AppContext.BASE_DIR, System.getProperty("user.home") + File.separatorChar + "smServer");
		try {
			start();
		} catch (Exception e) {
			e.printStackTrace();
			log.log(Level.SEVERE, "start failed!", e);
		}
		sce.getServletContext().setAttribute(ATTRIBUTE_CONTEXT, ctx);
	}

	public void start() throws Exception {

		Hashtable<String, String> appProps = loadAppProps();
		ctx.transfer(appProps, AppContext.SENDER_NO);
		ShortMessageSender sms = new InnoApi(appProps.get("username"), appProps.get("password"));

		// start sending threads
		Runnable sendingRunnable = (Runnable) sms;
		ctx.put(AppContext.SMS, sms);

		int noSendingThreads = Integer.valueOf(appProps.get("noSendingThreads"));
		Thread[] sendThreads = new Thread[noSendingThreads];
		for (int i = 0; i < sendThreads.length; i++) {
			sendThreads[i] = new Thread(sendingRunnable);
			sendThreads[i].start();
		}
		ctx.put(AppContext.SEND_THREADS, sendThreads);

		AbstractNewMessageWatcher nmw = getInstance(appProps.get("newMessageWatcherClass"));
		ctx.put(AppContext.NMW, nmw);

		Thread nmwThread = new Thread(nmw);
		nmwThread.start();
		ctx.put(AppContext.NMW_THREAD, nmwThread);

		AbstractPeriodicMessageWatcher pmw = getInstance(appProps.get("periodicMessageWatcherClass"));
		ctx.put(AppContext.PMW, pmw);

		Thread pmwThread = new Thread(pmw);
		pmwThread.start();
		ctx.put(AppContext.PMW_THREAD, pmwThread);
	}

	public void stop() throws Exception {
		((Thread)ctx.get(AppContext.NMW_THREAD)).interrupt();

		// stop all timers
		((Thread)ctx.get(AppContext.PMW_THREAD)).interrupt();

		// stop all senders
		stopAllSenders();
	}

	private void stopAllSenders() {
		for(Thread t : (Thread[])ctx.get(AppContext.SEND_THREADS)) {
			t.interrupt();
		}
	}

	private <T> T getInstance(String className) {
		try {
			Class<T> clazz = (Class<T>) Class.forName(className);
			Constructor<T> c = clazz.getConstructor(AppContext.class);
			return c.newInstance(ctx);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.log(Level.SEVERE, "class not found!", e);
		}
		return null;
	}

	private Hashtable<String, String> loadAppProps() {
		String accessMode = System.getenv(ENV_ACCESS_MODE);
		log.log(Level.INFO, "Using access mode {0}", accessMode);

		PropsReader pr = null;
		if("file".equals(accessMode)) {
			pr = new FilePropsReader((String) ctx.get(AppContext.BASE_DIR));
		} else if("db".equals(accessMode)) {
			pr = new DbPropsReader();
		}

		return pr.get();
	}
}
