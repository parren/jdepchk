package ch.parren.jdepchk.classes;

import java.io.IOException;

public interface ClassSets {

	void accept(Visitor visitor) throws IOException;
	
	interface Visitor {
		void visitClassSet(ClassSet classSet) throws IOException;
	}
	
}
