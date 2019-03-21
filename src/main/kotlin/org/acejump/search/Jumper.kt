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
import org.acejump.search.JumpMode.DEFINE
import org.acejump.search.JumpMode.TARGET
import org.acejump.view.Model.editor
import org.acejump.view.Model.editorText
import org.acejump.view.Model.project
import java.lang.Math.max
import java.lang.Math.min
import java.util.*

/**
 * Caret controller. Responsible for moving the caret once a tag is selected.
 */

object Jumper: Resettable {
  private val logger = Logger.getInstance(Jumper::class.java)

  fun toggleMode() = toggleMode(null)

  fun toggleTargetMode() = toggleMode(TARGET)

  fun toggleDeclarationMode() = toggleMode(DEFINE)

  private fun toggleMode(mode: JumpMode? = null) =
    logger.info("Entering ${JumpMode.toggle(mode)} mode")

  fun jumpTo(newOffset: Int, done: Boolean = true) =
    editor.run {
      val logPos = offsetToLogicalPosition(newOffset)
      logger.info("Jumping to line ${logPos.line}, column ${logPos.column}...")

      val oldOffset = caretModel.offset
      moveCaretTo(newOffset)

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

  private fun Editor.appendCaretPositionToNavigationHistory() =
    CommandProcessor.getInstance().executeCommand(project,
      aceJumpHistoryAppender, "AceJumpHistoryAppender",
      DocCommandGroupId.noneGroupId(document), document)

  private fun moveCaretTo(offset: Int) = editor.run {
    appendCaretPositionToNavigationHistory()
    selectionModel.removeSelection()
    caretModel.moveToOffset(offset)
  }

  private fun Editor.selectWordAtOffset(offset: Int = caretModel.offset) {
    val ranges = ArrayList<TextRange>()
    addWordSelection(false, editorText, offset, ranges)

    if (ranges.isEmpty()) return

    val firstRange = ranges[0]
    val startOfWordOffset = max(0, firstRange.startOffset)
    val endOfWordOffset = min(firstRange.endOffset, editorText.length)

    selectRange(startOfWordOffset, endOfWordOffset)
  }

  private fun gotoSymbolAction() =
    runAndWait {
      ActionManager.getInstance().tryToExecute(GotoDeclarationAction(),
        ActionCommand.getInputEvent("NewFromTemplate"), null, null, true)
    }

  override fun reset() = JumpMode.reset()
}
