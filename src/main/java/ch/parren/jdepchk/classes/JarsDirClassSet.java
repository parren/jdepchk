package ch.parren.jdepchk.classes;

import java.io.File;
import java.io.IOException;

public final class JarsDirClassSet implements ClassSets {

	private final boolean recursive;
	private final File dir;

	public JarsDirClassSet(boolean recursive, File dir) {
		this.recursive = recursive;
		this.dir = dir;
	}

	/* @Override */public void accept(Visitor visitor) throws IOException {
		accept(visitor, dir);
	}

	private void accept(Visitor visitor, File dir) throws IOException {
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".jar"))
				visitor.visitClassSet(new JarFileClassSet(file));
			else if (recursive && file.isDirectory() && file.getName().charAt(0) != '.')
				accept(visitor, file);
		}

	}

}
