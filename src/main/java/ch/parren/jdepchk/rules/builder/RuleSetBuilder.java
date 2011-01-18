package ch.parren.jdepchk.rules.builder;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.rules.ClassFileFilter;
import ch.parren.jdepchk.rules.PatternMatcher;
import ch.parren.jdepchk.rules.PrefixMatcher;
import ch.parren.jdepchk.rules.RuleSet;

public final class RuleSetBuilder {

	private final Map<String, AbstractScopeBuilder> scopesByName = New.hashMap();
	private final Set<AbstractScopeBuilder> scopesInDefinitionOrder = New.linkedHashSet();
	private final Collection<AbstractScopeBuilder> scopesToCheck = New.linkedList();
	private final String name;
	private final ComponentBuilder defaultLib;

	public RuleSetBuilder(String name) {
		this.name = name;
		this.defaultLib = lib(ComponentBuilder.DEFAULT_NAME);
	}

	public ScopeBuilder scope(String name) {
		return (ScopeBuilder) define(referenceScope(name, new ScopeBuilder(name)));
	}

	public ComponentBuilder lib(String name) {
		return (ComponentBuilder) define(referenceScope(name, new ComponentBuilder(this, name)));
	}

	public ComponentBuilder comp(String name) {
		return check(lib(name));
	}

	ComponentBuilder ref(String name) {
		return (ComponentBuilder) referenceScope(name, new ComponentBuilder(this, name));
	}

	private ComponentBuilder check(ComponentBuilder scope) {
		scope.use(defaultLib);
		// FIXME sees itself
		return scope;
	}

	private AbstractScopeBuilder define(AbstractScopeBuilder scope) {
		if (!scopesInDefinitionOrder.add(scope))
			if (!ComponentBuilder.DEFAULT_NAME.equals(scope.name))
				throw new IllegalStateException("The " + scope + " is already defined.");
		return scope;
	}

	public FilterBuilder glob(String glob) {
		if (glob.endsWith("**") && glob.indexOf('*') >= (glob.length() - 2))
			return prefix(glob.substring(0, glob.length() - 2));
		return pattern(globToPattern(glob));
	}

	public FilterBuilder pattern(Pattern pattern) {
		return filter(new PatternMatcher(pattern));
	}

	public FilterBuilder prefix(String prefix) {
		return filter(new PrefixMatcher(prefix.replace('.', '/')));
	}

	public FilterBuilder filter(ClassFileFilter filter) {
		return new FilterBuilder(filter);
	}

	public static Pattern globToPattern(String spec) {
		final String regex = spec //
				.replace('.', '/') //
				.replaceAll("[*][*]", "~") //
				.replaceAll("[*]", "[^/]*") //
				.replaceAll("~", ".*");
		return Pattern.compile(regex);
	}

	AbstractScopeBuilder referenceScope(String name, AbstractScopeBuilder def) {
		final AbstractScopeBuilder found = scopesByName.get(name);
		if (null != found)
			return found;
		scopesByName.put(name, def);
		return def;
	}

	public RuleSet finish() {
		for (AbstractScopeBuilder refd : scopesByName.values())
			if (!scopesInDefinitionOrder.contains(refd))
				throw new IllegalStateException("The " + refd + " has not been defined.");
		final RuleSet ruleSet = new RuleSet(name);
		for (AbstractScopeBuilder defd : scopesInDefinitionOrder)
			defd.prepare(ruleSet);
		for (AbstractScopeBuilder defd : scopesInDefinitionOrder)
			defd.finish(ruleSet);
		for (AbstractScopeBuilder defd : scopesToCheck)
			defd.checkIn(ruleSet);
		return ruleSet;
	}

	@Override public String toString() {
		return "rule set " + name;
	}

}
