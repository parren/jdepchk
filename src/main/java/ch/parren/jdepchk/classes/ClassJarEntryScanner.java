package ch.parren.jdepchk.classes;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassJarEntryScanner extends AbstractClassScanner {

	private final JarFile file;
	private final JarEntry entry;
	private InputStream stream;

	public ClassJarEntryScanner(String name, JarFile file, JarEntry entry) {
		super(name);
		this.file = file;
		this.entry = entry;
	}

	@Override protected ClassParser newClassParser() throws IOException {
		stream = file.getInputStream(entry);
		return new AsmClassParser((int) entry.getSize(), stream);
	}

	public InputStream inputStream() throws IOException {
		return file.getInputStream(entry);
	}
	
	/* @Override */public void close() throws IOException {
		if (null != stream)
			stream.close();
		stream = null;
	}

}
