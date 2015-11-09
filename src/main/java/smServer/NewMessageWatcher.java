package smServer;

public abstract class NewMessageWatcher implements Runnable {

	protected Controller controller;

	public NewMessageWatcher(Controller controller) {
		this.controller = controller;
	}
}
