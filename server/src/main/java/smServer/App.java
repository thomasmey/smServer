package smServer;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("api")
public class App extends Application {

	private final Logger log;

	public App() throws Exception {
		log = Logger.getLogger(App.class.getName());
		log.log(Level.INFO, "Starting!");
	}
}
