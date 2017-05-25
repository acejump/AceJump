package com.johnlindquist.acejump.search

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import com.intellij.find.FindResult
import com.johnlindquist.acejump.search.Pattern.Companion.adjacent
import com.johnlindquist.acejump.search.Pattern.Companion.nearby
import com.johnlindquist.acejump.ui.AceUI.document
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.findManager
import com.johnlindquist.acejump.ui.AceUI.findModel
import com.johnlindquist.acejump.ui.JumpInfo
import java.lang.Math.max
import java.lang.Math.min
import java.util.*

/**
 * Singleton that searches for text in the editor and tags matching results.
 */

object Finder {
  var targetModeEnabled = false
    private set
  var jumpLocations: Collection<JumpInfo> = emptyList()
    private set

  var originalQuery = ""
  var query = ""
    private set
  private var sitesToCheck = listOf<Int>()
  private var tagMap: BiMap<String, Int> = HashBiMap.create()
  private var unseen1grams: LinkedHashSet<String> = linkedSetOf()
  private var unseen2grams: LinkedHashSet<String> = linkedSetOf()
  private var digraphs: Multimap<String, Int> = LinkedListMultimap.create()

  fun findOrJump(text: String, key: Char) {
    originalQuery = text
    query = if (Pattern.contains(text)) key.toString() else text.toLowerCase()
    findModel.stringToFind = text

    maybeJump()
  }

  fun toggleTargetMode(status: Boolean? = null): Boolean {
    if (status != null) {
      targetModeEnabled = status
    } else {
      targetModeEnabled = !targetModeEnabled
    }
    return targetModeEnabled
  }

  private fun maybeJump() {
    fun jumpTo(jumpInfo: JumpInfo) {
      Jumper.jump(jumpInfo)
      reset()
    }

    // TODO: Clean up this ugliness.
    jumpLocations = determineJumpLocations()
    if (jumpLocations.size <= 1) {
      if (tagMap.containsKey(query)) {
        jumpTo(JumpInfo(query, tagMap[query]!!))
      } else if (2 <= query.length) {
        val last1 = query.substring(query.length - 1)
        val last2 = query.substring(query.length - 2)
        if (tagMap.containsKey(last2)) {
          jumpTo(JumpInfo(last2, tagMap[last2]!!))
        } else if (tagMap.containsKey(last1)) {
          val index = tagMap[last1]!!
          val charIndex = index + query.length - 1
          if (charIndex > document.length || document[charIndex] != last1[0])
            jumpTo(JumpInfo(last1, index))
        }
      }
    }
  }

  private fun determineJumpLocations(): Collection<JumpInfo> {
    populateNgrams()

    if (!findModel.isRegularExpressions || sitesToCheck.isEmpty()) {
      sitesToCheck = getSitesInView(document)
      digraphs = makeMap(document, sitesToCheck)
    }

    tagMap = compact(mapDigraphs(digraphs))

    return tagMap.values.map { JumpInfo(tagMap.inverse()[it]!!, it) }
      .sortedBy { it.index }
  }

  fun populateNgrams() {
    val a_z = 'a'..'z'
    unseen1grams.addAll(a_z.mapTo(linkedSetOf(), { "$it" }))
    a_z.flatMapTo(unseen2grams, { e -> a_z.map { c -> "$e$c" } })
  }

  /**
   * Shortens assigned tags. Effectively, this will only shorten two-character
   * tags to one-character tags. This will happen if and only if:
   *
   * 1. The shortened tag is unique among the set of existing tags.
   * 2. The shortened tag does not match the next character of text.
   * 3. The query does not end with the shortened tag, in whole or part.
   */

  private fun compact(tagMap: BiMap<String, Int>) =
    tagMap.mapKeysTo(HashBiMap.create(tagMap.size), { e ->
      val firstCharacter = e.key[0].toString()
      if (tagMap.keys.count { it[0] == e.key[0] } == 1 &&
        unseen1grams.contains(firstCharacter) &&
        !query.endsWith(firstCharacter) &&
        !query.endsWith(e.key))
        firstCharacter
      else e.key
    })

  /**
   * Returns a list of indices where the given query string ends, within the
   * current editor screen. These are full indices, ie. are not offset to the
   * first line of the editor window.
   */

  private fun getSitesInView(fullText: String): List<Int> {
    val (viewTop, viewBottom) = editor.getVisibleRange()

    fun FindResult.getNextSite(oldResults: Iterator<Int>): Int {
      while (oldResults.hasNext()) {
        val startingFrom = oldResults.next()
        if (startingFrom >= endOffset)
          return startingFrom
      }

      return endOffset
    }

    /**
     * Returns a list of indices where the query matches the index position.
     */
    fun getResultIndices(): MutableList<Int> {
      val indicesToCheck = mutableListOf<Int>()
      val oldResults = sitesToCheck.iterator()
      // If sitesToCheck is populated, we can filter it instead of redoing work
      var nextSite = if (oldResults.hasNext()) oldResults.next() else viewTop

      while (true) {
        findManager.findString(fullText, nextSite, findModel).run {
          if (!isStringFound || startOffset > viewBottom)
            return indicesToCheck

          if (!editor.foldingModel.isOffsetCollapsed(startOffset))
            indicesToCheck.add(startOffset)

          nextSite = getNextSite(oldResults)
        }
      }
    }

    return getResultIndices()
  }

  /**
   * Builds a map of all existing bigrams, starting from the index of the last
   * character in the search results. Simultaneously builds a map of all
   * available tags, by removing used bigrams after each search result, and
   * prior to the end of a word (ie. a contiguous group of letters/digits).
   */

  fun makeMap(text: CharSequence, sites: Iterable<Int>): Multimap<String, Int> {
    val stringToIndex = LinkedListMultimap.create<String, Int>()
    for (site in sites) {
      val toCheck = site + query.length
      var (p0, p1, p2) = Triple(toCheck - 1, toCheck, toCheck + 1)
      var (c0, c1, c2) = Triple(' ', ' ', ' ')
      if (0 <= p0) c0 = text[p0]
      if (p1 < text.length) c1 = text[p1]
      if (p2 < text.length) c2 = text[p2]

      stringToIndex.run {
        put("$c1", site)
        put("$c0$c1", site)
        put("$c1$c2", site)
      }

      while (c1.isLetterOrDigit()) {
        unseen1grams.remove("$c1")
        unseen2grams.remove("$c0$c1")
        unseen2grams.remove("$c1$c2")
        p0++; p1++; p2++
        c0 = text[p0]
        c1 = if (p1 < text.length) text[p1] else ' '
        c2 = if (p2 < text.length) text[p2] else ' '
      }
    }

    return stringToIndex
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
   */

  private fun mapDigraphs(digraphs: Multimap<String, Int>): BiMap<String, Int> {
    if (query.isEmpty())
      return HashBiMap.create()

    val newTagMap: BiMap<String, Int> = setupTagMap()
    val tags: HashSet<String> = setupTags(digraphs)

    fun hasNearbyTag(index: Int): Boolean {
      val (start, end) = document.wordBounds(index)
      val (left, right) = Pair(max(start, index - 2), min(end, index + 2))
      return (left..right).any { newTagMap.containsValue(it) }
    }

    /**
     * Iterates through the remaining available tags, until we find one that
     * matches our criteria, i.e. does not collide with an existing tag or
     * plaintext string. To have the desired behavior, this has a surprising
     * number of edge cases and irregularities that must explicitly prevented.
     */

    fun tryToAssignTagToIndex(index: Int) {
      if (newTagMap.containsValue(index) || hasNearbyTag(index))
        return

      val (left, right) = document.wordBounds(index)
      val (matching, nonMatching) = tags.partition { tag ->
        document[index, right].all { char ->
          // Prevents "...a[IJ]...ij..." ij
          !digraphs.containsKey("$char${tag[0]}") &&
            // Prevents "...re[Q]...rdre[QA]sor" req
            !newTagMap.containsKey("${tag[0]}") &&
            // Prevents "...a[IJ]...i[JX]..." ij
            !newTagMap.contains("$char${tag[0]}") &&
            // Prevents "...r[BK]iv...r[VB]in..." rivb
            newTagMap.keys.none { it[0] == char && it.last() == tag[0] } &&
            // Prevents "...i[JX]...i[IJ]..." ij
            !(char == tag[0] && newTagMap.keys.any { it[0] == tag.last() })
        }
      }

      val tag = matching.firstOrNull()

      if (tag == null)
        document[left, right].let {
          println("\"$it\" rejected: " + nonMatching.joinToString(","))
          println("No remaining tags could be assigned to word: \"$it\"")
        }
      else
        tagMap.inverse().getOrElse(index, { tag }).let {
          // Assigns chosen tag to index
          newTagMap[it] = index
          // Prevents "...a[bc]...z[bc]..."
          tags::remove
        }
    }

    query.run {
      if (isNotEmpty()) {
        val pTag = if (2 <= length) substring(length - 2) else last().toString()

        if (tagMap.contains(pTag))
          return HashBiMap.create(mapOf(pTag to tagMap[pTag]))
      }
    }

    if (!findModel.isRegularExpressions || newTagMap.isEmpty())
      sortValidJumpTargets(digraphs).forEach {
        if (tags.isEmpty()) return newTagMap
        tryToAssignTagToIndex(it)
      }

    return newTagMap
  }

  private fun sortValidJumpTargets(digraphs: Multimap<String, Int>) =
    digraphs.asMap().entries.sortedBy { it.value.size }
      .flatMap { it.value }.sortedWith(compareBy(
      // Ensure that the first letter of a word is prioritized for tagging
      { document[max(0, it - 1)].isLetterOrDigit() },
      // Target words with more unique characters to the immediate right ought
      // to have first pick for tags, since they are the most "picky" targets
      { -document[it, document.wordBounds(it).second].distinct().size }
    ))

  /**
   * Adds pre-existing tags where search string and tag are intermingled. For
   * example, tags starting with the last character of the query should be
   * considered. The user could be starting to select a tag, or continuing to
   * filter plaintext results.
   */

  private fun setupTagMap() =
    tagMap.filterTo(HashBiMap.create<String, Int>(),
      { (key, _) -> query == key || query.last() == key.first() })

  private fun setupTags(searchResults: Multimap<String, Int>) =
    unseen2grams.sortedWith(compareBy(
      // Least frequent first-character comes first
      { searchResults[it[0].toString()].orEmpty().size },
      // Adjacent keys come before non-adjacent keys
      { !adjacent[it[0]]!!.contains(it.last()) },
      // Rotate to remove "clumps" (ie. AA, AB, AC => AA BA CA)
      String::last,
      // Minimize the distance between tag characters
      { nearby[it[0]]!!.indexOf(it.last()) }
    )).mapTo(linkedSetOf<String>(), { it })

  fun reset() {
    findModel.isRegularExpressions = false
    findModel.stringToFind = ""
    targetModeEnabled = false
    sitesToCheck = listOf<Int>()
    digraphs.clear()
    tagMap.clear()
    query = ""
    originalQuery = ""
    unseen1grams.clear()
    unseen2grams.clear()
    jumpLocations = emptyList()
  }
}
