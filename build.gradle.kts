import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories.mavenCentral()

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  withType<RunIdeTask> {
    findProperty("luginDev")?.let { args = listOf(projectDir.absolutePath) }
  }
}

plugins {
  kotlin("jvm") version "1.3.0-rc-116"
  id("org.jetbrains.intellij") version "0.3.11"
}

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "org.acejump"
version = "3.5.1"