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
    }
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
