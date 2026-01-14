package dev.onexeor.kdownloader

/**
 * Network type restriction for downloads.
 */
enum class NetworkType {
    /**
     * Allow download on any network (WiFi or cellular).
     */
    ANY,

    /**
     * Only download when connected to WiFi.
     */
    WIFI_ONLY
}
