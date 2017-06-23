package com.johnlindquist.acejump.search

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import com.intellij.find.FindModel
import com.johnlindquist.acejump.search.Pattern.Companion.adjacent
import com.johnlindquist.acejump.search.Pattern.Companion.nearby
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.editorText
import com.johnlindquist.acejump.ui.JumpInfo
import java.lang.Math.max
import java.lang.Math.min
import java.nio.CharBuffer
import java.util.*
import kotlin.collections.LinkedHashSet
import kotlin.text.RegexOption.MULTILINE

/**
 * Singleton that searches for text in the editor and tags matching results.
 */

object Finder {
  var targetModeEnabled = false
    private set
  var jumpLocations: Collection<JumpInfo> = emptyList()
    private set

  var isRegex = false
  var origQ = ""
  var regex = ""
  var query = ""
    private set
  var sitesToCheck = listOf<Int>()
  private var tagMap: BiMap<String, Int> = HashBiMap.create()
  private var unseen2grams: LinkedHashSet<String> = linkedSetOf()
  private var digraphs: Multimap<String, Int> = LinkedListMultimap.create()

  fun findOrJump(findModel: FindModel) =
    findModel.run {
      isRegex = isRegularExpressions
      origQ = findModel.stringToFind
      regex = if (isRegex) findModel.compileRegExp().pattern() else
        Regex.escape(stringToFind.toLowerCase())
      query = if (isRegex) " " else stringToFind.toLowerCase()
      maybeJump()
    }

  fun toggleTargetMode(status: Boolean? = null): Boolean {
    if (status != null) targetModeEnabled = status
    else targetModeEnabled = !targetModeEnabled
    return targetModeEnabled
  }

  fun maybeJumpIfJustOneTagRemains() =
    tagMap.entries.firstOrNull()?.run { jumpTo(JumpInfo(key, value)) }

  fun jumpTo(jumpInfo: JumpInfo) = Jumper.jump(jumpInfo)

  private fun maybeJump() {
    jumpLocations = determineJumpLocations()
    if (jumpLocations.isEmpty()) Skipper.ifQueryExistsSkipToNextInEditor()

    // TODO: Clean up this ugliness.
    if (jumpLocations.size > 1 || query.length < 2) return

    val last1 = query.substring(query.length - 1)
    val indexLast1 = tagMap[last1]

    val last2 = query.substring(query.length - 2)
    val indexLast2 = tagMap[last2]

    // If the tag is two chars, the query must be at least 3
    if (indexLast2 != null && query.length > 2)
      jumpTo(JumpInfo(last2, indexLast2))
    else if (indexLast1 != null) {
      val charIndex = indexLast1 + query.length - 1
      if (charIndex >= editorText.length || editorText[charIndex] != last1[0])
        jumpTo(JumpInfo(last1, indexLast1))
    }
  }

  private fun determineJumpLocations(): Collection<JumpInfo> {
    fun allBigrams() = with('a'..'z') { flatMap { e -> map { c -> "$e$c" } } }
    unseen2grams = LinkedHashSet(allBigrams())

    if (!isRegex || sitesToCheck.isEmpty()) {
      sitesToCheck = editorText.findInEditor()
      digraphs = makeMap(editorText, sitesToCheck.filter { it in editor.getView() })
    }

    return compact(mapDigraphs(digraphs)).apply { tagMap = this }.run {
      values.map { JumpInfo(inverse()[it]!!, it) }.sortedBy { it.index }
    }
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

  /**
   * Returns a list of indices where the query begins, within the given range.
   * These are full indices, ie. are not offset to the beginning of the range.
   */

  fun String.findInEditor(key: String = query.toLowerCase(),
                          cache: List<Int> = sitesToCheck): List<Int> =
    // If the cache is populated, filter it instead of redoing extra work
    if (!cache.isEmpty()) cache.filter { regionMatches(it, key, 0, key.length) }
    else find(regex).filterFoldedRegions()

  fun CharSequence.find(key: String, startingFrom: Int = 0): Sequence<Int> =
    Regex(key, MULTILINE).findAll(this, startingFrom).map { it.range.first }

  fun String.clip(range: IntRange): CharSequence =
    if (length <= range.endInclusive) this
    // Be very careful to avoid substring copying here for performance reasons
    else CharBuffer.wrap(this).subSequence(0, range.endInclusive)

  // Do not accept any sites which fall between folded regions in the gutter
  fun Sequence<Int>.filterFoldedRegions() =
    filter { !editor.foldingModel.isOffsetCollapsed(it) }.toList()

  /**
   * Builds a map of all existing bigrams, starting from the index of the last
   * character in the search results. Simultaneously builds a map of all
   * available tags, by removing used bigrams after each search result, and
   * prior to the end of a word (ie. a contiguous group of letters/digits).
   */

  fun makeMap(text: CharSequence, sites: List<Int>): Multimap<String, Int> =
    LinkedListMultimap.create<String, Int>().apply {
      sites.forEach { site ->
        val toCheck = site + query.length
        var (p0, p1, p2) = Triple(toCheck - 1, toCheck, toCheck + 1)
        var (c0, c1, c2) = Triple(' ', ' ', ' ')
        if (0 <= p0) c0 = text[p0]
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
   */

  private fun mapDigraphs(digraphs: Multimap<String, Int>): BiMap<String, Int> {
    if (query.isEmpty()) return HashBiMap.create()

    val newTagMap: BiMap<String, Int> = setupTagMap()
    val tags: HashSet<String> = setupTags(digraphs)

    fun String.hasNearbyTag(index: Int): Boolean {
      val (start, end) = wordBounds(index)
      val (left, right) = Pair(max(start, index - 2), min(end, index + 2))
      return (left..right).any { newTagMap.containsValue(it) }
    }

    /**
     * Iterates through the remaining available tags, until we find one that
     * matches our criteria, i.e. does not collide with an existing tag or
     * plaintext string. To have the desired behavior, this has a surprising
     * number of edge cases and irregularities that must explicitly prevented.
     */

    fun tryToAssignTagToIndex(idx: Int) {
      if (newTagMap.containsValue(idx) || editorText.hasNearbyTag(idx)) return

      val (left, right) = editorText.wordBounds(idx)
      val (matching, nonMatching) = tags.partition { tag ->
        // Prevents a situation where some sites couldn't be assigned last time
        !newTagMap.containsKey("${tag[0]}") &&
          ((idx + 1)..right).map {
            // Never use a tag which can be partly completed by typing plaintext
            editorText.substring(idx, min(it, editorText.length)) + tag[0]
          }.none { !editorText.findInEditor(it).isEmpty() }
      }

      val tag = matching.firstOrNull()

      if (tag == null)
        String(editorText[left, right]).let {
          // TODO: Write this to this IDE log instead
          println("\"$it\" rejected: " + nonMatching.joinToString(","))
          println("No remaining tags could be assigned to word: \"$it\"")
        }
      else
        tagMap.inverse().getOrElse(idx) { tag }.let { chosenTag ->
          newTagMap[chosenTag] = idx
          // Prevents "...a[bc]...z[bc]..."
          tags.remove(chosenTag)
        }
    }

    query.run {
      if (isNotEmpty()) {
        val pTag = substring(max(0, length - 2))

        if (tagMap.contains(pTag))
          return HashBiMap.create(mapOf(pTag to tagMap[pTag]))
      }
    }

    if (!isRegex || newTagMap.isEmpty())
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

  private fun setupTagMap() = tagMap.filterTo(HashBiMap.create<String, Int>(),
    { (key, _) -> query == key || query.last() == key.first() })

  private fun setupTags(searchResults: Multimap<String, Int>) =
    unseen2grams.sortedWith(compareBy(
      // Least frequent first-character comes first
//      { searchResults[it[0].toString()].orEmpty().size },
      // Adjacent keys come before non-adjacent keys
      { !adjacent[it[0]]!!.contains(it.last()) },
      // Rotate to remove "clumps" (ie. AA, AB, AC => AA BA CA)
      { it.last() },
      // Minimize the distance between tag characters
      { nearby[it[0]]!!.indexOf(it.last()) }
    )).mapTo(linkedSetOf<String>()) { it }

  fun reset() {
    isRegex = false
    targetModeEnabled = false
    sitesToCheck = listOf()
    digraphs.clear()
    tagMap.clear()
    origQ = ""
    query = ""
    regex = ""
    unseen2grams.clear()
    jumpLocations = emptyList()
  }
}