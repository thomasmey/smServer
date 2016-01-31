package smServer.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.inject.Produces;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import smServer.AppContext;
import smServer.ShortMessage;
import smServer.AbstractPeriodicMessageWatcher;

public class DbPeriodicMessageWatcher extends AbstractPeriodicMessageWatcher {

	private Logger log;

	public DbPeriodicMessageWatcher(AppContext controller) {
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
			DataSource dataSource = InitialContext.doLookup("jdbc/DefaultDS");
			Connection con = dataSource.getConnection();
			PreparedStatement ps = con.prepareStatement("select message_id, receiver_no, message_text, period, send_at from sms_message_periodic");
			ResultSet q = ps.executeQuery();
			List<ShortMessage> messages = new ArrayList<>();

			while(q.next()) {
				ShortMessage msg = new ShortMessage();
				msg.put(ShortMessage.ID, String.valueOf(q.getInt(1)));
				msg.put(ShortMessage.RECEIVER_NO, q.getString(2));
				msg.put(ShortMessage.TEXT, q.getString(3));
				msg.put(ShortMessage.PERIOD, q.getString(4));
				msg.put(ShortMessage.AT, q.getString(5));

				messages.add(msg);
			}
			ps.close();
			con.close();

			log.log(Level.INFO, "Read {0} periodic messages!", messages.size());
			for(ShortMessage m: messages) {
				addPeriodicTimer(m);
			}
		} catch (SQLException | NamingException e) {
			log.log(Level.SEVERE, "failed to read periodic messages!", e);
		}
	}

	@Override
	public void refresh() {
		refreshPeriodicEvents();
	}
}
