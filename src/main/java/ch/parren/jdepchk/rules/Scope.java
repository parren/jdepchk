package ch.parren.jdepchk.rules;

import java.util.Collection;

import ch.parren.java.lang.New;
import ch.parren.java.lang.Predicate;

public final class Scope {

	private final Collection<Scope> used = New.linkedList();

	private final Collection<Predicate<String>> filters;
	private final String name;
	
	public static int nContains = 0;
	public static int nSees = 0;
	public static int nTests = 0;

	public Scope(String name, Collection<Predicate<String>> patterns) {
		this.name = name;
		this.filters = New.arrayList(patterns);
	}

	public String name() {
		return name;
	}

	void use(Scope scope) {
		used.add(scope);
	}

	public boolean contains(String compiledClassName) {
		nContains++;
		for (Predicate<String> f : filters) {
			nTests++;
			if (f.accepts(compiledClassName))
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
