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
				.replaceAll("[*]", "[^.]*") //
				.replaceAll("~", ".*");
		return Pattern.compile(regex);
	}

	ScopeBuilder referenceScope(String name) {
		final ScopeBuilder found = scopesByName.get(name);
		if (null != found)
			return found;
		final ScopeBuilder made = new ScopeBuilder(name);
		scopesByName.put(name, made);
		return made;
	}

	public RuleSet finish() {
		for (ScopeBuilder refd : scopesByName.values())
			if (!scopesInDefinitionOrder.contains(refd))
				throw new IllegalStateException("The " + refd + " has not been defined.");
		final RuleSet ruleSet = new RuleSet(name);
		for (ScopeBuilder defd : scopesInDefinitionOrder)
			defd.finish(ruleSet);
		for (ScopeBuilder defd : scopesToCheck)
			defd.checkIn(ruleSet);
		return ruleSet;
	}

	@Override public String toString() {
		return "rule set " + name;
	}

}
