package hentrope.runeframe.task;

import java.applet.Applet;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.concurrent.Callable;

import hentrope.runeframe.JavConfig;
import hentrope.runeframe.Runner;
import hentrope.runeframe.util.JSObjectClassLoader;

public class CreateApplet implements Callable<Applet> {
	private final JavConfig config;
	private final Map<String, byte[]> classMap;

	public CreateApplet(JavConfig config, Map<String, byte[]> classMap) {
		this.config = config;
		this.classMap = classMap;
	}

	@Override
	public Applet call() throws Exception {

		/*
		 * Before creating a ClassLoader to handle the gamepack, a different
		 * ClassLoader needs to be created in order to hook the client's
		 * JavaScript calls. This is done by loading JSObject in a separate
		 * ClassLoader so that it will replace Java's default implementation.
		 */
		ClassLoader parent = JSObjectClassLoader.fromURL(
				((URLClassLoader)Runner.class.getClassLoader()).findResource("netscape/javascript/JSObject.class"));



		/*
		 * Using this ClassLoader as the parent, a new ClassLoader can be
		 * created that will enable the game to access all of the classes
		 * defined in the gamepack.
		 */
		ClassLoader loader = new SecureClassLoader(parent) {
			@Override
			public Class<?> findClass(String name) throws ClassNotFoundException {
				byte[] data = classMap.remove(name);

				if (data == null)
					throw new ClassNotFoundException(name);

				return defineClass(name, data, 0, data.length);
			}
		};

		Applet applet = (Applet) loader.loadClass(config.getInitialClass()).newInstance();
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
}
