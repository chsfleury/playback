plugins {
  kotlin("jvm")
  application
  id("com.google.cloud.tools.jib") version "2.7.0"
}

application {
  mainClassName = "com.github.playback.bundle.jvm.PlaybackApp"
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation(project(":playback-core"))
  implementation(project(":playback-jvm-ext-javalin"))
  implementation(project(":playback-jvm-ext-mysql"))
  implementation(project(":playback-jvm-ext-gson"))

  implementation("org.slf4j:slf4j-simple:1.7.30")
  implementation("com.github.uchuhimo.konf:konf-core:v0.23.0")
  implementation("com.github.uchuhimo.konf:konf-yaml:v0.23.0")
}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
  compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
}

jib {
  to {
    image = "playback-jvm-sql"
  }
}
