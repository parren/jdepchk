package ch.parren.jdepchk.rules.parser;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Test;

import ch.parren.jdepchk.rules.RuleSet;
import ch.parren.jdepchk.rules.builder.RuleSetBuilder;

public class RuleSetParserTest {

	@Test public void scopes() throws Exception {
		final RuleSetBuilder b = new RuleSetBuilder("scopes.jdep");

		final InputStream is = getClass().getResourceAsStream("data/scopes.jdep");
		try {
			RuleSetLoader.loadInto(is, b);
		} finally {
			is.close();
		}

		final RuleSet set = b.finish();
		assertEquals("rule set scopes.jdep\n" + // 
				"	scope ch.abacus.java\n" + // 
				"		.contains: composite; default: false\n" + // 
				"				prefix: ch/abacus/java/\n" + // 
				"		.allows: composite; default: true\n" + // 
				"				not prefix: ch/abacus/\n" + // 
				"				prefix: ch/abacus/java/\n" + // 
				"	scope ch.abacus.ulc.client\n" + // 
				"		.contains: composite; default: false\n" + // 
				"				prefix: ch/abacus/ulc/client/\n" + // 
				"		.allows: composite; default: true\n" + // 
				"				not prefix: ch/abacus/lib/\n" + // 
				"				prefix: ch/abacus/lib/swing/\n" + // 
				"				pattern: ch/abacus/lib/net/SocketServer[^/]*\n" + // 
				"	scope ch.abacus.ulc.shared\n" + // 
				"		.contains: composite; default: false\n" + // 
				"				prefix: ch/abacus/ulc/shared/\n" + // 
				"		.allows: composite; default: true\n" + // 
				"				not prefix: ch/abacus/lib/\n" + // 
				"", set.describe());
	}

	@Test public void comps() throws Exception {
		final RuleSetBuilder b = new RuleSetBuilder("comps.jdep");

		final InputStream is = getClass().getResourceAsStream("data/comps.jdep");
		try {
			RuleSetLoader.loadInto(is, b);
		} finally {
			is.close();
		}

		final RuleSet set = b.finish();
		assertEquals("rule set comps.jdep\n" + // 
				"	scope $default\n" + // 
				"		.contains: composite; default: false\n" + // 
				"				prefix: java/\n" + // 
				"		.allows: composite; default: false\n" + // 
				"				prefix: java/\n" + // 
				"	scope api\n" + // 
				"		.contains: composite; default: false\n" + // 
				"				pattern: api/[^/]*\n" + // 
				"		.allows: composite; default: false\n" + // 
				"				pattern: api/[^/]*\n" + // 
				"				prefix: java/\n" + // 
				"	scope api.impl\n" + // 
				"		.contains: composite; default: false\n" + // 
				"		.allows: composite; default: false\n" + // 
				"				prefix: java/\n" + // 
				"				pattern: api/[^/]*\n" + // 
				"	scope api.impl.one\n" + // 
				"		.contains: composite; default: false\n" + // 
				"				prefix: api/impl/one/\n" + // 
				"		.allows: composite; default: false\n" + // 
				"				prefix: api/impl/one/\n" + // 
				"				prefix: java/\n" + // 
				"				pattern: api/[^/]*\n" + // 
				"	scope api.impl.two\n" + // 
				"		.contains: composite; default: false\n" + // 
				"				prefix: api/impl/two/\n" + // 
				"		.allows: composite; default: false\n" + // 
				"				prefix: api/impl/two/\n" + // 
				"				prefix: java/\n" + // 
				"				pattern: api/[^/]*\n" + // 
				"				prefix: api/impl/one/\n" + // 
				"	scope user\n" + // 
				"		.contains: composite; default: false\n" + // 
				"				prefix: user/\n" + // 
				"		.allows: composite; default: false\n" + // 
				"				prefix: user/\n" + // 
				"				prefix: java/\n" + // 
				"				pattern: api/[^/]*\n" + // 
				"", set.describe());
	}

}