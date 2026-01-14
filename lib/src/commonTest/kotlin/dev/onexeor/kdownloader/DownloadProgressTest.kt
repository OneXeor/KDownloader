package dev.onexeor.kdownloader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadProgressTest {

    @Test
    fun zeroProgressHasCorrectDefaults() {
        val progress = DownloadProgress.ZERO
        assertEquals(0, progress.bytesDownloaded)
        assertEquals(-1, progress.totalBytes)
        assertEquals(-1, progress.percentage)
    }

    @Test
    fun isSizeKnownReturnsTrueWhenTotalBytesPositive() {
        val progress = DownloadProgress(
            bytesDownloaded = 100,
            totalBytes = 1000,
            percentage = 10
        )
        assertTrue(progress.isSizeKnown)
    }

    @Test
    fun isSizeKnownReturnsFalseWhenTotalBytesNegative() {
        val progress = DownloadProgress(
            bytesDownloaded = 100,
            totalBytes = -1,
            percentage = -1
        )
        assertFalse(progress.isSizeKnown)
    }

    @Test
    fun isSizeKnownReturnsFalseWhenTotalBytesZero() {
        val progress = DownloadProgress(
            bytesDownloaded = 0,
            totalBytes = 0,
            percentage = 0
        )
        assertFalse(progress.isSizeKnown)
    }

    @Test
    fun hasStartedReturnsTrueWhenBytesDownloaded() {
        val progress = DownloadProgress(
            bytesDownloaded = 1,
            totalBytes = 100,
            percentage = 1
        )
        assertTrue(progress.hasStarted)
    }

    @Test
    fun hasStartedReturnsFalseWhenNoBytesDownloaded() {
        val progress = DownloadProgress(
            bytesDownloaded = 0,
            totalBytes = 100,
            percentage = 0
        )
        assertFalse(progress.hasStarted)
    }

    @Test
    fun progressValuesAreCorrect() {
        val progress = DownloadProgress(
            bytesDownloaded = 500,
            totalBytes = 1000,
            percentage = 50
        )
        assertEquals(500, progress.bytesDownloaded)
        assertEquals(1000, progress.totalBytes)
        assertEquals(50, progress.percentage)
    }
}
