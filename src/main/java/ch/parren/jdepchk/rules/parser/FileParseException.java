package ch.parren.jdepchk.rules.parser;

import java.io.File;

public final class FileParseException extends Exception {

	public final File file;
	public final StreamParseException cause;

	public FileParseException(File file, StreamParseException cause) {
		super("Error parsing file " + file, cause);
		this.file = file;
		this.cause = cause;
	}

}
