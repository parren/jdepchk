package ch.parren.jdepchk.classes;

import java.io.IOException;
import java.io.InputStream;

public abstract class ClassBytes {
	public abstract String compiledClassName() throws IOException;
	public abstract InputStream inputStream() throws IOException;
}
