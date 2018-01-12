package hentrope.runeframe.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import javax.swing.Timer;

import hentrope.runeframe.util.ProgressListener;

/**
 * An AWT component that renders a progress bar identical to that which
 * OSRS uses.
 * <p>
 * Progress is updated using the {@link setProgress(int, String)} method,
 * and will automatically be redrawn afterwards.
 * 
 * @author hentrope
 */
public class ProgressBar extends Component implements ActionListener, ProgressListener {
	private final static Dimension size = new Dimension(303, 33);
	private final static Color color = new Color(140, 17, 17);
	private final static Font font = new Font("Helvetica", 1, 13);
	private final static int DELAY = 1000 / 30;

	private final Timer timer;
	private volatile boolean dirty = false;
	private volatile int progress = 0;
	private volatile String message = "";


	public ProgressBar() {
		super();
		setBackground(Color.BLACK);

		timer = new Timer(DELAY, this);
		timer.setCoalesce(true);
		timer.setRepeats(true);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		if (enabled && !timer.isRunning())
			timer.start();
		else if (!enabled && timer.isRunning())
			timer.stop();
	}

	@Override
	public final void setProgress(int progress, String message) {
		this.progress = progress;
		this.message = message;
		this.dirty = true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (dirty) {
			dirty = false;
			repaint();
		}
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		final int x = (getWidth() - size.width)/2 - 1;
		final int y = (getHeight() - size.height)/2 - 2; // 233;

		g.setColor(color);
		g.drawRect(x, y, size.width, size.height);
		if (progress > 0)
			g.fillRect(x+2, y+2, 3*progress, size.height-3);

		g.setColor(Color.WHITE);
		g.setFont(font);
		Rectangle2D textSize = g.getFontMetrics().getStringBounds(message, g);
		g.drawString(message, x + size.width/2 - (int)(textSize.getWidth()/2), y + 22);
	}
}
