package com.johnlindquist.acejump

import com.intellij.find.FindManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.project.ProjectManager

class AceJumpPlugin : ApplicationComponent {
  val COMPONENT_NAME = "AceJump"
  val project = ProjectManager.getInstance().defaultProject
  val findManager = FindManager.getInstance(project)
  var searchBox: KeyboardHandler? = null
  val action = ActionManager.getInstance().getAction("AceJumpKeyAction")
  override fun initComponent() {
  }


  override fun disposeComponent() {

  }

  override fun getComponentName(): String {
    return COMPONENT_NAME
  }
}

