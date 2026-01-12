# Contributing to KDownloader

Thank you for your interest in contributing to KDownloader! This document provides guidelines for contributing to the project.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Create a branch for your changes

## Development Setup

### Prerequisites

- JDK 8 or higher
- Android SDK with API 34
- Xcode (for iOS development on macOS)

### Building

```bash
# Build the library
./gradlew build

# Build example app
./gradlew :example:androidApp:build
```

## Making Changes

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions focused and concise

### Multiplatform Considerations

- Shared code goes in `commonMain`
- Platform-specific implementations go in `androidMain` or `iosMain`
- Use `expect`/`actual` declarations for platform-specific APIs
- Avoid platform-specific dependencies in common code

### Commit Messages

- Use clear, descriptive commit messages
- Start with a verb (Add, Fix, Update, Remove)
- Keep the first line under 72 characters

## Pull Requests

1. Update your branch with the latest `main`
2. Ensure the build passes: `./gradlew build`
3. Open a pull request with a clear description
4. Link any related issues

## Reporting Issues

When reporting issues, please include:

- Library version
- Platform (Android/iOS) and OS version
- Steps to reproduce
- Expected vs actual behavior
- Relevant code snippets or logs

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
