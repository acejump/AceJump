package com.johnlindquist.acejump.label

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.johnlindquist.acejump.search.getLineEndOffset
import com.johnlindquist.acejump.search.getView
import com.johnlindquist.acejump.search.wordBounds
import com.johnlindquist.acejump.search.wordBoundsPlus
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import java.lang.Math.max
import java.lang.Math.min
import kotlin.collections.MutableMap.MutableEntry
import kotlin.collections.set
import kotlin.system.measureTimeMillis

/**
 * Solves the tag assignment problem. The tag assignment problem can be stated
 * thusly: Given a set of indices I in document D, and a set of two-character
 * tags T, find a mapping of T->I such that D[i_n..i_(n+1)] + t_n[0] is not in
 * {D[i_j..(i_(j+1) + 1)]}. Maximize |T->I|.
 */

object Solver {
  private var bigrams: MutableSet<String> = linkedSetOf()
  private var newTags: BiMap<String, Int> = HashBiMap.create()
  private var strings: Set<String> = hashSetOf()

  /**
   * Iterates through the remaining available tags, until we find one that
   * matches our criteria, i.e. does not collide with an existing tag or
   * plaintext string. To have the desired behavior, this has a surprising
   * number of edge cases that must explicitly prevented.
   *
   * @param idx the index which a tag is to be assigned
   */

  private fun tryToAssignTag(tag: String): Boolean {
    if (tagsStats[tag]!!.isEmpty()) return false
    val idx = tagsStats[tag]!!.firstOrNull { idx ->
      val (left, right) = editorText.wordBoundsPlus(idx)

      fun hasNearbyTag(index: Int) =
        Pair(max(left, index - 2), min(right, index + 2))
          .run { (first..second).any { newTags.containsValue(it) } }

      !hasNearbyTag(idx) && !newTags.containsValue(idx)
    } ?: return true

    newTags[tag] = idx
    return true
  }

  /**
   * Sorts jump targets to determine which positions get first choice for tags,
   * by taking into account the structure of the surrounding text. For example,
   * if the jump target is the first letter in a word, it is advantageous to
   * prioritize this location (in case we run out of tags), since the user is
   * more likely to target words by their leading character than not.
   */

  private fun MutableList<Int>.sortVaidJumpTargets() =
    sortWith(compareBy(
      // Sites in immediate view should come first
      { it !in editor.getView() },
      // Ensure that the first letter of a word is prioritized for tagging
      { editorText[max(0, it - 1)].isLetterOrDigit() }))

  private var leastFlexibleTags: List<MutableEntry<String, MutableList<Int>>> = listOf()
  private val tagsStats: MutableMap<String, MutableList<Int>> = hashMapOf()

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

  fun solve(results: Set<Int>, tags: Set<String>): BiMap<String, Int> {
    newTags = HashBiMap.create()
    bigrams = tags.toMutableSet()
    tagsStats.clear()

    strings = HashSet(results.map { getWordFragments(it) }.flatten())

    val timeElapsed = measureTimeMillis {
      results.forEach { site ->
        val byLetter = bigrams.groupBy { it[0] }
        val tagsForSite = byLetter.keys.filter { letter ->
          val compat = site isCompatibleWithTag letter
        compat
        }
        tagsForSite.forEach { letter ->
          byLetter[letter]!!.forEach { tag ->
            tagsStats.put(tag, tagsStats.getOrDefault(tag,
              mutableListOf()).apply { add(site) })
          }
        }
      }

      tagsStats.values.forEach { it.sortVaidJumpTargets() }
      leastFlexibleTags = tagsStats.entries.sortedBy { it.value.size }
      leastFlexibleTags.map { it.key }.forEach { tryToAssignTag(it) }
    }

    if (tagsStats.any { it.value.isEmpty() }) Tagger.full = false

    println("results size: ${results.size}")
    println("newTags size: ${newTags.size}")
    println("Time elapsed: $timeElapsed")

    return newTags
  }

  // Provides a way to short-circuit the full text search if a match is found
  private operator fun String.contains(key: String) =
    Tagger.textMatches.any { regionMatches(it, key, 0, key.length) }

  /**
   * Returns true IFF the tag, when inserted at any position in the word, could
   * match an existing substring elsewhere in the editor text. We should never
   * use a tag which can be partly completed by typing plaintext, where the tag
   * is the receiver, the tag index is the leftIndex, and rightIndex is the last
   * character we care about (this is usually the last letter of the same word).
   *
   * @param leftIndex index where a tag is to be used
   * @param rightIndex index of last character (ie. end of the word)
   */

  private infix fun Int.isCompatibleWithTag(tag: Char) =
    getWordFragments(this).map { it + tag }.none { it in strings }

  private fun getWordFragments(site: Int): List<String> {
    val left = site + Tagger.query.length - 1
    val right = editorText.wordBoundsPlus(site).second

    return (left..right).map { editorText.substring(left, it) }
  }
}