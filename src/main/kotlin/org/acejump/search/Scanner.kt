package org.acejump.search

import com.intellij.openapi.diagnostic.Logger
import org.acejump.view.Boundary.FULL_FILE_BOUNDARY
import org.acejump.view.Model.LONG_DOCUMENT
import org.acejump.view.Model.boundaries
import org.acejump.view.Model.editorText
import java.util.*
import kotlin.streams.toList

/**
 * Returns a set of indices indicating query matches, within the given range.
 * These are full indices, i.e. are not offset to the beginning of the range.
 */

internal object Scanner {
  val cores = Runtime.getRuntime().availableProcessors() - 1
  private val logger = Logger.getInstance(Scanner::class.java)

  /**
   * Returns [SortedSet] of indices matching the [model]. Providing a [cache]
   * will filter prior results instead of searching the editor contents.
   */

  fun find(model: AceFindModel, boundaries: IntRange, cache: Set<Int> = emptySet()): SortedSet<Int> =
    if (!LONG_DOCUMENT || cache.size != 0 || boundaries != FULL_FILE_BOUNDARY.intRange())
      editorText.search(model, cache, boundaries).toSortedSet()
    else editorText.chunk().parallelStream().map { chunk ->
      editorText.search(model, cache, chunk)
    }.toList().flatten().toSortedSet()

  /**
   * Divides lines of text into equally-sized chunks for parallelized search.
   */

  private fun String.chunk(): List<IntRange> {
    val lines = splitToSequence("\n", "\r").toList()
    val chunkSize = lines.size / cores + 1
    logger.info("Parallelizing ${lines.size}-line search across $cores cores")
    var offset = 0
    return lines.chunked(chunkSize).map {
      val len = it.joinToString("\n").length
      (offset..(offset + len)).also { offset += len + 1 }
    }
  }

  /**
   * Searches the [cache] (if it is populated), or else the whole document.
   */

  fun String.search(model: AceFindModel, cache: Set<Int>, chunk: IntRange) =
    run {
      val query = model.stringToFind
      if (isEmpty() || query.isEmpty()) sortedSetOf<Int>()
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

  fun filterNext(result: MatchResult, chunk: IntRange): MatchResult? =
    result.next()?.let { if(it.range.first !in chunk) null else it }
}
