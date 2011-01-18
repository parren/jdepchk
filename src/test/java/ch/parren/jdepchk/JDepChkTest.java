package ch.parren.jdepchk;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.OutputDirClassSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;

public class JDepChkTest {

	@Test public void allRules() throws Exception {
		final RuleSet rules = makeDemoRules();
		final OutputDirClassSet classes = new OutputDirClassSet(new File("temp/classes/test-examples"));
		final StringBuilder violations = new StringBuilder();
		final ViolationListener listener = new ViolationListener() {
			@Override protected boolean report(Violation v) {
				violations.append(v).append("\n");
				return true;
			}
		};
		final Checker checker = new Checker(listener, rules);
		checker.check(classes);
		assertEquals("user/MyUser > api/impl/MyImpl\n", violations.toString());
	}

	private RuleSet makeDemoRules() {
		final RuleSetBuilder b = new RuleSetBuilder("demo");

		b.lib("$default") //
				.contains(b.glob("java.**")) //
		;
		b.comp("api") //
				.contains(b.glob("api.*")) //
				.use("api.impl") //
		;
		b.comp("api.impl") //
				.impliedPackages() //
				.extend("api") //
		;
		b.comp("api.impl.one") //
				.impliedPackages() //
				.extend("api") //
		;
		b.comp("api.impl.two") //
				.impliedPackages() //
				.extend("api") //
				.use("api.impl.one") //
		;
		b.comp("user") //
				.impliedPackages() //
				.use("api") //
		;

		final RuleSet rules = b.finish();
		return rules;
	}

}
