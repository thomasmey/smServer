package smServer;

import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FixedMessageTimerTask extends TimerTask {

	private Properties props;
	private Controller controller;
	private Consumer<Calendar> nextTime;
	private Logger log;
	private Calendar calendar;
	private Timer timer;
	private Map<String, TimerTask> periodicTimers;
	private String timerId;

	public FixedMessageTimerTask(Controller controller, Properties msgProps) {
		this.props = msgProps;
		this.controller = controller;
		this.log = Logger.getLogger(FixedMessageTimerTask.class.getName());
	}

	@Override
	public void run() {
		log.log(Level.INFO, "Executing scheduled task {0}",props.getProperty("id"));
		controller.sendMessage(props);

		if(nextTime != null) {
			System.out.println("before=" + calendar);
			nextTime.accept(calendar);
			System.out.println("after=" + calendar);
			FixedMessageTimerTask timerTask = new FixedMessageTimerTask(controller, props);
			timerTask.setReschedule(periodicTimers, timerId, timer, calendar, nextTime);
			timer.schedule(timerTask, calendar.getTime());
			periodicTimers.put(timerId, timerTask);
		}
	}

	public void setReschedule(Map<String, TimerTask> periodicTimers, String timerId, Timer timer, Calendar cal, Consumer<Calendar> nextTime) {
		this.nextTime = nextTime;
		this.calendar = (Calendar) cal.clone();
		this.timer = timer;
		this.periodicTimers = periodicTimers;
		this.timerId = timerId;
	}
}