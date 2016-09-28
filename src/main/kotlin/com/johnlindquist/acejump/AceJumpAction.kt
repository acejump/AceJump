package com.johnlindquist.acejump

import com.intellij.find.FindManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.ui.AceCanvas
import com.johnlindquist.acejump.ui.SearchBox

open class AceJumpAction() : DumbAwareAction() {
  override fun update(e: AnActionEvent?) {
    e?.presentation?.isEnabled = (e?.getData(CommonDataKeys.EDITOR)) != null
  }

  override fun actionPerformed(actionEvent: AnActionEvent) {
    val project = actionEvent.getData(CommonDataKeys.PROJECT) as Project
    val editor = actionEvent.getData(CommonDataKeys.EDITOR) as EditorImpl
    val virtualFile = actionEvent.getData(CommonDataKeys.VIRTUAL_FILE) as VirtualFile
    val findManager = FindManager.getInstance(project)!!
    val aceFinder = AceFinder(findManager, editor, virtualFile)
    val searchBox = SearchBox(aceFinder, editor)

    ApplicationManager.getApplication().invokeLater({
      IdeFocusManager.getInstance(project).requestFocus(searchBox, false)
    })
  }
}