package smServer.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import smServer.ShortMessage;

public class HeizölpreisChecker implements Supplier<ShortMessage> {

	private final String eventId;
	private final ShortMessage sm;

	public HeizölpreisChecker(String eventId, ShortMessage sm) {
		this.eventId = eventId;
		this.sm = sm;
	}

	@Override
	public ShortMessage get() {
		String page = getPage();
		Number preis = extract(page);
		Number lowestPreis = getLowestPreis();
		if(preis.doubleValue() < lowestPreis.doubleValue()) {
			updateEventData(preis);
			String templateText = (String) this.sm.get(ShortMessage.TEXT);
			ShortMessage sm = new ShortMessage();
			sm.putAll(this.sm);
			sm.put(ShortMessage.TEXT, replaceTempalteText(templateText, preis));
			return sm;
		}
		return null;
	}

	private static String replaceTempalteText(String templateText, Object... args) {
		String t = templateText;
		for(int i=0,n=args.length; i < n; i++) {
			t = t.replace("{" + i + '}', String.valueOf(args[i]));
		}
		return t;
	}

	private void updateEventData(Object data) {
		DataSource dataSource = getDataSource();
		try (Connection c = dataSource.getConnection();
				PreparedStatement ps = c.prepareStatement("replace into sms_event_data (event_id, data_version, event_data) values (?, ?, ?)" );) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(data);
				oos.close();

				ps.setInt(1, Integer.valueOf(eventId));
				ps.setInt(2, 1);
				ps.setBytes(3, baos.toByteArray());
				ps.execute();
			} catch (SQLException | IOException e) {
				Logger.getLogger(HeizölpreisChecker.class.getName()).log(Level.SEVERE, "failed!", e);
			}
	}

	private DataSource getDataSource() {
		try {
			return InitialContext.doLookup("jdbc/DefaultDS");
		} catch (NamingException e) {
			Logger.getLogger(HeizölpreisChecker.class.getName()).log(Level.SEVERE, "failed!", e);
		}
		return null;
	}

	private Number getLowestPreis() {
		Number n = (Number) getEventData();
		if(n == null) {
			return Short.MAX_VALUE;
		} else return n;
	}

	private Object getEventData() {
		DataSource dataSource = getDataSource();
		try (Connection c = dataSource.getConnection();
			PreparedStatement ps = c.prepareStatement("select event_data, entry_ts, change_ts, data_version from sms_event_data where event_id = ?");) {
			ps.setInt(1, Integer.valueOf(eventId));
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(rs.getBytes(1)));
				Object ro = ois.readObject();
				ois.close();
				return ro;
			}
		} catch (SQLException | IOException | ClassNotFoundException e) {
			Logger.getLogger(HeizölpreisChecker.class.getName()).log(Level.SEVERE, "failed!", e);
		}
		return null;
	}

	private Number extract(String pageContext) {
		Matcher m = Pattern.compile("HEIZÖL</a>\\r\\n.*>(\\d{1,2},\\d{2} €)").matcher(pageContext);
		if(m.find()) {
			String ps = m.group(1);
			try {
				Number preis = NumberFormat.getCurrencyInstance(Locale.GERMANY).parse(ps);
				return preis;
			} catch (ParseException e) {
				Logger.getLogger(HeizölpreisChecker.class.getName()).log(Level.SEVERE, "failed!", e);
			}
		}
		return null;
	}

	private String getPage() {
		try(InputStream in = new URL("http://www.heizoel24.de/heizoelpreise").openStream()) {
			int len = 0, off = 0, s = 262144;
			byte[] page = new byte[s];
			while((len = in.read(page, off, s - off)) >= 0) {
				off += len; 
			}
			return new String(page,0, off,"UTF-8");
		} catch (IOException e) {
			Logger.getLogger(HeizölpreisChecker.class.getName()).log(Level.SEVERE, "failed!", e);
		}
		return null;
	}
}
