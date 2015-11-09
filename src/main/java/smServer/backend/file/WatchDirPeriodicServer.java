package smServer.backend.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.Controller;
import smServer.PeriodicMessageWatcher;

public class WatchDirPeriodicServer extends PeriodicMessageWatcher {

	private Logger log;

	public WatchDirPeriodicServer(Controller controller) {
		super(controller);
	}

	@Override
	public void run() {
		try {
			while(true) {
				refreshPeriodicEvents();
				Thread.sleep(TimeUnit.SECONDS.toMillis(5));
			}
		} catch (InterruptedException e) {}
	}

	private void refreshPeriodicEvents() {
		log.log(Level.FINE, "Processing periodic events");
		File dir = new File(controller.getBaseDir(), "periodic");
		if(!dir.exists())
			return;

		for(File periodic: dir.listFiles(new FilenamePostfixFilter(Constants.FILENAME_POSTFIX))) {
			Properties msgProps = new Properties();
			try {
				Reader fr = new FileReader(periodic);
				msgProps.load(fr);
			} catch(IOException e) {
				log.log(Level.SEVERE, "cannot load file {0}, excp {1}", new Object[] {periodic, e});
				continue;
			}

			addPeriodicTimer(msgProps);
		}
	}

	@Override
	public void stop() {
		super.stop();
		stopAllTimers();
	}

	private void stopAllTimers() {
		for(TimerTask t : periodicTimers.values()) {
			t.cancel();
		}
	}
}
