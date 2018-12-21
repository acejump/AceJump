package org.acejump.label

import com.intellij.openapi.diagnostic.Logger
import org.acejump.control.Scroller
import org.acejump.label.Pattern.Companion.defaultTagOrder
import org.acejump.label.Pattern.Companion.filterTags
import org.acejump.search.*
import org.acejump.search.Jumper.hasJumped
import org.acejump.view.Marker
import org.acejump.view.Model.caretOffset
import org.acejump.view.Model.editor
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
 * has been selected, then (4) calls Jumper to reposition the caret.
 *
 * @see Finder
 * @see Solver
 * @see Jumper
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
    get() = none { it inside viewBounds }

  fun markOrJump(model: AceFindModel, results: SortedSet<Int>) {
    model.run {
      if (!regex) regex = isRegularExpressions
      query = if (regex) " $stringToFind" else stringToFind.mapIndexed { i, c ->
        if (i == 0) c else c.toLowerCase()
      }.joinToString("")
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

    val feasibleRegion = getFeasibleRegion(sites) ?: return sites.toSortedSet()
    val remainder = sites.partition { hasAmpleTags || it in feasibleRegion }
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

  fun jumpToNextOrNearestVisible() = Jumper.jumpTo(nextOrNearestVisibleOffset())

  fun nextOrNearestVisibleOffset() =
    tagMap.values.filter { it in viewBounds }.sorted().let { tags ->
      tags.firstOrNull { it > caretOffset } ?: tags.firstOrNull() ?: caretOffset
    }

  private fun giveJumpOpportunity() =
    tagMap.entries.firstOrNull { it.solves(query) && it.value in viewBounds }
      ?.run {
        logger.info("User selected tag: ${key.toUpperCase()}")
        Jumper.jumpTo(value)
      }

  /**
   * Returns true if and only if a tag location is unambiguously completed by a
   * given query. This can only happen if the query matches the underlying text,
   * AND ends with the tag in question. Tags are case-insensitive.
   */

  private fun Map.Entry<String, Int>.solves(query: String) =
    query.endsWith(key, true) && isCompatibleWithQuery(query)

  private fun Map.Entry<String, Int>.isCompatibleWithQuery(query: String) =
    getTextPortionOfQuery(key, query).let { text ->
      regex || editorText.regionMatches(
        thisOffset = value,
        other = text,
        otherOffset = 0,
        length = text.length,
        ignoreCase = true
      )
    }

  private fun markOrScrollToNextOccurrence() {
    markAndMapTags().apply { if (isNotEmpty()) tagMap = this }

    if (markers.isNotEmpty() && markers.noneInView && 1 < query.length)
      runAndWait { Scroller.scroll() }
  }

  private fun markAndMapTags(): Map<String, Int> {
    full = true
    if (query.isEmpty()) return emptyMap()
    return compact(assignTags(textMatches)).apply {
      runAndWait {
        markers = map { (tag, index) -> Marker(query, tag, index) }
      }
    }
  }

  /**
   * Shortens previously assigned tags. Two-character tags may be shortened to
   * one-character tags if and only if:
   *
   * 1. The shortened tag is unique among the set of visible tags.
   * 3. The query does not end with the shortened tag, in whole or part.
   */

  private fun compact(tagMap: Map<String, Int>): Map<String, Int> {
    var timeElapsed = System.currentTimeMillis()
    var totalCompacted = 0
    val compacted = tagMap.mapKeysTo(HashMap(tagMap.size)) { e ->
      val tag = e.key
      if (e.value !in viewBounds) return@mapKeysTo tag
      val canBeCompacted = tag.canAssignShortTag(tagMap)
      // Avoid matching query - will trigger a jump. TODO: lift this constraint.
      val queryEndsWith = query.endsWith(tag[0]) || query.endsWith(tag)
      return@mapKeysTo if (canBeCompacted && !queryEndsWith) {
        totalCompacted++
        tag[0].toString()
      } else tag
    }

    timeElapsed = System.currentTimeMillis() - timeElapsed
    logger.info("Compacted $totalCompacted visible tags in $timeElapsed ms")
    return compacted
  }

  private fun String.canAssignShortTag(tagMap: Map<String, Int>): Boolean {
    var i = 0
    for (tag in tagMap) {
      if (tag.key[0] == this[0] &&
        editor.canIndicesBeSimultaneouslyVisible(tagMap[this]!!, tag.value)) i++
      if (1 < i) return false
    }

    return true
  }

  private fun assignTags(results: Set<Int>): Map<String, Int> {
    var timeElapsed = System.currentTimeMillis()
    val newTags = transferExistingTagsCompatibleWithQuery()
    // Ongoing queries with results in view do not need further tag assignment
    newTags.run {
      if (regex && isNotEmpty() && values.all { it in viewBounds }) return this
      else if (hasTagSuffixInView(query)) return this
    }

    // TODO: Fix missing tags, cf. https://github.com/acejump/AceJump/issues/245
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
    availableTags.sortedWith(defaultTagOrder).zip(vacantResults).toMap()

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
    when {
      query.endsWith(tag, true) -> query.dropLast(tag.length)
      query.endsWith(tag.first(), true) -> query.dropLast(1)
      else -> query
    }

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
    tagMap.any { it.value in viewBounds && it.isCompatibleWithQuery(query) }

  infix fun canDiscard(i: Int) = !(Finder.skim || i in tagMap.values || i == -1)
}