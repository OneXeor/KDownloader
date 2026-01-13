plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

group = "dev.onexeor.kdownloader"
version = "0.2.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "KDownloader"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
        }
    }
}

android {
    namespace = group.toString()
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}

publishing {
    repositories {
        maven {
            name = "KDownloader"
            url = uri("https://maven.pkg.github.com/OneXeor/KDownloader")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("API_KEY")
            }
        }
    }
}
