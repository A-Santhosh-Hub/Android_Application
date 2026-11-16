package com.sanden.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class SafeFileOperationsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testStreamCopy_basic() {
        val data = "Hello, SanDen vault storage engine!".toByteArray(Charsets.UTF_8)
        val input = ByteArrayInputStream(data)
        val output = ByteArrayOutputStream()

        val copied = SafeFileOperations.streamCopy(input, output, data.size.toLong())

        assertEquals(data.size.toLong(), copied)
        assertEquals("Hello, SanDen vault storage engine!", output.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun testStreamCopy_progressCallbackAccuracy() {
        val data = ByteArray(1024 * 64) { (it % 128).toByte() }
        val input = ByteArrayInputStream(data)
        val output = ByteArrayOutputStream()

        val progressUpdates = mutableListOf<Float>()
        SafeFileOperations.streamCopy(
            input = input,
            output = output,
            totalSize = data.size.toLong(),
            bufferSize = 8192,
            onProgress = { progressUpdates.add(it) }
        )

        assertTrue(progressUpdates.isNotEmpty())
        assertEquals(1.0f, progressUpdates.last(), 0.001f)
        // Ensure monotonically increasing
        for (i in 1 until progressUpdates.size) {
            assertTrue(progressUpdates[i] >= progressUpdates[i - 1])
        }
    }

    @Test
    fun testStreamCopy_emptyStream() {
        val input = ByteArrayInputStream(ByteArray(0))
        val output = ByteArrayOutputStream()

        val copied = SafeFileOperations.streamCopy(input, output, 0L)
        assertEquals(0L, copied)
        assertEquals(0, output.size())
    }

    @Test
    fun testCalculateCRC32_consistency() {
        val file1 = tempFolder.newFile("sample1.bin").apply {
            writeBytes("Vault CRC32 Verification Test".toByteArray())
        }
        val file2 = tempFolder.newFile("sample2.bin").apply {
            writeBytes("Vault CRC32 Verification Test".toByteArray())
        }
        val file3 = tempFolder.newFile("sample3.bin").apply {
            writeBytes("Different content entirely".toByteArray())
        }

        val crc1 = SafeFileOperations.calculateCRC32(file1)
        val crc2 = SafeFileOperations.calculateCRC32(file2)
        val crc3 = SafeFileOperations.calculateCRC32(file3)

        assertEquals(crc1, crc2)
        assertTrue(crc1 != crc3)
    }

    @Test
    fun testVerifyFileCopy_matchingFiles() {
        val source = tempFolder.newFile("source.bin").apply {
            writeBytes(ByteArray(1024 * 16) { 0x42 })
        }
        val dest = tempFolder.newFile("dest.bin").apply {
            writeBytes(ByteArray(1024 * 16) { 0x42 })
        }

        assertTrue(SafeFileOperations.verifyFileCopy(source, dest))
    }

    @Test
    fun testVerifyFileCopy_mismatchedSize() {
        val source = tempFolder.newFile("source.bin").apply {
            writeBytes(ByteArray(100) { 0x11 })
        }
        val dest = tempFolder.newFile("dest.bin").apply {
            writeBytes(ByteArray(50) { 0x11 })
        }

        assertFalse(SafeFileOperations.verifyFileCopy(source, dest))
    }

    @Test
    fun testVerifyFileCopy_mismatchedContentSameSize() {
        val source = tempFolder.newFile("source.bin").apply {
            writeBytes(ByteArray(100) { 0x11 })
        }
        val dest = tempFolder.newFile("dest.bin").apply {
            writeBytes(ByteArray(100) { 0x22 })
        }

        assertFalse(SafeFileOperations.verifyFileCopy(source, dest))
    }

    @Test
    fun testSafeMoveFile_sameDirectory() {
        val source = tempFolder.newFile("move_source.txt").apply {
            writeText("Secret data to be kept safe")
        }
        val dest = File(tempFolder.root, "move_dest.txt")

        val result = SafeFileOperations.safeMoveFile(source, dest)

        assertTrue(result.isSuccess)
        assertFalse(source.exists())
        assertTrue(dest.exists())
        assertEquals("Secret data to be kept safe", dest.readText())
    }

    @Test
    fun testSafeMoveFile_acrossDirectories() {
        val dir1 = tempFolder.newFolder("inbox")
        val dir2 = tempFolder.newFolder("vault_storage")

        val source = File(dir1, "photo.heic").apply {
            writeBytes(ByteArray(4096) { 0x7F })
        }
        val dest = File(dir2, "photo.heic")

        val result = SafeFileOperations.safeMoveFile(source, dest)

        assertTrue(result.isSuccess)
        assertFalse(source.exists())
        assertTrue(dest.exists())
        assertEquals(4096L, dest.length())
    }

    @Test
    fun testSafeMoveFile_nonExistentSource_fails() {
        val nonExistent = File(tempFolder.root, "does_not_exist.bin")
        val dest = File(tempFolder.root, "out.bin")

        val result = SafeFileOperations.safeMoveFile(nonExistent, dest)
        assertTrue(result.isFailure)
    }

    @Test
    fun testSafeMoveFile_neverDeletesSourceIfCopyFails() {
        val source = tempFolder.newFile("safe_source.txt").apply {
            writeText("Data must not be lost")
        }
        val blockerFile = tempFolder.newFile("not_a_directory.txt")
        val impossibleDest = File(blockerFile, "test.txt")

        val result = SafeFileOperations.safeMoveFile(source, impossibleDest)

        assertTrue(result.isFailure)
        assertTrue("Source file MUST be preserved upon move failure!", source.exists())
        assertEquals("Data must not be lost", source.readText())
    }

    @Test
    fun testLargeFileStreaming_simulated() {
        val largeDataSize = 1024 * 1024 * 2 // 2MB test stream
        val sourceFile = tempFolder.newFile("large.bin").apply {
            outputStream().buffered().use { out ->
                val chunk = ByteArray(8192) { (it % 256).toByte() }
                repeat(largeDataSize / chunk.size) {
                    out.write(chunk)
                }
            }
        }
        val destFile = File(tempFolder.root, "large_copy.bin")

        sourceFile.inputStream().buffered().use { input ->
            destFile.outputStream().buffered().use { output ->
                val copied = SafeFileOperations.streamCopy(input, output, sourceFile.length())
                assertEquals(sourceFile.length(), copied)
            }
        }

        assertTrue(SafeFileOperations.verifyFileCopy(sourceFile, destFile))
    }
}
