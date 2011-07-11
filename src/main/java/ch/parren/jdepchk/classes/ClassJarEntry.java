package ch.parren.jdepchk.classes;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassJarEntry extends AbstractClassBytes {

	private final JarFile file;
	private final JarEntry entry;

	public ClassJarEntry(String name, JarFile file, JarEntry entry) {
		super(name);
		this.file = file;
		this.entry = entry;
	}

	@Override public InputStream inputStream() throws IOException {
		return file.getInputStream(entry);
	}
	
}
