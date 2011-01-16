package ch.parren.jdepchk;

import java.io.File;

import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.ClassBytesReader;
import ch.parren.jdepchk.classes.ClassFileIterator;
import ch.parren.jdepchk.classes.PathIterator;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.RuleSetBuilder;
import ch.parren.jdepchk.rules.Scope;

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
public final class JDepChk {

	// Features:
	// TODO Scan .jar files too
	// TODO FlatteningIterator to extract the commonalities of Dirs, Jars
	
	// To turn this into a real speed demon:
	// TODO Read class files lazily (especially with HDD, zipped jars)
	// TODO PathPrefix to stop scanning dirs early
	// TODO Dir emulation in .jar scanning
	// TODO Use full ASM-based reader only if at least one containing scope has visibility-scoped rules
	// TODO Form a hierarchy of scopes by path prefix to exit matching early
	// TODO Profile!
	// TODO Feed the classfile iterator into a checker queue and use multiple checking workers
	// TODO See if can avoid conversion from bytes to chars (when doing only prefix matching)
	
	public static void main(String[] args) throws Exception {
		final RuleSet rules = makeDemoRules();
		final ClassFileIterator classes = new PathIterator(new File("/home/peo/dev/aba/trunk/abajava/temp/eclipse"));
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
		System.out.println(Scope.nSees + " Scope.sees checks.");
		System.out.println(Scope.nContains + " Scope.contains checks.");
		System.out.println(Scope.nTests + " predicate tests.");
		System.out.println(ClassBytesReader.nBytesRead + " class bytes read.");
		System.out.println(ClassBytesReader.nBytesUsed + " class bytes accessed.");
	}

	private static RuleSet makeDemoRules() {
		final RuleSetBuilder b = new RuleSetBuilder("demo");

		b.lib("$default") //
				.packages("java.**") //
				.packages("javax.**") //
				.packages("com.sun.**") //
				.packages("sun.**") //
				.packages("org.w3c.**") //
				.packages("org.xml.**") //
		;
		b.lib("abalib") //
				.packages("org.junit.**") //
				.packages("org.apache.**") //
				.packages("org.jgraph.**") //
				.packages("org.jpedal.**") //
				.packages("org.jfree.**") //
				.packages("org.jgroups.**") //
				.packages("org.jdesktop.**") //
				.packages("org.opends.**") //
				.packages("org.mozilla.**") //
				.packages("org.netbeans.**") //
				.packages("org.objectweb.**") //
				.packages("org.bouncycastle.**") //
				.packages("org.ccil.**") //
				.packages("org.h2.**") //
				.packages("org.syntax.**") //
				.packages("org.comedia.**") //
				.packages("org.codehaus.**") //
				.packages("org.tanukisoftware.**") //
				.packages("junit.**") //
				.packages("electric.**") //
				.packages("jcifs.**") //
				.packages("ice.**") //
				.packages("pv.**") //
				.packages("com.lowagie.**") //
				.packages("com.google.**") //
				.packages("com.jniwrapper.**") //
				.packages("com.aspose.**") //
				.packages("com.steadystate.**") //
				.packages("com.ibm.**") //
				.packages("com.nqadmin.**") //
				.packages("com.idautomation.**") //
		;
		b.scope("com.ulcjava") //
				.packages("com.ulcjava.**") //
		;
		b.scope("ch.abacus") //
				.packages("ch.abacus.**") //
				.extend("com.ulcjava") //
				.use("abalib") //
		;

		final RuleSet rules = b.finish();
		return rules;
	}

}
