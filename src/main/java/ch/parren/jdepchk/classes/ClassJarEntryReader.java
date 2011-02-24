package ch.parren.jdepchk.classes;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassJarEntryReader extends AbstractClassReader {

	private final JarFile file;
	private final JarEntry entry;
	private InputStream stream;

	public ClassJarEntryReader(String name, JarFile file, JarEntry entry) {
		super(name);
		this.file = file;
		this.entry = entry;
	}

	@Override protected ClassParser newClassParser() throws IOException {
		stream = file.getInputStream(entry);
		return new ClassParser((int) entry.getSize(), stream);
	}

	/* @Override */public void close() throws IOException {
		if (null != stream)
			stream.close();
		stream = null;
	}

}
