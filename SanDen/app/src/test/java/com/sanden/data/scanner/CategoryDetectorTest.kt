package com.sanden.data.scanner

import com.sanden.domain.model.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryDetectorTest {

    // ── Images ──────────────────────────────────────────────────────────

    @Test
    fun testDetectCategory_jpg() {
        assertEquals(FileCategory.IMAGE, CategoryDetector.detectCategory("photo.jpg"))
    }

    @Test
    fun testDetectCategory_jpeg() {
        assertEquals(FileCategory.IMAGE, CategoryDetector.detectCategory("vacation.jpeg"))
    }

    @Test
    fun testDetectCategory_png() {
        assertEquals(FileCategory.IMAGE, CategoryDetector.detectCategory("screenshot.png"))
    }

    @Test
    fun testDetectCategory_heic() {
        assertEquals(FileCategory.IMAGE, CategoryDetector.detectCategory("IMG_2026.heic"))
    }

    @Test
    fun testDetectCategory_webp() {
        assertEquals(FileCategory.IMAGE, CategoryDetector.detectCategory("banner.webp"))
    }

    @Test
    fun testDetectCategory_avif() {
        assertEquals(FileCategory.IMAGE, CategoryDetector.detectCategory("artwork.avif"))
    }

    @Test
    fun testDetectCategory_svg() {
        assertEquals(FileCategory.IMAGE, CategoryDetector.detectCategory("vector_logo.svg"))
    }

    @Test
    fun testDetectCategory_raw() {
        assertEquals(FileCategory.IMAGE, CategoryDetector.detectCategory("camera_shot.raw"))
    }

    // ── Audio ───────────────────────────────────────────────────────────

    @Test
    fun testDetectCategory_mp3() {
        assertEquals(FileCategory.AUDIO, CategoryDetector.detectCategory("song.mp3"))
    }

    @Test
    fun testDetectCategory_flac() {
        assertEquals(FileCategory.AUDIO, CategoryDetector.detectCategory("track_lossless.flac"))
    }

    @Test
    fun testDetectCategory_opus() {
        assertEquals(FileCategory.AUDIO, CategoryDetector.detectCategory("voice_note.opus"))
    }

    @Test
    fun testDetectCategory_ogg() {
        assertEquals(FileCategory.AUDIO, CategoryDetector.detectCategory("podcast.ogg"))
    }

    @Test
    fun testDetectCategory_aac() {
        assertEquals(FileCategory.AUDIO, CategoryDetector.detectCategory("audio.aac"))
    }

    @Test
    fun testDetectCategory_m4a() {
        assertEquals(FileCategory.AUDIO, CategoryDetector.detectCategory("recording.m4a"))
    }

    @Test
    fun testDetectCategory_wav() {
        assertEquals(FileCategory.AUDIO, CategoryDetector.detectCategory("sample.wav"))
    }

    // ── Video ───────────────────────────────────────────────────────────

    @Test
    fun testDetectCategory_mp4() {
        assertEquals(FileCategory.VIDEO, CategoryDetector.detectCategory("movie.mp4"))
    }

    @Test
    fun testDetectCategory_mkv() {
        assertEquals(FileCategory.VIDEO, CategoryDetector.detectCategory("episode.mkv"))
    }

    @Test
    fun testDetectCategory_webm() {
        assertEquals(FileCategory.VIDEO, CategoryDetector.detectCategory("stream.webm"))
    }

    @Test
    fun testDetectCategory_avi() {
        assertEquals(FileCategory.VIDEO, CategoryDetector.detectCategory("clip.avi"))
    }

    @Test
    fun testDetectCategory_mov() {
        assertEquals(FileCategory.VIDEO, CategoryDetector.detectCategory("recorded.mov"))
    }

    @Test
    fun testDetectCategory_3gp() {
        assertEquals(FileCategory.VIDEO, CategoryDetector.detectCategory("old_recording.3gp"))
    }

    // ── Documents ───────────────────────────────────────────────────────

    @Test
    fun testDetectCategory_pdf() {
        assertEquals(FileCategory.DOCUMENT, CategoryDetector.detectCategory("contract.pdf"))
    }

    @Test
    fun testDetectCategory_docx() {
        assertEquals(FileCategory.DOCUMENT, CategoryDetector.detectCategory("report.docx"))
    }

    @Test
    fun testDetectCategory_xlsx() {
        assertEquals(FileCategory.DOCUMENT, CategoryDetector.detectCategory("budget.xlsx"))
    }

    @Test
    fun testDetectCategory_epub() {
        assertEquals(FileCategory.DOCUMENT, CategoryDetector.detectCategory("novel.epub"))
    }

    @Test
    fun testDetectCategory_txt() {
        assertEquals(FileCategory.DOCUMENT, CategoryDetector.detectCategory("notes.txt"))
    }

    @Test
    fun testDetectCategory_md() {
        assertEquals(FileCategory.DOCUMENT, CategoryDetector.detectCategory("README.md"))
    }

    @Test
    fun testDetectCategory_json() {
        assertEquals(FileCategory.DOCUMENT, CategoryDetector.detectCategory("config.json"))
    }

    // ── Others / Archives ───────────────────────────────────────────────

    @Test
    fun testDetectCategory_apk() {
        assertEquals(FileCategory.OTHER, CategoryDetector.detectCategory("installer.apk"))
    }

    @Test
    fun testDetectCategory_zip() {
        assertEquals(FileCategory.OTHER, CategoryDetector.detectCategory("backup.zip"))
    }

    @Test
    fun testDetectCategory_rar() {
        assertEquals(FileCategory.OTHER, CategoryDetector.detectCategory("archive.rar"))
    }

    @Test
    fun testDetectCategory_7z() {
        assertEquals(FileCategory.OTHER, CategoryDetector.detectCategory("bundle.7z"))
    }

    // ── Edge Cases ──────────────────────────────────────────────────────

    @Test
    fun testDetectCategory_caseInsensitivity() {
        assertEquals(FileCategory.IMAGE, CategoryDetector.detectCategory("PICTURE.JPG"))
        assertEquals(FileCategory.AUDIO, CategoryDetector.detectCategory("Song.MP3"))
        assertEquals(FileCategory.DOCUMENT, CategoryDetector.detectCategory("Document.PDF"))
        assertEquals(FileCategory.VIDEO, CategoryDetector.detectCategory("Clip.MKV"))
    }

    @Test
    fun testDetectCategory_dotFile() {
        assertEquals(FileCategory.OTHER, CategoryDetector.detectCategory(".nomedia"))
        assertEquals(FileCategory.OTHER, CategoryDetector.detectCategory(".gitignore"))
    }

    @Test
    fun testDetectCategory_noExtension() {
        assertEquals(FileCategory.OTHER, CategoryDetector.detectCategory("filename_without_ext"))
    }

    @Test
    fun testDetectCategory_multipleDots() {
        assertEquals(FileCategory.DOCUMENT, CategoryDetector.detectCategory("archive.tar.gz.pdf"))
        assertEquals(FileCategory.OTHER, CategoryDetector.detectCategory("archive.tar.gz"))
    }
}
