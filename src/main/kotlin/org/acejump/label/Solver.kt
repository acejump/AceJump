package org.acejump.label

import com.google.common.collect.*
import com.intellij.openapi.diagnostic.Logger
import org.acejump.config.AceConfig
import org.acejump.label.Pattern.Companion.defaultTagOrder
import org.acejump.search.wordBoundsPlus
import kotlin.collections.set
import kotlin.math.max
import kotlin.system.measureTimeMillis

/*
 * Solves the Tag Assignment Problem. The tag assignment problem can be stated
 * thusly: Given a set of indices I in document d, and a set of tags T, find a
 * bijection f: T*⊂T → I*⊂I s.t. d[i..k] + t ∉ d[i'..(k + |t|)], ∀ i' ∈ I\{i},
 * ∀ k ∈ (i, |d|-|t|], where t ∈ T, i ∈ I. Maximize |I*|. This can be relaxed
 * to t=t[0] and ∀ k ∈ (i, i+K] for some fixed K, in most natural documents.
 *
 * More concretely, tags are typically two-character strings containing alpha-
 * numeric symbols. Documents are plaintext files. Indices are produced by a
 * search query of length N, i.e. the preceding N characters of every index i in
 * document d are identical. For characters proceeding d[i], all bets are off.
 * We can assume that P(d[i]|d[i-1]) has some structure for d~D. Ultimately, we
 * want a fast algorithm which maximizes the number of tagged document indices.
 *
 * Tags are used by the typist to select indices within a document. To select an
 * index, the typist starts by activating AceJump and searching for a character.
 * As soon as the first character is received, we begin to scan the document for
 * matching locations and assign as many valid tags as possible. When subsequent
 * characters are received, we refine the search results to match either:
 *
 *    1.) The plaintext query alone, or
 *    2.) The concatenation of plaintext query and partial tag
 *
 * The constraint in paragraph no. 1 tries to impose the following criteria:
 *
 *    1.) All valid key sequences will lead to a unique location in the document
 *    2.) All indices in the document will be reachable by a short key sequence
 *
 * If there is an insufficient number of two-character tags to cover every index
 * (which typically occurs for a common character in a long document), then we
 * attempt to maximize the number of tags assigned to document indices. The key
 * is, all tags must be assigned as soon as possible, i.e. as soon as the first
 * character is received or when the typist ceases typing (at the very latest).
 * Once assigned, a visible tag must never change during the selection process.
 */

class Solver(val text: String,
             val query: String,
             val results: Collection<Int>,
             val availableTags: Set<String>,
             val viewBounds: IntRange = 0..text.length) {
  private val logger = Logger.getInstance(Solver::class.java)
  private var newTags: MutableMap<String, Int> = HashMap(Pattern.NUM_TAGS)
  private var strings: Set<String> =
    HashSet(results.map { getWordFragments(it) }.flatten())

  /**
   * Iterates through remaining available tags, until we find one matching our
   * criteria, i.e. does not collide with an existing tag or plaintext string.
   *
   * @param tag the tag string which is to be assigned
   * @param sites potential indices where a tag may be assigned
   */

  private fun tryToAssignTag(tag: String, sites: Collection<Int>): Boolean {
    if (tag in newTags.keys) return false
    val index = sites.firstOrNull { it !in newTags.values } ?: return false
    newTags[tag] = index
    return true
  }

  /**
   * Ensures tag conservation. Most tags prefer to occupy certain sites during
   * assignment, since not all tags may be assigned to all sites. Therefore, we
   * must spend our tag "budget" wisely, in order to cover the most sites with
   * the tags we have at our disposal. We should consider the "most restrictive"
   * tags first, since they have the least chance of being available as further
   * sites are assigned.
   *
   * Tags which are compatible with the fewest sites should have preference for
   * first assignment. Here we ensure that scarce tags are prioritized for their
   * subsequent binding to available sites.
   *
   * @see isCompatibleWithTagChar This defines how tags may be assigned to sites.
   */

  private val tagOrder = defaultTagOrder
    .thenBy { eligibleSitesByTag[it].size }
    .thenBy(AceConfig.layout.priority { it.last() })

  /**
   * Sorts jump targets to determine which positions get first choice for tags,
   * by taking into account the structure of the surrounding text. For example,
   * if the jump target is the first letter in a word, it is advantageous to
   * prioritize this location (in case we run out of tags), since the typist is
   * more likely to target words by their leading character.
   */

  private val siteOrder: Comparator<Int> = compareBy(
    // Sites in immediate view should come first
    { it !in viewBounds },
    // Ensure that the first letter of a word is prioritized for tagging
    { text[max(0, it - 1)].isLetterOrDigit() },
    { it })

  private val eligibleSitesByTag = Multimaps.synchronizedSetMultimap(
    TreeMultimap.create<String, Int>(Ordering.natural(), siteOrder))

  /**
   * Maps tags to search results according to the following constraints.
   *
   * 1. A tag's first letter must not match any letters of the covered word.
   * 2. Once assigned, a tag must never change until it has been selected. *A.
   *
   * Tags *should* have the following properties:
   *
   * A. Should be as short as possible. A tag may be "compacted" later.
   * B. Should prefer keys that are physically closer on a QWERTY keyboard.
   *
   * @return A list of all tags and their corresponding indices
   */

  fun map(): Map<String, Int> {
    var totalAssigned = 0
    var timeAssigned = 0L
    val timeElapsed = measureTimeMillis {
      val tagsByFirstLetter = availableTags.groupBy { it[0] }
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
   * assign a tag which can be partly completed by typing plaintext.
   */

  private infix fun Int.isCompatibleWithTagChar(char: Char) =
    getWordFragments(this).map { it + char }.none { it in strings }

  private fun getWordFragments(site: Int): List<String> {
    val left = max(0, site + query.length - 1)
    val right = text.wordBoundsPlus(site).second

    return (left..right).map { text.substring(left, it).toLowerCase() }
  }
}
