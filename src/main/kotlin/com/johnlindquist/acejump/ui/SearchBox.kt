package com.johnlindquist.acejump.ui

import com.google.common.collect.BiMap
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.ui.popup.ComponentPopupBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.popup.AbstractPopup
import com.johnlindquist.acejump.keycommands.*
import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.guessBestLocation
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import javax.swing.JRootPane
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.event.ChangeListener

class SearchBox(val aceFinder: AceFinder, val editor: EditorImpl) : JTextField() {
  var aceCanvas = AceCanvas(editor)
  var keyMap: Map<Int, AceKeyCommand> = hashMapOf()
  var popupContainer: AbstractPopup? = null
  var defaultKeyCommand = DefaultKeyCommand(aceFinder)

  init {
    keyMap = configureKeyMap()
    popupContainer = configurePopup()

    aceFinder.eventDispatcher.addListener(ChangeListener {
      aceCanvas.jumpInfos = aceFinder.plotJumpLocations()
      if (aceCanvas.jumpInfos.isEmpty() || aceCanvas.jumpInfos.size == 1) {
        popupContainer?.cancel()
        exit()
      }
      aceCanvas.repaint()
    })
  }

  private fun configurePopup(): AbstractPopup? {
    val scheme = EditorColorsManager.getInstance().globalScheme
    val font = Font(scheme.editorFontName, Font.BOLD, scheme.editorFontSize)
    val popupBuilder: ComponentPopupBuilder? =
      JBPopupFactory.getInstance()?.createComponentPopupBuilder(this, this)
    popupBuilder?.setCancelKeyEnabled(true)
    val popup = popupBuilder?.createPopup() as AbstractPopup?
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
      override fun focusGained(p0: FocusEvent) {
        addAceCanvas()
      }

      override fun focusLost(p0: FocusEvent) {
        exit()
      }
    })
    return popup
  }

  private fun configureKeyMap(): Map<Int, AceKeyCommand> {
    val showBeginningOfLines = ShowBeginningOfLines(aceFinder)
    val showEndOfLines = ShowEndOfLines(aceFinder)
    return mapOf(VK_HOME to showBeginningOfLines,
      VK_LEFT to showBeginningOfLines,
      VK_RIGHT to showEndOfLines,
      VK_END to showEndOfLines,
      VK_UP to ShowFirstCharOfLines(aceFinder),
      VK_SPACE to ShowWhiteSpace(aceFinder))
  }

  override fun requestFocus() {
    transferHandler = null
    super.requestFocus()
  }

  override fun paintBorder(p0: Graphics?) {
  }

  //todo: I need to really rethink this entire approach
  override fun processKeyEvent(keyEvent: KeyEvent) {
    if (keyEvent.id == KEY_PRESSED) {
      if ((keyEvent.isMetaDown || keyEvent.isControlDown)) {
        if (aceFinder.toggleTargetMode())
          background = Color.RED
        else
          background = Color.WHITE
      }

      if (keyEvent.keyCode == VK_BACK_SPACE) {
        if (text.isNotEmpty())
          text = text.substring(0, text.length - 1)

        return
      }

      defaultKeyCommand.execute(keyEvent, text)
      if (keyMap.contains(keyEvent.keyCode)) {
        keyEvent.consume()
        keyMap[keyEvent.keyCode]?.execute(keyEvent)
        return
      }
    }
    super.processKeyEvent(keyEvent)
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
    aceFinder.tagMap.clear()
  }
}