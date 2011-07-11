import java.io.File;
import java.io.IOException;

import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.ClassSets;
import ch.parren.jdepchk.classes.JarsDirClassSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;

public final class AbaChk {

	public static void main(String[] args) throws Exception {
		final RuleSet rules = makeDemoRules();
		System.out.println(rules.describe());
//		final ClassSet classes = new OutputDirClassSet(new File("/home/peo/dev/aba/trunk/abajava/temp/eclipse"));
		final ClassSets classes = new JarsDirClassSet(true, new File("/home/peo/dev/aba/trunk/abajars/jars/aba/"));
		final ViolationListener listener = new ViolationListener() {
			private int nViol = 0;
			@Override public boolean report(Violation v) {
				System.out.println(v.fromClassName + " > " + v.toClassName //
						+ " in " + v.scope.name() //
						+ " from " + v.ruleSet.name());
				nViol++;
				return true;
			}
			@Override public String toString() {
				return nViol + " violations.";
			}
		};
		final Checker checker = new Checker(listener, rules);
		final long before = System.currentTimeMillis();
		classes.accept(new ClassSets.Visitor() {
			public void visitClassSet(ClassSet classSet) throws IOException {
				classSet.accept(checker.newClassSetVisitor());
			}
		});
		final long after = System.currentTimeMillis();

		System.out.println(listener);
		System.out.println((after - before) + " ms taken.");
		System.out.println(checker.nContains + " containment checks.");
		System.out.println(checker.nSees + " usage checks.");
	}

	private static RuleSet makeDemoRules() {
		final RuleSetBuilder b = new RuleSetBuilder("AbaLib");

		b.scope("ch.abacus.java.**") //
				.allows(b.glob("ch.abacus.**").not() //
						, b.glob("ch.abacus.java.**") //
				) //
		;

		b.scope("ch.abacus.ulc.client.**") //
				.allows(b.glob("ch.abacus.lib.**").not() //
						, b.glob("ch.abacus.lib.swing.**") //
						, b.glob("ch.abacus.lib.awt.**") //
						, b.glob("ch.abacus.lib.net.SocketServer*") //
				) //
		;

		b.scope("ch.abacus.ulc.shared.**") //
				.allows(b.glob("ch.abacus.lib.**").not() //
				) //
		;

		b.scope("!logproc") //
				.contains(b.glob("ch.abacus.server.logproc.server").not() //
				) //
				.allows(b.glob("ch.abacus.server.logproc.server").not() //
				) //
		;

		final RuleSet rules = b.finish();
		return rules;
	}
}
