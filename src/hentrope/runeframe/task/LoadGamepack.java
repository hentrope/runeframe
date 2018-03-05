package hentrope.runeframe.task;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import hentrope.runeframe.io.DecompressStream;
import hentrope.runeframe.io.ProgressInputStream;
import hentrope.runeframe.util.CertificateVerifier;
import hentrope.runeframe.util.ProgressListener;

public interface LoadGamepack extends Callable<LoadGamepack.Results> {
	public static final int BUFFER_SIZE = 4096, BYTESTREAM_SIZE = 65536;

	/**
	 * Prepares a gamepack InputStream by wrapping it in any InputStreams
	 * necessary to decompress the gamepack and track loading progress.
	 * <p>
	 * The gamepack InputStream will be decompressed using a set of InputStreams
	 * based on the encoding of the gamepack. These streams can be found in
	 * {@link DecompressStream}'s <code>getInputStream</code> method.
	 * <p>
	 * Additionally, the provided {@link ProgressListener} will be updated
	 * based on the progress tracked by a <code>ProgressInputStream</code>.
	 * The progress will reflect how much of the gamepack has been downloaded,
	 * as opposed to how much has been decompressed or verified.
	 * 
	 * @param stream InputStream to be wrapped
	 * @param length Length of the given gamepack InputStream, in bytes
	 * @param encoding Encoding of the given gamepack InputStream
	 * @param listener ProgressListener to be updated about download progress
	 * @return An InputStream that will output the decompressed gamepack
	 * @throws IOException If an exception was thrown while creating a stream.
	 * @see DecompressStream, ProgressListener
	 */
	public static InputStream wrapStream(InputStream stream, int length, String encoding, ProgressListener listener) throws IOException {
		if (listener == null)
			return DecompressStream.getInputStream(stream, encoding);
		
		return DecompressStream.getInputStream(
				new ProgressInputStream(stream) {
					@Override
					protected void update(int bytesRead) {
						int percent = 100 * bytesRead / length;
						listener.setProgress(percent, "Loading application - " + percent + "%");
					}
				}, encoding);
	}

	/**
	 * Loads a gamepack from the given InputStream, returning the gamepack data
	 * as a map from class names to class data.
	 * 
	 * @param stream stream from which to load the gamepack. Must not be compressed
	 * @param verifier CertificateVerifier instance used to verify the JAR's contents. If null, the gamepack will not be verified
	 * @return a map from a String class name to a byte[] class data
	 * @throws GeneralSecurityException if there is a GeneralSecurityException while attempting to validate the gamepack
	 * @throws IOException if there is an IOException while attempting to load the gamepack
	 * @throws SecurityException if there is a SecurityException while attempting to validate the gamepack
	 */
	public static Map<String, byte[]> fromStream(final InputStream stream, final CertificateVerifier verifier)
			throws GeneralSecurityException, IOException, SecurityException {
		try (	JarInputStream in = new JarInputStream(stream, verifier != null);
				ByteArrayOutputStream out = new ByteArrayOutputStream(BYTESTREAM_SIZE) ) {
			final byte[] buff = new byte[BUFFER_SIZE];
			final Map<String, byte[]> map = new HashMap<String, byte[]>();

			JarEntry entry = null;
			while ((entry = in.getNextJarEntry()) != null) {
				String name = entry.getName();
				if (entry.isDirectory() || isUnverifiable(name)) {
					while (in.read(buff, 0, buff.length) > 0);
				} else {
					int bytesRead;
					while ((bytesRead = in.read(buff, 0, buff.length)) > 0)
						out.write(buff, 0, bytesRead);

					if (verifier != null)
						verifier.verify(entry.getCertificates());

					if (name.endsWith(".class"))
						map.put(formatClassName(name), out.toByteArray());
					out.reset();
				}
			}

			while (stream.read(buff) > 0);

			return map;
		}
	}

	/**
	 * Determines whether a file with the given name can be verified.
	 * <p>
	 * Any files in the META-INF folder that end with .SF, .RSA, and .DSA are
	 * files containing keys for verifying code, and thus cannot be verified
	 * on their own.
	 * 
	 * @param name name of a file 
	 * @return whether the given file can be verified
	 */
	public static boolean isUnverifiable(String name) {
		return name.startsWith("META-INF/") && (
				name.endsWith(".SF") ||
				name.endsWith(".RSA") ||
				name.endsWith(".DSA") );
	}


	public static String formatClassName(String filename) {
		return filename.substring(0, filename.length()-6).replaceAll("/", ".");
	}

	public static class Results {
		public final Map<String, byte[]> classMap;
		public final byte[] raw;

		protected Results(Map<String, byte[]> classMap, byte[] raw) {
			this.classMap = classMap;
			this.raw = raw;
		}
	}
}
