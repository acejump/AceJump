package com.johnlindquist.acejump.search

import com.intellij.find.FindModel
import com.intellij.find.FindResult
import com.intellij.find.impl.livePreview.LivePreviewController
import com.intellij.find.impl.livePreview.SearchResults
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColors
import com.johnlindquist.acejump.control.Handler
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import com.johnlindquist.acejump.view.Model.project
import java.awt.Color

object Searcher : Disposable, SearchResults.SearchResultsListener {
  var query: String = ""
    set(value) {
      model = FindModel().apply { stringToFind = value }
      field = value
    }

  var model: FindModel = FindModel()

  override fun searchResultsUpdated(sr: SearchResults?) {
    results?.occurrences
      ?.filter { it.startOffset in editor.getView() }
      .let { resultsInView = it }

    doFind()
  }

  private fun SearchResults.filterOrNarrowInView() =
    resultsInView?.partition { Tagger.hasTagsAtIndex(it.startOffset) }?.run {
      second.forEach { exclude(it) }
      resultsInView = first
    }

  override fun cursorMoved() = TODO()
  override fun updateFinished() = TODO()
  override fun dispose() = TODO()

  private var results: SearchResults? = null
  private var resultsInView: List<FindResult>? = null

  private lateinit var livePreviewController: LivePreviewController

  fun skim() = search(model.apply { skim = true })

  fun search(string: String = query) = search(FindModel().apply { stringToFind = string })

  fun search(pattern: Pattern) =
    Searcher.search(FindModel().apply {
      stringToFind = pattern.string
      isRegularExpressions = true
      skim = false
      Tagger.reset()
    })

  fun search(findModel: FindModel) {
    model = findModel
    if (results == null) init()

    results?.filterOrNarrowInView()

    livePreviewController.updateInBackground(model, false)
  }

  private fun doFind() =
    runLater {
      Tagger.markOrJump(model, results?.occurrences?.map { it.startOffset })
      results?.filterOrNarrowInView()
      Handler.updateUIState()
    }

  fun findOrDropLast(text: String = query) =
    if (!isQueryDeadEnd(text)) {
      search(text)
    } else {
      query = text.dropLast(1)
    }

  private fun init() {
    results = SearchResults(editor, project).apply { addListener(Searcher) }

    editor.colorsScheme.run {
      setAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES,
        getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES)
          .apply { backgroundColor = Color.GREEN })
    }

    livePreviewController = LivePreviewController(results, null, this)
    livePreviewController.on()
  }

  fun isQueryDeadEnd(query: String) =
    results?.occurrences?.any { editorText.regionMatches(it.startOffset, query, 0, query.length) } ?: true ||
      !Tagger.hasTagSuffix(query)

  fun getResultsInView() = resultsInView?.map { it.startOffset }

  fun discard() {
    query = ""
    model = FindModel()
    results?.removeListener(this)
    results?.dispose()
    livePreviewController.off()
    results = null
    resultsInView = null
  }
}