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
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import smServer.AbstractNewMessageWatcher;
import smServer.AppContext;
import smServer.MessageUtil;
import smServer.ShortMessage;

public class DbNewMessageWatcher extends AbstractNewMessageWatcher {

	private Logger log;

	public DbNewMessageWatcher(AppContext controller) {
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
			DataSource dataSource = InitialContext.doLookup("jdbc/DefaultDS");
			Connection con = dataSource.getConnection();
			PreparedStatement ps = con.prepareStatement("select message_id, receiver_no, message_text, send_at from sms_message_queue", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet q = ps.executeQuery();
			List<ShortMessage> messages = new ArrayList<>();
			while(q.next()) {
				ShortMessage msg = new ShortMessage();
				msg.put(ShortMessage.ID, String.valueOf(q.getInt(1)));
				msg.put(ShortMessage.RECEIVER_NO, q.getString(2));
				msg.put(ShortMessage.TEXT, q.getString(3));
				String termin = q.getString(4);
				if(termin != null)
					msg.put(ShortMessage.TERMIN, termin);

				messages.add(msg);
				q.deleteRow();
			}
			ps.close();
			con.close();

			log.log(Level.INFO, "Read {0} queued messages!", messages.size());
			for(ShortMessage m: messages) {
				MessageUtil.sendMessage(ctx, m);
			}
		} catch (SQLException | NamingException e) {
			log.log(Level.SEVERE, "failed to read queued messages!", e);
		}

	}

	@Override
	public void refresh() {
		processQueuedMessages();
	}
}
