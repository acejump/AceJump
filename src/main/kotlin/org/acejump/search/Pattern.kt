package org.acejump.search

enum class Pattern(val regex: String) {
  LINE_STARTS("^.|^\\n|(?<!.)\\Z"),
  LINE_ENDS("\\n|\\Z"),
  LINE_INDENTS("[^\\s].*|^\\n|(?<!.)\\Z"),
  LINE_ALL_MARKS(listOf(LINE_ENDS, LINE_STARTS, LINE_INDENTS).flatMap { it.regex.split("|") }.distinct().joinToString("|")),
  ALL_WORDS("(?<=[^a-zA-Z0-9_]|\\A)[a-zA-Z0-9_]");
}
