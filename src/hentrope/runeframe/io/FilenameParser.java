package hentrope.runeframe.io;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import hentrope.runeframe.util.OperatingSystem;

/**
 * Contains methods used to parse filename paths used in RuneFrame's
 * Preferences file.
 * 
 * @author hentrope
 */
public class FilenameParser {
	private static String cwd, home, appdata, programdata;

	public static File parseFile(String filename) {
		int pos = filename.indexOf('/');
		String prefix = pos < 0 ? filename : filename.substring(0, pos);
		switch (prefix.toUpperCase()) {
		
		case ".":
			if (cwd == null)
				cwd = getCwd();
			prefix = cwd;
			break;

		case "$HOME":
			if (home == null)
				home = getHome();
			prefix = home;
			break;

		case "$APPDATA":
			if (appdata == null)
				appdata = getAppdata();
			prefix = appdata;
			break;

		case "$PROGRAMDATA":
			if (programdata == null)
				programdata = getProgramdata();
			prefix = programdata;
			break;
		}
		
		if (pos < 0)
			return new File(prefix);
		else
			return new File(prefix, filename.substring(pos+1));
	}

	public static String getCwd() {
		try {
			String path = ClassLoader.getSystemClassLoader().getResource(".").getPath();
			return URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return ".";
		}
	}

	public static String getHome() {
		String home = System.getProperty("user.home");

		if (home == null) {
			if (OperatingSystem.getCurrent() == OperatingSystem.WINDOWS)
				home = System.getenv("USERPROFILE");
			else
				home = System.getenv("HOME");
		}

		return home != null ? home : "~";
	}

	public static String getAppdata() {
		String appdata = System.getProperty("user.appdata");

		if (OperatingSystem.getCurrent() == OperatingSystem.WINDOWS)
			appdata = System.getenv("APPDATA");

		return appdata != null ? appdata : getHome() + "/.config";
	}

	public static String getProgramdata() {
		String programdata = System.getProperty("user.programdata");

		if (OperatingSystem.getCurrent() == OperatingSystem.WINDOWS) {
			programdata = System.getenv("PROGRAMDATA");

			if (programdata == null)
				programdata = System.getenv("ALLUSERSPROFILE");
		}

		return programdata != null ? programdata : getHome();
	}
}
