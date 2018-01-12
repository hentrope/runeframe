package hentrope.runeframe.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A variant of {@link FilterInputStream} that reports the total number of
 * bytes that have passed through the stream after each read is called. This
 * is used to update a progress bar as the gamepack is loaded.
 * <p>
 * Based on {@link javax.swing.ProgressMonitorInputStream}.
 * 
 * @author hentrope
 */
public abstract class ProgressInputStream extends FilterInputStream {
	private int read = 0;

	protected ProgressInputStream(InputStream in) {
		super(in);
	}

	protected abstract void update(int bytesRead);

	/*@Override
	public int read() throws IOException {
		int t = super.read();
		update(++read);
		return t;
	}*/

	@Override
	public int read(byte[] data) throws IOException {
		int t = super.read(data);
		if ( t > 0 ) {
			read += t;
			update(read);
		}
		return t;
	}

	@Override
	public int read(byte[] data, int offset, int length) throws IOException {
		int t = super.read(data, offset, length);
		if ( t > 0 ) {
			read += t;
			update(read);
		}
		return t;
	}

	@Override
	public long skip(long length) throws IOException {
		long t = super.skip(length);

		// 'read' may overflow here in rare situations.
		assert ( (long) read + t <= (long) Integer.MAX_VALUE );

		read += (int) t;
		update(read);

		return t;
	}
}
