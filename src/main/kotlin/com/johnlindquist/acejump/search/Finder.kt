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
import kotlin.comparisons.compareBy

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
    val (viewTop, viewBottom) = getVisibleRange(editor)

    fun getNextSite(oldResults: Iterator<Int>, result: FindResult): Int {
      while (oldResults.hasNext()) {
        val startingFrom = oldResults.next()
        if (startingFrom >= result.endOffset)
          return startingFrom
      }

      return result.endOffset
    }

    fun resultsIndices(): MutableList<Int> {
      val indicesToCheck = mutableListOf<Int>()
      val oldResults = sitesToCheck.iterator()
      var nextSite = if (oldResults.hasNext()) oldResults.next() else viewTop

      var result = findManager.findString(fullText, nextSite, findModel)
      while (result!!.isStringFound && result.startOffset <= viewBottom) {
        if (!editor.foldingModel.isOffsetCollapsed(result.startOffset))
          indicesToCheck.add(result.startOffset)

        nextSite = getNextSite(oldResults, result)
        result = findManager.findString(fullText, nextSite, findModel)
      }

      return indicesToCheck
    }

    return resultsIndices()
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

      stringToIndex.put("$c1", site)
      stringToIndex.put("$c0$c1", site)
      stringToIndex.put("$c1$c2", site)

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
   * Identifies the bounds of a word, defined as a contiguous group of letters
   * and digits, by expanding the provided index until a non-matching character
   * is seen on either side.
   */

  fun getWordBounds(index: Int): Pair<Int, Int> {
    var (front, end) = Pair(index, index)
    while (0 < front && document[front - 1].isJavaIdentifierPart()) front--
    while (end < document.length && document[end].isJavaIdentifierPart()) end++
    return Pair(front, end)
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
      val (start, end) = getWordBounds(index)
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

      val (left, right) = getWordBounds(index)
      val (matching, nonMatching) = tags.partition { tag ->
        substring(index, right).all { char ->
          //Prevents "...a[IJ]...ij..." ij
          !digraphs.containsKey("$char${tag[0]}") &&
            //Prevents "...re[Q]...rdre[QA]sor" req
            !newTagMap.containsKey("${tag[0]}") &&
            //Prevents "...a[IJ]...i[JX]..." ij
            !newTagMap.contains("$char${tag[0]}") &&
            //Prevents "...r[BK]iv...r[VB]in..." rivb
            newTagMap.keys.none { it[0] == char && it.last() == tag[0] } &&
            //Prevents "...i[JX]...i[IJ]..." ij
            !(char == tag[0] && newTagMap.keys.any { it[0] == tag.last() })
        }
      }

      val tag = matching.firstOrNull()

      if (tag == null) {
        val word = substring(left, right)
        println("\"$word\" rejected: " + nonMatching.joinToString(","))
        println("No remaining tags could be assigned to word: \"$word\"")
        return
      }

      val choosenTag = tagMap.inverse().getOrElse(index, { tag })!!
      newTagMap[choosenTag] = index
      //Prevents "...a[bc]...z[bc]..."
      tags.remove(choosenTag)
    }

    if (query.isNotEmpty()) {
      val possibleTag: String =
        if (2 <= query.length)
          query.substring(query.length - 2)
        else
          query.last().toString()

      if (tagMap.contains(possibleTag))
        return HashBiMap.create(mapOf(possibleTag to tagMap[possibleTag]))
    }

    val remainingSites = sortValidJumpTargets(digraphs).iterator()
    if (!findModel.isRegularExpressions || newTagMap.isEmpty())
      while (remainingSites.hasNext() && tags.isNotEmpty())
        tryToAssignTagToIndex(remainingSites.next())

    return newTagMap
  }

  private fun substring(start: Int, end: Int) = document.substring(start, end)

  private fun sortValidJumpTargets(digraphs: Multimap<String, Int>) =
    digraphs.asMap().entries.sortedBy { it.value.size }
      .flatMap { it.value }.sortedWith(compareBy(
      // Ensure that the first letter of a word is prioritized for tagging
      { document[max(0, it - 1)].isLetterOrDigit() },
      // Target words with more unique characters to the immediate right ought
      // to have first pick for tags, since they are the most "picky" targets
      { -substring(it, getWordBounds(it).second).toCharArray().distinct().size }
    ))

  /**
   * Adds pre-existing tags where search string and tag are intermingled.
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