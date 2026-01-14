package dev.onexeor.kdownloader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadRequestBuilderTest {

    @Test
    fun builderSetsUrl() {
        val request = DownloadRequestBuilder("https://example.com/file.zip").build()
        assertEquals("https://example.com/file.zip", request.url)
    }

    @Test
    fun builderSetsFileName() {
        val request = DownloadRequestBuilder("https://example.com/file.zip").apply {
            fileName = "custom.zip"
        }.build()
        assertEquals("custom.zip", request.fileName)
    }

    @Test
    fun builderSetsDirectory() {
        val request = DownloadRequestBuilder("https://example.com/file.zip").apply {
            directory = "/downloads"
        }.build()
        assertEquals("/downloads", request.directory)
    }

    @Test
    fun builderDefaultsToNoOverwrite() {
        val request = DownloadRequestBuilder("https://example.com/file.zip").build()
        assertEquals(false, request.overwrite)
    }

    @Test
    fun overwriteIfExistsSetsOverwriteTrue() {
        val request = DownloadRequestBuilder("https://example.com/file.zip").apply {
            overwriteIfExists()
        }.build()
        assertTrue(request.overwrite)
    }

    @Test
    fun wifiOnlySetsNetworkType() {
        val request = DownloadRequestBuilder("https://example.com/file.zip").apply {
            wifiOnly()
        }.build()
        assertEquals(NetworkType.WIFI_ONLY, request.networkType)
    }

    @Test
    fun headersDslAddsHeaders() {
        val request = DownloadRequestBuilder("https://example.com/file.zip").apply {
            headers {
                "X-Custom" to "value1"
                "Accept" to "application/json"
            }
        }.build()
        assertEquals("value1", request.headers["X-Custom"])
        assertEquals("application/json", request.headers["Accept"])
    }

    @Test
    fun authBearerSetsAuth() {
        val request = DownloadRequestBuilder("https://example.com/file.zip").apply {
            auth {
                bearer("my-token")
            }
        }.build()
        val auth = request.auth
        assertTrue(auth is Auth.Bearer)
        assertEquals("my-token", auth.token)
    }

    @Test
    fun authBasicSetsAuth() {
        val request = DownloadRequestBuilder("https://example.com/file.zip").apply {
            auth {
                basic("user", "pass")
            }
        }.build()
        val auth = request.auth
        assertTrue(auth is Auth.Basic)
        assertEquals("user", auth.username)
        assertEquals("pass", auth.password)
    }

    @Test
    fun defaultsAreNull() {
        val request = DownloadRequestBuilder("https://example.com/file.zip").build()
        assertNull(request.fileName)
        assertNull(request.directory)
        assertNull(request.auth)
        assertNull(request.networkType)
        assertTrue(request.headers.isEmpty())
    }
}
