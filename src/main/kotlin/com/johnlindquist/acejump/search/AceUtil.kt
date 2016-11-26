package com.johnlindquist.acejump.search

import com.intellij.find.FindManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.awt.RelativePoint
import com.johnlindquist.acejump.AceJumpAction.Companion.editor
import com.johnlindquist.acejump.ui.Canvas
import java.lang.Math.max
import java.lang.Math.min

object AceFont {
  var font = Canvas.font!!
  val fontWidth = Canvas.getFontMetrics(font).stringWidth("w")
  val fontHeight = font.size
  val lineHeight: Int
    get() = editor.lineHeight
  val lineSpacing = Canvas.scheme.lineSpacing
  val fontSpacing = fontHeight * lineSpacing
  val rectHOffset = fontSpacing - fontHeight
  val hOffset = fontHeight - fontSpacing
}

fun getDefaultEditor() = FileEditorManager.getInstance(ProjectManager
  .getInstance().openProjects[0]).selectedTextEditor!!

fun cloneProjectFindModel(project: Project) =
  FindManager.getInstance(project).findInFileModel.clone()

fun getDocumentFromEditor(editor: Editor) =
  editor.document.charsSequence.toString().toLowerCase()

fun getNaturalCursorColor() =
  EditorColorsManager.getInstance().globalScheme.getColor(EditorColors.CARET_COLOR)!!

fun getBlockCursorUserSetting() =
  EditorSettingsExternalizable.getInstance().isBlockCursor

fun getPointFromVisualPosition(editor: Editor,
                               logicalPosition: VisualPosition) =
  RelativePoint(editor.contentComponent,
    editor.visualPositionToXY(logicalPosition))

fun getVisibleRange(editor: Editor): Pair<Int, Int> {
  val firstVisibleLine = getVisualLineAtTopOfScreen(editor)
  val firstLine = visualLineToLogicalLine(editor, firstVisibleLine)
  val startOffset = getLineStartOffset(editor, firstLine)

  val height = getScreenHeight(editor)
  val lastLine = visualLineToLogicalLine(editor, firstVisibleLine + height)
  var endOffset = getLineEndOffset(editor, lastLine, true)
  endOffset = normalizeOffset(editor, lastLine, endOffset, true)
  endOffset = min(max(0, editor.document.textLength - 1), endOffset + 1)

  return Pair(startOffset, endOffset)
}

fun getThisLineLength(editor: Editor, offset: Int): Int {
  val pos = editor.offsetToVisualPosition(offset)
  if (pos.line - 1 > editor.offsetToVisualPosition(getVisibleRange(editor).first).line)
    return getVisualLineLength(editor, pos.line)
  return getVisualLineLength(editor, pos.line)
}

fun getPreviousLineLength(editor: Editor, offset: Int): Int {
  val pos = editor.offsetToVisualPosition(offset)
  if (pos.line - 1 > editor.offsetToVisualPosition(getVisibleRange(editor).first).line)
    return getVisualLineLength(editor, pos.line - 1)
  return getVisualLineLength(editor, pos.line)
}

fun getNextLineLength(editor: Editor, offset: Int): Int {
  val pos = editor.offsetToVisualPosition(offset)
  if (pos.line + 1 < editor.offsetToVisualPosition(getVisibleRange(editor).second).line)
    return getVisualLineLength(editor, pos.line + 1)
  return getVisualLineLength(editor, pos.line)
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
 * This is a set of helper methods for working with editors. All line and column values are zero based.
 */
fun getVisualLineAtTopOfScreen(editor: Editor): Int {
  val lh = editor.lineHeight
  return (editor.scrollingModel.verticalScrollOffset + lh - 1) / lh
}

/**
 * Gets the number of actual lines in the file

 * @param editor The editor

 * @return The file line count
 */
fun getLineCount(editor: Editor): Int {
  var len = editor.document.lineCount
  if (editor.document.textLength > 0 && editor.document.charsSequence[editor.document.textLength - 1] == '\n') {
    len--
  }

  return len
}

/**
 * Gets the actual number of characters in the file

 * @param editor            The editor

 * @param includeEndNewLine True include newline

 * @return The file's character count
 */
fun getFileSize(editor: Editor, includeEndNewLine: Boolean = false): Int {
  val len = editor.document.textLength
  return if (includeEndNewLine || len == 0 || editor.document.charsSequence[len - 1] != '\n')
    len
  else
    len - 1
}

/**
 * Gets the number of lines than can be displayed on the screen at one time. This is rounded down to the
 * nearest whole line if there is a partial line visible at the bottom of the screen.

 * @param editor The editor

 * @return The number of screen lines
 */
fun getScreenHeight(editor: Editor): Int {
  val lh = editor.lineHeight
  val height = editor.scrollingModel.visibleArea.y +
    editor.scrollingModel.visibleArea.height -
    getVisualLineAtTopOfScreen(editor) * lh
  return height / lh
}

/**
 * Converts a visual line number to a logical line number.

 * @param editor The editor

 * @param line   The visual line number to convert

 * @return The logical line number
 */
fun visualLineToLogicalLine(editor: Editor, line: Int) =
  normalizeLine(editor,
    editor.visualToLogicalPosition(VisualPosition(line, 0)).line)

/**
 * Returns the offset of the start of the requested line.

 * @param editor The editor

 * @param line   The logical line to get the start offset for.

 * @return 0 if line is &lt 0, file size of line is bigger than file, else the start offset for the line
 */
fun getLineStartOffset(editor: Editor, line: Int): Int {
  if (line < 0) {
    return 0
  } else if (line >= getLineCount(editor)) {
    return getFileSize(editor)
  } else {
    return editor.document.getLineStartOffset(line)
  }
}

/**
 * Returns the offset of the end of the requested line.

 * @param editor   The editor

 * @param line     The logical line to get the end offset for

 * @param allowEnd True include newline

 * @return 0 if line is &lt 0, file size of line is bigger than file, else the end offset for the line
 */
fun getLineEndOffset(editor: Editor, line: Int, allowEnd: Boolean): Int {
  if (line < 0) {
    return 0
  } else if (line >= getLineCount(editor)) {
    return getFileSize(editor, allowEnd)
  } else {
    return editor.document.getLineEndOffset(line) - if (allowEnd) 0 else 1
  }
}

/**
 * Ensures that the supplied logical line is within the range 0 (incl) and the number of logical lines in the file
 * (excl).

 * @param editor The editor

 * @param line   The logical line number to normalize

 * @return The normalized logical line number
 */
fun normalizeLine(editor: Editor, line: Int) =
  max(0, min(line, getLineCount(editor) - 1))

/**
 * Ensures that the supplied offset for the given logical line is within the range for the line. If allowEnd
 * is true, the range will allow for the offset to be one past the last character on the line.

 * @param editor   The editor

 * @param line     The logical line number

 * @param offset   The offset to normalize

 * @param allowEnd true if the offset can be one past the last character on the line, false if not

 * @return The normalized column number
 */
fun normalizeOffset(editor: Editor,
                    line: Int,
                    offset: Int,
                    allowEnd: Boolean): Int {
  if (getFileSize(editor, allowEnd) == 0) {
    return 0
  }

  val min = getLineStartOffset(editor, line)
  val max = getLineEndOffset(editor, line, allowEnd)
  return max(min(offset, max), min)
}

/**
 * Gets the number of characters on the specified visual line. This will be different than the number of visual
 * characters if there are "real" tabs in the line.

 * @param editor The editor
 * *
 * @param line   The visual line within the file
 * *
 * @return The number of characters in the specified line
 */
fun getVisualLineLength(editor: Editor, line: Int) =
  getLineLength(editor, visualLineToLogicalLine(editor, line))

/**
 * Gets the number of characters on the specified logical line. This will be different than the number of visual
 * characters if there are "real" tabs in the line.

 * @param editor The editor
 * *
 * @param line   The logical line within the file
 * *
 * @return The number of characters in the specified line
 */

fun getLineLength(editor: Editor, line: Int): Int {
  if (getLineCount(editor) === 0) {
    return 0
  } else {
    return max(0,
      editor.offsetToLogicalPosition(editor.document.getLineEndOffset(line)).column)
  }
}


fun getLengthFromStartToOffset(editor: Editor, offset: Int): Int {
  if (getLineCount(editor) === 0) {
    return 0
  } else {
    return max(0, editor.offsetToLogicalPosition(offset).column)
  }
}

fun getLeadingCharacterOffset(editor: Editor, line: Int) =
  getLeadingCharacterOffset(editor, line, 0)

fun getLeadingCharacterOffset(editor: Editor, line: Int, col: Int): Int {
  val start = getLineStartOffset(editor, line) + col
  val end = getLineEndOffset(editor, line, true)
  val chars = editor.document.charsSequence
  var pos = end
  for (offset in start..end - 1) {
    if (offset >= chars.length) {
      break
    }

    if (!Character.isWhitespace(chars[offset])) {
      pos = offset
      break
    }
  }

  return pos
}

