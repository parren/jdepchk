package ch.parren.jdepchk.extraction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class RuleFilesManager {

	public final DirManager internal;
	public final DirManager external;
	private final DirManager[] all;

	public RuleFilesManager(File internalDir, File externalDir, String filePrefix, boolean isConcurrent) {
		internal = new DirManager(internalDir, filePrefix, isConcurrent);
		external = new DirManager(externalDir, filePrefix, isConcurrent);
		all = new DirManager[] { internal, external };
	}

	public boolean finish() {
		boolean changed = false;
		for (DirManager d : all)
			if (d.finish())
				changed = true;
		return changed;
	}

	public static final class DirManager {

		private final Set<String> pending;
		private volatile boolean changed = false;
		private final File dir;
		private final String filePrefix;

		private DirManager(File dir, String filePrefix, boolean isConcurrent) {
			this.dir = dir;
			this.filePrefix = filePrefix;
			final Set<String> s = scanDir(dir, filePrefix);
			this.pending = isConcurrent ? Collections.synchronizedSet(s) : s;
		}

		private static HashSet<String> scanDir(File dir, String filePrefix) {
			final HashSet<String> names = new HashSet<String>();
			if (dir.isDirectory())
				for (String fileName : dir.list())
					if (fileName.startsWith(filePrefix))
						names.add(fileName);
			return names;
		}

		public void update(String fileName, String newText) throws IOException {
			if (!fileName.startsWith(filePrefix))
				throw new IllegalArgumentException(fileName);
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

		private boolean finish() {
			boolean deleted = false;
			for (String fileName : pending) {
				new File(dir, fileName).delete();
				deleted = true;
			}
			return deleted || changed;
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
