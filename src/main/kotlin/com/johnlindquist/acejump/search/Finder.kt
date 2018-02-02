package com.johnlindquist.acejump.search

import com.intellij.find.FindModel
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.Editor
import com.johnlindquist.acejump.control.Handler
import com.johnlindquist.acejump.control.Trigger
import com.johnlindquist.acejump.label.Pattern
import com.johnlindquist.acejump.label.Tagger
import com.johnlindquist.acejump.view.Marker
import com.johnlindquist.acejump.view.Model.LONG_DOCUMENT
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import com.johnlindquist.acejump.view.Model.markup
import com.johnlindquist.acejump.view.Model.viewBounds
import org.jetbrains.concurrency.runAsync
import java.lang.Math.max
import java.lang.Math.min
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.text.RegexOption.MULTILINE

interface FinderBoundry {
	fun getStart() : Int;
	fun getEnd() : Int;
}

class FullFileBoundry : FinderBoundry {
  override fun getStart() : Int = max(0, viewBounds.first - 20000)
  override fun getEnd() : Int = min(viewBounds.last + 20000, editorText.length)
}

class ScreenBoundry : FinderBoundry {
  override fun getStart() : Int = max(0, viewBounds.first)
  override fun getEnd() : Int = min(viewBounds.last, editorText.length)
}

class BeforeCurserBoundry : FinderBoundry {
  override fun getStart() : Int = max(0, viewBounds.first)
  override fun getEnd() : Int {

	  var offset = editor.getCaretModel().getOffset()

	  return min( offset - 1, min(viewBounds.last, editorText.length) )
  }
}

class AfterCurserBoundry : FinderBoundry {
  override fun getStart() : Int {
	  var offset = editor.getCaretModel().getOffset()

	  return max(offset + 1, max(0, viewBounds.first))
  }
  override fun getEnd() : Int = min(viewBounds.last, editorText.length)
}


/**
 * Singleton that searches for text in editor and highlights matching results.
 *
 * @see Tagger
 */

object Finder : Resettable {
  private var results: SortedSet<Int> = sortedSetOf<Int>()
  @Volatile
  private var textHighlights = listOf<RangeHighlighter>()
  @Volatile
  private var viewHighlights = listOf<RangeHighlighter>()
  private var HIGHLIGHT_LAYER = HighlighterLayer.LAST + 1
  private val logger = Logger.getInstance(Finder::class.java)
  var isShiftSelectEnabled = false
  
  private var boundries : FinderBoundry = FullFileBoundry()

  var skim = false
    private set

  @Volatile
  var query: String = ""
    set(value) {
      field = value.toLowerCase()
      if (query.isNotEmpty()) logger.info("Received query: \"$value\"")
      isShiftSelectEnabled = value.isNotEmpty() && value.last().isUpperCase()

      when {
        value.isEmpty() -> return
        Tagger.regex -> search()
        value.length == 1 -> skimThenSearch()
        value.isValidQuery() -> skimThenSearch()
        else -> {
          logger.info("Invalid query, dropping: ${field.last()}")
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
      Trigger(400L) { runLater { skim = false; search() } }
    } else search()

  fun search(pattern: Pattern) {
    logger.info("Searching for regular expression: ${pattern.name}")
    reset()
    search(FindModel().apply {
      stringToFind = pattern.string
      isRegularExpressions = true
      Tagger.reset()
    })
  }

  fun backwardsMode() {
	  boundries = BeforeCurserBoundry()
  }

  fun forwardMode() {
	  boundries = AfterCurserBoundry()
  }

  fun fullFileMode() {
	  boundries = FullFileBoundry()
  }

  fun search(model: FindModel = FindModel().apply { stringToFind = query }) {
    measureTimeMillis {
      results = editorText.findMatchingSites(model).toSortedSet()
    }.let { ms -> logger.info("Found ${results.size} matches in $ms ms") }

    if (!results.isEmpty()) paintTextHighlights(model)
    if (!skim) runAsync { tag(model, results) }
  }

  /**
   * Paints text highlights to the editor using the MarkupModel API.
   *
   * @see com.intellij.openapi.editor.markup.MarkupModel
   */

  fun paintTextHighlights(model: FindModel = FindModel().apply { stringToFind = query }) {
    val newHighlights = results.map { index ->
      val s = if (index == editorText.length) index - 1 else index
      val e = if (model.isRegularExpressions) s + 1 else s + query.length
      createTextHighlight(s, e)
    }

    textHighlights.forEach { markup.removeHighlighter(it) }
    textHighlights = newHighlights
    viewHighlights = textHighlights.filter { it.startOffset in viewBounds }
  }

  private fun createTextHighlight(s: Int, e: Int) =
    markup.addRangeHighlighter(s, e, HIGHLIGHT_LAYER, null, EXACT_RANGE)
      .apply { customRenderer = Marker(query, null, this.startOffset) }

  private fun tag(model: FindModel, results: Set<Int>) {
    synchronized(this) { Tagger.markOrJump(model, results) }
    viewHighlights = viewHighlights.narrowBy { Tagger canDiscard startOffset }
      .also { newHighlights ->
        val numDiscarded = viewHighlights.size - newHighlights.size
        if (numDiscarded != 0) logger.info("Discarded $numDiscarded highlights")
      }

    if (model.stringToFind == query) Handler.repaintTagMarkers()
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

  private fun String.findMatchingSites(model: FindModel,
                                       key: String = model.stringToFind.toLowerCase(),
                                       cache: Set<Int> = results) =
    // If the cache is populated, filter it instead of redoing extra work
    if (cache.isEmpty()) findAll(model.sanitizedString())
    else cache.asSequence().filter { regionMatches(it, key, 0, key.length) }

  private fun Set<Int>.isCacheValidForRange() =
    viewBounds.let { view ->
      first() < view.first && last() > view.last
    }

  private fun CharSequence.findAll(key: String, start: Int = getStartBound()) =
    generateSequence({ Regex(key, MULTILINE).find(this, start) },
      Finder::filterNextResult).map { it.range.first }

  private fun getStartBound() = boundries.getStart()
  private fun getEndBound() = boundries.getEnd()

  private tailrec fun filterNextResult(result: MatchResult): MatchResult? {
    val next = result.next()
    return if (next == null) null
    else if (getEndBound() <= next.range.first) null
    else if (editor.isVisible(next.range.first)) next
    else filterNextResult(next)
  }

  private fun String.isValidQuery() =
    results.any { editorText.regionMatches(it, this, 0, length) } ||
      Tagger.hasTagSuffixInView(query)

  override fun reset() {
    runLater { markup.removeAllHighlighters() }
    query = ""
    skim = false
    results = sortedSetOf()
    textHighlights = listOf()
    viewHighlights = listOf()
  }
}
