package ch.parren.jdepchk;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.ClassParser;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.JarsDirClassSet;
import ch.parren.jdepchk.classes.ClassesDirClassSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.parser.RuleSetLoader;

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
	// TODO Visibility-scoped rules (public, protected, etc.)

	// To turn this into a real speed demon:
	// TODO Full dir emulation in .jar scanning?
	// TODO Bulk file attr scanning on JDK 7
	// TODO Use full ASM-based reader only if at least one containing scope has visibility-scoped rules
	// TODO Form a hierarchy of scopes by path prefix to exit matching early
	// TODO Feed the classfile iterator into a checker queue and use multiple checking workers
	// TODO See if can avoid conversion from bytes to chars (when doing only prefix matching)

	public static void main(String[] args) throws Exception {
		final Collection<RuleSet> ruleSets = New.linkedList();
		final Collection<ClassSet> classSets = New.linkedList();
		boolean showRules = false;
		boolean showStats = false;

		int i = 0;
		while (i < args.length) {
			final String arg = args[i++];
			if ("--rules".equals(arg) || "-r".equals(arg))
				ruleSets.add(RuleSetLoader.load(new File(args[i++])));
			else if ("--classes".equals(arg) || "-c".equals(arg))
				classSets.add(new ClassesDirClassSet(new File(args[i++])));
			else if ("--jars".equals(arg) || "-j".equals(arg))
				classSets.add(new JarsDirClassSet(true, new File(args[i++])));
			else if ("--show-rules".equals(arg))
				showRules = true;
			else if ("--show-stats".equals(arg))
				showStats = true;
			else if ("--help".equals(arg) || "-h".equals(arg)) {
				showHelp();
				return;
			} else {
				System.out.println("ERROR: Invalid command line argument: " + arg);
				System.out.println("Use --help to see help.");
				System.exit(2);
			}
		}

		if (showRules) {
			for (RuleSet ruleSet : ruleSets)
				System.out.println(ruleSet.describe());
			System.out.println();
		}

		final PrintingListener listener = new PrintingListener();
		final Checker checker = new Checker(listener, ruleSets);
		final long before = System.currentTimeMillis();
		for (ClassSet classSet : classSets)
			checker.check(classSet);
		final long after = System.currentTimeMillis();

		System.out.println(listener);

		if (showStats) {
			System.out.println();
			System.out.println((after - before) + " ms taken.");
			System.out.println(checker.nContains + " containment checks.");
			System.out.println(checker.nSees + " usage checks.");
			System.out.println(ClassParser.nFilesRead + " class files read.");
			System.out.println(ClassParser.nBytesRead + " class bytes read.");
			System.out.println(ClassParser.nBytesUsed + " class bytes accessed.");
		}

		if (listener.hasViolations())
			System.exit(1);
	}

	private static final class PrintingListener extends ViolationListener {
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
		public boolean hasViolations() {
			return nViol > 0;
		}
	}

	private static void showHelp() throws IOException {
		final Reader r = new InputStreamReader(JDepChk.class.getResourceAsStream("help.txt"), "UTF-8");
		try {
			final char[] buf = new char[1024];
			int red;
			while ((red = r.read(buf)) > 0)
				if (red < buf.length)
					System.out.print(Arrays.copyOf(buf, red));
				else
					System.out.print(buf);
		} finally {
			r.close();
		}
	}

}
