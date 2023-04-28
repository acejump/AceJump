package org.acejump.session

import com.intellij.openapi.editor.Editor

/**
 * Holds [Editor] caret settings. The settings are saved the
 * moment a [Session] starts, modified to indicate AceJump
 * states, and restored once the [Session] ends.
 */
internal data class EditorSettings(
  private val isBlockCursor: Boolean,
  private val isBlinkCaret: Boolean,
) {
  companion object {
    fun setup(editor: Editor): EditorSettings {
      val settings = editor.settings
      val document = editor.document

      val original = EditorSettings(
        isBlockCursor = settings.isBlockCursor,
        isBlinkCaret = settings.isBlinkCaret,
      )

      settings.isBlockCursor = true
      settings.isBlinkCaret = false

      return original
    }
  }

  fun restore(editor: Editor) {
    val settings = editor.settings
    val document = editor.document

    settings.isBlockCursor = isBlockCursor
    settings.isBlinkCaret = isBlinkCaret
  }
}
