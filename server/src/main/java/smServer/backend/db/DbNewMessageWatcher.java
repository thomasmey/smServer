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
import smServer.NewMessageWatcher;

public class DbNewMessageWatcher extends NewMessageWatcher {

	private Logger log;

	public DbNewMessageWatcher(Controller controller) {
		super(controller);
		log = Logger.getLogger(DbNewMessageWatcher.class.getName());
	}

	@Override
	public void run() {
		processQueuedMessages();
	}

	private void processQueuedMessages() {
		log.log(Level.INFO, "Processing queued messages");

		try {
			Connection con = ConMan.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("select message_id, receiver_no, message_text, send_at from sms_message_queue", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet q = ps.executeQuery();
			List<Properties> messages = new ArrayList<>();
			while(q.next()) {
				Properties msg = new Properties();
				msg.put("id", String.valueOf(q.getInt(1)));
				msg.put("receiverNo", q.getString(2));
				msg.put("text", q.getString(3));
				String termin = q.getString(4);
				if(termin != null)
					msg.put("termin", termin);

				messages.add(msg);
				q.deleteRow();
			}
			ps.close();

			log.log(Level.INFO, "Read {0} queued messages!", messages.size());
			for(Properties m: messages) {
				controller.sendMessage(m);
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, "failed to read queued messages!", e);
		}

	}

	@Override
	public void refresh() {
		processQueuedMessages();
	}

}
