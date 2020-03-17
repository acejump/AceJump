package org.acejump.search

import com.intellij.openapi.diagnostic.Logger
import org.acejump.view.Model.boundaries
import org.acejump.view.Model.editor
import kotlin.streams.toList

/**
 * Returns a list of indices where the query begins, within the given range.
 * These are full indices, ie. are not offset to the beginning of the range.
 */

internal object Scanner {
  val numCores = Runtime.getRuntime().availableProcessors() - 1
  const val minLinesToParallelize = 1000
  private val logger = Logger.getInstance(Scanner::class.java)

  fun findMatchingSites(searchText: String, model: AceFindModel, cache: Set<Int>) =
    searchText.run {
      val chunks: List<Pair<Int, String>> = chunkIfTooLong(searchText)
      if (chunks.size == 1)
        chunks.first().second.search(model, searchText, cache)
      else chunks.parallelStream().map { (offset, string) ->
        string.search(model, searchText, cache).map { it + offset }
      }.toList().flatten()
    }.toSortedSet()

  private fun String.chunkIfTooLong(searchText: String): List<Pair<Int, String>> {
    val lines = count { it == '\n' }
    var offset = 0
    return if (minLinesToParallelize < lines)
      splitToSequence("\n", "\r").toList().run { chunked(lines / numCores + 1) }
        .map { Pair(offset, it.joinToString("\n")).also { offset += it.second.length + 1 } }
        .also { logger.info("Query parallelization enabled ($lines lines)") }
    else listOf(Pair(searchText.length, searchText))
      .also { logger.info("Query parallelization disabled ($lines lines)") }
  }

  private fun String.search(model: AceFindModel, searchText: String, cache: Set<Int>) =
    run {
      val query = model.stringToFind
      if (searchText.isEmpty() || query.isEmpty()) sortedSetOf<Int>()
      // If the cache is populated, filter it instead of redoing prior work
      else if (cache.isEmpty()) findAll(model.toRegex(), boundaries.start)
      else filterCache(cache, query)
    }.toList()

  private fun String.filterCache(cache: Set<Int>, query: String) =
    cache.asSequence().filter { index ->
      regionMatches(
        thisOffset = index + query.length - 1,
        other = query.last().toString(),
        otherOffset = 0,
        length = 1,
        ignoreCase = query.last().isLowerCase()
      )
    }.toList()

  private fun CharSequence.findAll(regex: Regex, startingFromIndex: Int) =
    generateSequence(
      seedFunction = { regex.find(this, startingFromIndex) },
      nextFunction = ::filterNextResult
    ).map { it.range.first }.toList()

  private tailrec fun filterNextResult(result: MatchResult): MatchResult? {
    val next = result.next() ?: return null
    val offset = next.range.first
    return if (offset !in boundaries) null
    else if (editor.isNotFolded(offset)) next
    else filterNextResult(next)
  }
}
