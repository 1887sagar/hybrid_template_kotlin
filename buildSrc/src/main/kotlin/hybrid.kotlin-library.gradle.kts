/*
 * Kotlin Hybrid Architecture Template - Build Configuration
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 */

plugins {
    id("hybrid.kotlin-common")
    `java-library`
}

dependencies {
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }
}
