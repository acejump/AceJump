package com.johnlindquist.acejump.ui

import com.intellij.find.FindManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.johnlindquist.acejump.search.getDefaultEditor
import java.awt.Color
import java.awt.Color.BLUE
import javax.swing.SwingUtilities.convertPoint

object AceUI {
  var editor: Editor = getDefaultEditor()
  private val project
    get() = editor.project!!
  val document
    get() = editor.document.charsSequence.toString().toLowerCase()

  val findModel = FindManager.getInstance(project).findInFileModel.clone()
  val findManager: FindManager
    get() = FindManager.getInstance(project)
  val naturalCursor: Boolean
    get() = EditorSettingsExternalizable.getInstance().isBlockCursor
  val naturalColor: Color?
    get() = EditorColorsManager.getInstance().globalScheme.getColor(CARET_COLOR)
  val naturalBlink: Boolean
    get() = EditorSettingsExternalizable.getInstance().isBlinkCaret

  val scheme
    get() = EditorColorsManager.getInstance().globalScheme
  val font
    get() = Canvas.font!!
  val fontWidth
    get() = Canvas.getFontMetrics(font).stringWidth("w")
  val fontHeight
    get() = font.size
  val lineHeight: Int
    get() = AceUI.editor.lineHeight
  val lineSpacing
    get() = scheme.lineSpacing
  val fontSpacing
    get() = fontHeight * lineSpacing
  val rectHOffset
    get() = fontSpacing - fontHeight
  val hOffset
    get() = fontHeight - fontSpacing

  init {
    findModel.isFindAll = true
    findModel.isFromCursor = true
    findModel.isForward = true
    findModel.isRegularExpressions = false
    findModel.isWholeWordsOnly = false
    findModel.isCaseSensitive = false
    findModel.isPreserveCase = false
    findModel.setSearchHighlighters(true)
  }

  fun setupCursor() {
    editor.settings.isBlockCursor = true
    editor.settings.isBlinkCaret = false
    editor.colorsScheme.setColor(CARET_COLOR, BLUE)
  }

  fun setupCanvas() {
    editor.contentComponent.add(Canvas)
    val viewport = editor.scrollingModel.visibleArea
    Canvas.setBounds(0, 0, viewport.width + 1000, viewport.height + 1000)
    val loc = convertPoint(Canvas, Canvas.location, editor.component.rootPane)
    Canvas.setLocation(-loc.x, -loc.y)
  }

  fun restoreEditorSettings() {
    editor.contentComponent.remove(Canvas)
    editor.contentComponent.repaint()
    editor.settings.isBlinkCaret = naturalBlink
    editor.settings.isBlockCursor = naturalCursor
    editor.colorsScheme.setColor(CARET_COLOR, naturalColor)
  }
}
