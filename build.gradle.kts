import org.gradle.api.internal.initialization.ClassLoaderIds.buildScript
import org.jetbrains.intellij.tasks.RunIdeaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.21")
  }
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  withType<RunIdeaTask> {
    findProperty("roject")?.let { args = listOf(it as String) }
  }
}

plugins {
  kotlin("jvm") version "1.2.21"
  id("org.jetbrains.intellij") version "0.2.17"
}

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

repositories {
  mavenCentral()
}

dependencies {
  compile("org.eclipse.collections:eclipse-collections-api:9.1.0")
  compile("org.eclipse.collections:eclipse-collections:9.1.0")
}

group = "com.johnlindquist"
version = "3.4.3"