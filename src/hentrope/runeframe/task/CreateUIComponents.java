package hentrope.runeframe.task;

import static hentrope.runeframe.Preferences.Key.*;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import hentrope.runeframe.screen.Screenshot;
import hentrope.runeframe.ui.GraphicsAcceleration;
import hentrope.runeframe.ui.InputManager;
import hentrope.runeframe.ui.ProgressBar;
import hentrope.runeframe.ui.RuneFrame;

public class CreateUIComponents implements Callable<RuneFrame> {
	private final Future<LoadClientConfig.Results> loadClientConfig;
	private final ProgressBar progressBar;

	public CreateUIComponents(Future<LoadClientConfig.Results> loadClientConfig, ProgressBar progressBar) {
		this.loadClientConfig = loadClientConfig;
		this.progressBar = progressBar;
	}

	@Override
	public RuneFrame call() throws Exception {
		/*
		 * Create the UI components, initializing them on the AWT event dispatch thread.
		 */
		RuneFrame ui = new RuneFrame();

		System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("sun.awt.erasebackgroundonresize", "false");

		LoadClientConfig.Results results = loadClientConfig.get();
		
		
		
		GraphicsAcceleration.set(results.pref.get(GRAPHICS_ACCELERATION));

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ui.init(results.pref, results.atlas.state);
				ui.setComponent(progressBar);
				ui.show();
				progressBar.setEnabled(true);

				InputManager.setupKeyEventDispatcher( results.pref.getBool(FULLSCREEN_ENABLED) ? ui : null,
						results.pref.getBool(SCREENSHOT_ENABLED) ? Screenshot.create(results.pref, results.atlas.screenDir, ui) : null);
			}
		});

		return ui;
	}
}
