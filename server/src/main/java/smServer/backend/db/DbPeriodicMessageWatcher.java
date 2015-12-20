package smServer.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
			List<Properties> messages = new ArrayList<>();

			while(q.next()) {
				Properties msg = new Properties();
				msg.put("id", String.valueOf(q.getInt(1)));
				msg.put("receiverNo", q.getString(2));
				msg.put("text", q.getString(3));
				msg.put("period", q.getString(4));
				msg.put("at", q.getString(5));

				messages.add(msg);
			}
			ps.close();

			log.log(Level.INFO, "Read {0} periodic messages!", messages.size());
			for(Properties m: messages) {
				addPeriodicTimer(m);
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, "failed to read periodic messages!", e);
		}
	}

	@Override
	public void refresh() {
		refreshPeriodicEvents();
	}

}
