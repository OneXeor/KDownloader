# KDownloader

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-purple.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-green.svg)](https://kotlinlang.org/docs/multiplatform.html)

A lightweight Kotlin Multiplatform download manager for Android and iOS. Uses platform-native APIs with no external dependencies.

## Features

- Simple, unified API across platforms
- Progress tracking with callbacks
- Error handling with detailed information
- Cancel downloads by ID
- MIME type detection
- Platform-native implementations (Android `DownloadManager`, iOS `NSURLSession`)

## Installation

Add the GitHub Packages repository and dependency to your project:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/OneXeor/KDownloader")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("USERNAME")
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("API_KEY")
            }
        }
    }
}

// build.gradle.kts (shared module)
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("dev.onexeor.kdownloader:shared:0.0.6")
        }
    }
}
```

## Usage

### Basic Download

```kotlin
val downloader = KDownloader(context) // Android requires Context

val downloadId = downloader.downloadFile(
    url = "https://example.com/file.pdf",
    fileName = "document.pdf",
    progressListener = { uri, status ->
        println("Download progress: $uri, status: $status")
    },
    errorListener = { error ->
        println("Error: ${error.description} (${error.statusCode})")
    }
)
```

### Cancel Download

```kotlin
downloader.cancelDownloadById(downloadId)
```

### Get Download Info

```kotlin
val mimeType = downloader.getMimeTypeById(downloadId)
val url = downloader.getUrlById(downloadId)
```

## API Reference

### KDownloader

| Method | Description |
|--------|-------------|
| `downloadFile(url, fileName?, progressListener?, errorListener?)` | Start a download, returns download ID |
| `cancelDownloadById(downloadId)` | Cancel an active download |
| `getMimeTypeById(downloadId)` | Get MIME type of downloaded file |
| `getUrlById(downloadId)` | Get original URL of download |

### DownloadError

```kotlin
data class DownloadError(
    val url: String,        // Original download URL
    val status: Int,        // Download manager status code
    val description: String,// Human-readable error description
    val statusCode: Int     // HTTP status code
)
```

## Platform Requirements

| Platform | Minimum Version |
|----------|-----------------|
| Android  | API 24 (7.0)    |
| iOS      | Supported via KMP |

## Roadmap

- [ ] Authentication support (Basic, Token)
- [ ] Cookie handling
- [ ] Custom headers
- [ ] Download queue management

## License

```
Copyright 2024 Viktor Savchik

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
