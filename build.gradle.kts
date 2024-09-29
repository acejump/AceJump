import org.jetbrains.changelog.Changelog.OutputType.HTML
import org.jetbrains.changelog.date
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  idea
  alias(libs.plugins.kotlin) // Kotlin support
  alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
  alias(libs.plugins.changelog) // Gradle Changelog Plugin
  alias(libs.plugins.kover) // Gradle Kover Plugin
  id("com.github.ben-manes.versions") version "0.51.0"
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
  intellijPlatform.defaultRepositories()
//  intellijPlatform.localPlatformArtifacts()
}

dependencies {
  // https://github.com/anyascii/anyascii
  implementation("com.anyascii:anyascii:0.3.2")
  intellijPlatform{
    testImplementation(libs.junit)

    bundledPlugins("com.intellij.java")
    create("IC", "2024.2.2")
    pluginVerifier()
    instrumentationTools()
    testFramework(TestFrameworkType.Platform)
  }
}

intellijPlatform {
  pluginConfiguration {
    version = acejumpVersion
    name = "AceJump"
  }

  pluginVerification.ides.recommended()
}

group = "org.acejump"
version = acejumpVersion
