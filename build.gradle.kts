import org.jetbrains.changelog.Changelog.OutputType.HTML
import org.jetbrains.changelog.date

plugins {
  idea
  kotlin("jvm") version "1.8.20" // https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
  id("org.jetbrains.intellij") version "1.17.3"
  id("org.jetbrains.changelog") version "2.2.0"
  id("com.github.ben-manes.versions") version "0.51.0"
  id("org.jetbrains.kotlinx.kover") version "0.8.1" // https://github.com/Kotlin/kotlinx-kover
}

tasks {
  named<Zip>("buildPlugin") {
    dependsOn("test")
    archiveFileName = "AceJump.zip"
  }

  runIde {
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
    ideVersions = listOf("241.*")
  }

  // Remove pending: https://youtrack.jetbrains.com/issue/IDEA-278926
  val test by getting(Test::class) {
    isScanForTestClasses = false
    // Only run tests from classes that end with "Test"
    include("**/AceTest.class")
    include("**/ExternalUsageTest.class")
    include("**/LatencyTest.class")
    afterTest(
      KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
        println("Completed `${desc.displayName}` in ${result.endTime - result.startTime}ms")
      })
    )
  }
}

kotlin {
  jvmToolchain(17)
  sourceSets.all {
    languageSettings.apply {
      languageVersion = "2.0"
    }
  }
}

val acejumpVersion = "3.8.19"

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
  version = "2024.1"
  pluginName = "AceJump"
  updateSinceUntilBuild = false
  plugins = listOf("java")
}

group = "org.acejump"
version = acejumpVersion
