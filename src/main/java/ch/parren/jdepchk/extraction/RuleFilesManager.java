package ch.parren.jdepchk.extraction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import ch.parren.java.lang.New;

public final class RuleFilesManager {

	public final DirManager internal;
	public final DirManager external;
	private final DirManager[] all;

	public RuleFilesManager(File internalDir, File externalDir, boolean isConcurrent, boolean isFullScan) {
		internal = new DirManager(internalDir, isConcurrent, isFullScan);
		external = new DirManager(externalDir, isConcurrent, isFullScan);
		all = new DirManager[] { internal, external };
	}

	public void scanning(String fileName) {
		fileName = fileName.replace('/', '.');
		internal.scanning(fileName);
		external.scanning(fileName);
	}

	public boolean finish() {
		boolean changed = false;
		for (DirManager d : all)
			if (d.finish())
				changed = true;
		return changed;
	}

	public static final class DirManager {

		private final File dir;
		private final Set<String> pending;
		private volatile boolean changed = false;

		private DirManager(File dir, boolean isConcurrent, boolean isFullScan) {
			this.dir = dir;
			final Set<String> s = New.hashSet();
			if (isFullScan && dir.isDirectory()) {
				FilenameFilter filter = new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return !name.startsWith(".");
					}
				};
				for (String fileName : dir.list(filter))
					s.add(fileName);
			}
			this.pending = isConcurrent ? Collections.synchronizedSet(s) : s;
		}

		public void update(String fileName, String newText) throws IOException {
			dir.mkdirs();
			final File file = new File(dir, fileName);
			final byte[] newContents = newText.getBytes();
			if (pending.remove(fileName)) {
				final byte[] oldContents = readFile(file);
				if (Arrays.equals(oldContents, newContents))
					return;
			}
			writeFile(file, newContents);
			changed = true;
		}

		public void scanning(String fileName) {
			if (new File(dir, fileName).exists())
				pending.add(fileName);
		}

		private boolean finish() {
			for (String fileName : pending)
				new File(dir, fileName).delete();
			return changed || !pending.isEmpty();
		}

	}

	private static byte[] readFile(File file) throws IOException {
		final byte[] res = new byte[(int) file.length()];
		final FileInputStream fs = new FileInputStream(file);
		try {
			if (fs.read(res, 0, res.length) < res.length)
				throw new IOException("failed to read entire file");
		} finally {
			fs.close();
		}
		return res;
	}

	private static void writeFile(File file, byte[] data) throws IOException {
		final FileOutputStream fs = new FileOutputStream(file);
		try {
			fs.write(data, 0, data.length);
		} finally {
			fs.close();
		}
	}

}
