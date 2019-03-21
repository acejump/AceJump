package org.acejump.search

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.awt.RelativePoint
import org.acejump.view.Model.MAX_TAG_RESULTS
import org.acejump.view.Model.viewBounds
import java.awt.Point
import java.util.*
import javax.swing.JComponent
import kotlin.math.*

interface Resettable {
  fun reset()
}

fun aceString(s: String) = ResourceBundle.getBundle("AceResources").getString(s)

fun String.mapIndices() = mapIndexed { i, c -> Pair(c, i) }.toMap()

fun <P> applyTo(vararg ps: P, fx: P.() -> Unit) = ps.forEach { it.fx() }

operator fun Point.component1() = x
operator fun Point.component2() = y

operator fun CharSequence.get(i: Int, j: Int) = substring(i, j).toCharArray()

fun String.hasSpaceRight(i: Int) = length <= i + 1 || this[i + 1].isWhitespace()

@Deprecated("This is applied too broadly. Narrow down usages where necessary.")
fun runAndWait(t: () -> Unit) = ApplicationManager.getApplication().invokeAndWait(t)

@Deprecated("This is applied too broadly. Narrow down usages where necessary.")
fun runLater(t: () -> Unit) = ApplicationManager.getApplication().invokeLater(t)

fun Editor.offsetCenter(first: Int, second: Int): LogicalPosition {
  val firstIndexLine = offsetToLogicalPosition(first).line
  val lastIndexLine = offsetToLogicalPosition(second).line
  val center = (firstIndexLine + lastIndexLine) / 2
  return offsetToLogicalPosition(getLineStartOffset(center))
}

fun Editor.getNameOfFileInEditor() =
  FileDocumentManager.getInstance().getFile(document)?.presentableName

fun Editor.isNotFolded(offset: Int) = !foldingModel.isOffsetCollapsed(offset)

/**
 * Identifies the bounds of a word, defined as a contiguous group of letters
 * and digits, by expanding the provided index until a non-matching character
 * is seen on either side.
 */

fun String.wordBounds(index: Int): Pair<Int, Int> {
  var (first, last) = Pair(index, index)
  while (0 < first && get(first - 1).isJavaIdentifierPart()) first--
  while (last < length && get(last).isJavaIdentifierPart()) last++
  return Pair(first, last)
}

fun String.wordBoundsPlus(index: Int): Pair<Int, Int> {
  var (left, right) = wordBounds(index)

  (right..(right + 3).coerceAtMost(length - 1)).asSequence()
    .takeWhile { !(get(it) == '\n' || get(it) == '\r') }
    .forEach { right = it }

  return Pair(left, right)
}

fun defaultEditor() = FileEditorManager.getInstance(ProjectManager.getInstance()
  .run { openProjects.firstOrNull() ?: defaultProject })
  .run { selectedTextEditor ?: allEditors.first { it is TextEditor } as Editor }

fun Editor.getPoint(idx: Int) = visualPositionToXY(offsetToVisualPosition(idx))

fun Editor.getPointRelative(index: Int, relativeToComponent: JComponent) =
  RelativePoint(relativeToComponent, getPoint(index)).originalPoint!!

fun Editor.isFirstCharacterOfLine(index: Int) =
  index == getLineStartOffset(offsetToLogicalPosition(index).line)

/**
 * Returns up to MAX_TAG_RESULTS by accumulating results before and after the
 * view boundaries, (approximately centered around the middle of the screen).
 *
 * @see MAX_TAG_RESULTS
 * @return
 */

fun getFeasibleRegion(results: List<Int>, takeAtMost: Int = MAX_TAG_RESULTS) =
  ((viewBounds.first + viewBounds.last) / 2).let { middleOfScreen ->
    results.sortedBy { abs(middleOfScreen - it) }
      .take(min(results.size, takeAtMost))
  }.sorted().let { if (it.isNotEmpty()) it.first()..it.last() else null }

fun Editor.getView(): IntRange {
  val firstVisibleLine = max(0, getVisualLineAtTopOfScreen() - 1)
  val firstLine = visualLineToLogicalLine(firstVisibleLine)
  val startOffset = getLineStartOffset(firstLine)

  val height = getScreenHeight() + 2
  val lastLine = visualLineToLogicalLine(firstVisibleLine + height)
  var endOffset = getLineEndOffset(lastLine, true)
  endOffset = normalizeOffset(lastLine, endOffset, true)
  endOffset = min(max(0, document.textLength - 1), endOffset + 1)

  return startOffset..endOffset
}

fun Editor.selectRange(fromOffset: Int, toOffset: Int) = runAndWait {
    selectionModel.removeSelection()
    selectionModel.setSelection(fromOffset, toOffset)
    caretModel.moveToOffset(toOffset)
  }

/**
 * Returns whether two indices can be simultaneously visible on screen
 */

fun Editor.canIndicesBeSimultaneouslyVisible(idx0: Int, idx1: Int): Boolean {
  val line1 = offsetToLogicalPosition(idx0).line
  val line2 = offsetToLogicalPosition(idx1).line

  return Math.abs(line1 - line2) < getScreenHeight()
}

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

/**
 * This is a set of helper methods for working with editors.
 * All line and column values are zero based.
 */

fun Editor.getVisualLineAtTopOfScreen() =
  (scrollingModel.verticalScrollOffset + lineHeight - 1) / lineHeight

/**
 * Gets the number of actual lines in the file
 *
 * @return The file line count
 */

fun Editor.getLineCount() = document.run {
  lineCount - if (textLength > 0 && text[textLength - 1] == '\n') 1 else 0
}

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
    line >= getLineCount() -> getFileSize()
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

fun Editor.getLineEndOffset(line: Int, allowEnd: Boolean) =
  when {
    line < 0 -> 0
    line >= getLineCount() -> getFileSize(allowEnd)
    else -> document.getLineEndOffset(line) - if (allowEnd) 0 else 1
  }

/**
 * Ensures that the supplied logical line is within the range 0 (incl) and the
 * number of logical lines in the file (excl).
 *
 * @param line   The logical line number to normalize
 *
 * @return The normalized logical line number
 */

fun Editor.normalizeLine(line: Int) = max(0, min(line, getLineCount() - 1))

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

fun Editor.normalizeOffset(line: Int, offset: Int, allowEnd: Boolean) =
  if (getFileSize(allowEnd) == 0) 0 else
    max(min(offset, getLineEndOffset(line, allowEnd)), getLineStartOffset(line))