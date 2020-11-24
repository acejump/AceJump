package org.acejump.search

import com.intellij.codeInsight.editorActions.SelectWordUtil.addWordSelection
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.ui.playback.commands.ActionCommand
import com.intellij.openapi.util.TextRange
import org.acejump.label.Tagger
import org.acejump.search.JumpMode.*
import org.acejump.view.Model.editor
import org.acejump.view.Model.editorText
import org.acejump.view.Model.project
import kotlin.math.max
import kotlin.math.min

/**
 * Updates the [com.intellij.openapi.editor.CaretModel] after a tag is selected.
 */

object Jumper: Resettable {
  private val logger = Logger.getInstance(Jumper::class.java)

  fun cycleMode() =
    logger.info("Entering ${JumpMode.cycle()} mode")

  fun toggleMode(mode: JumpMode) =
    logger.info("Entering ${JumpMode.toggle(mode)} mode")

  fun jumpTo(newOffset: Int, done: Boolean = true) =
    editor.run {
      val logPos = offsetToLogicalPosition(newOffset)
      logger.debug("Jumping to line ${logPos.line}, column ${logPos.column}...")

      val oldOffset = caretModel.offset

      when {
        JumpMode.equals(JUMP_END) ->
          moveCaretToEnd(newOffset + countMatchingCharacters(newOffset, Tagger.query))

        else ->
          moveCaretTo(newOffset)
      }

      when {
        Finder.isShiftSelectEnabled && done -> selectRange(oldOffset, newOffset)
        JumpMode.equals(TARGET) -> selectWordAtOffset(newOffset)
        JumpMode.equals(DEFINE) && done -> gotoSymbolAction()
      }
    }

  private val aceJumpHistoryAppender = {
    with(IdeDocumentHistory.getInstance(project) as IdeDocumentHistoryImpl) {
      onSelectionChanged()
      includeCurrentCommandAsNavigation()
      includeCurrentPlaceAsChangePlace()
    }
  }

  /**
   * Ensures each jump destination is appended to [IdeDocumentHistory] so that
   * users can navigate forward and backward using the IDE actions.
   */

  private fun Editor.appendCaretPositionToEditorNavigationHistory() =
    CommandProcessor.getInstance().executeCommand(project,
      aceJumpHistoryAppender, "AceJumpHistoryAppender",
      DocCommandGroupId.noneGroupId(document), document)

  private fun Editor.moveCaretTo(offset: Int) {
    appendCaretPositionToEditorNavigationHistory()
    selectionModel.removeSelection()
    caretModel.moveToOffset(offset)
  }

  private fun Editor.moveCaretToEnd(offset: Int) {
    val ranges = ArrayList<TextRange>()
    addWordSelection(settings.isCamelWords, editorText, offset, ranges)

    if (ranges.isEmpty()) {
      moveCaretTo(offset)
    }
    else {
      moveCaretTo(min(ranges[0].endOffset, editorText.length))
    }
  }

  private fun countMatchingCharacters(offset: Int, query: String): Int {
    var count = 0
    while (offset + count < editorText.length && count < query.length && editorText[offset + count] == query[count]) {
      count++
    }
    return count
  }

  /**
   * Selects a sequence of contiguous characters adjacent to the target offset
   * matching [Character.isJavaIdentifierPart], or nothing at all.
   *
   * TODO: Make this language agnostic.
   */

  private fun Editor.selectWordAtOffset(offset: Int = caretModel.offset) {
    val ranges = ArrayList<TextRange>()
    addWordSelection(settings.isCamelWords, editorText, offset, ranges)

    ranges.ifEmpty { return }

    val firstRange = ranges[0]
    val startOfWordOffset = max(0, firstRange.startOffset)
    val endOfWordOffset = min(firstRange.endOffset, editorText.length)

    selectRange(startOfWordOffset, endOfWordOffset)
  }

  /**
   * Navigates to the target symbol's declaration, using [GotoDeclarationAction]
    */

  private fun gotoSymbolAction() =
    runNow {
      ActionManager.getInstance().tryToExecute(GotoDeclarationAction(),
        ActionCommand.getInputEvent("NewFromTemplate"), null, null, true)
    }

  override fun reset() = JumpMode.reset()
}