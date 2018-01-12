package hentrope.runeframe.io;

import java.io.File;

import hentrope.runeframe.Arguments;
import hentrope.runeframe.Preferences;

/**
 * Stores the locations of all files used by RuneFrame.
 * 
 * @author hentrope
 */
public class FileAtlas {
	public static final String PREF_FILENAME = "runeframe.pref";
	
	public final File userDir, preferences, errors, state;
	public final File dataDir, cacheJar, cacheID, certificateDir;
	public final File screenDir;

	public FileAtlas(Arguments args, Preferences pref) {
		userDir = args.getPath();
		preferences = new File(userDir, PREF_FILENAME);
		errors = new File(userDir, "error.log");
		state = new File(userDir, "runeframe.state");

		dataDir = pref.getFile(Preferences.Key.DATA_DIRECTORY);
		cacheJar = new File(dataDir, "gamepack.jar");
		cacheID = new File(dataDir, "gamepack.dat");
		certificateDir = new File(dataDir, "certificates/");
		
		screenDir = pref.getFile(Preferences.Key.SCREENSHOT_DIRECTORY);
	}
}
