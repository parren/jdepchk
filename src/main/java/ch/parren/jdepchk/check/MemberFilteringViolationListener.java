package ch.parren.jdepchk.check;

import java.util.Set;

import ch.parren.java.lang.New;

/** Avoids showing multiple refs to the same class. */
public final class MemberFilteringViolationListener extends ViolationListener {

	private final ViolationListener wrapped;
	private String lastFrom = null;
	private Set<String> seenTos;

	public MemberFilteringViolationListener(ViolationListener wrapped) {
		this.wrapped = wrapped;
	}

	@Override final public boolean report(Violation v) {

		if (!v.fromClassName.equals(lastFrom)) {
			lastFrom = v.fromClassName;
			seenTos = New.hashSet();
		}
		if (seenTos.contains(v.toClassName))
			return true;
		if (null == v.toElementName)
			seenTos.add(v.toClassName);

		return wrapped.report(v);
	}

}
