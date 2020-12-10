plugins {
  kotlin("jvm")
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(project(":playback-api"))

  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
  compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
}
