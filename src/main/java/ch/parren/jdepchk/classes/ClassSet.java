package ch.parren.jdepchk.classes;

import java.io.IOException;

import ch.parren.jdepchk.classes.asm.ClassReader;

public interface ClassSet {

	void accept(Visitor visitor) throws IOException;
	
	abstract class Visitor {
		public abstract boolean visitPackage(String packagePath) throws IOException;
		public abstract boolean visitClassFile(ClassBytes classFile) throws IOException;
		public abstract void visitClassReader(ClassReader classReader) throws IOException;
		public abstract void visitPackageEnd() throws IOException;
	}
	
}
