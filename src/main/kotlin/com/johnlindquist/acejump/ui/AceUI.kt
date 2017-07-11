package com.johnlindquist.acejump.ui

import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.application.ModalityState.defaultModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColorsManager.getInstance
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.EffectType.BOXED
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.johnlindquist.acejump.KeyboardHandler
import com.johnlindquist.acejump.search.getDefaultEditor
import com.johnlindquist.acejump.settings.AceSettingsPage
import java.awt.Color
import java.awt.Color.*
import java.awt.Font
import java.awt.Font.BOLD

object AceUI {
  var editor: Editor = getDefaultEditor()
    set(value) {
      editorText = value.document.text.toLowerCase()
      if (value == field) return

      // When the editor is updated, we must update some properties
      KeyboardHandler.reset()

      field = value

      EditorSettingsExternalizable.getInstance().run {
        naturalBlock = isBlockCursor
        naturalBlink = isBlinkCaret
      }

      naturalColor = getInstance().globalScheme.getColor(CARET_COLOR) ?: BLACK
    }

  val project: Project
    get() = editor.project!!
  var editorText = editor.document.text.toLowerCase()

  var naturalBlock = EditorSettingsExternalizable.getInstance().isBlockCursor
  var naturalBlink = EditorSettingsExternalizable.getInstance().isBlinkCaret
  var naturalColor = getInstance().globalScheme.getColor(CARET_COLOR)!!

  val targetModeStyle = TextAttributes(null, null, RED, BOXED, Font.PLAIN)
  val highlightStyle = TextAttributes(null, GREEN, GREEN, BOXED, Font.PLAIN)

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
  val rectHeight: Int
    get() = fontHeight + 3
  val rectHOffset: Int
    get() = lineHeight - (editor as EditorImpl).descent - fontHeight

  data class UserSettings(var allowedChars: List<Char> = ('a'..'z').toList(),
                          var jumpModeColor: Color = blue,
                          var targetModeColor: Color = red,
                          var textHighLightColor: Color = green,
                          var tagForegroundColor: Color = black,
                          var tagBackgroundColor: Color = yellow)

  var settings = UserSettings()
  var gui = AceSettingsPage()

  fun Editor.setupCursor() {
    naturalBlock = settings.isBlockCursor
    settings.isBlockCursor = true

    naturalBlink = settings.isBlinkCaret
    settings.isBlinkCaret = false

    naturalColor = colorsScheme.getColor(CARET_COLOR)!!
    colorsScheme.setColor(CARET_COLOR, AceUI.settings.jumpModeColor)
  }

  fun restoreEditorSettings() {
    restoreCanvas()
    restoreCursor()
  }

  private fun restoreCanvas() =
    editor.component.run {
      Canvas.reset()
      remove(Canvas)
      repaint()
    }

  private fun restoreCursor() =
    getApplication().invokeAndWait({
      editor.run {
        settings.isBlinkCaret = naturalBlink
        settings.isBlockCursor = naturalBlock
        colorsScheme.setColor(CARET_COLOR, naturalColor)
      }
    }, defaultModalityState())
}