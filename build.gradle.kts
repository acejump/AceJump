plugins {
  id("org.jetbrains.intellij") version "0.2.15"
  id("org.jetbrains.kotlin.jvm") version "1.1.3"
}

configurations.all {
  resolutionStrategy.cacheDynamicVersionsFor(10, TimeUnit.DAYS)
}

intellij {
  pluginName = "AceJump"
  updateSinceUntilBuild = false
}

group = "com.johnlindquist"
version = "3.4.1"
