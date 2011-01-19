package ch.parren.jdepchk.rules;

public final class InverseClassFileFilter implements ClassFileFilter {

	final ClassFileFilter base;

	public InverseClassFileFilter(ClassFileFilter base) {
		this.base = base;
	}

	@Override public boolean mightIntersectPackage(String packagePath) {
		return true;
	}

	@Override public boolean allowsClassFile(String internalClassName, boolean currentResult) {
		if (base.allowsClassFile(internalClassName, false))
			return false;
		return currentResult;
	}

	@Override public void describe(StringBuilder to, String indent) {
		to.append("not ");
		base.describe(to, indent);
	}
	
}
