package org.acejump

import com.anyascii.AnyAscii
import com.intellij.diff.util.DiffUtil.getLineCount
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.*
import com.intellij.openapi.util.Computable
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.acejump.config.AceConfig
import java.awt.Point
import kotlin.math.*

/**
 * This annotation is a marker which means that the annotated function is
 *   used in external plugins.
 */

@Retention(AnnotationRetention.SOURCE)
annotation class ExternalUsage

/**
 * Returns an immutable version of the currently edited document.
 */
val Editor.immutableText get() = EditorsCache.getText(this)

object EditorsCache {
  private var stale = true
  fun invalidate() {
    stale = true
    editorTexts.clear()
  }

  private val editorTexts = mutableMapOf<Editor, CharSequence>()

  fun getText(editor: Editor) =
    if (stale || editor !in editorTexts)
      editor.document.immutableCharSequence
        .let { if (AceConfig.mapToASCII) it.mapToASCII() else it }
        .also { editorTexts[editor] = it; stale = false }
    else editorTexts[editor]!!
}

fun CharSequence.mapToASCII() =
  map { AnyAscii.transliterate("$it").firstOrNull() ?: it }.joinToString("")

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

fun Editor.offsetCenter(first: Int, second: Int): LogicalPosition {
  val firstIndexLine = offsetToLogicalPosition(first).line
  val lastIndexLine = offsetToLogicalPosition(second).line
  val center = (firstIndexLine + lastIndexLine) / 2
  return offsetToLogicalPosition(getLineStartOffset(center))
}

// Borrowed from Editor.calculateVisibleRange() but only available after 232.6095.10
fun Editor.getView(): IntRange {
  ApplicationManager.getApplication().assertIsDispatchThread()
  val rect = scrollingModel.visibleArea
  val startPosition = xyToLogicalPosition(Point(rect.x, rect.y))
  val visibleStart = logicalPositionToOffset(startPosition)
  val endPosition = xyToLogicalPosition(Point(rect.x + rect.width, rect.y + rect.height))
  val visibleEnd = logicalPositionToOffset(LogicalPosition(endPosition.line + 1, 0))
  return visibleStart..visibleEnd
}

/**
 * Returns the offset of the start of the requested line.
 *
 * @param line   The logical line to get the start offset for.
 *
 * @return 0 if line is &lt 0, file size of line is bigger than file, else the
 *         start offset for the line
 */

fun Editor.getLineStartOffset(line: Int) =
  when {
    line < 0 -> 0
    line >= getLineCount(document) -> getFileSize()
    else -> document.getLineStartOffset(line)
  }

/**
 * Returns the offset of the end of the requested line.
 *
 * @param line     The logical line to get the end offset for
 *
 * @param allowEnd True include newline
 *
 * @return 0 if line is &lt 0, file size of line is bigger than file, else the
 * end offset for the line
 */

fun Editor.getLineEndOffset(line: Int, allowEnd: Boolean = true) =
  when {
    line < 0 -> 0
    line >= getLineCount(document) -> getFileSize(allowEnd)
    else -> document.getLineEndOffset(line) - if (allowEnd) 0 else 1
  }

/**
 * Gets the number of lines than can be displayed on the screen at one time.
 * This is rounded down to the nearest whole line if there is a partial line
 * visible at the bottom of the screen.
 *
 * @return The number of screen lines
 */

fun Editor.getScreenHeight() =
  (scrollingModel.visibleArea.y + scrollingModel.visibleArea.height -
    getVisualLineAtTopOfScreen() * lineHeight) / lineHeight


/**
 * This is a set of helper methods for working with editors.
 * All line and column values are zero based.
 */

fun Editor.getVisualLineAtTopOfScreen() =
  (scrollingModel.verticalScrollOffset + lineHeight - 1) / lineHeight

/**
 * Gets the actual number of characters in the file
 *
 * @param countNewLines True include newline
 *
 * @return The file's character count
 */

fun Editor.getFileSize(countNewLines: Boolean = false): Int {
  val len = document.textLength
  val doc = document.charsSequence
  return if (countNewLines || len == 0 || doc[len - 1] != '\n') len else len - 1
}

/**
 * Ensures that the supplied logical line is within the range 0 (incl) and the
 * number of logical lines in the file (excl).
 *
 * @param line   The logical line number to normalize
 *
 * @return The normalized logical line number
 */

fun Editor.normalizeLine(line: Int) = max(0, min(line, getLineCount(document) - 1))

/**
 * Converts a visual line number to a logical line number.
 *
 * @param line   The visual line number to convert
 *
 * @return The logical line number
 */

fun Editor.visualLineToLogicalLine(line: Int) =
  normalizeLine(visualToLogicalPosition(
    VisualPosition(line.coerceAtLeast(0), 0)).line)


/**
 * Ensures that the supplied offset for the given logical line is within the
 * range for the line. If allowEnd is true, the range will allow for the offset
 * to be one past the last character on the line.
 *
 * @param line     The logical line number
 *
 * @param offset   The offset to normalize
 *
 * @param allowEnd true if the offset can be one past the last character on the
 *                 line, false if not
 *
 * @return The normalized column number
 */

fun Editor.normalizeOffset(line: Int, offset: Int, allowEnd: Boolean = true) =
  if (getFileSize(allowEnd) == 0) 0 else
    max(min(offset, getLineEndOffset(line, allowEnd)), getLineStartOffset(line))

// https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html#read-access
fun <T> read(action: () -> T): T =
  ApplicationManager.getApplication().runReadAction(Computable { action() })

fun <T> write(action: () -> T): T =
  ApplicationManager.getApplication().runWriteAction(Computable { action() })
