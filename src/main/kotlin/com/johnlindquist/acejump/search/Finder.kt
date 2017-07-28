package com.johnlindquist.acejump.search

import com.intellij.find.FindModel
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.johnlindquist.acejump.control.Handler
import com.johnlindquist.acejump.control.Trigger
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import com.johnlindquist.acejump.view.Model.markup
import com.johnlindquist.acejump.view.Model.targetModeHighlightStyle
import com.johnlindquist.acejump.view.Model.textHighlightStyle
import kotlin.text.RegexOption.MULTILINE

/**
 * Singleton that searches for text in editor and highlights matching results.
 *
 * @see Tagger
 */

object Finder {
  private var results = emptySet<Int>()
  private var resultsInView = setOf<RangeHighlighter>()
  private var model = FindModel()
  private var TEXT_HIGHLIGHT_LAYER = HighlighterLayer.LAST + 1
  private var TARGET_HIGHLIGHT_LAYER = HighlighterLayer.LAST + 2

  val isShiftSelectEnabled
    get() = model.stringToFind.last().isUpperCase()

  var skim = false

  var query: String = ""
    set(value) {
      field = value.toLowerCase()

      if (value.isEmpty()) return
      if (value.length == 1) skim() else searchForQueryOrDropLastCharacter()
    }

  private fun skim() {
    skim = true
    search(FindModel().apply { stringToFind = query })
    Trigger(350L) { search() }
  }

  fun search(string: String = query) =
    search(FindModel().apply { stringToFind = Regex.escape(string) })

  fun search(pattern: Pattern) =
    search(FindModel().apply {
      stringToFind = pattern.string
      isRegularExpressions = true
      Tagger.reset()
    })

  fun search(findModel: FindModel) {
    model = findModel
    if (!Tagger.hasTagSuffix(query)) highlight()

    results.tag()
  }

  private fun highlight() {
    results = editorText.findMatchingSites().toHashSet()
    resultsInView = results.mapNotNull {
      if (Jumper.targetModeEnabled) createTargetHighlighter(it)
      val textHighlight = createTextHighlighter(it)
      if (textHighlight.startOffset in editor.getView()) textHighlight else null
    }.toSet()
  }

  private fun createTargetHighlighter(index: Int) {
    if (!editorText[index].isLetterOrDigit()) return
    val (wordStart, wordEnd) = editorText.wordBounds(index)
    markup.addRangeHighlighter(wordStart, wordEnd,
      TARGET_HIGHLIGHT_LAYER, targetModeHighlightStyle, EXACT_RANGE)
  }

  private fun createTextHighlighter(it: Int) =
    markup.addRangeHighlighter(it,
      if (model.isRegularExpressions) it + 1 else it + query.length,
      TEXT_HIGHLIGHT_LAYER, textHighlightStyle, EXACT_RANGE)

  private fun Set<Int>.tag() = runLater {
    Tagger.markOrJump(model, resultsInView.map { it.startOffset }.toSet(), this)
    resultsInView.forEach {
      if (!Tagger.hasTagsAtIndex(it.startOffset)) markup.removeHighlighter(it)
    }
    skim = false
    Handler.paintTagMarkers()
  }

  /**
   * Returns a list of indices where the query begins, within the given range.
   * These are full indices, ie. are not offset to the beginning of the range.
   */

  private fun String.findMatchingSites(key: String = query.toLowerCase(),
                                       cache: Set<Int> = results) =
    // If the cache is populated, filter it instead of redoing extra work
    if (cache.isEmpty()) findAll(model.stringToFind)
    else cache.asSequence().filter { regionMatches(it, key, 0, key.length) }

  private fun CharSequence.findAll(key: String, startingFrom: Int = 0) =
    Regex(key, MULTILINE).findAll(this, startingFrom).mapNotNull {
      // Do not accept any sites which fall between folded regions in the gutter
      if (editor.foldingModel.isOffsetCollapsed(it.range.first)) null
      else it.range.first
    }

  private fun searchForQueryOrDropLastCharacter() =
    if (query.isValidQuery()) search() else query = query.dropLast(1)

  private fun String.isValidQuery() =
    results.any { editorText.regionMatches(it, this, 0, length) } ||
      Tagger.hasTagSuffix(query)

  fun discard() {
    markup.removeAllHighlighters()
    query = ""
    model = FindModel()
    results = emptySet()
    resultsInView = emptySet()
  }
}

