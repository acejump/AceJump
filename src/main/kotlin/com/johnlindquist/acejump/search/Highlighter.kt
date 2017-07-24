package com.johnlindquist.acejump.search

import com.intellij.find.FindModel
import com.intellij.find.FindResult
import com.intellij.find.impl.livePreview.LivePreviewController
import com.intellij.find.impl.livePreview.SearchResults
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColors
import com.johnlindquist.acejump.control.Handler
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.project
import java.awt.Color

object Highlighter : Disposable, SearchResults.SearchResultsListener {
  private var model: FindModel = FindModel()
  override fun searchResultsUpdated(sr: SearchResults?) {
    results?.occurrences
      ?.filter { it.startOffset in editor.getView() }
      .let { resultsInView = it }

    if (!model.skim) doFind()
  }

  private fun SearchResults.filterOrNarrowInView() =
    resultsInView?.partition { Finder.hasTagsAtIndex(it.startOffset) }?.run {
      second.forEach { exclude(it) }
      resultsInView = first
    }

  override fun cursorMoved() = TODO()
  override fun updateFinished() = TODO()
  override fun dispose() = TODO()

  private var results: SearchResults? = null
  private var resultsInView: List<FindResult>? = null

  private lateinit var livePreviewController: LivePreviewController

  fun search(findModel: FindModel) {
    model = findModel
    if (results == null) init()

    results?.filterOrNarrowInView()

    val hasTags = Finder.hasTagsStartingWithChar(model.stringToFind.last())
    if (!hasTags) livePreviewController.updateInBackground(model, false)
    if (hasTags) doFind()
  }

  private fun doFind() =
    runLater {
      Finder.findOrJump(model, results?.occurrences?.map { it.startOffset })
      results?.filterOrNarrowInView()
      Handler.updateUIState()
    }

  private fun init() {
    results = SearchResults(editor, project).apply { addListener(Highlighter) }

    editor.colorsScheme.run {
      setAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES,
        getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES)
          .apply { backgroundColor = Color.GREEN })
    }

    livePreviewController = LivePreviewController(results, null, this)
    livePreviewController.on()
  }

  fun getResultsInView() = resultsInView?.map { it.startOffset }

  fun discard() {
    results?.removeListener(this)
    results?.dispose()
    livePreviewController.off()
    results = null
    resultsInView = null
  }
}