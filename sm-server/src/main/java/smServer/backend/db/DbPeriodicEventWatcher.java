package smServer.backend.db;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import smServer.AppContext;
import smServer.ShortMessage;
import smServer.AbstractPeriodicEventWatcher;

public class DbPeriodicEventWatcher extends AbstractPeriodicEventWatcher {

	private Logger log;

	public DbPeriodicEventWatcher(AppContext controller) {
		super(controller);
		log = Logger.getLogger(DbPeriodicEventWatcher.class.getName());
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
			PreparedStatement ps = con.prepareStatement("select event_id, receiver_no, message_text_template, period, event_at, event_handler from sms_event_periodic");
			ResultSet q = ps.executeQuery();
			List<ShortMessage> messages = new ArrayList<>();

			while(q.next()) {
				ShortMessage msg = new ShortMessage();
				msg.put(ShortMessage.ID, String.valueOf(q.getInt(1)));
				msg.put(ShortMessage.RECEIVER_NO, q.getString(2));
				msg.put(ShortMessage.TEXT, q.getString(3));
				msg.put(ShortMessage.PERIOD, q.getString(4));
				msg.put(ShortMessage.AT, q.getString(5));
				String eventHandler = q.getString(6);
				if(eventHandler != null)
					msg.put("event_handler", eventHandler);

				messages.add(msg);
			}
			ps.close();
			con.close();

			log.log(Level.INFO, "Read {0} periodic messages!", messages.size());
			for(ShortMessage m: messages) {
				String eventId = (String)m.remove(ShortMessage.ID);
				Supplier<ShortMessage> supplier;

				String eventHandler = (String)m.remove("event_handler");
				if(eventHandler != null) {
					Class<?> c = Class.forName(eventHandler);
					Object i = c.getConstructor(String.class, ShortMessage.class).newInstance(eventId, m);
					supplier = (Supplier<ShortMessage>) i;
				} else {
					supplier = () -> m;
				}
				addPeriodicTimer(eventId, (String)m.remove(ShortMessage.PERIOD), (String)m.remove(ShortMessage.AT), supplier);
			}
		} catch (SQLException | NamingException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			log.log(Level.SEVERE, "failed to read periodic messages!", e);
		}
	}

	@Override
	public void refresh() {
		refreshPeriodicEvents();
	}
}
