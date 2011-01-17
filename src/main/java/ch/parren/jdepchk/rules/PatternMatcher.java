package ch.parren.jdepchk.rules;

import java.util.regex.Pattern;

import ch.parren.java.lang.Predicate;

public final class PatternMatcher implements Predicate<String> {

	private final Pattern pattern;

	public PatternMatcher(Pattern pattern) {
		this.pattern = pattern;
	}

	@Override public boolean accepts(String tested) {
		return pattern.matcher(tested).matches();
	}

}