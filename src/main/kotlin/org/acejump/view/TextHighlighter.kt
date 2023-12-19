package org.acejump.view

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.hint.*
import com.intellij.codeInsight.hint.HintManagerImpl.HIDE_BY_ESCAPE
import com.intellij.codeInsight.hint.HintManagerImpl.HIDE_BY_TEXT_CHANGE
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
import com.intellij.ui.*
import com.intellij.ui.util.preferredHeight
import com.intellij.util.DocumentUtil
import com.intellij.util.ui.*
import it.unimi.dsi.fastutil.ints.IntList
import org.acejump.*
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.config.AceConfig
import org.acejump.input.JumpMode
import org.acejump.search.SearchQuery
import java.awt.*
import javax.swing.*
import kotlin.math.max

/**
 * Renders highlights for search occurrences.
 */
internal class TextHighlighter {
  private companion object { private const val LAYER = HighlighterLayer.LAST + 1 }
  private var previousHighlights = mutableMapOf<Editor, Array<RangeHighlighter>>()
  private var previousHint: LightweightHint? = null

  /**
   * Label for the search notification.
   */
  private class NotificationLabel(text: String?): JLabel(text) {
    init {
      background = HintUtil.getInformationColor()
      foreground = JBColor.foreground()
      this.isOpaque = true
    }
  }

  /**
   * Removes all current highlights and re-creates them from scratch.
   * Must be called whenever any of the method parameters change.
   */
  fun render(results: Map<Editor, IntList>, query: SearchQuery, jumpMode: JumpMode) {

    val renderer = when {
      query is SearchQuery.RegularExpression -> RegexRenderer
      jumpMode === JumpMode.TARGET -> SearchedWordWithOutlineRenderer
      else -> SearchedWordRenderer
    }

    for ((editor, offsets) in results) {
      val highlights = previousHighlights[editor]

      val markup = editor.markupModel
      val document = editor.document
      val chars = editor.immutableText

      val modifications = (highlights?.size ?: 0) + offsets.size
      val enableBulkEditing = modifications > 1000

      DocumentUtil.executeInBulk(document, enableBulkEditing) {
        highlights?.forEach(markup::removeHighlighter)
        previousHighlights[editor] = Array(offsets.size) { index ->
          val start = offsets.getInt(index)
          val end = start + query.getHighlightLength(chars, start)

          markup.addRangeHighlighter(start, end, LAYER, null, EXACT_RANGE)
            .apply { customRenderer = renderer }
        }
      }
    }

    if (AceConfig.showSearchNotification)
      showSearchNotification(results, query, jumpMode)

    for (editor in previousHighlights.keys.toList()) {
      if (!results.containsKey(editor))
        previousHighlights.remove(editor)
          ?.forEach(editor.markupModel::removeHighlighter)
    }
  }

  /**
   * Show a notification with the current search text.
   */
  private fun showSearchNotification(results: Map<Editor, IntList>,
                                     query: SearchQuery, jumpMode: JumpMode) {
    // clear previous hint
    previousHint?.hide()

    // add notification hint to first editor
    val editor = results.keys.first()
    val component: JComponent = editor.component

    val label1 = NotificationLabel(" $jumpMode Mode:")
      .apply { font = UIUtil.getLabelFont().deriveFont(Font.BOLD) }

    val queryText = " " +
      (if (query is SearchQuery.RegularExpression) query.toRegex().toString()
      else query.rawText[0] + query.rawText.drop(1).lowercase()) + "   "
    val label2 = NotificationLabel(queryText)

    val label3 = NotificationLabel(
      "Found ${results.values.flatMap { it.asIterable() }.size}" +
        " results in ${results.keys.size}" +
        " editor" + if (1 != results.keys.size) "s" else ". "
    )

    val panel = JPanel(BorderLayout()).apply {
      add(label1, BorderLayout.WEST)
      add(label2, BorderLayout.CENTER)
      add(label3, BorderLayout.EAST)
      border = BorderFactory.createLineBorder(
        if (jumpMode == JumpMode.DISABLED) JBColor.BLACK else jumpMode.caretColor
      )

      preferredHeight = label1.preferredSize.height + 10
    }

    val hint = LightweightHint(panel)

    val x = SwingUtilities.convertPoint(component, 0, 0, component).x
    val y: Int = -hint.component.preferredSize.height
    val p = SwingUtilities.convertPoint(
      component, x, y,
      component.rootPane.layeredPane
    )

    HintManagerImpl.getInstanceImpl().showEditorHint(
      hint,
      editor,
      p,
      HIDE_BY_ESCAPE or HIDE_BY_TEXT_CHANGE,
      0,
      false,
      HintHint(editor, p).setAwtTooltip(false)
    )
    previousHint = hint
  }

  fun reset() {
    previousHighlights.forEach { (editor, highlighters) ->
      highlighters.forEach(editor.markupModel::removeHighlighter)
    }
    previousHighlights.clear()
    previousHint?.hide()
  }

  /**
   * Renders a filled highlight in the background of a searched text occurrence.
   */
  private object SearchedWordRenderer: CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) =
      drawFilled(g, editor, highlighter.startOffset, highlighter.endOffset)

    private fun drawFilled(g: Graphics, editor: Editor, startOffset: Int, endOffset: Int) {
      val start = EditorOffsetCache.Uncached.offsetToXY(editor, startOffset)
      val end = EditorOffsetCache.Uncached.offsetToXY(editor, endOffset)

      g.color = AceConfig.textHighlightColor
      g.fillRect(start.x, start.y, end.x - start.x, editor.lineHeight)

      g.color = AceConfig.tagBackgroundColor
      g.drawRect(start.x, start.y, end.x - start.x, editor.lineHeight)
    }
  }

  /**
   * Renders a filled highlight in the background of a searched
   * text occurrence, as well as an outline indicating the range
   * of characters that will be selected by [JumpMode.TARGET].
   */
  private object SearchedWordWithOutlineRenderer: CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      SearchedWordRenderer.paint(editor, highlighter, g)

      val chars = editor.immutableText
      val startOffset = highlighter.startOffset

      if (chars.getOrNull(startOffset)?.isWordPart == true)
        drawOutline(g, editor, chars.wordStart(startOffset), chars.wordEnd(startOffset) + 1)
    }

    private fun drawOutline(g: Graphics, editor: Editor, startOffset: Int, endOffset: Int) {
      val start = EditorOffsetCache.Uncached.offsetToXY(editor, startOffset)
      val end = EditorOffsetCache.Uncached.offsetToXY(editor, endOffset)

      g.color = AceConfig.targetModeColor
      g.drawRect(max(0, start.x - JBUI.scale(1)), start.y,
        end.x - start.x + JBUI.scale(2), editor.lineHeight)
    }
  }

  /**
   * Renders a filled highlight in the background of the first highlighted
   * position. Used for regex search queries.
   */
  private object RegexRenderer: CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) =
      drawSingle(g, editor, highlighter.startOffset)

    private fun drawSingle(g: Graphics, editor: Editor, offset: Int) {
      val pos = EditorOffsetCache.Uncached.offsetToXY(editor, offset)
      val char = editor.immutableText.getOrNull(offset)
        ?.takeUnless { it == '\n' || it == '\t' } ?: ' '
      val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
      val lastCharWidth = editor.component.getFontMetrics(font).charWidth(char)

      g.color = AceConfig.textHighlightColor
      g.fillRect(pos.x, pos.y, lastCharWidth, editor.lineHeight)

      g.color = AceConfig.tagBackgroundColor
      g.drawRect(pos.x, pos.y, lastCharWidth, editor.lineHeight)
    }
  }
}
