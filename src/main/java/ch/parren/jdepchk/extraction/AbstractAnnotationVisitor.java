package ch.parren.jdepchk.extraction;

import ch.parren.jdepchk.classes.asm.AnnotationVisitor;

abstract class AbstractAnnotationVisitor implements AnnotationVisitor {
	public void visit(String name, Object value) {}
	public void visitEnum(String name, String desc, String value) {}
	public AnnotationVisitor visitAnnotation(String name, String desc) {
		return null;
	}
	public AnnotationVisitor visitArray(String name) {
		return null;
	}
	public void visitEnd() {}
}
