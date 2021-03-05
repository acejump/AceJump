import org.jetbrains.changelog.closure
import org.jetbrains.intellij.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.tasks.PatchPluginXmlTask

plugins {
  idea apply true
  kotlin("jvm") version "1.5.0-M1"
  id("org.jetbrains.intellij") version "0.7.2"
  id("org.jetbrains.changelog") version "1.1.2"
  id("com.github.ben-manes.versions") version "0.38.0"
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      languageVersion = "1.5"
      apiVersion = "1.5"
      jvmTarget = JavaVersion.VERSION_1_8.toString()
      freeCompilerArgs += "-progressive"
    }
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
  // https://github.com/promeG/TinyPinyin
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
version = "3.7"
