package hentrope.runeframe.util;

/**
 * Interface for objects that will listen for progress updates.
 * 
 * @author hentrope
 */
public interface ProgressListener {
	/**
	 * Updates the listener on any progress that has been made.
	 * 
	 * @param progress an integer between 0 and 100 (inclusive) representing progress made
	 * @param message a message that describes the progress or task
	 */
	void setProgress(int progress, String message);
}
