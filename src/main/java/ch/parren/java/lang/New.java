package ch.parren.java.lang;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;

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

	public static <E> ArrayList<E> arrayList(Collection<? extends E> patterns) {
		return new ArrayList<E>(patterns);
	}

	public static <E> Deque<E> linkedDeque() {
		return new LinkedList<E>();
	}

	public static <E> Deque<E> arrayDeque() {
		return new ArrayDeque<E>();
	}

}
