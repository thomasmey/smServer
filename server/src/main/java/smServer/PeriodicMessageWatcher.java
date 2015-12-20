package smServer;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PeriodicMessageWatcher implements Runnable {

	protected Controller controller;
	protected Map<String, TimerTask> periodicTimers;
	protected Timer timer;
	private Logger log;

	public PeriodicMessageWatcher(Controller controller) {
		this.controller = controller;
		periodicTimers = Collections.synchronizedMap(new HashMap<>());
		timer = new Timer();
		log = Logger.getLogger(PeriodicMessageWatcher.class.getName());
	}

	public void stop() {
		timer.cancel();
	}

	protected void addPeriodicTimer(Properties msg) {

		msg.remove("termin");

		String timerId = msg.getProperty("id");
		String period = msg.getProperty("period");
		String at = msg.getProperty("at");
		if(timerId == null || period == null || at == null) {
			log.log(Level.SEVERE, "invalid periodic format! id, at and period mustn't be null!");
			return;
		}

		FixedMessageTimerTask timerTask = new FixedMessageTimerTask(controller, msg);

		if(periodicTimers.containsKey(timerId)) {
			TimerTask tt = periodicTimers.get(timerId);
			tt.cancel();
		}

		periodicTimers.put(timerId, timerTask);

		Calendar cal = Calendar.getInstance();

		switch(period) {
		case "hourly":
			// at = 14
			{
				long timerPeriod = TimeUnit.HOURS.toMillis(1);
				int minute = Integer.valueOf(at);
				cal.set(Calendar.MINUTE, minute);

				if(cal.compareTo(Calendar.getInstance()) < 0)
					cal.add(Calendar.HOUR_OF_DAY, 1);

				timer.scheduleAtFixedRate(timerTask, cal.getTime(), timerPeriod);
			}
			break;
		case "daily":
			// at = 14:00
			{
				long timerPeriod = TimeUnit.DAYS.toMillis(1);
				String[] hm = at.split(":");
				int hour = Integer.valueOf(hm[0]);
				int minute = Integer.valueOf(hm[1]);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);

				if(cal.compareTo(Calendar.getInstance()) < 0)
					cal.add(Calendar.DAY_OF_MONTH, 1);

				timer.scheduleAtFixedRate(timerTask, cal.getTime(), timerPeriod);
			}
			break;

		case "monthly":
			// at = 12 14:00
			{
				String[] dayTime = at.split(" ");
				cal.set(Calendar.DAY_OF_MONTH,Integer.valueOf(dayTime[0]));

				String[] hm = dayTime[1].split(":");
				int hour = Integer.valueOf(hm[0]);
				int minute = Integer.valueOf(hm[1]);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);

				if(cal.compareTo(Calendar.getInstance()) < 0)
					cal.add(Calendar.MONTH, 1);

				timerTask.setReschedule(periodicTimers, timerId, timer, cal, (c) -> { c.add(Calendar.MONTH, 1); } );
				timer.schedule(timerTask, cal.getTime());
			}
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

				if(cal.compareTo(Calendar.getInstance()) < 0)
					cal.add(Calendar.YEAR, 1);

				timerTask.setReschedule(periodicTimers, timerId, timer, cal, (c) -> { c.add(Calendar.YEAR, 1); } );
				timer.schedule(timerTask, cal.getTime());
			}
			break;
		}
	}

	abstract public void refresh();

}
