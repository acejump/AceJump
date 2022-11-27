import org.jetbrains.changelog.*
import org.jetbrains.intellij.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  idea apply true
  kotlin("jvm") version "1.8.0-Beta"
  id("org.jetbrains.intellij") version "1.10.0"
  id("org.jetbrains.changelog") version "1.3.1"
  id("com.github.ben-manes.versions") version "0.44.0"
}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
  }

  named<Zip>("buildPlugin") {
    dependsOn("test")
    archiveFileName.set("AceJump.zip")
  }

  runIde {
    dependsOn("test")
    findProperty("luginDev")?.let { args = listOf(projectDir.absolutePath) }
  }

  publishPlugin {
    val intellijPublishToken: String? by project
    token.set(intellijPublishToken)
  }

  patchPluginXml {
    sinceBuild.set("213.5744.223")
    changeNotes.set(provider {
      changelog.getAll().values.take(2).last().toHTML()
    })
  }

  runPluginVerifier {
    ideVersions.set(listOf("2022.2"))
  }

  // Remove pending: https://youtrack.jetbrains.com/issue/IDEA-278926
  @Suppress("UNUSED_VARIABLE")
  val test by getting(Test::class) {
    isScanForTestClasses = false
    // Only run tests from classes that end with "Test"
    include("**/AceTest.class")
    include("**/ExternalUsageTest.class")
    include("**/LatencyTest.class")
  }
}

kotlin.jvmToolchain {
  run {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

changelog {
  version.set("3.8.9")
  path.set("${project.projectDir}/CHANGES.md")
  header.set(provider { "[${project.version}] - ${date()}" })
  itemPrefix.set("-")
  unreleasedTerm.set("Unreleased")
}

repositories {
  mavenCentral()
}

dependencies {
  // https://github.com/anyascii/anyascii
  implementation("com.anyascii:anyascii:0.3.1")
}

intellij {
  version.set("2022.2")
  pluginName.set("AceJump")
  updateSinceUntilBuild.set(false)
  plugins.set(listOf("java"))
}

group = "org.acejump"
version = "3.8.9"
