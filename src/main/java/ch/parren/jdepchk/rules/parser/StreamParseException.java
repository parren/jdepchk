package ch.parren.jdepchk.rules.parser;

public final class StreamParseException extends Exception {

	public final ParseException cause;
	public final int startOffs;
	public final int endOffs;

	public StreamParseException(ParseException cause, int startOffs, int endOffs) {
		super(cause);
		this.cause = cause;
		this.startOffs = startOffs;
		this.endOffs = endOffs;
	}

}
