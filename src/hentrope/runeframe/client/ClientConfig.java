package hentrope.runeframe.client;

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.Desktop;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import hentrope.runeframe.util.OperatingSystem;

/**
 * Implements the process used to load a client config from a URL.
 * <p>
 * The information is stored in an object that implements {@link AppletContext}
 * and {@link AppletStub}, allowing it to be directly accessed by
 * the game's applet.
 * 
 * @author hentrope
 * @see ClientConfig#fromURL(URL)
 */
public final class ClientConfig implements AppletContext, AppletStub {
	private final static String
	URL_PREFIX = "http://oldschool",
	URL_SUFFIX = ".runescape.com/jav_config.ws";

	/**
	 * Downloads and returns the client configuration from a given world.
	 * 
	 * @param world which server should be used to retrieve the configuration
	 * @return a ClientConfig instance containing all of the key-value pairs from the config
	 * @throws IOException if there was an exception getting the config, or if the world is less than 301
	 * @see ClientConfig#fromURL(URL)
	 */
	public static ClientConfig fromWorld(int world) throws IOException {
		world -= 300;
		if (world < 1)
			throw new UnknownHostException("oldschool" + world + ".runescape.com");
		return fromURL(new URL(URL_PREFIX + world + URL_SUFFIX));
	}
	
	/**
	 * Downloads and returns the client configuration from a default world.
	 * 
	 * @return a ClientConfig instance containing all of the key-value pairs from the config
	 * @throws IOException if there was an exception getting the config
	 * @see ClientConfig#fromURL(URL)
	 */
	public static ClientConfig fromDefaultWorld() throws IOException {
		return fromURL(new URL(URL_PREFIX + URL_SUFFIX));
	}

	/**
	 * Downloads and returns the client configuration from a given URL.
	 * 
	 * @param url URL that points to the client configuration to be downloaded
	 * @return a ClientConfig instance containing all of the key-value pairs from the config
	 * @throws IOException if there was an exception getting the config
	 */
	public static ClientConfig fromURL(URL url) throws IOException {
		final URLConnection connection = url.openConnection();
		final BufferedReader reader = new BufferedReader( new InputStreamReader(
				connection.getInputStream() ) );
		final ClientConfig config = new ClientConfig();
		
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				int splitLocation = line.indexOf('=');
				String key = line.substring(0, splitLocation);

				if ("param".equals(key) || "msg".equals(key)) {
					splitLocation = line.indexOf('=', splitLocation + 1);
					key = line.substring(0, splitLocation).replaceAll("=", "-");
				}

				config.param.put(key, line.substring(splitLocation + 1));
			}
		} finally {
			reader.close();
		}
		
		return config;
	}
	
	/*
	 * A class containing some of the client configuation's keys.
	 */
	public static class Key {
		public final static String
		CODEBASE = "codebase",
		DOWNLOAD = "download",
		INITIAL_CLASS = "initial_class";
	}


	
	private final HashMap<String, String> param = new HashMap<String, String>();
	
	private ClientConfig() {}
	
	/**
	 * Retrieves the value from the client configuration using the given key.
	 * 
	 * @param key a key in the client configuration
	 * @return the value that matches the given key, or null if there is no match
	 */
	public String get(String key) {
		return param.get(key);
	}
	
	/**
	 * Returns the name of the initial class as determined by the client
	 * configuration.
	 * 
	 * @return the initial class without the file suffix, or "client" if none is found
	 */
	public String getInitialClass() {
		String className = param.get("initial_class");
		
		if (className == null)
			return "client";
		else if (className.endsWith(".class"))
			return className.substring(0, className.length() - 6);
		else
			return className;
	}

	@Override
	public void appletResize(int width, int height) {}

	@Override
	public AppletContext getAppletContext() {
		return this;
	}

	@Override
	public URL getCodeBase() {
		try {
			return new URL(param.get(Key.CODEBASE));
		} catch (MalformedURLException e) {
			throw new InvalidParameterException();
		}
	}

	@Override
	public URL getDocumentBase() {
		return getCodeBase();
	}

	@Override
	public String getParameter(String key) {
		// If the starting character of parameter begins with a number
		char startChar = key.charAt(0);
		if (startChar >= '0' && startChar <= '9')
			// Return the parameter as it is saved with a "param-" prefix
			return param.get("param-" + key);
		else
			return null;
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public Applet getApplet(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Enumeration<Applet> getApplets() {
		throw new UnsupportedOperationException();
	}

	@Override
	public AudioClip getAudioClip(URL url) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Image getImage(URL url) {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getStream(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<String> getStreamKeys() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setStream(String key, InputStream stream) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void showDocument(URL url) {
		showUrl(url.toString());
	}

	@Override
	public void showDocument(URL url, String target) {
		showUrl(url.toString());
	}

	@Override
	public void showStatus(String status) {
		throw new UnsupportedOperationException();
	}

	private static void showUrl(String url) {
		boolean success = false;

		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if (desktop.isSupported(Desktop.Action.BROWSE))
				try {
					desktop.browse(new URI(url));
					success = true;
				} catch (IOException | URISyntaxException e) {}	
		}

		if (!success) {
			Runtime runtime = Runtime.getRuntime();
			OperatingSystem os = OperatingSystem.getCurrent();
			try {
				if (os == OperatingSystem.WINDOWS)
					runtime.exec("cmd /c start \"j\" \"" + url + "\"");

				else if (os == OperatingSystem.MAC)
					runtime.exec("open \"" + url + "\"");

				else
					runtime.exec("xdg-open " + url);
			} catch (IOException e) {
				System.err.println("Unable to open url: " + url);
			}
		}
	}

	/**
	 * Prints all of the key-value pairs in the given config.
	 * 
	 * Used to debug.
	 */
	public void print() {
		for (Entry<String, String> entry: param.entrySet())
			System.out.println(entry.getKey() + "=" + entry.getValue());
	}
}
