import org.gradle.api.internal.initialization.ClassLoaderIds.buildScript
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

plugins {
  kotlin("jvm") version "1.2.0"
  id("org.jetbrains.intellij") version "0.2.17"
}

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "com.johnlindquist"
version = "3.4.3"