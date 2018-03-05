package hentrope.runeframe;

import static hentrope.runeframe.Preferences.Key.*;

import java.applet.Applet;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import hentrope.runeframe.io.FileAtlas;
import hentrope.runeframe.task.*;
import hentrope.runeframe.ui.RuneFrame;
import hentrope.runeframe.util.ProgressListener;

/**
 * Runner class for the RuneFrame client, which contains the client's main method.
 * 
 * @author hentrope
 */
public class Runner {
	public final static int CLEANUP_DELAY = 1000;

	/**
	 * Entry method for the RuneFrame client. See inline comments for details.
	 * 
	 * @param arguments command line arguments provided to the client
	 * @throws Exception if there is an unhandled exception during execution
	 */
	public static void main(String[] arguments) throws Exception {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}});
		long overallStart = System.nanoTime(); // Debug - output starting time
		long start; // start = System.nanoTime();

		start = System.nanoTime();

		/*
		 * Create a new cached thread pool to handle multi-threading the tasks
		 * performed to load the client.
		 */
		ExecutorService exec = Executors.newCachedThreadPool();
		ExecutorService swingExec = Executors.newSingleThreadExecutor();

		/*
		 * Submit a task that will create Arguments, Preferences, and FileAtlas
		 * instances based on the arguments submitted to the main method.
		 */
		Future<LoadClientConfig.Results> loadClientConfig = exec.submit(
				new LoadClientConfig(arguments) );

		/*
		 * Create a ProgressBar instance so that other tasks can update the
		 * progress bar without waiting for the GUI to be initialized.
		 */
		ProgressListener progress = new ProgressListener();

		/*
		 * Submit a task that will initialize and display the client frame,
		 * adding and displaying the previously created ProgressBar.
		 */
		Future<RuneFrame> createUIComponents = swingExec.submit(
				new CreateUIComponents(loadClientConfig, progress) );

		/*
		 * Since all remaining tasks will immediately depend on
		 * loadClientConfig completing, wait for it to complete and store the
		 * results in local variables for easier access.
		 */

		LoadClientConfig.Results clientConfigResults = loadClientConfig.get();
		Preferences pref = clientConfigResults.pref;
		FileAtlas atlas = clientConfigResults.atlas;
		
		System.out.println("Preferences:" + ((System.nanoTime() - start) / 1000000));
		
		start = System.nanoTime();
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

		/*
		 * Attempt to load the game client 3 times, terminating the process if
		 * there are more than 3 non-IO exceptions thrown.
		 */
		int counter = 3;
		
		int downloadID = -1;
		boolean triedCache = false;
		LoadGamepack.Results gamepack = null;
		Applet applet = null;
		while (applet == null) {
			try {
				gamepack = null;

				progress.setProgress(0, "Loading config");
				JavConfig jav = new LoadJavConfig(pref.getInt(HOME_WORLD)).call();
				downloadID = Integer.parseInt(jav.get(JavConfig.Key.DOWNLOAD));
				
				System.out.println("JavConfig:" + ((System.nanoTime() - start) / 1000000));
				
				
				start = System.nanoTime();
				
				progress.setProgress(0, "Loading application");
				if (!triedCache && pref.getBool(CACHE_GAMEPACK)) try {
					triedCache = true;
					int cacheID = loadCacheID.get();
					
					if (cacheID == downloadID)
						gamepack = loadCacheGamepack.get();
				} catch (Exception e) {
					System.err.println("Unable to load local cache.");
					e.printStackTrace();
				}

				if (gamepack == null)
					gamepack = new LoadWebGamepack(jav, pref.getBool(CACHE_GAMEPACK), progress, atlas.certificateDir).call();
				
				progress.setProgress(100, "Launching application");
				
				start = System.nanoTime();
				
				applet = swingExec.submit(
						new CreateApplet(jav, gamepack.classMap) ).get();
				
				System.out.println("Applet:" + ((System.nanoTime() - start) / 1000000));
			} catch (IOException e) {
				retry(progress, "Connection error.", 15);
			} catch (Exception e) {
				counter--;
				if (counter > 0) {
					if (e instanceof SecurityException || e instanceof GeneralSecurityException)
						retry(progress, "Invalid gamepack.", 10);
					else if (e instanceof ReflectiveOperationException)
						retry(progress, "Classload error.", 10);
					else throw e;
				} else throw e;
			}
		}

		start = System.nanoTime();

		/*
		 * Set the user.home property so that the game places its files in a
		 * a different directory.
		 */
		System.setProperty("user.home", atlas.dataDir.toString());

		swingExec.submit(
				new StartApplet(applet, createUIComponents.get()) ).get();
		//new StartApplet(applet, createUIComponents.get()).call();
		System.out.println("StartApplet:" + ((System.nanoTime() - start) / 1000000));

		//System.out.println((System.nanoTime() - start) / 1000000); // Debug - output loading time
		System.out.println("Overall: " + ((System.nanoTime() - overallStart) / 1000000));
		

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
		if (gamepack.raw != null) try {
			new SaveGamepackCache(atlas.cacheID, atlas.cacheJar, gamepack.raw, downloadID).call();
		} catch (Exception e) {
			System.err.println("Unable to save gamepack cache to disk.");
			e.printStackTrace();
		}

		exec.shutdown();
	}

	/**
	 * Causes the current thread to sleep for a given number of seconds, updating
	 * the given {@link ProgressListener} once every second.
	 * 
	 * @param listener ProgressListener to be notified of the time remaining
	 * @param message message to be displayed alongside the remaining time
	 * @param seconds how many seconds the thread should wait before continuing
	 */
	private static void retry(ProgressListener listener, String message, int seconds) {
		final long startTime = System.currentTimeMillis();

		for (int i = 0; i < seconds; i++) {
			listener.setProgress(0, message + " Retrying in " + (seconds-i));

			long sleepTime = startTime + (i+1) * 1000 - System.currentTimeMillis();
			if (sleepTime > 0)
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	}
}
