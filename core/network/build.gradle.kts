plugins {
    alias(libs.plugins.shelf.android.library)
    alias(libs.plugins.shelf.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.shelf.core.network"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.common)
}
