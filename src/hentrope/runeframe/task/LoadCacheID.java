package hentrope.runeframe.task;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;

public class LoadCacheID implements Callable<Integer> {
	private final File file;
	
	public LoadCacheID(File file) {
		this.file = file;
	}

	@Override
	public Integer call() throws Exception {
		try ( RandomAccessFile idFile = new RandomAccessFile(file, "rwd") ) {
			return idFile.readInt();
		} catch (IOException e) {
			return 0;
		}
	}
}