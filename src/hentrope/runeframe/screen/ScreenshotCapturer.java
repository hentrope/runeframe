package hentrope.runeframe.screen;

import java.applet.Applet;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.function.BiConsumer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import hentrope.runeframe.ui.RuneFrame;

/**
 * A producer that will capture the game's screen in a separate thread whenever
 * the {@link ScreenshotCapturer#takeScreenshot()} method is called.
 * 
 * @author hentrope
 */
public class ScreenshotCapturer implements Screenshot, Runnable {
	private final Rectangle bounds = new Rectangle();
	private final RuneFrame frame;
	private final Robot robot;
	private final BiConsumer<RenderedImage, Date> writer;
	private final boolean sound;

	private volatile boolean pending = false;
	private Clip clip = null;

	public ScreenshotCapturer(RuneFrame frame, BiConsumer<RenderedImage, Date> writer, boolean sound)
			throws AWTException {
		this.frame = frame;
		this.writer = writer;
		this.sound = sound;
		this.robot = new Robot();
	}

	@Override
	public void takeScreenshot() {
		pending = true;
		synchronized(this) {
			this.notify();
		}
	}

	@Override
	public void run() {
		while (true) {
			// Wait until there is a pending request for a screenshot
			while (!pending)
				try {
					synchronized (this) {
						this.wait();
					}
				} catch (InterruptedException e) {}
			pending = false;

			Component comp = frame.getComponent();
			if (comp instanceof Applet) {
				Applet applet = ((Applet)comp);

				if (applet.getComponentCount() > 0)
					comp = applet.getComponent(0);
			}

			comp.getBounds(bounds).setLocation(comp.getLocationOnScreen());
			writer.accept(robot.createScreenCapture(bounds), new Date());

			// If successful, play a sound
			if (sound)
				try {
					if (clip == null) {
						InputStream is = ScreenshotWriter.class.getClassLoader().getResourceAsStream("resource/shutter.wav");
						AudioInputStream stream = AudioSystem.getAudioInputStream(is);
						clip = AudioSystem.getClip();
						clip.open(stream);//AudioSystem.getAudioInputStream(is));
					}
					clip.setFramePosition(0);
					clip.start();
				} catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
					System.err.println("Unable to play screenshot audio.");
					e.printStackTrace();
				}
		}
	}
}
