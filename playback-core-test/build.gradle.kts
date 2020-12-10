plugins {
    kotlin("jvm")
    jacoco
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    api(project(":playback-api"))
    implementation(project(":playback-core"))
    implementation(project(":playback-jvm-ext-gson"))

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")

    implementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    implementation("org.assertj:assertj-core:3.18.1")

    testImplementation("io.mockk:mockk:1.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    test {
        useJUnitPlatform()
    }
}
