package ch.parren.jdepchk.rules;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

import ch.parren.java.lang.New;
import ch.parren.java.lang.Predicate;

public final class ScopeBuilder {

	public static final String DEFAULT_NAME = "$default";

	private final Collection<Predicate<String>> filters = New.linkedList();
	private final Set<ScopeBuilder> extended = New.hashSet();
	private final Set<ScopeBuilder> used = New.hashSet();

	private final RuleSetBuilder ruleSetBuilder;
	private final String name;

	private Scope scope = null;
	private boolean finished = false;

	ScopeBuilder(RuleSetBuilder ruleSetBuilder, String name) {
		this.ruleSetBuilder = ruleSetBuilder;
		this.name = name;
		if (!DEFAULT_NAME.equals(name))
			use(DEFAULT_NAME);
	}

	public ScopeBuilder pattern(final Pattern pattern) {
		filters.add(new PatternMatcher(pattern));
		return this;
	}

	public ScopeBuilder prefix(final String prefix) {
		filters.add(new PrefixMatcher(prefix));
		return this;
	}

	public ScopeBuilder packages(String spec) {
		if (spec.endsWith("**") && spec.indexOf('*') < (spec.length() - 2))
			return prefix(spec.substring(0, spec.length() - 2));
		return pattern(packageSpecToPattern(spec));
	}

	public static Pattern packageSpecToPattern(String spec) {
		final String regex = spec //
				.replace('.', '/') //
				.replaceAll("[*][*]", "~") //
				.replaceAll("[*]", "[^.]*") //
				.replaceAll("~", ".*");
		return Pattern.compile(regex);
	}

	public ScopeBuilder impliedPackages() {
		return packages(name + ".**");
	}

	public ScopeBuilder use(String scopeName) {
		final ScopeBuilder scope = ruleSetBuilder.referenceScope(scopeName);
		used.add(scope);
		return this;
	}

	public ScopeBuilder extend(String scopeName) {
		final ScopeBuilder scope = ruleSetBuilder.referenceScope(scopeName);
		extend(scope);
		return this;
	}

	private void extend(ScopeBuilder scope) {
		extended.add(scope);
		used.add(scope);
	}

	void define() {
		scope = new Scope(name, filters);
	}

	void finish() {
		if (finished)
			return;
		finished = true;

		// build transient closure
		for (ScopeBuilder e : extended) {
			e.finish();
			for (ScopeBuilder ee : e.extended)
				extend(ee);
		}

		for (ScopeBuilder u : used)
			scope.use(u.scope);
	}

	void checkIn(RuleSet ruleSet) {
		ruleSet.check(scope);
	}

	@Override public String toString() {
		return "scope " + name;
	}

	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScopeBuilder other = (ScopeBuilder) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
