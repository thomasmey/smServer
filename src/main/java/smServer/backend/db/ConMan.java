package smServer.backend.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConMan {

	public static final String ENV_DB_URL = "DB_URL";
	public static final String ENV_DB_USERNAME = "DB_USERNAME";
	public static final String ENV_DB_PASSWORD = "DB_PASSWORD";

	private static ConMan instance;

	private Connection con;
	private String url;

	private ConMan() {
		url = System.getenv(ENV_DB_URL);
	}

	public synchronized static ConMan getInstance() {
		if(instance == null) {
			instance = new ConMan();
		}
		return instance;
	}

	synchronized Connection getConnection() throws SQLException {
		if(con == null) {
			con = getNewConnection();
		}
		return con;
	}

	private Connection getNewConnection() throws SQLException {
		return DriverManager.getConnection(url, System.getenv(ENV_DB_USERNAME), System.getenv(ENV_DB_PASSWORD));
	}
}
