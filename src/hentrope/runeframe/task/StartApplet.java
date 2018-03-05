package hentrope.runeframe.task;

import java.applet.Applet;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

import hentrope.runeframe.ui.RuneFrame;

public class StartApplet implements Callable<Void> {
	private final Applet applet;
	private final RuneFrame frame;
	
	public StartApplet(Applet applet, RuneFrame frame) {
		this.applet = applet;
		this.frame = frame;
	}

	@Override
	public Void call() throws Exception {
		/*
		 * Add the applet to the frame on the AWT event dispatch thread.
		 */
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				frame.setComponent(applet);
				applet.revalidate();
			}
		});

		/*
		 * Only after the applet is added to the frame may it be initialized and started.
		 */
		applet.init();
		applet.start();
		
		return null;
	}

}
