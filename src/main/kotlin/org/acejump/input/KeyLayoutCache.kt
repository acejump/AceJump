package org.acejump.input

import org.acejump.config.AceSettings

/**
 * Stores data specific to the selected keyboard layout. We want to assign tags with easily reachable keys first, and ideally have tags
 * with repeated keys (ex. FF, JJ) or adjacent keys (ex. GH, UJ).
 */
internal object KeyLayoutCache {
  /**
   * Sorts tags according to current keyboard layout settings, and some predefined rules that force tags with digits, and tags with two
   * keys far apart, to be sorted after other (easier to type) tags.
   */
  lateinit var tagOrder: Comparator<String>
    private set
  
  /**
   * Returns all possible two key tags, pre-sorted according to [tagOrder].
   */
  lateinit var allPossibleTags: List<String>
    private set
  
  /**
   * Called before any lazily initialized properties are used, to ensure that they are initialized even if the settings are missing.
   */
  fun ensureInitialized(settings: AceSettings) =
    if (!::tagOrder.isInitialized) reset(settings) else Unit

  /**
   * Re-initializes cached data according to updated settings.
   */
  fun reset(settings: AceSettings) {
    tagOrder = compareBy(
      { it[0].isDigit() || it[1].isDigit() },
      { settings.layout.distanceBetweenKeys(it[0], it[1]) },
      settings.layout.priority { it[0] }
    )
    
    val allPossibleChars = settings.allowedChars
      .toCharArray()
      .filter(Char::isLetterOrDigit)
      .distinct()
      .joinToString("")
      .ifEmpty(settings.layout::allChars)
    
    allPossibleTags = allPossibleChars.flatMap { a ->
      allPossibleChars.map { b -> "$a$b".intern() }
    }.sortedWith(tagOrder)
  }
}
