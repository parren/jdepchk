package ch.parren.jdepchk.check;

import java.io.IOException;
import java.util.Collection;
import java.util.Deque;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.classes.ClassFile;
import ch.parren.jdepchk.classes.ClassFileSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.Scope;

public final class Checker {

	private final ViolationListener listener;
	private final RuleSet[] ruleSets;

	public int nContains = 0;
	public int nSees = 0;

	public Checker(ViolationListener listener, RuleSet... ruleSets) {
		this.listener = listener;
		this.ruleSets = ruleSets;
	}

	public void check(ClassFileSet classes) throws IOException {
		final Deque<Collection<Scope>> scopeSetStack = New.arrayDeque();

		final Collection<Scope> initalScopeSet = New.linkedList();
		for (RuleSet ruleSet : ruleSets)
			for (Scope scope : ruleSet.scopesToCheck())
				initalScopeSet.add(scope);
		scopeSetStack.push(initalScopeSet);

		classes.accept(new ClassFileSet.Visitor() {

			@Override public boolean visitPackage(String packagePath) {
				final Collection<Scope> scopeSet = scopeSetStack.peekLast();
				final Collection<Scope> newScopeSet = New.linkedList();
				for (Scope scope : scopeSet)
					if (scope.mightIntersectPackage(packagePath))
						newScopeSet.add(scope);
				if (newScopeSet.isEmpty())
					return false;
				scopeSetStack.addLast(newScopeSet);
				return true;
			}

			@Override public void visitPackageEnd() throws IOException {
				scopeSetStack.removeLast();
			}

			@Override public void visitClassFile(ClassFile classFile) throws IOException {
				final Collection<Scope> scopeSet = scopeSetStack.peekLast();
				for (Scope scope : scopeSet) {
					final String name = classFile.compiledClassName();
					nContains++;
					if (scope.contains(name)) {
						// System.out.println(name + " is in " + scope);
						for (String refd : classFile.referencedClassNames()) {
							nContains++;
							if (!scope.contains(refd)) {
								// System.out.println("  refs " + refd);
								nSees++;
								if (!scope.sees(refd))
									listener.report(new Violation(scope.ruleSet(), scope, name, refd));
							}
						}
					}
				}
			}

		});
	}

}
