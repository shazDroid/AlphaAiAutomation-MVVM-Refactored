plugins {
    kotlin("jvm")
}

group = "com.shazdroid.aiautomation.extractor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":modules:adb"))
    implementation(project(":modules:domain"))
    implementation("org.jsoup:jsoup:1.18.1")
}