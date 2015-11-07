package smServer.backend.file;
import java.io.File;
import java.io.FilenameFilter;


public class FilenamePostfixFilter implements FilenameFilter {

	private String postfix;
	
	public FilenamePostfixFilter(String postfix) {
		this.postfix = postfix;
	}

	@Override
	public boolean accept(File dir, String name) {
		assert(postfix!=null);
		return name.endsWith(postfix);
	}

}
