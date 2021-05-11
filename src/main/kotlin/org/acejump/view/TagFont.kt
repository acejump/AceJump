package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType.BOLD
import com.intellij.openapi.editor.colors.EditorFontType.PLAIN
import java.awt.Font
import java.awt.FontMetrics

/**
 * Stores font metrics for aligning and rendering [TagMarker]s.
 */
class TagFont(editor: Editor) {
  val tagFont: Font = editor.colorsScheme.getFont(BOLD)
  val tagCharWidth = editor.component.getFontMetrics(tagFont).charWidth('W')

  val editorFontMetrics: FontMetrics =
    editor.component.getFontMetrics(editor.colorsScheme.getFont(PLAIN))
  val lineHeight = editor.lineHeight
  val baselineDistance = editor.ascent
}
