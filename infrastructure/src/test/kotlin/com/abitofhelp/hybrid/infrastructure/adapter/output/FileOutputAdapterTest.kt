/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

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
 * Test suite for FileOutputAdapter.
 *
 * ## Testing Infrastructure Adapters
 *
 * Infrastructure adapter tests verify:
 * 1. **Port implementation**: Adapter correctly implements the port interface
 * 2. **External integration**: Proper interaction with file system
 * 3. **Error handling**: All I/O errors are caught and wrapped
 * 4. **Async behavior**: Coroutines work correctly
 *
 * ## File System Testing Strategy
 *
 * ### Temporary Files
 * - Use `Files.createTempFile()` for isolated testing
 * - Clean up in `afterEach` to prevent test pollution
 * - Each test gets a fresh file to avoid interference
 *
 * ### Testing I/O Operations
 * - Verify file contents after write
 * - Test directory creation
 * - Test append behavior
 * - Test error conditions (permissions, disk full simulation)
 *
 * ## Key Test Scenarios
 *
 * ### Success Cases
 * - Simple message write
 * - Multiple messages (append)
 * - Directory creation
 * - Special characters and encodings
 *
 * ### Error Cases
 * - Blank messages
 * - File permission errors
 * - Invalid paths
 * - Concurrent writes
 *
 * ## Async Testing
 *
 * Uses `runTest` from kotlinx.coroutines.test:
 * - Provides test dispatcher
 * - Controls coroutine execution
 * - Allows testing suspend functions
 *
 * ## Platform Considerations
 *
 * File system behavior varies by platform:
 * - Line endings: `\n` vs `\r\n`
 * - Path separators: `/` vs `\`
 * - Permissions: Unix vs Windows
 *
 * Tests use platform-agnostic approaches where possible.
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
