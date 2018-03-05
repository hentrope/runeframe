package hentrope.runeframe.util;

import java.awt.Component;

import hentrope.runeframe.ui.ProgressBar;

/**
 * Interface for objects that will listen for progress updates.
 * 
 * @author hentrope
 */
public class ProgressListener {
	private volatile ProgressBar bar;
	private volatile int progress = 0;
	private volatile String message = "";

	public Component getComponent() {
		if (bar == null) {
			bar = new ProgressBar();
			bar.setProgress(progress, message);
		}
		return bar;
	}

	/**
	 * Updates the listener on any progress that has been made.
	 * 
	 * @param progress an integer between 0 and 100 (inclusive) representing progress made
	 * @param message a message that describes the progress or task
	 */
	public void setProgress(int progress, String message) {
		if (bar == null) {
			this.progress = progress;
			this.message = message;
		} else
			bar.setProgress(progress, message);
	}
}
