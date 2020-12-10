plugins {
    kotlin("jvm")
}

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":playback-api"))

    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.10.0")

}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
