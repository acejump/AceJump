package com.johnlindquist.acejump.search

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import com.intellij.find.FindModel
import com.intellij.openapi.diagnostic.Logger
import com.johnlindquist.acejump.config.AceConfig.Companion.settings
import com.johnlindquist.acejump.search.Pattern.Companion.distance
import com.johnlindquist.acejump.search.Pattern.Companion.priotity
import com.johnlindquist.acejump.search.Tagger.textMatches
import com.johnlindquist.acejump.view.Marker
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import java.lang.Math.max
import java.lang.Math.min
import java.util.*

/**
 * Singleton that searches for text in the editor and tags matching results.
 */

object Tagger {
  var targetModeEnabled = false
  var markers: List<Marker> = emptyList()
    private set

  var regex = false
  var query = ""
    private set
  var textMatches: Set<Int> = emptySet()
  private var tagMap: BiMap<String, Int> = HashBiMap.create()
  private var unseen2grams: LinkedHashSet<String> = linkedSetOf()
  private var digraphs: Multimap<String, Int> = LinkedListMultimap.create()
  private val logger = Logger.getInstance(Tagger::class.java)
  var resultsInView: Set<Int> = emptySet()

  private val Iterable<Int>.allInView
    get() = all { it in editor.getView() }

  fun markOrJump(model: FindModel, resultsInView: Set<Int>, results: Set<Int>) {
    this.resultsInView = resultsInView
    textMatches = results
    if (!regex) regex = model.isRegularExpressions
    else
      query = (if (model.isRegularExpressions) " "
      else if (regex) " " + model.stringToFind
      else model.stringToFind).toLowerCase()

    mark()
  }

  fun toggleTargetMode(status: Boolean? = null): Boolean {
    targetModeEnabled = status ?: !targetModeEnabled
    return targetModeEnabled
  }

  fun maybeJumpIfJustOneTagRemains() =
    tagMap.entries.firstOrNull()?.run { Jumper.jump(value) }

  fun mark() {
    if (textMatches.isNotEmpty()) computeMarkers()
    if (markers.size > 1 || query.length < 2) return

    giveJumpOpportunity()

    if (markers.isEmpty()) Skipper.doesQueryExistIfSoSkipToIt()
  }

  private fun giveJumpOpportunity() {
    tagMap.forEach { if(query.completes(it.key)) Jumper.jump(it.value) }
//    if (query.length > 2 && tagMap[query.takeLast(2)] != null) {
//      Jumper.jump(tagMap[query.takeLast(2)]!!)
//      return true
//    }
//    if (query.length > 1 && tagMap[query.takeLast(1)] != null) {
//      val indexLast1 = tagMap[query.takeLast(1)]!!
//      val cIdx = (indexLast1 + query.length - 1).coerceAtMost(editorText.length)
//      if (editorText[cIdx] != query.last()) {
//        Jumper.jump(indexLast1)
//        return true
//      }
//    }
//
//    return false
  }

  private fun allBigrams() = settings.allowedChars.run { flatMap { e -> map { c -> "$e$c" } } }

  private fun computeMarkers() {
    if (Finder.skim && !regex) {
      markers = textMatches.map { Marker(query, null, it) }
      return
    }

    unseen2grams = LinkedHashSet(allBigrams())
    digraphs = makeMap(editorText, resultsInView)

    markers =
      (if (hasTagSuffix(query)) HashBiMap.create(tagMap.filterByQuery(query))
      else mapDigraphs(digraphs).let { compact(it) })
        .apply { if (this.isNotEmpty()) tagMap = this }
        .map { Marker(query, it.key, it.value) }
  }

  fun Map<String, Int>.filterByQuery(q: String) = filter { q.completes(it.key) }

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
   * Builds a map of all existing bigrams, starting from the index of the last
   * character in the search results. Simultaneously builds a map of all
   * available tags, by removing used bigrams after each search result, and
   * prior to the end of a word (ie. a contiguous group of letters/digits).
   */

  fun makeMap(text: CharSequence, sites: Set<Int>): Multimap<String, Int> =
    if (regex) LinkedListMultimap.create<String, Int>().apply {
      sites.forEach { put(" ", it) }
    } else LinkedListMultimap.create<String, Int>().apply {
      sites.forEach { site ->
        val toCheck = site + query.length
        var (p0, p1, p2) = Triple(toCheck - 1, toCheck, toCheck + 1)
        var (c0, c1, c2) = Triple(' ', ' ', ' ')
        if (0 <= p0 && p0 < text.length) c0 = text[p0]
        if (p1 < text.length) c1 = text[p1]
        if (p2 < text.length) c2 = text[p2]

        put("$c1", site)
        put("$c0$c1", site)
        put("$c1$c2", site)

        while (c1.isLetterOrDigit()) {
          unseen2grams.remove("$c0$c1")
          unseen2grams.remove("$c1$c2")
          p0++; p1++; p2++
          c0 = text[p0]
          c1 = if (p1 < text.length) text[p1] else ' '
          c2 = if (p2 < text.length) text[p2] else ' '
        }
      }
    }

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
   * @param digraphs All strings to be tagged and indices where to find them
   *
   * @return A list of all tags and their corresponding indices
   */

  private fun mapDigraphs(digraphs: Multimap<String, Int>): BiMap<String, Int> {
    if (query.isEmpty()) return HashBiMap.create()
    val newTagMap: BiMap<String, Int> = transferAnyExistingTagsThatMatchQuery()
    val availableTags: HashSet<String> = setupTags()

    /**
     * Iterates through the remaining available tags, until we find one that
     * matches our criteria, i.e. does not collide with an existing tag or
     * plaintext string. To have the desired behavior, this has a surprising
     * number of edge cases and irregularities that must explicitly prevented.
     *
     * @param idx the index which a tag is to be assigned
     */

    fun tryToAssignTagToIndex(idx: Int) {
      val (left, right) = editorText.wordBounds(idx)

      fun hasNearbyTag(index: Int) =
        Pair(max(left, index - 2), min(right, index + 2))
          .run { (first..second).any { newTagMap.containsValue(it) } }

      if (hasNearbyTag(idx)) return

      val (matching, nonMatching) = availableTags.partition { tag ->
        // Prevents a situation where some sites couldn't be assigned last time
        !newTagMap.containsKey("${tag[0]}") &&
          ((idx + 1)..min(right, editorText.length)).map {
            // Never use a tag which can be partly completed by typing plaintext
            editorText.substring(idx, it) + tag[0]
          }.none { it in editorText }
      }

      val tag = matching.firstOrNull()

      if (tag == null)
        String(editorText[left, right]).let {
          logger.info("\"$it\" rejected: " + nonMatching.size + " tags.")
        }
      else
        tagMap.inverse().getOrElse(idx) { tag }
          .let { chosenTag ->
            newTagMap[chosenTag] = idx
            // Prevents "...a[bc]...z[bc]..."
            availableTags.remove(chosenTag)
          }
    }

    query.run {
      if (isNotEmpty())
        substring(max(0, length - 2)).let {
          if (it in tagMap && it.length < length)
            return HashBiMap.create(mapOf(it to tagMap[it]))
        }
    }

    newTagMap.run { if (regex && isNotEmpty() && values.allInView) return this }

    sortValidJumpTargets(digraphs).forEach {
      if (availableTags.isEmpty()) return newTagMap
      tryToAssignTagToIndex(it)
    }

    return newTagMap
  }

  private fun sortValidJumpTargets(digraphs: Multimap<String, Int>) =
    if (regex) digraphs.values()
    else digraphs.asMap().entries.sortedBy { it.value.size }
      .flatMapTo(HashSet(), { it.value }).sortedWith(compareBy(
      // Ensure that the first letter of a word is prioritized for tagging
      { editorText[max(0, it - 1)].isLetterOrDigit() },
      // Target words with more unique characters to the immediate right ought
      // to have first pick for tags, since they are the most "picky" targets
      { -editorText[it, editorText.wordBounds(it).second].distinct().size }))

  /**
   * Adds pre-existing tags where search string and tag are intermingled. For
   * example, tags starting with the last character of the query should be
   * considered. The user could be starting to select a tag, or continuing to
   * filter plaintext results.
   */

  private fun transferAnyExistingTagsThatMatchQuery() =
    tagMap.filterTo(HashBiMap.create<String, Int>(),
      { (key, _) -> query == key || query.last() == key.first() })

  private fun setupTags() =
    // Minimize the distance between tag characters
    unseen2grams.filter { it[0] != query[0] }
      .sortedWith(compareBy(
        { distance(it[0], it.last()) },
        { priotity(it.first()) }))
      .mapTo(linkedSetOf()) { it }

  fun reset() {
    regex = false
    targetModeEnabled = false
    textMatches = emptySet()
    digraphs.clear()
    tagMap.clear()
    query = ""
    unseen2grams.clear()
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

  fun hasTagSuffix(query: String) = tagMap.any { query.completes(it.key) }
  fun String.completes(tag: String) = endsWith(tag.first()) || endsWith(tag)
  fun hasTagsAtIndex(i: Int) = Finder.skim || tagMap.containsValue(i)
}