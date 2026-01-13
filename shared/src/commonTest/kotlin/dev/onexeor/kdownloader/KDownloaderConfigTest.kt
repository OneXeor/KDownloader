package dev.onexeor.kdownloader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KDownloaderConfigTest {

    @Test
    fun defaultConfigHasNullDirectory() {
        val config = KDownloaderConfig()
        assertNull(config.defaultDirectory)
    }

    @Test
    fun defaultConfigHasAnyNetworkType() {
        val config = KDownloaderConfig()
        assertEquals(NetworkType.ANY, config.defaultNetworkType)
    }

    @Test
    fun configCanSetDirectory() {
        val config = KDownloaderConfig(defaultDirectory = "Downloads/MyApp")
        assertEquals("Downloads/MyApp", config.defaultDirectory)
    }

    @Test
    fun configCanSetNetworkType() {
        val config = KDownloaderConfig(defaultNetworkType = NetworkType.WIFI_ONLY)
        assertEquals(NetworkType.WIFI_ONLY, config.defaultNetworkType)
    }

    @Test
    fun configCanSetBothValues() {
        val config = KDownloaderConfig(
            defaultDirectory = "custom/path",
            defaultNetworkType = NetworkType.WIFI_ONLY
        )
        assertEquals("custom/path", config.defaultDirectory)
        assertEquals(NetworkType.WIFI_ONLY, config.defaultNetworkType)
    }
}
