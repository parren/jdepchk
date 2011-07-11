package ch.parren.jdepchk.tutorial;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.StringReader;

import org.junit.Test;

import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.ClassesDirClassSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;
import ch.parren.jdepchk.rules.parser.RuleSetLoader;
import ch.parren.jdepchk.rules.parser.StreamParseException;

/**
 * JDepChk is a dependency checker for the Java virtual machine (JVM) class
 * files. As such, it can be used for any code that runs on the JVM.
 */
public class TutorialTest extends AbstractTutorialTest {

	// ---- cite
	/**
	 * The most basic use of JDepChk is via low-level <em>rules</em>. I'll go
	 * into more detail about them later, but here they are in a nutshell.
	 */
	@Test public void rules() throws Exception {

		/**
		 * JDepChk's rules are specified via configuration files. Their
		 * suggested extension is .jdep, but it's really up to you. In these
		 * tests, I will be specifying the file contents directly as String
		 * arrays:
		 */
		final String[] rulesLines = { "// JDepChk rules file", // you can use Java comments in rules files
				"rule: only-core-java", // rules have names so they can be identified in error messages
				"	applies-to:", // rules apply to a particular set of classes
				"		ch.parren.jdepchk.tutorial.examples.**", // specified using a prefix glob here
				"		! ch.parren.jdepchk.tutorial.examples.Excluded", // you can add negative filters using "!"
				"	allows:", // rules then grant access to only another set of classes
				"		java.**", // again via a prefix glob
				"		! java.net.**", // and another, but negated
				"", //
				"rule: containment", "	applies-to:", // we confine the base package to see only itself within the package and its subs
				"		ch.parren.jdepchk.tutorial.examples.*", // specified using a glob that doesn't see subpackages
				"	allows:", //
				"		! ch.parren.jdepchk.tutorial.examples.**", // starting with a negated rule allows everything else by default
				"		xthis", // "this" refers to the "applies-to" or "contains" filter of the current rule
				"", //
		};

		/**
		 * So this simple rule should only allow access to core Java, except for
		 * the networking stuff, for all classes in my examples package, except
		 * for the Ecluded class. Let's check this.
		 */

		/**
		 * JDepCheck parses a single configuration files into something it calls
		 * a {@link RuleSet}.
		 */
		final RuleSet ruleSet = parse(rulesLines);

		/**
		 * We also need to tell it where to find the class files to be checked.
		 * JDepChk can handle dirs with .class files, .jar files, and dirs
		 * scanned for .jar files. Here, we only use a classes dir:
		 */
		final ClassSet classSet = new ClassesDirClassSet(new File("temp/classes/tutorial"));

		/** So we run the check, gathering rule violations into a list: */
		final Checker checker = new Checker(violationsGatherer, ruleSet);
		classSet.accept(checker.newClassSetVisitor());

		/**
		 * The class {@link OnlyCoreJava} should pass, but {@link UsesNet}
		 * should not. Note how JDepChk identifies classes in the JVM's native
		 * form. Also, {@link UsesInner} does not pass the containment check.
		 */
		assertEquals(2, violations.size());
		assertEquals("ch/parren/jdepchk/tutorial/examples/UsesInner", violations.get(0).fromClassName);
		assertEquals("ch/parren/jdepchk/tutorial/examples/UsesNet", violations.get(1).fromClassName);
	}

	/**
	 * Typical Java projects are structured around Java's packages. JDepChk
	 * directly supports this using shorthand notation for components that map
	 * directly to such packages. By <em>component</em>, I mean a collection of
	 * classes that obey the same dependency rules.
	 */
	@Test public void comps() throws Exception {

		/**
		 * JDepChk differentiates between <em>libraries</em> and
		 * <em>components</em>. The classes in components are checked by
		 * JDepChk, those in libraries are not. But libraries can still be
		 * referenced.
		 * 
		 * For example, {@code com.example.foo.ui} might only be allowed to see
		 * core Java, Swing, and {@code com.example.foo.core}. Here are the
		 * corresponding definitions for JDepChk:
		 */
		final String[] rulesLines = { "", //

				/**
				 * Component-based definitions usually start with the
				 * {@code $default} <em>library</em>, which is automatically
				 * referenced by all components. It should contain at least
				 * {@code java.lang.**}.
				 */
				"lib: $default", //
				"  contains:", // this is equivalent to applies-to: for rules; in fact, it's synonymous
				"    java.**  // core Java", //

				/**
				 * We define Swing separately, because only the UI is supposed
				 * to reference it. JDepChk has a shorthand for defining
				 * components that consist of a single package (and optionally
				 * its subpackages). If you end the component or library name
				 * with {@code .*} or {@code .**}, JDepChk automatically defines
				 * the component to contain this pattern and sets its name to
				 * the pattern withouth the suffix:
				 */
				"lib: javax.swing.**  // ", //

				/**
				 * Similarly we can now define the core component of Foo.
				 * Because we give no explicit dependencies, JDepChk will verify
				 * that the classes in the component refer only to what is in
				 * $default.
				 */
				"comp: com.example.foo.core.**", //

				/**
				 * The UI, however, can see $default, and the core and Swing. We
				 * specify this with a {@code uses:} clause, in which you
				 * reference other components by listing their names.
				 * 
				 * Note: With rules we used {@code allows}, not {@uses}.
				 * The former directly specifies patterns, the latter refers to
				 * other components. A component can actually have an
				 * {@code allows} clause too.
				 */
				"comp: com.example.foo.ui.**", //
				"  uses:", //
				"    com.example.foo.core", //
				"    javax.swing", //
		};

		/**
		 * Let's run JDepChk with these component-based definitions now:
		 */
		check(rulesLines, "temp/classes/tutorial");
		assertEquals(2, violations.size());
		assertEquals("com/example/foo/core/BadUIRef", violations.get(0).fromClassName);
		assertEquals("com/example/foo/ui/BadExternalRef", violations.get(1).fromClassName);
	}

	/**
	 * Components often have dependencies in their API. This means that any
	 * component using said API will also depend on what the API references. To
	 * avoid having to repeat these dependencies for every user of the API,
	 * JDepChk supports transient uses. They are written {@code extends:}.
	 * 
	 * Say we have a component XMLSetup that centralizes the parsing of XML for
	 * us. So it returns XML DOMs. Clearly, then, the API depends on Java's XML
	 * support:
	 */
	@Test public void transitive() throws Exception {

		final String[] rulesLines = { "", //
				"lib: $default contains java.**", // we can actually omit the colons
				"", //
				"lib: javax.xml.**", // we can use the shorthand ...
				"  contains: org.w3c.**", // ... plus further imports

				/**
				 * This component extends javax.xml. Now every component that
				 * uses or extends com.example.xml automatically uses javax.xml
				 * too.
				 */
				"comp: com.example.xml.**", //
				"  extends: javax.xml", //

				/** ... as can be seen here. */
				"comp: com.example.usesxml.**", //
				"  uses: com.example.xml", //
		};

		/** This works: */
		check(rulesLines, "temp/classes/tutorial");
		assertEquals(0, violations.size());

		/** If we try the same thing without extends", we get errors: */
		check(replaceIn(rulesLines, "extends", "uses"), "temp/classes/tutorial");
		assertEquals(2, violations.size());
		assertEquals("com/example/usesxml/UseXML", violations.get(0).fromClassName);
		assertEquals("javax/xml/validation/Schema", violations.get(0).toClassName);
		assertEquals("com/example/usesxml/UseXML", violations.get(1).fromClassName);
		assertEquals("org/w3c/dom/Node", violations.get(1).toClassName);
	}

	/**
	 * In the above example, we can already feel that it can be become tedious
	 * to have to repeat the common package namespace prefix, com.example, over
	 * an over again - especially if it gets a little longer. JDepChk supports
	 * abbreviations for this:
	 */
	@Test public void abbreviations() throws Exception {

		final String[] rulesLines = { "", //
				"lib: $default contains java.**", // we can actually omit the colons
				"lib: javax.xml.**", // we can use the shorthand ...
				"  contains: org.w3c.**", // ... plus further imports

				/**
				 * Define the abbreviation in the form placeholder=value. The
				 * text and the value can be any identifier (regexp:
				 * [a-zA-Z0-9.*$-_]+). The convention, however, is to use a "$"
				 * as the prefix for the placeholder.
				 */
				"def: $e=com.example", //

				/**
				 * Use the abbreviation. It is textually substituted in all
				 * identifiers.
				 */
				"comp: $e.xml.**", //
				"  uses: javax.xml", //
				"comp: $e.usesxml.**", //
				"  uses: $e.xml", //
		};

		/**
		 * This works, meaning it does catch the expected errors (we did not
		 * extend but only use here):
		 */
		check(rulesLines, "temp/classes/tutorial");
		assertEquals(2, violations.size());
		assertEquals("com/example/usesxml/UseXML", violations.get(0).fromClassName);
		assertEquals("javax/xml/validation/Schema", violations.get(0).toClassName);
		assertEquals("com/example/usesxml/UseXML", violations.get(1).fromClassName);
		assertEquals("org/w3c/dom/Node", violations.get(1).toClassName);
	}
	// ---- cite

	/**
	 * Sometimes you want to forbid access to only particulars members of types,
	 * not the entire types as such. JDepChk supports this via special rules.
	 * For example, we want to forbid access to {@link String#getBytes()} to
	 * avoid problems with differing platform charsets. Currently, JDepChk only
	 * supports this via full-blown regexp patterns against the internal
	 * representation of a member:
	 */
	@Test public void memberAccessViaRegExps() throws Exception {

		final String[] rulesLines = { "", //
				"lib: $default", //
				"  contains:", //
				"    java.**", //

				/**
				 * Deny access to String.getBytes() here. A regexp pattern is
				 * introduced by ^ and ends with a $. The internal
				 * representation of "byte[] java.lang.String.getBytes()" is
				 * "java/lang/String#getBytes#()[B", so we match against that,
				 * ignoring the trailing return type spec:
				 */
				"    ! ^java/lang/String#getBytes#[(][)].*$", //

				/**
				 * The above is clearly rather unreadable. I would prefer to be
				 * able to write something like
				 * {@code !java.lang.String::getBytes()>*}. However, since I
				 * don't expect a lot of folks to be using this, I'm not
				 * bothering right now.
				 */

				"comp: com.example.members.**", //
		};

		check(rulesLines, "temp/classes/tutorial");
		assertEquals(1, violations.size());
		assertEquals("com/example/members/UsesBytes", violations.get(0).fromClassName);
		assertEquals("java/lang/String", violations.get(0).toClassName);
	}

	private final void check(String[] rulesLines, String classDirName) throws Exception {
		final RuleSet ruleSet = parse(rulesLines);
		final ClassSet classSet = new ClassesDirClassSet(new File(classDirName));
		final Checker checker = new Checker(violationsGatherer, ruleSet);
		classSet.accept(checker.newClassSetVisitor());
	}

	private final RuleSet parse(String[] rulesLines) {
		final StringBuilder text = new StringBuilder();
		for (String line : rulesLines)
			text.append(line).append("\n");
		final RuleSetBuilder builder = new RuleSetBuilder("tutorial");
		try {
			RuleSetLoader.loadInto(new StringReader(text.toString()), builder);
		} catch (StreamParseException e) {
			throw new RuntimeException(e);
		}
		return builder.finish();
	}

}
