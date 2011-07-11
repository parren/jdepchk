package ch.parren.jdepchk.classes;

import java.io.IOException;

import ch.parren.jdepchk.classes.asm.ClassReader;

public class CombinedClassSetVisitor extends ClassSet.Visitor {

	private static final int MAX_PACKAGE_DEPTH = 64;

	private final ClassSet.Visitor a;
	private final ClassSet.Visitor b;

	private final boolean[] visitingA = new boolean[MAX_PACKAGE_DEPTH];
	private final boolean[] visitingB = new boolean[MAX_PACKAGE_DEPTH];
	private int at = 0;

	public CombinedClassSetVisitor(ClassSet.Visitor a, ClassSet.Visitor b) {
		this.a = a;
		this.b = b;
		visitingA[0] = true;
		visitingB[0] = true;
	}

	@Override public boolean visitPackage(String packagePath) throws IOException {
		final int next = at + 1;
		visitingA[next] = visitingA[at] && a.visitPackage(packagePath);
		visitingB[next] = visitingB[at] && b.visitPackage(packagePath);
		if (!visitingA[next] && !visitingB[next])
			return false;
		at = next;
		return true;
	}

	@Override public boolean visitClassFile(ClassBytes classFile) throws IOException {
		boolean visit = false;
		visit = visitingA[at] && a.visitClassFile(classFile) || visit;
		visit = visitingB[at] && b.visitClassFile(classFile) || visit;
		return visit;
	}
	
	@Override public void visitClassReader(ClassReader classReader) throws IOException {
		if (visitingA[at])
			a.visitClassReader(classReader);
		if (visitingB[at])
			b.visitClassReader(classReader);
	}

	@Override public void visitPackageEnd() throws IOException {
		if (visitingA[at])
			a.visitPackageEnd();
		if (visitingB[at])
			b.visitPackageEnd();
		at--;
	}

}
