package ch.parren.jdepchk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Set;
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
import ch.parren.jdepchk.config.ConfigParser;
import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;
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
			int nMaxJobs = Runtime.getRuntime().availableProcessors() * 2;

			{
				final Config cfg = new Config();
				int i = 0;
				while (i < args.length) {
					final String arg = args[i++];
					if ("--config".equals(arg) || "-f".equals(arg)) {
						parseConfig(new File(args[i++]), cfgs);
					} else if ("--rules".equals(arg) || "-r".equals(arg)) {
						parseRulesIn(args[i++], cfg.ruleSets);
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
					} else if ("--debug".equals(arg)) {
						Checker.debugOutput = true;
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

	private static void parseConfig(File configFile, final Collection<Config> configs) throws IOException, ErrorReport {
		new ConfigParser<ErrorReport>(new ConfigParser.Visitor<ErrorReport>() {
			Config scope = null;
			RuleSetBuilder builder = null;
			final Set<File> parsed = New.hashSet();
			@Override protected void visitClassSpecsStart() throws IOException, ErrorReport {
				scope = new Config();
			}
			@Override protected void visitClassSpec(String spec) throws IOException, ErrorReport {
				scope.classSets.add(new SingleClassSet(new ClassesDirClassSet(new File(spec))));
			}
			@Override protected void visitRuleSpecsStart(String name) throws IOException, ErrorReport {
				builder = new RuleSetBuilder(name);
				parsed.clear();
			}
			@Override protected void visitRuleSpec(String spec) throws IOException, ErrorReport {
				parseRulesIn(spec, builder, parsed);
			}
			@Override protected void visitRuleSpecsEnd() throws IOException, ErrorReport {
				scope.ruleSets.add(builder.finish());
				builder = null;
			}
			@Override protected void visitClassSpecsEnd() throws IOException, ErrorReport {
				configs.add(scope);
				scope = null;
			}
			@Override protected void visitError(String message) throws IOException, ErrorReport {
				throw new ErrorReport(message);
			}
		}).parseConfig(configFile);
	}

	private static void parseRulesIn(String fileOrDirPaths, Collection<RuleSet> ruleSets) throws IOException,
			ErrorReport {
		final RuleSetBuilder builder = new RuleSetBuilder(fileOrDirPaths);
		final Set<File> parsed = New.hashSet();
		parseRulesIn(fileOrDirPaths, builder, parsed);
		ruleSets.add(builder.finish());
	}

	private static void parseRulesIn(String fileOrDirPaths, RuleSetBuilder builder, Set<File> parsed)
			throws IOException, ErrorReport {
		final String[] parts = fileOrDirPaths.split("[" + File.pathSeparator + "]");
		for (String part : parts)
			parseRulesInPart(part, builder, parsed);
	}

	private static void parseRulesInPart(String fileOrDirPath, RuleSetBuilder builder, Set<File> parsed)
			throws IOException, ErrorReport {
		if (fileOrDirPath.endsWith("/*/")) {
			final File parentDir = new File(fileOrDirPath.substring(0, fileOrDirPath.length() - "/*/".length()));
			final FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return !name.startsWith(".");
				}
			};
			if (parentDir.isDirectory())
				for (File subDir : parentDir.listFiles(filter))
					if (subDir.isDirectory())
						parseRulesInDir(subDir, builder, parsed);
		} else {
			final File fileOrDir = new File(fileOrDirPath);
			if (!fileOrDir.exists() && fileOrDirPath.endsWith("/"))
				; // pass
			else if (fileOrDir.isDirectory())
				parseRulesInDir(fileOrDir, builder, parsed);
			else
				parseRulesInFile(fileOrDir, builder, parsed);
		}
	}

	private static final void parseRulesInDir(File dir, RuleSetBuilder builder, Set<File> parsed) throws IOException,
			ErrorReport {
		final FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.startsWith(".");
			}
		};
		for (File file : dir.listFiles(filter))
			if (file.isFile())
				parseRulesInFile(file, builder, parsed);
	}

	private static final void parseRulesInFile(File file, RuleSetBuilder builder, Set<File> parsed) throws IOException,
			ErrorReport {
		if (!parsed.add(file))
			return;
		try {
			RuleSetLoader.loadInto(file, builder);
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
