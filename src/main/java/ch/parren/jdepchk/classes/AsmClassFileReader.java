package ch.parren.jdepchk.classes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import ch.parren.java.lang.New;

final class AsmClassFileReader implements ClassFile {

	private static final int CLASS_EXT_LEN = ".class".length();

	private final String name;
	private final File classFile;
	private InputStream classFileStream;
	private ClassReader classFileReader;

	public AsmClassFileReader(File classFile) {
		final String fileName = classFile.getName();
		this.name = fileName.substring(0, fileName.length() - CLASS_EXT_LEN);
		this.classFile = classFile;
	}

	@Override public String compiledClassName() {
		return name;
	}

	@Override public Iterable<String> referencedClassNames() throws IOException {
		final Set<String> result = New.hashSet();
		final ClassVisitor classVisitor = new ClassVisitor() {

			private void add(Type type) {
				if (null == type)
					return;
				result.add(type.getInternalName());
				final Type eltType = type.getElementType();
				if (null != eltType)
					add(eltType);
			}

			@Override public void visit(int version, int access, String name, String signature, String superName,
					String[] interfaces) {
				result.add(superName);
				for (String s : interfaces)
					result.add(s);
			}

			@Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				return annVisitor;
			}

			@Override public FieldVisitor visitField(int access, String name, String desc, String signature,
					Object value) {
				add(Type.getType(desc));
				return fieldVisitor;
			}

			@Override public MethodVisitor visitMethod(int access, String name, String desc, String signature,
					String[] exceptions) {
				for (Type t: Type.getArgumentTypes(desc))
					add(t);
				add(Type.getReturnType(desc));
				return methodVisitor;
			}

			@Override public void visitAttribute(Attribute attr) {}
			@Override public void visitInnerClass(String name, String outerName, String innerName, int access) {}
			@Override public void visitOuterClass(String owner, String name, String desc) {}
			@Override public void visitSource(String source, String debug) {}
			@Override public void visitEnd() {}

			private final AnnotationVisitor annVisitor = new AnnotationVisitor() {
				@Override public void visit(String name, Object value) {
					result.add(name);
				}
				@Override public AnnotationVisitor visitAnnotation(String name, String desc) {
					return annVisitor;
				}
				@Override public AnnotationVisitor visitArray(String name) {
					return annVisitor;
				}
				@Override public void visitEnum(String name, String desc, String value) {
					result.add(desc);
				}
				@Override public void visitEnd() {}
			};

			private final FieldVisitor fieldVisitor = new FieldVisitor() {
				@Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
					return annVisitor;
				}
				@Override public void visitAttribute(Attribute attr) {}
				@Override public void visitEnd() {};
			};

			private final MethodVisitor methodVisitor = new MethodVisitor() {
				@Override public AnnotationVisitor visitAnnotationDefault() {
					return annVisitor;
				}
				@Override public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
					return annVisitor;
				}
				@Override public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
					return annVisitor;
				}
				@Override public void visitAttribute(Attribute attr) {}

				@Override public void visitCode() {}

				@Override public void visitFieldInsn(int opcode, String owner, String name, String desc) {
					add(Type.getType(desc));
				}

				@Override public void visitLocalVariable(String name, String desc, String signature, Label start,
						Label end, int index) {
					add(Type.getType(desc));
				}

				@Override public void visitMethodInsn(int opcode, String owner, String name, String desc) {
					for (Type t: Type.getArgumentTypes(desc))
						add(t);
					add(Type.getReturnType(desc));
				}

				@Override public void visitMultiANewArrayInsn(String desc, int dims) {
					add(Type.getType(desc));
				}

				@Override public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
					result.add(type);
				}

				@Override public void visitTypeInsn(int opcode, String type) {
					result.add(type);
				}

				@Override public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {}
				@Override public void visitIincInsn(int var, int increment) {}
				@Override public void visitInsn(int opcode) {}
				@Override public void visitIntInsn(int opcode, int operand) {}
				@Override public void visitJumpInsn(int opcode, Label label) {}
				@Override public void visitLabel(Label label) {}
				@Override public void visitLdcInsn(Object cst) {}
				@Override public void visitLineNumber(int line, Label start) {}
				@Override public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {}
				@Override public void visitMaxs(int maxStack, int maxLocals) {}
				@Override public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {}
				@Override public void visitVarInsn(int opcode, int var) {}

				@Override public void visitEnd() {}
			};

		};

		reader().accept(classVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
		return result;
	}

	private ClassReader reader() throws IOException {
		if (null == classFileReader) {
			if (null == classFileStream)
				classFileStream = new BufferedInputStream(new FileInputStream(classFile));
			classFileReader = new ClassReader(classFileStream);
		}
		return classFileReader;
	}

	@Override public void close() throws IOException {
		classFileReader = null;
		if (null != classFileStream)
			classFileStream.close();
		classFileStream = null;
	}

}
