package hentrope.runeframe.task;

import java.util.concurrent.Callable;

import hentrope.runeframe.Preferences;
import hentrope.runeframe.io.FileAtlas;

public class SaveDefaultPreferences implements Callable<Void> {
	private final Preferences pref;
	private final FileAtlas atlas;

	public SaveDefaultPreferences(Preferences pref, FileAtlas atlas) {
		this.pref = pref;
		this.atlas = atlas;
	}
	
	@Override
	public Void call() throws Exception {
		if (!atlas.preferences.exists())
			pref.saveDefault(atlas.preferences);
		
		return null;
	}

}
