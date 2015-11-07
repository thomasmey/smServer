package smServer;

import java.util.Properties;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FixedMessageTimerTask extends TimerTask {

	private Properties props;
	private Controller controller;
	private Runnable reschedule;
	private Logger log;

	public FixedMessageTimerTask(Controller controller, Properties msgProps) {
		this.props = msgProps;
		this.controller = controller;
		this.log = Logger.getLogger(FixedMessageTimerTask.class.getName());
	}

	@Override
	public void run() {
		log.log(Level.INFO, "Executing scheduled task {0}",props.getProperty("id"));
		props.remove("termin");
		controller.sendMessage(props);
		if(reschedule != null) {
			reschedule.run();
		}
	}

	public void setReschedule(Runnable reschedule) {
		this.reschedule = reschedule;
	}
}