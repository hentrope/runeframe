package hentrope.runeframe.task;

import static hentrope.runeframe.Preferences.Key.CACHE_GAMEPACK;
import static hentrope.runeframe.Preferences.Key.HOME_WORLD;

import java.applet.Applet;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import hentrope.runeframe.JavConfig;
import hentrope.runeframe.Preferences;
import hentrope.runeframe.io.FileAtlas;
import hentrope.runeframe.task.game.CreateApplet;
import hentrope.runeframe.task.game.LoadJavConfig;
import hentrope.runeframe.task.game.LoadWebGamepack;
import hentrope.runeframe.ui.UIStub;

public class LoadGame implements Callable<LoadGame.Results> {
	private final Preferences pref;
	private final FileAtlas atlas;
	private final UIStub stub;
	private final Future<Integer> loadCacheID;
	private final Future<LoadGamepack.Results> loadCacheGamepack;
	
	public LoadGame(Preferences pref, FileAtlas atlas, UIStub stub,
			Future<Integer> loadCacheID, Future<LoadGamepack.Results> loadCacheGamepack) {
		this.pref = pref;
		this.atlas = atlas;
		this.stub = stub;
		this.loadCacheID = loadCacheID;
		this.loadCacheGamepack = loadCacheGamepack;
	}

	@Override
	public LoadGame.Results call() throws Exception {
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

				stub.setProgress(0, "Loading config");

				JavConfig jav = new LoadJavConfig(pref.getInt(HOME_WORLD)).call();
				downloadID = Integer.parseInt(jav.get(JavConfig.Key.DOWNLOAD));

				stub.setProgress(0, "Loading application");
				if (!triedCache && pref.getBool(CACHE_GAMEPACK)) try {
					triedCache = true;
					int cacheID = loadCacheID.get();
					
					if (cacheID == downloadID)
						gamepack = loadCacheGamepack.get();
					else
						loadCacheGamepack.cancel(true);
				} catch (Exception e) {
					System.err.println("Unable to load local cache.");
					e.printStackTrace();
				}

				if (gamepack == null)
					gamepack = new LoadWebGamepack(jav, pref.getBool(CACHE_GAMEPACK), stub, atlas.certificateDir).call();

				stub.setProgress(100, "Launching application");

				applet = new CreateApplet(jav, gamepack.classMap).call();
			} catch (IOException e) {
				retry(stub, "Connection error.", 15);
			} catch (Exception e) {
				counter--;
				if (counter > 0) {
					if (e instanceof SecurityException || e instanceof GeneralSecurityException)
						retry(stub, "Invalid gamepack.", 10);
					else if (e instanceof ReflectiveOperationException)
						retry(stub, "Classload error.", 10);
					else throw e;
				} else throw e;
			}
		}
		
		return new LoadGame.Results(applet);
	}
	
	/**
	 * Causes the current thread to sleep for a given number of seconds, updating
	 * the given {@link UIStub} once every second.
	 * 
	 * @param listener ProgressListener to be notified of the time remaining
	 * @param message message to be displayed alongside the remaining time
	 * @param seconds how many seconds the thread should wait before continuing
	 */
	private static void retry(UIStub listener, String message, int seconds) {
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

	public static class Results {
		public final Applet applet;
		
		Results(Applet applet) {
			this.applet = applet;
		}
	}
}
