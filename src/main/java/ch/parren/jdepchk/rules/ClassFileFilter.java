package ch.parren.jdepchk.rules;

public interface ClassFileFilter {

	boolean mightIntersectPackage(String packagePath);
	boolean allowsClassFile(String internalClassName);
	
}
