package hentrope.runeframe.client;

import java.applet.Applet;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.Map;

public class Gamepack extends SecureClassLoader {
	/**
	 * Data obtained from loading jav_config.ws.
	 */
	public final ClientConfig config;
	
	/**
	 * A map of all classes within the gamepack, mapped from
	 */
	private final Map<String, Entry> classMap;

	/**
	 * The raw bytes representing the unpacked JAR downloaded from the remote server.
	 */
	public final byte[] raw;

	/**
	 * A flag that keeps track of whether an Applet has already been instantiated from this gamepack.
	 */
	private boolean appletCreated = false;
	
	public Gamepack(ClassLoader parent, ClientConfig config, Map<String, Entry> classMap, byte[] raw) {
		super(parent);

		this.config = config;
		this.classMap = classMap;
		this.raw = raw;
	}

	public Applet createApplet(ClientConfig config) throws ReflectiveOperationException {
		if (appletCreated)
			throw new InstantiationException("Only one applet can be created per ClientClassLoader.");
		
		Applet applet = (Applet) loadClass(config.getInitialClass()).newInstance();
		applet.setStub(config);

		//if (true)
			applet.setLayout(null);

		// Listen for any new components being added to the Applet container.
		// If a Canvas is added, enable "ignore repaint" to stop screen flickering.
		// Note: Found that this was unnecessary when using heavyweight components
		/*applet.addContainerListener(new ContainerListener() {
			@Override
			public void componentAdded(ContainerEvent e) {
				Component c = e.getChild();
				if (c instanceof Canvas)
					((Canvas) c).setIgnoreRepaint(true);
			}

			@Override
			public void componentRemoved(ContainerEvent e) {}
		});*/

		return applet;
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		Entry entry = classMap.remove(name);

		if (entry == null)
			throw new ClassNotFoundException(name);

		return defineClass(name, entry.data, 0, entry.data.length, new CodeSource(null, entry.certs));
	}

	public static class Entry {
		public final byte[] data;
		public final Certificate[] certs;

		public Entry(byte[] data, Certificate[] certs) {
			this.data = data;
			this.certs = certs;
		}
	}
}
