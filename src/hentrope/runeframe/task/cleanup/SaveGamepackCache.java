package hentrope.runeframe.task.cleanup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;

public class SaveGamepackCache implements Callable<Void> {
	private final File idFile, cacheFile;
	private final byte[] raw;
	private final int downloadID;
	
	public SaveGamepackCache(File idFile, File cacheFile, byte[] raw, int downloadID) {
		this.idFile = idFile;
		this.cacheFile = cacheFile;
		this.raw = raw;
		this.downloadID = downloadID;
	}

	@Override
	public Void call() throws Exception {
		try (	OutputStream out = new FileOutputStream(cacheFile);
				RandomAccessFile idOut = new RandomAccessFile(idFile, "rwd") ) {
			out.write(raw, 0, raw.length);
			idOut.writeInt(downloadID);
		}
		
		return null;
	}

}
