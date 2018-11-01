package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.ProjectManager
import org.acejump.config.AceConfig
import org.acejump.search.defaultEditor
import org.acejump.view.Boundary.FULL_FILE_BOUNDARY
import java.awt.Color.*
import java.awt.Font
import java.awt.Font.BOLD

/**
 * Data holder for all settings and IDE components needed by AceJump.
 *
 * TODO: Integrate this class with AceSettings.
 */

object Model {
  var editor = defaultEditor()
    get() = if (field.isDisposed) defaultEditor() else field
    set(value) {
      editorText = value.document.text
      if (value == field) return
      field = value

      editor.run {
        settings.run {
          naturalBlock = isBlockCursor
          naturalBlink = isBlinkCaret
        }

        colorsScheme.run {
          getColor(CARET_COLOR).let { naturalColor = it }
          getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES)
            ?.backgroundColor.let { naturalHighlight = it }
        }
      }
    }

  val markup
    get() = editor.markupModel
  val project
    get() = editor.project ?: ProjectManager.getInstance().defaultProject
  val caretOffset
    get() = editor.caretModel.offset
  var editorText = editor.document.text

  var naturalBlock = false
  var naturalBlink = true
  var naturalColor = BLACK
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
  const val DEFAULT_BUFFER = 40000
  val LONG_DOCUMENT
    get() = DEFAULT_BUFFER < editorText.length
  const val MAX_TAG_RESULTS = 300

  val defaultBoundary = FULL_FILE_BOUNDARY
  var boundaries: Boundary = defaultBoundary

  fun Editor.setupCaret() {
    settings.isBlockCursor = true
    settings.isBlinkCaret = false
    colorsScheme.setColor(CARET_COLOR, AceConfig.settings.jumpModeColor)
  }
}
