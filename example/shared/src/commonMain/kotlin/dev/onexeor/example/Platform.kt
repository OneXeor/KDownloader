package dev.onexeor.example

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform