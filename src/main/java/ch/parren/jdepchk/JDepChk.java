package ch.parren.jdepchk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.MemberFilteringViolationListener;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.ClassParser;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.ClassSets;
import ch.parren.jdepchk.classes.ClassesDirClassSet;
import ch.parren.jdepchk.classes.JarFileClassSet;
import ch.parren.jdepchk.classes.JarsDirClassSet;
import ch.parren.jdepchk.classes.SingleClassSet;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.parser.FileParseException;
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
	// TODO Form a hierarchy of scopes by path prefix to exit matching early

	public static void main(String[] args) throws Exception {
		try {
			final Collection<Config> cfgs = New.arrayList();
			boolean showRules = false;
			boolean showStats = false;
			int nMaxJobs = Runtime.getRuntime().availableProcessors();

			{
				final Config cfg = new Config();
				int i = 0;
				while (i < args.length) {
					final String arg = args[i++];
					if ("--config".equals(arg) || "-f".equals(arg)) {
						parseConfig(new File(args[i++]), cfgs);
					} else if ("--rules".equals(arg) || "-r".equals(arg)) {
						cfg.ruleSets.add(parseRulesIn(new File(args[i++])));
					} else if ("--classes".equals(arg) || "-c".equals(arg)) {
						final File f = new File(args[i++]);
						if (f.isDirectory())
							cfg.classSets.add(new SingleClassSet(new ClassesDirClassSet(f)));
						else
							System.err.println("WARNING: Ignoring --classes " + f);
					} else if ("--jars".equals(arg) || "--jar".equals(arg) || "-j".equals(arg)) {
						final File f = new File(args[i++]);
						if (f.isDirectory())
							cfg.classSets.add(new JarsDirClassSet(true, f));
						else if (f.isFile())
							cfg.classSets.add(new SingleClassSet(new JarFileClassSet(f)));
						else
							System.err.println("WARNING: Ignoring --jar(s) " + f);
					} else if ("--jobs".equals(arg)) {
						nMaxJobs = Integer.parseInt(args[i++]);
					} else if ("--show-rules".equals(arg)) {
						showRules = true;
					} else if ("--show-stats".equals(arg)) {
						showStats = true;
					} else if ("--help".equals(arg) || "-h".equals(arg)) {
						showHelp();
						return;
					} else {
						System.out.println("ERROR: Invalid command line argument: " + arg);
						System.out.println("Use --help to see help.");
						System.exit(2);
					}
				}

				if (!cfg.classSets.isEmpty())
					cfgs.add(cfg);
			}

			for (Config cfg : cfgs) {
				if (showRules) {
					for (RuleSet ruleSet : cfg.ruleSets)
						System.out.println(ruleSet.describe());
					System.out.println();
				}
			}

			long taken = 0;
			int contains = 0;
			int sees = 0;
			boolean hadViolations = false;
			for (final Config cfg : cfgs) {
				final PrintingListener listener = new PrintingListener();
				final long before = System.currentTimeMillis();

				if (1 >= nMaxJobs) {
					final Checker checker = new Checker(new MemberFilteringViolationListener(listener), cfg.ruleSets);
					for (ClassSets classSets : cfg.classSets) {
						classSets.accept(new ClassSets.Visitor() {
							public void visitClassSet(ClassSet classSet) throws IOException {
								checker.check(classSet);
							}
						});
					}
					contains += checker.nContains;
					sees += checker.nSees;
				} else {
					final ConcurrentLinkedQueue<ClassSet> sets = new ConcurrentLinkedQueue<ClassSet>();
					for (ClassSets classSets : cfg.classSets) {
						classSets.accept(new ClassSets.Visitor() {
							public void visitClassSet(ClassSet classSet) throws IOException {
								sets.add(classSet);
							}
						});
					}
					final int nJobs = Math.min(nMaxJobs, sets.size());
					final Checker[] checkers = new Checker[nJobs];
					final Thread[] jobs = new Thread[nJobs];
					for (int i = 0; i < nJobs; i++) {
						final int iJob = i;
						jobs[iJob] = new Thread() {
							@Override public void run() {
								final Checker checker = new Checker(new MemberFilteringViolationListener(listener),
										cfg.ruleSets);
								checkers[iJob] = checker;
								while (true) {
									final ClassSet set = sets.poll();
									if (null == set)
										break;
									try {
										checker.check(set);
									} catch (IOException e) {
										e.printStackTrace();
										throw new RuntimeException(e);
									}
								}
							}
						};
						jobs[iJob].start();
					}
					for (int i = 0; i < nJobs; i++) {
						jobs[i].join();
						contains += checkers[i].nContains;
						sees += checkers[i].nSees;
					}
				}

				final long after = System.currentTimeMillis();
				taken += after - before;
				hadViolations |= listener.hasViolations();
				System.out.println(listener);
			}

			if (showStats) {
				System.out.println();
				System.out.println(taken + " ms taken.");
				System.out.println(contains + " containment checks.");
				System.out.println(sees + " usage checks.");
				System.out.println(ClassParser.nFilesRead + " class files read.");
				System.out.println(ClassParser.nBytesRead + " class bytes read.");
				System.out.println(ClassParser.nBytesUsed + " class bytes accessed.");
			}

			if (hadViolations)
				System.exit(1);

		} catch (ErrorReport report) {
			System.err.println(report.getMessage());
			System.exit(2);
		}
	}

	private static final class Config {
		final Collection<RuleSet> ruleSets = New.linkedList();
		final Collection<ClassSets> classSets = New.linkedList();
	}

	private static void parseConfig(File configFile, Collection<Config> configs) throws IOException, ErrorReport {
		final BufferedReader cfgReader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
		try {
			String line;
			boolean pathStartsNewScope = true;
			Config scope = null;
			while (null != (line = cfgReader.readLine())) {
				final int posOfComment = line.indexOf('#');
				if (posOfComment >= 0)
					line = line.substring(0, posOfComment);
				final String trimmed = line.trim();
				if (0 == trimmed.length())
					continue;
				if (trimmed.startsWith("max-errors:"))
					continue;
				final boolean isRulesFile = Character.isWhitespace(line.charAt(0));
				if (isRulesFile) {
					if (null == scope) {
						scope = new Config();
						configs.add(scope);
					}
					scope.ruleSets.add(parseRulesIn(new File(trimmed)));
				} else if (null == scope || pathStartsNewScope) {
					// null check keeps compiler happy
					scope = new Config();
					configs.add(scope);
					scope.classSets.add(new SingleClassSet(new ClassesDirClassSet(new File(trimmed))));
				} else {
					scope.classSets.add(new SingleClassSet(new ClassesDirClassSet(new File(trimmed))));
				}
				pathStartsNewScope = isRulesFile;
			}
		} finally {
			cfgReader.close();
		}
	}

	private static final RuleSet parseRulesIn(File file) throws IOException, ErrorReport {
		try {
			return RuleSetLoader.load(file);
		} catch (FileParseException fpe) {
			throw new ErrorReport("Error parsing file " + fpe.file + "\n" //
					+ fpe.cause.getMessage() + "\n" //
					+ "in the following fragment:\n" //
					+ "\n" //
					+ highlightLine(file, fpe.cause.cause.currentToken.next.beginLine) //
			);
		}
	}

	private static String highlightLine(File file, int lineNumber) throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			int atLine = 1;
			while (atLine < lineNumber - 1) {
				reader.readLine();
				atLine++;
			}
			final StringBuilder lines = new StringBuilder();
			while (atLine < lineNumber + 1) {
				final String line = reader.readLine();
				if (null == line)
					break;
				lines.append(line).append("\n");
				atLine++;
			}
			return lines.toString();
		} finally {
			reader.close();
		}
	}

	private static final class PrintingListener extends ViolationListener {
		private int nViol = 0;
		@Override protected synchronized boolean report(Violation v) {
			System.out.print(v.fromClassName + " > " + v.toClassName);
			if (null != v.toElementName)
				System.out.print("#" + v.toElementName + "#" + v.toElementDesc);
			System.out.println(" in " + v.scope.name() //
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
				if (red < buf.length) {
					final char[] part = new char[red];
					System.arraycopy(buf, 0, part, 0, red);
					System.out.print(part);
				} else {
					System.out.print(buf);
				}
		} finally {
			r.close();
		}
	}

	private static final class ErrorReport extends Throwable {
		public ErrorReport(String message) {
			super(message);
		}
	}

}
