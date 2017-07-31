package com.johnlindquist.acejump.label

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.johnlindquist.acejump.search.get
import com.johnlindquist.acejump.search.getView
import com.johnlindquist.acejump.search.wordBounds
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import java.lang.Math.max
import java.lang.Math.min

/**
 * Tumbles tags around sites to maximize the number of sites covered. Should be
 * able tag all results in the editor, otherwise we have failed.
 */

object Solver {
  private var bigrams: LinkedHashSet<String> = linkedSetOf()
  private var newTags: BiMap<String, Int> = HashBiMap.create()

  /**
   * Iterates through the remaining available tags, until we find one that
   * matches our criteria, i.e. does not collide with an existing tag or
   * plaintext string. To have the desired behavior, this has a surprising
   * number of edge cases that must explicitly prevented.
   *
   * @param idx the index which a tag is to be assigned
   */

  fun tryToAssignTagToIndex(idx: Int): Boolean {
    val (left, right) = editorText.wordBounds(idx)

    fun hasNearbyTag(index: Int) =
      Pair(max(left, index - 2), min(right, index + 2))
        .run { (first..second).any { newTags.containsValue(it) } }

    if (hasNearbyTag(idx)) return true

//      val (matching, nonMatching) = availableTags.partition { tag ->
//        !newTags.containsKey("${tag[0]}") && !tag.collidesWithText(idx, right)
//      }

//      val tag = matching.firstOrNull()
    val chosenTag = bigrams.firstOrNull {
      !newTags.containsKey("${it[0]}") && !it.collidesWithText(idx, right)
    }

    if (chosenTag == null)
      String(editorText[left, right]).let {
        //          logger.info("\"$it\" rejected: " + nonMatching.size + " tags.")
        return false
      }
    else {
      newTags[chosenTag] = idx
      // Prevents "...a[bc]...z[bc]..."
      bigrams.remove(chosenTag)
    }
    return true
  }

  /**
   * Sorts jump targets to determine which positions get first choice for tags,
   * by taking into account the structure of the surrounding text. For example,
   * if the jump target is the first letter in a word, it is advantageous to
   * prioritize this location (in case we run out of tags), since the user is
   * more likely to target words by their leading character than not.
   */

  fun sortValidJumpTargets(jumpTargets: Set<Int>) =
    if (Tagger.regex) jumpTargets.sortedBy { it !in editor.getView() }
    else jumpTargets.sortedWith(compareBy(
      // Sites in immediate view should come first
      { it !in editor.getView() },
      // Ensure that the first letter of a word is prioritized for tagging
      { editorText[max(0, it - 1)].isLetterOrDigit() },
      // Target words with more unique characters to the immediate right ought
      // to have first pick for tags, since they are the most "picky" targets
      { -editorText[it, editorText.wordBounds(it).second].distinct().size }))

  fun solve(results: Set<Int>, tags: LinkedHashSet<String>, newTags: BiMap<String, Int>) {
    bigrams = tags
    Solver.newTags = newTags
    var totalRejects = 0

    // Hope for the best
    sortValidJumpTargets(results).forEach {
      if (tags.isEmpty()) {
        Tagger.full = false; return
      }
      if (!tryToAssignTagToIndex(it)) {
        // But fail as soon as we miss one
        Tagger.full = false
        totalRejects++
        // We already outside the view, no need to search further if it failed
        if (it !in editor.getView()) return
      }
    }

    println("Total rejects: $totalRejects")
  }

  /**
   * Returns true IFF the receiver, inserted between the left and right indices,
   * matches an existing substring elsewhere in the editor text. We should never
   * use a tag which can be partly completed by typing plaintext, where the tag
   * is the receiver, the tag index is the leftIndex, and rightIndex is the last
   * character we care about (this is usually the last letter of the same word).
   *
   * @param leftIndex index where a tag is to be used
   * @param rightIndex index of last character (ie. end of the word)
   */

  private fun String.collidesWithText(leftIndex: Int, rightIndex: Int) =
    ((leftIndex + 1)..min(rightIndex, editorText.length)).map {
      editorText.substring(leftIndex, it) + this[0] // && it in view??
    }.any { it in editorText }
}