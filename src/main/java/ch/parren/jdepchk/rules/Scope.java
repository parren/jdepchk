package ch.parren.jdepchk.rules;

public final class Scope {

	private final RuleSet ruleSet;
	private final String name;
	private final CompositeClassFileFilter contains;
	private final CompositeClassFileFilter allows;
	
	public Scope(RuleSet ruleSet, String name, CompositeClassFileFilter contains, CompositeClassFileFilter allows) {
		this.ruleSet = ruleSet;
		this.name = name;
		this.contains = contains;
		this.allows = allows;
	}

	public RuleSet ruleSet() {
		return ruleSet;
	}
	
	public String name() {
		return name;
	}

	public boolean mightIntersectPackage(String packagePath) {
		return this.contains.mightIntersectPackage(packagePath);
	}

	public boolean contains(String internalClassName) {
		return this.contains.allowsClassFile(internalClassName);
	}

	public boolean allows(String internalClassName) {
		return this.allows.allowsClassFile(internalClassName);
	}

	@Override public String toString() {
		return "scope " + name;
	}

	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Scope other = (Scope) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
