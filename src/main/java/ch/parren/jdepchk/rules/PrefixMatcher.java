package ch.parren.jdepchk.rules;

public final class PrefixMatcher implements ClassFileFilter {

	private final String prefix;
	private final String packagePath;

	public PrefixMatcher(String prefix) {
		this.prefix = prefix;
		this.packagePath = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : null;
	}

	@Override public boolean mightIntersectPackage(String packagePath) {
		if (null == this.packagePath)
			return true;
		if (this.packagePath.startsWith(packagePath))
			return true;
		if (packagePath.startsWith(this.packagePath))
			return true;
		return false;
	}

	@Override public boolean allowsClassFile(String internalClassName) {
		return internalClassName.startsWith(prefix);
	}

}