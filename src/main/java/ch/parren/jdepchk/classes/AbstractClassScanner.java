package ch.parren.jdepchk.classes;

import java.io.IOException;
import java.util.Set;

public abstract class AbstractClassScanner implements ClassScanner {

	public static int nFilesRead = 0; 
	
	private final String name;
	private Set<String> refdNames = null;

	protected AbstractClassScanner(String compiledClassName) {
		this.name = compiledClassName;
	}

	/* @Override */public String compiledClassName() {
		return name;
	}

	/* @Override */public Iterable<String> referencedElementNames() throws IOException {
		if (null != refdNames)
			return refdNames;
		final Set<String> result;

		nFilesRead++;
		final ClassParser bytes = newClassParser();
		try {
			// TODO use visibility
			result = bytes.referencedElementNames().keySet();
		} finally {
			bytes.close();
		}

		refdNames = result;
		return result;
	}

	abstract protected ClassParser newClassParser() throws IOException;

	@Override public String toString() {
		return this.name;
	}

}
