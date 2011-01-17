package ch.parren.jdepchk.classes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import ch.parren.java.lang.New;

public class DirectClassFileReader implements ClassFile {

	private static final int CLASS_EXT_LEN = ".class".length();

	private final String name;
	private final File classFile;
	private Set<String> refdNames = null;

	public DirectClassFileReader(String rootPath, File classFile) {
		final String fileName = classFile.getPath();
		this.name = fileName.substring(rootPath.length(), fileName.length() - CLASS_EXT_LEN);
		this.classFile = classFile;
	}

	@Override public String compiledClassName() {
		return name;
	}

	@Override public Iterable<String> referencedClassNames() throws IOException {
		if (null != refdNames)
			return refdNames;
		final Set<String> result = New.hashSet();

		final InputStream stream = new FileInputStream(classFile);
		try {
			final ClassBytesReader bytes = new ClassBytesReader(stream);
			assert name.equals(bytes.getClassName());
			add(bytes.getRefdClasses(), result);
			bytes.release();
		} finally {
			stream.close();
		}

		refdNames = result;
		return result;
	}

	private void add(String name, Set<String> result) {
		if (null != name && !this.name.equals(name))
			result.add(name);
	}

	private void add(String[] names, Set<String> result) {
		if (null == names)
			return;
		final int n = names.length;
		for (int i = 0; i < n; i++)
			add(names[i], result);
	}

	@Override public void close() throws IOException {}
	
	@Override public String toString() {
		return this.name;
	}
}
