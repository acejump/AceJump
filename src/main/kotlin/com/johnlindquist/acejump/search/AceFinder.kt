package com.johnlindquist.acejump.search

import com.google.common.collect.*
import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.DocumentImpl
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

class AceFinder(val findManager: FindManager, val editor: EditorImpl) {
  val document = editor.document as DocumentImpl
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
    if (key == 0.toChar())
      reset()

    query = text.toLowerCase()
    findModel.stringToFind = text

    val application = ApplicationManager.getApplication()
    application.runReadAction(jump(key))
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
  private fun jump(key: Char): () -> Unit {
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

    return {
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
            jumpTo(JumpInfo(last1, query, tagMap[last1]!!, editor))
          }
        }
      }
    }
  }

  private fun determineJumpLocations(): Collection<JumpInfo> {
    populateNgrams()
    val fullText = document.charsSequence.toString().toLowerCase()
    val sitesToCheck = getSitesInView(fullText)
    val existingDigraphs = makeMap(fullText, sitesToCheck)
    tagMap = compact(mapDigraphs(existingDigraphs))

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
   * Returns a list of idices where the given query string ends, within the
   * current editor screen. These are full indices, ie. are not offset to the
   * first line of the editor window.
   */

  private fun getSitesInView(fullText: String): List<Int> {
    fun getSitesToCheck(window: String): Iterable<Int> {
      if (query.isEmpty())
        return 0..(window.length - 2)

      val indicesToCheck = arrayListOf<Int>()
      var result = findManager.findString(window, 0, findModel)
      while (result.isStringFound) {
        indicesToCheck.add(result.endOffset)
        result = findManager.findString(window, result.endOffset, findModel)
      }

      return indicesToCheck
    }

    val (startIndex, endIndex) = getVisibleRange(editor)
    val window = fullText.substring(startIndex, endIndex)
    return getSitesToCheck(window).map { it + startIndex }
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
      var (p0, p1, p2) = Triple(site - 1, site, site + 1)
      var (c0, c1, c2) = Triple(' ', text[p1], ' ')
      if (0 <= p0)
        c0 = text[p0]
      if(p2 < text.length) {
        c2 = text[p2]
      }

      val origin = p1 - query.length
      stringToIndex.put("$c1", origin)
      stringToIndex.put("$c1$c2", origin)
      unseen1grams.remove("$c1")

      while (c1.isLetterOrDigit() && c2.isLetterOrDigit()) {
        unseen2grams.remove("$c0$c1")
        unseen2grams.remove("$c1$c2")
        p1++; p2++; c1 = text[p1]; c2 = text[p2]
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
    val chars = document.charsSequence
    var (left, right) = Pair(index, index)
    while (1 <= left && chars[left - 1].isLetterOrDigit())
      left--

    while (right + 1 <= chars.length && chars[right + 1].isLetterOrDigit())
      right++

    return Pair(left, right)
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
   * A. Should be as short as possible. A tag may be shortened later.
   * B. Should prefer keys that are physically closer to the last key pressed.
   */

  private fun mapDigraphs(digraphs: Multimap<String, Int>): BiMap<String, Int> {
    val newTagMap: BiMap<String, Int> = HashBiMap.create()
    if (query.isEmpty())
      return newTagMap

    val chars = document.charsSequence
    val unusedNgrams = LinkedHashSet<String>(unseen1grams)
    fun hasNearbyTag(index: Int): Boolean {
      val (left, right) = getWordBounds(index)
      return (left..right).any { newTagMap.containsValue(it) }
    }

    /**
     * Iterates through the remaining available tags, until we find one that
     * matches our criteria, i.e. does not collide with an existing tag or
     * plaintext string. To have the desired behavior, this has a surprising
     * number of edge cases and irregularities that must explicitly prevented.
     */

    fun tryToAssignTagToIndex(index: Int) {
      if (hasNearbyTag(index) || newTagMap.containsValue(index))
        return

      val (left, right) = getWordBounds(index)

      val tag = unusedNgrams.filter { tag ->
        ((left..right).all {
          //Prevents "...a[IJ]...ij..."
          !digraphs.containsKey("${chars[it]}${tag[0]}") &&
            //Prevents "...a[IJ]...i[JX]..."
            !newTagMap.contains("${chars[it]}${tag[0]}") &&
            //Prevents "...i[JX]...i[IJ]..."
            !(chars[it] == tag[0] && !newTagMap.containsKey("${tag.last()}"))
        })
      }.sortedBy { nearby[query.last()]!!.indexOf(it.first()) }.firstOrNull()

      if (tag == null) {
        println("No tags could be assigned to word: " +
          (left..right).map { "${chars[it]}" }.joinToString(""))
        return
      }

      val choosenTag = tagMap.inverse().getOrElse(index, { tag })!!
      newTagMap[choosenTag] = index
      if (choosenTag.length == 1) {
        //Prevents "...a[b]...z[b]..."
        //Prevents "...a[b]...z[bc]..."
        unusedNgrams.removeAll { choosenTag[0] == it.last() }
      } else {
        //Prevents "...a[bc]...z[b]..."
        unusedNgrams.remove(choosenTag[0].toString())
        //Prevents "...a[bc]...ab[c]..."
        unusedNgrams.remove(choosenTag[1].toString())
        //Prevents "...a[bc]...z[bc]..."
        unusedNgrams.remove(choosenTag)
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
        val last1: String = query.substring(query.length - 1)
        val last1Index = tagMap[last1]
        if (last1Index != null) {
          newTagMap[last1] = last1Index
          return newTagMap
        }
      }
    }

    // Add pre-existing tags where search string and tag are intermingled
    for (entry in tagMap) {
      if (query == entry.key || query.last() == entry.key.first()) {
        newTagMap[entry.key] = entry.value
        unusedNgrams.remove(entry.key[0].toString())
      }
    }

    val remaining = digraphs.asMap().entries.sortedBy { it.value.size }
    val tags = unseen2grams.sortedWith(compareBy(
      { digraphs["${it[0]}"].orEmpty().size },
      { !adjacent[it[0]]!!.contains(it[1]) },
      String::last,
      { nearby[it[0]]!!.indexOf(it[1]) }
    )).iterator()
    while (tags.hasNext()) {
      val biGram = tags.next()
      unusedNgrams.remove(biGram[0].toString())
      unusedNgrams.add(biGram)
    }

    val remainingSites = remaining.filter {
      it.key.first().isLetterOrDigit() || query.isNotEmpty()
    }.flatMap { it.value }.listIterator()

    while (remainingSites.hasNext() && unusedNgrams.isNotEmpty())
      tryToAssignTagToIndex(remainingSites.next())

    return newTagMap
  }

  fun findPattern(text: Pattern) {
    findModel.isRegularExpressions = true
    find(text.pattern, 0.toChar())
    findModel.isRegularExpressions = false
  }

  fun plotJumpLocations(): List<JumpInfo> {
    return tagMap.values.map {
      JumpInfo(tagMap.inverse()[it]!!, query, it, editor)
    }
  }

  fun reset() {
    tagMap = HashBiMap.create()
    unseen1grams = linkedSetOf()
    jumpLocations = emptyList()
  }
}