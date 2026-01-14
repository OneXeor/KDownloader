package dev.onexeor.kdownloader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadStateTest {

    @Test
    fun pendingStateIsCorrectType() {
        val state: DownloadState = DownloadState.Pending
        assertTrue(state is DownloadState.Pending)
    }

    @Test
    fun downloadingStateContainsProgress() {
        val progress = DownloadProgress(50, 100, 50)
        val state = DownloadState.Downloading(progress)
        assertEquals(progress, state.progress)
    }

    @Test
    fun pausedStateContainsReason() {
        val state = DownloadState.Paused("Waiting for WiFi")
        assertEquals("Waiting for WiFi", state.reason)
    }

    @Test
    fun completedStateContainsFilePath() {
        val state = DownloadState.Completed("/path/to/file.zip")
        assertEquals("/path/to/file.zip", state.filePath)
    }

    @Test
    fun failedStateContainsError() {
        val error = DownloadError.Network("Connection failed")
        val state = DownloadState.Failed(error)
        assertEquals(error, state.error)
    }

    @Test
    fun cancelledStateIsCorrectType() {
        val state: DownloadState = DownloadState.Cancelled
        assertTrue(state is DownloadState.Cancelled)
    }

    @Test
    fun statesCanBeUsedInWhenExpression() {
        val states = listOf(
            DownloadState.Pending,
            DownloadState.Downloading(DownloadProgress.ZERO),
            DownloadState.Paused("test"),
            DownloadState.Completed("/path"),
            DownloadState.Failed(DownloadError.Cancelled),
            DownloadState.Cancelled
        )

        states.forEach { state ->
            val result = when (state) {
                is DownloadState.Pending -> "pending"
                is DownloadState.Downloading -> "downloading"
                is DownloadState.Paused -> "paused"
                is DownloadState.Completed -> "completed"
                is DownloadState.Failed -> "failed"
                is DownloadState.Cancelled -> "cancelled"
            }
            assertTrue(result.isNotEmpty())
        }
    }
}
