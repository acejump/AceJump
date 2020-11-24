package org.acejump.session

import com.intellij.openapi.editor.Editor

/**
 * Holds [Editor] caret settings. The settings are saved the moment a [Session] starts, modified to indicate AceJump states, and restored
 * once the [Session] ends.
 */
internal data class EditorSettings(private val isBlockCursor: Boolean, private val isBlinkCaret: Boolean) {
  companion object {
    fun setup(editor: Editor): EditorSettings {
      val settings = editor.settings
      val original = EditorSettings(
        isBlockCursor = settings.isBlockCursor,
        isBlinkCaret = settings.isBlinkCaret
      )
      
      settings.isBlockCursor = true
      settings.isBlinkCaret = false
      
      return original
    }
  }
  
  fun restore(editor: Editor) = editor.settings.let {
    it.isBlockCursor = isBlockCursor
    it.isBlinkCaret = isBlinkCaret
  }
}
