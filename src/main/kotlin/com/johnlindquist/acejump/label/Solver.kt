package com.johnlindquist.acejump.label

import com.google.common.collect.HashBiMap
import com.google.common.collect.Multimaps
import com.google.common.collect.Ordering
import com.google.common.collect.TreeMultimap
import com.intellij.openapi.diagnostic.Logger
import com.johnlindquist.acejump.label.Pattern.Companion.defaultOrder
import com.johnlindquist.acejump.search.wordBoundsPlus
import com.johnlindquist.acejump.view.Model.editorText
import com.johnlindquist.acejump.view.Model.viewBounds
import java.lang.Math.max
import kotlin.collections.set
import kotlin.system.measureTimeMillis

/**
 * Solves the Tag Assignment Problem. The tag assignment problem can be stated
 * thusly: Given a set of indices I in document d, and a set of tags T, find a
 * bijection f: T*⊂T → I*⊂I s.t. d[i..k] + t ∉ d[i'..(k + |t|)], ∀ i' ∈ I\{i},
 * ∀ k ∈ [i, |d|], where t ∈ T, i ∈ I. Maximize |T*|. Can be relaxed to t=t[0]
 * and ∀ k ∈ [i, i+K] for some fixed K, in most naturally appearing documents.
 * 
 * More concretely, tags are typically two-character strings containing alpha-
 * numeric symbols. Documents are plaintext documents. Indices are produced by
 * a search query, so the preceeding N characters of every index i in document
 * d are identical. For characters proceeding d[i], all bets are off. We might
 * assume that P(d[i]|d[i-1]) has some stucture for d~D. At the end of the day
 * we would like to have an efficient TAP algorithm. (Certain approximations 
 * may be considered, however no string d[i..k] + t may ever be contained in 
 * the set of strings d[i'..j] or there will be tag collisions in practice.)
 */

object Solver {
  private val logger = Logger.getInstance(Solver::class.java)
  private var newTags: MutableMap<String, Int> = HashBiMap.create()
  private var strings: Set<String> = hashSetOf()

  /**
   * Iterates through the remaining available tags, until we find one that
   * matches our criteria, i.e. does not collide with an existing tag or
   * plaintext string. To have the desired behavior, this has a surprising
   * number of edge cases that must explicitly prevented.
   *
   * @param tag the tag string which is to be assigned
   * @param sites potential indices where a tag may be assigned
   */

  private fun tryToAssignTag(tag: String, sites: Collection<Int>): Boolean {
    if (newTags.containsKey(tag)) return false
    val index = sites.firstOrNull { !newTags.containsValue(it) } ?: return false
    newTags[tag] = index
    return true
  }

  private val tagOrder = defaultOrder
    .thenBy { eligibleSitesByTag[it].size }
    .thenBy { Pattern.priority(it.last()) }

  /**
   * Sorts jump targets to determine which positions get first choice for tags,
   * by taking into account the structure of the surrounding text. For example,
   * if the jump target is the first letter in a word, it is advantageous to
   * prioritize this location (in case we run out of tags), since the user is
   * more likely to target words by their leading character.
   */

  private val siteOrder: Comparator<Int> = compareBy(
    // Sites in immediate view should come first
    { it !in viewBounds },
    // Ensure that the first letter of a word is prioritized for tagging
    { editorText[max(0, it - 1)].isLetterOrDigit() },
    { it })

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
   * @see isCompatibleWithTagChar This defines how tags may be assigned to sites.
   */

  private val eligibleSitesByTag = Multimaps.synchronizedSetMultimap(
    TreeMultimap.create<String, Int>(Ordering.natural(), siteOrder))

  /**
   * Maps tags to search results according to the following constraints.
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

  fun solve(results: Collection<Int>, tags: Set<String>): Map<String, Int> {
    eligibleSitesByTag.clear()
    newTags = HashMap(Pattern.NUM_TAGS)
    strings = HashSet(results.map { getWordFragments(it) }.flatten())

    var totalAssigned = 0
    var timeAssigned = 0L
    val timeElapsed = measureTimeMillis {
      val tagsByFirstLetter = tags.groupBy { it[0] }
      results.parallelStream().forEach { site ->
        val compatibleTags = tagsByFirstLetter.getTagsCompatibleWith(site)
        compatibleTags.forEach { tag -> eligibleSitesByTag.put(tag, site) }
      }

      val sortedTags = eligibleSitesByTag.keySet().sortedWith(tagOrder)

      timeAssigned = measureTimeMillis {
        for (tagString in sortedTags) {
          val eligibleSites = eligibleSitesByTag[tagString]
          if (totalAssigned == results.size) break
          else if (tryToAssignTag(tagString, eligibleSites)) totalAssigned++
        }
      }
    }

    logger.run {
      info("results size: ${results.size}")
      info("newTags size: ${newTags.size}")
      info("Time elapsed: $timeElapsed ms")
      info("Total assign: $totalAssigned")
      info("Completed in: $timeAssigned ms")
    }

    return newTags
  }

  private fun Map<Char, List<String>>.getTagsCompatibleWith(site: Int) =
    entries.flatMap { (firstLetter, tags) ->
      if (site isCompatibleWithTagChar firstLetter) tags else emptyList()
    }

  /**
   * Returns true IFF the tag, when inserted at any position in the word, could
   * match an existing substring elsewhere in the editor text. We should never
   * use a tag which can be partly completed by typing plaintext.
   */

  private infix fun Int.isCompatibleWithTagChar(char: Char) =
    getWordFragments(this).map { it + char }.none { it in strings }

  private fun getWordFragments(site: Int): List<String> {
    val left = max(0, site + Tagger.query.length - 1)
    val right = editorText.wordBoundsPlus(site).second

    return (left..right).map { editorText.substring(left, it) }
  }
}
