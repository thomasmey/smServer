package smServer;

public abstract class AbstractNewMessageWatcher implements Runnable, Refreshable {

	protected AppContext ctx;

	public AbstractNewMessageWatcher(AppContext controller) {
		this.ctx = controller;
	}
}
