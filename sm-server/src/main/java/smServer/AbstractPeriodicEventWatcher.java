package smServer;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import smServer.ShortMessage;

public abstract class AbstractPeriodicEventWatcher implements Runnable, Refreshable {

	protected AppContext ctx;
	protected Map<String, ScheduledFuture> eventIdToScheduledTask;
	protected Map<String, EventTimerTask> eventIdToTimerTask;
	protected ScheduledExecutorService timer;
	private Logger log;

	public AbstractPeriodicEventWatcher(AppContext ctx) {
		this.ctx = ctx;
		eventIdToScheduledTask = Collections.synchronizedMap(new HashMap<>());
		eventIdToTimerTask = Collections.synchronizedMap(new HashMap<>());
		timer = Executors.newScheduledThreadPool(1);
		log = Logger.getLogger(AbstractPeriodicEventWatcher.class.getName());
	}

	public void stop() {
		timer.shutdown();
	}

	protected void addPeriodicTimer(String eventId, String period, String at, Supplier<ShortMessage> supplier) {

		if(eventId == null || period == null || at == null) {
			log.log(Level.SEVERE, "invalid periodic format! id, at and period mustn't be null!");
			return;
		}

		EventTimerTask timerTask = new EventTimerTask(ctx, supplier, this, eventId);

		// does an still active task is scheduled for this event id?
		if(eventIdToScheduledTask.containsKey(eventId)) {
			ScheduledFuture<?> tt = eventIdToScheduledTask.get(eventId);
			tt.cancel(false);
			eventIdToScheduledTask.remove(eventId);
		}

		eventIdToTimerTask.put(eventId, timerTask);

		Calendar cal = Calendar.getInstance();
		ScheduledFuture<?> future = null;

		switch(period) {
		case "hourly":
			// at = 14
			{
				long timerPeriod = TimeUnit.HOURS.toMillis(1);
				int minute = Integer.valueOf(at);
				cal.set(Calendar.MINUTE, minute);

				long delay = getDelay(cal, c -> c.add(Calendar.HOUR_OF_DAY, 1));
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

				long delay = getDelay(cal, c ->  c.add(Calendar.DAY_OF_MONTH, 1));
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

		eventIdToScheduledTask.put(eventId, future);
	}

	private long getDelay(Calendar cal, Consumer<Calendar> nextTime) {
		Calendar cc = Calendar.getInstance();
		long delay = cal.getTimeInMillis() - cc.getTimeInMillis();
		if(delay < 0)
			nextTime.accept(cal);

		delay = cal.getTimeInMillis() - cc.getTimeInMillis();
		assert delay > 0;
		log.log(Level.INFO, "delay={0}", delay);
		return delay;
	}

	private long getDelay(EventTimerTask timerTask) {
		return getDelay(timerTask.getCalendar(), timerTask.getReschedule());
	}

	public void reschedule(EventTimerTask timerTask) {
		timerTask.applyNextTime();
		long delay = getDelay(timerTask);

		ScheduledFuture future = timer.schedule(timerTask, delay, TimeUnit.MILLISECONDS);
		eventIdToScheduledTask.put(timerTask.getEventId(), future);
	}

	public List<Calendar> getTimerTasks() {
		List<Calendar> dates = eventIdToTimerTask.values().stream()
				.filter(tt -> { if(tt.getCalendar() == null) return false; else return true;} )
				.map(EventTimerTask::getCalendar)
				.sorted()
				.collect(Collectors.toList());
		return dates;
	}
}
