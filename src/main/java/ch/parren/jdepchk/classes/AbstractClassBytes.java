package ch.parren.jdepchk.classes;


public abstract class AbstractClassBytes extends ClassBytes {

	private final String name;

	protected AbstractClassBytes(String compiledClassName) {
		this.name = compiledClassName;
	}

	@Override public String compiledClassName() {
		return name;
	}

	@Override public String toString() {
		return this.name;
	}

}
