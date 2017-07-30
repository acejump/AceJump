package com.johnlindquist.acejump.search

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import com.intellij.find.FindModel
import com.intellij.openapi.diagnostic.Logger
import com.johnlindquist.acejump.config.AceConfig.Companion.settings
import com.johnlindquist.acejump.search.Pattern.Companion.distance
import com.johnlindquist.acejump.search.Pattern.Companion.priority
import com.johnlindquist.acejump.search.Tagger.textMatches
import com.johnlindquist.acejump.view.Marker
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import java.lang.Math.max
import java.lang.Math.min
import java.util.*

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
  var textMatches: Set<Int> = emptySet()
  var allTagged = false
  private var tagMap: BiMap<String, Int> = HashBiMap.create()
  private var unseen2grams: LinkedHashSet<String> = linkedSetOf()
  private var digraphs: Multimap<String, Int> = LinkedListMultimap.create()
  private val logger = Logger.getInstance(Tagger::class.java)

  private val Iterable<Int>.allInView
    get() = all { it in editor.getView() }

  fun markOrJump(model: FindModel, results: Set<Int>) {
    textMatches = results
    if (!regex) regex = model.isRegularExpressions

    query = (if (model.isRegularExpressions) " "
    else if (regex) " " + model.stringToFind
    else model.stringToFind).toLowerCase()

    giveJumpOpportunity()
    markTags()
  }

  fun maybeJumpIfJustOneTagRemains() =
    tagMap.entries.firstOrNull()?.run { Jumper.jump(value) }

  fun markTags() {
    computeMarkers()

    if (markers.isEmpty() && query.length > 1) Skipper.ifQueryExistsSkipAhead()
  }

  private fun giveJumpOpportunity() =
    tagMap.forEach {
      if (query.endsWith(it.key)) {
        return Jumper.jump(it.value)
      }
    }

  private fun allBigrams() = settings.allowedChars.run { flatMap { e -> map { c -> "$e$c" } } }

  private fun computeMarkers() {
    if (Finder.skim && !regex) {
      markers = textMatches.map { Marker(query, null, it) }
      return
    }

    markers = scan().apply { if (this.isNotEmpty()) tagMap = this }
      .map { (tag, index) -> Marker(query, tag, index) }
  }

  private fun scan(deep: Boolean = false): BiMap<String, Int> {
    val textMatchesInView =
      if (deep) {
        allTagged = true
        textMatches
      } else {
        allTagged = false
        textMatches.filter { it in editor.getView() }.toSet()
      }

    unseen2grams = LinkedHashSet(allBigrams())
    digraphs = makeMap(editorText, textMatchesInView)

    val tags = mapDigraphs(digraphs).let { compact(it) }
    val uniToBigram = tags.count { it.key.length == 1 }.toDouble() / tags.size
    // If there are few unigrams, let's use all bigrams and try to cover all
    if (uniToBigram < 0.5 && !deep && allTagged && !Finder.skim) scan(true)

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
    val newTags: BiMap<String, Int> = transferExistingTagsCompatibleWithQuery()
    val availableTags: HashSet<String> = setupTags()

    /**
     * Iterates through the remaining available tags, until we find one that
     * matches our criteria, i.e. does not collide with an existing tag or
     * plaintext string. To have the desired behavior, this has a surprising
     * number of edge cases that must explicitly prevented.
     *
     * @param idx the index which a tag is to be assigned
     */

    fun tryToAssignTagToIndex(idx: Int): Boolean {
      val (left, right) = editorText.wordBounds(idx)

      fun hasNearbyTag(index: Int) =
        Pair(max(left, index - 2), min(right, index + 2))
          .run { (first..second).any { newTags.containsValue(it) } }

      if (hasNearbyTag(idx)) return true

      val (matching, nonMatching) = availableTags.partition { tag ->
        !newTags.containsKey("${tag[0]}") && !tag.collidesWithText(idx, right)
      }

      val tag = matching.firstOrNull()

      if (tag == null)
        String(editorText[left, right]).let {
          logger.info("\"$it\" rejected: " + nonMatching.size + " tags.")
          return false
        }
      else
        tagMap.inverse().getOrElse(idx) { tag }
          .let { chosenTag ->
            newTags[chosenTag] = idx
            // Prevents "...a[bc]...z[bc]..."
            availableTags.remove(chosenTag)
          }
      return true
    }

    newTags.run { if (regex && isNotEmpty() && values.allInView) return this }

    // Hope for the best
    allTagged = true
    sortValidJumpTargets(digraphs).forEach {
      if (availableTags.isEmpty()) {
        allTagged = false; return newTags
      }
      if (!tryToAssignTagToIndex(it)) {
        // But fail as soon as we miss one
        allTagged = false
        // We already outside the view, no need to search further if it failed
        if (it !in editor.getView()) return newTags
      }
    }

    return newTags
  }

  /**
   * Sorts jump targets to determine which positions get first choice for tags,
   * by taking into account the structure of the surrounding text. For example,
   * if the jump target is the first letter in a word, it is advantageous to
   * prioritize this location (in case we run out of tags), since the user is
   * more likely to target words by their leading character than not.
   */

  private fun sortValidJumpTargets(digraphs: Multimap<String, Int>) =
    if (regex) digraphs.values().sortedBy { it !in editor.getView() }
    else digraphs.asMap().entries.sortedBy { it.value.size }
      .flatMapTo(HashSet(), { it.value }).sortedWith(compareBy(
      { it !in editor.getView() },
      // Ensure that the first letter of a word is prioritized for tagging
      { editorText[max(0, it - 1)].isLetterOrDigit() },
      // Target words with more unique characters to the immediate right ought
      // to have first pick for tags, since they are the most "picky" targets
      { -editorText[it, editorText.wordBounds(it).second].distinct().size }))

  /**
   * Adds pre-existing tags where search string and tag overlap. For example,
   * tags starting with the last character of the query should be considered.
   */

  private fun transferExistingTagsCompatibleWithQuery() =
    tagMap.filterTo(HashBiMap.create(), { (tag, _) -> query overlaps tag })

  /**
   * Sorts available tags by key distance. Tags that are ergonomically easier to
   * type will be assigned first, ie. we should prefer to use tags which contain
   * repeated keys (ex. FF, JJ), and tags which contain physically adjacent keys
   * (ex. 12, 21) to keys that are located further apart on the keyboard.
   */

  private fun setupTags() =
    // Minimize the distance between tag characters
    unseen2grams.filter { it[0] != query[0] }
      .sortedWith(compareBy(
        { it[0].isDigit() || it[1].isDigit() },
        { distance(it[0], it.last()) },
        { priority(it.first()) }))
      .mapTo(linkedSetOf()) { it }

  fun reset() {
    regex = false
    allTagged = false
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

  fun hasTagSuffix(query: String) = tagMap.any {
    query overlaps it.key && it.value in editor.getView()
  }

  infix fun String.overlaps(xx: String) = endsWith(xx.first()) || endsWith(xx)
  infix fun canDiscard(i: Int) = !(Finder.skim || tagMap.containsValue(i))

  /**
   * Returns true IFF the receiver, inserted between the left and right indices,
   * matches an existing substring elsewhere in the editor text. We should never
   * use a tag which can be partly completed by typing plaintext, where the tag
   * is the receiver, the tag index is the leftIndex, and rightIndex is the last
   * character we care about (this is usually the last letter of the same word).
   *
   * @param leftIndex index where a tag is to be used
   * @param rightIndex index of last character (ie. end of the word)
   */

  private fun String.collidesWithText(leftIndex: Int, rightIndex: Int) =
    ((leftIndex + 1)..min(rightIndex, editorText.length)).map {
      editorText.substring(leftIndex, it) + this[0]
    }.any { it in editorText }
}