
import java.io.File;

import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.ClassParser;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.JarPathClassSet;
import ch.parren.jdepchk.classes.OutputDirClassSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;

/**
 * Package and class dependency checker for the JVM. Operates on compiled JVM
 * classes. Key planned features:
 * <ul>
 * <li>Multiple rule sets, loadable from different files.</li>
 * <li>Multiple class sets with different combinations of rule sets.</li>
 * <li>Rule sets are held in memory, but classes are loaded one by one.</li>
 * <li>RegExp-based matching.</li>
 * <li>Syntactic sugar for public/private visibility scopes.</li>
 * <li>Export to graphviz.</li>
 * <li>Support for transient and permanent exceptions.</li>
 * <li>Checking of different visibility levels independently (API vs.
 * internals).</li>
 * </ul>
 */
public final class AbaChk {

	// Features:
	// TODO Visibility-scoped rules (public, protected, etc.)

	// To turn this into a real speed demon:
	// TODO Full dir emulation in .jar scanning?
	// TODO Bulk file attr scanning on JDK 7
	// TODO Use full ASM-based reader only if at least one containing scope has visibility-scoped rules
	// TODO Form a hierarchy of scopes by path prefix to exit matching early
	// TODO Feed the classfile iterator into a checker queue and use multiple checking workers
	// TODO See if can avoid conversion from bytes to chars (when doing only prefix matching)

	public static void main(String[] args) throws Exception {
		final RuleSet rules = makeDemoRules();
		System.out.println(rules.describe());
//		final ClassSet classes = new OutputDirClassSet(new File("/home/peo/dev/aba/trunk/abajava/temp/eclipse"));
		final ClassSet classes = new JarPathClassSet(true, new File("/home/peo/dev/aba/trunk/abajars/jars/aba/"));
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
		System.out.println(ClassParser.nFilesRead + " class files read.");
		System.out.println(ClassParser.nBytesAvail + " class bytes inspected.");
		System.out.println(ClassParser.nBytesRead + " class bytes read.");
		System.out.println(ClassParser.nBytesUsed + " class bytes accessed.");
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
