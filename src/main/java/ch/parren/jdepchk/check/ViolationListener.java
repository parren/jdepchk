package ch.parren.jdepchk.check;

public abstract class ViolationListener {

	abstract protected boolean report(Violation v);
	
}
