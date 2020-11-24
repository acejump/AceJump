package org.acejump.search

import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.acejump.boundaries.Boundaries
import org.acejump.immutableText
import org.acejump.isWordPart
import org.acejump.matchesAt

/**
 * Searches editor text for matches of a [SearchQuery], and updates previous results when the user [type]s a character.
 */
internal class SearchProcessor private constructor(private val editor: Editor, query: SearchQuery, boundaries: Boundaries) {
  companion object {
    fun fromChar(editor: Editor, char: Char, boundaries: Boundaries): SearchProcessor {
      return SearchProcessor(editor, SearchQuery.Literal(char.toString()), boundaries)
    }
    
    fun fromRegex(editor: Editor, pattern: String, boundaries: Boundaries): SearchProcessor {
      return SearchProcessor(editor, SearchQuery.RegularExpression(pattern), boundaries)
    }
  }
  
  var query = query
    private set
  
  var results = IntArrayList(0)
    private set
  
  init {
    val regex = query.toRegex()
    
    if (regex != null) {
      val offsetRange = boundaries.getOffsetRange(editor)
      var result = regex.find(editor.immutableText, offsetRange.first)
      
      while (result != null) {
        val index = result.range.first // For some reason regex matches can be out of bounds, but boundary check prevents an exception.
        val highlightEnd = index + query.getHighlightLength("", index)
        
        if (highlightEnd > offsetRange.last) {
          break
        }
        else if (boundaries.isOffsetInside(editor, index)) {
          results.add(index)
        }
        
        result = result.next()
      }
    }
  }
  
  /**
   * Appends a character to the search query and removes all search results that no longer match the query. If the last typed character
   * transitioned the search query from a non-word to a word, it notifies the [Tagger] to reassign all tags. If the new query does not
   * make sense because it would remove every result, the change is reverted and this function returns false.
   */
  fun type(char: Char, tagger: Tagger): Boolean {
    val newQuery = query.rawText + char
    val chars = editor.immutableText
    val canMatchTag = tagger.canQueryMatchAnyTag(newQuery)
    
    // If the typed character is not compatible with any existing tag or as a continuation of any previous occurrence, reject the query
    // change and return false to indicate that nothing else should happen.
    
    if (newQuery.length > 1 && !canMatchTag && results.none { chars.matchesAt(it, newQuery, ignoreCase = true) }) {
      return false
    }
    
    // If the typed character transitioned the search query from a non-word to a word, and the typed character does not belong to an
    // existing tag, we basically restart the search at the beginning of every new word, and unmark existing results so that all tags get
    // regenerated immediately afterwards. Although this causes tags to change, it is one solution for conflicts between tag characters and
    // search query characters, and moving searches across word boundaries during search should be fairly uncommon.
    
    if (!canMatchTag && newQuery.length >= 2 && !newQuery[newQuery.length - 2].isWordPart && char.isWordPart) {
      query = SearchQuery.Literal(char.toString())
      tagger.unmark()
      
      val iter = results.iterator()
      while (iter.hasNext()) {
        val movedOffset = iter.nextInt() + newQuery.length - 1
        
        if (movedOffset < chars.length && chars[movedOffset].equals(char, ignoreCase = true)) {
          iter.set(movedOffset)
        }
        else {
          iter.remove()
        }
      }
    }
    else {
      removeObsoleteResults(newQuery, tagger)
      query = SearchQuery.Literal(newQuery)
    }
    
    return true
  }
  
  /**
   * After updating the query, removes all results that no longer match the search query.
   */
  private fun removeObsoleteResults(newQuery: String, tagger: Tagger) {
    val lastCharOffset = newQuery.lastIndex
    val lastChar = newQuery[lastCharOffset]
    val ignoreCase = newQuery[0].isLowerCase()
    val chars = editor.immutableText
    
    val remaining = IntArrayList()
    val iter = results.iterator()
    
    while (iter.hasNext()) {
      val offset = iter.nextInt()
      val endOffset = offset + lastCharOffset
      val lastTypedCharMatches = endOffset < chars.length && chars[endOffset].equals(lastChar, ignoreCase)
      
      if (lastTypedCharMatches || tagger.isQueryCompatibleWithTagAt(newQuery, offset)) {
        remaining.add(offset)
      }
    }
    
    results = remaining
  }
}
