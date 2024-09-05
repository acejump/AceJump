package org.acejump.input

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.actionSystem.TypedActionHandler

/**
 * If at least one session exists, this listener redirects all characters
 * typed in [Editor]s with attached sessions to the appropriate sessions'
 * own handlers.
 */
internal object EditorKeyListener: TypedActionHandler {
  private val attached = mutableMapOf<Editor, TypedActionHandler>()
  private var originalHandler: TypedActionHandler? = null

  override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
    (attached[editor] ?: originalHandler ?: return).execute(editor, charTyped, dataContext)
  }

  fun attach(editor: Editor, callback: TypedActionHandler) {
    if (attached.isEmpty()) {
      val typedAction = TypedAction.getInstance()
      originalHandler = typedAction.rawHandler
      typedAction.setupRawHandler(this)
    }

    attached[editor] = callback
  }

  fun detach(editor: Editor) {
    attached.remove(editor)

    if (attached.isEmpty()) {
      originalHandler?.let(TypedAction.getInstance()::setupRawHandler)
      originalHandler = null
    }
  }
}
