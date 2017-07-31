package com.johnlindquist.acejump.label

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.intellij.find.FindModel
import com.intellij.openapi.diagnostic.Logger
import com.johnlindquist.acejump.label.Pattern.Companion.setupTags
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Jumper
import com.johnlindquist.acejump.search.Skipper
import com.johnlindquist.acejump.search.getView
import com.johnlindquist.acejump.view.Marker
import com.johnlindquist.acejump.view.Model.editor

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
  var full = false
  private var deep: Boolean = false
  var textMatches: Set<Int> = emptySet()
  private var tagMap: BiMap<String, Int> = HashBiMap.create()
  private val logger = Logger.getInstance(Tagger::class.java)

  private val Iterable<Int>.allInView
    get() = all { it in editor.getView() }

  fun markOrJump(model: FindModel, results: Set<Int>) {
    textMatches = results
    if (!regex) regex = model.isRegularExpressions

    query = (if (model.isRegularExpressions) " "
    else model.stringToFind).toLowerCase()

    model.run {
      if (isRegularExpressions && stringToFind.all { it.isLetterOrDigit() })
        query += model.stringToFind
    }

    giveJumpOpportunity()
    markTags()
  }

  fun maybeJumpIfJustOneTagRemains() =
    tagMap.entries.firstOrNull()?.run { Jumper.jump(value) }

  private fun markTags() {
    computeMarkers()

    if (markers.isEmpty() && query.length > 1 && !Finder.skim)
      Skipper.ifQueryExistsSkipAhead()
  }

  private fun giveJumpOpportunity() =
    tagMap.forEach { if (query.endsWith(it.key)) return Jumper.jump(it.value) }

  private fun computeMarkers() {
    if (Finder.skim) return

    markers = scan().apply { if (this.isNotEmpty()) tagMap = this }
      .map { (tag, index) -> Marker(query, tag, index) }
  }

  private fun scan(): BiMap<String, Int> {
    deep = false
    val resultsToTag =
      if (deep) {
        full = true
        textMatches
      } else {
        full = false
        textMatches.filter { it in editor.getView() }.toSet()
      }

    val tags = assignTags(resultsToTag).let { compact(it) }
    val uniToBigram = tags.count { it.key.length == 1 }.toDouble() / tags.size
    // If there are few unigrams, let's use all bigrams and try to cover all
    if (uniToBigram < 0.5 && !deep && full) {
      deep = true; scan()
    }

    return tags
  }

  /**
   * Shortens assigned tags. Effectively, this will only shorten two-character
   * tags to one-character tags. This will happen if and only if:
   *
   * 1. The shortened tag is unique among the set of existing tags.
   * 3. The query does not end with the shortened tag, in whole or part.
   */

  private fun compact(tagMap: BiMap<String, Int>) =
    tagMap.mapKeysTo(HashBiMap.create(tagMap.size)) { e ->
      val firstChar = e.key[0]
      val firstCharUnique = tagMap.keys.count { it[0] == firstChar } == 1
      val queryEndsWith = query.endsWith(firstChar) || query.endsWith(e.key)
      if (firstCharUnique && !queryEndsWith) firstChar.toString() else e.key
    }

  // Provides a way to short-circuit the full text search if a match is found
  private operator fun String.contains(key: String) =
    textMatches.any { regionMatches(it, key, 0, key.length) }

  /**
   * Maps tags to search results. Tags *must* have the following properties:
   *
   * 1. A tag must not match *any* bigrams on the screen.
   * 2. A tag's 1st letter must not match any letters of the covered word.
   * 3. Tag must not match any combination of any plaintext and tag. "e(a[B)X]"
   * 4. Once assigned, a tag must never change until it has been selected. *A.
   *
   * Tags *should* have the following properties:
   *
   * A. Should be as short as possible. A tag may be "compacted" later.
   * B. Should prefer keys that are physically closer to the last key pressed.
   *
   * @param results All indices to be tagged
   *
   * @return A list of all tags and their corresponding indices
   */

  private fun assignTags(results: Set<Int>): BiMap<String, Int> {
    if (query.isEmpty()) return HashBiMap.create()
    val newTags: BiMap<String, Int> = transferExistingTagsCompatibleWithQuery()
    newTags.run { if (regex && isNotEmpty() && values.allInView) return this }

    val vacantResults = results.filter { it !in newTags.values }.toSet()
    val availableTags = setupTags(query).filter { it !in tagMap }.toSet()
    if (availableTags.size < vacantResults.size) full = false

    if (regex) return HashBiMap.create(availableTags.zip(vacantResults).toMap())

    newTags.putAll(Solver.solve(vacantResults, availableTags))

    return newTags
  }

  /**
   * Adds pre-existing tags where search string and tag overlap. For example,
   * tags starting with the last character of the query should be considered.
   */

  private fun transferExistingTagsCompatibleWithQuery() =
    tagMap.filterTo(HashBiMap.create(), { (tag, index) ->
      query overlaps tag || index in textMatches })

  fun reset() {
    regex = false
    full = false
    deep = false
    textMatches = emptySet()
    tagMap.clear()
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

  fun hasTagSuffix(query: String) = tagMap.any {
    query overlaps it.key && it.value in editor.getView()
  }

  infix fun String.overlaps(xx: String) = endsWith(xx.first()) || endsWith(xx)
  infix fun canDiscard(i: Int) = !(Finder.skim || tagMap.containsValue(i))
}