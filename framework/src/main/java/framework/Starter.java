package framework;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class Starter {

	/**
	 * @param args
	 * @throws BundleException 
	 */
	public static void main(String[] args) throws BundleException {
		ServiceLoader<FrameworkFactory> service = ServiceLoader.load(org.osgi.framework.launch.FrameworkFactory.class);
		Iterator<FrameworkFactory> i = service.iterator();
		if(!i.hasNext())
			throw new IllegalStateException();

		FrameworkFactory frameworkFactory = i.next();
		Map<String, String> configuration = new HashMap<>();
		Framework framework = frameworkFactory.newFramework(configuration);
		framework.start();

		HashMap<String, String> bundles = new HashMap<>();
		bundles.put("server", "/osgi/modules/smServer-server-0.1.0-SNAPSHOT.jar");
		bundles.put("mysql-driver", "/osgi/modules/mysql-connector-java-5.1.37.jar");

		BundleContext bundleContext = framework.getBundleContext();
		Bundle[] installedBundles = installBundles(bundleContext, bundles);
		startBundles(installedBundles);
	}

	private static Bundle[] installBundles(BundleContext bundleContext, HashMap<String, String> bundles) throws BundleException {
		Bundle[] bi = new Bundle[bundles.size()];
		int i = 0;
		for(Map.Entry<String, String> e : bundles.entrySet()) {
			URL url = Starter.class.getResource(e.getValue());
			Logger.getLogger(Starter.class.getName()).log(Level.INFO, "about to install bundle {0} - URL {1}", new Object[] {e.getKey(), url});
			Bundle bundle = bundleContext.installBundle(url.toString());
			bi[i++] = bundle;
		}
		return bi;
	}

	private static void startBundles(Bundle[] installedBundles) throws BundleException {
		for(Bundle b: installedBundles) {
			b.start();
		}
	}
}
