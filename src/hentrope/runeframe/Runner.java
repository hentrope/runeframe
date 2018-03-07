package hentrope.runeframe;

import static hentrope.runeframe.Arguments.Key.DEBUG;
import static hentrope.runeframe.Preferences.Key.*;

import java.applet.Applet;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import hentrope.runeframe.io.FileAtlas;
import hentrope.runeframe.task.*;
import hentrope.runeframe.task.cleanup.SaveDefaultPreferences;
import hentrope.runeframe.task.cleanup.SaveGamepackCache;
import hentrope.runeframe.ui.UIStub;

/**
 * Runner class for the RuneFrame client, which contains the client's main method.
 * 
 * @author hentrope
 */
public class Runner {
	public final static int CLEANUP_DELAY = 1000;
	private static long prev = System.nanoTime();
	private static long total = 0;

	/**
	 * Entry method for the RuneFrame client. See inline comments for details.
	 * 
	 * @param arguments command line arguments provided to the client
	 * @throws Exception if there is an unhandled exception during execution
	 */
	public static void main(String[] arguments) throws Exception {
		//SwingUtilities.invokeAndWait(new Runnable() { public void run() {} });
		//time("main function start");

		/*
		 * Create a new cached thread pool to handle multi-threading the tasks
		 * performed to load the client.
		 */
		ExecutorService exec = Executors.newCachedThreadPool();

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

		/*
		 * Create a ProgressBar instance so that other tasks can update the
		 * progress bar without waiting for the GUI to be initialized.
		 */
		UIStub progress = new UIStub();

		/*
		 * If the client is set to cache the gamepack, submit tasks that will
		 * load both the gamepack and the cacheID from the disk.
		 */
		Future<LoadGamepack.Results> loadCacheGamepack = null;
		Future<Integer> loadCacheID = null;
		if (pref.getBool(CACHE_GAMEPACK)) {
			loadCacheGamepack = exec.submit(new LoadCacheGamepack(atlas.cacheJar));
			loadCacheID = exec.submit(new LoadCacheID(atlas.cacheID));
		}

		Future<LoadGame.Results> loadGame = exec.submit(
				new LoadGame(pref, atlas, progress, loadCacheID, loadCacheGamepack));

		/*
		 * Submit a task that will initialize and display the client frame,
		 * adding and displaying the previously created ProgressBar.
		 */
		//time("Beginning UI thread");
		new DisplayUI(pref, atlas, progress, loadGame).call();
		//time("Finished UI thread");
		

		LoadGame.Results game = loadGame.get();
		Applet applet = game.applet;
		
		/*
		 * Set the user.home property so that the game places its files in a
		 * a different directory.
		 */
		System.setProperty("user.home", atlas.dataDir.toString());
		
		/*
		 * Only after the applet is added to the frame may it be initialized and started.
		 */
		applet.init();
		applet.start();
		
		time("Applet started");
		totalTime("Total");

		/*
		 * Lower the current thread's priority and wait a set amount of time
		 * for the game to finish loading.
		 * 
		 * After waiting for a period of time, low-priority tasks will be
		 * performed before terminating the thread.
		 */
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
		try {
			Thread.sleep(CLEANUP_DELAY);
		} catch (InterruptedException e) {}

		/*
		 * If a preferences file does not exist, create it with the default values.
		 */
		try {
			new SaveDefaultPreferences(pref, atlas).call();
		} catch (IOException e) {
			// Ignore IOExceptions
		} catch (Exception e) {
			System.err.println("Unable to save default preferences to disk.");
			e.printStackTrace();
		}

		/*
		 * If the gamepack was downloaded remotely but the preference is set to
		 * cache it, save the gamepack to disk using bytes from the intercept
		 * stream.
		 */
		/*if (gamepack.raw != null) try {
			new SaveGamepackCache(atlas.cacheID, atlas.cacheJar, gamepack.raw, downloadID).call();
		} catch (Exception e) {
			System.err.println("Unable to save gamepack cache to disk.");
			e.printStackTrace();
		}*/

		exec.shutdown();
	}

	public synchronized static void time(String message) {
		long dif = System.nanoTime() - prev;
		System.out.println(message + ": " + (dif / 1000000));
		total += dif;
		prev = System.nanoTime();
	}
	
	public synchronized static void totalTime(String message) {
		System.out.println(message + ": " + (total / 1000000));
	}
}
