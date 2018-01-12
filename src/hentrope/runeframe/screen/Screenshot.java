package hentrope.runeframe.screen;

import static hentrope.runeframe.Preferences.Key.*;

import java.awt.AWTException;
import java.io.File;

import hentrope.runeframe.Preferences;
import hentrope.runeframe.ui.RuneFrame;

/**
 * The interface for the screenshotting system, which is used to create
 * an object that can be later called to take screenshots of the game.
 * <p>
 * The screenshotting system takes place in two stages, each of which is
 * handled on a separate thread:
 * <ol>
 * <li>A ScreenshotCapturer captures the game's screen using a Robot, and saves
 * data alongside the current time.
 * <li>The image and time are sent to a ScreenshotWriter, which keeps all
 * screenshots in a queue as it writes them to the disk.
 * </ol>
 * 
 * @author hentrope
 */
public interface Screenshot {
	public static Screenshot create(Preferences pref, File directory, RuneFrame frame) {
		try {
			ScreenshotWriter writer = new ScreenshotWriter(directory, ScreenshotWriter.SORT_YEAR_MONTH);
			ScreenshotCapturer capturer = new ScreenshotCapturer(frame, writer, pref.getBool(SCREENSHOT_SOUND));

			new Thread(capturer).start();
			new Thread(writer).start();

			return capturer;
		} catch (AWTException e) {
			System.err.println("Unable to initialize screenshop capture utility.");
			throw new RuntimeException(e);
		}
	}

	void takeScreenshot();
}
