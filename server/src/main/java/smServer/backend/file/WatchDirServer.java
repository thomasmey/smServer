package smServer.backend.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
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

import smServer.AbstractNewMessageWatcher;
import smServer.AppContext;
import smServer.MessageUtil;
import smServer.ShortMessage;

public class WatchDirServer extends AbstractNewMessageWatcher {

	private final Logger log;
	private final FilenameFilter fnFilter;
	private final Path watchDir;

	public WatchDirServer(AppContext controller) {
		super(controller);
		this.log = Logger.getLogger(WatchDirServer.class.getName());
		this.watchDir = Paths.get((String)controller.get(AppContext.BASE_DIR));
		this.fnFilter = new FilenamePostfixFilter(Constants.FILENAME_POSTFIX);
	}

	public void run() {

		assert log != null;

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
			log.log(Level.SEVERE, "Failed to register watch dir service", e);
			return;
		}

		while(true) {

			if(Thread.currentThread().isInterrupted())
				return;

			WatchKey currentKey = null;
			try {
				currentKey = watcher.take();
			} catch (InterruptedException e) {
//				log.log(Level.INFO, "WatchDirServer was interrupted", e);
				return;
			}

			if(currentKey == null)
				continue;

			if(currentKey.isValid()) {
				List<WatchEvent<?>> currentEvents = currentKey.pollEvents();
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
							} catch (IOException ex) {
								log.log(Level.SEVERE,"IO error!", ex);
								Thread.currentThread().interrupt();
							}
						}
					}
				}
			}
			currentKey.reset();
		}
	}

	private void processExistingFiles() {
		File files[] = watchDir.toFile().listFiles(fnFilter);
		for(File sm : files) {
			try {
				processFile(sm);
			} catch (FileNotFoundException ex) {
				log.log(Level.SEVERE,"File not found!", ex);
			} catch (IOException ex) {
				log.log(Level.SEVERE,"IO error!", ex);
				return;
			}
		}
	}

	void processFile(File sm) throws IOException {

		assert sm != null;

		log.log(Level.FINE,"Sending file {0}", sm.getName());

		if(!sm.exists())
			return;

		ShortMessage msg = new ShortMessage();
		Reader fr = new FileReader(sm);
		msg.load(fr);
		fr.close();

		MessageUtil.sendMessage(ctx, msg);

		if(sm.delete() == false) {
			log.log(Level.SEVERE, "Cannot delete file {0}. Stopping server.", sm.getName());
			throw new IOException();
		}
	}

	@Override
	public void refresh() {}
}

