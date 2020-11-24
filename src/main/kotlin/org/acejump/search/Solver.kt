package org.acejump.search

import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.*
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries
import org.acejump.config.AceConfig
import org.acejump.immutableText
import org.acejump.input.KeyLayoutCache
import org.acejump.isWordPart
import org.acejump.wordEndPlus
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.max

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
 * (which typically occurs when the user searches for a common character within
 * a long document), then we attempt to maximize the number of tags assigned to
 * document indices. The key is, all tags must be assigned as soon as possible,
 * i.e. as soon as the first character is received or whenever the user ceases
 * typing (at the very latest). Once assigned, a visible tag must never change
 * at any time during the selection process, so as not to confuse the user.
 */

internal class Solver private constructor(private val editor: Editor, private val queryLength: Int, private val results: IntList) {
  companion object {
    fun solve(editor: Editor, query: SearchQuery, results: IntList, tags: List<String>, cache: EditorOffsetCache): Map<String, Int> {
      return Solver(editor, max(1, query.rawText.length), results).map(tags, cache)
    }
  }
  
  private var newTags = Object2IntOpenHashMap<String>(KeyLayoutCache.allPossibleTags.size)
  private val newTagIndices = IntOpenHashSet()
  
  private var allWordFragments = HashSet<String>(results.size).apply {
    val iter = results.iterator()
    while (iter.hasNext()) {
      forEachWordFragment(iter.nextInt()) { add(it) }
    }
  }
  
  fun map(availableTags: List<String>, cache: EditorOffsetCache): Map<String, Int> {
    val eligibleSitesByTag = HashMap<String, IntList>(100)
    val tagsByFirstLetter = availableTags.groupBy { it[0] }
    
    val iter = results.iterator()
    while (iter.hasNext()) {
      val site = iter.nextInt()
      
      for ((firstLetter, tags) in tagsByFirstLetter.entries) {
        if (canTagBeginWithChar(site, firstLetter)) {
          for (tag in tags) {
            eligibleSitesByTag.getOrPut(tag) { IntArrayList(10) }.add(site)
          }
        }
      }
    }
    
    val matchingSites = HashMap<IntList, IntArray>()
    val matchingSitesAsArrays = IdentityHashMap<String, IntArray>() // Keys are guaranteed to be from a single collection.
    
    val siteOrder = siteOrder(cache)
    val tagOrder = KeyLayoutCache.tagOrder
      .thenComparingInt { eligibleSitesByTag.getValue(it).size }
      .thenBy(AceConfig.layout.priority(String::last))
    
    val sortedTags = eligibleSitesByTag.keys.toMutableList().apply {
      sortWith(tagOrder)
    }
    
    for ((key, value) in eligibleSitesByTag.entries) {
      matchingSitesAsArrays[key] = matchingSites.getOrPut(value) {
        value.toIntArray().apply { IntArrays.mergeSort(this, siteOrder) }
      }
    }
    
    var totalAssigned = 0
    
    for (tag in sortedTags) {
      if (totalAssigned == results.size) {
        break
      }
      
      if (tryToAssignTag(tag, matchingSitesAsArrays.getValue(tag))) {
        totalAssigned++
      }
    }
    
    return newTags
  }
  
  private fun tryToAssignTag(tag: String, sites: IntArray): Boolean {
    if (newTags.containsKey(tag)) {
      return false
    }
    
    val index = sites.firstOrNull { it !in newTagIndices } ?: return false
    
    @Suppress("ReplacePutWithAssignment")
    newTags.put(tag, index)
    newTagIndices.add(index)
    return true
  }
  
  private fun siteOrder(cache: EditorOffsetCache) = IntComparator { a, b ->
    val aIsVisible = StandardBoundaries.VISIBLE_ON_SCREEN.isOffsetInside(editor, a, cache)
    val bIsVisible = StandardBoundaries.VISIBLE_ON_SCREEN.isOffsetInside(editor, b, cache)
    
    if (aIsVisible != bIsVisible) {
      // Sites in immediate view should come first.
      return@IntComparator if (aIsVisible) -1 else 1
    }
    
    val chars = editor.immutableText
    val aIsNotWordStart = chars[max(0, a - 1)].isWordPart
    val bIsNotWordStart = chars[max(0, b - 1)].isWordPart
    
    if (aIsNotWordStart != bIsNotWordStart) {
      // Ensure that the first letter of a word is prioritized for tagging.
      return@IntComparator if (bIsNotWordStart) -1 else 1
    }
    
    when {
      a < b -> -1
      a > b -> 1
      else  -> 0
    }
  }
  
  private fun canTagBeginWithChar(site: Int, char: Char): Boolean {
    if (char.toString() in allWordFragments) {
      return false
    }
    
    forEachWordFragment(site) {
      if (it + char in allWordFragments) {
        return false
      }
    }
    
    return true
  }
  
  private inline fun forEachWordFragment(site: Int, callback: (String) -> Unit) {
    val chars = editor.immutableText
    val left = max(0, site + queryLength - 1)
    val right = chars.wordEndPlus(site)
    
    val builder = StringBuilder(1 + right - left)
    
    for (i in left..right) {
      builder.append(chars[i].toLowerCase())
      callback(builder.toString())
    }
  }
}
