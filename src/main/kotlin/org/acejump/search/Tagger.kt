package org.acejump.search

import com.google.common.collect.HashBiMap
import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import org.acejump.ExternalUsage
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries
import org.acejump.immutableText
import org.acejump.input.KeyLayoutCache.allPossibleTags
import org.acejump.isWordPart
import org.acejump.matchesAt
import org.acejump.view.Tag
import java.util.AbstractMap.SimpleImmutableEntry
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Assigns tags to search occurrences, updates them when the search query changes, and requests a jump if the search query matches a tag.
 */
internal class Tagger(private val editor: Editor) {
  private var tagMap = HashBiMap.create<String, Int>()
  
  @ExternalUsage
  val tags
    get() = tagMap.map { SimpleImmutableEntry(it.key, it.value) }.sortedBy { it.value }
    
  /**
   * Removes all markers, allowing them to be regenerated from scratch.
   */
  fun unmark() {
    tagMap = HashBiMap.create()
  }
  
  /**
   * Assigns tags to as many results as possible, keeping previously assigned tags. Returns a [TaggingResult.Jump] if the current search
   * query matches any existing tag and we should jump to it and end the session, or [TaggingResult.Mark] to continue the session with
   * updated tag markers.
   *
   * Note that the [results] collection will be mutated.
   */
  fun markOrJump(query: SearchQuery, results: IntList): TaggingResult {
    val isRegex = query is SearchQuery.RegularExpression
    val queryText = if (isRegex) " ${query.rawText}" else query.rawText[0] + query.rawText.drop(1).toLowerCase()
    
    val availableTags = allPossibleTags.filter { !queryText.endsWith(it[0]) && it !in tagMap }
    
    if (!isRegex) {
      for (entry in tagMap.entries) {
        if (entry solves queryText) {
          return TaggingResult.Jump(entry.value)
        }
      }
      
      if (queryText.length == 1) {
        removeResultsWithOverlappingTags(results)
      }
    }
    
    if (!isRegex || tagMap.isEmpty()) {
      tagMap = assignTagsAndMerge(results, availableTags, query, queryText)
    }
    
    return TaggingResult.Mark(createTagMarkers(results, query.rawText.ifEmpty { null }))
  }
  
  /**
   * Assigns as many unassigned tags as possible, and merges them with the existing compatible tags.
   */
  private fun assignTagsAndMerge(results: IntList, availableTags: List<String>, query: SearchQuery, queryText: String): HashBiMap<String, Int> {
    val cache = EditorOffsetCache.new()
    
    results.sort { a, b ->
      val aIsVisible = StandardBoundaries.VISIBLE_ON_SCREEN.isOffsetInside(editor, a, cache)
      val bIsVisible = StandardBoundaries.VISIBLE_ON_SCREEN.isOffsetInside(editor, b, cache)
      
      when {
        aIsVisible && !bIsVisible -> -1
        bIsVisible && !aIsVisible -> 1
        else                      -> 0
      }
    }
    
    val allAssignedTags = mutableMapOf<String, Int>()
    val oldCompatibleTags = tagMap.filter { isTagCompatibleWithQuery(it.key, it.value, queryText) || it.value in results }
    val vacantResults: IntList
    
    if (oldCompatibleTags.isEmpty()) {
      vacantResults = results
    }
    else {
      vacantResults = IntArrayList()
      
      val iter = results.iterator()
      while (iter.hasNext()) {
        val offset = iter.nextInt()
    
        if (offset !in oldCompatibleTags.values) {
          vacantResults.add(offset)
        }
      }
    }
    
    allAssignedTags.putAll(oldCompatibleTags)
    allAssignedTags.putAll(Solver.solve(editor, query, vacantResults, results, availableTags, cache))
    
    return allAssignedTags.mapKeysTo(HashBiMap.create(allAssignedTags.size)) { (tag, _) ->
      // Avoid matching query - will trigger a jump.
      // TODO: lift this constraint.
      val queryEndsWith = queryText.endsWith(tag[0]) || queryText.endsWith(tag)
      
      if (!queryEndsWith && canShortenTag(tag, allAssignedTags))
        tag[0].toString()
      else
        tag
    }
  }
  
  private infix fun Map.Entry<String, Int>.solves(query: String): Boolean {
    return query.endsWith(key, true) && isTagCompatibleWithQuery(key, value, query)
  }
  
  private fun isTagCompatibleWithQuery(tag: String, offset: Int, query: String): Boolean {
    return editor.immutableText.matchesAt(offset, getPlaintextPortion(query, tag), ignoreCase = true)
  }
  
  fun isQueryCompatibleWithTagAt(query: String, offset: Int): Boolean {
    return tagMap.inverse()[offset].let { it != null && isTagCompatibleWithQuery(it, offset, query) }
  }
  
  fun canQueryMatchAnyTag(query: String): Boolean {
    return tagMap.any { (tag, offset) ->
      val tagPortion = getTagPortion(query, tag)
      tagPortion.isNotEmpty() && tag.startsWith(tagPortion, ignoreCase = true) && isTagCompatibleWithQuery(tag, offset, query)
    }
  }
  
  private fun removeResultsWithOverlappingTags(results: IntList) {
    val iter = results.iterator()
    val chars = editor.immutableText
    
    while (iter.hasNext()) {
      if (!chars.canTagWithoutOverlap(iter.nextInt())) {
        iter.remove() // Very uncommon, so slow removal is fine.
      }
    }
  }
  
  private fun createTagMarkers(results: IntList, literalQueryText: String?): List<Tag> {
    val tagMapInv = tagMap.inverse()
    return results.mapNotNull { index -> tagMapInv[index]?.let { tag -> Tag.create(editor, tag, index, literalQueryText) } }
  }
  
  private companion object {
    private fun CharSequence.canTagWithoutOverlap(loc: Int) = when {
      loc - 1 < 0                                                                             -> true
      loc + 1 >= length                                                                       -> true
      this[loc] isUnlike this[loc - 1]                                                        -> true
      this[loc] isUnlike this[loc + 1]                                                        -> true
      this[loc] != this[loc - 1]                                                              -> true
      this[loc] != this[loc + 1]                                                              -> true
      this[loc + 1] == '\r' || this[loc + 1] == '\n'                                          -> true
      this[loc - 1] == this[loc] && this[loc] == this[loc + 1]                                -> false
      this[loc + 1].isWhitespace() && this[(loc + 2).coerceAtMost(length - 1)].isWhitespace() -> true
      else                                                                                    -> false
    }
    
    private infix fun Char.isUnlike(other: Char): Boolean {
      return this.isWordPart xor other.isWordPart || this.isWhitespace() xor other.isWhitespace()
    }
    
    private fun getPlaintextPortion(query: String, tag: String) = when {
      query.endsWith(tag, true)         -> query.dropLast(tag.length)
      query.endsWith(tag.first(), true) -> query.dropLast(1)
      else                              -> query
    }
  
    private fun getTagPortion(query: String, tag: String) = when {
      query.endsWith(tag, true)         -> query.takeLast(tag.length)
      query.endsWith(tag.first(), true) -> query.takeLast(1)
      else                              -> ""
    }
    
    private fun canShortenTag(tag: String, tagMap: Map<String, Int>): Boolean {
      for (other in tagMap.keys) {
        if (tag != other && tag[0] == other[0]) {
          return false
        }
      }
      
      return true
    }
  }
}
