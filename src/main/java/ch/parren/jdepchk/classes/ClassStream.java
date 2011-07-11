package ch.parren.jdepchk.classes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClassStream extends AbstractClassBytes {

	private static final int CLASS_EXT_LEN = ".class".length();

	private final File classFile;

	public ClassStream(String rootPath, File classFile) {
		super(extractNameFrom(rootPath, classFile));
		this.classFile = classFile;
	}

	private static String extractNameFrom(String rootPath, File classFile) {
		final String fileName = classFile.getPath();
		return fileName.substring(rootPath.length(), fileName.length() - CLASS_EXT_LEN);
	}

	@Override public InputStream inputStream() throws IOException {
		return new FileInputStream(classFile);
	}
	
}
