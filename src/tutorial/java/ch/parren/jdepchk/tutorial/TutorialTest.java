package ch.parren.jdepchk.tutorial;

import static org.junit.Assert.*;

import java.io.File;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.ClassesDirClassSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;
import ch.parren.jdepchk.rules.parser.RuleSetLoader;

/**
 * JDepChk is a dependency checker for the Java virtual machine (JVM) class
 * files. As such, it can be used for any code that runs on the JVM.
 */
public class TutorialTest {

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
				"", //
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
		checker.check(classSet);

		/**
		 * The class {@link OnlyCoreJava} should pass, but {@link UsesNet}
		 * should not. Note how JDepChk identifies classes in the JVM's native
		 * form.
		 */
		assertEquals(1, violations.size());
		assertEquals("ch/parren/jdepchk/tutorial/examples/UsesNet", violations.get(0).fromClassName);
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
		assertEquals("org/w3c/dom/Node", violations.get(0).toClassName);
		assertEquals("com/example/usesxml/UseXML", violations.get(1).fromClassName);
		assertEquals("javax/xml/validation/Schema", violations.get(1).toClassName);
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

		/** This works, meaning it does catch the expected errors (we did not extend but only use here): */
		check(rulesLines, "temp/classes/tutorial");
		assertEquals(2, violations.size());
		assertEquals("com/example/usesxml/UseXML", violations.get(0).fromClassName);
		assertEquals("org/w3c/dom/Node", violations.get(0).toClassName);
		assertEquals("com/example/usesxml/UseXML", violations.get(1).fromClassName);
		assertEquals("javax/xml/validation/Schema", violations.get(1).toClassName);
	}
	// ---- cite

	private String[] replaceIn(String[] lines, String what, String with) {
		final String[] res = new String[lines.length];
		for (int i = 0; i < lines.length; i++)
			res[i] = lines[i].replace(what, with);
		return res;
	}

	private void check(String[] rulesLines, String classDirName) throws Exception {
		final RuleSet ruleSet = parse(rulesLines);
		final ClassSet classSet = new ClassesDirClassSet(new File(classDirName));
		final Checker checker = new Checker(violationsGatherer, ruleSet);
		checker.check(classSet);
	}

	private RuleSet parse(String[] rulesLines) {
		final StringBuilder text = new StringBuilder();
		for (String line : rulesLines)
			text.append(line).append("\n");
		final RuleSetBuilder builder = new RuleSetBuilder("tutorial");
		RuleSetLoader.loadInto(new StringReader(text.toString()), builder);
		return builder.finish();
	}

	private final ViolationListener violationsGatherer = new ViolationListener() {
		@Override protected boolean report(Violation v) {
			violations.add(v);
			return true;
		}
	};
	private final List<Violation> violations = New.arrayList();

}
