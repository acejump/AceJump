package com.johnlindquist.acejump.label

import com.google.common.collect.*
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.wordBoundsPlus
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
  private var bigrams: MutableSet<String> = LinkedHashSet(Pattern.NUM_TAGS)
  private var newTags: MutableMap<String, Int> = HashMap(Pattern.NUM_TAGS)
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
    if (availableTagsPerSite[tag]!!.isEmpty()) return false
    val index = availableTagsPerSite[tag]!!.firstOrNull { index ->
      val (left, right) = editorText.wordBoundsPlus(index)

      fun hasNearbyTag(index: Int) =
        Pair(max(left, index - 2), min(right, index + 2))
          .run { (first..second).any { newTags.containsValue(it) } }

      !hasNearbyTag(index) && !newTags.containsValue(index)
    } ?: return true

    newTags[tag] = index
    return true
  }

  /**
   * Sorts jump targets to determine which positions get first choice for tags,
   * by taking into account the structure of the surrounding text. For example,
   * if the jump target is the first letter in a word, it is advantageous to
   * prioritize this location (in case we run out of tags), since the user is
   * more likely to target words by their leading character than not.
   */

  val siteOrder: Comparator<Int> = compareBy(
    // Sites in immediate view should come first
    { it !in Finder.viewRange },
    // Ensure that the first letter of a word is prioritized for tagging
    { editorText[max(0, it - 1)].isLetterOrDigit() })

  /**
   * Enforces tag conservation precedence. Tags have certain restrictions during
   * assignment, ie. not all tags may be assigned to all sites. Therefore, we
   * must spend our tag "budget" wisely, in order to cover the most sites with
   * the tags we have at our disposal. We should consider the "most restrictive"
   * tags first, since they have the least chance of being available as more
   * sites are assigned.
   *
   * Tags which are compatible with the fewest sites should have precedence for
   * first assignment. Here we ensure that scarce tags are prioritized for their
   * subsequent binding to available sites.
   *
   * @see isCompatibleWithSite This defines how tags may be assigned to sites.
   */

  val tagOrder: Comparator<String> = Ordering.natural()

  private val availableTagsPerSite = Multimaps.synchronizedSetMultimap(
    TreeMultimap.create<String, Int>(tagOrder, siteOrder))

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

  fun solve(results: Set<Int>, tags: Set<String>): Map<String, Int> {
    newTags = HashMap(Pattern.NUM_TAGS)
    bigrams = tags.toMutableSet()
    availableTagsPerSite.clear()

    strings = HashSet(results.map { getWordFragments(it) }.flatten())

    val timeElapsed = measureTimeMillis {
      results.parallelStream().forEach { site ->
        val firstLetters = bigrams.groupBy { it[0] }
        val tagsForSite = firstLetters.keys.filter { letter ->
          val compat  = site isCompatibleWithTag letter
          if(!compat) println("Site $site rejected $letter")
          compat
        }

        tagsForSite.forEach { letter ->
          firstLetters[letter]!!.forEach { tag ->
            availableTagsPerSite.put(tag, site)
          }
        }
      }

      // Tag conservation precedence is in effect. Scarce tags come first!
      availableTagsPerSite.asMap().entries.sortedBy { it.value.size }
        .map { it.key }.forEach { tryToAssignTag(it) }
    }

    if (availableTagsPerSite.asMap().any { it.value.isEmpty() }) Tagger.full = false

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
    getWordFragments(this).map { it + tag }.none {

      val inS = it in strings

      if(inS) println(it)

      inS
    }

  private fun getWordFragments(site: Int): List<String> {
    val left = site + Tagger.query.length - 1
    val right = editorText.wordBoundsPlus(site).second

    return (left..right).map { editorText.substring(left, it) }
  }
}