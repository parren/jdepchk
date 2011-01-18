package ch.parren.jdepchk.classes;

import java.io.IOException;

public interface ClassSet {

	void accept(Visitor visitor) throws IOException;
	
	interface Visitor {
		boolean visitPackage(String packagePath) throws IOException;
		void visitClassFile(ClassReader classFile) throws IOException;
		void visitPackageEnd() throws IOException;
	}
	
}
