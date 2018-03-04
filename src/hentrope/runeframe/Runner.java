package hentrope.runeframe;

import static hentrope.runeframe.Arguments.Key.*;
import static hentrope.runeframe.Preferences.Key.*;

import java.applet.Applet;
import java.io.*;
import java.security.GeneralSecurityException;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import hentrope.runeframe.client.Client;
import hentrope.runeframe.client.ClientConfig;
import hentrope.runeframe.io.FileAtlas;
import hentrope.runeframe.screen.Screenshot;
import hentrope.runeframe.ui.GraphicsAcceleration;
import hentrope.runeframe.ui.InputManager;
import hentrope.runeframe.ui.ProgressBar;
import hentrope.runeframe.ui.RuneFrame;
import hentrope.runeframe.util.ProgressListener;

/**
 * Runner class for the RuneFrame client, which contains the client's main method.
 * 
 * @author hentrope
 */
public class Runner {
	public final static int TASK_DELAY = 1000;

	/**
	 * Entry method for the RuneFrame client. See inline comments for details.
	 * 
	 * @param arguments command line arguments provided to the client
	 * @throws Exception if there is an unhandled exception during execution
	 */
	public static void main(String[] arguments) throws Exception {
		long start = System.nanoTime(); // Debug - output starting time

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
		 * Create the UI components, initializing them on the AWT event dispatch thread.
		 */
		RuneFrame ui = new RuneFrame();
		ProgressBar progressBar = new ProgressBar();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
				System.setProperty("sun.awt.noerasebackground", "true");
				System.setProperty("sun.awt.erasebackgroundonresize", "false");
				GraphicsAcceleration.set(pref.get(GRAPHICS_ACCELERATION));
				
				ui.init(pref, atlas.state);
				ui.setComponent(progressBar);
				ui.show();
				progressBar.setEnabled(true);

				InputManager.setupKeyEventDispatcher( pref.getBool(FULLSCREEN_ENABLED) ? ui : null,
						pref.getBool(SCREENSHOT_ENABLED) ? Screenshot.create(pref, atlas.screenDir, ui) : null);
			}
		});

		/*
		 * Attempt to load the game client 3 times, terminating the process if
		 * there are more than 3 non-IO exceptions thrown.
		 */
		int counter = 3;
		Client game = null;
		Applet applet = null;
		while (game == null) {
			try {
				game = Client.loadAll(pref, atlas, progressBar);
				applet = game.applet;
			} catch (IOException e) {
				retry(progressBar, "Connection error.", 15);
			} catch (Exception e) {
				counter--;
				if (counter > 0) {
					if (e instanceof SecurityException || e instanceof GeneralSecurityException)
						retry(progressBar, "Invalid gamepack.", 10);
					else if (e instanceof ReflectiveOperationException)
						retry(progressBar, "Classload error.", 10);
					else throw e;
				} else throw e;
			}
		}

		/*
		 * Add the applet to the frame on the AWT event dispatch thread.
		 */
		final Applet finalApplet = applet;
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				ui.setComponent(finalApplet);
				finalApplet.revalidate();
			}
		});
		
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


		System.out.println((System.nanoTime() - start) / 1000000); // Debug - output loading time


		/*
		 * Lower the current thread's priority and wait a set amount of time
		 * for the game to finish loading.
		 * 
		 * After waiting for a period of time, low-priority tasks will be
		 * performed before terminating the thread.
		 */
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
		try {
			Thread.sleep(TASK_DELAY);
		} catch (InterruptedException e) {}

		/*
		 * If a preferences file does not exist, create it with the default values.
		 */
		if (!atlas.preferences.exists())
			try {
				pref.saveDefault(atlas.preferences);
			} catch (IOException e) {
				// Ignore IOExceptions
			} catch (Exception e) {
				System.err.println("Unable to save default preferences to disk.");
				e.printStackTrace();
			}

		/*
		 * If the gamepack was downloaded remotely but the preference is set to
		 * cache it, save the gamepack to disk using the intercept stream.
		 */
		if (game.intercept != null) {
			try (	OutputStream out = new FileOutputStream(atlas.cacheJar);
					RandomAccessFile idFile = new RandomAccessFile(atlas.cacheID, "rwd") ) {
				byte[] data = game.intercept.toByteArray();
				out.write(data, 0, data.length);
				idFile.writeInt(Integer.parseInt(game.config.get(ClientConfig.Key.DOWNLOAD)));
			} catch (Exception e) {
				System.err.println("Unable to save gamepack cache to disk.");
				e.printStackTrace();
			}
		}
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
