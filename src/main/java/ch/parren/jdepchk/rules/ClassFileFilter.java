package ch.parren.jdepchk.rules;

public interface ClassFileFilter {

	boolean mightIntersectPackage(String packagePath);
	boolean allowsClassFile(String internalClassName, boolean currentResult);

	void describe(StringBuilder to, String indent);

}
