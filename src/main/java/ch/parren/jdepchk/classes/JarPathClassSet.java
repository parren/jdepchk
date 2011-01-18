package ch.parren.jdepchk.classes;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import ch.parren.java.lang.New;

public final class JarPathClassSet implements ClassSet {

	private final boolean recursive;
	private final Collection<File> dirs;

	public JarPathClassSet(boolean recursive, Collection<File> dirs) {
		this.recursive = recursive;
		this.dirs = New.arrayList(dirs);
	}

	public JarPathClassSet(boolean recursive, File... dirs) {
		// Blame javac for this kludge - Eclipse handles inference just fine!
		this(recursive, New.<File, File>arrayList(dirs));
	}

	@Override public void accept(Visitor visitor) throws IOException {
		for (File dir : dirs)
			accept(visitor, dir);
	}

	private void accept(Visitor visitor, File dir) throws IOException {
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".jar"))
				new JarFileClassSet(file).accept(visitor);
			else if (recursive && file.isDirectory() && file.getName().charAt(0) != '.')
				accept(visitor, file);
		}

	}

}
