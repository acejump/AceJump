package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.ui.popup.ComponentPopupBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.popup.AbstractPopup
import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.guessBestLocation
import com.johnlindquist.acejump.keycommands.*
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Point
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import java.util.*
import javax.swing.JRootPane
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.event.ChangeListener
import javax.swing.text.BadLocationException

class SearchBox(val aceFinder: AceFinder, val editor: EditorImpl) : JTextField() {
  val keyReleasedMap = HashMap<Int, AceKeyCommand>()
  val keyPressedMap = HashMap<Int, AceKeyCommand>()

  var aceCanvas: AceCanvas = AceCanvas(editor)
  var popupContainer: AbstractPopup? = null
  var defaultKeyCommand: AceKeyCommand = DefaultKeyCommand(this, aceFinder)
  var isSearchEnabled = true
    get() {
      return field
    }

  init {
    val releasedHome = ShowBeginningOfLines(this, aceFinder)
    val releasedEnd = ShowEndOfLines(this, aceFinder)
    keyReleasedMap[VK_HOME] = releasedHome
    keyReleasedMap[VK_LEFT] = releasedHome
    keyReleasedMap[VK_RIGHT] = releasedEnd
    keyReleasedMap[VK_END] = releasedEnd
    keyReleasedMap[VK_UP] = ShowFirstCharOfLines(this, aceFinder)
    keyPressedMap[VK_BACK_SPACE] = ClearResults(this, aceCanvas)
    keyPressedMap[VK_SPACE] = ShowWhiteSpace(this, aceFinder)
    keyPressedMap[VK_SEMICOLON] = ChangeToTargetMode(this, aceFinder)

    val scheme = EditorColorsManager.getInstance().globalScheme
    val font = Font(scheme.editorFontName, Font.BOLD, scheme.editorFontSize)
    val popupBuilder: ComponentPopupBuilder? = JBPopupFactory.getInstance()?.createComponentPopupBuilder(this, this)
    popupBuilder?.setCancelKeyEnabled(true)
    val popup = (popupBuilder?.createPopup() as AbstractPopup?)
    popup?.show(guessBestLocation(editor))
    popup?.setRequestFocus(true)

    val width = getFontMetrics(font).stringWidth("w")
    val dimension: Dimension = Dimension(width * 2, (editor.lineHeight))
    if (SystemInfo.isMac) {
      dimension.setSize(dimension.width * 2, dimension.height * 2)
    }

    popup?.size = dimension
    popupContainer = popup
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

    aceFinder.addResultsReadyListener(ChangeListener {
      aceCanvas.jumpInfos = aceFinder.setupJumpLocations()
      aceCanvas.repaint()
    })
  }

  override fun requestFocus() {
    transferHandler = null
    super.requestFocus()
  }

  override fun paintBorder(p0: Graphics?) {
  }

  //todo: I need to really rethink this entire approach
  override fun processKeyEvent(keyEvent: KeyEvent) {
    if (text.length == 0) {
      //todo: rethink the "isSearchEnabled" state approach. Works great now, could be cleaner
      isSearchEnabled = true
    }

    var aceKeyCommand: AceKeyCommand? = null
    if (keyEvent.id == KeyEvent.KEY_RELEASED) {
      aceKeyCommand = keyReleasedMap[keyEvent.keyCode]
    }

    if (keyEvent.id == KeyEvent.KEY_PRESSED) {
      aceKeyCommand = keyPressedMap[keyEvent.keyCode]
      //prevent "alt" from triggering menu items
      keyEvent.consume()
    }

    if (aceKeyCommand != null) {
      aceKeyCommand.execute(keyEvent)
      return
    }

    super.processKeyEvent(keyEvent)

    if (keyEvent.id != KeyEvent.KEY_TYPED) return

    if (keyEvent.isConsumed) {
      defaultKeyCommand.execute(keyEvent)
    }

    if (text?.length == 2) {
      try {
        text = getText(0, 1)
      } catch (e1: BadLocationException) {
        e1.printStackTrace()
      }
    }
  }

  fun forceSpaceChar() {
    text = " "
    disableSearch()
  }

  fun disableSearch() {
    isSearchEnabled = false
  }

  fun addAceCanvas() {
    editor.contentComponent.add(aceCanvas)
    val viewport = editor.scrollPane.viewport
    aceCanvas.setBounds(0, 0, viewport.width + 1000, viewport.height + 1000)
    val rootPane: JRootPane = editor.component.rootPane
    val locationOnScreen: Point = SwingUtilities.convertPoint(aceCanvas, aceCanvas.location, rootPane)
    aceCanvas.setLocation(-locationOnScreen.x, -locationOnScreen.y)
  }

  fun exit() {
    val contentComponent = editor.contentComponent
    contentComponent.remove(aceCanvas)
    contentComponent.repaint()
    aceFinder.textAndOffsetHash.clear()
  }
}