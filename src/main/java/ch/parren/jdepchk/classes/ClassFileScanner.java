package ch.parren.jdepchk.classes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClassFileScanner extends AbstractClassScanner {

	private static final int CLASS_EXT_LEN = ".class".length();

	private final File classFile;

	public ClassFileScanner(String rootPath, String filePath, File classFile) {
		super(extractNameFrom(rootPath, filePath));
		this.classFile = classFile;
	}

	public ClassFileScanner(String className, File classFile) {
		super(className);
		this.classFile = classFile;
	}

	private static String extractNameFrom(String rootPath, String filePath) {
		return filePath.substring(rootPath.length(), filePath.length() - CLASS_EXT_LEN);
	}

	@Override protected ClassParser newClassParser() throws IOException {
		return new AsmClassParser(classFile);
	}

	public InputStream inputStream() throws IOException {
		return new FileInputStream(classFile);
	}
	
	/* @Override */public void close() throws IOException {}

}
