package ch.parren.jdepchk.rules.parser;

import java.io.File;

public final class FileParseException extends Exception {

	public final File file;
	public final ParseException cause;

	public FileParseException(File file, ParseException cause) {
		super("Error parsing file " + file, cause);
		this.file = file;
		this.cause = cause;
	}

}
