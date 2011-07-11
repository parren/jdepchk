
import java.io.File;

import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.AbstractClassScanner;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.ClassesDirClassSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.parser.RuleSetLoader;

public final class SelfChk {

	public static void main(String[] args) throws Exception {
		final RuleSet rules = RuleSetLoader.load(new File("src/main/rules.jdep"));
//		System.out.print(rules.describe());
		
		final ClassSet classes = new ClassesDirClassSet(new File("temp/classes/main"));
		final ViolationListener listener = new ViolationListener() {
			private int nViol = 0;
			@Override protected boolean report(Violation v) {
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
		checker.check(classes);
		final long after = System.currentTimeMillis();

		System.out.println(listener);
		System.out.println((after - before) + " ms taken.");
		System.out.println(checker.nContains + " containment checks.");
		System.out.println(checker.nSees + " usage checks.");
		System.out.println(AbstractClassScanner.nFilesRead + " class files read.");
	}

}
