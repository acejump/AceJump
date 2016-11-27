package com.johnlindquist.acejump.ui

import com.intellij.find.FindManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.johnlindquist.acejump.search.*
import java.awt.Color
import java.awt.Color.BLUE
import javax.swing.JRootPane
import javax.swing.SwingUtilities

object AceUI {
  var editor: Editor = getDefaultEditor()
  var project = editor.project!!
  var document = getDocumentFromEditor(editor)
  var findModel = cloneProjectFindModel(project)
  var findManager = FindManager.getInstance(project)
  val naturalCursor: Boolean
    get() = getBlockCursorUserSetting()
  val naturalColor: Color
    get() = getNaturalCursorColor()
  val naturalBlink: Boolean
    get() = getNaturalCursorBlink()

  fun setupCursor() {
    editor.settings.isBlockCursor = true
    editor.settings.isBlinkCaret = false
    editor.colorsScheme.setColor(CARET_COLOR, BLUE)
  }

  fun setupCanvas() {
    editor.contentComponent.add(Canvas)
    val viewport = editor.scrollingModel.visibleArea
    Canvas.setBounds(0, 0, viewport.width + 1000, viewport.height + 1000)
    val root: JRootPane = editor.component.rootPane
    val loc = SwingUtilities.convertPoint(Canvas, Canvas.location, root)
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