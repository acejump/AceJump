package org.acejump.search

import com.intellij.openapi.editor.Editor
import org.acejump.view.TagMarker

sealed class TaggingResult {
  class Jump(val query: String, val mark: String, val tag: Tag): TaggingResult()
  class Mark(val markers: MutableMap<Editor, Collection<TagMarker>>): TaggingResult()
}
