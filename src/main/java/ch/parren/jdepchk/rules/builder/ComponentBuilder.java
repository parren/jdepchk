package ch.parren.jdepchk.rules.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.rules.RuleSet;

public class ComponentBuilder extends ScopeBuilder {

	public static final String DEFAULT_NAME = "$default";

	// We want to preserve insertion order on these for finalizing the rules in a stable way.
	private final Set<ComponentBuilder> extended = New.linkedHashSet();
	private final Set<ComponentBuilder> used = New.linkedHashSet();

	private final RuleSetBuilder ruleSet;

	private boolean prepared = false;

	ComponentBuilder(RuleSetBuilder ruleSet, String name) {
		super(name);
		this.ruleSet = ruleSet;
	}

	public ComponentBuilder impliedPackages() {
		contains(ruleSet.prefix(name + '.'));
		return this;
	}

	@Override public ComponentBuilder contains(FilterBuilder... filters) {
		super.contains(filters);
		return this;
	}

	public ComponentBuilder extend(String componentName) {
		extend(ref(componentName));
		return this;
	}

	public ComponentBuilder use(String componentName) {
		use(ref(componentName));
		return this;
	}

	private ComponentBuilder ref(String componentName) {
		return ruleSet.ref(componentName);
	}

	void extend(ComponentBuilder comp) {
		checkNotThis(comp);
		extended.add(comp);
		if (!used.isEmpty()) // already checked
			used.add(comp);
	}

	void use(ComponentBuilder comp) {
		checkNotThis(comp);
		used.add(comp);
	}

	private void checkNotThis(ComponentBuilder comp) {
		if (comp == this)
			throw new IllegalArgumentException(comp + " refers to itself.");
	}

	@Override protected void prepare(RuleSet ruleSet) {
		if (prepared)
			return;
		prepared = true;

		if (!used.isEmpty()) {
			// components see themselves by default
			final List<FilterBuilder> rev = New.arrayList(contains);
			Collections.reverse(rev);
			for (FilterBuilder it : rev)
				allows.addFirst(it);
		}

		final Set<ComponentBuilder> seen = New.hashSet();
		seen.add(this);
		for (ComponentBuilder u : used)
			closeOver(u, seen, allows);
	}

	private void closeOver(ComponentBuilder comp, Set<ComponentBuilder> seen, Collection<FilterBuilder> allows) {
		if (!seen.add(comp))
			return;
		allows.addAll(comp.contains);
		for (ComponentBuilder e : comp.extended)
			closeOver(e, seen, allows);
	}

	@Override public String toString() {
		return "component " + name;
	}

}
