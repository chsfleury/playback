plugins {
  kotlin("jvm")
}

dependencies {

  implementation(kotlin("stdlib-jdk8"))
  implementation(project(":playback-api"))

  implementation("org.jooq:jooq:3.14.3")
  implementation("mysql:mysql-connector-java:8.0.22")
  implementation("com.zaxxer:HikariCP:3.4.5")

}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
  compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
}
