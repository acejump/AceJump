package com.johnlindquist.acejump.ui

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColorsManager.getInstance
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.project.Project
import com.johnlindquist.acejump.KeyboardHandler
import com.johnlindquist.acejump.search.getDefaultEditor
import java.awt.Color.*
import java.awt.Font
import java.awt.Font.BOLD
import javax.swing.SwingUtilities.convertPoint

object AceUI {
  var editor: Editor = getDefaultEditor()
    set(value) {
      if (value == field) {
        return
      }

      try {
        KeyboardHandler.reset()
      } catch (e: UninitializedPropertyAccessException) {
        println(e)
      }
      field = value

      document = editor.document.charsSequence.toString().toLowerCase()

      with(EditorSettingsExternalizable.getInstance()) {
        naturalBlock = isBlockCursor
        naturalBlink = isBlinkCaret
      }

      naturalColor = getInstance().globalScheme.getColor(CARET_COLOR)!!
    }

  val project: Project
    get() = editor.project!!
  var document: String = editor.document.charsSequence.toString().toLowerCase()

  val findModel: FindModel by lazy {
    val findModel = FindModel()
    findModel.isFindAll = true
    findModel.setSearchHighlighters(true)
    findModel
  }

  val findManager: FindManager = FindManager.getInstance(project)
  var naturalBlock = EditorSettingsExternalizable.getInstance().isBlockCursor
  var naturalBlink = EditorSettingsExternalizable.getInstance().isBlinkCaret
  var naturalColor = getInstance().globalScheme.getColor(CARET_COLOR)!!

  val scheme: EditorColorsScheme
    get() = editor.colorsScheme
  val font: Font
    get() = Font(scheme.editorFontName, BOLD, scheme.editorFontSize)
  val fontWidth
    get() = editor.component.getFontMetrics(font).stringWidth("w")
  val fontHeight: Int
    get() = editor.colorsScheme.editorFontSize
  val lineHeight: Int
    get() = editor.lineHeight
  val lineSpacing: Float
    get() = scheme.lineSpacing
  val rectHOffset: Int
    get() = fontHeight - lineHeight + 4

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
    restoreCanvas()
    restoreCursor()
  }

  private fun restoreCanvas() {
    Canvas.reset()
    editor.contentComponent.remove(Canvas)
    editor.contentComponent.repaint()
  }

  private fun restoreCursor() {
    getApplication().invokeAndWait {
      with(editor) {
        settings.isBlinkCaret = naturalBlink
        settings.isBlockCursor = naturalBlock
        colorsScheme.setColor(CARET_COLOR, naturalColor)
      }
    }
  }
}