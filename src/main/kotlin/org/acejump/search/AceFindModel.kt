package org.acejump.search

import com.intellij.find.FindModel
import kotlin.text.RegexOption.IGNORE_CASE
import kotlin.text.RegexOption.MULTILINE

class AceFindModel: FindModel {
  internal constructor(key: String, isRegex: Boolean = false): super() {
    isCaseSensitive = false
    stringToFind = key
    isRegularExpressions = isRegex
  }

  fun toRegex(): Regex {
    var regex = stringToFind
    val options = mutableSetOf(MULTILINE)

    if (!isCaseSensitive && stringToFind.first().isLowerCase())
      options.add(IGNORE_CASE)
    if (!isRegularExpressions)
      regex = Regex.escape(stringToFind)

    return Regex(regex, options)
  }
}