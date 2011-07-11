package ch.parren.jdepchk.classes;

import java.io.Closeable;
import java.util.SortedMap;

public interface ClassParser extends Closeable {

	SortedMap<String, Visibility> referencedElementNames();
	Visibility visibility();

}
