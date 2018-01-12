package hentrope.runeframe.io;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A variant of {@link FilterInputStream} that stores a copy of all data
 * that is read from the underlying InputStream.
 * <p>
 * Once the underlying stream has been read completely, a byte array of all
 * retrieved data can be obtained using the {@link InterceptInputStream#toByteArray()} method.
 * <p>
 * Based on {@link javax.swing.ProgressMonitorInputStream}.
 * 
 * @author hentrope
 */
public class InterceptInputStream extends FilterInputStream {
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();

	public InterceptInputStream(InputStream in) {
		super(in);
	}

	public byte[] toByteArray() {
		return out.toByteArray();
	}

	@Override
	public int read() throws IOException {
		int t = super.read();
		if (t > 0)
			out.write(t);
		return t;
	}

	@Override
	public int read(byte[] data, int offset, int length) throws IOException {
		int t = super.read(data, offset, length);
		if (t > 0)
			out.write(data, offset, t);
		return t;
	}

	@Override
	public long skip(long n) throws IOException {
		System.out.println("skip(" + n + ")");
		return super.skip(n);
	}
	

	@Override
	public boolean markSupported() {
		System.out.println("markSupported()");
		return false;
	}
	
	@Override
	public void mark(int readlimit) {
		System.out.println("mark(" + readlimit + ")");
		super.mark(readlimit);
	}

	@Override
	public void reset() throws IOException {
		throw new IOException("mark/reset not supported");
	}

}
