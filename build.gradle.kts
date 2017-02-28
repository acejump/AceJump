
import org.gradle.script.lang.kotlin.*
import org.jetbrains.intellij.IntelliJPluginExtension

buildscript {
  repositories {
    gradleScriptKotlin()
    maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.1") }
  }

  dependencies {
    classpath(kotlinModule("gradle-plugin"))
  }
}

plugins {
  id("org.jetbrains.intellij") version "0.2.5"
}

apply {
  plugin("org.jetbrains.intellij")
  plugin("kotlin")
}

configure<IntelliJPluginExtension> {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "com.johnlindquist"
version = "3.2.0"
