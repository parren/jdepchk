package ch.parren.jdepchk.rules;

import java.util.regex.Pattern;

public final class PatternMatcher implements ClassFileFilter {

	private final Pattern pattern;

	public PatternMatcher(Pattern pattern) {
		this.pattern = pattern;
	}

	/* @Override */public boolean mightIntersectPackage(String packagePath) {
		return true;
	}

	/* @Override */public boolean allowsClassFile(String internalClassName, boolean currentResult) {
		if (pattern.matcher(internalClassName).matches())
			return true;
		return currentResult;
	}

	/* @Override */public void describe(StringBuilder to, String indent) {
		to.append("pattern: ").append(pattern);
	}

	@Override public String toString() {
		final StringBuilder b = new StringBuilder();
		describe(b, "");
		return b.toString();
	}
	
}