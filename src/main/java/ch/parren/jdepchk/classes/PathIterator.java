package ch.parren.jdepchk.classes;

import java.io.File;
import java.util.Iterator;

import ch.parren.java.lang.Predicate;
import ch.parren.java.lang.util.FilteringIterator;


public final class PathIterator extends ClassFileIterator {

	private final File path;

	public PathIterator(File path) {
		this.path = path;
	}

	@Override protected File classFilesRoot() {
		return path;
	}
	
	@Override protected Iterable<File> classFiles() {
		return classFiles;
	}

	private final Iterable<File> classFiles = new Iterable<File>() {
		@Override public final Iterator<File> iterator() {
			return new FilteringIterator<File>(new RecursiveFilesIterator(path), new Predicate<File>() {
				@Override public boolean accepts(File tested) {
					return tested.getName().endsWith(".class");
				}
			});
		}
	};

}
