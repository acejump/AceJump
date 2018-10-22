package org.acejump.search

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
import com.intellij.openapi.editor.markup.RangeHighlighter
import org.acejump.control.Handler
import org.acejump.control.Trigger
import org.acejump.label.Pattern
import org.acejump.label.Tagger
import org.acejump.view.Boundary
import org.acejump.view.Boundary.FULL_FILE_BOUNDARY
import org.acejump.view.Marker
import org.acejump.view.Model.LONG_DOCUMENT
import org.acejump.view.Model.boundaries
import org.acejump.view.Model.editorText
import org.acejump.view.Model.markup
import org.acejump.view.Model.viewBounds
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Singleton that searches for text in editor and highlights matching results.
 *
 * @see Tagger
 */

object Finder : Resettable {
  @Volatile
  private var results: SortedSet<Int> = sortedSetOf()
  @Volatile
  private var textHighlights = listOf<RangeHighlighter>()
  @Volatile
  private var viewHighlights = listOf<RangeHighlighter>()
  private var HIGHLIGHT_LAYER = HighlighterLayer.LAST + 1
  private val logger = Logger.getInstance(Finder::class.java)
  var isShiftSelectEnabled = false

  var skim = false
    private set

  @Volatile
  var query: String = ""
    set(value) {
      field = value
      if (query.isNotEmpty()) logger.info("Received query: \"$value\"")
      isShiftSelectEnabled = value.lastOrNull()?.isUpperCase() == true

      when {
        value.isEmpty() -> return
        Tagger.regex -> search()
        value.length == 1 -> skimThenSearch()
        value.isValidQuery() -> skimThenSearch()
        else -> {
          logger.info("Invalid query \"$field\", dropping: ${field.last()}")
          field = field.dropLast(1)
        }
      }
    }

  /**
   * A user has two possible goals when launching an AceJump search.
   *
   * 1. To locate the position of a known string in the document (a.k.a. Find)
   * 2. To reposition the caret to a known location (i.e. staring at location)
   *
   * Since we cannot know why the user initiated any query a priori, here we
   * attempt to satisfy both goals. First, we highlight all matches on (or off)
   * the screen. This operation has very low latency. As soon as the user types
   * a single character, we highlight all matches immediately. If we should
   * receive no further characters after a short delay (indicating a pause in
   * typing cadence), then we apply tags.
   *
   * Typically when a user searches for a known string, they will type several
   * characters in rapid succession. We can avoid unnecessary work by only
   * applying tags once we have received a "chunk" of search text.
   */

  private fun skimThenSearch() =
    if (results.size == 0 && LONG_DOCUMENT) {
      skim = true
      logger.info("Skimming document for matches of: $query")
      search()
      Trigger(400L) { skim = false; search() }
    } else search()

  fun search(pattern: Pattern, bounds: Boundary = FULL_FILE_BOUNDARY) {
    logger.info("Searching for regular expression: ${pattern.name} in $bounds")
    boundaries = bounds
    // TODO: Fix this broken reset
    reset()
    Tagger.reset()
    search(AceFindModel(pattern.string, true))
  }

  fun search(model: AceFindModel = AceFindModel(query)) {
    measureTimeMillis {
      results = Scanner.findMatchingSites(editorText, model, results)
    }.let { logger.info("Found ${results.size} matching sites in $it ms") }

    paintTextHighlights(model)
    if (!skim) runLater { tag(model, results) }
  }

  /**
   * Paints text highlights to the editor using the MarkupModel API.
   *
   * @see com.intellij.openapi.editor.markup.MarkupModel
   */

  fun paintTextHighlights(model: AceFindModel = AceFindModel(query)) =
    runLater {
      val newHighlights = results.map { index ->
        val s = if (index == editorText.length) index - 1 else index
        val e = if (model.isRegularExpressions) s + 1 else s + query.length
        createTextHighlight(s, e)
      }

      if (!results.isEmpty()) {
        textHighlights.forEach { markup.removeHighlighter(it) }
        textHighlights = newHighlights
      }

      viewHighlights = textHighlights.filter { it.startOffset in viewBounds }
    }

  private fun createTextHighlight(start: Int, end: Int) =
    markup.addRangeHighlighter(start, end, HIGHLIGHT_LAYER, null, EXACT_RANGE)
      .apply { customRenderer = Marker(query, null, this.startOffset) }

  private fun tag(model: AceFindModel, results: SortedSet<Int>) {
    synchronized(this) { Tagger.markOrJump(model, results) }
    viewHighlights = viewHighlights.discardIf { Tagger canDiscard startOffset }
      .also { newHighlights ->
        val numDiscarded = viewHighlights.size - newHighlights.size
        if (numDiscarded != 0) logger.info("Discarded $numDiscarded highlights")
      }

    if (model.stringToFind == query || model.isRegularExpressions) Handler.repaintTagMarkers()
  }

  private fun List<RangeHighlighter>.discardIf(cond: RangeHighlighter.() -> Boolean) =
    filter {
      if (cond(it)) {
        runLater { markup.removeHighlighter(it) }
        false
      } else true
    }

  private fun String.isValidQuery() =
    Tagger.hasTagSuffixInView(query) ||
      results.any {
        editorText.regionMatches(
          thisOffset = it,
          other = this,
          otherOffset = 0,
          length = length,
          ignoreCase = true
        )
      }

  override fun reset() {
    runLater { markup.removeAllHighlighters() }
    query = ""
    skim = false
    results = sortedSetOf()
    textHighlights = listOf()
    viewHighlights = listOf()
  }
}