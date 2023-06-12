package org.acejump.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.IdeActions.ACTION_GOTO_DECLARATION
import com.intellij.openapi.actionSystem.IdeActions.ACTION_GOTO_TYPE_DECLARATION
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.playback.commands.ActionCommand
import org.acejump.countMatchingCharacters
import org.acejump.immutableText
import org.acejump.input.JumpMode
import org.acejump.input.JumpMode.DECLARATION
import org.acejump.input.JumpMode.JUMP_END
import org.acejump.input.JumpMode.TARGET
import org.acejump.isWordPart
import org.acejump.search.SearchProcessor
import org.acejump.search.Tag
import org.acejump.wordEnd
import org.acejump.wordStart

/**
 * Performs [JumpMode] navigation and actions.
 */
internal class TagJumper(private val mode: JumpMode, private val searchProcessor: SearchProcessor?) {
  /**
   * Moves caret to a specific offset in the editor according to the positioning and selection rules of the current [JumpMode].
   */
  fun visit(tag: Tag) {
    val editor = tag.editor
    val offset = tag.offset

    if (mode === JUMP_END || mode === TARGET) {
      val chars = editor.immutableText
      val matchingChars = searchProcessor?.let { chars.countMatchingCharacters(offset, it.query.rawText) } ?: 0
      val targetOffset = offset + matchingChars
      val isInsideWord = matchingChars > 0 && chars[targetOffset - 1].isWordPart && chars[targetOffset].isWordPart
      val finalTargetOffset = if (isInsideWord) chars.wordEnd(targetOffset) + 1 else targetOffset

      if (mode === JUMP_END) {
        moveCaretTo(editor, finalTargetOffset)
      } else if (mode === TARGET) {
        if (isInsideWord) {
          selectRange(editor, chars.wordStart(targetOffset), finalTargetOffset)
        } else {
          selectRange(editor, offset, finalTargetOffset)
        }
      }
    } else {
      moveCaretTo(editor, offset)
    }
  }

  /**
   * Updates caret and selection by [visit]ing a specific offset in the editor, and applying session-finalizing [JumpMode] actions such as
   * using the Go To Declaration action, or selecting text between caret and target offset/word if Shift was held during the jump.
   */
  fun jump(tag: Tag, shiftMode: Boolean, isCrossEditor: Boolean) {
    val editor = tag.editor
    val oldOffset = editor.caretModel.offset

    visit(tag)

    if (mode === DECLARATION) {
      performAction(ActionManager.getInstance().getAction(if (shiftMode) ACTION_GOTO_TYPE_DECLARATION else ACTION_GOTO_DECLARATION))
      return
    }

    if (shiftMode && !isCrossEditor) {
      val newOffset = editor.caretModel.offset

      if (mode === TARGET) {
        selectRange(
          editor, oldOffset, when {
            newOffset < oldOffset -> editor.selectionModel.selectionStart
            else -> editor.selectionModel.selectionEnd
          }
        )
      } else {
        selectRange(editor, oldOffset, newOffset)
      }
    }
  }

  private companion object {
    private fun moveCaretTo(editor: Editor, offset: Int) = with(editor) {
      ensureEditorFocused(this)
      project?.let { addCurrentPositionToHistory(it, document) }
      selectionModel.removeSelection(true)
      caretModel.moveToOffset(offset)
    }

    private fun selectRange(editor: Editor, fromOffset: Int, toOffset: Int) = with(editor) {
      ensureEditorFocused(this)
      selectionModel.removeSelection(true)
      selectionModel.setSelection(fromOffset, toOffset)
      caretModel.moveToOffset(toOffset)
    }

    private fun ensureEditorFocused(editor: Editor) {
      val project = editor.project ?: return
      val fem = FileEditorManager.getInstance(project) as FileEditorManagerEx

      val window = fem.windows.firstOrNull { (it.getSelectedComposite(false)?.selectedWithProvider?.fileEditor as? TextEditor)?.editor === editor }
      if (window != null && window !== fem.currentWindow) {
        fem.currentWindow = window
      }
    }

    private fun addCurrentPositionToHistory(project: Project, document: Document) {
      CommandProcessor.getInstance().executeCommand(
        project, {
          with(IdeDocumentHistory.getInstance(project)) {
            setCurrentCommandHasMoves()
            includeCurrentCommandAsNavigation()
            includeCurrentPlaceAsChangePlace()
          }
        }, "AceJumpHistoryAppender", DocCommandGroupId.noneGroupId(document),
        UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION, document
      )
    }

    private fun performAction(action: AnAction) = ActionManager.getInstance()
      .tryToExecute(action, ActionCommand.getInputEvent(null), null, null, true)
  }
}
