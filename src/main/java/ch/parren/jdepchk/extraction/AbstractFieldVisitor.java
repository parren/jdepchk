package ch.parren.jdepchk.extraction;

import ch.parren.jdepchk.classes.asm.Attribute;
import ch.parren.jdepchk.classes.asm.FieldVisitor;

abstract class AbstractFieldVisitor implements FieldVisitor {
	public void visitAttribute(Attribute attr) {}
	public void visitEnd() {}
}
