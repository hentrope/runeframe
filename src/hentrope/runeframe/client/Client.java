package hentrope.runeframe.client;

import static hentrope.runeframe.Preferences.Key.*;

import java.applet.Applet;
import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;

import hentrope.runeframe.Preferences;
import hentrope.runeframe.Runner;
import hentrope.runeframe.io.DecompressStream;
import hentrope.runeframe.io.FileAtlas;
import hentrope.runeframe.io.InterceptInputStream;
import hentrope.runeframe.io.ProgressInputStream;
import hentrope.runeframe.ui.UIStub;
import hentrope.runeframe.util.*;

/**
 * Implements the process used to load the game, including loading the client
 * configuration and gamepack, and creating the ClassLoader.
 * 
 * @author hentrope
 * @see Client#loadAll(Preferences, FileAtlas, UIStub)
 */
public class Client {
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
	 * @return a Client instance that contains public references to the loaded data
	 * @throws IOException if there is an IOException while attempting to load the gamepack
	 * @throws GeneralSecurityException if there is a GeneralSecurityException while attempting to validate the gamepack
	 * @throws ReflectiveOperationException if the ClassLoader is unable to load the client classes
	 * @throws SecurityException if there is a SecurityException while attempting to validate the gamepack
	 * @see ClientConfig
	 * @see ClientGamepack
	 * @see ClientClassLoader
	 * @see JSObjectClassLoader
	 */
	public static Client loadAll(Preferences pref, FileAtlas atlas, UIStub listener)
			throws IOException, GeneralSecurityException, ReflectiveOperationException, SecurityException {
		
		listener.setProgress(0, "Loading config");

		/*
		 * Create a thread
		 */

		listener.setProgress(0, "Loading application");

		/*
		 * If the preferences state that the gamepack should be loaded from
		 * cache, first attempt to load it locally. This process will also
		 * compare the gamepack's ID to make sure that it is not out of date.
		 * 
		 * Since this code will have already been verified when it was
		 * downloaded, there's no need to verify it again.
		 */

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

	


	/**
	 * Data obtained from loading jav_config.ws.
	 */
	public final ClientConfig config;
	
	/**
	 * Metadata from the loaded gamepack, as well as the file data itself.
	 */
	public final ClientGamepack gamepack;
	
	/**
	 * An InterceptInputStream containing the raw data unpacked from a downloaded JAR.
	 */
	public final InterceptInputStream intercept;
	
	/**
	 * The Applet instance created by loading the client's main class.
	 */
	public final Applet applet;

	private Client(ClientConfig config, ClientGamepack gamepack, InterceptInputStream intercept, Applet applet) {
		this.config = config;
		this.gamepack = gamepack;
		this.intercept = intercept;
		this.applet = applet;
	}
}
