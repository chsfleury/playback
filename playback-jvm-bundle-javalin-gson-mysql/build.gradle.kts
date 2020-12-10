plugins {
  kotlin("jvm")
  application
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
}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
  compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
}
