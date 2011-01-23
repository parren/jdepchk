package ch.parren.jdepchk.classes;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ch.parren.java.lang.New;

public class ClassParserTest {

	@Test public void parseClass() throws Exception {
		final ClassParser parser = new ClassParser(new File("temp/classes/test-examples/test/ParserTestClass.class"));
		try {
			assertEquals(Visibility.PUBL, parser.visibility());
			final Map<String, Visibility> refMap = parser.referencedClasses();
			final List<String> refs = New.arrayList(refMap.size());
			for (Map.Entry<String, Visibility> e : refMap.entrySet())
				refs.add(e.getKey() + " " + e.getValue().toString().toLowerCase());
			Collections.sort(refs);
			assertEquals("" + //
					"java/io/PrintStream priv\n" + //
					"java/lang/Exception priv\n" + //
					"java/lang/IllegalArgumentException publ\n" + //
					"java/lang/System priv\n" + //
					"java/util/ArrayList priv\n" + //
					"java/util/Set prot\n" + //
					"test/Base publ\n" + //
					"test/Const publ\n" + //
//					"test/ConstAttr publ\n" + // FIXME
//					"test/ClassAttr publ\n" + // FIXME
					"test/Field priv\n" + //
//					"test/FieldAttr priv\n" + // FIXME
					"test/FieldRef priv\n" + // 
					"test/GenericLower publ\n" + //
					"test/GenericLowerMtd prot\n" + //
					"test/GenericUpper prot\n" + //
					"test/IntfA publ\n" + //
					"test/IntfB publ\n" + //
					"test/MethodRef priv\n" + //
					"test/Param1 priv\n" + //
					"test/Param2 priv\n" + //
//					"test/PrivMtdAttr priv\n" + // FIXME
//					"test/PublMtdAttr publ\n" + // FIXME
					"test/Returned priv\n" + //
					"test/StaticMethodRef priv\n" + //
					"test/StaticRef priv\n" + //
					"", toString(refs));
		} finally {
			parser.close();
		}
	}

	private <E> String toString(Collection<E> coll) {
		final StringBuilder b = new StringBuilder();
		for (E e : coll)
			b.append(e).append("\n");
		return b.toString();
	}

}
