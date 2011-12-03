package smServer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WatchDirServer {

	private File watchDir;
	private Properties msgProps;
	private Logger log;
	private ShortMessageSender sms;
	private long timeout;
	private FilenameFilter fnFilter;
	
	WatchDirServer (ShortMessageSender sms, Logger log, String watchDir) {

		this.log = log;
		this.sms = sms;
		this.watchDir = new File(watchDir);
		this.msgProps = new Properties();
		this.fnFilter = new FilenamePostfixFilter("sm");
		this.timeout = 5000;
	}

	void run() throws InterruptedException, IOException {
		
		File[] messages;

		assert(log!=null);
		assert(sms!=null);

		log.log(Level.INFO,"Server started.");
		while(true) {
			
			messages = null;
			if(!watchDir.exists()) {
				log.log(Level.SEVERE,"working directory {0} is missing!", watchDir.getAbsoluteFile());
				throw new FileNotFoundException();
			} else {
				messages = watchDir.listFiles(fnFilter);
			}
			
			if(messages!= null) {
				for(File sm: messages) {
					if(sm.isFile()) {
						try {
							processFile(sm);
						} catch (FileNotFoundException e) {
							log.log(Level.SEVERE,"File not found!", e);
							throw e;
						} catch (IOException e) {
							log.log(Level.SEVERE,"IO error!", e);
							throw e;
						}
					}
				}
			}

			Thread.sleep(timeout);
		}
	}

	private void processFile(File sm) throws FileNotFoundException, IOException {
		
		assert(sm!=null);

		msgProps.clear();
		FileReader fr = new FileReader(sm);
		msgProps.load(fr);
		String rn = msgProps.getProperty("receiverNo");
		String textMessage = msgProps.getProperty("text");
		String sendDate = msgProps.getProperty("termin");
		fr.close();

		if(rn != null && textMessage != null) {
			BigDecimal receiverNo = new BigDecimal(rn);
			ShortMessage message = new ShortMessage(Controller.senderNo,receiverNo,textMessage);
			message.setSendDate(sendDate);

			sms.send(message);
			if(sm.delete() == false) {
				log.log(Level.SEVERE, "Cannot delete file {0}. Stopping server.", sm.getName());
				throw new IOException();
			}
		}
	}
}

