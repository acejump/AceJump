package com.johnlindquist.acejump.label

import com.intellij.find.FindModel
import com.intellij.openapi.diagnostic.Logger
import com.johnlindquist.acejump.label.Pattern.Companion.sortTags
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Jumper
import com.johnlindquist.acejump.search.Skipper
import com.johnlindquist.acejump.view.Marker
import com.johnlindquist.acejump.view.Model.viewBounds

/**
 * Singleton that works with Finder to tag text search results in the editor.
 *
 * @see Finder
 */

object Tagger {
  var markers: List<Marker> = emptyList()
    private set

  var regex = false
  var query = ""
    private set
  var full = false // Tracks whether all search results were successfully tagged
  var textMatches: Set<Int> = emptySet()
  private var tagMap: Map<String, Int> = emptyMap()
  private val logger = Logger.getInstance(Tagger::class.java)

  private val Iterable<Int>.allInView
    get() = all { it in viewBounds }

  fun markOrJump(model: FindModel, results: Set<Int>) {
    textMatches = results

    model.run {
      if (!regex) regex = isRegularExpressions
      query = if (isRegularExpressions) " " else stringToFind.toLowerCase()
      if (regex) query += model.stringToFind
    }

    giveJumpOpportunity()
    markTags()
  }

  fun maybeJumpIfJustOneTagRemains() =
    tagMap.entries.firstOrNull()?.run { Jumper.jump(value) }

  private fun giveJumpOpportunity() =
    tagMap.forEach { if (query.endsWith(it.key)) return Jumper.jump(it.value) }

  private fun markTags() {
    computeMarkers()

    if (markers.isEmpty() && query.length > 1) Skipper.ifQueryExistsSkipAhead()
  }

  private fun computeMarkers() {
    markers = scan().apply { if (this.isNotEmpty()) tagMap = this }
      .map { (tag, index) -> Marker(query, tag, index) }
  }

  private fun scan(): Map<String, Int> {
    full = true
    if (query.isEmpty()) return emptyMap()
    return assignTags(textMatches).let { compact(it) }
  }

  /**
   * Shortens assigned tags. Effectively, this will only shorten two-character
   * tags to one-character tags. This will happen if and only if:
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
    newTags.run { if (regex && isNotEmpty() && values.allInView) return this }
    if (hasTagSuffixInView(query)) return newTags

    val vacantResults = results.filter { it !in newTags.values }.toSet()
    logger.info("Vacant Results: ${vacantResults.size}")
    val availableTags = sortTags(query).filter { it !in tagMap }.toSet()
    logger.info("Available Tags: ${availableTags.size}")
    if (availableTags.size < vacantResults.size) full = false

    if (regex) return availableTags.zip(vacantResults).toMap()

    timeElapsed = System.currentTimeMillis() - timeElapsed
    logger.info("Time elapsed: $timeElapsed")

    newTags.putAll(Solver.solve(vacantResults, availableTags))
    return newTags
  }

  /**
   * Adds pre-existing tags where search string and tag overlap. For example,
   * tags starting with the last character of the query should be considered.
   */

  private fun transferExistingTagsCompatibleWithQuery() =
    tagMap.filterTo(HashMap(), { (tag, index) ->
      query overlaps tag || index in textMatches
    })

  fun reset() {
    regex = false
    full = false
    textMatches = emptySet()
    tagMap = emptyMap()
    query = ""
    markers = emptyList()
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
    tagMap.any { query overlaps it.key && it.value in viewBounds }

  infix fun String.overlaps(xx: String) = endsWith(xx.first()) || endsWith(xx)
  infix fun canDiscard(i: Int) = !(Finder.skim || tagMap.containsValue(i))
}