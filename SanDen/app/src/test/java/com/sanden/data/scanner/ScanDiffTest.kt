package com.sanden.data.scanner

import com.sanden.domain.model.FileCategory
import com.sanden.domain.model.VaultFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDiffTest {

    private fun createFile(
        path: String,
        name: String,
        size: Long = 1024L,
        lastModified: Long = 1000L,
        isDirectory: Boolean = false,
        category: FileCategory = FileCategory.DOCUMENT
    ) = VaultFile(
        id = 0L,
        path = path,
        name = name,
        parentPath = "/vault",
        size = size,
        lastModified = lastModified,
        mimeType = null,
        category = category,
        isDirectory = isDirectory
    )

    @Test
    fun testEmptyOldIndex_allNewFilesAdded() {
        val current = listOf(
            createFile("/vault/a.txt", "a.txt"),
            createFile("/vault/b.jpg", "b.jpg", category = FileCategory.IMAGE)
        )
        val diff = ScanDiff(oldIndex = emptyList(), currentFiles = current).compute()

        assertEquals(2, diff.added.size)
        assertEquals(0, diff.removed.size)
        assertEquals(0, diff.modified.size)
        assertEquals(2, diff.totalFiles)
    }

    @Test
    fun testOldFiles_emptyNew_allRemoved() {
        val old = listOf(
            createFile("/vault/a.txt", "a.txt"),
            createFile("/vault/b.jpg", "b.jpg")
        )
        val diff = ScanDiff(oldIndex = old, currentFiles = emptyList()).compute()

        assertEquals(0, diff.added.size)
        assertEquals(2, diff.removed.size)
        assertEquals(0, diff.modified.size)
        assertEquals(0, diff.totalFiles)
    }

    @Test
    fun testSameFiles_noChanges() {
        val files = listOf(
            createFile("/vault/a.txt", "a.txt", size = 100L, lastModified = 200L),
            createFile("/vault/b.jpg", "b.jpg", size = 500L, lastModified = 300L)
        )
        val diff = ScanDiff(oldIndex = files, currentFiles = files).compute()

        assertEquals(0, diff.added.size)
        assertEquals(0, diff.removed.size)
        assertEquals(0, diff.modified.size)
        assertEquals(2, diff.totalFiles)
    }

    @Test
    fun testFileModified_sizeChanged() {
        val old = listOf(createFile("/vault/doc.pdf", "doc.pdf", size = 1000L, lastModified = 500L))
        val current = listOf(createFile("/vault/doc.pdf", "doc.pdf", size = 2000L, lastModified = 500L))

        val diff = ScanDiff(oldIndex = old, currentFiles = current).compute()

        assertEquals(0, diff.added.size)
        assertEquals(0, diff.removed.size)
        assertEquals(1, diff.modified.size)
        assertEquals("/vault/doc.pdf", diff.modified.first().path)
        assertEquals(2000L, diff.modified.first().size)
    }

    @Test
    fun testFileModified_lastModifiedChanged() {
        val old = listOf(createFile("/vault/doc.pdf", "doc.pdf", size = 1000L, lastModified = 500L))
        val current = listOf(createFile("/vault/doc.pdf", "doc.pdf", size = 1000L, lastModified = 800L))

        val diff = ScanDiff(oldIndex = old, currentFiles = current).compute()

        assertEquals(0, diff.added.size)
        assertEquals(0, diff.removed.size)
        assertEquals(1, diff.modified.size)
    }

    @Test
    fun testSimultaneousAddAndRemove() {
        val old = listOf(createFile("/vault/old.txt", "old.txt"))
        val current = listOf(createFile("/vault/new.txt", "new.txt"))

        val diff = ScanDiff(oldIndex = old, currentFiles = current).compute()

        assertEquals(1, diff.added.size)
        assertEquals("/vault/new.txt", diff.added.first().path)
        assertEquals(1, diff.removed.size)
        assertEquals("/vault/old.txt", diff.removed.first().path)
        assertEquals(0, diff.modified.size)
    }

    @Test
    fun testRename_detectedAsRemoveAndAdd() {
        val old = listOf(createFile("/vault/file_v1.txt", "file_v1.txt", size = 500L))
        val current = listOf(createFile("/vault/file_v2.txt", "file_v2.txt", size = 500L))

        val diff = ScanDiff(oldIndex = old, currentFiles = current).compute()

        assertEquals(1, diff.added.size)
        assertEquals("/vault/file_v2.txt", diff.added.first().path)
        assertEquals(1, diff.removed.size)
        assertEquals("/vault/file_v1.txt", diff.removed.first().path)
    }

    @Test
    fun testDirectoriesNotCountedInTotalFiles() {
        val current = listOf(
            createFile("/vault/subfolder", "subfolder", isDirectory = true),
            createFile("/vault/subfolder/file.txt", "file.txt", isDirectory = false)
        )
        val diff = ScanDiff(oldIndex = emptyList(), currentFiles = current).compute()

        assertEquals(2, diff.added.size)
        assertEquals(1, diff.totalFiles) // Only non-directory file
    }

    @Test
    fun testTotalSizeComputation() {
        val current = listOf(
            createFile("/vault/folder", "folder", size = 0L, isDirectory = true),
            createFile("/vault/a.bin", "a.bin", size = 3000L, isDirectory = false),
            createFile("/vault/b.bin", "b.bin", size = 7000L, isDirectory = false)
        )
        val diff = ScanDiff(oldIndex = emptyList(), currentFiles = current).compute()

        assertEquals(10000L, diff.totalSize)
    }

    @Test
    fun testLargeNumberOfFiles_performance() {
        val count = 2000
        val old = (1..count).map {
            createFile("/vault/file_$it.dat", "file_$it.dat", size = it.toLong(), lastModified = 1000L)
        }

        // 100 modified, 50 removed, 50 added
        val current = mutableListOf<VaultFile>()
        for (i in 1..(count - 50)) {
            if (i <= 100) {
                current.add(createFile("/vault/file_$i.dat", "file_$i.dat", size = (i * 2).toLong(), lastModified = 2000L))
            } else {
                current.add(createFile("/vault/file_$i.dat", "file_$i.dat", size = i.toLong(), lastModified = 1000L))
            }
        }
        for (i in (count + 1)..(count + 50)) {
            current.add(createFile("/vault/file_$i.dat", "file_$i.dat", size = i.toLong(), lastModified = 1000L))
        }

        val startTime = System.currentTimeMillis()
        val diff = ScanDiff(oldIndex = old, currentFiles = current).compute()
        val duration = System.currentTimeMillis() - startTime

        assertEquals(50, diff.added.size)
        assertEquals(50, diff.removed.size)
        assertEquals(100, diff.modified.size)
        assertTrue("Diff took too long: ${duration}ms", duration < 500)
    }
}
