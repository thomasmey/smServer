package smServer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WatchDirServer implements Runnable {

	private Path watchDir;
	private Properties msgProps;
	private Logger log;
	private ShortMessageSender sms;
	private FilenameFilter fnFilter;
	
	WatchDirServer (ShortMessageSender sms, Logger log, String watchDir) {

		this.log = log;
		this.sms = sms;
		this.watchDir = Paths.get(watchDir);
		this.msgProps = new Properties();
		this.fnFilter = new FilenamePostfixFilter("sm");
	}

	public void run() {

		assert(log!=null);
		assert(sms!=null);

		log.log(Level.INFO,"Server started.");
		if(!watchDir.toFile().exists()) {
			log.log(Level.SEVERE,"working directory {0} is missing!", watchDir.toString());
			return;
		}

		processExistingFiles();
		
		FileSystem fs = watchDir.getFileSystem();
		WatchService watcher;
		try {
			watcher = fs.newWatchService();
			watchDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		while(true) {

			WatchKey curentKey = null;
			try {
				curentKey = watcher.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}

			if(curentKey != null && curentKey.isValid()) {
				List<WatchEvent<?>> currentEvents = curentKey.pollEvents();
				for(WatchEvent<?> ev: currentEvents) {
					if(ev.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
						Path currentPath =  (Path) ev.context();
						File sm = null;

						/*
						 * FIXME: This doesn't work as java returns:
						 * working directory + file name and not
						 * watch directory   + file name
						 * It's a feature not a bug?!
						 * sm = currentPath.toFile(); 
						 */
						sm = watchDir.resolve(currentPath).toFile();

						// only process files with certain file name
						if(fnFilter.accept(null, sm.getName())) {
							try {
								processFile(sm);
							} catch (FileNotFoundException ex) {
								log.log(Level.SEVERE,"File not found!", ex);
								return;
							} catch (IOException ex) {
								log.log(Level.SEVERE,"IO error!", ex);
								return;
							}
						}
					}
				}
			}
			curentKey.reset();
		}
	}

	private void processExistingFiles() {
		File files[] = watchDir.toFile().listFiles(fnFilter);
		for(File sm : files) {
			try {
				processFile(sm);
			} catch (FileNotFoundException ex) {
				log.log(Level.SEVERE,"File not found!", ex);
				return;
			} catch (IOException ex) {
				log.log(Level.SEVERE,"IO error!", ex);
				return;
			}
		}
	}

	private void processFile(File sm) throws FileNotFoundException, IOException {
		
		assert(sm!=null);

		log.log(Level.FINE,"Sending file {0}", sm.getName());

		if(!sm.exists())
			return;

		msgProps.clear();
		Reader fr = new FileReader(sm);
		msgProps.load(fr);
		String rnSplit[] = msgProps.getProperty("receiverNo").split(",");
		String textMessage = msgProps.getProperty("text");
		String sendDate = msgProps.getProperty("termin");
		fr.close();

		for(String rn: rnSplit) {
	
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
}

