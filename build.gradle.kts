import org.jetbrains.intellij.IntelliJPluginExtension

buildscript {
  repositories {
    gradleScriptKotlin()
    maven { setUrl("http://dl.bintray.com/jetbrains/intellij-plugin-service") }
  }

  dependencies {
    classpath(kotlinModule("gradle-plugin"))
  }
}

plugins {
  id("org.jetbrains.intellij") version "0.2.0"
}

apply {
  plugin("org.jetbrains.intellij")
  plugin("kotlin")
}

configure<IntelliJPluginExtension> {
  version = "171.2014.21"
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "com.johnlindquist"
version = "3.1.6"
