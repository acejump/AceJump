package com.johnlindquist.acejump.search

import com.intellij.find.FindModel
import com.intellij.find.FindResult
import com.intellij.find.impl.livePreview.LivePreviewController
import com.intellij.find.impl.livePreview.SearchResults
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.naturalHighlight
import com.johnlindquist.acejump.view.Model.project
import java.awt.Color.GREEN

object Searcher : Disposable {
  var searchResults: SearchResults? = null
  private var occurencesInView: List<FindResult>? = null
  override fun dispose() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  private lateinit var livePreviewController: LivePreviewController

  fun search(findModel: FindModel) {
    if (searchResults == null) init()

    searchResults?.run {
      if (occurencesInView == null)
        occurencesInView = occurrences.filter { it.startOffset in editor.getView() }
      else {
        val partitioned = occurencesInView!!.partition { Finder.hasTagsAtIndex(it.startOffset) }
        partitioned.second.forEach { exclude(it) }
        occurencesInView = partitioned.first
      }
    }

    if (Finder.hasTagsStartingWithChar(findModel.stringToFind.last())) return
    livePreviewController.on()
  }

  private fun init() {
    searchResults = SearchResults(editor, project)

    editor.colorsScheme.run {
      setAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES,
        getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES)
          .apply { backgroundColor = GREEN })
    }

    livePreviewController = LivePreviewController(searchResults, null, this)
    livePreviewController.userActivityDelay = 0
    livePreviewController.on()
  }


  fun discard() {
    searchResults?.clear()

    editor.colorsScheme.run {
      setAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES,
        getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES)
          .apply { backgroundColor = naturalHighlight })
    }

    livePreviewController.off()
    searchResults = null
    occurencesInView = null
  }
}