package ch.parren.jdepchk.classes;

import java.io.Closeable;
import java.io.IOException;

public interface ClassFile extends Closeable {
	String compiledClassName() throws IOException;
	Iterable<String> referencedClassNames() throws IOException;
}
