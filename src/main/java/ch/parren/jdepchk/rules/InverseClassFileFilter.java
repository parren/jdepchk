package ch.parren.jdepchk.rules;

public final class InverseClassFileFilter implements ClassFileFilter {

	final ClassFileFilter base;

	public InverseClassFileFilter(ClassFileFilter base) {
		this.base = base;
	}

	@Override public boolean mightIntersectPackage(String packagePath) {
		return false;
	}

	@Override public boolean allowsClassFile(String internalClassName, boolean currentResult) {
		if (base.allowsClassFile(internalClassName, false))
			return false;
		return currentResult;
	}

}
