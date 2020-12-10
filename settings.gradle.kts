pluginManagement {
  repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")

    mavenCentral()

    maven("https://plugins.gradle.org/m2/")
  }
}
rootProject.name = "playback"
include("playback-api")
include("playback-core")
include("playback-core-test")
include("playback-jvm-ext-javalin")
include("playback-jvm-ext-mysql")
include("playback-jvm-bundle-javalin-gson-mysql")
include("playback-jvm-ext-gson")
include("playback-jvm-ext-elasticsearch")
