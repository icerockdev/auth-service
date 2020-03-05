/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("maven-publish")
    id("java-library")
}

apply(plugin = "java")
apply(plugin = "kotlin")

group = "com.icerockdev.service"
version = "0.0.4"

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${properties["coroutines_version"]}")
    // Ktor jwt
    implementation("io.ktor:ktor-auth-jwt:${properties["ktor_version"]}")

    // logging
    implementation("ch.qos.logback:logback-classic:${properties["logback_version"]}")

    // tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${properties["kotlin_version"]}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

repositories {
    mavenCentral()
}

publishing {
    repositories.maven("https://api.bintray.com/maven/icerockdev/backend/auth-service/;publish=1") {
        name = "bintray"

        credentials {
            username = System.getProperty("BINTRAY_USER")
            password = System.getProperty("BINTRAY_KEY")
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}
