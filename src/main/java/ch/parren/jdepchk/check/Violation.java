package ch.parren.jdepchk.check;

import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.Scope;

public final class Violation {

	public final RuleSet ruleSet;
	public final Scope scope;
	public final String fromClassName;
	public final String toClassName;
	public final String toElementName;
	public final String toElementDesc;

	public Violation(RuleSet ruleSet, Scope scope, String fromClassName, String toClassName, String toElementName,
			String toElementDesc) {
		this.ruleSet = ruleSet;
		this.scope = scope;
		this.fromClassName = fromClassName;
		this.toClassName = toClassName;
		this.toElementName = toElementName;
		this.toElementDesc = toElementDesc;
	}

	@Override public String toString() {
		return fromClassName + " > " + toClassName
				+ ((null == toElementName) ? "" : "#" + toElementName + "#" + toElementDesc);
	}

}
