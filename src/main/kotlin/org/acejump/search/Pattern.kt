package org.acejump.search

enum class Pattern(val regex: String) {
  LINE_STARTS("^.|^\\n"),
  LINE_ENDS("\\n|\\Z"),
  LINE_INDENTS("[^\\s].*|^\\n"),
  LINE_ALL_MARKS(LINE_ENDS.regex + "|" + LINE_STARTS.regex + "|" + LINE_INDENTS.regex),
  ALL_WORDS("(?<=[^a-zA-Z0-9_]|\\A)[a-zA-Z0-9_]");
}
