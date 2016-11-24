package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.ui.popup.AbstractPopup
import com.johnlindquist.acejump.keycommands.*
import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Pattern.Companion.REGEX_PREFIX
import com.johnlindquist.acejump.ui.AceCanvas
import com.sun.glass.events.KeyEvent.VK_BACKSPACE
import java.awt.event.KeyEvent.*
import javax.swing.JRootPane
import javax.swing.SwingUtilities
import javax.swing.event.ChangeListener

class KeyboardHandler(val finder: AceFinder, var editor: EditorImpl) {
  var text = ""
  var aceCanvas = AceCanvas(editor)
  var keyMap: Map<Int, AceKeyCommand> = hashMapOf()
  var popupContainer: AbstractPopup? = null
  var defaultKeyCommand = DefaultKeyCommand(finder)
  var keyHandler = EditorActionManager.getInstance().typedAction.rawHandler
  val specials = intArrayOf(VK_BACKSPACE, VK_LEFT, VK_RIGHT, VK_UP, VK_ESCAPE)

  init {
    configureKeyMap()
    configureEditor()
    EditorActionManager.getInstance().typedAction.setupRawHandler {
      editor: Editor, c: Char, dataContext: DataContext ->
      text += c
      defaultKeyCommand.execute(c, text)
    }

    finder.eventDispatcher.addListener(ChangeListener {
      aceCanvas.jumpLocations = finder.jumpLocations
      if (finder.hasJumped) {
        finder.hasJumped = false
        popupContainer?.cancel()
        exit()
      }

      aceCanvas.repaint()
    })
  }

  fun processRegexCommand(aceKeyCommand: AceKeyCommand) {
    text = REGEX_PREFIX.toString()
    aceKeyCommand.execute()
  }

  fun processRegexCommand(keyCode: Int) {
    text = REGEX_PREFIX.toString()
    keyMap[keyCode]?.execute()
  }

  fun processBackspaceCommand() {
    text = ""
    defaultKeyCommand.execute(0.toChar())
  }

  private fun configureEditor() {
    addAceCanvas()
    editor.scrollingModel.addVisibleAreaListener { exit() }
  }

  private fun configureKeyMap() {
    specials.forEach {
      ActionManager.getInstance().getAction("AceJumpKeyAction")
        .registerCustomShortcutSet(it, 0, editor.component)
    }

    val showBeginningOfLines = ShowStartOfLines(finder)
    val showEndOfLines = ShowEndOfLines(finder)
    keyMap = mapOf(VK_HOME to showBeginningOfLines,
      VK_LEFT to showBeginningOfLines,
      VK_RIGHT to showEndOfLines,
      VK_END to showEndOfLines,
      VK_UP to ShowFirstLetters(finder),
      VK_SPACE to ShowWhiteSpace(finder))
  }

  fun addAceCanvas() {
    editor.contentComponent.add(aceCanvas)
    val viewport = editor.scrollPane.viewport
    aceCanvas.setBounds(0, 0, viewport.width + 1000, viewport.height + 1000)
    val root: JRootPane = editor.component.rootPane
    val loc = SwingUtilities.convertPoint(aceCanvas, aceCanvas.location, root)
    aceCanvas.setLocation(-loc.x, -loc.y)
  }

  fun exit() {
    val contentComponent = editor.contentComponent
    contentComponent.remove(aceCanvas)
    contentComponent.repaint()
    EditorActionManager.getInstance().typedAction.setupRawHandler(keyHandler)
    specials.forEach {
      ActionManager.getInstance().getAction("AceJumpKeyAction")
        .unregisterCustomShortcutSet(editor.component)
    }
    popupContainer?.dispose()
    finder.reset()
  }
}