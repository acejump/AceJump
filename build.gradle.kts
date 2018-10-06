import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  withType<RunIdeTask> {
    findProperty("luginDev")?.let { args = listOf(projectDir.absolutePath) }
  }
}

plugins {
  kotlin("jvm") version "1.2.71"
  id("org.jetbrains.intellij") version "0.3.11"
}

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "org.acejump"
version = "3.5.0"

repositories.mavenCentral()
dependencies.testCompile("io.reactivex.rxjava2:rxkotlin:2.3.0")