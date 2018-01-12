package hentrope.runeframe;

import java.io.File;
import java.util.HashMap;

import hentrope.runeframe.util.CustomDictionary;

/**
 * A {@link CustomDictionary} tailored towards storing arguments passed into the main method.
 * 
 * @author hentrope
 */
public class Arguments extends CustomDictionary<Arguments.Key> {
	private final File path;
	
	/**
	 * Instantiates an instance with the given list of arguments.
	 * 
	 * @param args Arguments provided to the program via command line.
	 */
	public Arguments(String[] args) {
		super(Arguments.Key.class);

		for (String arg: args) {
			if (!arg.startsWith("--"))
				continue;

			int split = arg.indexOf('=');
			if (split > -1) {
				Key key = Key.fromId(arg.substring(2, split));

				if (key != null)
					map.put(key, arg.substring(split+1));
			}
		}
		
		portable = getBool(Key.PORTABLE);
		path = getFile(Key.PATH);
	}

	/**
	 * Gets the path of the directory that will contain runeframe.pref.
	 * 
	 * @return The directory containing runeframe.pref
	 */
	public File getPath() {
		return path;
	}

	public static enum Key implements CustomDictionary.Key {
		DEBUG("debug", "false", null),
		PORTABLE("portable", "true", null),
		PATH("path", "$APPDATA/RuneFrame", "."),
		;

		private static final HashMap<String, Key> map = new HashMap<String, Key>();
		static {
			for (Key k: values())
				map.put(k.id, k);
		}



		final String id, normalDefault, portableDefault;

		Key(String id, String normalDefault, String portableDefault) {
			this.id = id;
			this.normalDefault = normalDefault;
			this.portableDefault = portableDefault;
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
