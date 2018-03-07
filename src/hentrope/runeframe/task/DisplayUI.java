package hentrope.runeframe.task;

import static hentrope.runeframe.Preferences.Key.FULLSCREEN_ENABLED;
import static hentrope.runeframe.Preferences.Key.GRAPHICS_ACCELERATION;
import static hentrope.runeframe.Preferences.Key.SCREENSHOT_ENABLED;

import java.applet.Applet;
import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import hentrope.runeframe.Preferences;
import hentrope.runeframe.Runner;
import hentrope.runeframe.io.FileAtlas;
import hentrope.runeframe.screen.Screenshot;
import hentrope.runeframe.ui.GraphicsAcceleration;
import hentrope.runeframe.ui.InputManager;
import hentrope.runeframe.ui.RuneFrame;
import hentrope.runeframe.ui.UIStub;

public class DisplayUI implements Callable<Void> {
	private final Preferences pref;
	private final FileAtlas atlas;
	private final UIStub progress;
	private final Future<LoadGame.Results> loadGame;

	public DisplayUI(Preferences pref, FileAtlas atlas, UIStub progress, Future<LoadGame.Results> loadGame) {
		this.pref = pref;
		this.atlas = atlas;
		this.progress = progress;
		this.loadGame = loadGame;
	}

	@Override
	public Void call() throws Exception {
		/*
		 * Create the UI components, initializing them on the AWT event dispatch thread.
		 */
		RuneFrame frame = new RuneFrame();
		Component progressBar = progress.getComponent();

		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
				System.setProperty("sun.awt.noerasebackground", "true");
				System.setProperty("sun.awt.erasebackgroundonresize", "false");
				GraphicsAcceleration.set(pref.get(GRAPHICS_ACCELERATION));

				InputManager.setupKeyEventDispatcher( pref.getBool(FULLSCREEN_ENABLED) ? frame : null,
						pref.getBool(SCREENSHOT_ENABLED) ? Screenshot.create(pref, atlas.screenDir, frame) : null);

				frame.init(pref, atlas.state);
				frame.setComponent(progressBar);
				frame.setVisible(true);
				progressBar.setEnabled(true);
			}
		});

		//Runner.time("UI thread pausing");
		Applet applet = (Applet) loadGame.get().applet;
		//Runner.time("UI thread resuming");

		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				frame.setComponent(applet);
				applet.revalidate();
			}
		});

		return null;
	}
}
