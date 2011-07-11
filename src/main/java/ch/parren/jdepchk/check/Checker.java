package ch.parren.jdepchk.check;

import java.io.IOException;
import java.util.Collection;
import java.util.Stack;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.classes.ClassScanner;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.Scope;

public final class Checker {

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

	public void check(ClassSet classes) throws IOException {
		final Stack<Collection<Scope>> scopeSetStack = New.stack();

		final Collection<Scope> initalScopeSet = New.linkedList();
		for (RuleSet ruleSet : ruleSets)
			for (Scope scope : ruleSet.scopesToCheck())
				initalScopeSet.add(scope);
		scopeSetStack.push(initalScopeSet);

		classes.accept(new ClassSet.Visitor() {

			/* @Override */public boolean visitPackage(String packagePath) {
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

			/* @Override */public void visitPackageEnd() throws IOException {
				scopeSetStack.pop();
			}

			/* @Override */public void visitClassFile(ClassScanner classFile) throws IOException {
				final Collection<Scope> scopeSet = scopeSetStack.peek();
				for (Scope scope : scopeSet) {
					final String name = classFile.compiledClassName();
					nContains++;
					if (scope.contains(name)) {
						for (String refd : classFile.referencedElementNames()) {
							nContains++;
							nSees++;
							if (debugOutput)
								System.out.println(refd);
							if (!scope.allows(refd))
								report(scope, name, refd);
						}
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

		});
	}

}
