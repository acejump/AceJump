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
import com.intellij.openapi.util.ActionCallback
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

object Jumper : Resettable {

  @Volatile
  var hasJumped = false
  private val logger = Logger.getInstance(Jumper::class.java)

  fun toggleMode() {
    logger.info("Setting jump mode to ${JumpMode.toggle()}")
  }

  fun toggleTargetMode() {
    logger.info("Setting jump mode to ${JumpMode.toggle(TARGET)}")
  }

  fun toggleDeclarationMode() {
    logger.info("Setting jump mode to ${JumpMode.toggle(DEFINE)}")
  }

  fun jump(index: Int) = runAndWait {
    editor.run {
      val logPos = editor.offsetToLogicalPosition(index)
      logger.info("Jumping to line ${logPos.line}, column ${logPos.column}...")

      when {
        Finder.isShiftSelectEnabled -> selectRange(caretModel.offset, index)
        // Moving the caret will trigger a reset, flipping targetModeEnabled,
        // so we need to move the caret and select the word at the same time
        JumpMode.equals(TARGET) -> moveCaret(index).also { selectWordAtOffset(index) }
        JumpMode.equals(DEFINE) -> moveCaret(index).also { gotoSymbolAction() }
        else -> moveCaret(index)
      }

      hasJumped = true
    }
  }

  // Add current caret position to navigation history
  private fun Editor.appendToHistory() =
    CommandProcessor.getInstance().executeCommand(project,
      aceJumpHistoryAppender, "AceJumpHistoryAppender",
      DocCommandGroupId.noneGroupId(document), document)


  private fun moveCaret(offset: Int) = editor.run {
    appendToHistory()
    selectionModel.removeSelection()
    caretModel.moveToOffset(offset)
  }

  private val aceJumpHistoryAppender = {
    with(IdeDocumentHistory.getInstance(project) as IdeDocumentHistoryImpl) {
      onSelectionChanged()
      includeCurrentCommandAsNavigation()
      includeCurrentPlaceAsChangePlace()
    }
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

  private fun gotoSymbolAction(): ActionCallback =
    ActionManager.getInstance().tryToExecute(GotoDeclarationAction(), ActionCommand.getInputEvent("NewFromTemplate"), null, null, true)


  override fun reset() {
    JumpMode.reset()
    hasJumped = false
  }
}
