package hentrope.runeframe.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import hentrope.runeframe.util.CertificateVerifier;

/**
 * Implements the process used to load a gamepack from an InputStream.
 * 
 * @author hentrope
 * @see ClientGamepack#fromStream(InputStream, CertificateVerifier)
 */
public class ClientGamepack implements Iterable<ClientGamepack.Entry> {
	public static final int BUFFER_SIZE = 4096, BYTESTREAM_SIZE = 65536;

	/**
	 * Loads a gamepack from the given InputStream, returning the gamepack data
	 * in a new ClientGamepack instance.
	 * 
	 * @param stream stream from which to load the gamepack. Must not be compressed
	 * @param verifier CertificateVerifier instance used to verify the JAR's contents. If null, the gamepack will not be verified
	 * @return an instance of ClientGamepack
	 * @throws GeneralSecurityException if there is a GeneralSecurityException while attempting to validate the gamepack
	 * @throws IOException if there is an IOException while attempting to load the gamepack
	 * @throws SecurityException if there is a SecurityException while attempting to validate the gamepack
	 */
	public static ClientGamepack fromStream(final InputStream stream, final CertificateVerifier verifier)
			throws GeneralSecurityException, IOException, SecurityException {
		try (	JarInputStream in = new JarInputStream(stream, verifier != null);
				ByteArrayOutputStream out = new ByteArrayOutputStream(BYTESTREAM_SIZE) ) {
			byte[] buff = new byte[BUFFER_SIZE];
			ClientGamepack gamepack = new ClientGamepack(in.getManifest());

			JarEntry entry = null;
			while ((entry = in.getNextJarEntry()) != null) {
				if (entry.isDirectory() || isUnverifiable(entry.getName())) {
					while (in.read(buff, 0, buff.length) > 0);
				} else {
					int bytesRead;
					while ((bytesRead = in.read(buff, 0, buff.length)) > 0)
						out.write(buff, 0, bytesRead);
					
					if (verifier != null)
						verifier.verify(entry.getCertificates());

					gamepack.fileList.add(new ClientGamepack.Entry(new JarEntry(entry), out.toByteArray()));
					out.reset();
				}
			}

			while (stream.read(buff) > 0);

			return gamepack;
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



	private final Manifest manifest;
	private final List<ClientGamepack.Entry> fileList = new ArrayList<ClientGamepack.Entry>();

	private ClientGamepack(Manifest manifest) {
		this.manifest = manifest;
	}

	public Manifest getManifest() {
		return manifest;
	}

	@Override
	public Iterator<Entry> iterator() {
		return fileList.iterator();
	}

	/**
	 * Prints the names of all files in the gamepack.
	 * 
	 * Used to debug.
	 */
	public void print() {
		for (ClientGamepack.Entry file: fileList)
			System.out.println(file.entry.getName());
	}

	public static class Entry {
		public final JarEntry entry;
		public final byte[] data;

		Entry(JarEntry entry, byte[] data) {
			this.entry = entry;
			this.data = data;
		}
	}
}
