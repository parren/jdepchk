package ch.parren.jdepchk.rules;

import java.util.Collection;

import ch.parren.java.lang.New;

public final class CompositeClassFileFilter implements ClassFileFilter {

	private final Collection<ClassFileFilter> filters;
	private final boolean defaultValue;
	private final boolean mixedAllowDeny;

	public CompositeClassFileFilter(Collection<ClassFileFilter> filters, boolean defaultValue, boolean mixed) {
		this.filters = New.arrayList(filters);
		this.defaultValue = defaultValue;
		this.mixedAllowDeny = mixed;
	}

	/* @Override */public boolean mightIntersectPackage(String packagePath) {
		for (ClassFileFilter f : filters)
			if (f.mightIntersectPackage(packagePath))
				return true;
		return false;
	}

	/* @Override */public boolean allowsClassFile(String internalClassName, boolean currentResult) {
		boolean result = currentResult;
		for (ClassFileFilter f : filters) {
			result = f.allowsClassFile(internalClassName, result);
			if (!mixedAllowDeny && result != defaultValue)
				return result;
		}
		return result;
	}

	public boolean allowsClassFile(String internalClassName) {
		return allowsClassFile(internalClassName, defaultValue);
	}

	public boolean isEmpty() {
		return filters.isEmpty();
	}

	/* @Override */public void describe(StringBuilder to, String indent) {
		to.append("composite; default: ").append(defaultValue);
		final String sub = indent + '\t';
		for (ClassFileFilter f : filters) {
			to.append('\n').append(sub);
			f.describe(to, sub);
		}
	}

}
