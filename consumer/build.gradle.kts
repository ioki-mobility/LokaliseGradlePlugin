plugins {
    kotlin("jvm") version "1.9.0"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    gradleApi()
    implementation("com.ioki.lokalise:lokalise-gradle-plugin:2.3.0-SNAPSHOT")
}