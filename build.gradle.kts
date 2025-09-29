////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Build Configuration
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
////////////////////////////////////////////////////////////////////////////////

// Root build script as pure aggregator

plugins {
    jacoco
    id("org.owasp.dependencycheck") version "10.0.3" apply false
}

group = "com.abitofhelp"
version = "1.0-SNAPSHOT"

// Aggregate JaCoCo reports from all subprojects
tasks.register<JacocoReport>("jacocoRootReport") {
    description = "Generates an aggregate report from all subprojects"
    
    val subprojectsWithTests = subprojects.filter { 
        it.name != "architecture-tests" && it.name != "buildSrc"
    }
    
    dependsOn(subprojectsWithTests.map { it.tasks.named("test") })
    
    sourceDirectories.setFrom(subprojectsWithTests.flatMap { project ->
        project.extensions.findByType(SourceSetContainer::class)?.named("main")?.get()?.allSource?.srcDirs ?: emptyList()
    })
    
    classDirectories.setFrom(subprojectsWithTests.map { project ->
        project.fileTree("build/classes/kotlin/main")
    })
    
    executionData.setFrom(subprojectsWithTests.flatMap { project ->
        project.fileTree("build/jacoco") {
            include("*.exec")
        }
    })
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregate"))
    }
}

// Verify coverage meets minimum threshold
tasks.register<JacocoCoverageVerification>("jacocoRootCoverageVerification") {
    dependsOn("jacocoRootReport")
    
    val subprojectsWithTests = subprojects.filter { 
        it.name != "architecture-tests" && it.name != "buildSrc"
    }
    
    sourceDirectories.setFrom(subprojectsWithTests.flatMap { project ->
        project.extensions.findByType(SourceSetContainer::class)?.named("main")?.get()?.allSource?.srcDirs ?: emptyList()
    })
    
    classDirectories.setFrom(subprojectsWithTests.map { project ->
        project.fileTree("build/classes/kotlin/main")
    })
    
    executionData.setFrom(subprojectsWithTests.flatMap { project ->
        project.fileTree("build/jacoco") {
            include("*.exec")
        }
    })
    
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

// Configure OWASP Dependency Check for all projects
allprojects {
    apply(plugin = "org.owasp.dependencycheck")
    
    configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
        // Fail the build on CVSS score 7 or higher
        failBuildOnCVSS = 7.0f
        
        // Configure analyzers
        analyzers.apply {
            // Enable experimental analyzers for better coverage
            experimentalEnabled = true
            
            // Archive analyzer settings
            archiveEnabled = true
            jarEnabled = true
            
            // Assembly analyzer for .NET (not needed for Kotlin)
            assemblyEnabled = false
            
            // Node.js analyzer (not needed for pure Kotlin)
            nodeEnabled = false
        }
        
        // Configure report formats
        formats = listOf("HTML", "JSON", "JUNIT")
        
        // Suppress false positives
        suppressionFile = "$rootDir/config/owasp/suppressions.xml"
    }
}

// Configure existing dependency check aggregate task
tasks.named("dependencyCheckAggregate") {
    doLast {
        println("Dependency vulnerability reports available at:")
        println("- build/reports/dependency-check-report.html")
        subprojects.forEach { project ->
            println("- ${project.name}/build/reports/dependency-check-report.html")
        }
    }
}
