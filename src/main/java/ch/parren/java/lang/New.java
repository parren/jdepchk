package ch.parren.java.lang;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 * Defines static constructor methods for collections to make use of Java's
 * limited type inference.
 */
public final class New {

	public static <K, V> HashMap<K, V> hashMap() {
		return new HashMap<K, V>();
	}

	public static <E> HashSet<E> hashSet() {
		return new HashSet<E>();
	}

	public static <E> LinkedHashSet<E> linkedHashSet() {
		return new LinkedHashSet<E>();
	}

	public static <E> LinkedList<E> linkedList() {
		return new LinkedList<E>();
	}

	public static <E> ArrayList<E> arrayList() {
		return new ArrayList<E>();
	}

	public static <E> ArrayList<E> arrayList(Collection<? extends E> es) {
		return new ArrayList<E>(es);
	}

	public static <E, A extends E> ArrayList<E> arrayList(A... es) {
		final int n = es.length;
		final ArrayList<E> res = new ArrayList<E>(n);
		for (int i = 0; i < n; i++)
			res.add(es[i]);
		return res;
	}

	public static <E> ArrayList<E> arrayList(int size) {
		return new ArrayList<E>(size);
	}

	public static <E> Deque<E> linkedDeque() {
		return new LinkedList<E>();
	}

	public static <E> Deque<E> arrayDeque() {
		return new ArrayDeque<E>();
	}

}
