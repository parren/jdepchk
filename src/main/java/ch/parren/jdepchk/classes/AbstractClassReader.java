package ch.parren.jdepchk.classes;

import java.io.IOException;
import java.util.Set;

import ch.parren.java.lang.New;

abstract class AbstractClassReader implements ClassReader {

	private final String name;
	private Set<String> refdNames = null;

	protected AbstractClassReader(String compiledClassName) {
		this.name = compiledClassName;
	}

	@Override public String compiledClassName() {
		return name;
	}

	@Override public Iterable<String> referencedClassNames() throws IOException {
		if (null != refdNames)
			return refdNames;
		final Set<String> result = New.hashSet();

		final ClassParser bytes = newClassBytesReader();
		try {
			assert name.equals(bytes.getClassName());
			add(bytes.getRefdClasses(), result);
		} finally {
			bytes.close();
		}

		refdNames = result;
		return result;
	}

	abstract protected ClassParser newClassBytesReader() throws IOException;

	private void add(String name, Set<String> result) {
		if (null != name && !this.name.equals(name))
			result.add(name);
	}

	private void add(String[] names, Set<String> result) {
		if (null == names)
			return;
		final int n = names.length;
		for (int i = 0; i < n; i++)
			add(names[i], result);
	}

	@Override public String toString() {
		return this.name;
	}
}
