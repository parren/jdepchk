package ch.parren.jdepchk.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConfigParser<T extends Throwable> {

	@SuppressWarnings("unused")//
	public static abstract class Visitor<T extends Throwable> {
		protected void visitMaxErrors(int maxErrors) throws IOException, T {}
		protected void visitClassSpecsStart() throws IOException, T {}
		protected void visitClassSpec(String spec) throws IOException, T {}
		protected void visitRuleSpecsStart(String name) throws IOException, T {}
		protected void visitRuleSpec(String spec) throws IOException, T {}
		protected void visitRuleSpecsEnd() throws IOException, T {}
		protected void visitClassSpecsEnd() throws IOException, T {}
		protected abstract void visitError(String message) throws IOException, T;
	}

	private final Visitor<T> visitor;

	public ConfigParser(Visitor<T> visitor) {
		this.visitor = visitor;
	}

	public void parseConfig(File configFile) throws IOException, T {
		final BufferedReader cfgReader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
		try {
			parseConfig(cfgReader);
		} finally {
			cfgReader.close();
		}
	}

	public void parseConfig(BufferedReader cfgReader) throws IOException, T {
		boolean inClassSpecs = false;
		boolean classSpecsSawRuleSpec = false;
		boolean inRuleSpecs = false;
		String line;
		while (null != (line = cfgReader.readLine())) {
			final int posOfComment = line.indexOf('#');
			if (posOfComment >= 0)
				line = line.substring(0, posOfComment);
			final String trimmed = line.trim();
			if (0 == trimmed.length())
				continue;
			if (trimmed.startsWith("max-errors:"))
				visitor.visitMaxErrors(Integer.parseInt(trimmed.substring("max-errors:".length()).trim()));
			final boolean isRulesSpec = Character.isWhitespace(line.charAt(0));
			if (isRulesSpec)
				if (!inClassSpecs)
					visitor.visitError("Rule specification not preceded by class specification: " + trimmed);
				else {
					if (trimmed.endsWith(":")) {
						if (inRuleSpecs)
							visitor.visitRuleSpecsEnd();
						visitor.visitRuleSpecsStart(trimmed.substring(0, trimmed.length() - 1));
						inRuleSpecs = true;
					} else if (inRuleSpecs) {
						visitor.visitRuleSpec(trimmed);
						classSpecsSawRuleSpec = true;
					} else {
						visitor.visitRuleSpecsStart(trimmed);
						visitor.visitRuleSpec(trimmed);
						visitor.visitRuleSpecsEnd();
						classSpecsSawRuleSpec = true;
					}
				}
			else {
				if (inRuleSpecs) {
					visitor.visitRuleSpecsEnd();
					inRuleSpecs = false;
					visitor.visitClassSpecsEnd();
					inClassSpecs = false;
				}
				if (!inClassSpecs) {
					visitor.visitClassSpecsStart();
					inClassSpecs = true;
					classSpecsSawRuleSpec = false;
				}
				visitor.visitClassSpec(trimmed);
			}
		}
		if (inClassSpecs) {
			if (inRuleSpecs)
				visitor.visitRuleSpecsEnd();
			if (!classSpecsSawRuleSpec)
				visitor.visitError("Final class specifications not followed by rule specification.");
			visitor.visitClassSpecsEnd();
		}
	}

}
