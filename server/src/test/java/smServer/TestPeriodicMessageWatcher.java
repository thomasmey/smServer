package smServer;

import org.junit.Test;

public class TestPeriodicMessageWatcher {

	AbstractPeriodicMessageWatcher pwm = new TestPwm(null);

	@Test
	public void testPwm() {
		pwm.run();
	}
}

class TestPwm extends AbstractPeriodicMessageWatcher {

	public TestPwm(AppContext ctx) {
		super(ctx);
	}

	@Override
	public void refresh() {
	}

	@Override
	public void run() {
		ShortMessage s = new ShortMessage();
		s.put(ShortMessage.ID, "test123");
		s.put(ShortMessage.RECEIVER_NO, "123456789");
		s.put(ShortMessage.AT, "30.1 22:40");
		s.put(ShortMessage.PERIOD, "yearly");

		addPeriodicTimer(s);
	}

}
