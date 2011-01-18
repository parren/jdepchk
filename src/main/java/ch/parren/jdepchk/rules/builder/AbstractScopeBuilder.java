package ch.parren.jdepchk.rules.builder;

import java.util.Collection;
import java.util.Iterator;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.rules.ClassFileFilter;
import ch.parren.jdepchk.rules.CompositeClassFileFilter;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.Scope;

public abstract class AbstractScopeBuilder {

	protected final String name;
	protected final Collection<FilterBuilder> contains = New.linkedList();

	private Scope scope = null;

	protected AbstractScopeBuilder(String name) {
		this.name = name;
	}

	public AbstractScopeBuilder contains(FilterBuilder... filters) {
		for (FilterBuilder f : filters)
			contains.add(f);
		return this;
	}

	@SuppressWarnings("unused")//
	protected void prepare(RuleSet ruleSet) {}

	final void finish(RuleSet ruleSet) {
		final CompositeClassFileFilter allows = buildAllows();
		scope = new Scope(ruleSet, name, buildFilter(contains), allows);
		if (!allows.isEmpty())
			checkIn(ruleSet);
	}

	protected abstract CompositeClassFileFilter buildAllows();

	protected final CompositeClassFileFilter buildFilter(Collection<FilterBuilder> builders) {
		final Iterator<FilterBuilder> it = builders.iterator();
		final boolean def = it.hasNext() ? it.next().defaultValue() : false;
		final Collection<ClassFileFilter> filters = New.arrayList(builders.size());
		boolean mixed = false;
		for (FilterBuilder b : builders) {
			if (b.defaultValue() != def)
				mixed = true;
			filters.add(b.filter);
		}
		return new CompositeClassFileFilter(filters, def, mixed);
	}

	final void checkIn(RuleSet ruleSet) {
		ruleSet.check(scope);
	}

	@Override public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractScopeBuilder other = (AbstractScopeBuilder) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
