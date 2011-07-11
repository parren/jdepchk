package ch.parren.jdepchk.classes;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface ClassScanner extends Closeable {
	String compiledClassName() throws IOException;
	Iterable<String> referencedElementNames() throws IOException;
	InputStream inputStream() throws IOException;
}
