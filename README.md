# KDownloader

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-purple.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-green.svg)](https://kotlinlang.org/docs/multiplatform.html)

A lightweight Kotlin Multiplatform download manager for Android and iOS. Uses platform-native APIs with no external dependencies.

## Features

- Kotlin DSL for easy configuration
- Progress tracking with callbacks
- State management (pending, downloading, paused, completed, failed, cancelled)
- Authentication support (Bearer token, Basic auth)
- Custom headers
- Network type restrictions (WiFi only)
- Platform-native implementations:
  - Android: `DownloadManager` (system-managed, survives app kill, shows notifications)
  - iOS: `NSURLSession` (background download support)

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
            implementation("dev.onexeor:kdownloader:1.0.0")
        }
    }
}
```

### Android Setup

Initialize KDownloader in your Application class:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KDownloader.init(this)
    }
}
```

## Usage

### Basic Download

```kotlin
val downloader = KDownloader()

downloader.download("https://example.com/file.pdf") {
    fileName = "document.pdf"

    onProgress { progress ->
        println("${progress.percentage}%")
    }

    onComplete { filePath ->
        println("Downloaded to: $filePath")
    }

    onError { error ->
        println("Failed: ${error.message}")
    }
}
```

### With Authentication

```kotlin
downloader.download("https://api.example.com/private/file.zip") {
    fileName = "data.zip"

    auth {
        bearer("your-access-token")
    }

    onComplete { println("Done: $it") }
}
```

### With Custom Headers

```kotlin
downloader.download("https://example.com/file.pdf") {
    headers {
        "X-Custom-Header" to "value"
        "Accept" to "application/octet-stream"
    }
}
```

### WiFi Only Download

```kotlin
downloader.download("https://example.com/large-file.zip") {
    wifiOnly()

    onStateChange { state ->
        when (state) {
            is DownloadState.Paused -> println("Waiting: ${state.reason}")
            is DownloadState.Downloading -> println("Progress: ${state.progress.percentage}%")
            is DownloadState.Completed -> println("Done!")
            else -> {}
        }
    }
}
```

### Task Control

```kotlin
val task = downloader.download("https://example.com/file.zip")

// Add listeners after creation (fluent API)
task.onProgress { println("${it.percentage}%") }
    .onComplete { println("Done: $it") }
    .onError { println("Error: ${it.message}") }

// Check current state
println("State: ${task.currentState}")
println("Progress: ${task.currentProgress.percentage}%")

// Cancel if needed
task.cancel()
```

### Configuration

```kotlin
val downloader = KDownloader(
    KDownloaderConfig(
        defaultDirectory = "MyApp/Downloads",
        defaultNetworkType = NetworkType.ANY
    )
)
```

## API Reference

### KDownloader

| Method | Description |
|--------|-------------|
| `download(url, builder)` | Start a download with DSL configuration |
| `download(request)` | Start a download with pre-built request |
| `getTask(id)` | Get an existing download task by ID |
| `cancelAll()` | Cancel all active downloads |

### DownloadState

| State | Description |
|-------|-------------|
| `Pending` | Download is queued |
| `Downloading(progress)` | Download is in progress |
| `Paused(reason)` | Download is paused |
| `Completed(filePath)` | Download completed successfully |
| `Failed(error)` | Download failed with an error |
| `Cancelled` | Download was cancelled |

### DownloadError Types

| Type | Description |
|------|-------------|
| `Network` | Connection failed, timeout, etc. |
| `Http` | HTTP error response (4xx, 5xx) |
| `Storage` | Disk full, permission denied, etc. |
| `InvalidUrl` | Malformed URL |
| `Cancelled` | User cancelled the download |
| `Unknown` | Unexpected error |

### Auth

```kotlin
auth {
    bearer("token")           // Bearer token authentication
    basic("user", "password") // Basic authentication
}
```

## Platform Requirements

| Platform | Minimum Version |
|----------|-----------------|
| Android  | API 24 (7.0)    |
| iOS      | 13.0            |

## Roadmap

- [x] Authentication support (Bearer, Basic)
- [x] Custom headers
- [x] Kotlin DSL API
- [ ] Download queue management
- [ ] Resume/pause support
- [ ] Download speed tracking

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
