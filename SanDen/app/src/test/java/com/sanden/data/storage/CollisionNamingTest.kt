package com.sanden.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CollisionNamingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testNoCollision_returnsSameFile() {
        val nonExistent = File(tempFolder.root, "photo.jpg")
        val resolved = SafeFileOperations.resolveNameCollision(nonExistent)
        assertEquals(nonExistent.absolutePath, resolved.absolutePath)
    }

    @Test
    fun testSingleCollision_appendsOne() {
        val original = tempFolder.newFile("document.pdf")
        val resolved = SafeFileOperations.resolveNameCollision(original)

        val expected = File(tempFolder.root, "document (1).pdf")
        assertEquals(expected.name, resolved.name)
        assertFalse(resolved.exists())
    }

    @Test
    fun testMultipleCollisions_appendsNextAvailableIndex() {
        val f0 = tempFolder.newFile("song.mp3")
        val f1 = tempFolder.newFile("song (1).mp3")
        val f2 = tempFolder.newFile("song (2).mp3")

        val resolved = SafeFileOperations.resolveNameCollision(f0)
        assertEquals("song (3).mp3", resolved.name)
    }

    @Test
    fun testFileWithoutExtension_appendsIndexDirectly() {
        val original = tempFolder.newFile("LICENSE")
        val resolved = SafeFileOperations.resolveNameCollision(original)

        assertEquals("LICENSE (1)", resolved.name)
    }

    @Test
    fun testFileWithMultipleDots() {
        val original = tempFolder.newFile("backup.tar.gz")
        val resolved = SafeFileOperations.resolveNameCollision(original)

        assertEquals("backup.tar (1).gz", resolved.name)
    }

    @Test
    fun testHiddenFileCollision() {
        val hidden = tempFolder.newFile(".nomedia")
        val resolved = SafeFileOperations.resolveNameCollision(hidden)

        // .nomedia: name is ".nomedia", extension is ""
        assertTrue(resolved.name.contains("(1)"))
    }

    @Test
    fun testSpecialCharactersInFileName() {
        val original = tempFolder.newFile("My File [2026] #1 @home.txt")
        val resolved = SafeFileOperations.resolveNameCollision(original)

        assertEquals("My File [2026] #1 @home (1).txt", resolved.name)
    }

    @Test
    fun testUnicodeAndEmojiInFileName() {
        val original = tempFolder.newFile("vacation_🏖️_tokyo.jpg")
        val resolved = SafeFileOperations.resolveNameCollision(original)

        assertEquals("vacation_🏖️_tokyo (1).jpg", resolved.name)
    }

    @Test
    fun testDeeplyNestedDirectory() {
        val subDir = tempFolder.newFolder("level1", "level2", "level3")
        val original = File(subDir, "deep.dat").apply { createNewFile() }

        val resolved = SafeFileOperations.resolveNameCollision(original)
        assertEquals(File(subDir, "deep (1).dat").absolutePath, resolved.absolutePath)
    }

    @Test
    fun testGapInCounters_usesLowestAvailableCounter() {
        val f0 = tempFolder.newFile("archive.zip")
        val f1 = tempFolder.newFile("archive (1).zip")
        // Note: archive (2).zip does NOT exist
        val f3 = tempFolder.newFile("archive (3).zip")

        val resolved = SafeFileOperations.resolveNameCollision(f0)
        assertEquals("archive (2).zip", resolved.name)
    }

    @Test
    fun testVeryLongFileName() {
        val longName = "a".repeat(120) + ".log"
        val original = tempFolder.newFile(longName)
        val resolved = SafeFileOperations.resolveNameCollision(original)

        val expected = "a".repeat(120) + " (1).log"
        assertEquals(expected, resolved.name)
    }

    @Test
    fun testNonExistentParent_handledSafely() {
        val weirdFile = File("/path_that_is_just_root")
        val resolved = SafeFileOperations.resolveNameCollision(weirdFile)
        assertEquals(weirdFile, resolved)
    }
}
