package com.johnlindquist.acejump.search

import com.intellij.find.FindModel
import com.johnlindquist.acejump.view.Model.boundaries
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.viewBounds
import kotlin.text.RegexOption.MULTILINE

/**
* Returns a list of indices where the query begins, within the given range.
* These are full indices, ie. are not offset to the beginning of the range.
*/

object Scanner {
  fun findMatchingSites(editorText: String, model: FindModel, cache: Set<Int>) =
    editorText.run {
      val key: String = model.stringToFind.toLowerCase()
      // If the cache is populated, filter it instead of redoing prior work
      if (cache.isEmpty()) findAll(model.sanitizedString(), 0)
      else cache.asSequence().filter { regionMatches(it, key, 0, key.length) }
    }.toSortedSet()

  private fun Set<Int>.isCacheValidForRange() =
    viewBounds.let { view ->
      first() < view.first && last() > view.last
    }

  private fun CharSequence.findAll(key: String, startingFrom: Int) =
    generateSequence({ Regex(key, MULTILINE).find(this, startingFrom) },
      ::filterNextResult).map { it.range.first }

  private tailrec fun filterNextResult(result: MatchResult): MatchResult? {
    val next = result.next() ?: return null
    val offset = next.range.first
    return if (offset >= boundaries.endInclusive) null
    else if (editor.isNotFolded(offset) && offset in boundaries) next
    else filterNextResult(next)
  }
}
