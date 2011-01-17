package ch.parren.jdepchk.rules;

import java.util.Collection;

import ch.parren.java.lang.New;

public final class Scope {

	private final Collection<Scope> used = New.linkedList();

	private final RuleSet ruleSet;
	private final String name;
	private final Collection<ClassFileFilter> filters;

	public static int nIntersects = 0;
	public static int nContains = 0;
	public static int nSees = 0;
	public static int nTests = 0;

	public Scope(RuleSet ruleSet, String name, Collection<ClassFileFilter> patterns) {
		this.ruleSet = ruleSet;
		this.name = name;
		this.filters = New.arrayList(patterns);
	}

	public RuleSet ruleSet() {
		return ruleSet;
	}
	
	public String name() {
		return name;
	}

	void use(Scope scope) {
		used.add(scope);
	}

	public boolean mightIntersectPackage(String packageName) {
		nIntersects++;
		for (ClassFileFilter f : filters) {
			nTests++;
			if (f.mightIntersectPackage(packageName))
				return true;
		}
		return false;
	}

	public boolean contains(String compiledClassName) {
		nContains++;
		for (ClassFileFilter f : filters) {
			nTests++;
			if (f.allowsClassFile(compiledClassName))
				return true;
		}
		return false;
	}

	public boolean sees(String compiledClassName) {
		nSees++;
		for (Scope seen : used)
			if (seen.contains(compiledClassName))
				return true;
		return false;
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
