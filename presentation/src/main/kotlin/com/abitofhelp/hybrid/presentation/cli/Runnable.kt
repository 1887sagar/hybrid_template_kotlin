////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.presentation.cli

/**
 * A functional interface representing a task that can be executed.
 *
 * ## What is a Functional Interface?
 * A functional interface is an interface with exactly one abstract method.
 * In Kotlin, the `fun interface` keyword creates a functional interface that:
 * - Can be implemented using lambda expressions
 * - Can be created from method references
 * - Provides SAM (Single Abstract Method) conversion
 * - Is compatible with Java's functional interfaces
 *
 * ## Why Create Our Own Runnable?
 * While Java provides `java.lang.Runnable`, creating our own version offers benefits:
 * - **No Dependencies**: Doesn't depend on Java standard library
 * - **Clear Intent**: Explicitly shows this is for our domain
 * - **Future Flexibility**: Can be extended with domain-specific methods if needed
 * - **Kotlin-First**: Written with Kotlin conventions in mind
 *
 * ## Comparison with Java Runnable
 * ```kotlin
 * // Java Runnable (external dependency)
 * val javaRunnable = java.lang.Runnable { 
 *     println("Running task") 
 * }
 * 
 * // Our Runnable (self-contained)
 * val ourRunnable = Runnable { 
 *     println("Running task") 
 * }
 * ```
 *
 * Both work identically, but ours is self-contained.
 *
 * ## When to Use This Interface
 * This interface is ideal for:
 * - CLI programs that need to run in threads
 * - Background tasks that don't return values
 * - Integration with thread-based frameworks
 * - Legacy code that expects Runnable interface
 *
 * For modern async code, prefer suspend functions instead:
 * ```kotlin
 * // Old style - blocks thread
 * val runnable = Runnable { 
 *     blockingOperation() 
 * }
 * 
 * // Modern style - suspends without blocking
 * suspend fun asyncOperation() {
 *     suspendingOperation()
 * }
 * ```
 *
 * ## Example Usage
 * ```kotlin
 * // Lambda expression
 * val task1 = Runnable { 
 *     println("Task 1 executing") 
 * }
 * 
 * // Method reference (if you have a matching function)
 * fun doSomething() { 
 *     println("Doing something") 
 * }
 * val task2 = Runnable(::doSomething)
 * 
 * // Anonymous implementation
 * val task3 = object : Runnable {
 *     override fun run() {
 *         println("Task 3 executing")
 *     }
 * }
 * 
 * // Execute in thread
 * val thread = Thread(task1)
 * thread.start()
 * thread.join()
 * 
 * // Execute directly
 * task1.run()
 * ```
 */
fun interface Runnable {
    /**
     * Executes the task when called.
     *
     * ## Execution Context
     * This method will be called in whatever thread context the caller provides:
     * - Main thread: If called directly from main
     * - Background thread: If submitted to a thread pool
     * - Custom thread: If wrapped in a Thread instance
     *
     * ## Error Handling
     * Any uncaught exceptions thrown from this method will:
     * - Terminate the current thread (if running in a separate thread)
     * - Propagate to the caller (if called directly)
     * - Be logged by the thread's UncaughtExceptionHandler (if configured)
     *
     * ## Best Practices
     * - Keep the implementation focused and simple
     * - Handle exceptions appropriately within the method
     * - Don't assume which thread this will run on
     * - Consider thread safety if accessing shared state
     * - Prefer suspend functions for new async code
     *
     * ## Thread Safety
     * The implementation of this method should be thread-safe if the Runnable
     * instance might be executed concurrently from multiple threads.
     */
    fun run()
}
