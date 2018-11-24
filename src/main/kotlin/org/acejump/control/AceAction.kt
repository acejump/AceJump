package org.acejump.control

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import org.acejump.control.Handler.regexSearch
import org.acejump.label.Pattern
import org.acejump.label.Pattern.ALL_WORDS
import org.acejump.search.Jumper
import org.acejump.search.getNameOfFileInEditor
import org.acejump.view.Boundary.*
import org.acejump.view.Model.boundaries
import org.acejump.view.Model.defaultBoundary
import org.acejump.view.Model.editor

/**
 * Entry point for all actions. The IntelliJ Platform calls AceJump here.
 */

open class AceAction: DumbAwareAction() {
  open val logger = Logger.getInstance(javaClass)
  override fun update(action: AnActionEvent) {
    action.presentation.isEnabled = action.getData(EDITOR) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    boundaries = defaultBoundary
    editor = e.getData(EDITOR) ?: return
    val textLength = editor.document.textLength
    logger.info("Invoked on ${editor.getNameOfFileInEditor()} ($textLength)")
    Handler.activate()
    customize()
  }

  open fun customize() = Jumper.toggleMode()
}

/**
 * When target mode is activated, selecting a tag will highlight an entire word.
 */

class AceTargetAction: AceAction() {
  override fun customize() = Jumper.toggleTargetMode()
}

/**
 * When line mode is activated, we will tag the beginning and end of each line.
 */

class AceLineAction: AceAction() {
  override fun customize() = regexSearch(Pattern.LINE_MARK)
}

/**
 * When declaration mode is activated, selecting a tag will take us to the
 * definition (i.e. declaration) of the token in the editor, if it exists.
 */

object AceDefinitionAction: AceAction() {
  override fun customize() = Jumper.toggleDeclarationMode()
}

/**
 * When word mode is activated, we will tag all words on the screen.
 */

class AceWordAction: AceAction() {
  override fun customize() = regexSearch(ALL_WORDS, SCREEN_BOUNDARY)
}

/**
 * Search for words from the start of the screen to the caret
 */

object AceWordForwardAction: AceAction() {
  override fun customize() = regexSearch(ALL_WORDS, AFTER_CARET_BOUNDARY)
}

/**
 * Search for words from the caret position to the start of the screen
 */

object AceWordBackwardsAction: AceAction() {
  override fun customize() = regexSearch(ALL_WORDS, BEFORE_CARET_BOUNDARY)
}