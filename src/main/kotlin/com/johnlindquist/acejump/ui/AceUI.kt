package com.johnlindquist.acejump.ui

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColorsManager.getInstance
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.project.Project
import com.johnlindquist.acejump.KeyboardHandler
import com.johnlindquist.acejump.search.getDefaultEditor
import java.awt.Color.*
import javax.swing.SwingUtilities.convertPoint

object AceUI {
  lateinit var project: Project
  lateinit var document: String
  lateinit var findModel: FindModel
  lateinit var findManager: FindManager

  var editor: Editor = getDefaultEditor()
    set(value) {
      if (value != field) {
        try {
          KeyboardHandler.reset()
        } catch (e: UninitializedPropertyAccessException) { }
        field = value
      }

      project = editor.project!!
      document = editor.document.charsSequence.toString().toLowerCase()
      findModel = FindManager.getInstance(project).findInFileModel.clone()
      findManager = FindManager.getInstance(project)

      //TODO: add listener to update these when settings change
      scheme = getInstance().globalScheme
      fontWidth = Canvas.getFontMetrics(Canvas.font).stringWidth("w")
      fontHeight = Canvas.font.size
      lineHeight = editor.lineHeight
      lineSpacing = scheme.lineSpacing
      rectHOffset = fontHeight - lineHeight + 4

      findModel.isFindAll = true
      findModel.isFromCursor = true
      findModel.isForward = true
      findModel.isRegularExpressions = false
      findModel.isWholeWordsOnly = false
      findModel.isCaseSensitive = false
      findModel.isPreserveCase = false
      findModel.setSearchHighlighters(true)

      if (!KeyboardHandler.isEnabled) {
        naturalBlock = EditorSettingsExternalizable.getInstance().isBlockCursor
        naturalColor = getInstance().globalScheme.getColor(CARET_COLOR)!!
        naturalBlink = EditorSettingsExternalizable.getInstance().isBlinkCaret
      }
    }

  var naturalBlock = EditorSettingsExternalizable.getInstance().isBlockCursor
  var naturalColor = getInstance().globalScheme.getColor(CARET_COLOR)!!
  var naturalBlink = EditorSettingsExternalizable.getInstance().isBlinkCaret

  var scheme = getInstance().globalScheme
  var fontWidth = Canvas.getFontMetrics(Canvas.font).stringWidth("w")
  var fontHeight = Canvas.font.size
  var lineHeight = editor.lineHeight
  var lineSpacing = scheme.lineSpacing
  var rectHOffset = fontHeight - lineHeight + 4

  val boxColor = red
  val editorHighlightColor = yellow
  val acejumpHighlightColor = green

  fun setupCursor() {
    naturalBlock = editor.settings.isBlockCursor
    editor.settings.isBlockCursor = true

    naturalBlink = editor.settings.isBlinkCaret
    editor.settings.isBlinkCaret = false

    naturalColor = editor.colorsScheme.getColor(CARET_COLOR)!!
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
    editor.settings.isBlockCursor = naturalBlock
    editor.colorsScheme.setColor(CARET_COLOR, naturalColor)
  }
}