package org.acejump.search

import org.acejump.view.Tag

internal sealed class TaggingResult {
  class Jump(val offset: Int) : TaggingResult()
  class Mark(val tags: List<Tag>) : TaggingResult()
}
