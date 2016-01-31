package smServer;

import java.util.Calendar;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.ShortMessage;

public class FixedMessageTimerTask implements Runnable {

	private Logger log;
	private ShortMessage message;

	private AppContext ctx;
	private Consumer<Calendar> nextTime;
	private Calendar calendar;

	private AbstractPeriodicMessageWatcher pwm;

	public FixedMessageTimerTask(AppContext context, ShortMessage msgProps, AbstractPeriodicMessageWatcher pwm) {
		this.log = Logger.getLogger(FixedMessageTimerTask.class.getName());
		this.message = msgProps;
		this.ctx = context;
		this.pwm = pwm;
	}

	@Override
	public void run() {
		log.log(Level.INFO, "Executing scheduled task {0}", message.getProperty("id"));
		MessageUtil.sendMessage(ctx, message);

		if(nextTime != null) {
			pwm.reschedule(this);
		}
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
	public ShortMessage getMessage() {
		return message;
	}
}