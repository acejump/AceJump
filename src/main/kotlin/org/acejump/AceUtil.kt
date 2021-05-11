package org.acejump

import com.anyascii.AnyAscii
import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.acejump.config.AceConfig
import java.lang.ref.WeakReference

/**
 * This annotation is a marker which means that the annotated function is
 *   used in external plugins.
 */

@Retention(AnnotationRetention.SOURCE)
annotation class ExternalUsage

/**
 * Returns an immutable version of the currently edited document.
 */
val Editor.immutableText get() = EditorCache.getText(this)

object EditorCache {
  private var text: CharSequence = ""
  private var editor: WeakReference<Editor>? = null

  fun getText(editor: Editor): CharSequence {
    if (this.editor?.get() !== editor) {
      this.text = editor.document.immutableCharSequence
        .let { if (AceConfig.mapToASCII) it.mapToASCII() else it }
      this.editor = WeakReference(editor)
    }
    
    return text
  }
}

fun CharSequence.mapToASCII() =
  map { AnyAscii.transliterate("$it").first() }.joinToString("")

/**
 * Returns true if [this] contains [otherText] at the specified offset.
 */
fun CharSequence.matchesAt(selfOffset: Int, otherText: String, ignoreCase: Boolean) =
  regionMatches(selfOffset, otherText, 0, otherText.length, ignoreCase)

/**
 * Calculates the length of a common prefix in [this] starting
 * at index [selfOffset], and [otherText] starting at index 0.
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
inline fun CharSequence.wordStart(
  pos: Int, isPartOfWord: (Char) -> Boolean = Char::isWordPart
): Int {
  var start = pos

  while (start > 0 && isPartOfWord(this[start - 1])) --start

  return start
}

/**
 * Finds index of the last character in a word.
 */
inline fun CharSequence.wordEnd(
  pos: Int, isPartOfWord: (Char) -> Boolean = Char::isWordPart
): Int {
  var end = pos

  while (end < length - 1 && isPartOfWord(this[end + 1])) ++end

  return end
}

/**
 * Finds index of the first word character following a sequence of non-word
 * characters following the end of a word.
 */
inline fun CharSequence.wordEndPlus(
  pos: Int, isPartOfWord: (Char) -> Boolean = Char::isWordPart
): Int {
  var end = this.wordEnd(pos, isPartOfWord)

  while (end < length - 1 && !isPartOfWord(this[end + 1])) ++end

  if (end < length - 1 && isPartOfWord(this[end + 1])) ++end

  return end
}

fun MutableMap<Editor, IntArrayList>.clone(): MutableMap<Editor, IntArrayList> {
  val clone = HashMap<Editor, IntArrayList>(size)
  
  for ((editor, offsets) in this) {
    clone[editor] = offsets.clone()
  }
  
  return clone
}
