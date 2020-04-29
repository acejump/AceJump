package org.acejump.label

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.intellij.openapi.diagnostic.Logger
import org.acejump.config.AceConfig
import org.acejump.control.Scroller
import org.acejump.search.AceFindModel
import org.acejump.search.Finder
import org.acejump.search.Jumper
import org.acejump.search.Resettable
import org.acejump.search.canIndicesBeSimultaneouslyVisible
import org.acejump.search.getFeasibleRegion
import org.acejump.search.runNow
import org.acejump.view.Marker
import org.acejump.view.Model.editor
import org.acejump.view.Model.editorText
import org.acejump.view.Model.viewBounds
import java.util.*
import kotlin.collections.Iterable
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.collections.all
import kotlin.collections.any
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.contains
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.isNotEmpty
import kotlin.collections.iterator
import kotlin.collections.joinToString
import kotlin.collections.lastOrNull
import kotlin.collections.map
import kotlin.collections.mapKeysTo
import kotlin.collections.none
import kotlin.collections.partition
import kotlin.collections.plus
import kotlin.collections.sortedSetOf
import kotlin.collections.sortedWith
import kotlin.collections.toMap
import kotlin.collections.toSortedSet
import kotlin.collections.zip
import kotlin.streams.toList
import kotlin.system.measureTimeMillis

/**
 * The [Tagger] works with [Finder] to assign selectable tags to search results
 * in the editor. These tags may be selected by typing their label at any point
 * during the search. Since there is no explicit signal to begin selecting a tag
 * when AceJump is active, we must infer when a tag is being selected. We do so
 * by carefully assigning tags to search results so that every search result can
 * be expanded indefinitely and selected unambiguously any time the user wishes.
 *
 * To do so, we must solve a tag assignment problem, where each search result is
 * assigned an available tag. The Tagger (1) identifies available tags (2) uses
 * the [Solver] to assign them, and (3) determines when a previously assigned
 * tag has been selected, then (4) calls [Jumper] to reposition the caret.
 */

object Tagger : Resettable {
  var markers: List<Marker> = emptyList()
    private set
  var regex = false
    private set
  var query = ""
    private set

  @Volatile
  var full = false // Tracks whether all search results were successfully tagged
  var textMatches: SortedSet<Int> = sortedSetOf()
  @Volatile
  var tagMap: BiMap<String, Int> = HashBiMap.create()
  private val logger = Logger.getInstance(Tagger::class.java)

  @Volatile
  var tagSelected = false

  private fun Iterable<Int>.noneInView() = none { it in viewBounds }

  fun markOrJump(model: AceFindModel, results: SortedSet<Int>) {
    model.run {
      if (!regex) regex = isRegularExpressions
      query = if (regex) " $stringToFind" else stringToFind.mapIndexed { i, c ->
        if (i == 0) c else c.toLowerCase()
      }.joinToString("")
      logger.info("Received query: \"$query\"")
    }

    val availableTags = AceConfig.getCompatibleTags(query) { it !in tagMap }

    measureTimeMillis { textMatches = refineSearchResults(results) }
      .let { if (!regex) logger.info("Refined search results in $it ms") }

    giveJumpOpportunity()
    if (!tagSelected && query.isNotEmpty()) mark(textMatches, availableTags)

    if (1 < query.length && tagMap.values.noneInView())
      runNow { Scroller.scroll() }
  }

  /**
   * Narrows down results that need to be tagged. For example, "eee" need not be
   * tagged three times. Furthermore, we will not be able to tag every location
   * in a very large document.
   */

  private fun refineSearchResults(results: SortedSet<Int>): SortedSet<Int> {
    if (regex) return results

    val admittance: (t: Int) -> Boolean = { editorText.admitsTagAtLocation(it) }
    val sites = if (results.size < 500) results.filter(admittance)
    else results.parallelStream().filter(admittance).toList()

    val discards = results.size - sites.size
    discards.let { if (it > 0) logger.info("Discarded $it unsuitable results") }

    return sites.toSortedSet()
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

  /**
   * Checks whether a visible tag has been selected, and if so, jumps to it.
   */

  private fun giveJumpOpportunity() =
    tagMap.entries.firstOrNull { it.value in viewBounds && it solves query }
      ?.run {
        logger.info("User selected tag: ${key.toUpperCase()}")
        tagSelected = true
        Jumper.jumpTo(value)
      }

  /**
   * Returns true if and only if a tag location is unambiguously completed by a
   * given query. This can only happen if the query matches the underlying text,
   * AND ends with the tag in question. Tags are case-insensitive.
   */

  private infix fun Map.Entry<String, Int>.solves(query: String) =
    query.endsWith(key, true) && isCompatibleWithQuery(query)

  private fun Map.Entry<String, Int>.isCompatibleWithQuery(query: String) =
    query.getPlaintextPortion(key).let { text ->
      regex || editorText.regionMatches(
        thisOffset = value,
        other = text,
        otherOffset = 0,
        length = text.length,
        ignoreCase = true
      )
    }

  private fun mark(results: SortedSet<Int>, availableTags: Set<String>) =
      assignMissingTags(results, availableTags).compact().apply {
        tagMap = this
        markers = if(results.isEmpty()) // Last query char must be a tag char
          tagMap.map { (tag, index) -> Marker(query, tag, index) }
        else results.map { Marker(query, tagMap.inverse()[it], it) }
        full = markers.size == tagMap.size
      }

  /**
   * Shortens previously assigned tags. Two-character tags may be shortened to
   * one-character tags if and only if:
   *
   * 1. The shortened tag is unique among the set of visible tags.
   * 3. The query does not end with the shortened tag, in whole or part.
   */

  private fun Map<String, Int>.compact(): HashBiMap<String, Int> {
    var timeElapsed = System.currentTimeMillis()
    var totalCompacted = 0
    val compacted = mapKeysTo(HashBiMap.create(size)) { e ->
      val tag = e.key
      if (e.value !in viewBounds) return@mapKeysTo tag
      // Avoid matching query - will trigger a jump. TODO: lift this constraint.
      val queryEndsWith = query.endsWith(tag[0]) || query.endsWith(tag)
      return@mapKeysTo if (!queryEndsWith && tag.canBeShortened(this)) {
        totalCompacted++
        tag[0].toString()
      } else tag
    }

    timeElapsed = System.currentTimeMillis() - timeElapsed
    logger.info("Compacted $totalCompacted visible tags in $timeElapsed ms")
    return compacted
  }

  private fun String.canBeShortened(tagMap: Map<String, Int>): Boolean {
    var i = 0
    var canBeShortened = true

    runNow {
      for (tag in tagMap) {
        if (tag.key[0] == this[0] &&
          editor.canIndicesBeSimultaneouslyVisible(tagMap[this]!!, tag.value))
          i++
        if (1 < i) {
          canBeShortened = false; break
        }
      }
    }

    return canBeShortened
  }

  /**
   * Assigns [availableTags] to [results]. Initially, all results are vacant.
   * If there are any untagged results visible, assign as many tags as possible.
   * Assuming all visible tags have been assigned, there is nothing left to do.
   */

  private fun assignMissingTags(results: Set<Int>,
                                availableTags: Set<String>): Map<String, Int> {
    var timeElapsed = System.currentTimeMillis()
    val oldTags = transferExistingTagsCompatibleWithQuery()
    // Ongoing queries with results in view do not need further tag assignment
    oldTags.run {
      if (regex && isNotEmpty() && values.all { it in viewBounds }) return this
      else if (hasTagSuffixInView(query)) return this
    }

    val remainder = getFeasibleSites(results)
    val (onScreen, offScreen) = remainder.partition { it in viewBounds }
    val completeResultSet = onScreen + offScreen
    // Some results are untagged. Let's assign some tags!
    val vacantResults = completeResultSet.filter { it !in oldTags.values }

    logger.run {
      timeElapsed = System.currentTimeMillis() - timeElapsed
      info("Results on screen: ${onScreen.size}, off screen: ${offScreen.size}")
      info("Vacant Results: ${vacantResults.size}")
      info("Available Tags: ${availableTags.size}")
      info("Time elapsed: $timeElapsed ms")
    }

    return if (regex) solveRegex(vacantResults, availableTags) else oldTags +
      Solver(editorText, query, vacantResults, availableTags, viewBounds).map()
  }

  private fun getFeasibleSites(results: Set<Int>): List<Int> {
    val feasibleRegion = getFeasibleRegion(results)
    val remainder = results.partition { it in feasibleRegion }
    remainder.second.size.let { if (it > 0) logger.info("Discarded $it OOBs") }
    return remainder.first
  }

  private fun solveRegex(vacantResults: List<Int>, availableTags: Set<String>) =
    availableTags.sortedWith(AceConfig.defaultTagOrder).zip(vacantResults).toMap()

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
    tagMap = HashBiMap.create()
    query = ""
    markers = emptyList()
    tagSelected = false
  }

  private fun String.getPlaintextPortion(tag: String) = when {
    endsWith(tag, true) -> dropLast(tag.length)
    endsWith(tag.first(), true) -> dropLast(1)
    else -> this
  }

  /**
   * Returns true if the Tagger contains a match in the new view, that is not
   * contained (visible) in the old view. This method assumes that [textMatches]
   * are in ascending order by index.
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