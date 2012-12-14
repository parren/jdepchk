package ch.parren.jdepchk.rules.builder;

import java.util.Collection;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.rules.RuleSet;

public class ComponentBuilder extends ScopeBuilder {

	public static final String DEFAULT_NAME = "$default";

	// We want to preserve insertion order on these for finalizing the rules in a stable way.
	private final Set<ComponentBuilder> extended = New.linkedHashSet();
	private final Set<ComponentBuilder> used = New.linkedHashSet();
	private final Set<ComponentBuilder> exceptions = New.linkedHashSet();

	private final RuleSetBuilder ruleSet;
	private final ComponentBuilder parent;

	private static enum Done { NONE, DEFAULTS, DEPENDENCIES, EXCEPTIONS };

	private Done done = Done.NONE;

	boolean isComponent;

	ComponentBuilder(RuleSetBuilder ruleSet, String name) {
		this(null, ruleSet, name);
	}

	ComponentBuilder(ComponentBuilder parent, RuleSetBuilder ruleSet, String name) {
		super(name);
		this.ruleSet = ruleSet;
		this.parent = parent;
	}

	@Override public ComponentBuilder contains(FilterBuilder... filters) {
		super.contains(filters);
		return this;
	}

	@Override public ComponentBuilder allows(FilterBuilder... filterBuilders) {
		super.allows(filterBuilders);
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

	public ComponentBuilder except(String componentName) {
		ComponentBuilder exception = this.ruleSet.comp(this, componentName);
		this.exceptions.add(exception);
		return exception;
	}

	public ComponentBuilder done() {
		return parent;
	}

	private ComponentBuilder ref(String componentName) {
		return ruleSet.ref(componentName);
	}

	void extend(ComponentBuilder comp) {
		checkNotThis(comp);
		extended.add(comp);
		if (isComponent)
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

	@Override protected void prepareDefaults(RuleSet ruleSet, ComponentBuilder defaultLib) {
		if (done.ordinal() >= Done.DEFAULTS.ordinal())
			return;
		done = Done.DEFAULTS;

		if (!isComponent)
			return;

		for (ComponentBuilder exception : exceptions) {
			// exceptions duplicate their host's usage
			exception.used.add(this);
			exception.used.addAll(used);
			addFirst(exception.allows, allows);
			// default for exceptions is to contain their name as prefix, assuming a class reference
			if (exception.contains.isEmpty()) {
				exception.contains.add(this.ruleSet.glob(exception.name));
			}
		}

		// components see themselves by default
		addFirst(allows, contains);
		addFirst(allows, defaultLib.contains);
	}

	@Override protected void prepareDependencies(RuleSet ruleSet) {
		if (done.ordinal() >= Done.DEPENDENCIES.ordinal())
			return;
		done = Done.DEPENDENCIES;

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

	@Override protected void prepareExceptions(RuleSet ruleSet) {
		if (done.ordinal() >= Done.EXCEPTIONS.ordinal())
			return;
		done = Done.EXCEPTIONS;

		// components don't contain their exceptions
		for (ComponentBuilder exception : exceptions) {
			// default for exceptions is to contain their name as prefix, assuming a class reference
			if (exception.contains.isEmpty()) {
				exception.contains.add(this.ruleSet.glob(exception.name));
			}
			for (FilterBuilder exceptionContains : exception.contains) {
				contains.add(exceptionContains.not());
			}
		}
	}

	@Override public String toString() {
		return "component " + name;
	}

	private static <E> void addFirst(LinkedList<? super E> to, LinkedList<? extends E> what) {
		ListIterator<? extends E> it = what.listIterator(what.size());
		while (it.hasPrevious())
			to.addFirst(it.previous());
	}
}
