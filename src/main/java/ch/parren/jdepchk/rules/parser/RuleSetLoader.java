package ch.parren.jdepchk.rules.parser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;

public final class RuleSetLoader {

	static public void loadInto(Reader reader, RuleSetBuilder builder) throws StreamParseException {
		final RuleSetParser parser = new RuleSetParser(reader);
		parser.builder = builder;
		try {
			parser.ruleSet();
		} catch (ParseException pe) {
			final SimpleCharStream scs = parser.jj_input_stream;
			throw new StreamParseException(pe, scs.tokenBegin, scs.bufpos);
		}
	}

	static public void loadInto(InputStream stream, RuleSetBuilder builder) throws StreamParseException {
		final RuleSetParser parser = new RuleSetParser(new BufferedInputStream(stream));
		parser.builder = builder;
		try {
			parser.ruleSet();
		} catch (ParseException pe) {
			final SimpleCharStream scs = parser.jj_input_stream;
			throw new StreamParseException(pe, scs.tokenBegin, scs.bufpos);
		}
	}

	static public RuleSet load(File file) throws IOException, FileParseException {
		final RuleSetBuilder builder = new RuleSetBuilder(file.getPath());
		final InputStream stream = new FileInputStream(file);
		try {
			loadInto(stream, builder);
		} catch (StreamParseException pe) {
			throw new FileParseException(file, pe);
		} finally {
			stream.close();
		}
		return builder.finish();
	}

}
