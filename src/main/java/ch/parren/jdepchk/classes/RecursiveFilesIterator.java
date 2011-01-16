package ch.parren.jdepchk.classes;

import java.io.File;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import ch.parren.java.lang.New;

/** Iterates the files and directories in a directory and its subdirectories. */
final class RecursiveFilesIterator implements Iterator<File> {

	private static final File[] NO_FILES = new File[0];

	private final Deque<File> pending = New.arrayDeque();

	private File[] files = NO_FILES;
	private int fileIdx = 0;
	private File next = null;

	public RecursiveFilesIterator(File path) {
		if (path.isDirectory()) {
			pending.addLast(path);
			nextDir();
		} else {
			next = path;
		}
	}

	private boolean nextDir() {
		File dir;
		while ((dir = pending.pollLast()) != null) {
			files = dir.listFiles();
			fileIdx = 0;
			if (nextInCurrentDir())
				return true;
		}
		return false;
	}

	private boolean nextInCurrentDir() {
		while (fileIdx < files.length) {
			final File file = files[fileIdx++];
			if (file.isDirectory()) {
				pending.addLast(file);
			} else {
				next = file;
				return true;
			}
		}
		return false;
	}
	
	@Override public boolean hasNext() {
		return (null != next);
	}

	@Override public File next() {
		if (null == next)
			throw new NoSuchElementException();
		final File file = next;
		if (!nextInCurrentDir() && !nextDir())
			next = null;
		return file;
	}

	@Override public void remove() {
		throw new UnsupportedOperationException();
	}

}