package ch.parren.jdepchk.check;

import java.io.IOException;

import ch.parren.jdepchk.classes.ClassFile;
import ch.parren.jdepchk.classes.ClassFileIterator;
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

	public void check(ClassFileIterator classes) throws IOException {
		for (ClassFile cls : classes) {
			try {
				for (RuleSet ruleSet : ruleSets) {
					for (Scope scope : ruleSet.scopesToCheck()) {
						final String name = cls.compiledClassName();
						nContains++;
						if (scope.contains(name)) {
							// System.out.println(name + " is in " + scope);
							for (String refd : cls.referencedClassNames()) {
								nContains++;
								if (!scope.contains(refd)) {
									// System.out.println("  refs " + refd);
									nSees++;
									if (!scope.sees(refd))
										listener.report(new Violation(ruleSet, scope, name, refd));
								}
							}
						}
					}
				}
			} finally {
				cls.close();
			}
		}
	}

}
