import org.jetbrains.changelog.closure
import org.jetbrains.intellij.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.tasks.PatchPluginXmlTask

plugins {
  idea apply true
  kotlin("jvm") version "1.3.72"
  id("org.jetbrains.intellij") version "0.6.1"
  id("org.jetbrains.changelog") version "0.6.2"
}

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
    val intellijPublishToken: String? by project
    token(intellijPublishToken)
  }

  withType<PatchPluginXmlTask> {
    sinceBuild("201.6668.0")
    changeNotes({ changelog.getLatest().toHTML() })
  }
}

changelog {
  path = "${project.projectDir}/CHANGES.md"
  header = closure { "${project.version}" }
}

dependencies {
  // gradle-intellij-plugin doesn't attach sources properly for Kotlin :(
  compileOnly(kotlin("stdlib-jdk8"))
  implementation("com.github.promeg:tinypinyin:2.0.3")
}

repositories {
  mavenCentral()
  jcenter()
}

intellij {
  version = "2020.2"
  pluginName = "AceJump"
  updateSinceUntilBuild = false
  setPlugins("java")
}

group = "org.acejump"
version = "3.6.3"