package ch.parren.jdepchk.classes;

import java.io.File;
import java.util.Iterator;

public abstract class ClassFileIterator implements Iterable<ClassFile> {

	@Override public final Iterator<ClassFile> iterator() {
		final String rootPath = classFilesRoot().getPath() + File.separator;
		final Iterator<File> files = classFiles().iterator();
		return new Iterator<ClassFile>() {
			@Override public boolean hasNext() {
				return files.hasNext();
			}
			@Override public ClassFile next() {
				return new DirectClassFileReader(rootPath, files.next());
			}
			@Override public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	abstract protected File classFilesRoot();
	abstract protected Iterable<File> classFiles();

}
