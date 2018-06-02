import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  withType<RunIdeTask> {
    findProperty("roject")?.let { args = listOf(it as String) }
  }
}

plugins {
  kotlin("jvm") version "1.2.41"
  id("org.jetbrains.intellij") version "0.3.2"
}

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "com.johnlindquist"
version = "3.5.0"

repositories.mavenCentral()
dependencies.testCompile("io.reactivex.rxjava2:rxkotlin:2.2.0")