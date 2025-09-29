// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - SystemSignals
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

/**
 * Abstraction for system signal handling and shutdown hooks.
 *
 * This interface allows the application to register shutdown handlers in a
 * testable and cross-platform way, avoiding direct dependencies on internal
 * JVM APIs like sun.misc.Signal.
 *
 * @since 1.0.0
 */
interface SystemSignals {
    /**
     * Installs a shutdown handler that will be called when the application
     * should gracefully terminate.
     *
     * @param onShutdown Callback to execute during shutdown
     */
    fun install(onShutdown: () -> Unit)
}

/**
 * JVM implementation of SystemSignals using shutdown hooks and optionally
 * sun.misc.Signal for better signal handling.
 *
 * This implementation:
 * - Always uses standard JVM shutdown hooks (cross-platform, reliable)
 * - Optionally uses sun.misc.Signal via reflection (better UX on Unix)
 * - Gracefully degrades if internal APIs are unavailable
 *
 * @param useInternalSignalApi Whether to attempt using sun.misc.Signal for
 *                           immediate signal handling (SIGINT, SIGTERM)
 * @since 1.0.0
 */
class JvmSignals(
    private val useInternalSignalApi: Boolean = true,
) : SystemSignals {

    override fun install(onShutdown: () -> Unit) {
        // Always install shutdown hook as fallback
        Runtime.getRuntime().addShutdownHook(
            Thread {
                onShutdown()
            },
        )

        // Optionally try to install immediate signal handlers
        if (useInternalSignalApi) {
            installSignalHandlers(onShutdown)
        }
    }

    /**
     * Attempts to install signal handlers using sun.misc.Signal via reflection.
     * If this fails (module system, security, or platform limitations),
     * falls back gracefully to shutdown hooks only.
     */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private fun installSignalHandlers(onShutdown: () -> Unit) {
        try {
            val signalClass = Class.forName("sun.misc.Signal")
            val handlerClass = Class.forName("sun.misc.SignalHandler")

            // Create signals
            val sigInt = signalClass.getConstructor(String::class.java).newInstance("INT")
            val sigTerm = signalClass.getConstructor(String::class.java).newInstance("TERM")

            // Create handler proxy
            val handler = java.lang.reflect.Proxy.newProxyInstance(
                handlerClass.classLoader,
                arrayOf(handlerClass),
            ) { _, _, _ ->
                onShutdown()
                null
            }

            // Install handlers
            val handleMethod = signalClass.getMethod("handle", signalClass, handlerClass)
            handleMethod.invoke(null, sigInt, handler)
            handleMethod.invoke(null, sigTerm, handler)
        } catch (e: Throwable) {
            // Gracefully degrade if signal handling unavailable - shutdown hook will still work
            // This could fail due to:
            // - Module system restrictions
            // - Security manager
            // - Platform differences (Windows)
            // - ClassNotFound on custom JVMs
            // Exception intentionally not propagated to maintain graceful degradation
            // Shutdown hook will still work even if signal handling fails
            Unit
        }
    }
}

/**
 * Test-friendly implementation that does nothing.
 * Use this in unit tests to avoid installing global signal handlers.
 *
 * @since 1.0.0
 */
class NoOpSignals : SystemSignals {
    override fun install(onShutdown: () -> Unit) {
        // Intentionally do nothing - for testing
    }
}
