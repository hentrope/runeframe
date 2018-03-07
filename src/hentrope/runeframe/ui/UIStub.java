package hentrope.runeframe.ui;

import java.awt.Component;
import java.awt.Frame;

/**
 * Interface for objects that will listen for progress updates.
 * 
 * @author hentrope
 */
public class UIStub {
	private volatile RuneFrame frame;
	private volatile ProgressBar bar;
	private volatile int progress = 0;
	private volatile String message = "";
	
	public Frame getFrame() {
		if (frame == null) {
			frame = new RuneFrame();
		}
		return frame;
	}

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
	
	public void setComponent(Component c) {
		if (frame == null)
			throw new IllegalStateException("Frame not initialized.");
		else
			frame.setComponent(c);
	}
}
