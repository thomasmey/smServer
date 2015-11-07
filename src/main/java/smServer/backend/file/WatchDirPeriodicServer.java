package smServer.backend.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.Controller;
import smServer.FixedMessageTimerTask;
import smServer.PeriodicMessageWatcher;

public class WatchDirPeriodicServer extends PeriodicMessageWatcher {

	private Logger log;
	private Controller controller;
	private Map<String, Timer> periodicTimers;

	public WatchDirPeriodicServer(Controller controller) {
		periodicTimers = new HashMap<>();
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

			String timerId = msgProps.getProperty("id");
			String period = msgProps.getProperty("period");
			String at = msgProps.getProperty("at");
			if(timerId == null || period == null || at == null) {
				log.log(Level.SEVERE, "invalid periodic format! id, at and period mustn't be null! {0}", periodic);
				continue;
			}

			FixedMessageTimerTask timerTask = new FixedMessageTimerTask(controller, msgProps);

			if(periodicTimers.get(timerId) != null) {
				log.log(Level.SEVERE, "Duplicate timer id detected {0}, file {1}", new Object[] { timerId, period });
				continue;
			}

			Timer timer = new Timer(timerId);
			Calendar cal = Calendar.getInstance();
			long timerPeriod = -1;

			switch(period) {
			case "hourly":
				// at = 14
				{
					timerPeriod = TimeUnit.HOURS.toMillis(1);
					int minute = Integer.valueOf(at);
					cal.set(Calendar.MINUTE, minute);
	
					if(cal.compareTo(Calendar.getInstance()) > 0)
						cal.add(Calendar.HOUR_OF_DAY, 1);
	
					timer.scheduleAtFixedRate(timerTask, cal.getTime(), timerPeriod);
				}
				break;
			case "daily":
				// at = 14:00
				{
					timerPeriod = TimeUnit.DAYS.toMillis(1);
					String[] hm = at.split(":");
					int hour = Integer.valueOf(hm[0]);
					int minute = Integer.valueOf(hm[1]);
					cal.set(Calendar.HOUR_OF_DAY, hour);
					cal.set(Calendar.MINUTE, minute);
	
					if(cal.compareTo(Calendar.getInstance()) > 0)
						cal.add(Calendar.DAY_OF_MONTH, 1);
	
					timer.scheduleAtFixedRate(timerTask, cal.getTime(), timerPeriod);
				}
				break;

			case "monthly":
				break;

			case "yearly":
				//at = 25.10 14:00
				{
					String[] dayTime = at.split(" ");

					String[] dm = dayTime[0].split("\\.");
					int day = Integer.valueOf(dm[0]);
					int month = Integer.valueOf(dm[1]);
					cal.set(Calendar.DAY_OF_MONTH, day);
					cal.set(Calendar.MONTH, month - 1);

					String[] hm = dayTime[1].split(":");
					int hour = Integer.valueOf(hm[0]);
					int minute = Integer.valueOf(hm[1]);
					cal.set(Calendar.HOUR_OF_DAY, hour);
					cal.set(Calendar.MINUTE, minute);

					if(cal.compareTo(Calendar.getInstance()) > 0)
						cal.add(Calendar.YEAR, 1);

					timerTask.setReschedule(() -> { cal.add(Calendar.YEAR, 1); timer.schedule(timerTask, cal.getTime()); } );
					timer.schedule(timerTask, cal.getTime());
				}
				break;
			}
			periodicTimers.put(timerId, timer);
		}
	}

	@Override
	public void stop() {
		stopAllTimers();
	}

	private void stopAllTimers() {
		for(Timer t : periodicTimers.values()) {
			t.cancel();
		}
	}
}
