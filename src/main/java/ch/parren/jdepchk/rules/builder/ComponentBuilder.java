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
		extend(ruleSet.ref(componentName));
		return this;
	}

	public ComponentBuilder use(String componentName) {
		use(ruleSet.ref(componentName));
		return this;
	}

	void extend(ComponentBuilder comp) {
		extended.add(comp);
		use(comp);
	}

	void use(ComponentBuilder comp) {
		used.add(comp);
	}

	@Override protected void prepare(RuleSet ruleSet) {
		if (prepared)
			return;
		prepared = true;

		// build transient closure
		for (ComponentBuilder e : extended) {
			e.prepare(ruleSet);
			for (ComponentBuilder ee : e.extended)
				extend(ee);
		}

		if (!used.isEmpty()) {
			// components see themselves by default
			final Iterator<FilterBuilder> it = contains.descendingIterator();
			while (it.hasNext())
				allows.addFirst(it.next());
		}

		for (ComponentBuilder u : used)
			allows.addAll(u.contains);
	}

	@Override public String toString() {
		return "component " + name;
	}

}
