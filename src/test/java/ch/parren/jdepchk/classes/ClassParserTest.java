package ch.parren.jdepchk.classes;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.junit.Test;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.classes.asm.ClassReader;

public class ClassParserTest {

	@Test public void testClass() throws Exception {
		assertParse("TestClass", "" + //
				"java/io/PrintStream priv\n" + //
				"java/io/PrintStream#println#()V priv\n" + //
				"java/lang/Exception priv\n" + //
				"java/lang/IllegalArgumentException publ\n" + //
				"java/lang/Integer priv\n" + //
				"java/lang/Object publ\n" + //
				"java/lang/System priv\n" + //
				"java/lang/System#out#Ljava/io/PrintStream; priv\n" + //
				"java/util/ArrayList priv\n" + //
				"java/util/ArrayList#<init>#()V priv\n" + //
				"java/util/Map priv\n" + //
				"java/util/Set prot\n" + //
				"test/Base publ\n" + //
				"test/Base#<init>#()V priv\n" + //
				"test/Const publ\n" + //
//					"test/ConstAttr publ\n" + // FIXME
//					"test/ClassAttr publ\n" + // FIXME
				"test/Field priv\n" + //
//					"test/FieldAttr priv\n" + // FIXME
				"test/FieldRef priv\n" + // 
				"test/FieldRef#value#I priv\n" + // 
				"test/GenericLower publ\n" + //
				"test/GenericLower1 publ\n" + //
				"test/GenericLower2 publ\n" + //
				"test/GenericLower3 publ\n" + //
				"test/GenericLowerMtd prot\n" + //
				"test/GenericUpper1 prot\n" + //
				"test/GenericUpper2 priv\n" + //
				"test/IntfA publ\n" + //
				"test/IntfB publ\n" + //
				"test/MethodRef priv\n" + //
				"test/MethodRef#bar#()Z priv\n" + //
				"test/Param1 priv\n" + //
				"test/Param2 priv\n" + //
//					"test/PrivMtdAttr priv\n" + // FIXME
//					"test/PublMtdAttr publ\n" + // FIXME
				"test/Returned priv\n" + //
				"test/StaticMethodRef priv\n" + //
				"test/StaticMethodRef#foo#()Z priv\n" + //
				"test/StaticRef priv\n" + //
				"test/StaticRef#value#I priv\n" + //
				"test/TestClass#CONST#Ltest/Const; priv\n" + //
				"test/TestClass#field#Ltest/Field; priv\n" + //
				"test/TestClass#map#Ljava/util/Map; priv\n" + //
				"test/TestClass#set#Ljava/util/Set; priv\n" + //
				"test/TestClass$Gen priv\n" + //
				"test/TestClass$Lala priv\n" + //
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

	private void assertParse(String className, final String refd) throws Exception {
		final File file = new File("temp/classes/test-examples/test/" + className + ".class");
		final BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
		try {
			final ClassReader reader = new ClassReader(stream);
			final RefFinder finder = new RefFinder() {
				@Override public void visitRefs(Visibility vis, SortedMap<String, Visibility> refs) {
					assertEquals(Visibility.PUBL, vis);
					final List<String> sortedRefs = New.arrayList();
					for (Map.Entry<String, Visibility> e : refs.entrySet())
						sortedRefs.add(e.getKey() + " " + e.getValue().toString().toLowerCase());
					Collections.sort(sortedRefs);
					assertEquals(refd, collToString(sortedRefs));
				}
				private <E> String collToString(Collection<E> coll) {
					final StringBuilder b = new StringBuilder();
					for (E e : coll)
						b.append(e).append("\n");
					return b.toString();
				}
			};
			reader.accept(finder.classVisitor(), 0);
		} finally {
			stream.close();
		}
	}

}
