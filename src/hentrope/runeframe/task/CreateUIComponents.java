package hentrope.runeframe.task;

import static hentrope.runeframe.Preferences.Key.*;

import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import hentrope.runeframe.screen.Screenshot;
import hentrope.runeframe.ui.GraphicsAcceleration;
import hentrope.runeframe.ui.InputManager;
import hentrope.runeframe.ui.RuneFrame;
import hentrope.runeframe.util.ProgressListener;

public class CreateUIComponents implements Callable<RuneFrame> {
	private final Future<LoadClientConfig.Results> loadClientConfig;
	private final ProgressListener progress;

	public CreateUIComponents(Future<LoadClientConfig.Results> loadClientConfig, ProgressListener progress) {
		this.loadClientConfig = loadClientConfig;
		this.progress = progress;
	}

	@Override
	public RuneFrame call() throws Exception {
		long start;
		start = System.nanoTime();
		/*
		 * Create the UI components, initializing them on the AWT event dispatch thread.
		 */
		RuneFrame frame = new RuneFrame();
		Component progressBar = progress.getComponent();

		System.setProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName());
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("sun.awt.erasebackgroundonresize", "false");
		
		System.out.println("UI Starting:" + ((System.nanoTime() - start) / 1000000));

		LoadClientConfig.Results config = loadClientConfig.get();

		GraphicsAcceleration.set(config.pref.get(GRAPHICS_ACCELERATION));

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				System.out.println("UI Starting:" + ((System.nanoTime() - start) / 1000000));
				frame.init(config.pref, config.atlas.state);
				frame.setComponent(progressBar);
				frame.show();
				progressBar.setEnabled(true);
			}
		});

		InputManager.setupKeyEventDispatcher( config.pref.getBool(FULLSCREEN_ENABLED) ? frame : null,
				config.pref.getBool(SCREENSHOT_ENABLED) ? Screenshot.create(config.pref, config.atlas.screenDir, frame) : null);

		return frame;
	}
}
