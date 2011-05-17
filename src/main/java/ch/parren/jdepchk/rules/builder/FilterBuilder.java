package ch.parren.jdepchk.rules.builder;

import ch.parren.jdepchk.rules.ClassFileFilter;
import ch.parren.jdepchk.rules.InverseClassFileFilter;

public final class FilterBuilder {

	public static final FilterBuilder SELFREF = new FilterBuilder(null);
	
	final ClassFileFilter filter;

	public FilterBuilder(ClassFileFilter filter) {
		this.filter = filter;
	}

	public FilterBuilder not() {
		return new FilterBuilder(new InverseClassFileFilter(filter));
	}

	boolean defaultValue() {
		return filter instanceof InverseClassFileFilter ? true : false;
	}

}
