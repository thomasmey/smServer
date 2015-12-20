package smServer.backend.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.PropsReader;

public class FilePropsReader implements PropsReader {

	private String baseDir;
	private Logger log;

	public FilePropsReader(String baseDir) {
		this.baseDir = baseDir;
		this.log = Logger.getLogger(FilePropsReader.class.getName());
	}

	public Hashtable<String, String> get() {
		try {
			Properties p = new Properties();
			p.load(new FileReader(new File(baseDir, "AppSender.properties")));
			return (Hashtable)p;
		} catch (IOException e) {
			log.log(Level.SEVERE,"Failed to load app props!", e);
		}
		return null;
	}
}
