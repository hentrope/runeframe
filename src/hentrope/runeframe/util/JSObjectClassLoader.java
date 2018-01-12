package hentrope.runeframe.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Loads in a class to replace Java's default implementation of JSObject,
 * which allows the client to intercept the game's JavaScript calls.
 * 
 * @author hentrope
 */
public class JSObjectClassLoader extends ClassLoader {
	private final static String CLASS_NAME = "netscape.javascript.JSObject";

	public static ClassLoader fromURL(URL url) throws IOException {
		final byte[] buff = new byte[4096];
		final InputStream in = url.openStream();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			int bytesRead;
			while ((bytesRead = in.read(buff)) > 0)
				out.write(buff, 0, bytesRead);
		} finally {
			in.close();
		}

		return new JSObjectClassLoader(out.toByteArray());
	}
	
	

	private final byte[] data;
	
	public JSObjectClassLoader(byte[] data) {
		super();
		this.data = data;
	}
	
	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {

		if (CLASS_NAME.equals(name)) {
			Class<?> c = findLoadedClass(name);
			if (c == null)
				c = findClass(name);
			if (resolve)
				resolveClass(c);
			return c;
		} else
			return super.loadClass(name, resolve);
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		if (CLASS_NAME.equals(name))
			return defineClass(name, data, 0, data.length);
		return super.findClass(name);
	}
}
