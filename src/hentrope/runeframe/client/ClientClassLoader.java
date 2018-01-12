package hentrope.runeframe.client;

import java.applet.Applet;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * A specialized ClassLoader that loads classes from a ClientGamepack instance.
 * <p>
 * This ClassLoader uses something similar to lazy initialization, in that
 * it will not actually load a class until it is requested by the client.
 * 
 * @author hentrope
 */
public class ClientClassLoader extends SecureClassLoader {
	public static ClientClassLoader fromGamepack(ClientGamepack gamepack) {
		ClientClassLoader loader = new ClientClassLoader();
		importClassData(loader, gamepack);
		return loader;
	}

	public static ClientClassLoader fromGamepack(ClientGamepack gamepack, ClassLoader parent) {
		ClientClassLoader loader = new ClientClassLoader(parent);
		importClassData(loader, gamepack);
		return loader;
	}

	private static void importClassData(ClientClassLoader loader, ClientGamepack gamepack) {
		for (ClientGamepack.Entry file: gamepack) {
			final String name = file.entry.getName();

			if (file.entry.isDirectory() || !name.endsWith(".class"))
				continue;

			loader.classMap.put(formatClassName(name), file);
		}
	}

	private static String formatClassName(String filename) {
		return filename.substring(0, filename.length()-6).replaceAll("/", ".");
	}


	
	private final Map<String, ClientGamepack.Entry> classMap = new HashMap<String, ClientGamepack.Entry>();
	private boolean appletCreated = false;

	private ClientClassLoader() {
		super();
	}

	private ClientClassLoader(ClassLoader parent) {
		super(parent);
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
		ClientGamepack.Entry file = classMap.remove(name);

		if (file != null)
			return defineClass(name, file.data, 0, file.data.length, new CodeSource(null, file.entry.getCertificates()));
		else
			throw new ClassNotFoundException(name);
	}
}
