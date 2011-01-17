package ch.parren.jdepchk;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.PathClassFileSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.RuleSetBuilder;

public class JDepChkTest {

	@Test public void allRules() throws Exception {
		final RuleSet rules = makeDemoRules();
		final PathClassFileSet classes = new PathClassFileSet(new File("temp/classes/test-examples"));
		final StringBuilder violations = new StringBuilder();
		final ViolationListener listener = new ViolationListener() {
			@Override protected boolean report(Violation v) {
				violations.append(v).append("\n");
				return true;
			}
		};
		final Checker checker = new Checker(listener, rules);
		checker.check(classes);
		assertEquals("...", violations.toString());
	}

	private RuleSet makeDemoRules() {
		final RuleSetBuilder b = new RuleSetBuilder("demo");

		b.lib("$default") //
				.packages("java.**") //
		;
		b.scope("api") //
				.packages("api.**") //
				.use("api.impl") //
		;
		b.scope("api.impl") //
				.impliedPackages() //
				.extend("api") //
		;
		b.scope("api.impl.one") //
				.impliedPackages() //
				.extend("api") //
		;
		b.scope("api.impl.two") //
				.impliedPackages() //
				.extend("api") //
				.use("api.impl.one") //
		;
		b.scope("user") //
				.impliedPackages() //
				.use("api") //
		;

		final RuleSet rules = b.finish();
		return rules;
	}

}
