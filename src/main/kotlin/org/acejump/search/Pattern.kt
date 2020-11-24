package org.acejump.search

enum class Pattern(val regex: String) {
  START_OF_LINE("^.|^\\n"),
  END_OF_LINE("\\n|\\Z"),
  CODE_INDENTS("[^\\s].*|^\\n"),
  LINE_MARK(END_OF_LINE.regex + "|" + START_OF_LINE.regex + "|" + CODE_INDENTS.regex),
  ALL_WORDS("(?<=[^a-zA-Z0-9_]|\\A)[a-zA-Z0-9_]");
}