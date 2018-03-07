package hentrope.runeframe.task.game;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.concurrent.Callable;

import hentrope.runeframe.JavConfig;

public class LoadJavConfig implements Callable<JavConfig> {
	private final static String
	URL_PREFIX = "http://oldschool",
	URL_SUFFIX = ".runescape.com/jav_config.ws";

	private final int world;

	public LoadJavConfig(int world) {
		this.world = world;
	}

	@Override
	public JavConfig call() throws Exception {
		if (world > 300) try {
			return fromURL(new URL(URL_PREFIX + (world - 300) + URL_SUFFIX));
		} catch (IOException | NumberFormatException e) {}

		return fromURL(new URL(URL_PREFIX + URL_SUFFIX));
		
	}

	/**
	 * Downloads and returns the client configuration from a given URL.
	 * 
	 * @param url URL that points to the client configuration to be downloaded
	 * @return a JavConfig instance containing all of the key-value pairs from the config
	 * @throws IOException if there was an exception getting the config
	 */
	public static JavConfig fromURL(URL url) throws IOException {
		
		//final URLConnection connection = url.openConnection();
		final BufferedReader reader = new BufferedReader( new InputStreamReader(
				//connection.getInputStream() ) );
				//new FileInputStream("jav_config.ws") ) );
				url.openStream() ) );
		final HashMap<String, String> map = new HashMap<String, String>();

		try {
			String line;
			while ((line = reader.readLine()) != null) {
				int splitLocation = line.indexOf('=');
				String key = line.substring(0, splitLocation);

				if ("param".equals(key) || "msg".equals(key)) {
					splitLocation = line.indexOf('=', splitLocation + 1);
					key = line.substring(0, splitLocation).replaceAll("=", "-");
				}

				map.put(key, line.substring(splitLocation + 1));
			}
		} finally {
			reader.close();
		}

		return new JavConfig(map);
	}
}
