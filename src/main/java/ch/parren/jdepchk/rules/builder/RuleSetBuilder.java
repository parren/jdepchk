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

	private final Collection<String[]> defs = New.linkedList();
	private final Set<String> defNames = New.hashSet();
	private final Map<String, AbstractScopeBuilder> scopesByName = New.hashMap();
	private final Set<AbstractScopeBuilder> scopesInDefinitionOrder = New.linkedHashSet();
	private final Collection<AbstractScopeBuilder> scopesToCheck = New.linkedList();
	private final String name;
	private final ComponentBuilder defaultLib;

	public RuleSetBuilder(String name) {
		this.name = name;
		this.defaultLib = lib(ComponentBuilder.DEFAULT_NAME);
	}

	public RuleSetBuilder define(String name, String value) {
		if (!defNames.add(name))
			throw new IllegalArgumentException(name + " is already defined.");
		defs.add(new String[] { name, value });
		return this;
	}

	public ScopeBuilder scope(String name) {
		return (ScopeBuilder) define(new ScopeBuilder(subst(name)));
	}

	public ComponentBuilder lib(String name) {
		final ComponentBuilder def = new ComponentBuilder(this, subst(name));
		return (ComponentBuilder) define(referenceScope(def.name, def));
	}

	public ComponentBuilder comp(String name) {
		return check(lib(name));
	}

	ComponentBuilder ref(String name) {
		final ComponentBuilder def = new ComponentBuilder(this, subst(name));
		return (ComponentBuilder) referenceScope(def.name, def);
	}

	public String subst(String str) {
		String res = str;
		for (String[] def : defs)
			res = res.replace(def[0], def[1]);
		return res;
	}

	private ComponentBuilder check(ComponentBuilder scope) {
		scope.use(defaultLib);
		return scope;
	}

	private AbstractScopeBuilder define(AbstractScopeBuilder scope) {
		if (!scopesInDefinitionOrder.add(scope))
			if (!ComponentBuilder.DEFAULT_NAME.equals(scope.name))
				throw new IllegalStateException("The " + scope + " is already defined.");
		return scope;
	}

	public static FilterBuilder glob(String glob) {
		if (glob.endsWith("**") && glob.indexOf('*') >= (glob.length() - 2))
			return prefix(glob.substring(0, glob.length() - 2));
		return pattern(globToPattern(glob));
	}

	public static FilterBuilder pattern(Pattern pattern) {
		return filter(new PatternMatcher(pattern));
	}

	public static FilterBuilder prefix(String prefix) {
		return filter(new PrefixMatcher(prefix.replace('.', '/')));
	}

	public static FilterBuilder filter(ClassFileFilter filter) {
		return new FilterBuilder(filter);
	}

	public FilterBuilder selfref() {
		return FilterBuilder.SELFREF;
	}

	public static Pattern globToPattern(String spec) {
		final String regex = spec //
				.replace('.', '/') //
				.replaceAll("[*][*]", "~") //
				.replaceAll("[*]", "[^/]*") //
				.replaceAll("~", ".*");
		if (regex.endsWith(".*"))
			return Pattern.compile(regex);
		else
			return Pattern.compile(regex + "([#].*$|$)"); // allow trailing member info
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
