package ch.parren.jdepchk.classes;

import java.io.Closeable;
import java.io.IOException;

public interface ClassReader extends Closeable {
	String compiledClassName() throws IOException;
	Iterable<String> referencedClassNames() throws IOException;
}
