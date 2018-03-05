package hentrope.runeframe.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class LoadCacheGamepack implements LoadGamepack {
	private final File file;
	
	public LoadCacheGamepack(File file) {
		this.file = file;
	}

	@Override
	public LoadGamepack.Results call() throws Exception {
		try ( InputStream fileInput = new FileInputStream(file) ) {
			InputStream stream = LoadGamepack.wrapStream(
					fileInput,
					(int)file.length(),
					null,
					null);
			return new LoadGamepack.Results(
					LoadGamepack.fromStream(stream, null),
					null);
		}
	}
	
}
