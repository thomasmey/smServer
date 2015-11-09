package smServer.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.Controller;
import smServer.PeriodicMessageWatcher;

public class DbPeriodicMessageWatcher extends PeriodicMessageWatcher {

	private Logger log;

	public DbPeriodicMessageWatcher(Controller controller) {
		super(controller);
		log = Logger.getLogger(DbPeriodicMessageWatcher.class.getName());
	}

	@Override
	public void run() {
		refreshPeriodicEvents();
	}

	private void refreshPeriodicEvents() {
		log.log(Level.INFO, "Processing periodic events");

		try {
			Connection con = ConMan.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("select message_id, receiver_no, message_text, period, send_at from sms_message_periodic");
			ResultSet q = ps.executeQuery();
			while(q.next()) {
				Properties msgProps = new Properties();
				msgProps.put("id", String.valueOf(q.getInt(1)));
				msgProps.put("receiverNo", q.getString(2));
				msgProps.put("text", q.getString(3));
				msgProps.put("period", q.getString(4));
				msgProps.put("at", q.getString(5));

				addPeriodicTimer(msgProps);
			}
			ps.close();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "failed to read periodic messages!", e);
		}
	}
}
