import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

group = "dev.onexeor.kdownloader"
version = "0.0.5"

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
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

    publishing()
}

android {
    namespace = group.toString()
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
}


fun KotlinMultiplatformExtension.publishing() {
    val publicationsFromMainHost = listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        androidTarget()
    ).map { it.name } + "kotlinMultiplatform"


    publishing {
        publications {
            matching { it.name in publicationsFromMainHost }.all {
                val targetPublication = this@all
                tasks.withType<AbstractPublishToMaven>()
                    .matching { it.publication == targetPublication }
                    .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
            }
        }
        println("UserName ${System.getenv("USERNAME")}")
        println("Password: ${System.getenv("API_KEY")}")
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
}
