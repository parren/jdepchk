package ch.parren.jdepchk.rules.parser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;

public final class RuleSetLoader {

	static public void loadInto(InputStream stream, RuleSetBuilder builder) {
		try {
			final RuleSetParser parser = new RuleSetParser(new BufferedInputStream(stream));
			parser.builder = builder;
			parser.ruleSet();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	static public RuleSet load(File file) throws IOException {
		final RuleSetBuilder builder = new RuleSetBuilder(file.getPath());
		final InputStream stream = new FileInputStream(file);
		try {
			loadInto(stream, builder);
		} finally {
			stream.close();
		}
		return builder.finish();
	}

}
