package smServer.backend.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.AppContext;
import smServer.ShortMessage;
import smServer.AbstractPeriodicMessageWatcher;

public class WatchDirPeriodicServer extends AbstractPeriodicMessageWatcher {

	private Logger log;

	public WatchDirPeriodicServer(AppContext controller) {
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
		File dir = new File((String) ctx.get(AppContext.BASE_DIR), "periodic");
		if(!dir.exists())
			return;

		for(File periodic: dir.listFiles(new FilenamePostfixFilter(Constants.FILENAME_POSTFIX))) {
			ShortMessage sm = new ShortMessage();
			try {
				Reader fr = new FileReader(periodic);
				sm.load(fr);
			} catch(IOException e) {
				log.log(Level.SEVERE, "cannot load file {0}, excp {1}", new Object[] {periodic, e});
				continue;
			}

			addPeriodicTimer(sm);
		}
	}

	@Override
	public void stop() {
		super.stop();
		stopAllTimers();
	}

	private void stopAllTimers() {
		for(ScheduledFuture t : timerTasks.values()) {
			t.cancel(true);
		}
	}

	@Override
	public void refresh() {}
}
