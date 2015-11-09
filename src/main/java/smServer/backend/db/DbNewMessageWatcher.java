package smServer.backend.db;

import smServer.Controller;
import smServer.NewMessageWatcher;

public class DbNewMessageWatcher extends NewMessageWatcher {

	public DbNewMessageWatcher(Controller controller) {
		super(controller);
	}

	@Override
	public void run() {
	}

}
