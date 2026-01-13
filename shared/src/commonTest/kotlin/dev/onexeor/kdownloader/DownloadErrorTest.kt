package dev.onexeor.kdownloader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadErrorTest {

    @Test
    fun networkErrorHasMessage() {
        val error = DownloadError.Network("Connection timeout")
        assertEquals("Connection timeout", error.message)
        assertNull(error.cause)
    }

    @Test
    fun networkErrorCanHaveCause() {
        val cause = RuntimeException("Underlying error")
        val error = DownloadError.Network("Connection failed", cause)
        assertEquals("Connection failed", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun httpErrorHasStatusCode() {
        val error = DownloadError.Http(404, "Not Found")
        assertEquals(404, error.statusCode)
        assertEquals("Not Found", error.message)
    }

    @Test
    fun storageErrorHasMessage() {
        val error = DownloadError.Storage("Disk full")
        assertEquals("Disk full", error.message)
    }

    @Test
    fun invalidUrlErrorHasMessage() {
        val error = DownloadError.InvalidUrl("Malformed URL: abc")
        assertEquals("Malformed URL: abc", error.message)
    }

    @Test
    fun cancelledErrorHasDefaultMessage() {
        val error = DownloadError.Cancelled
        assertEquals("Download cancelled", error.message)
    }

    @Test
    fun unknownErrorHasMessage() {
        val error = DownloadError.Unknown("Something went wrong")
        assertEquals("Something went wrong", error.message)
    }

    @Test
    fun errorsCanBeUsedInWhenExpression() {
        val errors: List<DownloadError> = listOf(
            DownloadError.Network("test"),
            DownloadError.Http(500, "Server error"),
            DownloadError.Storage("test"),
            DownloadError.InvalidUrl("test"),
            DownloadError.Cancelled,
            DownloadError.Unknown("test")
        )

        errors.forEach { error ->
            val result = when (error) {
                is DownloadError.Network -> "network"
                is DownloadError.Http -> "http-${error.statusCode}"
                is DownloadError.Storage -> "storage"
                is DownloadError.InvalidUrl -> "invalid-url"
                is DownloadError.Cancelled -> "cancelled"
                is DownloadError.Unknown -> "unknown"
            }
            assertTrue(result.isNotEmpty())
        }
    }

    @Test
    fun httpErrorCoversCommonStatusCodes() {
        val codes = listOf(400, 401, 403, 404, 500, 502, 503)
        codes.forEach { code ->
            val error = DownloadError.Http(code, "Error $code")
            assertEquals(code, error.statusCode)
        }
    }
}
