package hentrope.runeframe.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import hentrope.runeframe.Preferences;

/**
 * Manages the window which the game client is embedded into.
 * <p>
 * While much of this class contains the code required to configure the
 * client's window, there are two other important features that it provides:
 * <ol>
 * <li>The {@link RuneFrame#init(Preferences, File)} method handles loading
 * and restoring the previous window state, as well as creating a shutdown
 * hook that will save the final window state upon exit.
 * <li>The {@link RuneFrame#toggleFullscreen()} handles switching the client
 * between windowed and fullscreen mode on the fly.
 * </ol>
 * 
 * @author hentrope
 */
public class RuneFrame implements ComponentListener, WindowStateListener {
	public static final Dimension MINIMUM_CLIENT_SIZE = new Dimension(765, 503);
	public static final int INBOUNDS_X = 32, INBOUNDS_Y = 16;

	private final Rectangle bounds = new Rectangle();
	private int state = Frame.NORMAL;
	private boolean fullscreen = false;

	private Frame frame;
	private volatile Component comp;

	/**
	 * Initializes an instance of RuneFrame, which is used to manage the
	 * client's window.
	 * <p>
	 * This method uses the provided Preferences to determine whether or not
	 * the state of the window should be saved between executions. If this is
	 * true, then the previous state will be loaded from a file, and a shutdown
	 * hook will be created that will save the final window state.
	 * 
	 * @param pref the Preferences used to determine if the state should be saved
	 * @param file the state file to be used when loading/saving the window's state
	 */
	public void init(Preferences pref, File file) {
		/*
		 * Create a Frame, and set some basic properties.
		 */
		frame = new Frame();
		frame.setLayout(new BorderLayout());
		frame.setTitle("Old School RuneScape - RuneFrame");
		frame.setBackground(Color.BLACK);
		frame.setResizable(true);
		frame.setUndecorated(false);


		if (pref.getBool(Preferences.Key.PRESERVE_WINDOW_STATE)) {
			/*
			 * Load the previous window state from the runeframe.state file.
			 */
			try ( RandomAccessFile raFile = new RandomAccessFile(file, "rwd") ) {
				bounds.x = raFile.readInt();
				bounds.y = raFile.readInt();
				bounds.width = raFile.readInt();
				bounds.height = raFile.readInt();
				state = raFile.readInt();

				frame.setBounds(bounds);
			} catch (IOException e) {
				frame.setLocationRelativeTo(null);
				frame.getBounds(bounds);
			}

			/*
			 * Add a shutdown hook that will save the state of the window when
			 * the client is closed.
			 */
			Runtime.getRuntime().addShutdownHook( new Thread() {
				public void run() {
					try ( RandomAccessFile raFile = new RandomAccessFile(file, "rwd") ) {
						raFile.writeInt(bounds.x);
						raFile.writeInt(bounds.y);
						raFile.writeInt(bounds.width);
						raFile.writeInt(bounds.height);
						raFile.writeInt(state);
					} catch (IOException e) {
						System.err.println("Unable to save window state.");
					}
				}
			} );
		} else {
			/*
			 * If state is not saved, simply put the window in the center of the screen.
			 */
			frame.setLocationRelativeTo(null);
			frame.getBounds(bounds);
			if (file.exists() && file.length() < 1024)
				file.delete();
		}

		/*
		 * Add listeners that will handle state change and closing events.
		 */
		frame.addComponentListener(this);
		frame.addWindowStateListener(this);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				close();
			}
		});
	}

	/**
	 * Sets the component contained by the window. Any component that had
	 * been previously added to the window will automatically be removed.
	 * 
	 * @param comp the component to be added to the window
	 */
	public void setComponent(Component comp) {
		if (this.comp != null)
			frame.remove(this.comp);
		this.comp = comp;
		frame.add(comp);
	}

	/**
	 * Gets the component currently contained by the window.
	 * 
	 * @return the component currently contained by the window
	 */
	public Component getComponent() {
		return comp;
	}

	/**
	 * Method to display the window after it has been initialized. After making
	 * it visible, it will attempt to move the window back in-bounds if it was
	 * initialized in a position that is out of bounds for the current screen.
	 * <p>
	 * This method is partially based on the code found in
	 * {@link sun.java2d.SunGraphicsEnvironment#getUsableBounds(GraphicsDevice).
	 * It was rewritten to allow it to get the usable bounds of a specific
	 * GraphicsConfiguration, instead of a GraphicsDevice. 
	 */
	public void show() {
		/*
		 * Sets the window to be visible, which is needed to calculate size and insets.
		 */
		frame.setVisible(true);

		/*
		 * Set the window's minimum size to fit the dimensions of the game.
		 */
		Insets insets = frame.getInsets();
		frame.setMinimumSize(new Dimension(
				MINIMUM_CLIENT_SIZE.width + insets.left + insets.right,
				MINIMUM_CLIENT_SIZE.height + insets.top + insets.bottom));

		/*
		 * Get the usable bounds of the window's current GraphicsConfiguration.
		 */
		GraphicsConfiguration config = frame.getGraphicsConfiguration();
		Rectangle usableBounds = config.getBounds();
		insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
		usableBounds.x += insets.left;
		usableBounds.y += insets.top;
		usableBounds.width -= (insets.left + insets.right);
		usableBounds.height -= (insets.top + insets.bottom);

		/*
		 * Get the height of the title bar, which needs to be visible upon startup.
		 */
		Rectangle windowBounds = frame.getBounds();
		windowBounds.height = frame.getInsets().top;

		/*
		 * Update the window's bounds to ensure the title bar is in view.
		 */
		windowBounds.x = Math.max(windowBounds.x, usableBounds.x - windowBounds.width + INBOUNDS_X);
		windowBounds.x = Math.min(windowBounds.x, usableBounds.x + usableBounds.width - INBOUNDS_X);

		windowBounds.y = Math.max(windowBounds.y, usableBounds.y - windowBounds.height + INBOUNDS_Y);
		windowBounds.y = Math.min(windowBounds.y, usableBounds.y + usableBounds.height - INBOUNDS_Y);

		/*
		 * Move the window to this updated bounds, and change its state to reflect
		 * whether it is maximized or not.
		 */
		frame.setLocation(windowBounds.x, windowBounds.y);
		frame.setExtendedState(state);
	}

	/**
	 * Toggles the client between fullscreen and windowed mode. If the current
	 * graphics configuration (screen) does not support fullscreen mode, this
	 * method will do nothing.
	 */
	public void toggleFullscreen() {
		GraphicsDevice device = frame.getGraphicsConfiguration().getDevice();

		if (!device.isFullScreenSupported())
			return;

		boolean newFullscreen = !fullscreen;

		frame.dispose();
		frame.setResizable(!newFullscreen);
		frame.setUndecorated(newFullscreen);

		device.setFullScreenWindow(newFullscreen ? frame : null);

		frame.setVisible(true);
		fullscreen = !fullscreen;

		if (!newFullscreen) {
			frame.setBounds(bounds);
			frame.setExtendedState(state);
		}
	}

	/**
	 * Method to display an exit confirmation prompt. If the user chooses to
	 * confirm, the application will exit.
	 */
	public void close() {
		if (ExitConfirmDialog.showConfirmDialog(frame,
				"Are you sure you want to quit?",
				"Exit Confirmation",
				null) == JOptionPane.YES_OPTION)
			System.exit(0);
	}

	@Override
	public void windowStateChanged(WindowEvent e) {
		state = e.getNewState() & ~JFrame.ICONIFIED;
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		Frame frame = (Frame) e.getComponent();
		if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0 && !fullscreen)
			frame.getBounds(bounds);
	}

	@Override
	public void componentResized(ComponentEvent e) {
		Frame frame = (Frame) e.getComponent();
		if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0 && !fullscreen)
			frame.getBounds(bounds);
	}

	@Override
	public void componentHidden(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}
}
