package org.acejump.search

import com.intellij.openapi.diagnostic.Logger
import org.acejump.view.Boundary.FULL_FILE_BOUNDARY
import org.acejump.view.Model.LONG_DOCUMENT
import org.acejump.view.Model.boundaries
import org.acejump.view.Model.editorText
import kotlin.streams.toList

/**
 * Returns a list of indices where the query begins, within the given range.
 * These are full indices, ie. are not offset to the beginning of the range.
 */

internal object Scanner {
  val cores = Runtime.getRuntime().availableProcessors() - 1
  private val logger = Logger.getInstance(Scanner::class.java)

  fun findMatchingSites(model: AceFindModel, cache: Set<Int>) =
    if (!LONG_DOCUMENT || cache.size != 0 || boundaries != FULL_FILE_BOUNDARY)
      editorText.search(model, cache, boundaries.intRange()).toSortedSet()
    else
      editorText.chunk().parallelStream().map { chunk ->
        editorText.search(model, cache, chunk)
      }.toList().flatten().toSortedSet()

  private fun String.chunk(chunkSize: Int = count { it == '\n' } / cores + 1) =
    splitToSequence("\n", "\r").toList().run {
      logger.info("Parallelizing query across $cores cores")
      var offset = 0
      chunked(chunkSize).map {
        val len = it.joinToString("\n").length
        (offset..(offset + len)).also { offset += len + 1 }
      }
    }

  fun String.search(model: AceFindModel, cache: Set<Int>, chunk: IntRange) =
    run {
      val query = model.stringToFind
      if (isEmpty() || query.isEmpty()) sortedSetOf<Int>()
      // If the cache is populated, filter it instead of redoing prior work
      else if (cache.isNotEmpty()) filterCache(cache, query)
      else findAll(model.toRegex(), chunk)
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

  fun CharSequence.findAll(regex: Regex, chunk: IntRange) =
    generateSequence(
      seedFunction = { regex.find(this, chunk.first) },
      nextFunction = { result -> filterNext(result, chunk) }
    ).map { it.range.first }.toList()

  fun filterNext(result: MatchResult, chunk: IntRange): MatchResult? {
    val next = result.next() ?: return null
    val offset = next.range.first
    return if (offset !in chunk) null else next
  }
}
