import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

plugins {
  id("org.jetbrains.intellij") version "0.2.15"
  id("org.jetbrains.kotlin.jvm") version "1.1.3"
}

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "com.johnlindquist"
version = "3.4.1"