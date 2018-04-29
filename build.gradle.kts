import org.gradle.api.internal.initialization.ClassLoaderIds.buildScript
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.30")
  }
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  withType<RunIdeTask> {
    findProperty("roject")?.let { args = listOf(it as String) }
  }
}

plugins {
  kotlin("jvm") version "1.2.30"
  id("org.jetbrains.intellij") version "0.3.1"
}

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "com.johnlindquist"
version = "3.5.0"
