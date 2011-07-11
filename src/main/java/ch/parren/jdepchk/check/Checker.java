package ch.parren.jdepchk.check;

import java.io.IOException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.Stack;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.classes.ClassBytes;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.RefFinder;
import ch.parren.jdepchk.classes.ClassSet.Visitor;
import ch.parren.jdepchk.classes.RefsOnlyClassParser;
import ch.parren.jdepchk.classes.Visibility;
import ch.parren.jdepchk.classes.asm.ClassReader;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.Scope;

public final class Checker {

	public static boolean useCustomParser = true;
	public static boolean debugOutput = false;

	private final ViolationListener listener;
	private final Iterable<RuleSet> ruleSets;

	public int nContains = 0;
	public int nSees = 0;

	public Checker(ViolationListener listener, RuleSet... ruleSets) {
		this(listener, New.arrayList(ruleSets));
	}

	public Checker(ViolationListener listener, Iterable<RuleSet> ruleSets) {
		this.listener = listener;
		this.ruleSets = ruleSets;
	}

	public Visitor newClassSetVisitor() {
		return new ClassSet.Visitor() {

			private final Stack<Collection<Scope>> scopeSetStack = New.stack();
			private final Collection<Scope> initalScopeSet = New.linkedList();
			{
				for (RuleSet ruleSet : ruleSets)
					for (Scope scope : ruleSet.scopesToCheck())
						initalScopeSet.add(scope);
				scopeSetStack.push(initalScopeSet);
			}

			@Override public boolean visitPackage(String packagePath) {
				final Collection<Scope> scopeSet = scopeSetStack.peek();
				final Collection<Scope> newScopeSet = New.linkedList();
				for (Scope scope : scopeSet)
					if (scope.mightIntersectPackage(packagePath))
						newScopeSet.add(scope);
				if (newScopeSet.isEmpty())
					return false;
				scopeSetStack.push(newScopeSet);
				return true;
			}

			@Override public void visitPackageEnd() throws IOException {
				scopeSetStack.pop();
			}

			private final Collection<Scope> classScopes = New.arrayList();
			private String className;

			@Override public boolean visitClassFile(ClassBytes classFile) throws IOException {
				final String name = classFile.compiledClassName();
				final Collection<Scope> scopeSet = scopeSetStack.peek();
				className = name;
				classScopes.clear();
				for (Scope scope : scopeSet) {
					nContains++;
					if (scope.contains(name))
						classScopes.add(scope);
				}
				return !classScopes.isEmpty();
			}

			@Override public void visitClassBytes(byte[] bytes) throws IOException {
				if (useCustomParser)
					customParse(bytes);
				else
					asmParse(bytes);
			}

			private void customParse(byte[] bytes) throws IOException {
				final RefsOnlyClassParser parser = new RefsOnlyClassParser(bytes);
				final SortedMap<String, Visibility> refs = parser.referencedElementNames();
				check(refs);
			}

			private void asmParse(byte[] bytes) {
				new ClassReader(bytes).accept(refFinder.classVisitor(), 0);
			}
			private final RefFinder refFinder = new RefFinder() {
				@Override public void visitRefs(Visibility ownVisibility, SortedMap<String, Visibility> refs) {
					check(refs);
				}
			};

			private void check(final SortedMap<String, Visibility> refs) {
				for (Scope scope : classScopes) {
					for (String refd : refs.keySet()) {
						nContains++;
						nSees++;
						if (debugOutput)
							System.out.println(refd);
						if (!scope.allows(refd))
							report(scope, className, refd);
					}
				}
			}

			protected boolean report(Scope scope, final String name, String refd) {
				final String[] parts = refd.split("#");
				final String className = parts[0];
				final String eltName = (parts.length > 1) ? parts[1] : null;
				final String eltDesc = (parts.length > 2) ? parts[2] : null;
				return listener.report(new Violation(scope.ruleSet(), scope, name, className, eltName, eltDesc));
			}

		};
	}

}
