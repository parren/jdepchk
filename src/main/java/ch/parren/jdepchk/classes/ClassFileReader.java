package ch.parren.jdepchk.classes;

import java.io.File;
import java.io.IOException;

public class ClassFileReader extends AbstractClassReader {

	private static final int CLASS_EXT_LEN = ".class".length();

	private final File classFile;

	public ClassFileReader(String rootPath, String filePath, File classFile) {
		super(extractNameFrom(rootPath, filePath));
		this.classFile = classFile;
	}

	public ClassFileReader(String className, File classFile) {
		super(className);
		this.classFile = classFile;
	}

	private static String extractNameFrom(String rootPath, String filePath) {
		return filePath.substring(rootPath.length(), filePath.length() - CLASS_EXT_LEN);
	}

	@Override protected ClassParser newClassParser() throws IOException {
		return new ClassParser(classFile);
	}

	/* @Override */public void close() throws IOException {}

}
