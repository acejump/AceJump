package com.johnlindquist.acejump

import com.intellij.find.FindManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.johnlindquist.acejump.keycommands.ShowLineMarkers
import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.ui.SearchBox

open class AceJumpAction() : DumbAwareAction() {
  var aceFinder: AceFinder? = null
  var searchBox: SearchBox? = null

  override fun update(e: AnActionEvent?) {
    e?.presentation?.isEnabled = (e?.getData(EDITOR)) != null
  }

  override fun actionPerformed(actionEvent: AnActionEvent) {
    // Todo: find a way to avoid re-instantiating these on each invocation
    val project = actionEvent.getData(PROJECT) as Project
    val editor = actionEvent.getData(EDITOR) as EditorImpl
    val findManager = FindManager.getInstance(project)!!
    aceFinder = AceFinder(findManager, editor)
    searchBox = SearchBox(aceFinder!!, editor)

    ApplicationManager.getApplication().invokeLater({
      IdeFocusManager.getInstance(project).requestFocus(searchBox!!, false)
    })
  }
}

class AceJumpLineAction : AceJumpAction() {
  override fun actionPerformed(actionEvent: AnActionEvent) {
    super.actionPerformed(actionEvent)
    searchBox!!.processRegexCommand(ShowLineMarkers(aceFinder!!))
  }
}