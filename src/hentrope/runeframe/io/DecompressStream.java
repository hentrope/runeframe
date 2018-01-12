package hentrope.runeframe.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPInputStream;

/**
 * Class containing the <code>getInputStream</code> method, which is used to
 * decompress an InputStream on the fly.
 * 
 * @author hentrope
 */
public class DecompressStream {
	/**
	 * Creates and returns an InputStream that will decompress the data within
	 * a given InputStream, based on its encoding.
	 * 
	 * If the InputStream is not compressed, then the input InputStream will be
	 * returned immediately. Otherwise, the InputStream will be wrapped in
	 * additional InputStreams in order to decompress the data as it arrives.
	 * This function supports streams encoded with gzip and/or pack200.
	 * 
	 * @param in InputStream which contents should be decompressed
	 * @param encoding Encoding of in
	 * @return A
	 * @throws IOException 
	 * @throws UnsupportedEncodingException If decompressing the stream is unsupported
	 */
	public static InputStream getInputStream(final InputStream in, final String encoding)
			throws IOException, UnsupportedEncodingException {
		// If the stream is not compressed, return the stream
		if (encoding == null || "".equals(encoding))
			return in;

		// If the stream is compressed with gzip, wrap it in a stream to decompress it.
		else if ("gzip".equals(encoding))
			return new GZIPInputStream(in);

		// If the stream is compressed with both pack200 and gzip, then a piped
		// stream will need to be created using the pack200 unpack function.
		else if ("pack200-gzip".equals(encoding)) {
			final GZIPInputStream gzip = new GZIPInputStream(in);
			final PipedInputStream pin = new PipedInputStream() {
				@Override
				public void close() throws IOException {
					super.close();
					gzip.close();
				}
			};
			final PipedOutputStream pout = new PipedOutputStream(pin);

			new Thread( new Runnable() {
				public void run() {
					try {
						try {
							Pack200.newUnpacker().unpack(gzip, new JarOutputStream(pout));
						} finally {
							pout.close();
						}
					} catch (IOException e) {
						if (!"Pipe closed".equals(e.getMessage()))
							e.printStackTrace();
					}
				}
			} ).start();

			return pin;
		}

		// If none of the above cases cover the encoding, throw an exception.
		else throw new UnsupportedEncodingException();
	}
}
