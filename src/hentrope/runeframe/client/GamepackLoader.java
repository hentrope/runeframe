package hentrope.runeframe.client;

import static hentrope.runeframe.Preferences.Key.CACHE_GAMEPACK;
import static hentrope.runeframe.Preferences.Key.HOME_WORLD;

import java.applet.Applet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import hentrope.runeframe.Preferences;
import hentrope.runeframe.Runner;
import hentrope.runeframe.client.Gamepack.Entry;
import hentrope.runeframe.io.FileAtlas;
import hentrope.runeframe.io.InterceptInputStream;
import hentrope.runeframe.util.CertificateVerifier;
import hentrope.runeframe.util.JSObjectClassLoader;
import hentrope.runeframe.util.ProgressListener;

public class GamepackLoader {
	private static ExecutorService executor = Executors.newFixedThreadPool(1);
	private static Future<Integer> cacheID = null;
	private static Future<Map<String, Gamepack.Entry>> cacheGamepack = null;

	/**
	 * A series of operations that loads a game client's configuration and gamepack
	 * into memory, and creates a ClassLoader that allows the game code to be run.
	 * <p>
	 * The game is loaded in three major steps, each of which is implemented in
	 * one of the accompanying classes:
	 * <ol>
	 * <li>The client configuration is loaded from Jagex's jav_config.ws page.
	 *     The results are stored in a {@link ClientConfig} instance, which
	 *     is later used as an AppletStub for the Applet.
	 * <li>Using values obtained from the client configuration, a gamepack will
	 *     either be downloaded from Jagex's servers, or loaded in from local
	 *     cache. The end result will be an instance of {@link ClientGamepack},
	 *     which will store both the metadata of the files in the gamepack, as
	 *     well as the data of the files themselves.
	 * <li>A {@link ClientClassLoader} will be created from the gamepack
	 *     data, which will function as the ClassLoader which will load all of
	 *     the classes needed by the game.
	 * </ol>
	 * In addition, a {@link JSObjectClassLoader} will be created in order
	 * to hook JavaScript calls from the game.
	 * <p>
	 * If any of these steps fail, an exception will be thrown so that the process
	 * can be restarted.
	 * 
	 * @param pref An instance which contains all of the user's preferences
	 * @param atlas An instance defining the location of all relevant files and directories
	 * @param listener The listener which will receive progress updates as the client loads
	 * @return a Gamepack instance that contains public references to the loaded data
	 * @throws IOException if there is an IOException while attempting to load the gamepack
	 * @throws GeneralSecurityException if there is a GeneralSecurityException while attempting to validate the gamepack
	 * @throws ReflectiveOperationException if the ClassLoader is unable to load the client classes
	 * @throws SecurityException if there is a SecurityException while attempting to validate the gamepack
	 * @see ClientConfig
	 * @see ClientGamepack
	 * @see ClientClassLoader
	 * @see JSObjectClassLoader
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Gamepack loadAll(Preferences pref, FileAtlas atlas, ProgressListener listener)
			throws IOException, GeneralSecurityException, SecurityException {

		listener.setProgress(0, "Loading config");

		ClientConfig config;
		Map<String, Gamepack.Entry> classMap = null;
		byte[] raw = null;
		
		executor;
		
		if (pref.getBool(CACHE_GAMEPACK) && cacheID == null) {
			executor = Executors.newFixedThreadPool(1);
			
			cacheID = executor.submit(() -> {
				try ( RandomAccessFile idFile = new RandomAccessFile(atlas.cacheID, "rwd") ) {
					return idFile.readInt();
				} catch (GeneralSecurityException | IOException | SecurityException e) {
					return 0;
				}
			});
			
			cacheGamepack = executor.submit(() > {
				try ( InputStream fileInput = new FileInputStream(atlas.cacheJar) ) {
					InputStream stream = wrapStream(
							fileInput,
							(int)atlas.cacheJar.length(),
							null,
							listener);
					return ClientGamepack.fromStream(stream, null);
				} /*catch (GeneralSecurityException | IOException | SecurityException e) {
					e.printStackTrace();
				}*/
			});
		}

		/*
		 * If the preferences state that the gamepack should be loaded from
		 * cache, first attempt to load it locally using a pair of asynchronous tasks.
		 * 
		 * Since this code will have already been verified when it was
		 * downloaded, there's no need to verify it again.
		 */
		Thread cacheIDThread, gamepackThread;
		int[] cacheID = {-1};
		Map[] map = {null};
		if (pref.getBool(CACHE_GAMEPACK)) {
			// Create a thread that loads the gamepack's cacheID from disk
			cacheIDThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try ( RandomAccessFile idFile = new RandomAccessFile(atlas.cacheID, "rwd") ) {
						cacheID[0] = idFile.readInt();
					} catch (GeneralSecurityException | IOException | SecurityException e) {
						cacheID[0] = 0;
					}
				}
			});
			cacheIDThread.start();

			// Create a thread that loads the gamepack from disk
			gamepackThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try ( InputStream fileInput = new FileInputStream(atlas.cacheJar) ) {
						InputStream stream = wrapStream(
								fileInput,
								(int)atlas.cacheJar.length(),
								null,
								listener);
						gamepack = ClientGamepack.fromStream(stream, null);
					} catch (GeneralSecurityException | IOException | SecurityException e) {
						e.printStackTrace();
					}
				}
			});
			gamepackThread.start();
		}

		/*
		 * Load the client config from either the preferred world if available,
		 * or the default world if that world is invalid/fails.
		 */
		try {
			config = ClientConfig.fromWorld(pref.getInt(HOME_WORLD));
		} catch (IOException | NumberFormatException e) {
			try {
				config = ClientConfig.fromDefaultWorld();
			} catch (IOException e1) {
				// If we recieve a second error, 
				if (pref.getBool(CACHE_GAMEPACK)) {
					cacheIDThread.interrupt();
					gamepackThread.interrupt();
				}
				throw e1;
			}
		}

		listener.setProgress(0, "Loading application");

		/*
		 * If the client is set to cache the gamepack, handle joining or
		 * closing the threads now that the cacheID can be checked.
		 */
		if (pref.getBool(CACHE_GAMEPACK)) {
			// Wait for the cacheID thread to return a result.
			while (cacheID[0] == -1) try {
				cacheIDThread.join();
			} catch (InterruptedException e) {}

			// Compare the cacheID of the local gamepack to that from the config.
			// If they're the same, wait for the gamepack to return its classes.
			if (Integer.parseInt(config.get(ClientConfig.Key.DOWNLOAD)) == cacheID[0]) {
				while (map == null) try {
					gamepackThread.join();
				} catch (InterruptedException e) {}

				classMap = map[0];
			}

			// Otherwise, close the thread and disregard its result.
			else {
				gamepackThread.interrupt();
			}
		}

		/*
		 * If the preferences state that the gamepack should be loaded from
		 * cache, first attempt to load it locally. This process will also
		 * compare the gamepack's ID to make sure that it is not out of date.
		 * 
		 * Since this code will have already been verified when it was
		 * downloaded, there's no need to verify it again.
		 */
		ClientGamepack gamepack = null;
		InterceptInputStream intercept = null;
		if (pref.getBool(CACHE_GAMEPACK)) {
			try (	InputStream fileInput = new FileInputStream(atlas.cacheJar);
					RandomAccessFile idFile = new RandomAccessFile(atlas.cacheID, "rwd") ) {

				if (Integer.parseInt(config.get(ClientConfig.Key.DOWNLOAD)) == idFile.readInt()) {
					InputStream stream = wrapStream(
							fileInput,
							(int)atlas.cacheJar.length(),
							null,
							listener);
					gamepack = ClientGamepack.fromStream(stream, null);
				}
			} catch (GeneralSecurityException | IOException | SecurityException e) {}
		}

		/*
		 * If the local gamepack fails to load, is out of date, or is not set
		 * to use caching, it will fall back on attempting to load it from the
		 * server instead.
		 * 
		 * Since this loads code from over the internet, the JAR will be
		 * verified to ensure that all code is signed by Jagex.
		 */
		if (gamepack == null) {
			CertificateVerifier verifier = new CertificateVerifier(atlas.certificateDir);

			URL url = new URL(config.get("codebase") + config.get("initial_jar"));
			URLConnection connection = url.openConnection();
			connection.addRequestProperty("accept-encoding", "pack200-gzip");

			try ( InputStream urlStream = connection.getInputStream() ) {
				InputStream stream = wrapStream(
						urlStream,
						connection.getContentLength(),
						connection.getContentEncoding(),
						listener);

				if (pref.getBool(CACHE_GAMEPACK))
					stream = intercept = new InterceptInputStream(stream);

				gamepack = ClientGamepack.fromStream(stream, verifier);
			}
		}

		listener.setProgress(100, "Launching application");

		/*
		 * Before creating a ClassLoader to handle the gamepack, a different
		 * ClassLoader needs to be created in order to hook the client's
		 * JavaScript calls. This is done by loading JSObject in a separate
		 * ClassLoader so that it will replace Java's default implementation.
		 */
		ClassLoader parent = JSObjectClassLoader.fromURL(
				((URLClassLoader)Runner.class.getClassLoader()).findResource("netscape/javascript/JSObject.class"));

		/*
		 * Using this ClassLoader as the parent, a new ClassLoader can be
		 * created that will enable the game to access all of the classes
		 * defined in the gamepack.
		 */
		ClientClassLoader loader = ClientClassLoader.fromGamepack(gamepack, parent);
		Applet applet = loader.createApplet(config);

		/*
		 * Return a new instance of Client that contains references to all of
		 * the resources loaded during this process.
		 */
		return new Client(config, gamepack, intercept, applet);
	}

	private static class CacheLoader {
		private static CacheLoader instance;
		
		static CacheLoader load(File cacheIDFile, File gamepackFile) {
			if (instance == null)
				instance = new CacheLoader(cacheIDFile, gamepackFile);
			return instance;
		}
		
		private Thread cacheIDThread, gamepackThread;
		private int cacheID = -1;
		private boolean classMapLoaded = false;
		private Map<String, Gamepack.Entry> classMap = null;

		private CacheLoader(File cacheIDFile, File gamepackFile) {
			
			
			gamepackThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try ( InputStream fileInput = new FileInputStream(gamepackFile) ) {
						InputStream stream = wrapStream(
								fileInput,
								(int)gamepackFile.length(),
								null,
								null);
						gamepack = ClientGamepack.fromStream(stream, null);
					} catch (GeneralSecurityException | IOException | SecurityException e) {
						e.printStackTrace();
					}
					classMapLoaded = true;
				}
			});
			gamepackThread.start();	
			
			cacheIDThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try ( RandomAccessFile idFile = new RandomAccessFile(cacheIDFile, "rwd") ) {
						cacheID = idFile.readInt();
					} catch (IOException e) {
						cacheID = 0;
					}
				}
			});
			cacheIDThread.start();	
		}

		Map<String, Gamepack.Entry> get(int currentID) {
			// Wait for the cacheID thread to return a result.
			while (cacheIDThread != null && cacheIDThread.isAlive()) try {
				cacheIDThread.join();
				cacheIDThread = null;
			} catch (InterruptedException e) {}

			// Compare the cacheID of the local gamepack to that from the config.
			// If they're the same, wait for the gamepack to return its classes.
			if (cacheID == currentID) {
				while (gamepackThread != null && gamepackThread.isAlive()) try {
					gamepackThread.join();
					gamepackThread = null;
				} catch (InterruptedException e) {}

				classMap = map[0];
			}

			// Otherwise, close the thread and disregard its result.
			else {
				if (gamepackThread != null)
				gamepackThread.interrupt();
			}
		}
	}
}
