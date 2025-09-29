////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Build Configuration
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
////////////////////////////////////////////////////////////////////////////////

plugins {
    id("hybrid.kotlin-library")
}

dependencies {
    implementation(libs.arrow.core.v2)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockk)
    testImplementation(libs.archunit)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
