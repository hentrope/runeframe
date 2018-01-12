package hentrope.runeframe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import hentrope.runeframe.io.FileAtlas;
import hentrope.runeframe.util.CustomDictionary;

/**
 * A {@link CustomDictionary} tailored towards storing preferences loaded from runeframe.pref.
 * 
 * @author hentrope
 */
public class Preferences extends CustomDictionary<Preferences.Key> {

	/**
	 * Instantiates an instance using the runeframe.pref given by the provided Arguments.
	 * 
	 * @param args Arguments provided to the program via command line.
	 */
	public Preferences(Arguments args) {
		super(Preferences.Key.class);
		portable = args.getBool(Arguments.Key.PORTABLE);

		File file = new File(args.getPath(), FileAtlas.PREF_FILENAME);
		try (	FileInputStream input = new FileInputStream(file);
				BufferedReader reader = new BufferedReader(new InputStreamReader(input)) ) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.length() < 1 || line.charAt(0) == '#')
					continue;

				int split = line.indexOf('=');
				if (split > -1) {
					Key key = Key.fromId(line.substring(0, split));

					if (key != null)
						map.put(key, line.substring(split+1));
				}
			}
		} catch (IOException e) {
			System.out.println("Unable to load runeframe.pref");
		}
	}

	/**
	 * Saves the default preferences and comments in the provided file path.
	 * 
	 * @param file the full path of runeframe.pref
	 * @throws IOException if there is an exception while outputting the preferences
	 */
	public void saveDefault(File file) throws IOException {
		try (	FileOutputStream output = new FileOutputStream(file);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output)) ) {
			for (Preferences.Key key: Preferences.Key.values()) {
				for (String comment: key.comments) {
					writer.write("# " + comment);
					writer.newLine();
				}
				writer.write(key.id + "=" + key.getDefault(portable));
				writer.newLine();
				writer.newLine();
			}
		}
	}

	public static enum Key implements CustomDictionary.Key {
		HOME_WORLD("home-world", "0", null,
				"Sets which world the client will attempt use on startup."),
		CACHE_GAMEPACK("cache-gamepack", "true", null,
				"If true, the client will store the gamepack locally for faster startup."),
		PRESERVE_WINDOW_STATE("preserve-window-state", "true", null,
				"If true, the client will save the window's size and position between sessions."),
		GRAPHICS_ACCELERATION("graphics-acceleration", "Software", null,
				"Determines which rendering system the Java 2D system should use. If a certain",
				"system causes graphical issues, you may wish to try switching to another.",
				"Available options:",
				"  \"Software\"",
				"  \"OpenGL\"",
				"  \"DirectDraw\" (Windows only)",
				"  \"Direct3D\" (Windows only)",
				"  \"XRender\" (Linux/Solaris only)"),
		FULLSCREEN_ENABLED("fullscreen-enabled", "true", null,
				"If enabled, borderless fullscreen can be toggled by using Alt + Enter."),

		SCREENSHOT_ENABLED("screenshot-enabled", "true", null,
				"If enabled, a screenshot will automatically be saved when Print Screen is pressed."),
		SCREENSHOT_SORT("screenshot-sort", "2", null,
				"Determines how screenshots will be sorted.",
				"  0 = screenshots will be saved directly into the screenshot folder",
				"  1 = screenshots will be separated based on what year they were taken",
				"  2 = screenshots will be separated based on both year and month"),
		SCREENSHOT_SOUND("screenshot-sound", "true", null,
				"If true, a sound effect will play when a screenshot is taken."),

		DATA_DIRECTORY("data-directory", "$PROGRAMDATA/.jagex/oldschool", "./data",
				"Specifies the directory in which the game's cache and gamepack will be stored.",
				"Numerous \"pseudo-environment variables\" can be used at the beginning of the path name:",
				"  \".\" = folder in which the RuneFrame client is stored",
				"  \"$HOME\" = user profile on Windows, home directory on Unix",
				"  \"$APPDATA\" = appdata/roaming on Windows, $home/.config on Unix",
				"  \"$PROGRAMDATA\" = ProgramData on Windows, $home on Unix"),
		SCREENSHOT_DIRECTORY("screenshot-directory", "$HOME/Pictures/RuneFrame", "./screenshots",
				"Specifies the directory in which the game's cache and gamepack will be stored.",
				"This folder will not be created unless the screenshot feature is enabled."),
		;

		private static final HashMap<String, Key> map = new HashMap<String, Key>();
		static {
			for (Key k: values())
				map.put(k.id, k);
		}



		final String id, normalDefault, portableDefault;
		final String[] comments;

		Key(String id, String normalDefault, String portableDefault, String... comments) {
			this.id = id;
			this.normalDefault = normalDefault;
			this.portableDefault = portableDefault;
			this.comments = comments;
		}

		public static Key fromId(String id) {
			return map.get(id);
		}

		@Override
		public String getDefault(boolean portable) {
			return (portable && portableDefault != null) ? portableDefault : normalDefault;
		}
	}
}
