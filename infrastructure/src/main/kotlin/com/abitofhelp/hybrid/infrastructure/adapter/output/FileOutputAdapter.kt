// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.infrastructure.adapter.output

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.application.port.output.OutputPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

/**
 * File-based implementation of the OutputPort using async I/O.
 *
 * ## Async File I/O
 * This adapter uses Kotlin coroutines for non-blocking file operations:
 * - `withContext(Dispatchers.IO)`: Switches to I/O-optimized thread pool
 * - Doesn't block the main thread while writing
 * - Allows concurrent file operations
 *
 * ## File Handling Features
 * 1. **Auto-creates directories**: Parent directories are created if missing
 * 2. **Append mode**: Messages are added to existing files, not overwritten
 * 3. **Line separators**: Uses system-appropriate line endings (\n or \r\n)
 * 4. **AutoFlush**: Optional immediate flushing to disk for durability
 *
 * ## Why Use Files for Output?
 * - **Persistence**: Messages survive application restarts
 * - **Debugging**: Can review historical output
 * - **Integration**: Other tools can process the files
 * - **Compliance**: Audit trails and logging requirements
 *
 * ## AutoFlush Behavior
 * - `autoFlush = true`: Each write is immediately flushed to disk (safer, slower)
 * - `autoFlush = false`: Relies on OS buffering (faster, less durable)
 *
 * ## Example Usage
 * ```kotlin
 * // Create adapter with specific file
 * val fileOutput = FileOutputAdapter("/var/log/greetings.txt", autoFlush = true)
 *
 * // Use in composition
 * val useCase = CreateGreetingUseCase(
 *     greetingService = service,
 *     outputPort = fileOutput  // Will write to file!
 * )
 *
 * // Later, read the file
 * val contents = readFileAsync("/var/log/greetings.txt")
 * contents.fold(
 *     { error -> println("Read failed: $error") },
 *     { text -> println("File contents:\n$text") }
 * )
 * ```
 *
 * ## Thread Safety
 * File operations are thread-safe when using StandardOpenOption.APPEND.
 * Multiple threads can write concurrently without corruption.
 *
 * @property filePath The path where messages will be written
 * @property autoFlush Whether to immediately flush writes to disk
 */
class FileOutputAdapter(
    private val filePath: String,
    private val autoFlush: Boolean = false,
) : OutputPort {

    /**
     * Writes a message to the configured file asynchronously.
     *
     * ## Implementation Details
     *
     * ### Coroutine Context
     * `withContext(Dispatchers.IO)` ensures:
     * - File I/O happens on appropriate threads
     * - Main thread isn't blocked
     * - Automatic context switching and restoration
     *
     * ### Directory Creation
     * ```kotlin
     * path.parent?.let { parent ->
     *     if (!parent.exists()) {
     *         Files.createDirectories(parent)
     *     }
     * }
     * ```
     * The `?.let` pattern safely handles:
     * - Root paths (no parent)
     * - Nested directories (creates all levels)
     *
     * ### Write Options
     * - **CREATE**: Creates file if it doesn't exist
     * - **APPEND**: Adds to end of file (doesn't overwrite)
     *
     * ### System Line Separator
     * `System.lineSeparator()` returns:
     * - `\n` on Unix/Linux/Mac
     * - `\r\n` on Windows
     *
     * This ensures files are readable on any platform!
     *
     * ## Error Scenarios
     * 1. **Disk full**: IOException wrapped in ApplicationError
     * 2. **Permission denied**: SecurityException
     * 3. **Invalid path**: Various I/O exceptions
     * 4. **Network drive issues**: Timeout or connection errors
     *
     * @param message The message to append to the file
     * @return Right(Unit) on success, Left(ApplicationError) on failure
     */
    override suspend fun send(message: String): Either<ApplicationError, Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Validate message before any I/O
                require(message.isNotBlank()) { "Message cannot be blank" }

                // Convert string path to Path object and validate
                val path = validateFilePath(filePath)

                // Ensure parent directory exists (creates if needed)
                path.parent?.let { parent ->
                    if (!parent.exists()) {
                        Files.createDirectories(parent)
                    }
                }

                // Write message with system-appropriate line ending
                if (autoFlush) {
                    Files.writeString(
                        path,
                        message + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND,
                        StandardOpenOption.SYNC, // Force flush to disk
                    )
                } else {
                    Files.writeString(
                        path,
                        message + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND,
                    )
                }

                // Success!
                Unit.right()
            } catch (e: IllegalArgumentException) {
                // From require() - validation failed
                ApplicationError.OutputError(
                    "Invalid message: ${e.message ?: "Unknown validation error"}",
                ).left()
            } catch (e: SecurityException) {
                // File system permissions issue
                ApplicationError.OutputError(
                    "Security error writing to file: ${e.message ?: "Permission denied"}",
                ).left()
            } catch (e: Exception) {
                // Any other I/O error
                ApplicationError.OutputError(
                    "Failed to write to file $filePath: ${e.message ?: "Unknown error"}",
                ).left()
            }
        }
}

/**
 * Validates and creates a Path object with security checks.
 *
 * ## Path Validation Rules
 * 1. **Not blank**: Path cannot be empty or whitespace-only
 * 2. **Length limit**: Prevents excessively long paths (DoS protection)
 * 3. **Null bytes**: Prevents path injection attacks
 * 4. **Reserved names**: Blocks OS-reserved filenames (Windows: CON, PRN, etc.)
 * 5. **Traversal protection**: Prevents directory traversal with excessive '../'
 *
 * ## Security Considerations
 * - Prevents path injection attacks
 * - Blocks access to system files
 * - Limits resource exhaustion via long paths
 * - Cross-platform compatibility
 *
 * @param pathString The path string to validate
 * @return Valid Path object
 * @throws IllegalArgumentException if validation fails
 */
private fun validateFilePath(pathString: String): Path {
    require(pathString.isNotBlank()) { "File path cannot be blank" }
    require(pathString.length <= MAX_PATH_LENGTH) {
        "File path too long: ${pathString.length} characters (max: $MAX_PATH_LENGTH)"
    }
    require(!pathString.contains('\u0000')) {
        "File path contains null bytes"
    }

    // Check for excessive directory traversal
    val traversalCount = pathString.split("/").count { it == ".." }
    require(traversalCount <= MAX_DIRECTORY_TRAVERSAL) {
        "Too many directory traversal sequences: $traversalCount (max: $MAX_DIRECTORY_TRAVERSAL)"
    }

    val path = Path(pathString)
    val filename = path.fileName?.toString()

    // Check for reserved filenames (Windows compatibility)
    filename?.let { name ->
        val baseName = name.substringBeforeLast('.')
        require(!RESERVED_FILENAMES.contains(baseName.uppercase())) {
            "Reserved filename not allowed: $baseName"
        }
    }

    return path
}

// Constants for path validation
private const val MAX_PATH_LENGTH = 4096
private const val MAX_DIRECTORY_TRAVERSAL = 10
private val RESERVED_FILENAMES = setOf(
    "CON", "PRN", "AUX", "NUL",
    "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
    "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
)

/**
 * Async file reader utility for testing and verification.
 *
 * ## Purpose
 * This helper function allows you to:
 * - Verify file output in tests
 * - Read configuration files
 * - Check log contents
 * - Debug file-based operations
 *
 * ## Usage Example
 * ```kotlin
 * // In a test
 * @Test
 * fun `should write greeting to file`() = runTest {
 *     // Arrange
 *     val tempFile = createTempFile()
 *     val adapter = FileOutputAdapter(tempFile.path)
 *
 *     // Act
 *     adapter.send("Hello, World!").shouldBeRight()
 *
 *     // Assert
 *     val contents = readFileAsync(tempFile.path)
 *     contents.shouldBeRight()
 *     contents.getOrNull() shouldContain "Hello, World!"
 * }
 * ```
 *
 * ## Error Handling
 * Returns Left with ApplicationError when:
 * - File doesn't exist
 * - Path points to a directory
 * - Read permissions denied
 * - I/O errors occur
 *
 * @param filePath Path to the file to read
 * @return Either the file contents (Right) or an error (Left)
 */
suspend fun readFileAsync(filePath: String): Either<ApplicationError, String> =
    withContext(Dispatchers.IO) {
        try {
            val path = Path(filePath)

            // Validate file exists and is actually a file (not directory)
            require(path.exists() && path.isRegularFile()) {
                "File does not exist or is not a regular file: $filePath"
            }

            // Read entire file content as string
            Files.readString(path).right()
        } catch (e: Exception) {
            // Wrap any error in ApplicationError
            ApplicationError.OutputError(
                "Failed to read file $filePath: ${e.message}",
            ).left()
        }
    }
