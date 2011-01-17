package ch.parren.jdepchk.rules;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import ch.parren.java.lang.New;

public final class RuleSetBuilder {

	private final Map<String, ScopeBuilder> scopesByName = New.hashMap();
	private final Set<ScopeBuilder> scopesInDefinitionOrder = New.linkedHashSet();
	private Collection<ScopeBuilder> scopesToCheck = New.linkedList();
	private final String name;

	public RuleSetBuilder(String name) {
		this.name = name;
	}

	public ScopeBuilder lib(String name) {
		return define(referenceScope(name));
	}

	public ScopeBuilder scope(String name) {
		return check(define(referenceScope(name)));
	}

	private ScopeBuilder check(ScopeBuilder scope) {
		scopesToCheck.add(scope);
		return scope;
	}

	private ScopeBuilder define(ScopeBuilder scope) {
		if (!scopesInDefinitionOrder.add(scope))
			throw new IllegalStateException("The " + scope + " is already defined.");
		return scope;
	}

	ScopeBuilder referenceScope(String name) {
		final ScopeBuilder found = scopesByName.get(name);
		if (null != found)
			return found;
		final ScopeBuilder made = new ScopeBuilder(this, name);
		scopesByName.put(name, made);
		return made;
	}

	public RuleSet finish() {
		for (ScopeBuilder refd : scopesByName.values())
			if (!scopesInDefinitionOrder.contains(refd))
				throw new IllegalStateException("The " + refd + " has not been defined.");
		final RuleSet ruleSet = new RuleSet(name);
		for (ScopeBuilder defd : scopesInDefinitionOrder)
			defd.define(ruleSet);
		for (ScopeBuilder defd : scopesInDefinitionOrder)
			defd.finish();
		for (ScopeBuilder defd : scopesToCheck)
			defd.checkIn(ruleSet);
		return ruleSet;
	}

	@Override public String toString() {
		return "rule set " + name;
	}

}
