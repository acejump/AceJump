package org.acejump.input

/**
 * Defines common keyboard layouts. Each layout has a key priority order, based on each key's distance from the home row and how
 * ergonomically difficult they are to press.
 */
@Suppress("unused")
enum class KeyLayout(internal val rows: Array<String>, priority: String) {
  COLEMK(arrayOf("1234567890", "qwfpgjluy", "arstdhneio", "zxcvbkm"), priority = "tndhseriaovkcmbxzgjplfuwyq5849673210"),
  WORKMN(arrayOf("1234567890", "qdrwbjfup", "ashtgyneoi", "zxmcvkl"), priority = "tnhegysoaiclvkmxzwfrubjdpq5849673210"),
  DVORAK(arrayOf("1234567890", "pyfgcrl", "aoeuidhtns", "qjkxbmwvz"), priority = "uhetidonasxkbjmqwvzgfycprl5849673210"),
  QWERTY(arrayOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"), priority = "fjghdkslavncmbxzrutyeiwoqp5849673210"),
  QWERTZ(arrayOf("1234567890", "qwertzuiop", "asdfghjkl", "yxcvbnm"), priority = "fjghdkslavncmbxyrutzeiwoqp5849673210"),
  QGMLWY(arrayOf("1234567890", "qgmlwyfub", "dstnriaeoh", "zxcvjkp"), priority = "naterisodhvkcpjxzlfmuwygbq5849673210"),
  QGMLWB(arrayOf("1234567890", "qgmlwbyuv", "dstnriaeoh", "zxcfjkp"), priority = "naterisodhfkcpjxzlymuwbgvq5849673210"),
  NORMAN(arrayOf("1234567890", "qwdfkjurl", "asetgynioh", "zxcvbpm"), priority = "tneigysoahbvpcmxzjkufrdlwq5849673210");
  
  internal val allChars = rows.joinToString("").toCharArray().apply(CharArray::sort).joinToString("")
  internal val allPriorities = priority.mapIndexed { index, char -> char to index }.toMap()
  
  internal inline fun priority(crossinline tagToChar: (String) -> Char): (String) -> Int? {
    return { allPriorities[tagToChar(it)] }
  }
}
