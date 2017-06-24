import org.gradle.script.lang.kotlin.*
import org.jetbrains.intellij.IntelliJPluginExtension

buildscript {
  repositories {
    maven { setUrl("https://dl.bintray.com/jetbrains/intellij-plugin-service") }
  }
}

plugins {
  id("org.jetbrains.intellij") version "0.2.13"
  id("org.jetbrains.kotlin.jvm") version "1.1.3"
}

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "com.johnlindquist"
version = "3.3.2"