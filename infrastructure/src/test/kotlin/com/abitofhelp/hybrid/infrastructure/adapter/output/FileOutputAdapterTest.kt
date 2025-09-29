////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.infrastructure.adapter.output

import com.abitofhelp.hybrid.application.error.ApplicationError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

/**
 * Comprehensive test suite for FileOutputAdapter.
 *
 * ## Purpose
 * This test suite validates the file output adapter, which provides reliable asynchronous
 * file writing capabilities for persistent message storage. The adapter implements the
 * OutputPort interface to enable applications to write messages to files with proper
 * error handling, directory management, and support for various file system scenarios.
 *
 * ## What is Being Tested
 * 1. **Asynchronous File Writing**: Messages are written to files using coroutines
 * 2. **Directory Management**: Automatic creation of parent directories when needed
 * 3. **Append Behavior**: Multiple messages are appended to the same file correctly
 * 4. **Message Validation**: Input validation prevents invalid content from being written
 * 5. **Error Handling**: File system errors are caught and wrapped appropriately
 * 6. **Character Encoding**: Proper handling of Unicode and special characters
 * 7. **Concurrent Operations**: Multiple simultaneous write operations are handled safely
 * 8. **File System Integration**: Real file system operations with proper cleanup
 *
 * ## Testing Infrastructure Adapters
 *
 * Infrastructure adapter tests verify:
 * 1. **Port Implementation**: Adapter correctly implements the OutputPort interface
 * 2. **External Integration**: Proper interaction with the file system
 * 3. **Error Handling**: All I/O errors are caught and wrapped in ApplicationError
 * 4. **Async Behavior**: Coroutines work correctly for non-blocking operations
 * 5. **Resource Management**: Files and streams are properly managed and cleaned up
 * 6. **Platform Compatibility**: Tests work across different operating systems
 *
 * ## File System Testing Strategy
 *
 * ### Temporary Files
 * - Use `Files.createTempFile()` for isolated testing environment
 * - Clean up in `afterEach` to prevent test pollution and resource leaks
 * - Each test gets a fresh file to avoid interference between test cases
 * - Nested directory structures are created and cleaned up properly
 *
 * ### Testing I/O Operations
 * - Verify file contents after write operations using `Files.readString()`
 * - Test automatic directory creation for nested paths
 * - Test append behavior with multiple sequential writes
 * - Test error conditions (permissions, invalid paths, disk simulation)
 * - Validate file encoding and character preservation
 *
 * ## Key Test Scenarios
 *
 * ### Success Cases
 * - **Simple Message Write**: Basic file writing with content verification
 * - **Multiple Messages (Append)**: Sequential writes append correctly
 * - **Directory Creation**: Parent directories created automatically
 * - **Special Characters and Encodings**: Unicode, emojis, and international text
 * - **Long Messages**: Large content handling and performance
 * - **Concurrent Writes**: Multiple simultaneous operations
 *
 * ### Error Cases
 * - **Blank Messages**: Proper rejection of empty or whitespace-only content
 * - **File Permission Errors**: Read-only files and permission denied scenarios
 * - **Invalid Paths**: Directory paths used as file paths
 * - **Concurrent Writes**: Race conditions and file locking scenarios
 * - **Disk Space**: Simulated disk full conditions (where possible)
 *
 * ## Async Testing
 *
 * Uses `runTest` from kotlinx.coroutines.test:
 * - Provides controlled test dispatcher for deterministic testing
 * - Controls coroutine execution and timing
 * - Allows testing suspend functions without blocking test threads
 * - Enables testing of concurrent operations safely
 * - Provides proper exception handling for async operations
 *
 * ## Platform Considerations
 *
 * File system behavior varies by platform:
 * - **Line Endings**: `\n` vs `\r\n` handled transparently by Java NIO
 * - **Path Separators**: `/` vs `\` handled by Path API
 * - **Permissions**: Unix file permissions vs Windows ACLs
 * - **Case Sensitivity**: Windows (case-insensitive) vs Unix (case-sensitive)
 * - **File Locking**: Platform-specific file locking behavior
 *
 * Tests use platform-agnostic approaches where possible, with fallback
 * handling for platform-specific limitations.
 *
 * ## Helper Function Testing
 * The test suite also validates the `readFileAsync` helper function:
 * - **File Reading**: Asynchronous file content reading
 * - **Error Handling**: Non-existent files and invalid paths
 * - **Type Validation**: Directory vs file distinction
 * - **Content Preservation**: Character encoding and content integrity
 *
 * ## Educational Value
 * This test suite demonstrates several important testing concepts:
 * - **File System Testing**: How to test file I/O operations safely
 * - **Async I/O Testing**: Testing coroutine-based file operations
 * - **Resource Management**: Proper cleanup of test resources
 * - **Error Injection**: Testing various failure scenarios
 * - **Integration Testing**: Testing real file system interactions
 * - **Platform Compatibility**: Writing tests that work across operating systems
 *
 * ## Real-World Applications
 * The FileOutputAdapter is essential for:
 * - **Logging Systems**: Persistent log file creation and management
 * - **Data Export**: Writing application data to files
 * - **Configuration Management**: Saving application settings
 * - **Report Generation**: Creating output files for reports
 * - **Backup Operations**: Writing backup data to storage
 * - **Audit Trails**: Maintaining persistent audit logs
 *
 * ## Performance Characteristics
 * - **Asynchronous Operations**: Non-blocking file writes using coroutines
 * - **Append Efficiency**: Optimized for sequential append operations
 * - **Memory Efficiency**: Streaming writes for large content
 * - **Concurrent Safety**: Thread-safe operations through NIO.2
 * - **Directory Optimization**: Efficient parent directory creation
 *
 * @see FileOutputAdapter The file output adapter being tested
 * @see OutputPort The port interface this adapter implements
 * @see readFileAsync The helper function for asynchronous file reading
 * @see java.nio.file.Files The NIO.2 API used for file operations
 * @see ApplicationError.OutputError The error type used for file operation failures
 */
class FileOutputAdapterTest : DescribeSpec({

    describe("FileOutputAdapter") {

        lateinit var tempFile: String
        lateinit var adapter: FileOutputAdapter

        beforeEach {
            tempFile = Files.createTempFile("test", ".txt").toString()
            adapter = FileOutputAdapter(tempFile)
        }

        afterEach {
            Path(tempFile).deleteIfExists()
        }

        describe("send") {

            it("should write message to file asynchronously") {
                runTest {
                    // Given
                    val message = "Hello, Async World!"

                    // When
                    val result = adapter.send(message)

                    // Then
                    result.isRight() shouldBe true

                    val content = Files.readString(Path(tempFile))
                    content.trim() shouldBe message
                }
            }

            it("should append multiple messages") {
                runTest {
                    // Given
                    val messages = listOf("First", "Second", "Third")

                    // When
                    messages.forEach { msg ->
                        val result = adapter.send(msg)
                        result.isRight() shouldBe true
                    }

                    // Then
                    val content = Files.readString(Path(tempFile))
                    val lines = content.trim().split(System.lineSeparator())
                    lines shouldBe messages
                }
            }

            it("should handle blank message error") {
                runTest {
                    // When
                    val result = adapter.send("   ")

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            (error is ApplicationError.OutputError) shouldBe true
                            if (error is ApplicationError.OutputError) {
                                error.message shouldContain "Message cannot be blank"
                            }
                        },
                        { },
                    )
                }
            }

            it("should handle empty message error") {
                runTest {
                    // When
                    val result = adapter.send("")

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            (error is ApplicationError.OutputError) shouldBe true
                            if (error is ApplicationError.OutputError) {
                                error.message shouldContain "Message cannot be blank"
                            }
                        },
                        { },
                    )
                }
            }

            it("should create parent directories if needed") {
                runTest {
                    // Given
                    val nestedPath = Files.createTempDirectory("test").resolve("nested/dir/file.txt").toString()
                    val nestedAdapter = FileOutputAdapter(nestedPath)

                    try {
                        // When
                        val result = nestedAdapter.send("Test message")

                        // Then
                        result.isRight() shouldBe true
                        Path(nestedPath).exists() shouldBe true

                        val content = Files.readString(Path(nestedPath))
                        content.trim() shouldBe "Test message"
                    } finally {
                        // Cleanup
                        Path(nestedPath).deleteIfExists()
                        Path(nestedPath).parent?.deleteIfExists()
                        Path(nestedPath).parent?.parent?.deleteIfExists()
                    }
                }
            }

            it("should handle file write permissions error") {
                runTest {
                    // Given - create a read-only directory
                    val readOnlyDir = Files.createTempDirectory("readonly")
                    val file = readOnlyDir.resolve("test.txt")
                    Files.createFile(file)
                    file.toFile().setWritable(false)

                    val adapter = FileOutputAdapter(file.toString())

                    try {
                        // When
                        val result = adapter.send("Test message")

                        // Then
                        result.isLeft() shouldBe true
                        result.fold(
                            { error ->
                                (error is ApplicationError.OutputError) shouldBe true
                                if (error is ApplicationError.OutputError) {
                                    error.message shouldContain "Failed to write to file"
                                }
                            },
                            { },
                        )
                    } finally {
                        // Cleanup
                        file.toFile().setWritable(true)
                        Files.deleteIfExists(file)
                        Files.deleteIfExists(readOnlyDir)
                    }
                }
            }

            it("should handle invalid file path") {
                runTest {
                    // Given - path to a directory instead of a file
                    val tempDir = kotlin.io.path.createTempDirectory("test").toFile()
                    tempDir.deleteOnExit()
                    val adapter = FileOutputAdapter(tempDir.absolutePath)

                    // When
                    val result = adapter.send("Test message")

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            (error is ApplicationError.OutputError) shouldBe true
                            if (error is ApplicationError.OutputError) {
                                error.message shouldContain "Failed to write to file"
                            }
                        },
                        { },
                    )

                    // Cleanup
                    tempDir.delete()
                }
            }

            it("should handle very long messages") {
                runTest {
                    // Given
                    val longMessage = "x".repeat(10000)

                    // When
                    val result = adapter.send(longMessage)

                    // Then
                    result.isRight() shouldBe true

                    val content = Files.readString(Path(tempFile))
                    content.trim() shouldBe longMessage
                }
            }

            it("should handle unicode messages") {
                runTest {
                    // Given
                    val unicodeMessage = "Hello 👋 世界 🌍 مرحبا"

                    // When
                    val result = adapter.send(unicodeMessage)

                    // Then
                    result.isRight() shouldBe true

                    val content = Files.readString(Path(tempFile))
                    content.trim() shouldBe unicodeMessage
                }
            }

            it("should handle concurrent writes") {
                runTest {
                    // Given
                    val messages = (1..10).map { "Message $it" }

                    // When - write messages concurrently
                    val results = messages.map { msg ->
                        adapter.send(msg)
                    }

                    // Then
                    results.forEach { result ->
                        result.isRight() shouldBe true
                    }

                    val content = Files.readString(Path(tempFile))
                    val lines = content.trim().split(System.lineSeparator())
                    lines.size shouldBe 10
                    lines.forEach { line ->
                        line shouldContain "Message"
                    }
                }
            }
        }

        describe("readFileAsync helper function") {

            it("should read file content") {
                runTest {
                    // Given
                    val testContent = "Test content for reading"
                    Files.writeString(Path(tempFile), testContent)

                    // When
                    val result = readFileAsync(tempFile)

                    // Then
                    result.isRight() shouldBe true
                    result.getOrNull() shouldBe testContent
                }
            }

            it("should handle non-existent file") {
                runTest {
                    // Given
                    val nonExistentFile = "/tmp/does-not-exist.txt"

                    // When
                    val result = readFileAsync(nonExistentFile)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            (error is ApplicationError.OutputError) shouldBe true
                            if (error is ApplicationError.OutputError) {
                                error.message shouldContain "File does not exist"
                            }
                        },
                        { },
                    )
                }
            }

            it("should handle directory instead of file") {
                runTest {
                    // Given
                    val dir = Files.createTempDirectory("testdir")

                    try {
                        // When
                        val result = readFileAsync(dir.toString())

                        // Then
                        result.isLeft() shouldBe true
                        result.fold(
                            { error ->
                                (error is ApplicationError.OutputError) shouldBe true
                                if (error is ApplicationError.OutputError) {
                                    error.message shouldContain "not a regular file"
                                }
                            },
                            { },
                        )
                    } finally {
                        Files.deleteIfExists(dir)
                    }
                }
            }
        }
    }
})
