import org.jetbrains.changelog.*
import org.jetbrains.intellij.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  idea apply true
  kotlin("jvm") version "1.6.0-RC2"
  id("org.jetbrains.intellij") version "1.2.1"
  id("org.jetbrains.changelog") version "1.3.1"
  id("com.github.ben-manes.versions") version "0.39.0"
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
  }

  named<Zip>("buildPlugin") {
    dependsOn("test")
    archiveFileName.set("AceJump.zip")
  }

  withType<RunIdeTask> {
    dependsOn("test")
    findProperty("luginDev")?.let { args = listOf(projectDir.absolutePath) }
  }

  publishPlugin {
    val intellijPublishToken: String? by project
    token.set(intellijPublishToken)
  }

  patchPluginXml {
    sinceBuild.set("203.7717.56")
    changeNotes.set(provider {
      changelog.getAll().values.take(2).last().toHTML()
    })
  }

  runPluginVerifier {
    ideVersions.set(listOf("2021.2.1"))
  }
}

changelog {
  version.set("3.8.5")
  path.set("${project.projectDir}/CHANGES.md")
  header.set(provider { "[${project.version}] - ${date()}" })
  itemPrefix.set("-")
  unreleasedTerm.set("Unreleased")
}

repositories {
  mavenCentral()
  maven("https://jitpack.io")
}

dependencies {
  // gradle-intellij-plugin doesn't attach sources properly for Kotlin :(
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  compileOnly(kotlin("stdlib-jdk8"))
  implementation("com.anyascii:anyascii:0.3.0")
}

intellij {
  version.set("2021.2.1")
  pluginName.set("AceJump")
  updateSinceUntilBuild.set(false)
  plugins.set(listOf("java"))
}

group = "org.acejump"
version = "3.8.5"
