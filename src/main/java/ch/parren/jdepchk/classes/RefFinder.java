package ch.parren.jdepchk.classes;

import java.util.SortedMap;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.classes.asm.AnnotationVisitor;
import ch.parren.jdepchk.classes.asm.Attribute;
import ch.parren.jdepchk.classes.asm.ClassVisitor;
import ch.parren.jdepchk.classes.asm.FieldVisitor;
import ch.parren.jdepchk.classes.asm.Label;
import ch.parren.jdepchk.classes.asm.MethodVisitor;
import ch.parren.jdepchk.classes.asm.Type;

public abstract class RefFinder {

	public abstract void visitRefs(Visibility ownVisibility, SortedMap<String, Visibility> refs);
	
	public ClassVisitor classVisitor() {
		return classVisitor;
	}

	private final ClassVisitor classVisitor = new ClassVisitor() {

		private static final int ACC_PUBLIC = 0x0001;
		private static final int ACC_PRIVATE = 0x0002;
		private static final int ACC_PROTECTED = 0x0004;
		private static final int ACC_MASK = ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED;

		private SortedMap<String, Visibility> refdElements = New.treeMap(); // want sorting here
		private String ownName;
		private Visibility ownVisibility;
		private Visibility eltVisibility;
		private Visibility annVisibility;

		private void add(String name, Visibility vis) {
			refdElements.put(name, vis);
		}

		private void add(Type type, Visibility vis) {
			if (null == type)
				return;
			switch (type.getSort()) {
			case Type.OBJECT:
				add(type.getInternalName(), vis);
				break;
			case Type.ARRAY:
				final Type eltType = type.getElementType();
				if (null != eltType)
					add(eltType, vis);
			}
		}

		public void visit(int version, int access, String name, String signature, String superName,
				String[] interfaces) {
			refdElements.clear();
			ownName = name;
			ownVisibility = flagsToVis(access);
			addClassRef(name, ownVisibility);
			addClassRef(superName, ownVisibility);
			for (String s : interfaces)
				addClassRef(s, ownVisibility);
			addSignature(signature, ownVisibility);
		}

		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			annVisibility = ownVisibility;
			return annVisitor;
		}

		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			eltVisibility = flagsToVis(access);
			add(Type.getType(desc), eltVisibility);
			addSignature(signature, eltVisibility);
			return fieldVisitor;
		}

		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			eltVisibility = flagsToVis(access);
			for (Type t : Type.getArgumentTypes(desc))
				add(t, eltVisibility);
			add(Type.getReturnType(desc), eltVisibility);
			addSignature(signature, eltVisibility);
			if (null != exceptions)
				for (String s : exceptions)
					addClassRef(s, eltVisibility);
			return methodVisitor;
		}

		public void visitAttribute(Attribute attr) {}
		public void visitInnerClass(String name, String outerName, String innerName, int access) {}
		public void visitOuterClass(String owner, String name, String desc) {}
		public void visitSource(String source, String debug) {}
		public void visitEnd() {
			visitRefs(ownVisibility, refdElements);
			refdElements.clear();
		}

		private final AnnotationVisitor annVisitor = new AnnotationVisitor() {
			public AnnotationVisitor visitAnnotation(String name, String desc) {
				return annVisitor;
			}
			public void visit(String name, Object value) {}
			public AnnotationVisitor visitArray(String name) {
				return annVisitor;
			}
			public void visitEnum(String name, String desc, String value) {
				addDescriptor(desc, annVisibility);
			}
			public void visitEnd() {}
		};

		private final FieldVisitor fieldVisitor = new FieldVisitor() {
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				return annVisitor;
			}
			public void visitAttribute(Attribute attr) {}
			public void visitEnd() {};
		};

		private final MethodVisitor methodVisitor = new MethodVisitor() {
			public AnnotationVisitor visitAnnotationDefault() {
				return annVisitor;
			}
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				annVisibility = eltVisibility;
				return annVisitor;
			}
			public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
				annVisibility = eltVisibility;
				return annVisitor;
			}
			public void visitAttribute(Attribute attr) {}
			public void visitCode() {}
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				addMemberRef(toClassName(owner), name, desc);
			}
			public void visitLocalVariable(String name, String desc, String signature, Label start, Label end,
					int index) {
				addDescriptor(desc, Visibility.PRIV);
				addSignature(signature, Visibility.PRIV);
			}
			public void visitMethodInsn(int opcode, String owner, String name, String desc) {
				addMemberRef(toClassName(owner), name, desc);
			}
			public void visitMultiANewArrayInsn(String desc, int dims) {
				addDescriptor(desc, Visibility.PRIV);
			}
			public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
				addClassRef(type, Visibility.PRIV);
			}
			public void visitTypeInsn(int opcode, String type) {
				addClassRef(toClassName(type), Visibility.PRIV);
			}
			public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {}
			public void visitIincInsn(int var, int increment) {}
			public void visitInsn(int opcode) {}
			public void visitIntInsn(int opcode, int operand) {}
			public void visitJumpInsn(int opcode, Label label) {}
			public void visitLabel(Label label) {}
			public void visitLdcInsn(Object cst) {}
			public void visitLineNumber(int line, Label start) {}
			public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {}
			public void visitMaxs(int maxStack, int maxLocals) {}
			public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {}
			public void visitVarInsn(int opcode, int var) {}
			public void visitEnd() {}
		};

		Visibility flagsToVis(int flags) {
			switch (flags & ACC_MASK) {
			case ACC_PRIVATE:
				return Visibility.PRIV;
			case 0:
				return Visibility.DEF;
			case ACC_PROTECTED:
				return Visibility.PROT;
			default:
				return Visibility.PUBL;
			}
		}

		String addClassRef(String name, Visibility vis) {
			if (null == name || ownName.equals(name))
				return null;
			final Visibility found = refdElements.get(name);
			if (null == found || vis.compareTo(found) > 0)
				add(name, vis);
			return name;
		}

		/**
		 * Extracts type references from type descriptors. We can simply
		 * scan for 'L', which starts a type ref.
		 */
		void addDescriptor(String desc, Visibility vis) {
			int i = 0, n = desc.length();
			while (i < n) {
				if (desc.charAt(i++) == 'L') {
					int i0 = i;
					while (desc.charAt(i++) != ';') {}
					addClassRef(desc.substring(i0, i - 1), vis);
				}
			}
		}

		/**
		 * Extracts type references from generic type signatures. We need to
		 * fairly properly parse these.
		 */
		void addSignature(String sig, Visibility vis) {
			if (null == sig)
				return;
			new SigParser(sig, vis).parse();
		}

		final class SigParser {

			private final String sig;
			private final Visibility vis;
			private int at;

			public SigParser(String sig, Visibility vis) {
				this.sig = sig;
				this.vis = vis;
				this.at = 0;
			}

			public void parse() {
				char c = next();
				if (c == '<') {
					mtdGenParams();
					c = next();
				}
				if (c == '(') {
					mtdParams();
					c = next();
				}
				type(c);
			}

			private void mtdGenParams() {
				char c = next();
				while (c != '>') {
					while (next() != ':') {} // skip param name
					if ((c = next()) != ':')
						type(c); // super class
					else
						at--;
					while ((c = next()) == ':')
						type(next()); // super interface
				}
			}

			private void mtdParams() {
				char c;
				while ((c = next()) != ')')
					type(c);
			}

			private void type(char c) {
				switch (c) {
				case 'L':
					typeSig();
					break;
				case 'T':
					while (next() != ';') {}
					break;
				case '[':
					type(next());
					break;
				}
				// all other chars are assumed to be single letter primitives
			}

			private void typeSig() {
				final StringBuilder name = new StringBuilder();
				char c;
				do {
					final int start = at;
					while ((c = next()) != ';' && c != '<' && c != '.') {}
					name.append(sig, start, at - 1);
					if (c == '.')
						name.append('$');
					else if (c == '<')
						while ((c = next()) != '>')
							type(c);
				} while (c != ';');
				addClassRef(name.toString(), vis);
			}

			private char next() {
				return sig.charAt(at++);
			}

		}

		void addMemberRef(String className, String memberName, String memberDescriptor) {
			if (null == className)
				return;
			addClassRef(className, Visibility.PRIV);
			add(className + "#" + memberName + "#" + memberDescriptor, Visibility.PRIV);
		}

		String toClassName(String name) {
			if (name.charAt(0) == '[')
				if (name.charAt(1) == 'L')
					return name.substring(2, name.length() - 1);
				else
					return null;
			return name;
		}

	};

}
