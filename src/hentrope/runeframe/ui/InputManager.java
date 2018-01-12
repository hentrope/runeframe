package hentrope.runeframe.ui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import hentrope.runeframe.screen.Screenshot;

import static java.awt.event.KeyEvent.*;

/**
 * An implementation of {@link KeyEventDispatcher} that hooks keyboard
 * inputs to the game in order to listen for fullscreen toggle and
 * screenshot inputs.
 * 
 * @author hentrope
 */
public class InputManager implements KeyEventDispatcher {
	/**
	 * Creates and registers a new InputManager that listen for keystrokes for
	 * toggling fullscreen mode and taking screenshots.
	 * 
	 * @param frame the window handler that is used to toggle fullscreen mode
	 * @param screenshot the screenshot handler that is used to
	 */
	public static void setupKeyEventDispatcher(RuneFrame frame, Screenshot screenshot) {
		if (frame != null || screenshot != null) {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
					new InputManager(frame, screenshot) );
		}
	}





	private final RuneFrame frame;
	private final Screenshot screenshot;

	private InputManager(RuneFrame frame, Screenshot screenshot) {
		this.frame = frame;
		this.screenshot = screenshot;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		int keyCode = e.getKeyCode();

		if (keyCode == VK_ENTER && e.isAltDown() && e.getID() == KEY_PRESSED && frame != null) {
			frame.toggleFullscreen();
			return true;
		}

		if (keyCode == VK_PRINTSCREEN && e.getID() == KEY_RELEASED && screenshot != null) {
			screenshot.takeScreenshot();
			return true;
		}

		return false;
	}
}
