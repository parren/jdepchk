package ch.parren.jdepchk.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.classes.ClassSets;
import ch.parren.jdepchk.classes.ClassesDirClassSet;
import ch.parren.jdepchk.classes.JarFileClassSet;
import ch.parren.jdepchk.classes.JarsDirClassSet;
import ch.parren.jdepchk.classes.SingleClassSet;

public abstract class OptionsParser {

	public void parseCommandLine(String[] args) throws IOException, ErrorReport {
		parse(New.arrayList(args).iterator(), true);
		closeScope();
	}

	public void parseOptionsFile(File file) throws IOException, ErrorReport {
		parseFromFile(file);
		closeScope();
	}

	public void parseOptionsFile(BufferedReader reader) throws IOException, ErrorReport {
		parse(readFrom(reader), false);
		closeScope();
	}

	private void parse(Iterator<String> args, boolean flagUnknown) throws IOException, ErrorReport {
		while (args.hasNext()) {
			final String arg = args.next();
			if ("--config-file".equals(arg) || "-f".equals(arg)) {
				parseFromFile(new File(args.next()));

			} else if ("--scope".equals(arg)) {
				closeScope();
				startScope(args.next());

			} else if ("--classes".equals(arg) || "-c".equals(arg)) {
				startDefaultScopeIfNecessary();
				visitClasses(args.next());
			} else if ("--jars".equals(arg) || "--jar".equals(arg) || "-j".equals(arg)) {
				startDefaultScopeIfNecessary();
				visitJars(args.next());

			} else if ("--rule-set".equals(arg)) {
				closeRuleSet();
				startRuleSet(args.next());

			} else if ("--rules".equals(arg) || "-r".equals(arg)) {
				final String spec = args.next();
				startDefaultRuleSetIfNecessary(spec);
				visitRuleSpec(spec);

			} else if ("--local-rules".equals(arg) || "-l".equals(arg)) {
				startDefaultScopeIfNecessary();
				visitLocalRulesDir(new File(args.next()));
			} else if ("--global-rules".equals(arg) || "-g".equals(arg)) {
				startDefaultScopeIfNecessary();
				visitGlobalRulesDir(new File(args.next()));

			} else if ("--extract-annotations".equals(arg) || "-e".equals(arg)) {
				startDefaultScopeIfNecessary();
				visitExtractAnnotations(true);

			} else if ("--no-check".equals(arg)) {
				startDefaultScopeIfNecessary();
				visitCheckClasses(false);

			} else {
				visitArg(arg, args, flagUnknown);
			}
		}
	}

	private void parseFromFile(File file) throws IOException, ErrorReport {
		parse(readFrom(file), false);
	}

	private Iterator<String> readFrom(File file) throws IOException {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		try {
			return readFrom(reader);
		} finally {
			reader.close();
		}
	}

	private Iterator<String> readFrom(BufferedReader reader) throws IOException {
		final Collection<String> args = New.arrayList();
		String line;
		while (null != (line = reader.readLine())) {
			final Matcher commentMatcher = COMMENT_START.matcher(line);
			if (commentMatcher.find())
				line = line.substring(0, commentMatcher.start());
			final String trimmed = line.trim();
			if (0 == trimmed.length())
				continue;
			for (String arg : ARG_SEP.split(trimmed))
				args.add(arg);
		}
		return args.iterator();
	}
	private Pattern COMMENT_START = Pattern.compile("(\\s|^)#");
	private Pattern ARG_SEP = Pattern.compile("\\s");

	private void startScope(String name) throws IOException, ErrorReport {
		closeScope();
		visitScopeStart(name);
		inScope = true;
	}
	protected void startDefaultScopeIfNecessary() throws IOException, ErrorReport {
		if (inScope)
			return;
		startScope("default");
	}
	private void closeScope() throws IOException, ErrorReport {
		closeRuleSet();
		if (inScope)
			visitScopeEnd();
		inScope = false;
	}
	private boolean inScope = false;

	private void startRuleSet(String name) throws IOException, ErrorReport {
		startDefaultScopeIfNecessary();
		closeRuleSet();
		visitRuleSetStart(name);
		inRuleSet = true;
	}
	private void startDefaultRuleSetIfNecessary(String name) throws IOException, ErrorReport {
		if (inRuleSet)
			return;
		startRuleSet(name);
	}
	private void closeRuleSet() throws IOException, ErrorReport {
		if (inRuleSet)
			visitRuleSetEnd();
		inRuleSet = false;
	}
	private boolean inRuleSet = false;

	@SuppressWarnings("unused")//
	protected void visitArg(String arg, Iterator<String> more, boolean flagUnknown) throws IOException, ErrorReport {
		throw new ErrorReport("ERROR: Invalid command line argument: " + arg + "\n" + "Use --help to see help.");
	}

	protected void visitClasses(String spec) throws IOException, ErrorReport {
		final File f = new File(spec);
		if (f.isDirectory())
			visitClassSets(new SingleClassSet(new ClassesDirClassSet(f)));
		else
			System.err.println("WARNING: Ignoring --classes " + f);
	}
	protected void visitJars(String spec) throws IOException, ErrorReport {
		final File f = new File(spec);
		if (f.isDirectory())
			visitClassSets(new JarsDirClassSet(true, f));
		else if (f.isFile())
			visitClassSets(new SingleClassSet(new JarFileClassSet(f)));
		else
			System.err.println("WARNING: Ignoring --jar(s) " + f);
	}

	protected abstract void visitScopeStart(String name) throws IOException, ErrorReport;
	protected abstract void visitClassSets(ClassSets classSets) throws IOException, ErrorReport;
	protected abstract void visitRuleSetStart(String name) throws IOException, ErrorReport;
	protected abstract void visitRuleSpec(String spec) throws IOException, ErrorReport;
	protected abstract void visitRuleSetEnd() throws IOException, ErrorReport;
	protected abstract void visitLocalRulesDir(File dir) throws IOException, ErrorReport;
	protected abstract void visitGlobalRulesDir(File dir) throws IOException, ErrorReport;
	protected abstract void visitExtractAnnotations(boolean active) throws IOException, ErrorReport;
	protected abstract void visitCheckClasses(boolean active) throws IOException, ErrorReport;
	protected abstract void visitScopeEnd() throws IOException, ErrorReport;

	public static final class ErrorReport extends Throwable {
		public ErrorReport(String message) {
			super(message);
		}
	}

}
