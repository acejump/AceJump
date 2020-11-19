package org.acejump.label

import org.acejump.config.AceConfig

/**
 * Patterns related to key priority, separation, and regexps for line mode.
 */

enum class Pattern(val string: String) {
  END_OF_LINE("\\n|\\Z"),
  START_OF_LINE("^.|^\\n"),
  CODE_INDENTS("[^\\s].*|^\\n"),
  LINE_MARK(END_OF_LINE.string + "|" +
    START_OF_LINE.string + "|" +
    CODE_INDENTS.string),
  ALL_WORDS("(?<=[^a-zA-Z0-9_]|\\A)[a-zA-Z0-9_]");

  companion object {
    val NUM_TAGS: Int
      get() = NUM_CHARS * NUM_CHARS

    val NUM_CHARS: Int
      get() = AceConfig.allowedChars.length

    fun filter(bigrams: Set<String>, query: String) =
      bigrams.filter { !query.endsWith(it[0]) }

    /**
     * Sorts available tags by key distance. Tags which are ergonomically easier
     * to reach will be assigned first. We would prefer to use tags that contain
     * repeated keys (ex. FF, JJ), and use tags that contain physically adjacent
     * keys (ex. 12, 21) to keys that are located further apart on the keyboard.
     */

    enum class KeyLayout(vararg val rows: String) {
      COLEMK("1234567890", "qwfpgjluy", "arstdhneio", "zxcvbkm"),
      WORKMN("1234567890", "qdrwbjfup", "ashtgyneoi", "zxmcvkl"),
      DVORAK("1234567890", "pyfgcrl", "aoeuidhtns", "qjkxbmwvz"),
      QWERTY("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"),
      QWERTZ("1234567890", "qwertzuiop", "asdfghjkl", "yxcvbnm"),
      QGMLWY("1234567890", "qgmlwyfub", "dstnriaeoh", "zxcvjkp"),
      QGMLWB("1234567890", "qgmlwbyuv", "dstnriaeoh", "zxcfjkp"),
      NORMAN("1234567890", "qwdfkjurl", "asetgynioh", "zxcvbpm");

      private val priority by lazy {
        when (this) {
          QWERTY -> "fjghdkslavncmbxzrutyeiwoqp5849673210"
          QWERTZ -> "fjghdkslavncmbxyrutzeiwoqp5849673210"
          COLEMK -> "tndhseriaovkcmbxzgjplfuwyq5849673210"
          DVORAK -> "uhetidonasxkbjmqwvzgfycprl5849673210"
          NORMAN -> "tneigysoahbvpcmxzjkufrdlwq5849673210"
          QGMLWY -> "naterisodhvkcpjxzlfmuwygbq5849673210"
          QGMLWB -> "naterisodhfkcpjxzlymuwbgvq5849673210"
          WORKMN -> "tnhegysoaiclvkmxzwfrubjdpq5849673210"
        }.mapIndices()
      }

      val text by lazy {
        joinBy("").toCharArray().sortedBy { priority[it] }.joinToString("")
      }

      fun priority(tagToChar: (String) -> Char): (String) -> Int? =
        { priority[tagToChar(it)] }

      fun joinBy(separator: CharSequence) = rows.joinToString(separator)
    }
  }
}

fun String.mapIndices() = mapIndexed { i, c -> c to i }.toMap()