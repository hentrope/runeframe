package hentrope.runeframe.task;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import hentrope.runeframe.JavConfig;
import hentrope.runeframe.io.InterceptInputStream;
import hentrope.runeframe.util.CertificateVerifier;
import hentrope.runeframe.util.ProgressListener;

public class LoadWebGamepack implements LoadGamepack {
	private final JavConfig config;
	private final boolean getRaw;
	private final ProgressListener listener;
	private final File certDir;

	public LoadWebGamepack(JavConfig config, boolean getRaw, ProgressListener listener, File certDir) {
		this.config = config;
		this.getRaw = getRaw;
		this.listener = listener;
		this.certDir = certDir;
	}

	@Override
	public LoadGamepack.Results call() throws Exception {
		CertificateVerifier verifier = new CertificateVerifier(certDir);

		URL url = new URL(config.get("codebase") + config.get("initial_jar"));
		URLConnection connection = url.openConnection();
		connection.addRequestProperty("accept-encoding", "pack200-gzip");

		try ( InputStream urlStream = connection.getInputStream() ) {
			InputStream stream = LoadGamepack.wrapStream(
					urlStream,
					connection.getContentLength(),
					connection.getContentEncoding(),
					listener);

			if (getRaw)
				stream = new InterceptInputStream(stream);

			return new LoadGamepack.Results(
					LoadGamepack.fromStream(stream, verifier),
					getRaw ? ((InterceptInputStream) stream).toByteArray() : null);
		}
	}

}