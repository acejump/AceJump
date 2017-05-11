package com.johnlindquist.acejump.search

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.awt.RelativePoint
import java.lang.Math.max
import java.lang.Math.min

fun getDefaultEditor() = FileEditorManager.getInstance(ProjectManager
  .getInstance().openProjects[0]).selectedTextEditor!!

fun Editor.getPointFromVisualPosition(position: VisualPosition) =
  RelativePoint(contentComponent, visualPositionToXY(position))

fun Editor.getVisibleRange(): Pair<Int, Int> {
  val firstVisibleLine = getVisualLineAtTopOfScreen()
  val firstLine = visualLineToLogicalLine(firstVisibleLine)
  val startOffset = getLineStartOffset(firstLine)

  val height = getScreenHeight()
  val lastLine = visualLineToLogicalLine(firstVisibleLine + height)
  var endOffset = getLineEndOffset(lastLine, true)
  endOffset = normalizeOffset(lastLine, endOffset, true)
  endOffset = min(max(0, document.textLength - 1), endOffset + 1)

  return Pair(startOffset, endOffset)
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
 * @param editor The editor
 *
 * @return The file line count
 */

fun Editor.getLineCount() = with(document) {
    if (textLength > 0 && charsSequence[textLength - 1] == '\n') {
      lineCount - 1
    } else {
      lineCount
    }
  }

/**
 * Gets the actual number of characters in the file
 *
 * @param e            The editor
 *
 * @param includeEndNewLine True include newline
 *
 * @return The file's character count
 */

fun Editor.getFileSize(includeEndNewLine: Boolean = false): Int {
  val len = document.textLength
  val doc = document.charsSequence
  return if (includeEndNewLine || len == 0 || doc[len - 1] != '\n') len
  else len - 1
}

/**
 * Gets the number of lines than can be displayed on the screen at one time.
 * This is rounded down to the nearest whole line if there is a partial line
 * visible at the bottom of the screen.
 *
 * @param e The editor
 *
 * @return The number of screen lines
 */

fun Editor.getScreenHeight() =
  (scrollingModel.visibleArea.y + scrollingModel.visibleArea.height -
    getVisualLineAtTopOfScreen() * lineHeight) / lineHeight

/**
 * Converts a visual line number to a logical line number.
 *
 * @param e The editor
 *
 * @param line   The visual line number to convert
 *
 * @return The logical line number
 */

fun Editor.visualLineToLogicalLine(line: Int) =
  normalizeLine(visualToLogicalPosition(VisualPosition(line, 0)).line)

/**
 * Returns the offset of the start of the requested line.
 *
 * @param editor The editor
 *
 * @param line   The logical line to get the start offset for.
 *
 * @return 0 if line is &lt 0, file size of line is bigger than file, else the
 *         start offset for the line
 */

fun Editor.getLineStartOffset(line: Int) =
  if (line < 0) 0
  else if (line >= getLineCount()) getFileSize()
  else document.getLineStartOffset(line)

/**
 * Returns the offset of the end of the requested line.
 *
 * @param editor   The editor
 *
 * @param line     The logical line to get the end offset for
 *
 * @param allowEnd True include newline
 *
 * @return 0 if line is &lt 0, file size of line is bigger than file, else the
 * end offset for the line
 */

fun Editor.getLineEndOffset(line: Int, allowEnd: Boolean) =
  if (line < 0) 0
  else if (line >= getLineCount()) getFileSize(allowEnd)
  else document.getLineEndOffset(line) - if (allowEnd) 0 else 1

/**
 * Ensures that the supplied logical line is within the range 0 (incl) and the
 * number of logical lines in the file (excl).
 *
 * @param editor The editor
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
 * @param e   The editor
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
  if (getFileSize(allowEnd) == 0) 0
  else max(min(offset, getLineEndOffset(line, allowEnd)),
    getLineStartOffset(line))