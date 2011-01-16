package ch.parren.java.lang.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import ch.parren.java.lang.Predicate;

public final class FilteringIterator<T> implements Iterator<T> {

	private final Iterator<? extends T> base;
	private final Predicate<? super T> filter;

	private T next;

	public FilteringIterator(Iterator<? extends T> base, Predicate<? super T> filter) {
		this.base = base;
		this.filter = filter;
		step();
	}

	@Override public boolean hasNext() {
		return (null != next);
	}

	@Override public T next() {
		if (null == next)
			throw new NoSuchElementException();
		final T result = next;
		step();
		return result;
	}

	@Override public void remove() {
		throw new UnsupportedOperationException();
	}

	private void step() {
		while (base.hasNext()) {
			next = base.next();
			if (filter.accepts(next))
				return;
		}
		next = null;
	}

}
