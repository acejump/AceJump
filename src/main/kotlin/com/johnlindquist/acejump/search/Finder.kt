package com.johnlindquist.acejump.search

import com.intellij.find.FindModel
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.johnlindquist.acejump.control.Handler
import com.johnlindquist.acejump.control.Trigger
import com.johnlindquist.acejump.label.Pattern
import com.johnlindquist.acejump.label.Tagger
import com.johnlindquist.acejump.view.Marker
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import com.johnlindquist.acejump.view.Model.markup
import com.johnlindquist.acejump.view.Model.viewBounds
import org.jetbrains.concurrency.runAsync
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.text.RegexOption.MULTILINE

/**
 * Singleton that searches for text in editor and highlights matching results.
 *
 * @see Tagger
 */

object Finder : Resettable {
  private var results: SortedSet<Int> = sortedSetOf<Int>()
  private var textHighlights = listOf<RangeHighlighter>()
  private var viewHighlights = listOf<RangeHighlighter>()
  private var model = FindModel()
  private var TEXT_HIGHLIGHT_LAYER = HighlighterLayer.LAST + 1
  private val logger = Logger.getInstance(Finder::class.java)

  var isShiftSelectEnabled = false

  var skim = false

  var query: String = ""
    set(value) {
      logger.info("Received query: \"$value\"")
      field = value.toLowerCase()
      isShiftSelectEnabled = value.isNotEmpty() && value.last().isUpperCase()

      when {
        value.isEmpty() -> return
        Tagger.regex -> search()
        value.length == 1 -> skim()
        value.isValidQuery() -> skim()
        else -> field = field.dropLast(1)
      }
    }

  /**
   * A user has two possible goals when triggering an AceJump search.
   *
   * 1. To locate the position of a known string in the document (ie. Find)
   * 2. To move the cursor to a fixed location (ie. eyeball staring at location)
   *
   * Since we cannot know why the user initiated any query a priori, here we
   * attempt to satisfy both goals. First, we highlight all matches on (or off)
   * the screen. This operation has very low latency. As soon as the user types
   * a single character, we highlight all matches immediately. If we should
   * receive no further characters after a short delay (indicating a pause in
   * typing cadence), then apply tags.
   *
   * Typically when a user searches for a known string, they will type several
   * characters in rapid succession. We can avoid unnecessary work by only
   * applying tags once we have received a "chunk" of search text.
   */

  private fun skim() {
    logger.info("Skimming document for matches of: $query")
    if(2e4 < editorText.length) skim = true
    search(FindModel().apply { stringToFind = query })
    Trigger(400L) { if (skim) runLater { skim = false; search() } }
  }

  fun search(string: String = query) {
    logger.info("Searching for locations matching: $string")
    search(model.apply { stringToFind = string })
  }

  fun search(pattern: Pattern) {
    logger.info("Searching for regular expression: ${pattern.name}")
    reset()
    search(FindModel().apply {
      stringToFind = pattern.string
      isRegularExpressions = true
      Tagger.reset()
    })
  }

  fun search(findModel: FindModel) {
    model = findModel

    val timeElapsed = measureTimeMillis {
      results = editorText.findMatchingSites().toSortedSet()
    }

    logger.info("Discovered ${results.size} matches in $timeElapsed ms")

    if (!Tagger.hasTagSuffixInView(query)) highlightResults()
    if (!skim) runAsync { tag(results) }
  }

  private fun highlightResults() {
    if (results.size < 26) skim = false
    if (Tagger.regex) return
    paintTextHighlights()
  }

  fun paintTextHighlights() {
    val tempHighlights = results.map { createTextHighlighter(it) }
    textHighlights.forEach { markup.removeHighlighter(it) }
    textHighlights = tempHighlights
    viewHighlights = textHighlights.filter { it.startOffset in viewBounds }
  }

  private fun createTextHighlighter(it: Int) =
    markup.addRangeHighlighter(it,
      if (model.isRegularExpressions) it + 1 else it + query.length,
      TEXT_HIGHLIGHT_LAYER, null, EXACT_RANGE)
      .apply { customRenderer = Marker(query, null, this.startOffset) }

  private fun tag(results: Set<Int>) {
    synchronized(this) { Tagger.markOrJump(model, results) }
    viewHighlights = viewHighlights.narrowBy { Tagger canDiscard startOffset }
      .also { newHighlights ->
        val numDiscarded = viewHighlights.size - newHighlights.size
        if (numDiscarded != 0) logger.info("Discarded $numDiscarded highlights")
      }

    Handler.paintTagMarkers()
  }

  fun List<RangeHighlighter>.narrowBy(cond: RangeHighlighter.() -> Boolean) =
    filter {
      if (cond(it)) {
        runLater { markup.removeHighlighter(it) }
        false
      } else true
    }

  /**
   * Returns a list of indices where the query begins, within the given range.
   * These are full indices, ie. are not offset to the beginning of the range.
   */

  private fun String.findMatchingSites(key: String = query.toLowerCase(),
                                       cache: Set<Int> = results) =
    // If the cache is populated, filter it instead of redoing extra work
    if (cache.isEmpty()) findAll(model.sanitizedString())
    else cache.asSequence().filter { regionMatches(it, key, 0, key.length) }

  private fun Set<Int>.isCacheValidForRange() =
    viewBounds.let { view ->
      first() < view.first && last() > view.last
    }

  private fun CharSequence.findAll(key: String, startingFrom: Int = 0) =
    generateSequence({ Regex(key, MULTILINE).find(this, startingFrom) },
      Finder::filterNextResult).map { it.range.first }

  private tailrec fun filterNextResult(result: MatchResult): MatchResult? {
    val next = result.next()
    return if (next == null) null
    else if (editor.isVisible(next.range.first)) next
    else filterNextResult(next)
  }

  private fun String.isValidQuery() =
    results.any { editorText.regionMatches(it, this, 0, length) } ||
      Tagger.hasTagSuffixInView(query)

  override fun reset() {
    runLater { markup.removeAllHighlighters() }
    query = ""
    model = FindModel()
    skim = false
    results = sortedSetOf()
    textHighlights = listOf()
    viewHighlights = listOf()
  }
}