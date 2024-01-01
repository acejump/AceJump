package org.acejump.search

import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries.VISIBLE_ON_SCREEN
import org.acejump.config.AceConfig
import org.acejump.immutableText
import org.acejump.input.KeyLayoutCache
import org.acejump.isWordPart
import org.acejump.wordEndPlus
import java.util.IdentityHashMap
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

internal class Solver private constructor(
  private val editorPriority: List<Editor>,
  private val queryLength: Int,
  private val newResults: Map<Editor, IntList>,
  private val allResults: Map<Editor, IntList>
) {
  companion object {
    fun solve(
      editorPriority: List<Editor>,
      query: SearchQuery,
      newResults: Map<Editor, IntList>,
      allResults: Map<Editor, IntList>,
      tags: List<String>,
      caches: Map<Editor, EditorOffsetCache>
    ): Map<String, Tag> =
      Solver(editorPriority, max(1, query.rawText.length), newResults, allResults)
        .map(tags, caches)
  }

  private var newTags = HashMap<String, Tag>(KeyLayoutCache.allPossibleTags.size)
  private val newTagIndices = newResults.keys.associateWith { IntOpenHashSet() }

  private var allWordFragments =
    HashSet<String>(allResults.values.sumOf(IntList::size)).apply {
      for ((editor, offsets) in allResults) {
        val iter = offsets.iterator()
        while (iter.hasNext()) forEachWordFragment(editor, iter.nextInt()) { add(it) }
      }
    }

  fun map(availableTags: List<String>, caches: Map<Editor, EditorOffsetCache>): Map<String, Tag> {
    val eligibleSitesByTag = HashMap<String, MutableList<Tag>>(100)
    val tagsByFirstLetter = availableTags.groupBy { it[0] }
    
    for ((editor, offsets) in newResults) {
      val iter = offsets.iterator()
      while (iter.hasNext()) {
        val site = iter.nextInt()
        
        if (editor.foldingModel.isOffsetCollapsed(site)) {
          continue
        }
        
        for ((firstLetter, tags) in tagsByFirstLetter.entries) {
          if (canTagBeginWithChar(editor, site, firstLetter)) {
            for (tag in tags) {
              eligibleSitesByTag.getOrPut(tag, ::mutableListOf).add(Tag(editor, site))
            }
          }
        }
      }
    }
    
    val matchingSites = HashMap<MutableList<Tag>, MutableList<Tag>>()
    // Keys are guaranteed to be from a single collection.
    val matchingSitesAsArrays = IdentityHashMap<String, MutableList<Tag>>()
    
    val siteOrder = siteOrder(caches)
    val tagOrder = KeyLayoutCache.tagOrder
      .thenComparingInt { eligibleSitesByTag.getValue(it).size }
      .thenBy(AceConfig.layout.priority(String::last))

    val sortedTags = eligibleSitesByTag.keys.toMutableList().apply {
      sortWith(tagOrder)
    }

    for ((mark, tags) in eligibleSitesByTag.entries) {
      matchingSitesAsArrays[mark] = matchingSites.getOrPut(tags) {
        tags.toMutableList().apply { sortWith(siteOrder) }
      }
    }

    var totalAssigned = 0
    val totalResults = newResults.values.sumOf(IntList::size)
    
    for (tag in sortedTags) {
      if (totalAssigned == totalResults) {
        break
      }

      if (tryToAssignTag(tag, matchingSitesAsArrays.getValue(tag))) {
        totalAssigned++
      }
    }

    return newTags
  }

  private fun tryToAssignTag(mark: String, tags: List<Tag>): Boolean {
    if (newTags.containsKey(mark)) return false

    val tag = tags.firstOrNull { it.offset !in newTagIndices.getValue(it.editor) } ?: return false
    @Suppress("ReplacePutWithAssignment")
    newTags.put(mark, tag)
    newTagIndices.getValue(tag.editor).add(tag.offset)
    return true
  }

  private fun siteOrder(caches: Map<Editor, EditorOffsetCache>) = Comparator<Tag> { a, b ->
    val aEditor = a.editor
    val bEditor = b.editor
  
    if (aEditor !== bEditor) {
      val aEditorIndex = editorPriority.indexOf(aEditor)
      val bEditorIndex = editorPriority.indexOf(bEditor)
      // For multiple editors, prioritize them based on the provided order.
      return@Comparator if (aEditorIndex < bEditorIndex) -1 else 1
    }
  
    val aIsVisible = VISIBLE_ON_SCREEN.isOffsetInside(aEditor, a.offset, caches.getValue(aEditor))
    val bIsVisible = VISIBLE_ON_SCREEN.isOffsetInside(bEditor, b.offset, caches.getValue(bEditor))
    if (aIsVisible != bIsVisible) {
      // Sites in immediate view should come first.
      return@Comparator if (aIsVisible) -1 else 1
    }
  
    val aIsNotWordStart = aEditor.immutableText[max(0, a.offset - 1)].isWordPart
    val bIsNotWordStart = bEditor.immutableText[max(0, b.offset - 1)].isWordPart
    if (aIsNotWordStart != bIsNotWordStart) {
      // Ensure that the first letter of a word is prioritized for tagging.
      return@Comparator if (bIsNotWordStart) -1 else 1
    }
  
    when {
      a.offset < b.offset -> -1
      a.offset > b.offset -> 1
      else                -> 0
    }
  }

  private fun canTagBeginWithChar(editor: Editor, site: Int, char: Char): Boolean {
    if (char.toString() in allWordFragments) return false

    forEachWordFragment(editor, site) { if (it + char in allWordFragments) return false }
    return true
  }

  private inline fun forEachWordFragment(editor: Editor, site: Int, callback: (String) -> Unit) {
    val chars = editor.immutableText
    val left = max(0, site + queryLength - 1)
    val right = chars.wordEndPlus(site)
    if (right >= chars.length) {
      return
    }
  
    val builder = StringBuilder(1 + right - left)

    for (i in left..right) {
      builder.append(chars[i].lowercase())
      callback(builder.toString())
    }
  }
}
