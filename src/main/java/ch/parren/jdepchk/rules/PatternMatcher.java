package ch.parren.jdepchk.rules;

import java.util.regex.Pattern;


public final class PatternMatcher implements ClassFileFilter {

	private final Pattern pattern;

	public PatternMatcher(Pattern pattern) {
		this.pattern = pattern;
	}

	@Override public boolean mightIntersectPackage(String packagePath) {
		return true;
	}
	
	@Override public boolean allowsClassFile(String internalClassName) {
		return pattern.matcher(internalClassName).matches();
	}

}