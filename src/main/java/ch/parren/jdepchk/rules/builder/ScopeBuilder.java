package ch.parren.jdepchk.rules.builder;

import java.util.Collection;
import java.util.LinkedList;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.rules.CompositeClassFileFilter;

public class ScopeBuilder extends AbstractScopeBuilder {

	protected final LinkedList<FilterBuilder> allows = New.linkedList();

	ScopeBuilder(String name) {
		super(name);
	}

	@Override public ScopeBuilder contains(FilterBuilder... filters) {
		super.contains(filters);
		return this;
	}

	public final ScopeBuilder allows(FilterBuilder... filters) {
		for (FilterBuilder f : filters)
			allows.add(f);
		return this;
	}

	public Collection<FilterBuilder> allowsFilters() {
		return allows;
	}

	@Override protected final CompositeClassFileFilter buildAllows() {
		return buildFilter(allows);
	}

	@Override public String toString() {
		return "scope " + name;
	}

}
