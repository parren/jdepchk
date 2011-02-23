package ch.parren.jdepchk;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.ClassesDirClassSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;

public class JDepChkTest {

	@Test public void allRules() throws Exception {
		final RuleSet rules = makeDemoRules();
		final ClassesDirClassSet classes = new ClassesDirClassSet(new File("temp/classes/test-examples"));
		final StringBuilder violations = new StringBuilder();
		final ViolationListener listener = new ViolationListener() {
			@Override protected boolean report(Violation v) {
				violations.append(v).append("\n");
				return true;
			}
		};
		final Checker checker = new Checker(listener, rules);
		checker.check(classes);
		assertEquals("" + //
				"user/MyUser > api/impl/MyImpl\n" + //
				"api/BadRefByIntf > api/impl/MyImpl\n" + //
				"api/BadRefByClass > api/impl/MyImpl\n" //
		, violations.toString());
	}

	private RuleSet makeDemoRules() {
		final RuleSetBuilder b = new RuleSetBuilder("demo");

		b.lib("$default") //
				.contains(b.glob("java.**")) //
		;
		b.comp("api.*") //
				.use("api.impl") //
		;
		b.comp("api.impl.**") //
				.extend("api") //
		;
		b.comp("api.impl.one.**") //
				.extend("api") //
		;
		b.comp("api.impl.two.**") //
				.extend("api") //
				.use("api.impl.one") //
		;
		b.comp("user.**") //
				.use("api") //
		;

		final RuleSet rules = b.finish();
		return rules;
	}

}
