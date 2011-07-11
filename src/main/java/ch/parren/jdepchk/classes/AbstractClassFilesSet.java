package ch.parren.jdepchk.classes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import ch.parren.jdepchk.classes.asm.ClassReader;

public abstract class AbstractClassFilesSet<C> implements ClassSet {

	private static final int CLASS_EXT_LEN = ".class".length();

	protected final void accept(Visitor visitor, C context, Iterator<String> fileNames) throws IOException {
		String currentDir = null;
		boolean steppingIntoDir = true;
		while (fileNames.hasNext()) {
			final String name = fileNames.next();
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

				if (steppingIntoDir)
					visit(visitor, className, context);
			}
		}
		if (null != currentDir)
			visitor.visitPackageEnd();
	}

	protected abstract void visit(Visitor visitor, String className, C context) throws IOException;

	protected void acceptClassBytes(Visitor visitor, ClassBytes classBytes) throws IOException {
		if (visitor.visitClassFile(classBytes)) {
			final InputStream stream = classBytes.inputStream();
			try {
				visitor.visitClassReader(new ClassReader(stream));
			} finally {
				stream.close();
			}
		}
	}
}
