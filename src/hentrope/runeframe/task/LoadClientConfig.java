package hentrope.runeframe.task;

import static hentrope.runeframe.Arguments.Key.DEBUG;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;

import hentrope.runeframe.Arguments;
import hentrope.runeframe.Preferences;
import hentrope.runeframe.io.FileAtlas;

public class LoadClientConfig implements Callable<LoadClientConfig.Results> {
	private final String[] arguments;
	
	public LoadClientConfig(String[] arguments) {
		this.arguments = arguments;
	}
	
	@Override
	public Results call() throws Exception {
		/*
		 * Store all of the command line arguments in an Arguments instance.
		 */
		Arguments args = new Arguments(arguments);
		
		/*
		 * Based on the Arguments, load in all preferences from the preferences file.
		 */
		Preferences pref = new Preferences(args);
		
		/*
		 * Based on both arguments and preferences, determine the location of all files.
		 */
		FileAtlas atlas = new FileAtlas(args, pref);
		atlas.userDir.mkdirs();
		atlas.dataDir.mkdirs();
		atlas.certificateDir.mkdir();
		
		/*System.out.println("user = " + atlas.userDir); // Debug - print all directory paths
		System.out.println("system = " + atlas.systemDir);
		System.out.println("screen = " + atlas.screenDir);*/
		
		/*
		 * Unless the DEBUG flag is set, output any errors to an external file.
		 */
		if (!args.getBool(DEBUG))
			try {
				System.setErr(new PrintStream(new FileOutputStream(atlas.errors, true)));
				
			} catch (FileNotFoundException e) {
				System.err.println("Unable to open error output file.");
			}

		return new Results(args, pref, atlas);
	}

	public static class Results {
		public final Arguments args;
		public final Preferences pref;
		public final FileAtlas atlas;
		
		Results(Arguments args, Preferences pref, FileAtlas atlas) {
			this.args = args;
			this.pref = pref;
			this.atlas = atlas;
		}
	}
}
