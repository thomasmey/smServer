package smServer;

import java.util.Calendar;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.ShortMessage;

public class EventTimerTask implements Runnable {

	private Logger log;
	private Supplier<ShortMessage> message;

	private AppContext ctx;
	private Consumer<Calendar> nextTime;
	private Calendar calendar;

	private AbstractPeriodicEventWatcher pwm;
	private String eventId;

	public EventTimerTask(AppContext context, Supplier<ShortMessage> msg, AbstractPeriodicEventWatcher pwm, String eventId) {
		this.log = Logger.getLogger(EventTimerTask.class.getName());
		this.message = msg;
		this.ctx = context;
		this.pwm = pwm;
		this.eventId = eventId;
	}

	@Override
	public void run() {
		log.log(Level.INFO, "Executing scheduled task {0}", eventId);

		ShortMessage m = message.get();
		if(m != null) MessageUtil.sendMessage(ctx, m);

		if(nextTime != null) pwm.reschedule(this);
	}

	public void setReschedule(Calendar cal, Consumer<Calendar> nextTime) {
		this.nextTime = nextTime;
		this.calendar = (Calendar) cal.clone();
	}

	public void applyNextTime() {
		nextTime.accept(calendar);
	}
	public Calendar getCalendar() {
		return calendar;
	}
	public String getEventId() {
		return eventId;
	}

	public Consumer<Calendar> getReschedule() {
		return nextTime;
	}
}