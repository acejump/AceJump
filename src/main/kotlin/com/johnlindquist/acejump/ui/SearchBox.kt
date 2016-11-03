package com.johnlindquist.acejump.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.ui.popup.ComponentPopupBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.popup.AbstractPopup
import com.johnlindquist.acejump.keycommands.*
import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Pattern
import com.johnlindquist.acejump.search.guessBestLocation
import java.awt.Color.RED
import java.awt.Color.WHITE
import java.awt.Dimension
import java.awt.Font
import java.awt.Font.BOLD
import java.awt.Graphics
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import javax.swing.AbstractAction
import javax.swing.JRootPane
import javax.swing.JTextField
import javax.swing.KeyStroke.getKeyStroke
import javax.swing.SwingUtilities
import javax.swing.event.ChangeListener

class SearchBox(val finder: AceFinder, val editor: EditorImpl) : JTextField() {
  var aceCanvas = AceCanvas(editor)
  var keyMap: Map<Int, AceKeyCommand> = hashMapOf()
  var popupContainer: AbstractPopup? = null
  var defaultKeyCommand = DefaultKeyCommand(finder)
  var naturalColor = WHITE

  init {
    configureKeyMap()
    configurePopup()

    finder.eventDispatcher.addListener(ChangeListener {
      aceCanvas.jumpLocations = finder.jumpLocations
      if (finder.hasJumped) {
        finder.hasJumped = false
        popupContainer?.cancel()
        exit()
      }

      aceCanvas.repaint()
    })

    val search = "dispatch"
    (' '..'~').forEach { inputMap.put(getKeyStroke(it), search) }
    actionMap.put(search, object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        if (e.modifiers == 0) {
          text += e.actionCommand
          defaultKeyCommand.execute(e.actionCommand[0], text)
        } else if (e.modifiers == SHIFT_MASK) {
          text += e.actionCommand
          defaultKeyCommand.execute(e.actionCommand[0].toUpperCase(), text)
        }
      }
    })

    (VK_LEFT..VK_RIGHT).forEach {
      val keyName: String = KeyEvent.getKeyText(it)
      inputMap.put(getKeyStroke(it, 0), keyName)
      actionMap.put(keyName, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
          text = Pattern.REGEX_PREFIX.toString()
          keyMap[it]!!.execute()
        }
      })
    }

    val aja = "AceJumpAction"
    val scs = ActionManager.getInstance().getAction(aja).shortcutSet.shortcuts
    scs.forEach {
      if(it.isKeyboard) {
        val kbs = it as KeyboardShortcut
        inputMap.put(kbs.firstKeyStroke, aja)
        actionMap.put(aja, object : AbstractAction() {
          override fun actionPerformed(e: ActionEvent) {
            if (finder.toggleTargetMode())
              background = RED
            else
              background = naturalColor
          }
        })
      }
    }
  }

  /*
   * For some reason, Swing does not like to pass us actions via the inputMap or
   * actionMap registration technique. Until that works reliably, we need to use
   * low-level KeyEvents for processing the following keystrokes.
   */
  override fun processKeyEvent(p0: KeyEvent) {
    if (p0.keyCode == VK_BACK_SPACE && p0.id == KEY_RELEASED) {
      text = ""
      defaultKeyCommand.execute(0.toChar())
    } else if (p0.keyCode == VK_HOME && p0.id == KEY_RELEASED) {
      text = Pattern.REGEX_PREFIX.toString()
      keyMap[VK_HOME]!!.execute()
    } else if (p0.keyCode == VK_END && p0.id == KEY_RELEASED) {
      text = Pattern.REGEX_PREFIX.toString()
      keyMap[VK_END]!!.execute()
    }

    super.processKeyEvent(p0)
  }

  private fun configurePopup() {
    val scheme = EditorColorsManager.getInstance().globalScheme
    val font = Font(scheme.editorFontName, BOLD, scheme.editorFontSize)
    val pb: ComponentPopupBuilder? =
      JBPopupFactory.getInstance()?.createComponentPopupBuilder(this, this)
    pb?.setCancelKeyEnabled(true)
    val popup = pb?.createPopup() as AbstractPopup?
    popup?.show(guessBestLocation(editor))
    popup?.setRequestFocus(true)

    val width = getFontMetrics(font).stringWidth("w")
    val dimension = Dimension(width * 2, (editor.lineHeight))
    if (SystemInfo.isMac) {
      dimension.setSize(dimension.width * 2, dimension.height * 2)
    }

    popup?.size = dimension
    size = dimension
    isFocusable = true
    addFocusListener(object : FocusListener {
      override fun focusGained(p0: FocusEvent) = addAceCanvas()
      override fun focusLost(p0: FocusEvent) = exit()
    })
    popupContainer = popup
    naturalColor = background
  }

  private fun configureKeyMap() {
    val showBeginningOfLines = ShowStartOfLines(finder)
    val showEndOfLines = ShowEndOfLines(finder)
    keyMap = mapOf(VK_HOME to showBeginningOfLines,
      VK_LEFT to showBeginningOfLines,
      VK_RIGHT to showEndOfLines,
      VK_END to showEndOfLines,
      VK_UP to ShowFirstLetters(finder),
      VK_SPACE to ShowWhiteSpace(finder))
  }

  override fun requestFocus() {
    transferHandler = null
    super.requestFocus()
  }

  override fun paintBorder(p0: Graphics?) = Unit

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
    finder.tagMap.clear()
  }
}