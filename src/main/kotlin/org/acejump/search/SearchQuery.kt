package org.acejump.search

import org.acejump.countMatchingCharacters

/**
 * Defines the current search query for a session.
 */
internal sealed class SearchQuery {
  abstract val rawText: String

  /**
   * Returns how many characters the search occurrence highlight should cover.
   */
  abstract fun getHighlightLength(text: CharSequence, offset: Int): Int

  /**
   * Converts the query into a regular expression to find the initial matches.
   */
  abstract fun toRegex(): Regex?

  /**
   * Searches for all occurrences of a literal text query. If the first
   * character of the query is lowercase, then the entire query will be
   * case-insensitive.
   *
   * Each occurrence must either match the entire query, or match the query
   * up to a point so that the rest of the query matches the  beginning of
   * a tag at the location of the occurrence.
   */
  class Literal(override var rawText: String): SearchQuery() {
    init {
      require(rawText.isNotEmpty())
    }

    override fun getHighlightLength(text: CharSequence, offset: Int): Int =
      text.countMatchingCharacters(offset, rawText)

    override fun toRegex(): Regex {
      val options = mutableSetOf(RegexOption.MULTILINE)

      if (rawText.first().isLowerCase())
        options.add(RegexOption.IGNORE_CASE)

      return Regex(Regex.escape(rawText), options)
    }
  }

  /**
   * Searches for all matches of a regular expression.
   */
  class RegularExpression(private var pattern: String): SearchQuery() {
    override val rawText = ""

    override fun getHighlightLength(text: CharSequence, offset: Int) = 0

    override fun toRegex(): Regex =
      Regex(pattern, setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
  }
}
