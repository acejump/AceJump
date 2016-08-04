package com.johnlindquist.acejump.search

import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import java.util.*

class ResultComparator(document: DocumentImpl, editor: EditorImpl) : Comparator<Int> {
  val caretOffset = editor.caretModel.offset
  val lineNumber = document.getLineNumber(caretOffset)
  val lineStartOffset = document.getLineStartOffset(lineNumber)
  val lineEndOffset = document.getLineEndOffset(lineNumber)

  override fun equals(other: Any?): Boolean {
    throw UnsupportedOperationException()
  }

  override fun compare(p0: Int?, p1: Int?): Int {
    val i1 = Math.abs(caretOffset - p0!!)
    val i2 = Math.abs(caretOffset - p1!!)
    val o1OnSameLine = p0 >= lineStartOffset && p0 <= lineEndOffset
    val o2OnSameLine = p1 >= lineStartOffset && p1 <= lineEndOffset
    if (i1 > i2) {
      if (!o2OnSameLine && o1OnSameLine) {
        return -1
      }
      return 1
    } else if (i1 == i2) {
      return 0
    } else if (!o1OnSameLine && o2OnSameLine) {
      return 1
    }
    return -1
  }

  override fun hashCode(): Int {
    var result = caretOffset
    result = 31 * result + lineNumber
    result = 31 * result + lineStartOffset
    result = 31 * result + lineEndOffset
    return result
  }
}
