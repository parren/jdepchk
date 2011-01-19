/* Generated By:JavaCC: Do not edit this line. RuleSetParserConstants.java */
package ch.parren.jdepchk.rules.parser;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface RuleSetParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int SHELL_COMMENT = 5;
  /** RegularExpression Id. */
  int SINGLE_LINE_COMMENT = 6;
  /** RegularExpression Id. */
  int MULTI_LINE_COMMENT = 7;
  /** RegularExpression Id. */
  int Id = 8;

  /** Lexical state. */
  int DEFAULT = 0;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\n\"",
    "\"\\r\"",
    "<SHELL_COMMENT>",
    "<SINGLE_LINE_COMMENT>",
    "<MULTI_LINE_COMMENT>",
    "<Id>",
    "\"scope:\"",
    "\"contains:\"",
    "\"allows:\"",
    "\"lib:\"",
    "\"comp:\"",
    "\"extends:\"",
    "\"uses:\"",
    "\"-\"",
    "\"!\"",
  };

}