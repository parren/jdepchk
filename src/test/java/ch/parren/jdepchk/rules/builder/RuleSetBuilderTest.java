package ch.parren.jdepchk.rules.builder;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class RuleSetBuilderTest {

	@Test public void singleStarPattern() {
		final Pattern p = RuleSetBuilder.globToPattern("foo.bar.*");
		assertFalse(p.matcher("foo/Class").matches());
		assertFalse(p.matcher("test/foo/bar/Class").matches());
		assertFalse(p.matcher("foo/bar/baz/Class").matches());
		assertTrue(p.matcher("foo/bar/Class").matches());
		assertTrue(p.matcher("foo/bar/Class$InnerClass").matches());
		assertTrue(p.matcher("foo/bar/Class$InnerClass#elt#type(Ljava/lang/String;)V").matches());
	}

	@Test public void doubleStarPattern() {
		final Pattern p = RuleSetBuilder.globToPattern("foo.bar.**");
		assertFalse(p.matcher("foo/Class").matches());
		assertFalse(p.matcher("test/foo/bar/Class").matches());
		assertTrue(p.matcher("foo/bar/baz/Class").matches());
		assertTrue(p.matcher("foo/bar/baz/Class#elt#type(Ljava/lang/String;)V").matches());
		assertTrue(p.matcher("foo/bar/Class").matches());
		assertTrue(p.matcher("foo/bar/Class$InnerClass").matches());
		assertTrue(p.matcher("foo/bar/Class$InnerClass#elt#type(Ljava/lang/String;)V").matches());
	}

}
