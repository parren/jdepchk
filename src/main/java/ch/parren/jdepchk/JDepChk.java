package ch.parren.jdepchk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.check.Checker;
import ch.parren.jdepchk.check.MemberFilteringViolationListener;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;
import ch.parren.jdepchk.classes.AbstractClassFilesSet;
import ch.parren.jdepchk.classes.ClassSet;
import ch.parren.jdepchk.classes.ClassSets;
import ch.parren.jdepchk.classes.CombinedClassSetVisitor;
import ch.parren.jdepchk.config.OptionsParser;
import ch.parren.jdepchk.config.OptionsParser.ErrorReport;
import ch.parren.jdepchk.extraction.Extractor;
import ch.parren.jdepchk.extraction.RuleFilesManager;
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

	final Collection<Scope> scopes = New.arrayList();

	boolean autoRecheckWhenRulesChanged = false;
	boolean showRules = false;
	boolean showStats = false;
	int nMaxJobs = Runtime.getRuntime().availableProcessors() * 2;

	public static void main(String[] args) throws Exception {
		try {
			final JDepChk chk = new JDepChk();
			chk.parseOptions(args);
			final CountingListener counter = new CountingListener(new PrintingListener());
			if (chk.autoRecheckWhenRulesChanged) {
				final BufferingListener bufferer = new BufferingListener();
				if (chk.run(bufferer)) {
					System.out.println("Extracted rules changed. Rechecking.");
					final JDepChk rechk = new JDepChk();
					rechk.parseOptions(args);
					for (Scope scope : rechk.scopes)
						scope.extractRules = false;
					rechk.run(counter);
				} else
					bufferer.replayTo(counter);
			} else {
				if (chk.run(counter)) {
					System.out.println("Extracted rules changed.");
					System.exit(2);
				}
			}
			System.out.println(counter);
			if (counter.hasViolations())
				System.exit(1);
		} catch (ErrorReport report) {
			System.err.println(report.getMessage());
			System.exit(9);
		} catch (Throwable report) {
			report.printStackTrace();
			System.exit(9);
		}
	}

	private boolean run(final ViolationListener listener) throws Exception {
		if (showRules)
			showRules();
		long taken = 0;
		int contains = 0;
		int sees = 0;
		boolean hadChanges = false;
		for (final Scope scope : scopes) {
			final long before = System.currentTimeMillis();

			final boolean isSingleThreaded = (nMaxJobs <= 1);
			final RuleFilesManager rulesMgr = scope.extractRules ? new RuleFilesManager(scope.localExtractedRulesDir,
					scope.globalExtractedRulesDir, "", !isSingleThreaded) : null;

			class SingleThreaded {

				private final Checker checker;
				private final Extractor extractor;
				private final ClassSet.Visitor visitor;
				private int nContains;
				private int nSees;

				public SingleThreaded() {
					checker = scope.checkClasses ? new Checker(new MemberFilteringViolationListener(listener),
							scope.ruleSets) : null;
					final ClassSet.Visitor checkerVisitor = (null == checker) ? null : checker.newClassSetVisitor();
					extractor = scope.extractRules ? new Extractor(rulesMgr) : null;
					final ClassSet.Visitor extractorVisitor = (null == extractor) ? null : extractor
							.newClassSetVisitor();
					if (null == checkerVisitor)
						visitor = extractorVisitor;
					else if (null == extractorVisitor)
						visitor = checkerVisitor;
					else
						visitor = new CombinedClassSetVisitor(checkerVisitor, extractorVisitor);
				}

				public void process(Collection<ClassSets> classSetsInScope) throws IOException {
					final ClassSets.Visitor setsVisitor = new ClassSets.Visitor() {
						public void visitClassSet(ClassSet classSet) throws IOException {
							classSet.accept(visitor);
						}
					};
					for (ClassSets classSets : classSetsInScope)
						classSets.accept(setsVisitor);
				}

				public void process(ClassSet classSet) throws IOException {
					classSet.accept(visitor);
				}

				public void finish() {
					nContains = (null == checker) ? 0 : checker.nContains;
					nSees = (null == checker) ? 0 : checker.nSees;
				}

			}

			if (isSingleThreaded) {
				final SingleThreaded single = new SingleThreaded();
				single.process(scope.classSets);
				single.finish();
				contains += single.nContains;
				sees += single.nSees;
			} else {
				final ConcurrentLinkedQueue<ClassSet> sets = new ConcurrentLinkedQueue<ClassSet>();

				final ClassSets.Visitor setsVisitor = new ClassSets.Visitor() {
					public void visitClassSet(ClassSet classSet) throws IOException {
						sets.add(classSet);
					}
				};
				for (ClassSets classSets : scope.classSets)
					classSets.accept(setsVisitor);

				final int nJobs = Math.min(nMaxJobs, sets.size());
				final SingleThreaded[] singles = new SingleThreaded[nJobs];
				final Thread[] jobs = new Thread[nJobs];
				for (int i = 0; i < nJobs; i++) {
					final int iJob = i;
					jobs[iJob] = new Thread() {

						@Override public void run() {
							final SingleThreaded single = new SingleThreaded();
							singles[iJob] = single;
							while (true) {
								final ClassSet set = sets.poll();
								if (null == set)
									break;
								try {
									single.process(set);
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
					final SingleThreaded single = singles[i];
					single.finish();
					contains += single.nContains;
					sees += single.nSees;
				}
			}

			final long after = System.currentTimeMillis();
			taken += after - before;
			if (null != rulesMgr) {
				hadChanges |= rulesMgr.finish();
			}
		}

		if (showStats) {
			System.out.println();
			System.out.println(taken + " ms taken.");
			System.out.println(contains + " containment checks.");
			System.out.println(sees + " usage checks.");
			System.out.println(AbstractClassFilesSet.approximateNumberOfClassesParsed
					+ " class files read (approximate).");
		}

		return hadChanges;
	}

	private void showRules() {
		for (Scope scope : scopes) {
			for (RuleSet ruleSet : scope.ruleSets)
				System.out.println(ruleSet.describe());
			System.out.println();
		}
	}

	private void parseOptions(String[] args) throws IOException, ErrorReport {
		new OptionsParser() {

			private Scope scope;
			private RuleSetBuilder rules;
			private Set<File> parsed;

			@Override protected void visitScopeStart(String name) throws IOException, ErrorReport {
				scope = new Scope();
			}

			@Override protected void visitClassSets(ClassSets classSets) throws IOException, ErrorReport {
				scope.classSets.add(classSets);
			}

			@Override protected void visitRuleSetStart(String name) throws IOException, ErrorReport {
				rules = new RuleSetBuilder(name);
				parsed = New.hashSet();
			}

			@Override protected void visitRuleSpec(String spec) throws IOException, ErrorReport {
				parseRulesIn(spec, rules, parsed);
			}

			@Override protected void visitRuleSetEnd() throws IOException, ErrorReport {
				scope.ruleSets.add(rules.finish());
				rules = null;
				parsed = null;
			}

			@Override protected void visitCheckClasses(boolean active) throws IOException, ErrorReport {
				scope.checkClasses = active;
			}

			@Override protected void visitExtractAnnotations(boolean active) throws IOException, ErrorReport {
				scope.extractRules = active;
			}

			@Override protected void visitLocalRulesDir(File dir) throws IOException, ErrorReport {
				scope.localExtractedRulesDir = dir;
			}

			@Override protected void visitGlobalRulesDir(File dir) throws IOException, ErrorReport {
				scope.globalExtractedRulesDir = dir;
			}

			@Override protected void visitScopeEnd() throws IOException, ErrorReport {
				scopes.add(scope);
				scope = null;
			}

			@Override protected void visitArg(String arg, Iterator<String> more, boolean flagUnknown)
					throws IOException, ErrorReport {
				if ("--jobs".equals(arg)) {
					nMaxJobs = Integer.parseInt(more.next());
				} else if ("--auto-recheck".equals(arg) || "-a".equals(arg)) {
					autoRecheckWhenRulesChanged = true;
				} else if ("--show-rules".equals(arg)) {
					showRules = true;
				} else if ("--show-stats".equals(arg)) {
					showStats = true;
				} else if ("--help".equals(arg) || "-h".equals(arg)) {
					showHelp();

				} else if ("--debug".equals(arg)) {
					Checker.debugOutput = true;
				} else if ("--use-asm-checker".equals(arg)) {
					Checker.useCustomParser = false;

				} else {
					super.visitArg(arg, more, flagUnknown);
				}
			}

		}.parseCommandLine(args);
	}

	private static final class Scope {
		final Collection<RuleSet> ruleSets = New.linkedList();
		final Collection<ClassSets> classSets = New.linkedList();
		File localExtractedRulesDir;
		File globalExtractedRulesDir;
		boolean extractRules = false;
		boolean checkClasses = true;
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

	private static final class CountingListener extends ViolationListener {
		private final ViolationListener base;
		private int nViol = 0;
		public CountingListener(ViolationListener base) {
			this.base = base;
		}
		@Override public synchronized boolean report(Violation v) {
			nViol++;
			return base.report(v);
		}
		@Override public String toString() {
			return nViol + " violations.";
		}
		public boolean hasViolations() {
			return nViol > 0;
		}
	}

	private static final class PrintingListener extends ViolationListener {
		@Override public synchronized boolean report(Violation v) {
			System.out.print(v.fromClassName + " > " + v.toClassName);
			if (null != v.toElementName)
				System.out.print("#" + v.toElementName + "#" + v.toElementDesc);
			System.out.println(" in " + v.scope.name() //
					+ " from " + v.ruleSet.name());
			return true;
		}
	}

	private static final class BufferingListener extends ViolationListener {
		private final Collection<Violation> violations = New.arrayList();
		@Override public synchronized boolean report(Violation v) {
			violations.add(v);
			return true;
		}
		public void replayTo(ViolationListener listener) {
			for (Violation v : violations)
				listener.report(v);
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

}
