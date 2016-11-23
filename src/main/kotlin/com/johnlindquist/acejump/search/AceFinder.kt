package com.johnlindquist.acejump.search

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.find.FindResult
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.util.EventDispatcher
import com.johnlindquist.acejump.keycommands.AceJumper
import com.johnlindquist.acejump.search.Pattern.Companion.adjacent
import com.johnlindquist.acejump.search.Pattern.Companion.nearby
import com.johnlindquist.acejump.ui.JumpInfo
import java.util.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import kotlin.comparisons.compareBy

class AceFinder(val findManager: FindManager, var editor: EditorImpl) {
  val document = editor.document.charsSequence.toString().toLowerCase()
  val eventDispatcher = EventDispatcher.create(ChangeListener::class.java)
  val findModel: FindModel = findManager.findInFileModel.clone()

  var query = ""
  var hasJumped = false
  var tagMap: BiMap<String, Int> = HashBiMap.create()
  var unseen1grams: LinkedHashSet<String> = linkedSetOf()
  var unseen2grams: LinkedHashSet<String> = linkedSetOf()
  var jumpLocations: Collection<JumpInfo> = emptyList()

  init {
    findModel.isFindAll = true
    findModel.isFromCursor = true
    findModel.isForward = true
    findModel.isRegularExpressions = false
    findModel.isWholeWordsOnly = false
    findModel.isCaseSensitive = false
    findModel.isPreserveCase = false
    findModel.setSearchHighlighters(true)
  }

  fun find(text: String, key: Char) {
    // "0" is Backspace
    if (key == 0.toChar()) {
      reset()
      return
    }

    query = if (Pattern.contains(text)) key.toString() else text.toLowerCase()
    findModel.stringToFind = text

    val application = ApplicationManager.getApplication()
    application.runReadAction({ jump(key) })
    application.invokeLater({
      if (text.isNotEmpty())
        eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
    })
  }

  var targetModeEnabled = false
  fun toggleTargetMode(): Boolean {
    targetModeEnabled = !targetModeEnabled
    return targetModeEnabled
  }

  val aceJumper = AceJumper(editor, document)
  private fun jump(key: Char) {
    fun jumpTo(jumpInfo: JumpInfo) {
      if (key.isUpperCase())
        aceJumper.setSelectionFromCaretToOffset(jumpInfo.index)
      else
        aceJumper.moveCaret(jumpInfo.index)
      hasJumped = true

      if (targetModeEnabled)
        aceJumper.selectWordAtCaret()

      reset()
    }

    jumpLocations = determineJumpLocations()
    if (jumpLocations.size <= 1) {
      if (tagMap.containsKey(query)) {
        jumpTo(JumpInfo(query, query, tagMap[query]!!, editor))
      } else if (2 <= query.length) {
        val last1: String = query.substring(query.length - 1)
        val last2: String = query.substring(query.length - 2)
        if (tagMap.containsKey(last2)) {
          jumpTo(JumpInfo(last2, query, tagMap[last2]!!, editor))
        } else if (tagMap.containsKey(last1)) {
          val index = tagMap[last1]!!
          if (document[index + query.length - 1].toLowerCase() != last1[0])
            jumpTo(JumpInfo(last1, query, index, editor))
        }
      }
    }
  }

  private var sitesToCheck = listOf<Int>()
  private var digraphs: Multimap<String, Int> = LinkedListMultimap.create()

  private fun determineJumpLocations(): Collection<JumpInfo> {
    populateNgrams()

    if (!findModel.isRegularExpressions || sitesToCheck.isEmpty()) {
      sitesToCheck = getSitesInView(document)
      digraphs = makeMap(document, sitesToCheck)
    }
    tagMap = compact(mapDigraphs(digraphs))

    return plotJumpLocations()
  }

  fun populateNgrams() {
    val a_z = 'a'..'z'
    unseen1grams.addAll(a_z.mapTo(linkedSetOf(), { "$it" }))
    a_z.flatMapTo(unseen2grams, { e -> a_z.map { c -> "${e}$c" } })
  }

  /**
   * Shortens assigned tags. Effectively, this will only shorten two-character
   * strings to one-character strings. This should happen if and only if the
   * shortened tag:
   *
   * 1. Is unique among the set of all existing tags.
   * 2. The shortened tag does not equal the next character.
   * 3. The query does not end with the tag, in whole or part.
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
    val (windowStart, windowEnd) = getVisibleRange(editor)

    if (query.isEmpty())
      return (windowStart..(windowEnd - 2)).toList()

    val indicesToCheck = arrayListOf<Int>()
    val oldResults = sitesToCheck.iterator()
    var startingFrom = if (oldResults.hasNext()) oldResults.next() else windowStart

    do {
      val result = findManager.findString(fullText, startingFrom, findModel)

      if (!editor.foldingModel.isOffsetCollapsed(result.startOffset) &&
        result.startOffset <= windowEnd)
        indicesToCheck.add(result.startOffset)

      startingFrom = nextSite(oldResults, result)
    } while (result.isStringFound && result.startOffset < windowEnd)

    return indicesToCheck
  }

  private fun nextSite(oldResults: Iterator<Int>, result: FindResult): Int {
    while (oldResults.hasNext()) {
      val startingFrom = oldResults.next()
      if (startingFrom >= result.endOffset)
        return startingFrom
    }

    return result.endOffset
  }

  /**
   * Builds a map of all existing bigrams, starting from the index of the
   * last character in the search results. Simultaneously builds a map of all
   * available tags, by removing any used bigrams after each search result, and
   * prior to end of a word (ie. a contiguous group of letters/digits).
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
    var (front, back) = Pair(index, index)
    while (0 < front && document[front - 1].isLetterOrDigit())
      front--

    while (back < document.length && document[back].isLetterOrDigit())
      back++

    return Pair(front, back)
  }

  /**
   * Maps tags to search results. Tags *must* have the following properties:
   *
   * 1. A tag must not equal *any* bigrams on the screen.
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
    val newTagMap: BiMap<String, Int> = HashBiMap.create()
    if (query.isEmpty())
      return newTagMap

    val tags = LinkedHashSet<String>(unseen1grams)
    fun hasNearbyTag(index: Int): Boolean {
      val (wordStart, wordEnd) = getWordBounds(index)
      val left = Math.max(wordStart, index - 2)
      val right = Math.min(wordEnd, index + 2)
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
      val remainder = document.subSequence(index, right)

      val (matching, nonMatching) = tags.partition { tag ->
        remainder.all { letter ->
          //Prevents "...a[IJ]...ij..." ij
          !digraphs.containsKey("$letter${tag[0]}") &&
            //Prevents "...a[IJ]...i[JX]..." ij
            !newTagMap.contains("$letter${tag[0]}") &&
            //Prevents "...r[BK]iv...r[VB]in..."  rivb
            !newTagMap.keys.any { it[0] == letter && it.last() == tag[0] } &&
            //Prevents "...i[JX]...i[IJ]..." ij
            !(letter == tag[0] && newTagMap.keys.any { it[0] == tag.last() })
        }
      }

      val tag = matching.firstOrNull()

      if (tag == null) {
        val word = document.subSequence(left, right)
        println("\"$word\" rejected: " + nonMatching.joinToString(","))
        println("No remaining tags could be assigned to word: \"$word\"")
        return
      }

      val choosenTag = tagMap.inverse().getOrElse(index, { tag })!!
      newTagMap[choosenTag] = index
      if (choosenTag.length == 1) {
        //Prevents "...a[b]...z[b]..."
        //Prevents "...a[b]...z[bc]..."
        tags.removeAll { choosenTag[0] == it.last() }
      } else {
        //Prevents "...a[bc]...z[b]..."
        tags.remove(choosenTag[0].toString())
        //Prevents "...a[bc]...ab[c]..."
        tags.remove(choosenTag[1].toString())
        //Prevents "...a[bc]...z[bc]..."
        tags.remove(choosenTag)
      }
    }

    if (query.isNotEmpty()) {
      if (2 <= query.length) {
        val last2: String = query.substring(query.length - 2)
        val last2Index = tagMap[last2]
        if (last2Index != null) {
          newTagMap[last2] = last2Index
          return newTagMap
        }
      } else {
        val lastChar: String = query.last().toString()
        val lastCharIndex = tagMap[lastChar]
        if (lastCharIndex != null) {
          newTagMap[lastChar] = lastCharIndex
          return newTagMap
        }
      }
    }

    // Add pre-existing tags where search string and tag are intermingled
    for (entry in tagMap) {
      if (query == entry.key || query.last() == entry.key.first()) {
        newTagMap[entry.key] = entry.value
        tags.remove(entry.key[0].toString())
      }
    }

    unseen2grams.sortedWith(compareBy(
      // Least frequent first-character comes first
      { digraphs[it[0].toString()].orEmpty().size },
      // Adjacent keys come before non-adjacent keys
      { !adjacent[it[0]]!!.contains(it.last()) },
      // Rotate to remove "clumps" (ie. AA, AB, AC => AA BA CA)
      String::last,
      // Minimze the distance between tag characters
      { nearby[it[0]]!!.indexOf(it.last()) }
    )).forEach { tag ->
      tags.remove(tag[0].toString())
      tags.add(tag)
    }

    val remainingSites = digraphs.asMap().entries.filter {
      it.key.first().isLetterOrDigit() || query.isNotEmpty()
    }.sortedBy { it.value.size }.flatMap { it.value }.sortedBy {
      // Ensure that the first letter of a word is prioritized for tagging
      document[Math.max(0, it - 1)].isLetterOrDigit()
    }.iterator()

    if (!findModel.isRegularExpressions || newTagMap.isEmpty())
      while (remainingSites.hasNext() && tags.isNotEmpty())
        tryToAssignTagToIndex(remainingSites.next())

    return newTagMap
  }

  fun findPattern(text: Pattern) {
    reset()
    findModel.isRegularExpressions = true
    find(text.pattern, Pattern.REGEX_PREFIX)
  }

  fun plotJumpLocations(): List<JumpInfo> {
    return tagMap.values.map {
      JumpInfo(tagMap.inverse()[it]!!, query, it, editor)
    }.sortedBy { it.index }
  }

  fun reset() {
    findModel.isRegularExpressions = false
    findModel.stringToFind = ""
    sitesToCheck = listOf<Int>()
    digraphs.clear()
    tagMap.clear()
    query = ""
    unseen1grams.clear()
    unseen2grams.clear()
    jumpLocations = emptyList()
  }
}