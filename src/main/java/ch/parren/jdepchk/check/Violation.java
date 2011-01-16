package ch.parren.jdepchk.check;

import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.Scope;

public final class Violation {

	public final RuleSet ruleSet;
	public final Scope scope;
	public final String fromClassName;
	public final String toClassName;

	public Violation(RuleSet ruleSet, Scope scope, String fromClassName, String toClassName) {
		this.ruleSet = ruleSet;
		this.scope = scope;
		this.fromClassName = fromClassName;
		this.toClassName = toClassName;
	}

	@Override public String toString() {
		return fromClassName + " > " + toClassName;
	}
	
}
