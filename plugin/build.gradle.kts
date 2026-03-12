// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

plugins {
    `java-gradle-plugin`
//    id("buildlogic.java-common-conventions")

    // https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin
    kotlin("jvm") version "2.3.10"

    // Apply the Kotlin JVM plugin to add support for Kotlin.
//    alias(libs.plugins.kotlin.jvm)  // XXX works in 9.4.0 composite build, but not here?
}

repositories {
    mavenCentral()
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        // Keep in sync with `.readthedocs.yaml`
        languageVersion = JavaLanguageVersion.of(8)
    }
}

gradlePlugin {
    val fusion by plugins.creating {
        id = "dev.ionfusion.gradle.plugin"               // ???
        implementationClass = "dev.ionfusion.gradle.plugin.FusionPlugin"
    }
}

configurations.create("pluginClasses") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add("pluginClasses", tasks.jar)
}
