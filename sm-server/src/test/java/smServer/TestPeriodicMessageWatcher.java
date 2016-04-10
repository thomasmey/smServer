package smServer;

import org.junit.Test;

public class TestPeriodicMessageWatcher {

	AbstractPeriodicEventWatcher pwm = new TestPwm(null);

	@Test
	public void testPwm() {
		pwm.run();
	}
}

class TestPwm extends AbstractPeriodicEventWatcher {

	public TestPwm(AppContext ctx) {
		super(ctx);
	}

	@Override
	public void refresh() {
	}

	@Override
	public void run() {
		addPeriodicTimer("test123", "yearly", "30.1 22:40", () -> new ShortMessage());
	}

}
