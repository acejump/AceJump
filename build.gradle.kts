import org.jetbrains.intellij.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    kotlinOptions.freeCompilerArgs += "-progressive"
  }

  named<Zip>("buildPlugin") {
    dependsOn("test")
    archiveFileName.set("AceJump.zip")
  }

  withType<RunIdeTask> {
    dependsOn("test")
    findProperty("luginDev")?.let { args = listOf(projectDir.absolutePath) }
  }

  withType<PublishTask> {
    token(project.findProperty("jbr.token") as String? ?: System.getenv("JBR_TOKEN"))
  }
}

plugins {
  idea apply true
  kotlin("jvm") version "1.3.61"
  id("org.jetbrains.intellij") version "0.4.14"
}

dependencies {
  // gradle-intellij-plugin doesn't attach sources properly for Kotlin :(
  compileOnly(kotlin("stdlib-jdk8"))
}

repositories.mavenCentral()

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
  setPlugins("java")
}

group = "org.acejump"
version = "3.5.9"