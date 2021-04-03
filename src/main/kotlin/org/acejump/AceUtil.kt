package org.acejump

import com.intellij.openapi.editor.Editor

annotation class ExternalUsage

/**
 * Returns an immutable version of the currently edited document.
 */
val Editor.immutableText
  get() = this.document.immutableCharSequence

/**
 * Returns true if [this] contains [otherText] at the specified offset.
 */
fun CharSequence.matchesAt(selfOffset: Int, otherText: String, ignoreCase: Boolean): Boolean {
  return this.regionMatches(selfOffset, otherText, 0, otherText.length, ignoreCase)
}

/**
 * Calculates the length of a common prefix in [this] starting at index [selfOffset], and [otherText] starting at index 0.
 */
fun CharSequence.countMatchingCharacters(selfOffset: Int, otherText: String): Int {
  var i = 0
  var o = selfOffset + i
  
  while (i < otherText.length && o < this.length && otherText[i].equals(this[o], ignoreCase = true)) {
    i++
    o++
  }
  
  return i
}

/**
 * Determines which characters form a "word" for the purposes of functions below.
 */
val Char.isWordPart
  get() = this.isJavaIdentifierPart()

/**
 * Finds index of the first character in a word.
 */
inline fun CharSequence.wordStart(pos: Int, isPartOfWord: (Char) -> Boolean = Char::isWordPart): Int {
  var start = pos
  
  while (start > 0 && isPartOfWord(this[start - 1])) {
    --start
  }
  
  return start
}

/**
 * Finds index of the last character in a word.
 */
inline fun CharSequence.wordEnd(pos: Int, isPartOfWord: (Char) -> Boolean = Char::isWordPart): Int {
  var end = pos
  
  while (end < length - 1 && isPartOfWord(this[end + 1])) {
    ++end
  }
  
  return end
}

/**
 * Finds index of the first word character following a sequence of non-word characters following the end of a word.
 */
inline fun CharSequence.wordEndPlus(pos: Int, isPartOfWord: (Char) -> Boolean = Char::isWordPart): Int {
  var end = this.wordEnd(pos, isPartOfWord)
  
  while (end < length - 1 && !isPartOfWord(this[end + 1])) {
    ++end
  }
  
  if (end < length - 1 && isPartOfWord(this[end + 1])) {
    ++end
  }
  
  return end
}
