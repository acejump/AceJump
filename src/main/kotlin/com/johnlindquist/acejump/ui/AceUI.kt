package com.johnlindquist.acejump.ui

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.project.Project
import com.johnlindquist.acejump.KeyboardHandler
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Pattern
import com.johnlindquist.acejump.search.Pattern.*
import com.johnlindquist.acejump.search.getDefaultEditor
import com.sun.glass.events.KeyEvent.VK_BACKSPACE
import java.awt.Color
import java.awt.Color.*
import java.awt.event.KeyEvent.*
import javax.swing.SwingUtilities.convertPoint

object AceUI {
  var editor: Editor = getDefaultEditor()
    set(value) {
      if(value != field) {
        KeyboardHandler.removeListeners()
        KeyboardHandler.resetUIState()
        field = value
      }

      //Todo: figure out how to avoid duplicating init block
      project = editor.project!!
      document = editor.document.charsSequence.toString().toLowerCase()
      findModel = FindManager.getInstance(project).findInFileModel.clone()
      findManager = FindManager.getInstance(project)

      scheme = EditorColorsManager.getInstance().globalScheme
      font = Canvas.font!!
      fontWidth = Canvas.getFontMetrics(font).stringWidth("w")
      fontHeight = font.size
      lineHeight = editor.lineHeight
      lineSpacing = scheme.lineSpacing
      fontSpacing = fontHeight * lineSpacing
      rectHOffset = fontSpacing - fontHeight
      hOffset = fontHeight - fontSpacing

      findModel.isFindAll = true
      findModel.isFromCursor = true
      findModel.isForward = true
      findModel.isRegularExpressions = false
      findModel.isWholeWordsOnly = false
      findModel.isCaseSensitive = false
      findModel.isPreserveCase = false
      findModel.setSearchHighlighters(true)
    }

  var naturalBlock: Boolean =
    EditorSettingsExternalizable.getInstance().isBlockCursor
  var naturalColor: Color =
    EditorColorsManager.getInstance().globalScheme.getColor(CARET_COLOR)!!
  var naturalBlink: Boolean =
    EditorSettingsExternalizable.getInstance().isBlinkCaret

  var scheme = EditorColorsManager.getInstance().globalScheme
  var font = Canvas.font!!
  var fontWidth = Canvas.getFontMetrics(font).stringWidth("w")
  var fontHeight = font.size
  var lineHeight = editor.lineHeight
  var lineSpacing = scheme.lineSpacing
  var fontSpacing = fontHeight * lineSpacing
  var rectHOffset = fontSpacing - fontHeight
  var hOffset = fontHeight - fontSpacing

  var project: Project
  var document: String
  var findModel: FindModel
  var findManager: FindManager

  val boxColor = red
  val editorHighlightColor = yellow
  val acejumpHighlightColor = green

  init {
    project = editor.project!!
    document = editor.document.charsSequence.toString().toLowerCase()
    findModel = FindManager.getInstance(project).findInFileModel.clone()
    findManager = FindManager.getInstance(project)

    scheme = EditorColorsManager.getInstance().globalScheme
    font = Canvas.font!!
    fontWidth = Canvas.getFontMetrics(font).stringWidth("w")
    fontHeight = font.size
    lineHeight = editor.lineHeight
    lineSpacing = scheme.lineSpacing
    fontSpacing = fontHeight * lineSpacing
    rectHOffset = fontSpacing - fontHeight
    hOffset = fontHeight - fontSpacing

    findModel.isFindAll = true
    findModel.isFromCursor = true
    findModel.isForward = true
    findModel.isRegularExpressions = false
    findModel.isWholeWordsOnly = false
    findModel.isCaseSensitive = false
    findModel.isPreserveCase = false
    findModel.setSearchHighlighters(true)
  }

  val keyMap = mutableMapOf(
    VK_HOME to { find(START_OF_LINE) },
    VK_LEFT to { find(START_OF_LINE) },
    VK_RIGHT to { find(END_OF_LINE) },
    VK_END to { find(END_OF_LINE) },
    VK_UP to { find(CODE_INDENTS) },
    VK_ESCAPE to { KeyboardHandler.resetUIState() },
    VK_BACKSPACE to { KeyboardHandler.processBackspaceCommand() }
  )

  fun find(pattern: Pattern) = Finder.findPattern(pattern)

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