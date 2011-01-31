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

	@Test public void testClass() throws Exception {
		assertParse("TestClass", "" + //
				"java/io/PrintStream priv\n" + //
				"java/lang/Exception priv\n" + //
				"java/lang/IllegalArgumentException publ\n" + //
				"java/lang/Object publ\n" + //
				"java/lang/System priv\n" + //
				"java/util/ArrayList priv\n" + //
				"java/util/Map priv\n" + //
				"java/util/Set prot\n" + //
				"test/Base publ\n" + //
				"test/Const publ\n" + //
//					"test/ConstAttr publ\n" + // FIXME
//					"test/ClassAttr publ\n" + // FIXME
				"test/Field priv\n" + //
//					"test/FieldAttr priv\n" + // FIXME
				"test/FieldRef priv\n" + // 
				"test/GenericLower publ\n" + //
				"test/GenericLower2 priv\n" + //
				"test/GenericLowerMtd prot\n" + //
				"test/GenericUpper1 prot\n" + //
				"test/GenericUpper2 priv\n" + //
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
				"");
	}

	@Test public void genIntf() throws Exception {
		assertParse("TestGenIntf", "" + //
				"java/lang/Object publ\n" + //
				"java/util/Set publ\n" + //
				"test/A publ\n" + //
				"test/B publ\n" + //
				"test/C publ\n" + //
				"test/D publ\n" + //
				"test/E publ\n" + //
				"");
	}

	private void assertParse(String className, String refd) throws Exception {
		final ClassParser parser = new ClassParser(new File("temp/classes/test-examples/test/" + className + ".class"));
		try {
			assertEquals(Visibility.PUBL, parser.visibility());
			final Map<String, Visibility> refMap = parser.referencedClasses();
			final List<String> refs = New.arrayList(refMap.size());
			for (Map.Entry<String, Visibility> e : refMap.entrySet())
				refs.add(e.getKey() + " " + e.getValue().toString().toLowerCase());
			Collections.sort(refs);
			assertEquals(refd, toString(refs));
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
