package com.johnlindquist.acejump.label

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.johnlindquist.acejump.search.getLineEndOffset
import com.johnlindquist.acejump.search.getView
import com.johnlindquist.acejump.search.wordBounds
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import java.lang.Math.max
import java.lang.Math.min
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
      var (left, right) = editorText.wordBounds(idx)
      editor.run {
        right = (right + 3).coerceAtMost(getLineEndOffset(
          //Always include the trailing character
          offsetToLogicalPosition(right).line, true))
      }

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

  private var leastFlexibleTags: List<MutableMap.MutableEntry<String, MutableList<Int>>> = listOf()

  private val tagsStats: MutableMap<String, MutableList<Int>> = hashMapOf()

  fun solve(results: Set<Int>, tags: Set<String>): BiMap<String, Int> {
    newTags = HashBiMap.create()
    bigrams = tags.toMutableSet()
    tagsStats.clear()

    strings = HashSet(results.map { getWordFragments(it) }.flatten())

    val timeElapsed = measureTimeMillis {
      results.forEach { site ->
        val byLetter = bigrams.groupBy { it[0] }
        val tagsForSite = byLetter.keys.filter { site isCompatibleWithTag it }
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
    var right = editorText.wordBounds(site).second
    editor.run {
      right = (right + 3).coerceAtMost(getLineEndOffset(
        offsetToLogicalPosition(right).line, true))
    }

    return ((site + Tagger.query.length).coerceAtMost(right)..right)
      .map { editorText.substring(site, it) }
  }
}