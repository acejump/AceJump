package com.johnlindquist.acejump.search

enum class REGEX(val pattern: String) {
  END_OF_LINE("\\n"),
  BEGINNING_OF_LINE("^.|\\n(?<!.\\n)"),
  CODE_INDENTS("^\\s*\\S"),
  WHITE_SPACE("\\s+\\S(?<!^\\s*\\S)");

  companion object {
    fun contains(regex: String): Boolean {
      return REGEX.values().any { it.name == regex }
    }
  }
}