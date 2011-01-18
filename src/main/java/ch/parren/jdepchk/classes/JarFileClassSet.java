package ch.parren.jdepchk.classes;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarFileClassSet implements ClassSet {

	private static final int CLASS_EXT_LEN = ".class".length();

	private final File file;

	public JarFileClassSet(File jarFile) {
		this.file = jarFile;
	}

	@Override public void accept(Visitor visitor) throws IOException {
		final JarFile jarFile = new JarFile(file);
		try {
			String currentDir = null;
			boolean steppingIntoDir = true;
			final Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				final JarEntry entry = entries.nextElement();
				final String name = entry.getName();
				if (name.endsWith(".class")) {
					final String className = name.substring(0, name.length() - CLASS_EXT_LEN);

					final int posOfName = className.lastIndexOf('/') + 1;
					final String newDir = (posOfName == 0) ? "" : className.substring(0, posOfName - 1);
					if (!newDir.equals(currentDir)) {
						if (null != currentDir)
							visitor.visitPackageEnd();
						steppingIntoDir = visitor.visitPackage(newDir);
						if (steppingIntoDir)
							currentDir = newDir;
						else
							currentDir = null;
					}

					if (steppingIntoDir) {
						final ClassReader classFile = new ClassJarEntryReader(className, jarFile, entry);
						try {
							visitor.visitClassFile(classFile);
						} finally {
							classFile.close();
						}
					}
				}
			}
			if (null != currentDir)
				visitor.visitPackageEnd();
		} finally {
			jarFile.close();
		}
	}

}
