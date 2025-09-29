////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Build Configuration
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
////////////////////////////////////////////////////////////////////////////////

plugins {
    id("hybrid.kotlin-library")
}

dependencies {
    // Presentation depends ONLY on Application (not Infrastructure or Domain directly)
    implementation(project(":application"))

    // Presentation-specific dependencies
    implementation(libs.arrow.core.v2)
    implementation(libs.kotlinx.coroutines.core)
    // TODO: Add CLI framework dependencies when needed
    // implementation("com.github.ajalt.clikt:clikt:4.2.1")

    testImplementation(project(":domain")) // For testing domain errors
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.archunit)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
