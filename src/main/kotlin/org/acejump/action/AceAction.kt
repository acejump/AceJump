package org.acejump.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.PlatformDataKeys.LAST_ACTIVE_FILE_EDITOR
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.IncorrectOperationException
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries
import org.acejump.boundaries.StandardBoundaries.AFTER_CARET
import org.acejump.boundaries.StandardBoundaries.BEFORE_CARET
import org.acejump.boundaries.StandardBoundaries.VISIBLE_ON_SCREEN
import org.acejump.boundaries.StandardBoundaries.WHOLE_FILE
import org.acejump.input.JumpMode
import org.acejump.input.JumpMode.DECLARATION
import org.acejump.input.JumpMode.JUMP
import org.acejump.input.JumpMode.JUMP_END
import org.acejump.input.JumpMode.TARGET
import org.acejump.search.Pattern
import org.acejump.search.Pattern.ALL_WORDS
import org.acejump.search.Pattern.LINE_ALL_MARKS
import org.acejump.search.Pattern.LINE_ENDS
import org.acejump.search.Pattern.LINE_INDENTS
import org.acejump.search.Pattern.LINE_STARTS
import org.acejump.session.Session
import org.acejump.session.SessionManager

/**
 * Base class for keyboard-activated actions that create or update an AceJump [Session].
 */
sealed class AceAction: DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  final override fun update(action: AnActionEvent) {
    action.presentation.isEnabled =
      (action.getData(EDITOR) ?: (action.getData(LAST_ACTIVE_FILE_EDITOR) as? TextEditor)?.editor) != null
  }

  final override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(EDITOR) ?: (e.getData(LAST_ACTIVE_FILE_EDITOR) as? TextEditor)?.editor ?: return
    val project = e.project
  
    if (project != null) {
      try {
        val fem = FileEditorManager.getInstance(project) as FileEditorManagerEx
        val openEditors = fem.splitters.getSelectedEditors()
          .mapNotNull { (it as? TextEditor)?.editor }
          .sortedBy { if (it === editor) 0 else 1 }
        invoke(SessionManager.start(editor, openEditors))
      } catch (e: IncorrectOperationException) {
        invoke(SessionManager.start(editor))
      }
    } else {
      invoke(SessionManager.start(editor))
    }
  }
  
  abstract operator fun invoke(session: Session)

  /**
   * Generic action type that toggles a specific [JumpMode].
   */
  abstract class BaseToggleJumpModeAction(private val mode: JumpMode): AceAction() {
    final override fun invoke(session: Session) = session.toggleJumpMode(mode)
  }

  /**
   * Generic action type that toggles a specific [JumpMode] with [Boundaries].
   */
  abstract class BaseToggleBoundedJumpModeAction(private val mode: JumpMode, private val boundaries: Boundaries): AceAction() {
    final override fun invoke(session: Session) = session.toggleJumpMode(mode, boundaries)
  }

  /**
   * Generic action type that starts a regex search.
   */
  abstract class BaseRegexSearchAction(private val pattern: Pattern, private val boundaries: Boundaries): AceAction() {
    override fun invoke(session: Session) = session.startRegexSearch(pattern, boundaries)
  }

  /**
   * Initiates an AceJump session in the first [JumpMode], or cycles to the next [JumpMode] as defined in configuration.
   */
  class ActivateOrCycleMode: AceAction() {
    override fun invoke(session: Session) = session.cycleNextJumpMode()
  }

  /**
   * Initiates an AceJump session in the last [JumpMode], or cycles to the previous [JumpMode] as defined in configuration.
   */
  class ActivateOrReverseCycleMode: AceAction() {
    override fun invoke(session: Session) = session.cyclePreviousJumpMode()
  }

  // @formatter:off

  // Unbounded Toggle Modes
  class ToggleJumpMode        : BaseToggleJumpModeAction(JUMP)
  class ToggleJumpEndMode     : BaseToggleJumpModeAction(JUMP_END)
  class ToggleTargetMode      : BaseToggleJumpModeAction(TARGET)
  class ToggleDeclarationMode : BaseToggleJumpModeAction(DECLARATION)

  // Bounded Toggle Modes
  class ToggleBackwardJumpMode : BaseToggleBoundedJumpModeAction(JUMP, BEFORE_CARET)
  class ToggleForwardJumpMode  : BaseToggleBoundedJumpModeAction(JUMP, AFTER_CARET)

  // Regex Modes
  class StartAllWordsMode          : BaseRegexSearchAction(ALL_WORDS, WHOLE_FILE)
  class StartAllWordsBackwardsMode : BaseRegexSearchAction(ALL_WORDS, BEFORE_CARET)
  class StartAllWordsForwardMode   : BaseRegexSearchAction(ALL_WORDS, AFTER_CARET)
  class StartAllLineStartsMode     : BaseRegexSearchAction(LINE_STARTS, VISIBLE_ON_SCREEN)
  class StartAllLineEndsMode       : BaseRegexSearchAction(LINE_ENDS, VISIBLE_ON_SCREEN)
  class StartAllLineIndentsMode    : BaseRegexSearchAction(LINE_INDENTS, VISIBLE_ON_SCREEN)
  class StartAllLineMarksMode      : BaseRegexSearchAction(LINE_ALL_MARKS, VISIBLE_ON_SCREEN)

  // @formatter:on
}
