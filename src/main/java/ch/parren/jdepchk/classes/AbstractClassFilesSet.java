package ch.parren.jdepchk.classes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public abstract class AbstractClassFilesSet<C> implements ClassSet {

	public static int approximateNumberOfClassesParsed = 0;
	
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
			approximateNumberOfClassesParsed++;
			final InputStream stream = classBytes.inputStream();
			try {
				visitor.visitClassBytes(readClass(stream));
			} finally {
				stream.close();
			}
		}
	}

    /**
     * Reads the bytecode of a class.
     * 
     * @param is an input stream from which to read the class.
     * @return the bytecode read from the given input stream.
     * @throws IOException if a problem occurs during reading.
     */
    public static byte[] readClass(final InputStream is) throws IOException {
        if (is == null) {
            throw new IOException("Class not found");
        }
        byte[] b = new byte[is.available()];
        int len = 0;
        while (true) {
            int n = is.read(b, len, b.length - len);
            if (n == -1) {
                if (len < b.length) {
                    byte[] c = new byte[len];
                    System.arraycopy(b, 0, c, 0, len);
                    b = c;
                }
                return b;
            }
            len += n;
            if (len == b.length) {
                int last = is.read();
                if (last < 0) {
                    return b;
                }
                byte[] c = new byte[b.length + 1000];
                System.arraycopy(b, 0, c, 0, len);
                c[len++] = (byte) last;
                b = c;
            }
        }
    }

}
