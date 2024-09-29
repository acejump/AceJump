package org.acejump.action

import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.ScrollType.*
import org.acejump.search.SearchProcessor
import org.acejump.search.Tag
import kotlin.math.abs

/**
 * Enables navigation between currently active tags.
 */
internal class TagVisitor(private val editor: Editor, private val searchProcessor: SearchProcessor, private val tagJumper: TagJumper) {
  /**
   * Places caret at the closest tag following the caret position, according
   * to the rules of the current jump mode (see [TagJumper.visit]).
   * If the caret is at or past the last tag, it moves to the first tag instead.
   * If there is only one tag, it immediately performs the jump action as described in [TagJumper.jump].
   */
  fun visitNext(): Boolean =
    visit(SelectionModel::getSelectionEnd) { if (it < 0) -it - 1 else it + 1 }

  /**
   * Places caret at the closest tag preceding the caret position, according to
   * the rules of the current jump mode (see [TagJumper.visit]).
   * If the caret is at or before the first tag, it moves to the last tag instead.
   * If there is only one tag, it immediately performs the jump action as described in [TagJumper.jump].
   */
  fun visitPrevious(): Boolean =
    visit(SelectionModel::getSelectionStart) { if (it < 0) -it - 2 else it - 1 }

  /**
   * Scrolls to the closest result to the caret.
   */
  fun scrollToClosest() {
    val caret = editor.caretModel.offset
    val results = searchProcessor.results[editor].takeUnless { it.isNullOrEmpty() } ?: return
    val index = results.binarySearch(caret).let { if (it < 0) -it - 1 else it }

    val targetOffset = listOfNotNull(
      results.getOrNull(index - 1),
      results.getOrNull(index)
    ).minByOrNull { abs(it - caret) }

    if (targetOffset != null)
      editor.scrollingModel.scrollTo(editor.offsetToLogicalPosition(targetOffset), RELATIVE)
  }

  private inline fun visit(caretPosition: SelectionModel.() -> Int, indexModifier: (Int) -> Int): Boolean {
    val results = searchProcessor.results[editor].takeUnless { it.isNullOrEmpty() } ?: return false
    val nextIndex = indexModifier(results.binarySearch(caretPosition(editor.selectionModel)))

    val targetOffset = results.getInt(
      when {
        nextIndex < 0 -> results.lastIndex
        nextIndex > results.lastIndex -> 0
        else -> nextIndex
      }
    )

    val onlyResult = results.size == 1

    if (onlyResult) tagJumper.jump(Tag(editor, targetOffset), shiftMode = false, isCrossEditor = false)
    else tagJumper.visit(Tag(editor, targetOffset))

    editor.scrollingModel.scrollToCaret(RELATIVE)
    return onlyResult
  }
}
