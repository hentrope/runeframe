package hentrope.runeframe.screen;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;

/**
 * A consumer that will write any screenshot data to the disk in a separate thread.
 * 
 * @author hentrope
 */
public class ScreenshotWriter implements BiConsumer<RenderedImage, Date>, Runnable {
	public final static int SORT_NONE = 0, SORT_YEAR = 1, SORT_YEAR_MONTH = 2;

	private final BlockingQueue<Data> queue = new LinkedBlockingQueue<Data>();
	private final File directory;
	private final DateFormat pathFormat, nameFormat;

	public ScreenshotWriter(File directory, int sort) {
		this.directory = directory;
		
		if (sort == SORT_YEAR)
			pathFormat = new SimpleDateFormat("yyyy");
		else if (sort == SORT_YEAR_MONTH)
			pathFormat = new SimpleDateFormat("yyyy/MM MMMM");
		else
			pathFormat = new SimpleDateFormat("");
		
		nameFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
	}
	

	@Override
	public void accept(RenderedImage image, Date date) {
		try {
			queue.add(new Data(image, date));
		} catch (IllegalStateException e) {
			System.err.println("Unable to take screenshot: queue is full!");
		}
	}

	@Override
	public void run() {
		while (true) {
			Data data = null;
			while (data == null)
				try {
					data = queue.take();
				} catch (InterruptedException e) {}

			File path = new File(directory, pathFormat.format(data.date));
			path.mkdirs();
			
			String filename = nameFormat.format(data.date);
			File file = new File(path, filename + ".png");
			int count = 0;
			while (file.exists()) {
				count += 1;
				file = new File(path, filename + " (" + count + ").png");
			}

			// Attempt to save the screenshot
			try {
				ImageIO.write(data.image, "png", file);
			} catch (IOException | NullPointerException e) {
				System.err.println("Error writing screenshot to disk.");
				e.printStackTrace();
				continue;
			}
		}
	}
	
	public static class Data {
		final RenderedImage image;
		final Date date;

		Data(RenderedImage image, Date date) {
			this.image = image;
			this.date = date;
		}
	}
}
