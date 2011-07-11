package ch.parren.jdepchk.tutorial;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import ch.parren.java.lang.New;
import ch.parren.jdepchk.check.Violation;
import ch.parren.jdepchk.check.ViolationListener;

public abstract class AbstractTutorialTest {

	protected final String[] replaceIn(String[] lines, String what, String with) {
		final String[] res = new String[lines.length];
		for (int i = 0; i < lines.length; i++)
			res[i] = lines[i].replace(what, with);
		return res;
	}

	protected final ViolationListener violationsGatherer = new ViolationListener() {
		@Override public boolean report(Violation v) {
			violations.add(v);
			return true;
		}
	};
	protected final List<Violation> violations = New.arrayList();

	protected final void deleteRecursively(File f) throws IOException {
		if (!f.exists())
			return;
		if (f.isDirectory())
			for (File s : f.listFiles())
				deleteRecursively(s);
		if (!f.delete())
			throw new IOException("Failed to delete " + f);
	}

	protected final void assertContains(File f, String... lines) throws IOException {
		final BufferedReader r = new BufferedReader(new FileReader(f));
		try {
			String line;
			int i = 0;
			while (null != (line = r.readLine()))
				assertEquals(lines[i++], line);
		} finally {
			r.close();
		}
	}

}
