package org.acejump.label

import com.intellij.find.FindModel
import com.intellij.openapi.diagnostic.Logger
import org.acejump.label.Pattern.Companion.defaultOrder
import org.acejump.label.Pattern.Companion.filterTags
import org.acejump.search.*
import org.acejump.search.Jumper.hasJumped
import org.acejump.view.Marker
import org.acejump.view.Model.caretOffset
import org.acejump.view.Model.editorText
import org.acejump.view.Model.viewBounds
import java.util.SortedSet
import kotlin.collections.HashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.system.measureTimeMillis

/**
 * Singleton that works with Finder to assign selectable tags to search results
 * in the editor. These tags may be selected by typing their label at any point
 * during the search. Since there is no explicit signal to begin selecting a tag
 * when AceJump is active, we must infer when a tag is being selected. We do so
 * by carefully assigning tags to search results so that every search result can
 * be expanded indefinitely and selected unambiguously any time the user wishes.
 *
 * To do so, we must solve a tag assignment problem, where each search result is
 * assigned an available tag. The Tagger (1) identifies available tags (2) uses
 * the Solver to assign them, and (3) determines when a previously assigned tag
 * has been selected unambiguously, to reposition the caret.
 *
 * @see Finder
 * @see Solver
 */

object Tagger : Resettable {
  var markers: List<Marker> = emptyList()
    private set
  var regex = false
    private set
  var query = ""
    private set
  var full = false // Tracks whether all search results were successfully tagged
  var textMatches: SortedSet<Int> = sortedSetOf<Int>()
  private var tagMap: Map<String, Int> = emptyMap()
  private val logger = Logger.getInstance(Tagger::class.java)

  private val Iterable<Marker>.noneInView
    get() = none { it.index in viewBounds }

  fun markOrJump(model: FindModel, results: SortedSet<Int>) {
    model.run {
      if (!regex) regex = isRegularExpressions
      query = if (regex) " $stringToFind" else stringToFind.toLowerCase()
      logger.info("Received query: \"$query\"")
    }

    measureTimeMillis {
      textMatches = refineSearchResults(results)
    }.let { if (!regex) logger.info("Refined search results in $it ms") }

    giveJumpOpportunity()
    if (!hasJumped) markOrScrollToNextOccurrence()
  }

  /**
   * Narrows down results that need to be tagged. For example, "eee" need not be
   * tagged three times. Furthermore, we will not be able to tag every location
   * in a very large document.
   */

  private fun refineSearchResults(results: SortedSet<Int>): SortedSet<Int> {
    if (regex) return results
    val availableTags = filterTags(query).filter { it !in tagMap }.toSet()

    val sites = results.filter { editorText.admitsTagAtLocation(it) }
    val discards = results.size - sites.size
    discards.let { if (it > 0) logger.info("Discarded $it contiguous results") }

    val hasAmpleTags = availableTags.size >= sites.size

    val fesiableRegion = getFesiableRegion(sites) ?: return sites.toSortedSet()
    val remainder = sites.partition { hasAmpleTags || it in fesiableRegion }
    remainder.second.size.let { if (it > 0) logger.info("Discarded $it OOBs") }

    return remainder.first.toSortedSet()
  }

  /**
   * Returns whether a given index inside a String can be tagged with a two-
   * character tag (either to the left or right) without visually overlapping
   * any nearby tags.
   */

  private fun String.admitsTagAtLocation(loc: Int) = when {
    1 < query.length -> true
    loc - 1 < 0 -> true
    loc + 1 >= length -> true
    this[loc] isUnlike this[loc - 1] -> true
    this[loc] isUnlike this[loc + 1] -> true
    this[loc] != this[loc - 1] -> true
    this[loc] != this[loc + 1] -> true
    this[loc + 1] == '\r' || this[loc + 1] == '\n' -> true
    this[loc - 1] == this[loc] && this[loc] == this[loc + 1] -> false
    this[loc + 1].isWhitespace() && this[(loc + 2)
      .coerceAtMost(length - 1)].isWhitespace() -> true
    else -> false
  }

  private infix fun Char.isUnlike(other: Char) =
    this.isLetterOrDigit() xor other.isLetterOrDigit() ||
      this.isWhitespace() xor other.isWhitespace()

  fun jumpToNextOrNearestVisible() =
    tagMap.values.filter { it in viewBounds }.sorted().let { tags ->
      if (caretOffset in viewBounds) {
        tags.firstOrNull { it > caretOffset }?.let { return Jumper.jump(it) }
        tags.lastOrNull { it < caretOffset }?.let { return Jumper.jump(it) }
      } else {
        tags.firstOrNull()?.let { Jumper.jump(it) }
      }
    }

  private fun giveJumpOpportunity() =
    tagMap.entries.firstOrNull { it.solves(query) }?.run {
      logger.info("User selected tag: ${key.toUpperCase()}")
      Jumper.jump(value)
    }

  /**
   * Returns true if and only if a tag location is unambiguously completed by a
   * given query. This can only happen if the query matches the underlying text,
   * AND ends with the tag in question.
   */

  private fun Map.Entry<String, Int>.solves(query: String) =
    query.endsWith(key) && isCompatibleWithQuery(query)

  private fun Map.Entry<String, Int>.isCompatibleWithQuery(query: String) =
    getTextPortionOfQuery(key, query).let { text ->
      regex || editorText.regionMatches(value, text, 0, text.length, true)
    }

  private fun markOrScrollToNextOccurrence() {
    markAndMapTags().apply { if (isNotEmpty()) tagMap = this }

    if (!markers.isEmpty() && markers.noneInView && query.length > 1)
      runAndWait { Scroller.ifQueryExistsScrollToNextOccurrence() }
  }

  private fun markAndMapTags(): Map<String, Int> {
    full = true
    if (query.isEmpty()) return emptyMap()
    return assignTags(textMatches).let { compact(it) }
      .apply {
        runAndWait {
          markers = map { (tag, index) -> Marker(query, tag, index) }
        }
      }
  }

  /**
   * Shortens previously assigned tags. Two-character tags may be shortened to
   * one-character tags if and only if:
   *
   * 1. The shortened tag is unique among the set of existing tags.
   * 3. The query does not end with the shortened tag, in whole or part.
   */

  private fun compact(tagMap: Map<String, Int>): Map<String, Int> =
    tagMap.mapKeysTo(HashMap(tagMap.size)) { e ->
      val firstChar = e.key[0]
      val firstCharUnique = tagMap.keys.count { it[0] == firstChar } == 1
      val queryEndsWith = query.endsWith(firstChar) || query.endsWith(e.key)
      if (firstCharUnique && !queryEndsWith) firstChar.toString() else e.key
    }

  private fun assignTags(results: Set<Int>): Map<String, Int> {
    var timeElapsed = System.currentTimeMillis()
    val newTags = transferExistingTagsCompatibleWithQuery()
    // Ongoing queries with results in view do not need further tag assignment
    newTags.run {
      if (regex && isNotEmpty() && values.all { it in viewBounds }) return this
      else if (hasTagSuffixInView(query)) return this
    }

    val (onScreen, offScreen) = results.partition { it in viewBounds }
    val completeResultSet = onScreen + offScreen
    // Some results are untagged. Let's assign some tags!
    val vacantResults = completeResultSet.filter { it !in newTags.values }
    val availableTags = filterTags(query).filter { it !in tagMap }.toSet()
    if (availableTags.size < vacantResults.size) full = false

    logger.run {
      timeElapsed = System.currentTimeMillis() - timeElapsed
      info("Results on screen: ${onScreen.size}, off screen: ${offScreen.size}")
      info("Vacant Results: ${vacantResults.size}")
      info("Available Tags: ${availableTags.size}")
      info("Time elapsed: $timeElapsed ms")
    }

    return if (regex) solveRegex(vacantResults, availableTags)
    else Solver.solve(vacantResults, availableTags)
  }

  private fun solveRegex(vacantResults: List<Int>, availableTags: Set<String>) =
    availableTags.sortedWith(defaultOrder).zip(vacantResults).toMap()

  /**
   * Adds pre-existing tags where search string and tag overlap. For example,
   * tags starting with the last character of the query will be included. Tags
   * that no longer match the query will be discarded.
   */

  private fun transferExistingTagsCompatibleWithQuery() =
    tagMap.filter { it.isCompatibleWithQuery(query) || it.value in textMatches }

  override fun reset() {
    regex = false
    full = false
    textMatches = sortedSetOf()
    tagMap = emptyMap()
    query = ""
    markers = emptyList()
  }

  private fun getTextPortionOfQuery(tag: String, query: String) =
    if (query.endsWith(tag)) query.substring(0, query.length - tag.length)
    else if (query.endsWith(tag.first())) query.substring(0, query.length - 1)
    else query

  /**
   * Returns true if the Tagger contains a match in the new view, that is not
   * contained (visible) in the old view. This method assumes that textMatches
   * are in ascending order by index.
   *
   * @see textMatches
   *
   * @return true if there is a match in the new range not in the old range
   */

  fun hasMatchBetweenOldAndNewView(old: IntRange, new: IntRange) =
    textMatches.lastOrNull { it < old.first } ?: -1 >= new.first ||
      textMatches.firstOrNull { it > old.last } ?: new.last < new.last

  fun hasTagSuffixInView(query: String) =
    tagMap.any { it.isCompatibleWithQuery(query) && it.value in viewBounds }

  infix fun canDiscard(i: Int) = !(Finder.skim || tagMap.containsValue(i) || i == -1)
}