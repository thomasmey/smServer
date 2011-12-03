package smServer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.logging.Logger;

public class Controller {

	static public BigDecimal senderNo;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {

		Logger log = Logger.getLogger("appLog");

		// setup InnoApi
		Properties props = new Properties();
		props.load(new FileReader("AppSender.properties"));

		senderNo = new BigDecimal(props.getProperty("senderNo"));

		String username = props.getProperty("username");
		String password = props.getProperty("password");

		ShortMessageSender sms = new InnoApi(log, username, password);
		String watchDir = System.getProperty("user.dir") + (props.getProperty("watchPath") == null ? "" : File.separator + props.getProperty("watchPath"));
		WatchDirServer server = new WatchDirServer(sms, log, watchDir);
		
		server.run();
	}

}
