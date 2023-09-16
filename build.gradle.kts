import org.jetbrains.changelog.Changelog.OutputType.HTML
import org.jetbrains.changelog.date

plugins {
  idea apply true
  kotlin("jvm") version "1.9.20-Beta"
  id("org.jetbrains.intellij") version "1.15.0"
  id("org.jetbrains.changelog") version "2.2.0"
  id("com.github.ben-manes.versions") version "0.48.0"
}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
  }

  named<Zip>("buildPlugin") {
    dependsOn("test")
    archiveFileName = "AceJump.zip"
  }

  runIde {
    dependsOn("test")
    findProperty("luginDev")?.let { args = listOf(projectDir.absolutePath) }
  }

  publishPlugin {
    val intellijPublishToken: String? by project
    token = intellijPublishToken
  }

  patchPluginXml {
    sinceBuild = "223.7571.182"
    changeNotes = provider {
      changelog.renderItem(changelog.getAll().values.take(2).last(), HTML)
    }
  }

  runPluginVerifier {
    ideVersions = listOf("2023.2")
  }

  // Remove pending: https://youtrack.jetbrains.com/issue/IDEA-278926
  val test by getting(Test::class) {
    isScanForTestClasses = false
    // Only run tests from classes that end with "Test"
    include("**/AceTest.class")
    include("**/ExternalUsageTest.class")
    include("**/LatencyTest.class")
  }
}

kotlin {
  jvmToolchain {
    run {
      languageVersion = JavaLanguageVersion.of(17)
    }
  }
  sourceSets.all {
    languageSettings.apply {
      languageVersion = "2.0"
    }
  }
}

val acejumpVersion = "3.8.16"

changelog {
  version = acejumpVersion
  path = "${project.projectDir}/CHANGES.md"
  header = provider { "[${project.version}] - ${date()}" }
  itemPrefix = "-"
  unreleasedTerm = "Unreleased"
}

repositories {
  mavenCentral()
}

dependencies {
  // https://github.com/anyascii/anyascii
  implementation("com.anyascii:anyascii:0.3.2")
}

intellij {
  version = "2023.2.1"
  pluginName = "AceJump"
  updateSinceUntilBuild = false
  plugins = listOf("java")
}

group = "org.acejump"
version = acejumpVersion
