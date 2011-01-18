package ch.parren.jdepchk.classes;

import java.io.File;
import java.io.IOException;

public class StreamClassFileReader extends AbstractClassReader {

	private static final int CLASS_EXT_LEN = ".class".length();

	private final File classFile;

	public StreamClassFileReader(String rootPath, File classFile) {
		super(extractNameFrom(rootPath, classFile));
		this.classFile = classFile;
	}

	private static String extractNameFrom(String rootPath, File classFile) {
		final String fileName = classFile.getPath();
		return fileName.substring(rootPath.length(), fileName.length() - CLASS_EXT_LEN);
	}

	@Override protected ClassParser newClassBytesReader() throws IOException {
		return new ClassParser(classFile);
	}

	@Override public void close() throws IOException {}

}
