package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.johnlindquist.acejump.AceJumpAction.Companion.editor
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Jumper
import com.johnlindquist.acejump.search.Pattern.Companion.keyMap
import com.johnlindquist.acejump.ui.Canvas
import com.sun.glass.events.KeyEvent.VK_BACKSPACE
import java.awt.event.KeyEvent.*
import javax.swing.JRootPane
import javax.swing.SwingUtilities
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class KeyboardHandler {
  var text = ""
  var keyHandler = EditorActionManager.getInstance().typedAction.rawHandler
  val specials = intArrayOf(VK_BACKSPACE, VK_LEFT, VK_RIGHT, VK_UP, VK_ESCAPE)

  init {
    configureKeyMap()
    configureEditor()

    Finder.eventDispatcher.addListener(ChangeListener {
      Canvas.jumpLocations = Finder.jumpLocations
      if (Jumper.hasJumped) {
        Jumper.hasJumped = false
        exit()
      }

      Canvas.repaint()
    })
  }

  fun handleKeystroke(key: Char) {
    //fixes the delete bug
    if (key == '\b') return

    text += key
    //Find or jump
    Finder.find(text, key)
    Finder.eventDispatcher.multicaster.stateChanged(ChangeEvent("Finder"))
  }

  fun processRegexCommand(keyCode: Int) {
    keyMap[keyCode]?.invoke()
  }

  fun processBackspaceCommand() {
    text = ""
    handleKeystroke(0.toChar())
  }

  private fun configureEditor() {
    addAceCanvas()
    EditorActionManager.getInstance().typedAction.setupRawHandler {
      editor: Editor, c: Char, dataContext: DataContext ->
      handleKeystroke(c)
    }
    editor.scrollingModel.addVisibleAreaListener { exit() }
  }

  private fun configureKeyMap() {
    specials.forEach {
      ActionManager.getInstance().getAction("AceJumpKeyAction")
        .registerCustomShortcutSet(it, 0, editor.component)
    }
  }

  fun addAceCanvas() {
    editor.contentComponent.add(Canvas)
    val viewport = editor.scrollingModel.visibleArea
    Canvas.setBounds(0, 0, viewport.width + 1000, viewport.height + 1000)
    val root: JRootPane = editor.component.rootPane
    val loc = SwingUtilities.convertPoint(Canvas, Canvas.location, root)
    Canvas.setLocation(-loc.x, -loc.y)
  }

  fun exit() {
    editor.contentComponent.remove(Canvas)
    editor.contentComponent.repaint()
    specials.forEach {
      ActionManager.getInstance().getAction("AceJumpKeyAction")
        .unregisterCustomShortcutSet(editor.component)
    }

    EditorActionManager.getInstance().typedAction.setupRawHandler(keyHandler)
    Finder.reset()
  }
}