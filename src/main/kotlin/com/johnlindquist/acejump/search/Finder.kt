package com.johnlindquist.acejump.search

import com.intellij.find.FindModel
import com.intellij.find.FindResult
import com.intellij.find.impl.livePreview.LivePreviewController
import com.intellij.find.impl.livePreview.SearchResults
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
import com.johnlindquist.acejump.control.Handler
import com.johnlindquist.acejump.control.Trigger
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import com.johnlindquist.acejump.view.Model.project

object Finder : Disposable, SearchResults.SearchResultsListener {
  private var highlighters: LivePreviewController? = null
  private var results: SearchResults? = null
  private var resultsInView: List<FindResult>? = null
  private var model = FindModel()
  val isShiftSelectEnabled
    get() = model.stringToFind.last().isUpperCase()

  var skim = false

  var query: String = ""
    set(value) {
      field = value

      if (value.isNotEmpty()) {
        model = FindModel().apply { stringToFind = value }
        if (value.length == 1) skim().apply { Trigger(350L) { search() } }
        else findOrDropLast(value)
      }
    }

  override fun cursorMoved() = TODO()
  override fun updateFinished() = TODO()
  override fun dispose() = TODO()
  override fun searchResultsUpdated(sr: SearchResults?) {
    results?.occurrences
      ?.filter { it.startOffset in editor.getView() }
      .let { resultsInView = it }

    doTag()
  }

  private fun skim() {
    skim = true
    search(model)
  }

  fun search(string: String = query) = search(FindModel().apply { stringToFind = string })

  fun search(pattern: Pattern) =
    Finder.search(FindModel().apply {
      stringToFind = pattern.string
      isRegularExpressions = true
      Tagger.reset()
    })

  fun search(findModel: FindModel) {
    model = findModel
    if (results == null) init()
    if (Tagger.hasTagSuffix(query)) doTag()
    else highlighters?.updateInBackground(model, false)
  }

  private fun doTag() {
    Tagger.markOrJump(model, results?.occurrences?.map { it.startOffset } ?: listOf())
    skim = false
    Handler.updateUIState()
  }

  private fun findOrDropLast(text: String = query) =
    if (isQueryAlive(text)) search(text) else {
      query = text.dropLast(1)
    }

  private fun init() {
    results = SearchResults(editor, project).apply { addListener(Finder) }

    editor.colorsScheme.run {
      setAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES,
        getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES)
          .apply { backgroundColor = null; effectColor = null; errorStripeColor = null })
    }

    highlighters = LivePreviewController(results, null, this)
    highlighters?.on()
  }

  private fun isQueryAlive(query: String) =
    results?.occurrences?.any { editorText.regionMatches(it.startOffset, query, 0, query.length) } ?: true ||
      Tagger.hasTagSuffix(query)

  fun discard() {
    query = ""
    model = FindModel()
    results?.removeListener(this)
    results?.dispose()
    highlighters?.off()
    results = null
    resultsInView = null
  }
}