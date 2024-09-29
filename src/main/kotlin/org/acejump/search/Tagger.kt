package org.acejump.search

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashBiMap
import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import org.acejump.*
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries.VISIBLE_ON_SCREEN
import org.acejump.input.KeyLayoutCache.allPossibleTags
import org.acejump.view.TagMarker
import java.util.AbstractMap.SimpleImmutableEntry
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.min

/**
 * Assigns tags to search occurrences, updates them when the search query changes, and requests a jump if the search query matches a tag.
 * The ordering of [editors] may be used to prioritize tagging editors earlier in the list in case of conflicts.
 */
internal class Tagger(private val editors: List<Editor>) {
  private var tagMap = HashBiMap.create<String, Tag>()
  val hasTags
    get() = tagMap.isNotEmpty()

  @ExternalUsage
  val tags
    get() = tagMap.map { SimpleImmutableEntry(it.key, it.value) }.sortedBy { it.value.offset }
  
  /**
   * Removes all markers, allowing them to be regenerated from scratch.
   */
  fun unmark() {
    tagMap = HashBiMap.create()
  }

  /**
   * Assigns tags to as many results as possible, keeping previously assigned
   * tags. Returns a [TaggingResult.Jump] if the current search query matches
   * any existing tag and we should jump to it and end the session, or
   * [TaggingResult.Mark] to continue the session with updated tag markers.
   *
   * Note that the [results] collection will be mutated.
   */
  fun markOrJump(query: SearchQuery, results: Map<Editor, IntList>): TaggingResult {
    val isRegex = query is SearchQuery.RegularExpression
    val queryText = if (isRegex) " ${query.rawText}" else query.rawText[0] + query.rawText.drop(1).lowercase()

    val availableTags = allPossibleTags.filter { !queryText.endsWith(it[0]) && it !in tagMap }

    if (!isRegex) {
      for (entry in tagMap.entries) {
        if (entry solves queryText) {
          return TaggingResult.Jump(query = queryText.substringBefore(entry.key), mark = entry.key, tag = entry.value)
        }
      }
      
      if (queryText.length == 1) {
        for ((editor, offsets) in results) {
          removeResultsWithOverlappingTags(editor, offsets)
        }
      }
    }

    if (!isRegex || tagMap.isEmpty())
      tagMap = assignTagsAndMerge(results, availableTags, query, queryText)

    val resultTags = results.flatMap { (editor, offsets) -> offsets.map { Tag(editor, it) } }
    return TaggingResult.Mark(createTagMarkers(resultTags, query.rawText.ifEmpty { null }))
  }

  /**
   * Assigns as many unassigned tags as possible, and merges them with
   * the existing compatible tags.
   */
  private fun assignTagsAndMerge(
    results: Map<Editor, IntList>,
    availableTags: List<String>,
    query: SearchQuery,
    queryText: String
  ): HashBiMap<String, Tag> {
    val caches = results.keys.associateWith { EditorOffsetCache.new() }

    for ((editor, offsets) in results) {
      val cache = caches.getValue(editor)
      
      offsets.sort { a, b ->
        val aIsVisible = VISIBLE_ON_SCREEN.isOffsetInside(editor, a, cache)
        val bIsVisible = VISIBLE_ON_SCREEN.isOffsetInside(editor, b, cache)

        when {
          aIsVisible && !bIsVisible -> -1
          bIsVisible && !aIsVisible -> 1
          else -> 0
        }
      }
    }

    val allAssignedTags = mutableMapOf<String, Tag>()
    val oldCompatibleTags = tagMap.filter { (mark, tag) ->
      isTagCompatibleWithQuery(mark, tag, queryText) || results[tag.editor]?.contains(tag.offset) == true
    }
    
    val vacantResults: Map<Editor, IntList>
    if (oldCompatibleTags.isEmpty()) {
      vacantResults = results
    } else {
      val vacant = mutableMapOf<Editor, IntList>()
      
      for ((editor, offsets) in results) {
        val list = IntArrayList()
        val iter = offsets.iterator()
        
        while (iter.hasNext()) {
          val tag = Tag(editor, iter.nextInt())

        if (tag !in oldCompatibleTags.values) {
            list.add(tag.offset)
          }
        }
        
        vacant[editor] = list
      }
      
      vacantResults = vacant
    }

    allAssignedTags.putAll(oldCompatibleTags)
    allAssignedTags.putAll(Solver.solve(editors, query, vacantResults, results, availableTags, caches))
    
    return allAssignedTags.mapKeysTo(HashBiMap.create(allAssignedTags.size)) { (tag, _) ->
      // Avoid matching query - will trigger a jump.
      // TODO: lift this constraint.
      val queryEndsWith = queryText.endsWith(tag[0]) || queryText.endsWith(tag)

      if (!queryEndsWith && canShortenTag(tag, allAssignedTags.keys))
        tag[0].toString()
      else
        tag
    }
  }
  
  private infix fun Map.Entry<String, Tag>.solves(query: String): Boolean =
    query.endsWith(key, true) && isTagCompatibleWithQuery(key, value, query)
  
  private fun isTagCompatibleWithQuery(marker: String, tag: Tag, query: String): Boolean =
    tag.editor.immutableText.matchesAt(tag.offset, getPlaintextPortion(query, marker), ignoreCase = true)
  
  fun isQueryCompatibleWithTagAt(query: String, tag: Tag): Boolean =
    tagMap.inverse()[tag].let { it != null && isTagCompatibleWithQuery(it, tag, query) }
  
  fun canQueryMatchAnyVisibleTag(query: String): Boolean =
    tagMap.any { (label, tag) ->
      val tagPortion = getTagPortion(query, label)
      tagPortion.isNotEmpty()
        && label.startsWith(tagPortion, ignoreCase = true)
        && isTagCompatibleWithQuery(label, tag, query)
        && tag.isVisible()
    }

  private fun removeResultsWithOverlappingTags(editor: Editor, offsets: IntList) {
    val iter = offsets.iterator()
    val chars = editor.immutableText
    
    while (iter.hasNext()) {
      if (!chars.canTagWithoutOverlap(iter.nextInt())) {
        iter.remove() // Very uncommon, so slow removal is fine.
      }
    }
  }
  
  private fun createTagMarkers(tags: Collection<Tag>, literalQueryText: String?): MutableMap<Editor, Collection<TagMarker>> {
    val tagMapInv = tagMap.inverse()
    val markers = ArrayListMultimap.create<Editor, TagMarker>(editors.size, min(tags.size, 50))
    
    for (tag in tags) {
      val mark = tagMapInv[tag] ?: continue
      val editor = tag.editor
      val marker = TagMarker.create(editor, mark, tag.offset, literalQueryText)
      markers.put(editor, marker)
    }
    
    return markers.asMap()
  }
  
  private companion object {
    private fun CharSequence.canTagWithoutOverlap(loc: Int) = when {
      loc - 1 < 0 -> true
      loc + 1 >= length -> true
      this[loc] isUnlike this[loc - 1] -> true
      this[loc] isUnlike this[loc + 1] -> true
      this[loc] != this[loc - 1] -> true
      this[loc] != this[loc + 1] -> true
      this[loc + 1] == '\r' || this[loc + 1] == '\n' -> true
      this[loc - 1] == this[loc] && this[loc] == this[loc + 1] -> false
      this[loc + 1].isWhitespace() && this[(loc + 2).coerceAtMost(length - 1)].isWhitespace() -> true
      else -> false
    }

    private infix fun Char.isUnlike(other: Char) =
      this.isWordPart xor other.isWordPart ||
        this.isWhitespace() xor other.isWhitespace()

    private fun getPlaintextPortion(query: String, marker: String) = when {
      query.endsWith(marker, true) -> query.dropLast(marker.length)
      query.endsWith(marker.first(), true) -> query.dropLast(1)
      else -> query
    }

    private fun getTagPortion(query: String, marker: String) = when {
      query.endsWith(marker, true) -> query.takeLast(marker.length)
      query.endsWith(marker.first(), true) -> query.takeLast(1)
      else -> ""
    }

    private fun canShortenTag(marker: String, markers: Collection<String>): Boolean {
      for (other in markers)
        if (marker != other && marker[0] == other[0])
          return false

      return true
    }
  }
}
