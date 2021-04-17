package org.acejump.search

import org.acejump.view.Tag

sealed class TaggingResult {
  class Jump(val query: String, val tag: String, val offset: Int): TaggingResult()
  class Mark(val tags: List<Tag>): TaggingResult()
}
