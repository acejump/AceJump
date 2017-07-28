package com.johnlindquist.acejump.search

import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.find.FindModel
import com.intellij.openapi.editor.colors.EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
import com.intellij.openapi.editor.markup.*
import com.johnlindquist.acejump.control.Handler
import com.johnlindquist.acejump.control.Trigger
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import com.johnlindquist.acejump.view.Model.markup
import java.awt.Color.GREEN
import java.awt.Font
import kotlin.text.RegexOption.MULTILINE

object Finder {
  private var results = emptySet<Int>()
  private var resultsInView = setOf<RangeHighlighter>()
  private var highlightManager: HighlightManager? = null
  private var model = FindModel()

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
    init()
    skim = true
    search(FindModel().apply { stringToFind = query })
    Trigger(350L) { search() }
  }

  fun search(string: String = query) =
    search(FindModel().apply { stringToFind = Regex.escape(string) })

  fun search(pattern: Pattern) =
    Finder.search(FindModel().apply {
      stringToFind = pattern.string
      isRegularExpressions = true
      Tagger.reset()
    })

  fun search(findModel: FindModel) {
    model = findModel

    if (!Tagger.hasTagSuffix(query)) {
      results = editorText.findMatchingSites().toHashSet()
      results.highlight()
    }

    results.tag()
  }

  private fun Set<Int>.highlight() =
    mapNotNull {
      val highlighter: RangeHighlighter = createRangeHighlighter(it)
      if (highlighter.startOffset in editor.getView()) highlighter else null
    }.let { resultsInView = it.toSet() }

  private fun createRangeHighlighter(it: Int): RangeHighlighter {
    return editor.markupModel.addRangeHighlighter(it,
      if (model.isRegularExpressions) it + 1 else it + query.length,
      HighlighterLayer.LAST + 1,
      TextAttributes(null, GREEN, null, EffectType.ROUNDED_BOX, Font.PLAIN),
      HighlighterTargetArea.EXACT_RANGE)
  }

  private fun Set<Int>.tag() = runLater {
    Tagger.markOrJump(model, resultsInView.map { it.startOffset }.toSet(), this)
    resultsInView.forEach {
      if (!Tagger.hasTagsAtIndex(it.startOffset)) markup.removeHighlighter(it)
    }
    skim = false
    Handler.updateUIState()
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

  private fun init() {
    editor.colorsScheme.run {
      setAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES,
        getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES)
          .apply { backgroundColor = GREEN })
    }
    highlightManager = HighlightManager.getInstance(editor.project)
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
    highlightManager = null
    results = emptySet()
    resultsInView = emptySet()
  }
}

object T {
  operator fun plusAssign(i: Int) {}

  init {
    T += 1
  }
}

