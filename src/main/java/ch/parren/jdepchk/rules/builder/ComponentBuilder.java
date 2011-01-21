package ch.parren.jdepchk.rules.builder;

import java.util.Iterator;
import java.util.Set;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.rules.RuleSet;

public class ComponentBuilder extends ScopeBuilder {

	public static final String DEFAULT_NAME = "$default";

	private final Set<ComponentBuilder> extended = New.hashSet();
	private final Set<ComponentBuilder> used = New.hashSet();

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
		use(comp);
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

		// build transient closure; two-stage process ensures no concurrent modification
		final Set<ComponentBuilder> closure = New.hashSet();
		for (ComponentBuilder e : extended)
			closeOver(this, e, ruleSet, closure);
		for (ComponentBuilder e : closure)
			extend(e);

		if (!used.isEmpty()) {
			// components see themselves by default
			final Iterator<FilterBuilder> it = contains.descendingIterator();
			while (it.hasNext())
				allows.addFirst(it.next());
		}

		for (ComponentBuilder u : used)
			allows.addAll(u.contains);
	}

	private ComponentBuilder closeOver(ComponentBuilder origin, ComponentBuilder comp, RuleSet ruleSet,
			Set<ComponentBuilder> closure) {
		if (comp == origin)
			throw new IllegalStateException("Recursive extensions detected involving " + origin);
		comp.prepare(ruleSet);
		for (ComponentBuilder ee : comp.extended)
			closure.add(closeOver(origin, ee, ruleSet, closure));
		return comp;
	}

	@Override public String toString() {
		return "component " + name;
	}

}
