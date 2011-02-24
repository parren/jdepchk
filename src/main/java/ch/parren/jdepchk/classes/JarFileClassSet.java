package ch.parren.jdepchk.classes;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarFileClassSet extends AbstractClassFilesSet<JarFile> {

	private final File file;
	private JarEntry currentEntry;

	public JarFileClassSet(File jarFile) {
		this.file = jarFile;
	}

	/* @Override */public void accept(Visitor visitor) throws IOException {
		final JarFile jarFile = new JarFile(file);
		try {
			final Enumeration<JarEntry> entries = jarFile.entries();
			accept(visitor, jarFile, new Iterator<String>() {
				/* @Override */public boolean hasNext() {
					return entries.hasMoreElements();
				}
				/* @Override */public String next() {
					return (currentEntry = entries.nextElement()).getName();
				}
				/* @Override */public void remove() {
					throw new UnsupportedOperationException();
				}
			});
		} finally {
			jarFile.close();
		}
	}

	@Override protected void visit(Visitor visitor, String className, JarFile jarFile) throws IOException {
		final ClassReader classFile = new ClassJarEntryReader(className, jarFile, currentEntry);
		try {
			visitor.visitClassFile(classFile);
		} finally {
			classFile.close();
		}
	}

}
