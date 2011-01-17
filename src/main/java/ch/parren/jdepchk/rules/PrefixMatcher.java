package ch.parren.jdepchk.rules;

import ch.parren.java.lang.Predicate;

public final class PrefixMatcher implements Predicate<String> {

	private final String prefix;

	public PrefixMatcher(String prefix) {
		this.prefix = prefix;
	}

	@Override public boolean accepts(String tested) {
		return tested.startsWith(prefix);
	}

}