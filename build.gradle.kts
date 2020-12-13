plugins {
  kotlin("jvm") version "1.4.10" apply false
}

allprojects {
  group = "com.github"
  version = "latest"

  repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://kotlin.bintray.com/kotlinx/")
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
  }
}
