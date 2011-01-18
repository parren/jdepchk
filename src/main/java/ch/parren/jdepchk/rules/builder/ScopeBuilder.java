package ch.parren.jdepchk.rules.builder;

import java.util.Collection;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.rules.ClassFileFilter;
import ch.parren.jdepchk.rules.CompositeClassFileFilter;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.Scope;

public final class ScopeBuilder {

	public static final String DEFAULT_NAME = "$default";

	private final Collection<FilterBuilder> contains = New.linkedList();
	private final Collection<FilterBuilder> allows = New.linkedList();

	private final String name;

	private Scope scope = null;

	ScopeBuilder(String name) {
		this.name = name;
	}

	public ScopeBuilder contains(FilterBuilder... filters) {
		for (FilterBuilder f : filters)
			contains.add(f);
		return this;
	}

	public ScopeBuilder allows(FilterBuilder... filters) {
		for (FilterBuilder f : filters)
			allows.add(f);
		return this;
	}

	void finish(RuleSet ruleSet) {
		scope = new Scope(ruleSet, name, buildFilter(contains), buildFilter(allows));
	}

	private CompositeClassFileFilter buildFilter(Collection<FilterBuilder> builders) {
		final boolean def = builders.iterator().next().defaultValue();
		final Collection<ClassFileFilter> filters = New.arrayList(builders.size());
		boolean mixed = false;
		for (FilterBuilder b : builders) {
			if (b.defaultValue() != def)
				mixed = true;
			filters.add(b.filter);
		}
		return new CompositeClassFileFilter(filters, def, mixed);
	}

	void checkIn(RuleSet ruleSet) {
		ruleSet.check(scope);
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
		ScopeBuilder other = (ScopeBuilder) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
