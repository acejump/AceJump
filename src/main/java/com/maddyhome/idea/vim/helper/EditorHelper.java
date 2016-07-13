package com.maddyhome.idea.vim.helper;

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

import com.intellij.openapi.editor.*;
import org.jetbrains.annotations.NotNull;

/**
 * This is a set of helper methods for working with editors. All line and column values are zero based.
 */
public class EditorHelper {
  public static int getVisualLineAtTopOfScreen(@NotNull final Editor editor) {
    int lh = editor.getLineHeight();
    return (editor.getScrollingModel().getVerticalScrollOffset() + lh - 1) / lh;
  }

  /**
   * Gets the number of actual lines in the file
   *
   * @param editor The editor
   * @return The file line count
   */
  public static int getLineCount(@NotNull final Editor editor) {
    int len = editor.getDocument().getLineCount();
    if (editor.getDocument().getTextLength() > 0 &&
        editor.getDocument().getCharsSequence().charAt(editor.getDocument().getTextLength() - 1) == '\n') {
      len--;
    }

    return len;
  }

  /**
   * Gets the actual number of characters in the file
   *
   * @param editor The editor
   * @return The file's character count
   */
  public static int getFileSize(@NotNull final Editor editor) {
    return getFileSize(editor, false);
  }

  /**
   * Gets the actual number of characters in the file
   *
   * @param editor            The editor
   * @param includeEndNewLine True include newline
   * @return The file's character count
   */
  public static int getFileSize(@NotNull final Editor editor, final boolean includeEndNewLine) {
    final int len = editor.getDocument().getTextLength();
    return includeEndNewLine || len == 0 || editor.getDocument().getCharsSequence().charAt(len - 1) != '\n' ? len :
        len - 1;
  }

  /**
   * Gets the number of lines than can be displayed on the screen at one time. This is rounded down to the
   * nearest whole line if there is a partial line visible at the bottom of the screen.
   *
   * @param editor The editor
   * @return The number of screen lines
   */
  public static int getScreenHeight(@NotNull final Editor editor) {
    int lh = editor.getLineHeight();
    int height = editor.getScrollingModel().getVisibleArea().y +
                 editor.getScrollingModel().getVisibleArea().height -
                 getVisualLineAtTopOfScreen(editor) * lh;
    return height / lh;
  }

  /**
   * Converts a visual line number to a logical line number.
   *
   * @param editor The editor
   * @param line   The visual line number to convert
   * @return The logical line number
   */
  public static int visualLineToLogicalLine(@NotNull final Editor editor, final int line) {
    int logicalLine = editor.visualToLogicalPosition(new VisualPosition(line, 0)).line;
    return normalizeLine(editor, logicalLine);
  }

  /**
   * Returns the offset of the start of the requested line.
   *
   * @param editor The editor
   * @param line   The logical line to get the start offset for.
   * @return 0 if line is &lt 0, file size of line is bigger than file, else the start offset for the line
   */
  public static int getLineStartOffset(@NotNull final Editor editor, final int line) {
    if (line < 0) {
      return 0;
    } else if (line >= getLineCount(editor)) {
      return getFileSize(editor);
    } else {
      return editor.getDocument().getLineStartOffset(line);
    }
  }

  /**
   * Returns the offset of the end of the requested line.
   *
   * @param editor   The editor
   * @param line     The logical line to get the end offset for
   * @param allowEnd True include newline
   * @return 0 if line is &lt 0, file size of line is bigger than file, else the end offset for the line
   */
  public static int getLineEndOffset(@NotNull final Editor editor, final int line, final boolean allowEnd) {
    if (line < 0) {
      return 0;
    } else if (line >= getLineCount(editor)) {
      return getFileSize(editor, allowEnd);
    } else {
      return editor.getDocument().getLineEndOffset(line) - (allowEnd ? 0 : 1);
    }
  }

  /**
   * Ensures that the supplied logical line is within the range 0 (incl) and the number of logical lines in the file
   * (excl).
   *
   * @param editor The editor
   * @param line   The logical line number to normalize
   * @return The normalized logical line number
   */
  public static int normalizeLine(@NotNull final Editor editor, final int line) {
    return Math.max(0, Math.min(line, getLineCount(editor) - 1));
  }

  /**
   * Ensures that the supplied offset for the given logical line is within the range for the line. If allowEnd
   * is true, the range will allow for the offset to be one past the last character on the line.
   *
   * @param editor   The editor
   * @param line     The logical line number
   * @param offset   The offset to normalize
   * @param allowEnd true if the offset can be one past the last character on the line, false if not
   * @return The normalized column number
   */
  public static int normalizeOffset(@NotNull final Editor editor,
                                    final int line,
                                    final int offset,
                                    final boolean allowEnd) {
    if (getFileSize(editor, allowEnd) == 0) {
      return 0;
    }

    int min = getLineStartOffset(editor, line);
    int max = getLineEndOffset(editor, line, allowEnd);
    return Math.max(Math.min(offset, max), min);
  }


}