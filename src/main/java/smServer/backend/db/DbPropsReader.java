package smServer.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.PropsReader;
import smServer.backend.file.FilePropsReader;

public class DbPropsReader implements PropsReader {

	private Logger log;

	public DbPropsReader() {
		this.log = Logger.getLogger(FilePropsReader.class.getName());
	}

	public Hashtable<String, String> get() {
		Hashtable<String, String> p = new Hashtable<>();
		try {
			Connection c = ConMan.getInstance().getConnection();
			PreparedStatement ps = c.prepareStatement("select t_key, t_value from sms_app_config");
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String k = rs.getString(1);
				String v = rs.getString(2);
				p.put(k, v);
			}
		} catch(SQLException e) {
			log.log(Level.SEVERE, "failed to read app config from db!", e);
		}

		return p;
	}
}
