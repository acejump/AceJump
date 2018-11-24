import org.gradle.internal.impldep.org.testng.TestNG
import org.jetbrains.gradle.ext.GradleTask
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.Remote
import org.jetbrains.gradle.ext.RunConfiguration
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  withType<RunIdeTask> {
    dependsOn("test")
    findProperty("luginDev")?.let { args = listOf(projectDir.absolutePath) }
  }
}

plugins {
  idea apply true
  kotlin("jvm") version "1.3.0"
  id("org.jetbrains.intellij") version "0.3.12"
  id("org.jetbrains.gradle.plugin.idea-ext") version "0.3" apply true
}

tasks.withType(KotlinCompile::class) {
  kotlinOptions.freeCompilerArgs += "-progressive"
}

idea.project {
  (this as ExtensionAware)
  configure<ProjectSettings> {
    runConfigurations {
      create<org.jetbrains.gradle.ext.Application>("Run AceJump") {
        beforeRun.create<GradleTask>("runIde") {
          task = tasks.getByPath("runIde")
        }
      }
    }
  }
}

repositories.mavenCentral()

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "org.acejump"
version = "3.5.2"
