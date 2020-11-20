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
import org.acejump.view.Model.editor
import org.acejump.view.Model.editorText
import org.acejump.view.Model.markup
import org.acejump.view.Model.viewBounds
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

/**
 * Singleton that searches for text in editor and highlights matching results.
 */

object Finder : Resettable {
  @Volatile
  private var results: SortedSet<Int> = sortedSetOf()
  @Volatile
  private var textHighlights = listOf<RangeHighlighter>()
  private var HIGHLIGHT_LAYER = HighlighterLayer.LAST + 1
  private val logger = Logger.getInstance(Finder::class.java)
  private val skimTrigger = Trigger()
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
   * A user has two possible intentions when launching an AceJump search.
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
      skimTrigger(400L) { skim = false; search() }
    } else search()

  fun search(pattern: Pattern, bounds: Boundary = FULL_FILE_BOUNDARY) {
    logger.info("Searching for regular expression: ${pattern.name} in $bounds")
    search(pattern.string, bounds)
  }

  fun search(pattern: String, bounds: Boundary = FULL_FILE_BOUNDARY) {
    boundaries = bounds
    // TODO: Fix this broken reset
    reset()
    Tagger.reset()
    search(AceFindModel(pattern, true))
  }

  fun search(model: AceFindModel = AceFindModel(query)) {
    measureTimeMillis {
      results = Scanner.find(model, results)
    }.let { logger.info("Found ${results.size} matching sites in $it ms") }

    markResults(results, model)
  }

  /**
   * This method is used by IdeaVim integration plugin and must not be inlined.
   *
   * By default, when this function is called externally, [results] are already
   * collected and [AceFindModel] should be empty. Additionally, if the flag
   * [AceFindModel.isRegularExpressions] is true only one symbol is highlighted.
   */

  @ExternalUsage
  fun markResults(results: SortedSet<Int>,
    model: AceFindModel = AceFindModel("", true)
  ) {
    markup(results, model.isRegularExpressions)
    if (!skim) tag(model, results)
  }

  /**
   * Paints text highlights beneath each query result to the editor using the
   * [com.intellij.openapi.editor.markup.MarkupModel].
   */

  fun markup(markers: Set<Int> = results, isRegexQuery: Boolean = false) {
    if (markers.isEmpty()) {
      return
    }

    runLater {
      val highlightLen = if (isRegexQuery) 1 else query.length

      editor.document.isInBulkUpdate = true
      textHighlights.forEach { markup.removeHighlighter(it) }

      textHighlights = markers.map {
        val start = it - if (it == editorText.length) 1 else 0
        val end = start + highlightLen
        createTextHighlight(max(start, 0), min(end, editorText.length - 1))
      }
      editor.document.isInBulkUpdate = false
    }
  }

  private fun createTextHighlight(start: Int, end: Int) =
    markup.addRangeHighlighter(start, end, HIGHLIGHT_LAYER, null, EXACT_RANGE)
      .apply { customRenderer = Marker.Companion }

  private fun tag(model: AceFindModel, results: SortedSet<Int>) {
    synchronized(this) { Tagger.markOrJump(model, results) }
    val (ivb, ovb) = textHighlights.partition { it.startOffset in viewBounds }

    ivb.cull()
    runLater { ovb.cull() }

    if (model.stringToFind == query || model.isRegularExpressions)
      Handler.repaintTagMarkers()
  }

  /**
   * Erases highlights which are no longer compatible with the current query.
   */

  private fun List<RangeHighlighter>.cull() =
    eraseIf { Tagger canDiscard startOffset }
      .also { newHighlights ->
        val numDiscarded = size - newHighlights.size
        if (numDiscarded != 0) logger.info("Discarded $numDiscarded highlights")
      }

  fun List<RangeHighlighter>.eraseIf(cond: RangeHighlighter.() -> Boolean): List<RangeHighlighter> {
    val (erased, kept) = partition(cond)

    if (erased.isNotEmpty()) {
      runLater {
        editor.document.isInBulkUpdate = true
        erased.forEach { markup.removeHighlighter(it) }
        editor.document.isInBulkUpdate = false
      }
    }
    return kept
  }

  fun visibleResults() = results.filter { it in viewBounds }

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
    runLater {
      editor.document.isInBulkUpdate = true
      markup.removeAllHighlighters()
      editor.document.isInBulkUpdate = false
    }
    query = ""
    skim = false
    results = sortedSetOf()
    textHighlights = listOf()
  }
}