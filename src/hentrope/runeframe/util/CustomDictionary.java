package hentrope.runeframe.util;

import java.io.File;
import java.util.EnumMap;

import hentrope.runeframe.io.FilenameParser;

/**
 * A form of dictionary used to manage arguments and preferences sent to the client.
 * 
 * @author hentrope
 * @param <K> The datatype to be stored in the CustomDictionary.
 */
public class CustomDictionary<K extends Enum<K> & CustomDictionary.Key> {
	protected final EnumMap<K, String> map;
	protected boolean portable = false;
	
	public CustomDictionary(Class<K> clazz) {
		map = new EnumMap<K, String>(clazz);
	}
	
	public String get(K key) {
		String value = map.get(key);
		if (value != null)
			return value;
		else
			return key.getDefault(portable);
	}

	public int getInt(K key) {
		try {
			return Integer.valueOf(get(key));
		} catch (NumberFormatException e) {
			return Integer.valueOf(key.getDefault(portable));
		}
	}

	public boolean getBool(K key) {
		String value = map.get(key);
		if ("true".equalsIgnoreCase(value))
			return true;
		else if ("false".equalsIgnoreCase(value))
			return false;
		else return Boolean.parseBoolean(key.getDefault(portable));
	}
	
	public File getFile(K key) {
		String pathname = map.get(key);
		if (pathname != null)
			pathname = pathname.trim();
		if (pathname == null || pathname.length() < 1)
			pathname = key.getDefault(portable);
		return FilenameParser.parseFile(pathname);
	}

	public static interface Key {
		String getDefault(boolean portable);
	}
}
