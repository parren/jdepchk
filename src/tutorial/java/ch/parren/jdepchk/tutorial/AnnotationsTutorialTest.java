package ch.parren.jdepchk.tutorial;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.StringReader;

import org.junit.Test;

import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.ClassesDirClassSet;
import ch.parren.jdepchk.extraction.Extractor;
import ch.parren.jdepchk.extraction.RuleFilesManager;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;
import ch.parren.jdepchk.rules.parser.RuleSetLoader;
import ch.parren.jdepchk.rules.parser.StreamParseException;

/**
 * Tests which demonstrate the extraction of package and type annotations to
 * define the configuration for JDepChk.
 */
public class AnnotationsTutorialTest extends AbstractTutorialTest {

	/**
	 * Instead of specifying dependency rules centrally in configuration files,
	 * you can also place them closer to the code in annotations. Package-level
	 * rules go into annotations on the package in {@code package-info.java}
	 * files.
	 */
	@Test public void comps() throws Exception {

		/**
		 * We instruct JDepExtract to extract the remaining configuration from
		 * the annotations. It places them in two separate folders, one for
		 * rules which self-test the current project, and another for rules
		 * which should be enforced in clients of the current project too.
		 */
		final File internal = new File("temp/rules/internal");
		final File external = new File("temp/rules/external");
		deleteRecursively(internal);
		deleteRecursively(external);
		final RuleFilesManager mgr = new RuleFilesManager(internal, external, "", false);
		final Extractor extractor = new Extractor(mgr);
		final String classDirName = "temp/classes/tutorial";
		final ClassSet classSet = new ClassesDirClassSet(new File(classDirName));
		classSet.accept(extractor.newClassSetVisitor());

		/**
		 * JDepExtract writes the extracted rules to separate files, one per
		 * context (class or package-info) where the rules were specified.
		 */
		assertContains(new File(internal, "com.example.foo.core."), // 
				"comp:", //
				"  com.example.foo.core.**", //
				"");
		assertContains(new File(internal, "com.example.foo.ui."), //
				"comp:", //
				"  com.example.foo.ui.**", //
				"uses:", //
				"  com.example.foo.core", //
				"  javax.swing", //
				"");
		assertContains(new File(external, "com.example.foo.core.CoreClass"), //
				"rule:", //
				"  restrict-com.example.foo.core.CoreClass-bar", //
				"applies-to:", //
				"  ! com.example.foo.ui.UIClass", //
				"  ! com.example.foo.ui.UIClass$**", //
				"allows:", //
				"  ! ^\\Qcom/example/foo/core/CoreClass#bar#[I\\E$", //
				"", //
				"rule:", //
				"  restrict-com.example.foo.core.CoreClass-foo", //
				"applies-to:", //
				"  ! com.example.foo.ui.UIClass", //
				"  ! com.example.foo.ui.UIClass$**", //
				"allows:", //
				"  ! ^\\Qcom/example/foo/core/CoreClass#foo#([Ljava/lang/String;[[I)V\\E$", //
				"", //
				"rule:", //
				"  restrict-com.example.foo.core.CoreClass-foo-2", //
				"applies-to:", //
				"  ! com.example.foo.ui.UIClass", //
				"  ! com.example.foo.ui.UIClass$**", //
				"allows:", //
				"  ! ^\\Qcom/example/foo/core/CoreClass#foo#()I\\E$", //
				"");

		/**
		 * You will typically still have a small base configuration file where
		 * you define external libraries and the default component.
		 */
		final String[] rulesLines = { "", //
				"lib: $default", //
				"  contains:", //
				"    java.**  // core Java", //
				"lib: javax.swing.**", //
		};

		/**
		 * Let's run JDepChk with this base file and the extracted definitions
		 * now:
		 */
		final StringBuilder text = new StringBuilder();
		for (String line : rulesLines)
			text.append(line).append("\n");
		final RuleSetBuilder intBuilder = new RuleSetBuilder("internal");
		final RuleSetBuilder extBuilder = new RuleSetBuilder("external");
		try {
			RuleSetLoader.loadInto(new StringReader(text.toString()), intBuilder);
			for (File f : internal.listFiles())
				RuleSetLoader.loadInto(f, intBuilder);
			for (File f : external.listFiles())
				RuleSetLoader.loadInto(f, extBuilder);
		} catch (StreamParseException e) {
			throw new RuntimeException(e);
		}
		final RuleSet intSet = intBuilder.finish();
		final RuleSet extSet = extBuilder.finish();

		final Checker checker = new Checker(violationsGatherer, intSet, extSet);
		classSet.accept(checker.newClassSetVisitor());
		assertEquals(3, violations.size());
		assertEquals("com/example/foo/core/BadUIRef", violations.get(0).fromClassName);
		assertEquals("com/example/foo/ui/BadExternalRef", violations.get(1).fromClassName);
		assertEquals("com/example/foo/ui/BadRestrictedRef", violations.get(2).fromClassName);
	}

}
