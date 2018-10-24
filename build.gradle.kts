import org.gradle.internal.impldep.org.testng.TestNG
import org.jetbrains.gradle.ext.GradleTask
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.Remote
import org.jetbrains.gradle.ext.RunConfiguration
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories.mavenCentral()

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  withType<RunIdeTask> {
    findProperty("luginDev")?.let { args = listOf(projectDir.absolutePath) }
  }
}

plugins {
  idea apply true
  kotlin("jvm") version "1.3.0-rc-190"
  id("org.jetbrains.intellij") version "0.3.11"
  id("org.jetbrains.gradle.plugin.idea-ext") version "0.3" apply true
}

idea {
  project {
    (this as ExtensionAware)
    configure<ProjectSettings> {
      runConfigurations {
        create("runIde", org.jetbrains.gradle.ext.Application::class.java) {
          beforeRun.register("runIdx", GradleTask::class.java) {
            task = task("runIde")
          }
        }
      }
    }
  }
}

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "org.acejump"
version = "3.5.1"
