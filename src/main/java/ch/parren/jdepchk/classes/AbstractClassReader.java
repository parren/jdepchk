package ch.parren.jdepchk.classes;

import java.io.IOException;
import java.util.Set;

abstract class AbstractClassReader implements ClassReader {

	private final String name;
	private Set<String> refdNames = null;

	protected AbstractClassReader(String compiledClassName) {
		this.name = compiledClassName;
	}

	/* @Override */public String compiledClassName() {
		return name;
	}

	/* @Override */public Iterable<String> referencedClassNames() throws IOException {
		if (null != refdNames)
			return refdNames;
		final Set<String> result;

		final ClassParser bytes = newClassParser();
		try {
			// TODO use visibility
			result = bytes.referencedClasses().keySet();
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
