package org.acejump.view

import com.github.promeg.pinyinhelper.Pinyin
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.ProjectManager
import org.acejump.config.AceConfig
import org.acejump.search.defaultEditor
import org.acejump.view.Boundary.FULL_FILE_BOUNDARY
import java.awt.Color.BLACK
import java.awt.Color.YELLOW
import java.awt.Font
import java.awt.Font.BOLD

/**
 * Data holder for all settings and IDE components needed by AceJump.
 *
 * TODO: Integrate this class with [org.acejump.config.AceSettings]
 */

object Model {
  var editor: Editor = defaultEditor()

  val markup
    get() = editor.markupModel
  val project
    get() = editor.project ?: ProjectManager.getInstance().defaultProject
  val caretOffset
    get() = editor.caretModel.offset
  val editorText: String
    get() = editor.document.text
      .let { if (AceConfig.supportPinyin) it.mapToPinyin() else it }

  private fun String.mapToPinyin() =
    map { Pinyin.toPinyin(it).first() }.joinToString("")

  var naturalBlock = false
  var naturalBlink = true
  var naturalCaretColor = BLACK
  var naturalHighlight = YELLOW

  val scheme
    get() = editor.colorsScheme
  val font
    get() = Font(scheme.editorFontName, BOLD, scheme.editorFontSize)
  val fontWidth
    get() = editor.component.getFontMetrics(font).stringWidth("w")
  val fontHeight
    get() = editor.colorsScheme.editorFontSize
  val lineHeight
    get() = editor.lineHeight
  val rectHeight
    get() = fontHeight + 3
  val rectVOffset
    get() = lineHeight - (editor as EditorImpl).descent - fontHeight
  val arcD = rectHeight - 6
  var viewBounds = 0..0
  const val LONG_DOCUMENT_LENGTH = 100000
  val LONG_DOCUMENT
    get() = LONG_DOCUMENT_LENGTH < editorText.length
  const val MAX_TAG_RESULTS = 300

  val defaultBoundary = FULL_FILE_BOUNDARY
  var boundaries: Boundary = defaultBoundary

  fun Editor.setupCaret() {
    settings.isBlockCursor = true
    settings.isBlinkCaret = false
    colorsScheme.setColor(CARET_COLOR, AceConfig.jumpModeColor)
  }
}