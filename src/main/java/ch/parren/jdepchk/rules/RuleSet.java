package ch.parren.jdepchk.rules;

import java.util.Collection;

import ch.parren.java.lang.New;

public final class RuleSet {

	private final Collection<Scope> scopesToCheck = New.linkedList();
	private final String name;

	public RuleSet(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	public void check(Scope scope) {
		scopesToCheck.add(scope);
	}

	public Iterable<Scope> scopesToCheck() {
		return scopesToCheck;
	}

	@Override public String toString() {
		return "rule set " + name;
	}

	public void describe(StringBuilder to, String indent) {
		to.append(indent).append(this).append('\n');
		for (Scope s : scopesToCheck)
			s.describe(to, indent + '\t');
	}

	public String describe() {
		final StringBuilder b = new StringBuilder();
		describe(b, "");
		return b.toString();
	}
	
}
