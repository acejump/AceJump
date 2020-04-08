import org.jetbrains.intellij.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.tasks.PatchPluginXmlTask

plugins {
  idea apply true
  kotlin("jvm") version "1.3.71"
  id("org.jetbrains.intellij") version "0.4.18"
}

fun fetchChangeNotes() =
  File("CHANGES.md").readLines().drop(4).takeWhile { !it.startsWith("###") }.let { notes ->
  "<![CDATA[$notes<a href=\"https://github.com/acejump/AceJump/blob/master/src/main/resources/META-INF/CHANGES.md\">Release Notes</a> ]]>"
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
    token(project.findProperty("jbr.token") as String?
      ?: System.getenv("JBR_TOKEN"))
  }

  withType<PatchPluginXmlTask> {
    sinceBuild("183.*")
    changeNotes(fetchChangeNotes())
  }
}

dependencies {
  // gradle-intellij-plugin doesn't attach sources properly for Kotlin :(
  compileOnly(kotlin("stdlib-jdk8"))
  implementation("net.duguying.pinyin:pinyin:0.0.1")
}

repositories.mavenCentral()

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
  setPlugins("java")
}

group = "org.acejump"
version = "3.5.9"
