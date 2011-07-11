package ch.parren.jdepchk.extraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.annotations.Allows;
import ch.parren.jdepchk.annotations.AppliesTo;
import ch.parren.jdepchk.annotations.Checked;
import ch.parren.jdepchk.annotations.ExcludingSubPackages;
import ch.parren.jdepchk.annotations.Extends;
import ch.parren.jdepchk.annotations.RestrictedTo;
import ch.parren.jdepchk.annotations.Uses;
import ch.parren.jdepchk.classes.ClassBytes;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.asm.AnnotationVisitor;
import ch.parren.jdepchk.classes.asm.Attribute;
import ch.parren.jdepchk.classes.asm.ClassReader;
import ch.parren.jdepchk.classes.asm.ClassVisitor;
import ch.parren.jdepchk.classes.asm.FieldVisitor;
import ch.parren.jdepchk.classes.asm.MethodVisitor;

public final class Extractor {

	private static final String descOf(Class cls) {
		return "L" + cls.getCanonicalName().replace('.', '/') + ";";
	}

	private static final String restrictedToDesc = descOf(RestrictedTo.class);
	private static final String checkedDesc = descOf(Checked.class);
	private static final String excludesSubPackagesDesc = descOf(ExcludingSubPackages.class);

	private final RuleFilesManager mgr;

	public Extractor(RuleFilesManager mgr) {
		this.mgr = mgr;
	}

	public ClassSet.Visitor newClassSetVisitor() {
		return new ClassSet.Visitor() {
			@Override public boolean visitPackage(String packagePath) throws IOException {
				return true;
			}
			@Override public boolean visitClassFile(ClassBytes classFile) throws IOException {
				return true;
			}
			@Override public void visitClassReader(ClassReader classReader) throws IOException {
				classReader.accept(reusableClassVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
			}
			@Override public void visitPackageEnd() throws IOException {}
		};
	}

	final ClassVisitor reusableClassVisitor = new ClassVisitor() {

		final Collection<String> externals = New.arrayList();
		final Collection<String> appliesToList = New.arrayList();
		final Collection<String> extendsList = New.arrayList();
		final Collection<String> usesList = New.arrayList();
		final Collection<String> allowsList = New.arrayList();
		final Map<String, Collection<String>> lists = New.hashMap();
		{
			lists.put(descOf(AppliesTo.class), appliesToList);
			lists.put(descOf(Extends.class), extendsList);
			lists.put(descOf(Uses.class), usesList);
			lists.put(descOf(Allows.class), allowsList);
		}
		final Set<String> namesSeen = New.hashSet();

		String clsIntName;
		String clsName;
		boolean checked;
		boolean excludesSubPackages;
		boolean initDone;

		void initClassContext() {
			if (initDone)
				return;
			clsName = clsIntName.replace('/', '.');
			externals.clear();
			namesSeen.clear();
			for (Collection<?> c : lists.values())
				c.clear();
			initDone = true;
		}

		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			initDone = false;
			clsIntName = name;
			checked = false;
			excludesSubPackages = false;
		}

		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if (restrictedToDesc.equals(desc))
				return new RestrictionVisitor(null, null);
			else if (checkedDesc.equals(desc))
				checked = true;
			else if (excludesSubPackagesDesc.equals(desc))
				excludesSubPackages = true;
			else {
				final Collection<String> values = lists.get(desc);
				if (null != values) {
					initClassContext();
					return new StringListVisitor(values);
				}
			}
			return null;
		}

		public MethodVisitor visitMethod(int access, final String name, final String desc, String signature,
				String[] exceptions) {
			return new AbstractMethodVisitor() {
				public AnnotationVisitor visitAnnotation(String annDesc, boolean visible) {
					if (restrictedToDesc.equals(annDesc))
						return new RestrictionVisitor(name, desc);
					return null;
				}
			};
		}

		public FieldVisitor visitField(int access, final String name, final String desc, String signature, Object value) {
			return new AbstractFieldVisitor() {
				public AnnotationVisitor visitAnnotation(String annDesc, boolean visible) {
					if (restrictedToDesc.equals(annDesc))
						return new RestrictionVisitor(name, desc);
					return null;
				}
			};
		}

		public void visitEnd() {
			if (checked) {
				initClassContext();
				final boolean isPackage = clsName.endsWith("package-info");
				final String baseName = isPackage ? (clsName.substring(0, clsName.length() - "package-info".length()))
						: clsName;
				final String scope = isPackage ? baseName + (excludesSubPackages ? "*" : "**") : clsName;

				final StringBuilder b = new StringBuilder();
				b.append("comp:\n").append("  ").append(scope).append("\n");
				addList("applies-to", appliesToList, b);
				addList("extends", extendsList, b);
				addList("uses", usesList, b);
				addList("allows", allowsList, b);
				b.append("\n");

				try {
					mgr.internal.update(baseName, b.toString());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			if (initDone && !externals.isEmpty())
				try {
					mgr.external.update(clsName, concat(externals));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
		}

		private void addList(String header, Collection<String> values, StringBuilder b) {
			if (values.isEmpty())
				return;
			b.append(header).append(":\n");
			for (String v : values)
				b.append("  ").append(v).append("\n");
		}

		class StringListVisitor extends AbstractAnnotationVisitor {

			final Collection<String> values;

			public StringListVisitor(Collection<String> values) {
				this.values = values;
			}

			@Override public AnnotationVisitor visitArray(String name) {
				return new AbstractAnnotationVisitor() {
					@Override public void visit(String name, Object value) {
						values.add(value.toString());
					}
				};
			}

		}

		class RestrictionVisitor extends AbstractAnnotationVisitor {

			final List<String> values = new ArrayList<String>();
			final String name;
			final String desc;

			public RestrictionVisitor(String name, String desc) {
				this.name = name;
				this.desc = desc;
			}

			@Override public AnnotationVisitor visitArray(String name) {
				return new AbstractAnnotationVisitor() {
					@Override public void visit(String name, Object value) {
						values.add(value.toString());
					}
				};
			}

			@Override public void visitEnd() {
				initClassContext();
				final String ruleBaseName = (null != name) //
				? "restrict-" + clsName + "-" + name //
						: "restrict-" + clsName;
				String ruleName = ruleBaseName;
				int iName = 2;
				while (!namesSeen.add(ruleName))
					ruleName = ruleBaseName + "-" + (iName++);

				final StringBuilder b = new StringBuilder();
				b.append("rule:\n");
				b.append("  ").append(ruleName).append("\n");
				b.append("applies-to:\n");
				for (String v : values) {
					if (v.endsWith(".*") || v.endsWith(".**"))
						b.append("  ! ").append(v).append("\n");
					else {
						b.append("  ! ").append(v).append("\n");
						b.append("  ! ").append(v).append("$**\n");
					}
				}
				b.append("allows:\n").append("  ! ^");
				if (null == name)
					b.append(Pattern.quote(clsIntName));
				else
					b.append(Pattern.quote(clsIntName + "#" + name + "#" + desc));
				b.append("$\n");
				b.append("\n");
				externals.add(b.toString());
			}

		}

		public void visitSource(String source, String debug) {}
		public void visitOuterClass(String owner, String name, String desc) {}
		public void visitInnerClass(String name, String outerName, String innerName, int access) {}
		public void visitAttribute(Attribute attr) {}
	};

	static String concat(Collection<String> coll) {
		StringBuilder b = new StringBuilder();
		for (String s : coll)
			b.append(s);
		return b.toString();
	}

}
