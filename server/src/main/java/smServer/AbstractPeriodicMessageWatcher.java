package smServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import smServer.ShortMessage;

public abstract class AbstractPeriodicMessageWatcher implements Runnable, Refreshable {

	protected AppContext ctx;
	protected Map<String, ScheduledFuture> timerTasks;
	protected Map<String, FixedMessageTimerTask> timerTasksObj;
	protected ScheduledExecutorService timer;
	private Logger log;

	public AbstractPeriodicMessageWatcher(AppContext ctx) {
		this.ctx = ctx;
		timerTasks = Collections.synchronizedMap(new HashMap<>());
		timerTasksObj = Collections.synchronizedMap(new HashMap<>());
		timer = Executors.newScheduledThreadPool(1);
		log = Logger.getLogger(AbstractPeriodicMessageWatcher.class.getName());
	}

	public void stop() {
		timer.shutdown();
	}

	protected void addPeriodicTimer(ShortMessage msg) {

		msg.remove(ShortMessage.TERMIN);

		String timerId = msg.getProperty(ShortMessage.ID);
		String period = msg.getProperty(ShortMessage.PERIOD);
		String at = msg.getProperty(ShortMessage.AT);
		if(timerId == null || period == null || at == null) {
			log.log(Level.SEVERE, "invalid periodic format! id, at and period mustn't be null!");
			return;
		}

		FixedMessageTimerTask timerTask = new FixedMessageTimerTask(ctx, msg, this);

		if(timerTasks.containsKey(timerId)) {
			ScheduledFuture<?> tt = timerTasks.get(timerId);
			tt.cancel(false);
			timerTasks.remove(timerId);
		}

		timerTasksObj.put(timerId, timerTask);

		Calendar cal = Calendar.getInstance();
		ScheduledFuture<?> future = null;

		switch(period) {
		case "hourly":
			// at = 14
			{
				long timerPeriod = TimeUnit.HOURS.toMillis(1);
				int minute = Integer.valueOf(at);
				cal.set(Calendar.MINUTE, minute);

				timerTask.setReschedule(cal, (c) -> { c.add(Calendar.HOUR_OF_DAY, 1); } );

				long delay = getDelay(timerTask);
				future = timer.scheduleAtFixedRate(timerTask, delay, timerPeriod, TimeUnit.MILLISECONDS);
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

				timerTask.setReschedule(cal, (c) -> { c.add(Calendar.DAY_OF_MONTH, 1); } );

				long delay = getDelay(timerTask);
				future = timer.scheduleAtFixedRate(timerTask, delay, timerPeriod, TimeUnit.MILLISECONDS);
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

				timerTask.setReschedule(cal, (c) -> { c.add(Calendar.MONTH, 1); } );

				long delay = getDelay(timerTask);
				future = timer.schedule(timerTask, delay, TimeUnit.MILLISECONDS);
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

				timerTask.setReschedule(cal, (c) -> { c.add(Calendar.YEAR, 1); } );

				if(cal.compareTo(Calendar.getInstance()) < 0)
					cal.add(Calendar.YEAR, 1);

				long delay = getDelay(timerTask);
				future = timer.schedule(timerTask, delay, TimeUnit.MILLISECONDS);
			}
			break;
		}

		timerTasks.put(timerId, future);
	}

	private long getDelay(FixedMessageTimerTask timerTask) {
		Calendar cc = Calendar.getInstance();
		long delay = timerTask.getCalendar().getTimeInMillis() - cc.getTimeInMillis();
//		System.out.println("delay= " + delay);
		if(delay < 0)
			timerTask.applyNextTime();

		delay = timerTask.getCalendar().getTimeInMillis() - cc.getTimeInMillis();
		assert delay > 0;
		System.out.println("delay= " + delay);
		return delay;
	}

	public void reschedule(FixedMessageTimerTask timerTask) {
		timerTask.applyNextTime();
		long delay = getDelay(timerTask);
		ScheduledFuture future = timer.schedule(timerTask, delay, TimeUnit.MILLISECONDS);
		timerTasks.put(timerTask.getMessage().getProperty(ShortMessage.ID), future);
	}

	public List<Calendar> getTimerTasks() {
		List<Calendar> dates = timerTasksObj.values().stream().map(FixedMessageTimerTask::getCalendar).sorted().collect(Collectors.toList());
		return dates;
	}
}
