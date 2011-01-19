package ch.parren.jdepchk.rules.parser;

import java.io.BufferedInputStream;
import java.io.InputStream;

import ch.parren.jdepchk.rules.builder.RuleSetBuilder;

public final class RuleSetLoader {

	static public void loadInto(InputStream stream, RuleSetBuilder builder) {
		try {
			final RuleSetParser parser = new RuleSetParser(new BufferedInputStream(stream));
			parser.builder = builder;
			parser.ruleSet();
		} catch (ParseException e) {
			throw new RuntimeException( e );
		}
	}

}
